/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation, Pengyu Xiong and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation, Pengyu Xiong - initial API and implementation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.servlet;

import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.List;

/**
 *
 * jararta.annotation Diagnostics
 *
 * <li>Diagnostic 1: Class annotated with @WebServlet does not extend the
 * HttpServlet class.</li>
 * <li>Diagnostic 2: @WebServlet missing 'urlPatterns' and 'value' attribute
 * (one must be specified).</li>
 * <li>Diagnostic 3: @WebServlet has both 'urlPatterns' and 'value' attributes
 * specified.</li>
 *
 * @see <a href="https://jakarta.ee/specifications/servlet/5.0/jakarta-servlet-spec-5.0.html#webservlet">...</a>
 *
 */
public class ServletDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public ServletDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return ServletConstants.DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit != null) {
            PsiClass[] alltypes;
            PsiAnnotation[] allAnnotations;

            alltypes = unit.getClasses();
            for (PsiClass type : alltypes) {
                allAnnotations = type.getAnnotations();

                PsiAnnotation webServletAnnotation = null;
                for (PsiAnnotation annotation : allAnnotations) {
                    if (isMatchedJavaElement(type, annotation.getQualifiedName(),
                            ServletConstants.WEB_SERVLET_FQ_NAME)) {
                        webServletAnnotation = annotation;
                        break; // get the first one, the annotation is not repeatable
                    }
                }

                if (webServletAnnotation != null) {
                    // check if the class extends HttpServlet
                    int r = 1;
                    JavaPsiFacade facade = JavaPsiFacade.getInstance(type.getProject());
                    PsiClass httpServletClass = facade.findClass("jakarta.servlet.http.HttpServlet",
                            GlobalSearchScope.allScope(type.getProject()));
                    if (!type.isInheritor(httpServletClass, true)) {
                        r = -1;
                    }

                    if (r == -1) {
                        diagnostics.add(createDiagnostic(type, unit,
                                "Annotated classes with @WebServlet must extend the HttpServlet class.",
                                ServletConstants.DIAGNOSTIC_CODE, null, DiagnosticSeverity.Error));
                    } else if (r == 0) { // unknown super type
                        diagnostics.add(createDiagnostic(type, unit,
                                "Annotated classes with @WebServlet should extend the HttpServlet class.",
                                ServletConstants.DIAGNOSTIC_CODE, null, DiagnosticSeverity.Warning));
                    }

                    /* URL pattern diagnostic check */
                    PsiNameValuePair[] memberValues = webServletAnnotation.getParameterList().getAttributes();

                    boolean isUrlpatternSpecified = false;
                    boolean isValueSpecified = false;
                    for (PsiNameValuePair mv : memberValues) {
                        if (mv.getAttributeName().equals(ServletConstants.URL_PATTERNS)) {
                            isUrlpatternSpecified = true;
                            continue;
                        }
                        if (mv.getAttributeName().equals(ServletConstants.VALUE)) {
                            isValueSpecified = true;
                        }
                    }
                    if (!isUrlpatternSpecified && !isValueSpecified) {
                        diagnostics.add(createDiagnostic(webServletAnnotation, unit,
                                "The @WebServlet annotation must define the attribute 'urlPatterns' or 'value'.",
                                ServletConstants.DIAGNOSTIC_CODE_MISSING_ATTRIBUTE, null,
                                DiagnosticSeverity.Error));
                    }
                    if (isUrlpatternSpecified && isValueSpecified) {
                        diagnostics.add(createDiagnostic(webServletAnnotation, unit,
                                "The @WebServlet annotation cannot have both 'value' and 'urlPatterns' attributes specified at once.",
                                ServletConstants.DIAGNOSTIC_CODE_DUPLICATE_ATTRIBUTES, null,
                                DiagnosticSeverity.Error));
                    }
                }
            }
        }
    }
}
