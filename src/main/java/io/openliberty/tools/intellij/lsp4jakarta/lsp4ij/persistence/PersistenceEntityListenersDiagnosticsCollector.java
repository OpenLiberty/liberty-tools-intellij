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

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence;

import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.DiagnosticsUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.util.PsiUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Diagnostics collector that validates entity listeners registered via the
 * {@code @EntityListeners} annotation (Jakarta Persistence 3.0 §3.5.1).
 *
 * <p>An entity listener must:
 * <ul>
 *   <li>Be a concrete, non-inner, non-anonymous, non-local class.</li>
 *   <li>Declare (or implicitly have) a public no-argument constructor.</li>
 * </ul>
 */
public class PersistenceEntityListenersDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public PersistenceEntityListenersDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return PersistenceConstants.DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit == null) {
            return;
        }
        List<PsiClass> allClasses = new ArrayList<>();
        PsiUtils.collectAllClasses(unit.getClasses(), allClasses);
        for (PsiClass type : allClasses) {
            collectEntityListenersDiagnostics(type, unit, diagnostics);
        }
    }

    /**
     * Collects diagnostics for the {@code @EntityListeners} annotation on the given type.
     *
     * @param type        the type being analyzed
     * @param unit        the compilation unit
     * @param diagnostics the list of diagnostics to populate
     */
    private void collectEntityListenersDiagnostics(PsiClass type, PsiJavaFile unit, List<Diagnostic> diagnostics) {
        for (PsiAnnotation annotation : type.getAnnotations()) {
            if (isMatchedJavaElement(type, annotation.getQualifiedName(), PersistenceConstants.ENTITY_LISTENERS)) {
                validateEntityListenersAnnotation(annotation, unit, diagnostics);
            }
        }
    }

    /**
     * Validates the listener classes listed in a single {@code @EntityListeners} annotation.
     *
     * @param annotation  the {@code @EntityListeners} annotation
     * @param unit        the compilation unit
     * @param diagnostics the list of diagnostics to populate
     */
    private void validateEntityListenersAnnotation(PsiAnnotation annotation,
                                                   PsiJavaFile unit, List<Diagnostic> diagnostics) {
        List<String> nonInstantiableListenerNames = new ArrayList<>();
        List<String> invalidConstructorListenerNames = new ArrayList<>();
        Set<PsiClass> validatedListenerTypes = new HashSet<>();

        PsiAnnotationParameterList paramList = annotation.getParameterList();
        for (PsiNameValuePair pair : paramList.getAttributes()) {
            PsiAnnotationMemberValue value = pair.getValue();
            if (value instanceof PsiArrayInitializerMemberValue arrayValue) {
                for (PsiAnnotationMemberValue element : arrayValue.getInitializers()) {
                    checkListenerValue(element, validatedListenerTypes,
                            nonInstantiableListenerNames, invalidConstructorListenerNames);
                }
            } else if (value != null) {
                checkListenerValue(value, validatedListenerTypes,
                        nonInstantiableListenerNames, invalidConstructorListenerNames);
            }
        }

        if (!nonInstantiableListenerNames.isEmpty()) {
            String classNames = String.join(", ", nonInstantiableListenerNames);
            diagnostics.add(createDiagnostic(annotation, unit,
                    Messages.getMessage("EntityListenerMustBeInstantiable", classNames),
                    PersistenceConstants.DIAGNOSTIC_CODE_INVALID_ENTITY_LISTENER_TYPE,
                    null, DiagnosticSeverity.Error));
        }

        if (!invalidConstructorListenerNames.isEmpty()) {
            String classNames = String.join(", ", invalidConstructorListenerNames);
            diagnostics.add(createDiagnostic(annotation, unit,
                    Messages.getMessage("EntityListenerNoArgConstructor", classNames),
                    PersistenceConstants.DIAGNOSTIC_CODE_INVALID_CONSTRUCTOR_IN_ENTITY_LISTENER,
                    null, DiagnosticSeverity.Error));
        }
    }

    /**
     * Resolves the {@link PsiClass} referenced by an annotation value element and
     * validates it as an entity listener candidate.
     *
     * @param value                           annotation member value (expected to be a class literal)
     * @param validatedListenerTypes          set of already-validated listener types (deduplication)
     * @param nonInstantiableListenerNames    accumulator for non-instantiable type names
     * @param invalidConstructorListenerNames accumulator for names of listeners missing a public no-arg constructor
     */
    private void checkListenerValue(PsiAnnotationMemberValue value,
                                    Set<PsiClass> validatedListenerTypes,
                                    List<String> nonInstantiableListenerNames,
                                    List<String> invalidConstructorListenerNames) {
        if (!(value instanceof PsiClassObjectAccessExpression classObjectAccess)) {
            return;
        }
        PsiType referencedType = classObjectAccess.getOperand().getType();
        if (!(referencedType instanceof PsiClassType classType)) {
            return;
        }
        PsiClass listenerClass = classType.resolve();
        if (listenerClass == null || validatedListenerTypes.contains(listenerClass)) {
            return;
        }
        validatedListenerTypes.add(listenerClass);

        if (isNonInstantiableType(listenerClass)) {
            nonInstantiableListenerNames.add(listenerClass.getName());
        } else if (isMissingPublicNoArgsConstructor(listenerClass)) {
            invalidConstructorListenerNames.add(listenerClass.getName());
        }
    }

    /**
     * Returns {@code true} if the given class is non-instantiable as an entity listener:
     * abstract, an interface, or a non-static inner class.
     *
     * @param listenerClass the class to check
     * @return {@code true} if non-instantiable
     */
    private boolean isNonInstantiableType(PsiClass listenerClass) {
        if (listenerClass.isInterface()) {
            return true;
        }
        if (listenerClass.hasModifierProperty(PsiModifier.ABSTRACT)) {
            return true;
        }
        // Non-static inner class: has a containing class but is not declared static.
        PsiClass containingClass = listenerClass.getContainingClass();
        if (containingClass != null && !listenerClass.hasModifierProperty(PsiModifier.STATIC)) {
            return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if the entity listener class declares constructors but none
     * of them is a public no-argument constructor.
     *
     * <p>If the class declares no constructors at all the compiler provides a default
     * public no-argument constructor, so this method returns {@code false} in that case.
     *
     * @param listenerClass the class to check
     * @return {@code true} if the class is missing a public no-arg constructor
     */
    private boolean isMissingPublicNoArgsConstructor(PsiClass listenerClass) {
        PsiMethod[] constructors = listenerClass.getConstructors();
        if (constructors.length == 0) {
            // Implicit default public constructor provided by the compiler.
            return false;
        }
        for (PsiMethod constructor : constructors) {
            if (constructor.getParameterList().getParametersCount() == 0
                    && DiagnosticsUtils.isPublic(constructor)) {
                // Found a valid public no-arg constructor.
                return false;
            }
        }
        // Has constructors but none is a public no-arg constructor.
        return true;
    }
}
