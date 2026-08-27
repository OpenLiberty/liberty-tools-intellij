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

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.DiagnosticsUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistence diagnostic collector that validates bidirectional JPA
 * relationships across entity classes.
 *
 * <p>Two rules are checked using cross-file (module-wide) analysis:
 * <ol>
 *   <li>The inverse side of a bidirectional relationship must declare the
 *       {@code mappedBy} attribute on its {@code @OneToMany}, {@code @OneToOne},
 *       or {@code @ManyToMany} annotation.</li>
 *   <li>The inverse side of a relationship must not carry a {@code @JoinTable}
 *       annotation.</li>
 * </ol>
 *
 * <p>Cross-file analysis is performed via
 * {@link DiagnosticsUtils#scanSourceClasses}, which traverses
 * the module's source roots directly and is always consistent with the
 * current workspace state.
 *
 * <p>Specification reference:
 * https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0
 */
public class PersistenceBidirectionalDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public PersistenceBidirectionalDiagnosticsCollector() {
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

        for (PsiClass type : unit.getClasses()) {
            // Only process @Entity-annotated classes.
            if (!isMatchedAnnotation(type.getAnnotations(), PersistenceConstants.ENTITY)) {
                continue;
            }

            // Build a module-wide map of all @Entity types keyed by simple name using
            // the generic scanner — populated once per entity class in the file.
            Map<String, PsiClass> entityTypeMap = new HashMap<>();
            DiagnosticsUtils.scanSourceClasses(type, scannedClass -> {
                if (isMatchedAnnotation(scannedClass.getAnnotations(), PersistenceConstants.ENTITY)
                        && scannedClass.getName() != null) {
                    entityTypeMap.put(scannedClass.getName(), scannedClass);
                }
            });

            for (PsiField field : type.getFields()) {
                validateRelationshipMember(field, field.getType(), type, unit,
                        entityTypeMap, diagnostics);
            }

            for (PsiMethod method : type.getMethods()) {
                validateRelationshipMember(method, method.getReturnType(), type, unit,
                        entityTypeMap, diagnostics);
            }
        }
    }

    /**
     * Validates relationship annotations on a single field or method.
     *
     * @param member        the PSI element (field or method) being validated
     * @param memberType    the field type or method return type
     * @param declaringType the entity class that owns the member
     * @param unit          the compilation unit of the declaring class
     * @param entityTypeMap module-wide map from simple class name to {@link PsiClass}
     * @param diagnostics   the list to append new diagnostics to
     */
    private void validateRelationshipMember(PsiElement member, PsiType memberType,
                                            PsiClass declaringType,
                                            PsiJavaFile unit,
                                            Map<String, PsiClass> entityTypeMap,
                                            List<Diagnostic> diagnostics) {
        for (String relAnnotationFQ : PersistenceConstants.INVERSE_CAPABLE_RELATIONSHIP_ANNOTATIONS) {
            PsiAnnotation relAnnotation = AnnotationUtils.getAnnotation(member, relAnnotationFQ);
            if (relAnnotation == null) {
                continue;
            }

            String relSimpleName = JDTUtils.getSimpleName(relAnnotationFQ);
            String mappedByValue = AnnotationUtils.getAnnotationMemberValue(
                    relAnnotation, PersistenceConstants.MAPPED_BY);
            boolean hasMappedBy = mappedByValue != null && !mappedByValue.isEmpty();

            if (hasMappedBy) {
                // Explicitly the inverse side — @JoinTable is forbidden here.
                if (AnnotationUtils.getAnnotation(member, PersistenceConstants.JOIN_TABLE) != null) {
                    diagnostics.add(createDiagnostic(member, unit,
                            Messages.getMessage("JoinTableOnInverseSide"),
                            PersistenceConstants.DIAGNOSTIC_CODE_JOIN_TABLE_ON_INVERSE, null,
                            DiagnosticSeverity.Error));
                }
            } else {
                // No mappedBy — flag only when the target entity back-references this class,
                // proving the relationship is bidirectional.
                String targetSimpleName = DiagnosticsUtils.getElementTypeSimpleName(memberType);
                PsiClass targetType = targetSimpleName != null
                        ? entityTypeMap.get(targetSimpleName) : null;
                if (targetType != null && isInverseSideOf(targetType, declaringType, relAnnotationFQ)) {
                    diagnostics.add(createDiagnostic(member, unit,
                            Messages.getMessage("InverseSideMissingMappedBy", relSimpleName),
                            PersistenceConstants.DIAGNOSTIC_CODE_INVERSE_MISSING_MAPPED_BY, null,
                            DiagnosticSeverity.Error));
                }
            }
            // Only one relationship annotation per member expected — stop after first match.
            break;
        }
    }

    /**
     * Checks whether {@code targetType} has a back-reference field or method
     * pointing at {@code declaringType} via the mirrored relationship annotation,
     * confirming a bidirectional relationship.
     *
     * @param targetType      the other side of the potential relationship
     * @param declaringType   the entity class whose member is being validated
     * @param relAnnotationFQ the fully-qualified relationship annotation on the declaring side
     * @return {@code true} if a back-reference exists
     */
    private boolean isInverseSideOf(PsiClass targetType, PsiClass declaringType,
                                    String relAnnotationFQ) {
        // Mirrored annotations:
        //   @OneToMany  ↔  @ManyToOne
        //   @ManyToMany ↔  @ManyToMany  (self-mirroring)
        //   @OneToOne   ↔  @OneToOne    (self-mirroring)
        //
        // For self-mirroring annotations (@ManyToMany, @OneToOne), a bare match of the
        // annotation on the other side is not sufficient — the other side must also carry
        // a non-empty mappedBy, which is the explicit marker that it is the inverse side.
        // Without this extra check both sides would flag each other as the inverse.
        String mirroredAnnotationFQ = getMirroredAnnotation(relAnnotationFQ);
        boolean requiresMappedByOnTarget = mirroredAnnotationFQ.equals(relAnnotationFQ);
        String declaringSimpleName = declaringType.getName();

        for (PsiField field : targetType.getFields()) {
            PsiAnnotation annotation = AnnotationUtils.getAnnotation(field, mirroredAnnotationFQ);
            if (annotation != null) {
                if (requiresMappedByOnTarget) {
                    String mappedByValue = AnnotationUtils.getAnnotationMemberValue(
                            annotation, PersistenceConstants.MAPPED_BY);
                    if (mappedByValue == null || mappedByValue.isEmpty()) {
                        continue;
                    }
                }
                if (declaringSimpleName.equals(
                        DiagnosticsUtils.getElementTypeSimpleName(field.getType()))) {
                    return true;
                }
            }
        }

        for (PsiMethod method : targetType.getMethods()) {
            PsiAnnotation annotation = AnnotationUtils.getAnnotation(method, mirroredAnnotationFQ);
            if (annotation != null) {
                if (requiresMappedByOnTarget) {
                    String mappedByValue = AnnotationUtils.getAnnotationMemberValue(
                            annotation, PersistenceConstants.MAPPED_BY);
                    if (mappedByValue == null || mappedByValue.isEmpty()) {
                        continue;
                    }
                }
                PsiType returnType = method.getReturnType();
                if (returnType != null
                        && declaringSimpleName.equals(
                                DiagnosticsUtils.getElementTypeSimpleName(returnType))) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns the mirrored relationship annotation FQ name.
     * {@code @OneToMany} ↔ {@code @ManyToOne}; others are self-mirroring.
     *
     * @param relAnnotationFQ the fully-qualified annotation on the declaring side
     * @return the mirrored fully-qualified annotation name
     */
    private String getMirroredAnnotation(String relAnnotationFQ) {
        if (PersistenceConstants.ONE_TO_MANY.equals(relAnnotationFQ)) {
            return PersistenceConstants.MANY_TO_ONE;
        }
        if (PersistenceConstants.MANY_TO_MANY.equals(relAnnotationFQ)) {
            return PersistenceConstants.MANY_TO_MANY;
        }
        // ONE_TO_ONE is self-mirroring.
        return relAnnotationFQ;
    }
}
