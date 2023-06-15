/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation, Matthew Shocrylas and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation, Matthew Shocrylas - initial API and implementation, Bera Sogut
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jax_rs;

import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.List;

public class ResourceMethodDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public ResourceMethodDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return Jax_RSConstants.DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {

        if (unit != null) {
            String[] methodDesignators = ArrayUtils.addAll(Jax_RSConstants.SET_OF_METHOD_DESIGNATORS_ANNOTATIONS,
                    Jax_RSConstants.PATH_ANNOTATION);
            PsiClass[] alltypes;
            PsiMethod[] methods;

            alltypes = unit.getClasses();
            for (PsiClass type : alltypes) {
                methods = type.getMethods();
                for (PsiMethod method : methods) {
                    PsiAnnotation[] methodAnnotations = method.getAnnotations();
                    boolean isResourceMethod = false;
                    boolean isValid = true;
                    boolean isPublic = method.hasModifierProperty(PsiModifier.PUBLIC);

                    for (PsiAnnotation annotation : methodAnnotations) {
                        String matchedAnnotation = getMatchedJavaElementName(type, annotation.getQualifiedName(),
                                methodDesignators);
                        if (matchedAnnotation != null) {
                            if (isValid && !isPublic)
                                isValid = false;
                            if (!Jax_RSConstants.PATH_ANNOTATION.equals(matchedAnnotation)) {
                                isResourceMethod = true;
                                break;
                            }
                        }
                    }
                    if (!isValid) {
                        diagnostics.add(createDiagnostic(method, unit,
                                "Only public methods can be exposed as resource methods",
                                Jax_RSConstants.DIAGNOSTIC_CODE_NON_PUBLIC, null, DiagnosticSeverity.Error));
                    }
                    if (isResourceMethod) {
                        int numEntityParams = 0;
                        PsiParameter[] parameters = method.getParameterList().getParameters();
                        for (PsiParameter param : parameters) {
                            boolean isEntityParam = true;
                            PsiAnnotation[] annotations = param.getAnnotations();
                            for (PsiAnnotation annotation : annotations) {
                                String matchedAnnotation = getMatchedJavaElementName(type,
                                        annotation.getQualifiedName(),
                                        Jax_RSConstants.SET_OF_NON_ENTITY_PARAM_ANNOTATIONS);
                                if (matchedAnnotation != null) {
                                    isEntityParam = false;
                                    break;
                                }
                            }
                            if (isEntityParam)
                                numEntityParams++;
                        }
                        if (numEntityParams > 1) {
                            diagnostics.add(createDiagnostic(method, unit,
                                    "Resource methods cannot have more than one entity parameter",
                                    Jax_RSConstants.DIAGNOSTIC_CODE_MULTIPLE_ENTITY_PARAMS, null,
                                    DiagnosticSeverity.Error));
                        }
                    }
                }
            }
        }
    }
}
