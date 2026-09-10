/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence;

import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.search.JakartaSearchSettings;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.search.ProjectWideNameScanner;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Diagnostics collector that validates {@code @NamedEntityGraph} usage.
 *
 * <p>Rule enforced:
 * <ul>
 *   <li>{@link PersistenceConstants#DIAGNOSTIC_CODE_DUPLICATE_NAMED_ENTITY_GRAPH}:
 *       Graph names must be unique within the persistence unit.
 *       Spec §3.7.4:
 *       <a href="https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0.html#a13662">§a13662</a>
 *   </li>
 * </ul>
 *
 * <p>The project-wide name collection is delegated entirely to
 * {@link ProjectWideNameScanner}, which owns the scan backend and performance flag.
 * This collector only supplies the annotation-level extraction logic (what to collect)
 * as a {@link io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.search.NameExtractorStrategy} lambda.
 */
public class NamedEntityGraphDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public NamedEntityGraphDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return PersistenceConstants.DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit == null) {
            return;
        }

        // Guard: skip the expensive project-wide scan entirely unless the current
        // file contains at least one @Entity class that also carries @NamedEntityGraph
        // or @NamedEntityGraphs. This is an O(annotations-in-file) check.
        if (!fileHasEntityWithGraphAnnotation(unit)) {
            return;
        }

        // Feature gate: all search-engine-based diagnostics are disabled when
        // JakartaSearchSettings.SEARCH_ENGINE_DIAGNOSTICS_ENABLED is false.
        // This single flag disables every diagnostic that calls ProjectWideNameScanner.
        if (!JakartaSearchSettings.SEARCH_ENGINE_DIAGNOSTICS_ENABLED) {
            return;
        }

        // Phase 1: collect all @NamedEntityGraph names project-wide via the scanner.
        // Maps each graph name to the number of times it is declared across the entire
        // project. Any name with a count > 1 is a duplicate.
        Map<String, Integer> graphNameOccurrences = ProjectWideNameScanner.scan(
                unit.getProject(),
                (psiClass, nameCount) -> extractNamesFromClass(psiClass, nameCount));

        // Phase 2: validate classes in the current file against the collected counts.
        for (PsiClass psiClass : unit.getClasses()) {
            validateClass(psiClass, unit, graphNameOccurrences, diagnostics);
        }
    }

    // =========================================================================
    // File-level guard
    // =========================================================================

    /**
     * Returns {@code true} if the file contains at least one class annotated with
     * {@code @Entity} that also carries {@code @NamedEntityGraph} or
     * {@code @NamedEntityGraphs}.
     *
     * <p>This is a cheap O(annotations-in-file) check used to skip the expensive
     * project-wide scan for files that cannot possibly produce this diagnostic.
     */
    private boolean fileHasEntityWithGraphAnnotation(PsiJavaFile unit) {
        for (PsiClass psiClass : unit.getClasses()) {
            boolean hasEntity = false;
            boolean hasGraph = false;
            for (PsiAnnotation annotation : psiClass.getAnnotations()) {
                String qualifiedName = annotation.getQualifiedName();
                if (qualifiedName == null) {
                    continue;
                }
                if (PersistenceConstants.ENTITY.equals(qualifiedName)) {
                    hasEntity = true;
                }
                if (PersistenceConstants.NAMED_ENTITY_GRAPH.equals(qualifiedName)
                        || PersistenceConstants.NAMED_ENTITY_GRAPHS.equals(qualifiedName)) {
                    hasGraph = true;
                }
                if (hasEntity && hasGraph) {
                    return true;
                }
            }
        }
        return false;
    }

    // =========================================================================
    // Extraction logic — supplied to ProjectWideNameScanner as a lambda
    // =========================================================================

    /**
     * Inspects class-level annotations and merges any {@code @NamedEntityGraph} names
     * (including those nested inside {@code @NamedEntityGraphs}) into {@code nameCount}.
     */
    private void extractNamesFromClass(PsiClass psiClass, Map<String, Integer> graphNameOccurrences) {
        for (PsiAnnotation annotation : psiClass.getAnnotations()) {
            forEachGraph(annotation, inner -> {
                String name = AnnotationUtils.getAnnotationMemberValue(inner, "name");
                if (name != null) {
                    graphNameOccurrences.merge(name, 1, Integer::sum);
                }
            });
        }
    }

    // =========================================================================
    // Validation — flags duplicates in the current file
    // =========================================================================

    /**
     * Checks all {@code @NamedEntityGraph} / {@code @NamedEntityGraphs} annotations
     * on {@code psiClass} and adds a diagnostic for any whose name appears more than
     * once in the project-wide count map.
     */
    private void validateClass(PsiClass psiClass, PsiJavaFile unit,
                               Map<String, Integer> graphNameOccurrences, List<Diagnostic> diagnostics) {
        for (PsiAnnotation annotation : psiClass.getAnnotations()) {
            forEachGraph(annotation, inner -> checkForDuplicate(inner, unit, graphNameOccurrences, diagnostics));
        }
    }

    private void checkForDuplicate(PsiAnnotation annotation, PsiJavaFile unit,
                                   Map<String, Integer> graphNameOccurrences, List<Diagnostic> diagnostics) {
        String graphName = AnnotationUtils.getAnnotationMemberValue(annotation, "name");
        if (graphName == null) {
            return;
        }
        if (graphNameOccurrences.getOrDefault(graphName, 0) > 1) {
            diagnostics.add(createDiagnostic(
                    annotation, unit,
                    Messages.getMessage("DuplicateNamedEntityGraphName", graphName),
                    PersistenceConstants.DIAGNOSTIC_CODE_DUPLICATE_NAMED_ENTITY_GRAPH,
                    null,
                    DiagnosticSeverity.Error));
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Invokes {@code consumer} for every individual {@code @NamedEntityGraph}
     * reachable from {@code annotation}: directly if it is a
     * {@code @NamedEntityGraph}, or once per nested entry if it is a
     * {@code @NamedEntityGraphs} container.
     */
    private void forEachGraph(PsiAnnotation annotation, Consumer<PsiAnnotation> consumer) {
        String qualifiedName = annotation.getQualifiedName();
        if (PersistenceConstants.NAMED_ENTITY_GRAPH.equals(qualifiedName)) {
            consumer.accept(annotation);
        } else if (PersistenceConstants.NAMED_ENTITY_GRAPHS.equals(qualifiedName)) {
            forEachNestedGraph(annotation, consumer);
        }
    }

    /**
     * Iterates every {@code @NamedEntityGraph} nested inside the given
     * {@code @NamedEntityGraphs} container annotation and passes each to
     * {@code consumer}.  Handles both array form ({@code @NamedEntityGraphs({a, b})})
     * and single-element form ({@code @NamedEntityGraphs(a)}).
     */
    private void forEachNestedGraph(PsiAnnotation container, Consumer<PsiAnnotation> consumer) {
        PsiAnnotationMemberValue value = container.findAttributeValue("value");
        if (value instanceof PsiArrayInitializerMemberValue) {
            for (PsiAnnotationMemberValue item : ((PsiArrayInitializerMemberValue) value).getInitializers()) {
                if (item instanceof PsiAnnotation) {
                    consumer.accept((PsiAnnotation) item);
                }
            }
        } else if (value instanceof PsiAnnotation) {
            consumer.accept((PsiAnnotation) value);
        }
    }

}
