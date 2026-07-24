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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.List;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.*;

/**
 * CDI diagnostics collector that validates specialization.
 *
 * Per CDI spec section 3.1.4: a class annotated with @Specializes must directly
 * extend a managed bean (one whose immediate superclass carries a CDI scope annotation).
 * A scoped grandparent does NOT satisfy this requirement.
 *
 * @see <a href="https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0#specializing_a_managed_bean">CDI 3.0 §3.1.4</a>
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
        for (PsiClass type : unit.getClasses()) {
            validateSpecializes(type, unit, diagnostics);
        }
    }

    /**
     * Validates that a class annotated with @Specializes directly extends a valid bean.
     *
     * Per CDI spec 3.1.4: "the bean class of X must directly extend the bean class
     * of another managed bean Y". Only the immediate superclass is checked — a scoped
     * grandparent does NOT satisfy this requirement.
     *
     * @param type        the class to validate
     * @param unit        the containing file
     * @param diagnostics the list to add diagnostics to
     */
    private void validateSpecializes(PsiClass type, PsiJavaFile unit, List<Diagnostic> diagnostics) {
        // Only validate classes annotated with @Specializes
        if (!AnnotationUtils.hasAnnotation(type, SPECIALIZES_FQ_NAME)) {
            return;
        }

        // Per CDI spec 3.1.4, only the direct (immediate) superclass must be a bean.
        // A resolved superclass with any CDI scope annotation is valid; anything else
        // (null superclass or unscoped superclass) is a definition error.
        PsiClass superclass = type.getSuperClass();
        boolean directSuperclassIsBean = superclass != null
                && AnnotationUtils.hasAnyAnnotation(superclass, ALL_SCOPE_FQ_NAMES);

        if (!directSuperclassIsBean) {
            diagnostics.add(createDiagnostic(type, unit,
                    Messages.getMessage("InvalidSpecializesAnnotationOnNonBeanSuperclass"),
                    DIAGNOSTIC_CODE_INVALID_SPECIALIZES, null,
                    DiagnosticSeverity.Error));
        }
    }
}
