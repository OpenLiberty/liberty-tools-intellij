/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation, Reza Akhavan and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation, Reza Akhavan - initial API and implementation
 *******************************************************************************/

package com.langserver.devtools.intellij.lsp4jakarta.lsp4ij.servlet;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.langserver.devtools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.List;

public class ListenerDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public ListenerDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return ServletConstants.DIAGNOSTIC_SOURCE;
    }

    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit != null) {
            PsiClass[] alltypes;
            PsiAnnotation[] allAnnotations;

            alltypes = unit.getClasses();
            for (PsiClass type : alltypes) {
                allAnnotations = type.getAnnotations();
                boolean isWebListenerAnnotated = false;
                for (PsiAnnotation annotation : allAnnotations) {
                    if (isMatchedJavaElement(type, annotation.getQualifiedName(),
                            ServletConstants.WEB_LISTENER_FQ_NAME)) {
                        isWebListenerAnnotated = true;
                        break;
                    }
                }

                String[] interfaces = { ServletConstants.SERVLET_CONTEXT_LISTENER_FQ_NAME,
                        ServletConstants.SERVLET_CONTEXT_ATTRIBUTE_LISTENER_FQ_NAME,
                        ServletConstants.SERVLET_REQUEST_LISTENER_FQ_NAME,
                        ServletConstants.SERVLET_REQUEST_ATTRIBUTE_LISTENER_FQ_NAME,
                        ServletConstants.HTTP_SESSION_LISTENER_FQ_NAME,
                        ServletConstants.HTTP_SESSION_ATTRIBUTE_LISTENER_FQ_NAME,
                        ServletConstants.HTTP_SESSION_ID_LISTENER_FQ_NAME };
                boolean isImplemented = doesImplementInterfaces(type, interfaces);

                if (isWebListenerAnnotated && !isImplemented) {
                    diagnostics.add(createDiagnostic(type, unit,
                            "Annotated classes with @WebListener must implement one or more of the following interfaces: ServletContextListener, ServletContextAttributeListener,"
                                    + " ServletRequestListener, ServletRequestAttributeListener, HttpSessionListener, HttpSessionAttributeListener, or HttpSessionIdListener.",
                            ServletConstants.DIAGNOSTIC_CODE_LISTENER, null, DiagnosticSeverity.Error));
                }
            }
        }
    }
}
