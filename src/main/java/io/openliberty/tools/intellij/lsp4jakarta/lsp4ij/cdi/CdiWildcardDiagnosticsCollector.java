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

        PsiClass[] types = unit.getClasses();
        for (PsiClass type : types) {
            // Check fields for @Inject and @Produces with wildcard types
            for (PsiField field : type.getFields()) {
                boolean hasInject = hasAnnotation(field, INJECT_FQ_NAME);
                boolean hasProduces = hasAnnotation(field, PRODUCES_FQ_NAME);

                if (hasInject || hasProduces) {
                    PsiType fieldType = field.getType();
                    if (containsWildcard(fieldType)) {
                        String diagnosticCode = hasInject ?
                            DIAGNOSTIC_CODE_WILDCARD_INJECT :
                            DIAGNOSTIC_CODE_WILDCARD_PRODUCER_FIELD;
                        String message = hasInject ?
                            Messages.getMessage("InvalidWildcardTypeInInjectField") :
                            Messages.getMessage("InvalidWildcardTypeInProducerField");
                        
                        diagnostics.add(createDiagnostic(field, unit, message,
                                diagnosticCode, null, DiagnosticSeverity.Error));
                    }
                }
            }

            // Check methods for @Produces with wildcard return types
            for (PsiMethod method : type.getMethods()) {
                boolean hasProduces = hasAnnotation(method, PRODUCES_FQ_NAME);

                if (hasProduces) {
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
     * Checks if a field or method has a specific annotation.
     *
     * @param element the field or method to check
     * @param annotationFqName the fully qualified name of the annotation
     * @return true if the element has the annotation, false otherwise
     */
    private boolean hasAnnotation(PsiModifierListOwner element, String annotationFqName) {
        PsiAnnotation[] annotations = element.getAnnotations();
        return isMatchedAnnotation(annotations, annotationFqName);
    }

    /**
     * Checks if a PsiType contains a wildcard type parameter.
     *
     * @param type the type to check
     * @return true if the type contains a wildcard, false otherwise
     */
    private boolean containsWildcard(PsiType type) {
        if (type == null) {
            return false;
        }

        // Check if the type itself is a wildcard
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
        if (type instanceof PsiArrayType) {
            PsiArrayType arrayType = (PsiArrayType) type;
            return containsWildcard(arrayType.getComponentType());
        }

        return false;
    }
}