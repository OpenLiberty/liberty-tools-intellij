/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation.
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
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.List;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.*;

/**
 * Diagnostics collector for detecting illegal wildcard types in CDI bean types.
 *
 * According to CDI 3.0 specification section 2.2.1 (Legal bean types):
 * "A parameterized type that contains a wildcard type parameter is not a legal bean type."
 *
 * This collector checks:
 * - @Inject fields with wildcard types
 * - @Inject methods with wildcard parameter types
 * - @Produces fields with wildcard types
 * - @Produces methods returning wildcard types
 */
public class CdiWildcardDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public CdiWildcardDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit == null)
            return;

        for (PsiClass type : unit.getClasses()) {
            // Check fields for @Inject and @Produces with wildcard types
            for (PsiField field : type.getFields()) {
                boolean hasInject = AnnotationUtils.hasAnnotation(field, INJECT_FQ_NAME);
                boolean hasProduces = AnnotationUtils.hasAnnotation(field, PRODUCES_FQ_NAME);

                // Use if-else since @Inject and @Produces don't appear on the same field
                if (hasInject) {
                    PsiType fieldType = field.getType();
                    if (containsWildcard(fieldType)) {
                        diagnostics.add(createDiagnostic(field, unit,
                                Messages.getMessage("InvalidWildcardTypeInInjectField"),
                                DIAGNOSTIC_CODE_WILDCARD_INJECT, null,
                                DiagnosticSeverity.Error));
                    }
                } else if (hasProduces) {
                    PsiType fieldType = field.getType();
                    if (containsWildcard(fieldType)) {
                        diagnostics.add(createDiagnostic(field, unit,
                                Messages.getMessage("InvalidWildcardTypeInProducerField"),
                                DIAGNOSTIC_CODE_WILDCARD_PRODUCER_FIELD, null,
                                DiagnosticSeverity.Error));
                    }
                }
            }

            // Check methods for @Inject and @Produces with wildcard types
            for (PsiMethod method : type.getMethods()) {
                boolean hasInject = AnnotationUtils.hasAnnotation(method, INJECT_FQ_NAME);
                boolean hasProduces = AnnotationUtils.hasAnnotation(method, PRODUCES_FQ_NAME);

                // Use if-else since @Inject and @Produces don't appear on the same method
                if (hasInject) {
                    // Check method parameters for wildcard types
                    for (PsiParameter param : method.getParameterList().getParameters()) {
                        PsiType paramType = param.getType();
                        if (containsWildcard(paramType)) {
                            diagnostics.add(createDiagnostic(param, unit,
                                    Messages.getMessage("InvalidWildcardTypeInInjectMethod"),
                                    DIAGNOSTIC_CODE_WILDCARD_INJECT, null,
                                    DiagnosticSeverity.Error));
                        }
                    }
                } else if (hasProduces) {
                    // Check return type for wildcard types
                    PsiType returnType = method.getReturnType();
                    if (returnType != null && containsWildcard(returnType)) {
                        diagnostics.add(createDiagnostic(method, unit,
                                Messages.getMessage("InvalidWildcardTypeInProducerMethod"),
                                DIAGNOSTIC_CODE_WILDCARD_PRODUCER_METHOD, null,
                                DiagnosticSeverity.Error));
                    }
                }
            }
        }
    }

    /**
     * Checks if a PsiType contains a wildcard type parameter.
     *
     * This method recursively checks for wildcards in:
     * - Direct wildcard types (?, ? extends T, ? super T)
     * - Parameterized types with wildcard arguments (List<?>, Map<String, ?>)
     * - Array types with wildcard component types (List<?>[], List<?>[][])
     * - Type variables with multiple bounds (<T extends Number & Comparable>)
     * - Nested generic types (Map<String, List<?>>)
     *
     * @param type the type to check
     * @return true if the type contains a wildcard, false otherwise
     */
    private boolean containsWildcard(PsiType type) {
        if (type == null) {
            return false;
        }

        // Check if the type itself is a wildcard (?, ? extends T, ? super T)
        if (type instanceof PsiWildcardType) {
            return true;
        }

        // Check if it's a class type with type parameters
        if (type instanceof PsiClassType) {
            PsiClassType classType = (PsiClassType) type;
            PsiType[] parameters = classType.getParameters();
            
            // Recursively check each type parameter
            for (PsiType param : parameters) {
                if (containsWildcard(param)) {
                    return true;
                }
            }
        }

        // Check if it's an array type with wildcard component type
        // This handles both single and multi-dimensional arrays (List<?>[], List<?>[][])
        if (type instanceof PsiArrayType) {
            PsiArrayType arrayType = (PsiArrayType) type;
            return containsWildcard(arrayType.getComponentType());
        }

        // Check if it's an intersection type (type variable with multiple bounds)
        // Example: <T extends Number & Comparable>
        if (type instanceof PsiIntersectionType) {
            PsiIntersectionType intersectionType = (PsiIntersectionType) type;
            for (PsiType conjunct : intersectionType.getConjuncts()) {
                if (containsWildcard(conjunct)) {
                    return true;
                }
            }
        }

        return false;
    }
}