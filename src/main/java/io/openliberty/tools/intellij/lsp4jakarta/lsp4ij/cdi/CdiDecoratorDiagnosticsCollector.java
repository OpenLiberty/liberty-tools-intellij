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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils.getSimpleName;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

/**
 * CDI diagnostics collector that validates decorator delegate injection points.
 */
public class CdiDecoratorDiagnosticsCollector extends AbstractDiagnosticsCollector {

    private static final Logger LOGGER = Logger.getLogger(CdiDecoratorDiagnosticsCollector.class.getName());

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
            validateDelegate(unit, diagnostics, field, field, delegateElements, false);
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
                validateDelegate(unit, diagnostics, method, parameter, delegateElements,
                        isNonInjectionPointMethod, methodAnnotations);
            }
        }

        reportInvalidDelegateCountDiagnostics(type, unit, diagnostics, delegateElements);

        // Validate delegate type assignability (Section 8.1.3 of CDI spec)
        if (delegateElements.size() == 1) {
            validateDelegateTypeAssignability(type, delegateElements.getFirst(), unit, diagnostics);
        }
    }

    /**
     * Unified delegate processing for fields and parameters.
     *
     * @param unit                      the compilation unit
     * @param diagnostics               the list to collect diagnostics
     * @param owner                     element to report diagnostics on (field or method)
     * @param element                   actual element annotated with @Delegate
     * @param delegateElements          list to collect delegate elements
     * @param isNonInjectionPointMethod true when the enclosing method is not a valid
     *                                  injection-point context (not a constructor and not an
     *                                  @Inject void non-static method). When true the element
     *                                  is still added to delegateElements but
     *                                  InvalidDelegateOnNonInjectionPoint is reported instead.
     * @param reusableAnnots                    optional precomputed annotations (e.g. method annotations)
     */
    private void validateDelegate(PsiJavaFile unit, List<Diagnostic> diagnostics,
                                  PsiElement owner, PsiElement element, List<PsiElement> delegateElements,
                                  boolean isNonInjectionPointMethod, PsiAnnotation... reusableAnnots) {

        PsiAnnotation[] annotations = (element instanceof PsiModifierListOwner)
                ? ((PsiModifierListOwner) element).getAnnotations()
                : new PsiAnnotation[0];

        if (isMatchedAnnotation(annotations, ManagedBeanConstants.DELEGATE_FQ_NAME)) {
            delegateElements.add(element);
            validateDelegateInjectionPoint(unit, diagnostics,
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
     * Validates that the delegate type implements or extends all decorated types of the decorator.
     * <p>
     * Per CDI 3.0 specification section 8.1.3:
     * "The delegate type of a decorator must implement or extend every decorated type
     * (with exactly the same type parameters). If the delegate type does not implement
     * or extend a decorated type of the decorator (or specifies different type parameters),
     * the container automatically detects the problem and treats it as a definition error."
     *
     * @param decoratorClass   the decorator class
     * @param delegateElement the delegate injection point (field or parameter)
     * @param unit            the compilation unit
     * @param diagnostics     the list to add diagnostics to
     */
    private void validateDelegateTypeAssignability(PsiClass decoratorClass, PsiElement delegateElement,
                                                   PsiJavaFile unit, List<Diagnostic> diagnostics) {
        try {
            PsiType delegateType = null;
            if (delegateElement instanceof PsiField) {
                delegateType = ((PsiField) delegateElement).getType();
            } else if (delegateElement instanceof PsiParameter) {
                delegateType = ((PsiParameter) delegateElement).getType();
            }
            if (delegateType == null) {
                return;
            }
            // Primitives are never valid bean types — report immediately without further resolution.
            if (delegateType instanceof PsiPrimitiveType) {
                reportDecoratorDiagnostic(delegateElement, unit, ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_DECORATOR_DELEGATE_TYPE_ASSIGNABILITY,
                        delegateType.getPresentableText(), "",
                        ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_DECORATOR_DELEGATE_TYPE_ASSIGNABILITY,
                        diagnostics);
                return;
            }
            // Resolve the delegate type to a PsiClass
            PsiClass delegateClass = null;
            if (delegateType instanceof PsiClassType) {
                delegateClass = ((PsiClassType) delegateType).resolve();
            }
            if (delegateClass == null) {
                return; // Cannot resolve delegate type, skip validation
            }
            // Get all decorated types (interfaces and superclasses of the decorator)
            List<String> decoratedTypes = getDecoratedTypes(decoratorClass);
            if (decoratedTypes.isEmpty()) {
                // Decorator has no decorated types — definition error per CDI 3.0 §8.1.3
                reportDecoratorDiagnostic(delegateElement, unit, ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_DECORATOR_WITH_NO_DECORATED_TYPES,
                        null, null, ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_DECORATOR_WITH_NO_DECORATED_TYPES,
                        diagnostics);
                return;
            }
            // Check if delegate type implements/extends all decorated types
            for (String decoratedTypeFQN : decoratedTypes) {
                if (!InheritanceUtil.isInheritor(delegateClass, decoratedTypeFQN)) {
                    reportDecoratorDiagnostic(delegateElement, unit, ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_DECORATOR_DELEGATE_TYPE_ASSIGNABILITY,
                            delegateClass.getName(), getSimpleName(decoratedTypeFQN),
                            ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_DECORATOR_DELEGATE_TYPE_ASSIGNABILITY,
                            diagnostics);
                    return;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Exception during delegate type assignability validation", e);
        }
    }

    /**
     * Reports a decorator validation diagnostic on the given delegate element.
     *
     * @param delegateElement  the delegate injection point (field or parameter)
     * @param unit             the compilation unit
     * @param messageKey       the message key (e.g., "InvalidDecoratorDelegateTypeAssignability")
     * @param delegateTypeName the simple name of the delegate type (may be null)
     * @param decoratedTypeName the simple name of the decorated type (may be null or empty)
     * @param diagnostics      the list to add the diagnostic to
     */
    private void reportDecoratorDiagnostic(PsiElement delegateElement, PsiJavaFile unit,
                                           String messageKey, String delegateTypeName, String decoratedTypeName,
                                           String errorCode, List<Diagnostic> diagnostics) {
        String message = Messages.getMessage(messageKey, delegateTypeName, decoratedTypeName);
        diagnostics.add(createDiagnostic(delegateElement, unit, message, errorCode,
                null, DiagnosticSeverity.Error));
    }

    /**
     * Gets all decorated types of the decorator (Java interfaces only, excluding java.io.Serializable).
     * <p>
     * Per CDI 3.0 specification section 8.1.3:
     * "The set of decorated types of a decorator includes all bean types of the managed bean
     * which are Java interfaces, except for java.io.Serializable. The decorator bean class and
     * its superclasses are not decorated types of the decorator."
     *
     * @param decoratorClass the decorator class
     * @return list of decorated type fully qualified names (interfaces only)
     */
    private List<String> getDecoratedTypes(PsiClass decoratorClass) {
        return InheritanceUtil.getSuperClasses(decoratorClass).stream()
                .filter(PsiClass::isInterface)
                .map(PsiClass::getQualifiedName)
                .filter(fqn -> fqn != null && !ManagedBeanConstants.SERIALIZABLE_FQ_NAME.equals(fqn))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Validates that a @Delegate injection point is properly annotated with @Inject.
     * <p>
     * According to CDI specification, @Delegate must be applied to an injected field,
     * or to a parameter of an initializer or constructor.
     *
     * @param unit                      the compilation unit
     * @param diagnostics               the list to add diagnostics to
     * @param element                   the element to report the diagnostic on (field or method)
     * @param isNonInjectionPointMethod true when the enclosing method is not a valid injection point
     * @param annotations               annotations to check for @Inject
     */
    private void validateDelegateInjectionPoint(PsiJavaFile unit, List<Diagnostic> diagnostics,
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

