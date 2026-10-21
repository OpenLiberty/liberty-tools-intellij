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
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.util.PsiUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.List;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.*;

/**
 * Detects inconsistent specialization: more than one bean annotated with {@code @Specializes}
 * directly extends the same superclass bean, per CDI spec §5.1.3.
 *
 * <p>A valid transitive chain (B {@code @Specializes} A, A {@code @Specializes} Base) is permitted
 * because at each step only one direct subclass carries {@code @Specializes}.
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

        // Get all classes including nested classes
        List<PsiClass> allTypes = new ArrayList<>();
        PsiUtils.collectAllClasses(unit.getClasses(), allTypes);

        for (PsiClass type : allTypes) {
            if (!isMatchedAnnotation(type.getAnnotations(), SPECIALIZES_FQ_NAME)) {
                continue;
            }

            // Rule 3: inconsistent specialization — more than one direct subclass of the
            // immediate superclass carries @Specializes (CDI spec §5.1.3).
            PsiClass directSuperClass = type.getSuperClass();
            if (directSuperClass == null) {
                continue;
            }
            String superFqName = directSuperClass.getQualifiedName();
            if (superFqName == null || JAVA_LANG_OBJECT.equals(superFqName)) {
                continue;
            }
            if (hasInconsistentSpecialization(directSuperClass, type)) {
                diagnostics.add(createDiagnostic(type, unit,
                        Messages.getMessage("InconsistentSpecialization",
                                type.getName(), superFqName),
                        DIAGNOSTIC_CODE_INCONSISTENT_SPECIALIZATION, null,
                        DiagnosticSeverity.Error));
            }
        }
    }

    /**
     * Returns {@code true} if more than one direct subclass of {@code superClass}
     * carries {@code @Specializes}, indicating inconsistent specialization.
     *
     * <p>Uses {@link ClassInheritorsSearch} with {@code deep=false} to restrict the
     * search to immediate subclasses only, so valid linear transitive chains are not flagged.
     *
     * @param superClass   the direct superclass to inspect
     * @param currentType  the type being validated (used to resolve the project scope)
     * @return {@code true} if the specialization is inconsistent
     */
    private boolean hasInconsistentSpecialization(PsiClass superClass, PsiClass currentType) {
        GlobalSearchScope scope = GlobalSearchScope.projectScope(currentType.getProject());
        int specializerCount = 0;
        for (PsiClass subclass : ClassInheritorsSearch.search(superClass, scope, false)) {
            if (isMatchedAnnotation(subclass.getAnnotations(), SPECIALIZES_FQ_NAME)) {
                specializerCount++;
                if (specializerCount > 1) {
                    return true;
                }
            }
        }
        return false;
    }
}
