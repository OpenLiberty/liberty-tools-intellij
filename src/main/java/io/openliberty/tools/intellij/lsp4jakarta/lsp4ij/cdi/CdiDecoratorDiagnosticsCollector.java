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
        if (!isMatchedAnnotation(type.getAnnotations(), ManagedBeanConstants.DECORATOR_FQ_NAME)) {
            return;
        }

        List<PsiElement> delegateElements = new ArrayList<>();

        // Fields
        for (PsiField field : type.getFields()) {
            validateDelegate(type, unit, diagnostics, field, field, delegateElements, false);
        }

        // Methods + parameters
        for (PsiMethod method : type.getMethods()) {
            PsiAnnotation[] methodAnnotations = method.getAnnotations();

            // Per CDI spec §3.7 / §8.1.2, @Delegate is only valid on injection points:
            // fields, bean constructor parameters, or initializer method parameters.
            // An initializer method is a non-constructor, non-static, void method
            // annotated with @Inject.
            boolean isConstructor = isConstructorMethod(method);
            boolean isInitializerMethod = !isConstructor
                    && !method.hasModifierProperty(PsiModifier.STATIC)
                    && PsiTypes.voidType().equals(method.getReturnType())
                    && isMatchedAnnotation(methodAnnotations, ManagedBeanConstants.INJECT_FQ_NAME);
            // True when the method is not a valid @Delegate injection-point context.
            // Parameters of such methods still need to be scanned so that any @Delegate
            // annotation on them produces InvalidDelegateOnNonInjectionPoint.
            boolean isNonInjectionPointMethod = !isConstructor && !isInitializerMethod;

            for (PsiParameter parameter : method.getParameterList().getParameters()) {
                validateDelegate(type, unit, diagnostics, method, parameter, delegateElements,
                        isNonInjectionPointMethod, methodAnnotations);
            }
        }

        reportInvalidDelegateCountDiagnostics(type, unit, diagnostics, delegateElements);
    }

    /**
     * Unified delegate processing for fields and parameters.
     *
     * @param owner                    element to report diagnostics on (field or method)
     * @param element                  actual element annotated with @Delegate
     * @param isNonInjectionPointMethod true when the enclosing method is not a valid
     *                                 injection-point context (not a constructor and not an
     *                                 @Inject void non-static method). When true the element
     *                                 is still added to delegateElements but
     *                                 InvalidDelegateOnNonInjectionPoint is reported instead.
     * @param reusableAnnots           optional precomputed annotations (e.g. method annotations)
     */
    private void validateDelegate(PsiClass type, PsiJavaFile unit, List<Diagnostic> diagnostics,
                                  PsiElement owner, PsiElement element, List<PsiElement> delegateElements,
                                  boolean isNonInjectionPointMethod, PsiAnnotation... reusableAnnots) {

        PsiAnnotation[] annotations = (element instanceof PsiModifierListOwner)
                ? ((PsiModifierListOwner) element).getAnnotations()
                : new PsiAnnotation[0];

        if (isMatchedAnnotation(annotations, ManagedBeanConstants.DELEGATE_FQ_NAME)) {
            delegateElements.add(element);
            validateDelegateInjectionPoint(type, unit, diagnostics,
                    owner,
                    isNonInjectionPointMethod,
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
     * Validates that a @Delegate injection point is properly annotated with @Inject,
     * and that the enclosing method (if any) is a valid injection-point context.
     *
     * Two distinct diagnostics are possible:
     * - InvalidDelegateInjectionPoint: @Inject is missing — quickfix "Insert @Inject" is offered.
     * - InvalidDelegateOnNonInjectionPoint: @Inject is present but the method is not a valid
     *   injection-point context (non-void, static). No quickfix — user must fix the method.
     *
     * @param type                     the class containing the delegate injection point
     * @param unit                     the compilation unit
     * @param diagnostics              the list to add diagnostics to
     * @param element                  the element to report the diagnostic on (field or method)
     * @param isNonInjectionPointMethod true when the enclosing method is not a valid injection point
     * @param annotations              annotations to check for @Inject
     */
    private void validateDelegateInjectionPoint(PsiClass type, PsiJavaFile unit, List<Diagnostic> diagnostics,
                                                PsiElement element, boolean isNonInjectionPointMethod,
                                                PsiAnnotation[] annotations) {
        if (!isMatchedAnnotation(annotations, ManagedBeanConstants.INJECT_FQ_NAME)) {
            // @Inject missing — offer quickfix regardless of method kind
            diagnostics.add(createDiagnostic(element, unit,
                    Messages.getMessage("InvalidDelegateInjectionPoint"),
                    ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_DELEGATE_INJECTION_POINT, null,
                    DiagnosticSeverity.Error));
        } else if (isNonInjectionPointMethod) {
            // @Inject present but method is not a valid injection-point context (non-void or static)
            diagnostics.add(createDiagnostic(element, unit,
                    Messages.getMessage("InvalidDelegateOnNonInjectionPoint"),
                    ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_DELEGATE_ON_NON_INJECTION_POINT, null,
                    DiagnosticSeverity.Error));
        }
    }
}

