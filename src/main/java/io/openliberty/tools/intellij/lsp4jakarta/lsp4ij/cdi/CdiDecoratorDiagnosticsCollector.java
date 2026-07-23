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

import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

/**
 * CDI diagnostics collector that validates decorator delegate injection points.
 *
 * <p>For classes annotated with {@code @Decorator}: validates that exactly one injection
 * point is annotated with {@code @Delegate}, that it is also annotated with {@code @Inject},
 * and that its type implements all decorated types.
 *
 * <p>For all other classes: reports an error on any injection point annotated with
 * {@code @Delegate}, per CDI 3.0 spec §8.1.3.
 *
 * @see <a href="https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0#delegate_attribute">CDI 3.0 §8.1.3</a>
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
            if (isMatchedAnnotation(type.getAnnotations(), ManagedBeanConstants.DECORATOR_FQ_NAME)) {
                validateDecorator(type, unit, diagnostics);
            } else {
                validateDelegateNotInDecorator(type, unit, diagnostics);
            }
        }
    }

    /**
     * Collects all elements (fields or parameters) that are annotated with {@code @Delegate}.
     *
     * @param elements    the elements to scan
     * @param collected   list to add matching elements into
     */
    private void collectDelegates(PsiModifierListOwner[] elements, List<PsiModifierListOwner> collected) {
        for (PsiModifierListOwner element : elements) {
            if (isMatchedAnnotation(element.getAnnotations(), ManagedBeanConstants.DELEGATE_FQ_NAME)) {
                collected.add(element);
            }
        }
    }

    /**
     * Validates a class annotated with {@code @Decorator}:
     * <ul>
     *   <li>Must declare exactly one {@code @Delegate} injection point.</li>
     *   <li>That injection point must also be annotated with {@code @Inject}.</li>
     *   <li>The delegate type must implement or extend all decorated types.</li>
     * </ul>
     */
    private void validateDecorator(PsiClass type, PsiJavaFile unit, List<Diagnostic> diagnostics) {
        List<PsiModifierListOwner> delegateElements = new ArrayList<>();

        collectDelegates(type.getFields(), delegateElements);
        for (PsiMethod method : type.getMethods()) {
            collectDelegates(method.getParameterList().getParameters(), delegateElements);
        }

        reportInvalidDelegateCountDiagnostics(type, unit, diagnostics, delegateElements);

        if (delegateElements.size() == 1) {
            PsiModifierListOwner delegate = delegateElements.get(0);
            validateDelegateInjectionPoint(type, unit, diagnostics, delegate);
            validateDelegateTypeAssignability(type, delegate, unit, diagnostics);
        }
    }

    /**
     * Reports diagnostics when a decorator has an invalid number of {@code @Delegate}
     * injection points (zero or more than one).
     */
    private void reportInvalidDelegateCountDiagnostics(PsiClass type, PsiJavaFile unit,
                                                       List<Diagnostic> diagnostics,
                                                       List<PsiModifierListOwner> delegateElements) {
        int delegateCount = delegateElements.size();
        if (delegateCount == 0) {
            diagnostics.add(createDiagnostic(type, unit,
                    Messages.getMessage("MissingDelegateInDecorator"),
                    ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_DECORATOR_DELEGATE, null,
                    DiagnosticSeverity.Error));
        } else if (delegateCount > 1) {
            String message = Messages.getMessage("DecoratorWithMultipleDelegates", delegateCount);
            for (PsiModifierListOwner delegateElement : delegateElements) {
                diagnostics.add(createDiagnostic(delegateElement, unit, message,
                        ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_DECORATOR_DELEGATE, null,
                        DiagnosticSeverity.Error));
            }
        }
    }

    /**
     * Validates that a {@code @Delegate} injection point inside a decorator is also
     * annotated with {@code @Inject}.
     *
     * <p>For a field the field's own annotations are checked; for a parameter the
     * enclosing method's annotations are checked (per CDI initializer-method semantics).
     */
    private void validateDelegateInjectionPoint(PsiClass type, PsiJavaFile unit,
                                                List<Diagnostic> diagnostics,
                                                PsiModifierListOwner element) {
        PsiAnnotation[] injectAnnotations;
        PsiElement reportTarget;
        if (element instanceof PsiParameter) {
            PsiMethod method = (PsiMethod) element.getParent().getParent();
            injectAnnotations = method.getAnnotations();
            reportTarget = method;
        } else {
            injectAnnotations = element.getAnnotations();
            reportTarget = element;
        }

        if (!isMatchedAnnotation(injectAnnotations, ManagedBeanConstants.INJECT_FQ_NAME)) {
            diagnostics.add(createDiagnostic(reportTarget, unit,
                    Messages.getMessage("InvalidDelegateInjectionPoint"),
                    ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_DELEGATE_INJECTION_POINT, null,
                    DiagnosticSeverity.Error));
        }
    }

    /**
     * Validates that the delegate type implements or extends all decorated types of the
     * decorator (CDI 3.0 spec §8.1.3).
     */
    private void validateDelegateTypeAssignability(PsiClass decoratorType, PsiElement delegateElement,
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

            PsiClass delegateClass = null;
            if (delegateType instanceof PsiClassType) {
                delegateClass = ((PsiClassType) delegateType).resolve();
            }
            if (delegateClass == null) {
                return;
            }

            List<String> decoratedTypes = getDecoratedTypes(decoratorType);
            if (decoratedTypes.isEmpty()) {
                return;
            }

            List<String> missingTypes = new ArrayList<>();
            for (String decoratedTypeFQN : decoratedTypes) {
                if (!InheritanceUtil.isInheritor(delegateClass, decoratedTypeFQN)) {
                    missingTypes.add(decoratedTypeFQN);
                }
            }

            if (!missingTypes.isEmpty()) {
                diagnostics.add(createDiagnostic(delegateElement, unit,
                        Messages.getMessage("InvalidDecoratorDelegateTypeAssignability", delegateClass.getName()),
                        ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_DECORATOR_DELEGATE_TYPE_ASSIGNABILITY,
                        null, DiagnosticSeverity.Error));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Exception during delegate type assignability validation", e);
        }
    }

    /**
     * Returns all decorated types of a decorator (interfaces it implements and superclasses,
     * excluding {@code java.lang.Object}).
     */
    private List<String> getDecoratedTypes(PsiClass decoratorType) {
        List<String> decoratedTypes = new ArrayList<>();

        for (PsiClass iface : decoratorType.getInterfaces()) {
            if (iface != null) {
                String fqName = iface.getQualifiedName();
                if (fqName != null) {
                    decoratedTypes.add(fqName);
                }
            }
        }

        PsiClass superClass = decoratorType.getSuperClass();
        if (superClass != null) {
            String fqName = superClass.getQualifiedName();
            if (fqName != null && !fqName.equals("java.lang.Object")) {
                decoratedTypes.add(fqName);
            }
        }

        return decoratedTypes;
    }

    /**
     * Validates that a non-decorator class has no injection points annotated with
     * {@code @Delegate}, per CDI 3.0 spec §8.1.3.
     */
    private void validateDelegateNotInDecorator(PsiClass type, PsiJavaFile unit, List<Diagnostic> diagnostics) {
        List<PsiModifierListOwner> delegateElements = new ArrayList<>();

        collectDelegates(type.getFields(), delegateElements);
        for (PsiMethod method : type.getMethods()) {
            collectDelegates(method.getParameterList().getParameters(), delegateElements);
        }

        for (PsiModifierListOwner element : delegateElements) {
            diagnostics.add(createDiagnostic(element, unit,
                    Messages.getMessage("DelegateMustBeInDecorator"),
                    ManagedBeanConstants.DIAGNOSTIC_CODE_DELEGATE_MUST_BE_IN_DECORATOR, null,
                    DiagnosticSeverity.Error));
        }
    }
}
