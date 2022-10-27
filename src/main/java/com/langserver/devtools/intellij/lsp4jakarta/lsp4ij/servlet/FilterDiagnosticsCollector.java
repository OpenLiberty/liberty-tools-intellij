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
import com.intellij.psi.PsiNameValuePair;
import com.langserver.devtools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.List;

public class FilterDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public FilterDiagnosticsCollector() {
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
                PsiAnnotation webFilterAnnotation = null;

                for (PsiAnnotation annotation : allAnnotations) {
                    if (isMatchedJavaElement(type, annotation.getQualifiedName(),
                            ServletConstants.WEBFILTER_FQ_NAME)) {
                        webFilterAnnotation = annotation;
                    }
                }

                String[] interfaces = {ServletConstants.FILTER_FQ_NAME};
                boolean isFilterImplemented = doesImplementInterfaces(type, interfaces);

                if (webFilterAnnotation != null && !isFilterImplemented) {
                    diagnostics.add(createDiagnostic(type, unit,
                            "Annotated classes with @WebFilter must implement the Filter interface.",
                            ServletConstants.DIAGNOSTIC_CODE_FILTER, null, DiagnosticSeverity.Error));
                }

                /* URL pattern diagnostic check */
                if (webFilterAnnotation != null) {
                    PsiNameValuePair[] memberValues = webFilterAnnotation.getParameterList().getAttributes();

                    boolean isUrlpatternSpecified = false;
                    boolean isServletNamesSpecified = false;
                    boolean isValueSpecified = false;
                    for (PsiNameValuePair mv : memberValues) {
                        if (mv.getAttributeName().equals(ServletConstants.URL_PATTERNS)) {
                            isUrlpatternSpecified = true;
                            continue;
                        }
                        if (mv.getAttributeName().equals(ServletConstants.SERVLET_NAMES)) {
                            isServletNamesSpecified = true;
                            continue;
                        }
                        if (mv.getAttributeName().equals(ServletConstants.VALUE)) {
                            isValueSpecified = true;
                        }
                    }
                    if (!isUrlpatternSpecified && !isValueSpecified && !isServletNamesSpecified) {
                        diagnostics.add(createDiagnostic(webFilterAnnotation, unit,
                                "The annotation @WebFilter must define the attribute 'urlPatterns', 'servletNames' or 'value'.",
                                ServletConstants.DIAGNOSTIC_CODE_FILTER_MISSING_ATTRIBUTE, null,
                                DiagnosticSeverity.Error));
                    }
                    if (isUrlpatternSpecified && isValueSpecified) {
                        diagnostics.add(createDiagnostic(webFilterAnnotation, unit,
                                "The annotation @WebFilter can not have both 'value' and 'urlPatterns' attributes specified at once.",
                                ServletConstants.DIAGNOSTIC_CODE_FILTER_DUPLICATE_ATTRIBUTES, null,
                                DiagnosticSeverity.Error));
                    }
                }
            }
        }
    }
}
