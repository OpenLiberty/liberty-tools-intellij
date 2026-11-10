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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.TypeConversionUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils.getSimpleName;
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
     * decorator, and that any type parameters match exactly (CDI 3.0 spec §8.1.3).
     *
     * <p>Two separate diagnostics may be raised:
     * <ul>
     *   <li>{@code InvalidDecoratorDelegateTypeAssignability} — when the raw delegate type
     *       does not implement or extend a decorated type at all.</li>
     *   <li>{@code InvalidDecoratorDelegateTypeParamMismatch} — when the raw types are
     *       compatible but the type arguments differ (e.g., {@code Processor<Object>} vs
     *       the decorated type {@code Processor<String>}).</li>
     * </ul>
     */
    private void validateDelegateTypeAssignability(PsiClass decoratorType, PsiElement delegateElement,
                                                   PsiJavaFile unit, List<Diagnostic> diagnostics) {
        try {
            PsiType delegateType = delegateElement instanceof PsiField
                    ? ((PsiField) delegateElement).getType()
                    : delegateElement instanceof PsiParameter
                            ? ((PsiParameter) delegateElement).getType()
                            : null;
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

            if (!(delegateType instanceof PsiClassType)) {
                return;
            }

            PsiClassType delegateClassType = (PsiClassType) delegateType;
            PsiClass delegateClass = delegateClassType.resolve();
            if (delegateClass == null) {
                return;
            }

            List<PsiClassType> decoratedTypes = getDecoratedClassTypes(decoratorType);
            if (decoratedTypes.isEmpty()) {
                // Decorator has no decorated types — definition error per CDI 3.0 §8.1.3
                reportDecoratorDiagnostic(delegateElement, unit, ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_DECORATOR_WITH_NO_DECORATED_TYPES,
                        null, null, ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_DECORATOR_WITH_NO_DECORATED_TYPES,
                        diagnostics);
                return;
            }

            for (PsiClassType decoratedClassType : decoratedTypes) {
                PsiClass decoratedClass = decoratedClassType.resolve();
                if (decoratedClass == null) {
                    continue;
                }
                String decoratedTypeFQN = decoratedClass.getQualifiedName();
                if (decoratedTypeFQN == null || !InheritanceUtil.isInheritor(delegateClass, decoratedTypeFQN)) {
                    reportDecoratorDiagnostic(delegateElement, unit, ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_DECORATOR_DELEGATE_TYPE_ASSIGNABILITY,
                            delegateClass.getName(), decoratedTypeFQN != null ? getSimpleName(decoratedTypeFQN) : "",
                            ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_DECORATOR_DELEGATE_TYPE_ASSIGNABILITY,
                            diagnostics);
                    return;
                }
                // Raw types are compatible — check type parameters match
                checkTypeParamMismatch(delegateClassType, decoratedClassType, decoratedClass,
                        delegateClass, delegateElement, unit, diagnostics);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Exception during delegate type assignability validation", e);
        }
    }

    /**
     * Reports a decorator validation diagnostic on the given delegate element.
     *
     * @param delegateElement   the delegate injection point (field or parameter)
     * @param unit              the compilation unit
     * @param messageKey        the message key (e.g., "InvalidDecoratorDelegateTypeAssignability")
     * @param delegateTypeName  the simple name of the delegate type (may be null)
     * @param decoratedTypeName the simple name of the decorated type (may be null or empty)
     * @param errorCode         the diagnostic code
     * @param diagnostics       the list to add the diagnostic to
     */
    private void reportDecoratorDiagnostic(PsiElement delegateElement, PsiJavaFile unit,
                                           String messageKey, String delegateTypeName, String decoratedTypeName,
                                           String errorCode, List<Diagnostic> diagnostics) {
        String message = Messages.getMessage(messageKey, delegateTypeName, decoratedTypeName);
        diagnostics.add(createDiagnostic(delegateElement, unit, message, errorCode,
                null, DiagnosticSeverity.Error));
    }

    /**
     * Checks that the type arguments of the delegate's {@code PsiClassType} match those
     * of the corresponding supertype instantiation for the given decorated type.
     *
     * <p>When the delegate class is not the same class as the decorated class (i.e., the
     * delegate is a subtype), the delegate's supertype substitution for the decorated
     * interface is resolved and compared against the decorated type's arguments.
     */
    private void checkTypeParamMismatch(PsiClassType delegateClassType, PsiClassType decoratedClassType,
                                        PsiClass decoratedClass, PsiClass delegateClass,
                                        PsiElement delegateElement, PsiJavaFile unit,
                                        List<Diagnostic> diagnostics) {
        PsiType[] decoratedArgs = decoratedClassType.getParameters();
        if (decoratedArgs.length == 0) {
            return; // Raw/non-generic decorated type — no type param check needed
        }

        // Resolve the substitutor that maps the decorated class's type params to concrete types
        // as seen from the delegate class (e.g., Processor<String> delegating Processor<T>)
        PsiClassType.ClassResolveResult delegateResolve = delegateClassType.resolveGenerics();
        PsiSubstitutor superSubstitutor = TypeConversionUtil.getSuperClassSubstitutor(
                decoratedClass, delegateClass, delegateResolve.getSubstitutor());

        PsiTypeParameter[] decoratedTypeParams = decoratedClass.getTypeParameters();
        List<PsiType> delegateArgs = Arrays.stream(decoratedTypeParams)
                .map(superSubstitutor::substitute)
                .collect(Collectors.toList());

        boolean mismatch = delegateArgs.size() != decoratedArgs.length
                || !delegateArgs.equals(Arrays.asList(decoratedArgs));

        if (mismatch) {
            diagnostics.add(createDiagnostic(delegateElement, unit,
                    Messages.getMessage("InvalidDecoratorDelegateTypeParamMismatch",
                            delegateClassType.getPresentableText(),
                            decoratedClassType.getPresentableText()),
                    ManagedBeanConstants.DIAGNOSTIC_CODE_DELEGATE_TYPE_PARAM_MISMATCH,
                    null, DiagnosticSeverity.Error));
        }
    }

    /**
     * Returns all decorated types of a decorator as {@code PsiClassType} instances
     * (interfaces it implements and superclasses, excluding {@code java.lang.Object}
     * and {@code java.io.Serializable}).
     * Preserving type arguments allows the type-parameter mismatch check.
     */
    private List<PsiClassType> getDecoratedClassTypes(PsiClass decoratorClass) {
        List<PsiClassType> decoratedTypes = new ArrayList<>();

        for (PsiClassType ifaceType : decoratorClass.getImplementsListTypes()) {
            if (ifaceType != null) {
                PsiClass ifaceClass = ifaceType.resolve();
                if (ifaceClass != null) {
                    String fqn = ifaceClass.getQualifiedName();
                    if (fqn != null && !ManagedBeanConstants.SERIALIZABLE_FQ_NAME.equals(fqn)) {
                        decoratedTypes.add(ifaceType);
                    }
                }
            }
        }

        for (PsiClass superClass : InheritanceUtil.getSuperClasses(decoratorClass)) {
            if (superClass.isInterface()) {
                String fqn = superClass.getQualifiedName();
                if (fqn != null && !ManagedBeanConstants.SERIALIZABLE_FQ_NAME.equals(fqn)) {
                    boolean alreadyPresent = decoratedTypes.stream().anyMatch(t -> {
                        PsiClass c = t.resolve();
                        return c != null && fqn.equals(c.getQualifiedName());
                    });
                    if (!alreadyPresent) {
                        JavaPsiFacade facade = JavaPsiFacade.getInstance(decoratorClass.getProject());
                        PsiElementFactory factory = facade.getElementFactory();
                        decoratedTypes.add(factory.createType(superClass));
                    }
                }
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
