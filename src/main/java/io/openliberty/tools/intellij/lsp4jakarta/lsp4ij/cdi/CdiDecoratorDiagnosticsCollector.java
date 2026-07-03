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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

/**
 * CDI diagnostics collector that validates decorator delegate injection points.
 */
public class CdiDecoratorDiagnosticsCollector extends AbstractDiagnosticsCollector {

    @Override
    protected String getDiagnosticSource() {
        return ManagedBeanConstants.DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit == null) {
            return;
        }

        for (PsiClass type : unit.getClasses()) {
            validateDecorator(type, unit, diagnostics);
        }
    }

    /**
     * Validates that a decorator class declares exactly one @Delegate injection point.
     *
     * @param type       the class being validated
     * @param unit       the compilation unit
     * @param diagnostics list to collect diagnostics
     */
    private void validateDecorator(PsiClass type, PsiJavaFile unit, List<Diagnostic> diagnostics) {
        String[] typeAnnotations = Arrays.stream(type.getAnnotations())
                .map(PsiAnnotation::getQualifiedName)
                .toArray(String[]::new);

        if (getMatchedJavaElementNames(type, typeAnnotations,
                new String[]{ManagedBeanConstants.DECORATOR_FQ_NAME}).isEmpty()) {
            return;
        }

        List<PsiElement> delegateElements = new ArrayList<>();

        // Fields
        for (PsiField field : type.getFields()) {
            validateDelegate(type, unit, diagnostics, field, field, delegateElements);
        }

        // Methods + parameters
        for (PsiMethod method : type.getMethods()) {
            String[] methodAnnotations = Arrays.stream(method.getAnnotations())
                    .map(PsiAnnotation::getQualifiedName)
                    .toArray(String[]::new);

            for (PsiParameter parameter : method.getParameterList().getParameters()) {
                validateDelegate(type, unit, diagnostics, method, parameter, delegateElements, methodAnnotations);
            }
        }

        reportInvalidDelegateCountDiagnostics(type, unit, diagnostics, delegateElements);
    }

    /**
     * Unified delegate processing for fields and parameters.
     *
     * @param owner          element to report diagnostics on (field or method)
     * @param element        actual element annotated with @Delegate
     * @param reusableAnnots optional precomputed annotations (e.g. method annotations)
     */
    private void validateDelegate(PsiClass type, PsiJavaFile unit, List<Diagnostic> diagnostics,
                                 PsiElement owner, PsiElement element, List<PsiElement> delegateElements,
                                 String... reusableAnnots) {

        String[] annotations = (element instanceof PsiModifierListOwner)
                ? Arrays.stream(((PsiModifierListOwner) element).getAnnotations())
                .map(PsiAnnotation::getQualifiedName)
                .toArray(String[]::new)
                : new String[0];

        if (!getMatchedJavaElementNames(type, annotations,
                new String[]{ManagedBeanConstants.DELEGATE_FQ_NAME}).isEmpty()) {
            delegateElements.add(element);
            validateDelegateInjectionPoint(type, unit, diagnostics,
                    owner,
                    reusableAnnots.length > 0 ? reusableAnnots : annotations);
        }
    }

    /**
     * reportInvalidDelegateCountDiagnostics
     * Reports diagnostics when a decorator has an invalid number of @Delegate injection points.
     *
     * @param type
     * @param unit
     * @param diagnostics
     * @param delegateElements
     */
    private void reportInvalidDelegateCountDiagnostics(PsiClass type, PsiJavaFile unit, List<Diagnostic> diagnostics, List<PsiElement> delegateElements) {
        int delegateCount = delegateElements.size();
        if (delegateCount == 0) {
            diagnostics.add(createDiagnostic(type, unit,
                    Messages.getMessage("MissingDelegateInDecorator"),
                    ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_DECORATOR_DELEGATE, null,
                    DiagnosticSeverity.Error));
        } else if(delegateCount > 1) {
            String message = Messages.getMessage("DecoratorWithMultipleDelegates", delegateCount);
            for (PsiElement delegateElement : delegateElements) {
                diagnostics.add(createDiagnostic(delegateElement, unit, message,
                        ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_DECORATOR_DELEGATE, null,
                        DiagnosticSeverity.Error));
            }
        }
    }

    /**
     * Validates that a @Delegate injection point is properly annotated with @Inject.
     *
     * According to CDI specification, @Delegate must be applied to an injected field,
     * or to a parameter of an initializer or constructor.
     *
     * @param type the class containing the delegate injection point
     * @param unit the compilation unit
     * @param diagnostics the list to add diagnostics to
     * @param element the element annotated with @Delegate (field or parameter)
     * @param annotations the annotations to check for @Inject (field annotations for fields, method annotations for parameters)
     */
    private void validateDelegateInjectionPoint(PsiClass type, PsiJavaFile unit, List<Diagnostic> diagnostics,
                                                PsiElement element, String[] annotations) {
        // Check if @Inject annotation is present
        if (getMatchedJavaElementNames(type, annotations, new String[] { ManagedBeanConstants.INJECT_FQ_NAME }).isEmpty()) {
            // If @Inject is not present, report a diagnostic
            diagnostics.add(createDiagnostic(element, unit,
                    Messages.getMessage("InvalidDelegateInjectionPoint"),
                    ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_DELEGATE_INJECTION_POINT, null,
                    DiagnosticSeverity.Error));
        }
    }
}

