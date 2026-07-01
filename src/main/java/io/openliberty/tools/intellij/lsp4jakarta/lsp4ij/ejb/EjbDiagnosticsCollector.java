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

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb;

import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.List;
import java.util.stream.Stream;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb.EjbConstants.*;

/**
 * EJB diagnostic collector for session beans.
 */
public class EjbDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public EjbDiagnosticsCollector() {
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
            String[] typeAnnotations = getAnnotationNames(type);
            List<String> sessionBeanAnnotations = getMatchedJavaElementNames(type,
                    typeAnnotations,
                    SESSION_BEAN_ANNOTATIONS);

            if (!sessionBeanAnnotations.isEmpty()) {
                validateSessionBeanInterceptorDecorator(type, typeAnnotations, unit, diagnostics);
                validateSessionBeanConstructor(type, unit, diagnostics);
            }
        }
    }

    /**
     * Extracts the qualified names of all annotations on a class.
     *
     * @param type the class to extract annotation names from
     * @return array of fully qualified annotation names
     */
    private String[] getAnnotationNames(PsiClass type) {
        return Stream.of(type.getAnnotations())
                .map(annotation -> annotation.getQualifiedName())
                .toArray(String[]::new);
    }

    /**
     * Validates that a session bean does not have @Interceptor or @Decorator annotations.
     *
     * A diagnostic is reported if the session bean class is annotated with
     * @Interceptor or @Decorator, as these annotations are not allowed on session beans.
     *
     * @param type the class to validate
     * @param typeAnnotations the annotation names already extracted from the type
     * @param unit the compilation unit
     * @param diagnostics the list to add diagnostics to
     */
    private void validateSessionBeanInterceptorDecorator(PsiClass type, String[] typeAnnotations,
                                                         PsiJavaFile unit, List<Diagnostic> diagnostics) {
        List<String> invalidAnnotations = getMatchedJavaElementNames(type,
                typeAnnotations,
                new String[] {
                        INTERCEPTOR_FQ_NAME,
                        DECORATOR_FQ_NAME
                });

        if (!invalidAnnotations.isEmpty()) {
            diagnostics.add(createDiagnostic(type, unit,
                    Messages.getMessage("SessionBeanWithInterceptorOrDecorator"),
                    DIAGNOSTIC_CODE_SESSION_BEAN_INTERCEPTOR_DECORATOR,
                    null,
                    DiagnosticSeverity.Error));
        }
    }

    /**
     * Validates that a session bean has a public no-arg constructor.
     *
     * A diagnostic is reported if:
     * - The class has explicit constructors AND
     * - None of them are public no-arg constructors
     *
     * If the class has no explicit constructors, Java provides a default
     * public no-arg constructor, so no diagnostic is needed.
     *
     * @param type the class to validate
     * @param unit the compilation unit
     * @param diagnostics the list to add diagnostics to
     */
    private void validateSessionBeanConstructor(PsiClass type, PsiJavaFile unit, List<Diagnostic> diagnostics) {
        PsiMethod[] constructors = type.getConstructors();

        // If no explicit constructors, Java provides a default public no-arg constructor - no diagnostic needed
        if (constructors.length == 0) {
            return;
        }

        // Check if any constructor is public with no arguments
        boolean hasPublicNoArgConstructor = false;

        for (PsiMethod constructor : constructors) {
            if (constructor.hasModifierProperty(PsiModifier.PUBLIC) &&
                    constructor.getParameterList().getParametersCount() == 0) {
                hasPublicNoArgConstructor = true;
                break;
            }
        }

        // Report diagnostic only if there are explicit constructors but none are public no-arg
        if (!hasPublicNoArgConstructor) {
            diagnostics.add(createDiagnostic(type, unit,
                    Messages.getMessage("SessionBeanNoArgConstructor"),
                    DIAGNOSTIC_CODE_MISSING_PUBLIC_CONSTRUCTOR,
                    null,
                    DiagnosticSeverity.Error));
        }
    }
}