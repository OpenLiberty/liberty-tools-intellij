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

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi;

import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AllClassesSearch;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.*;

/**
 * Detects inconsistent specialization: two or more beans annotated with {@code @Specializes}
 * sharing the same ultimate base bean (resolved transitively).
 */
public class CdiSpecializesDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public CdiSpecializesDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit == null) {
            return;
        }

        // Collect @Specializes types in the current file
        List<PsiClass> specializersInUnit = new ArrayList<>();
        for (PsiClass type : unit.getClasses()) {
            if (isMatchedAnnotation(type.getAnnotations(), SPECIALIZES_FQ_NAME)) {
                specializersInUnit.add(type);
            }
        }

        if (specializersInUnit.isEmpty()) {
            return;
        }

        // Build a map of ultimate base FQ name → all @Specializes types in the project.
        Map<String, List<PsiClass>> specializersByUltimateBase = collectProjectSpecializersByUltimateBase(
                specializersInUnit.get(0).getProject());

        // Report inconsistent specialization on each conflicting type in this file
        for (PsiClass type : specializersInUnit) {
            String ultimateBaseFqName = resolveUltimateBaseFqName(type);
            if (ultimateBaseFqName == null) {
                continue;
            }
            List<PsiClass> allSpecializersOfBase = specializersByUltimateBase.get(ultimateBaseFqName);
            // Inconsistent specialization: more than one bean ultimately specializes the same base.
            if (allSpecializersOfBase != null && allSpecializersOfBase.size() > 1) {
                diagnostics.add(createDiagnostic(type, unit,
                        Messages.getMessage("InconsistentSpecialization",
                                type.getName(), ultimateBaseFqName),
                        DIAGNOSTIC_CODE_INCONSISTENT_SPECIALIZATION, null,
                        DiagnosticSeverity.Error));
            }
        }
    }

    /**
     * Scans all project source types and returns a map from ultimate base FQ name to the
     * list of {@code @Specializes} types (direct or transitive) that specialize that base.
     */
    private Map<String, List<PsiClass>> collectProjectSpecializersByUltimateBase(com.intellij.openapi.project.Project project) {
        Map<String, List<PsiClass>> result = new HashMap<>();
        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);

        AllClassesSearch.search(scope, project).forEach(type -> {
            if (!isMatchedAnnotation(type.getAnnotations(), SPECIALIZES_FQ_NAME)) {
                return true; // continue iteration
            }
            String ultimateBaseFqName = resolveUltimateBaseFqName(type);
            if (ultimateBaseFqName != null) {
                result.computeIfAbsent(ultimateBaseFqName, k -> new ArrayList<>()).add(type);
            }
            return true; // continue iteration
        });

        return result;
    }

    /**
     * Walks the {@code @Specializes} chain transitively and returns the FQ name of the
     * ultimate base bean (the first ancestor that does not itself carry {@code @Specializes}).
     */
    private String resolveUltimateBaseFqName(PsiClass type) {
        PsiClass superClass = type.getSuperClass();
        if (superClass == null) {
            return null;
        }
        String fqName = superClass.getQualifiedName();
        if (fqName == null || "java.lang.Object".equals(fqName)) {
            return null;
        }
        // If the superclass also has @Specializes, keep walking up the chain.
        if (isMatchedAnnotation(superClass.getAnnotations(), SPECIALIZES_FQ_NAME)) {
            return resolveUltimateBaseFqName(superClass);
        }
        return fqName;
    }
}
