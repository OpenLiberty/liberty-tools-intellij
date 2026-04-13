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

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.servlet;

import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.DiagnosticsUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.List;

/**
 * Security diagnostic participant.
 *
 * @see <a href="https://jakarta.ee/specifications/servlet/5.0/jakarta-servlet-spec-5.0#security">...</a>
 */
public class SecurityDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public SecurityDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return ServletConstants.DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit != null) {
            PsiClass[] allTypes;
            PsiAnnotation[] allAnnotations;
            allTypes = unit.getClasses();
            for (PsiClass type : allTypes) {
                allAnnotations = type.getAnnotations();
                PsiAnnotation declareRolesAnnotation = null;
                for (PsiAnnotation annotation : allAnnotations) {
                    if (isMatchedJavaElement(type, annotation.getQualifiedName(),
                            ServletConstants.DECLARE_ROLES_FQ_NAME)) {
                        declareRolesAnnotation = annotation;
                        break; // get the first one, the annotation is not repeatable
                    }
                }
                if (declareRolesAnnotation != null) {
                    if (!DiagnosticsUtils.inheritsFrom(type, ServletConstants.SERVLET_FQ_NAME)) {
                        diagnostics.add(createDiagnostic(type, unit,
                                Messages.getMessage("DeclareRolesMustImplement"),
                                ServletConstants.DIAGNOSTIC_CODE, null, DiagnosticSeverity.Error));
                    }
                }
            }
        }
    }
}
