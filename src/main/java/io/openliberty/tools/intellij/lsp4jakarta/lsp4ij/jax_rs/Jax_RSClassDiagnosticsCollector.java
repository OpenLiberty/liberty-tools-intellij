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
 *     IBM Corporation, Matthew Shocrylas - initial API and implementation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jax_rs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

/**
 * Diagnostic collector for root resource classes with multiple constructors
 *
 * @author Matthew Shocrylas
 *
 */
public class Jax_RSClassDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public Jax_RSClassDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return Jax_RSConstants.DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {

        if (unit != null) {
            PsiClass[] alltypes;

            alltypes = unit.getClasses();
            for (PsiClass type : alltypes) {
                boolean isRootResource = false;
                boolean isProviderResource = false;
                PsiAnnotation[] annotationList = type.getAnnotations();

                for (PsiAnnotation annotation : annotationList) {
                    String matchedAnnotation = getMatchedJavaElementName(type, annotation.getQualifiedName(),
                            Jax_RSConstants.SET_OF_JAXRS_ANNOTATIONS1);
                    if (matchedAnnotation != null) {
                        if (Jax_RSConstants.PATH_ANNOTATION.equals(matchedAnnotation)) {
                            isRootResource = true;
                        } else if (Jax_RSConstants.PROVIDER_ANNOTATION.equals(matchedAnnotation)) {
                            isProviderResource = true;
                        }
                    }
                }

                if (isRootResource || isProviderResource) { // annotated class
                    List<PsiMethod> nonPublicConstructors = new ArrayList<PsiMethod>();
                    boolean hasPublicConstructor = false;
                    int maxParams = 0;
                    Map<PsiMethod, Integer> constructorParamsMap = new HashMap<PsiMethod, Integer>();
                    PsiMethod[] methods = type.getMethods();
                    for (PsiMethod method : methods) {
                        if (isConstructorMethod(method)) {
                            if (method.hasModifierProperty(PsiModifier.PUBLIC)) {
                                hasPublicConstructor = true;
                                nonPublicConstructors.clear(); // ignore all non-public constructors
                                if (isRootResource) {
                                    int numParams = method.getParameterList().getParametersCount();
                                    if (numParams > maxParams) {
                                        maxParams = numParams;
                                    }
                                    constructorParamsMap.put(method, numParams);
                                }
                            } else if (!hasPublicConstructor) {
                                nonPublicConstructors.add(method);
                            }
                        }
                    }
                    // no public constructor defined
                    if (nonPublicConstructors.size() > 0) {
                        String diagnosticMessage = isRootResource
                                ? "Root resource classes are instantiated by the JAX-RS runtime and MUST have a public constructor"
                                : "Provider classes are instantiated by the JAX-RS runtime and MUST have a public constructor";
                        for (PsiMethod constructor : nonPublicConstructors) {
                            diagnostics.add(createDiagnostic(constructor, unit, diagnosticMessage,
                                    Jax_RSConstants.DIAGNOSTIC_CODE_NO_PUBLIC_CONSTRUCTORS, null,
                                    DiagnosticSeverity.Error));
                        }
                    }
                    // check public constructors' parameters
                    ArrayList<PsiMethod> equalMaxParamMethods = new ArrayList<PsiMethod>();
                    for (Map.Entry<PsiMethod, Integer> entry : constructorParamsMap.entrySet()) {
                        if (entry.getValue() == maxParams) {
                            equalMaxParamMethods.add(entry.getKey());
                        } else if (entry.getValue() < maxParams) {
                            PsiMethod method = entry.getKey();
                            diagnostics.add(createDiagnostic(method, unit,
                                    "This constructor is unused, as root resource classes will only use the constructor with the most parameters.",
                                    Jax_RSConstants.DIAGNOSTIC_CODE_UNUSED_CONSTRUCTOR, null,
                                    DiagnosticSeverity.Warning));
                        }
                    }
                    if (equalMaxParamMethods.size() > 1) { // more than one
                        for (PsiMethod method : equalMaxParamMethods) {
                            diagnostics.add(createDiagnostic(method, unit,
                                    "Multiple constructors have the same number of parameters, it might be ambiguous which constructor is used.",
                                    Jax_RSConstants.DIAGNOSTIC_CODE_AMBIGUOUS_CONSTRUCTORS, null,
                                    DiagnosticSeverity.Warning));
                        }
                    }
                }
            }
        }
    }
}
