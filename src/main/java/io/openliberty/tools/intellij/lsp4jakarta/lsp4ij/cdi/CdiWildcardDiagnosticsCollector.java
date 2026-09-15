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
import java.util.stream.Stream;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.*;

/**
 * Diagnostics collector for CDI producer type-variable and wildcard rules.
 *
 * <p>Rules enforced (CDI 3.0 spec sections 2.2.1, 3.2, and 3.3):
 * <ul>
 * <li>A parameterized type containing a wildcard is not a legal bean type.</li>
 * <li>A producer method/field whose type is a bare type variable (e.g. {@code T}) or
 * an array of one (e.g. {@code T[]}) is always a definition error.</li>
 * <li>A producer method/field whose type is a parameterized type with a type variable
 * (e.g. {@code List<T>}) must declare scope {@code @Dependent}.</li>
 * </ul>
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

        String[] scopeFQNames = SCOPE_FQ_NAMES.toArray(String[]::new);

        for (PsiClass type : unit.getClasses()) {
            // Whether the class declares any type parameters at all (e.g. class Foo<T>).
            // Used as a fast-exit guard before the costlier type-variable checks.
            boolean isGeneric = type.getTypeParameters().length > 0;

            for (PsiField field : type.getFields()) {
                if (AnnotationUtils.hasAnnotation(field, INJECT_FQ_NAME)) {
                    if (containsWildcard(field.getType())) {
                        diagnostics.add(createDiagnostic(field, unit,
                                Messages.getMessage(DIAGNOSTIC_CODE_WILDCARD_INJECT),
                                DIAGNOSTIC_CODE_WILDCARD_INJECT, null, DiagnosticSeverity.Error));
                    }
                } else if (AnnotationUtils.hasAnnotation(field, PRODUCES_FQ_NAME)) {
                    String[] annotationNames = Stream.of(field.getAnnotations())
                            .map(PsiAnnotation::getQualifiedName).toArray(String[]::new);
                    checkProducerMember(type, field, field.getType(), unit, diagnostics,
                            annotationNames, isGeneric, scopeFQNames,
                            new String[]{ DIAGNOSTIC_CODE_WILDCARD_PRODUCER_FIELD,
                                          DIAGNOSTIC_CODE_PRODUCER_FIELD_BARE_TYPE_VAR,
                                          DIAGNOSTIC_CODE_PRODUCER_FIELD_TYPE_VAR_NON_DEPENDENT });
                }
            }

            for (PsiMethod method : type.getMethods()) {
                if (AnnotationUtils.hasAnnotation(method, INJECT_FQ_NAME)) {
                    for (PsiParameter param : method.getParameterList().getParameters()) {
                        if (containsWildcard(param.getType())) {
                            diagnostics.add(createDiagnostic(param, unit,
                                    Messages.getMessage(DIAGNOSTIC_CODE_WILDCARD_INJECT_METHOD),
                                    DIAGNOSTIC_CODE_WILDCARD_INJECT_METHOD, null, DiagnosticSeverity.Error));
                        }
                    }
                } else if (AnnotationUtils.hasAnnotation(method, PRODUCES_FQ_NAME)) {
                    PsiType returnType = method.getReturnType();
                    if (returnType == null) continue;
                    String[] annotationNames = Stream.of(method.getAnnotations())
                            .map(PsiAnnotation::getQualifiedName).toArray(String[]::new);
                    checkProducerMember(type, method, returnType, unit, diagnostics,
                            annotationNames, isGeneric, scopeFQNames,
                            new String[]{ DIAGNOSTIC_CODE_WILDCARD_PRODUCER_METHOD,
                                          DIAGNOSTIC_CODE_PRODUCER_METHOD_BARE_TYPE_VAR,
                                          DIAGNOSTIC_CODE_PRODUCER_METHOD_TYPE_VAR_NON_DEPENDENT });
                }
            }
        }
    }

    /**
     * Applies the three CDI type-variable rules for a single {@code @Produces} member.
     *
     * <p>{@code codes[0]} — wildcard in type (always invalid)<br>
     * {@code codes[1]} — bare type variable or array of one (always invalid)<br>
     * {@code codes[2]} — parameterized type with type variable and non-{@code @Dependent} scope;
     * the message key for each is the code value itself (matches messages.properties keys)
     */
    private void checkProducerMember(PsiClass type, PsiElement element, PsiType psiType,
                                     PsiJavaFile unit, List<Diagnostic> diagnostics,
                                     String[] annotationNames, boolean isGeneric,
                                     String[] scopeFQNames, String[] codes) {
        // Rule 0: wildcard in type — always a definition error
        if (containsWildcard(psiType)) {
            diagnostics.add(createDiagnostic(element, unit, Messages.getMessage(codes[0]),
                    codes[0], null, DiagnosticSeverity.Error));
        }

        if (!isGeneric) {
            return;
        }

        // Rule 1: bare type variable (T or T[]) — always a definition error
        if (isBareTypeVariable(psiType)) {
            diagnostics.add(createDiagnostic(element, unit, Messages.getMessage(codes[1]),
                    codes[1], null, DiagnosticSeverity.Error));
        }
        // Rule 2: parameterized type with type variable — requires @Dependent scope
        else if (containsTypeVariable(psiType)) {
            boolean hasNonDependentScope = getMatchedJavaElementNames(type, annotationNames, scopeFQNames)
                    .stream().anyMatch(s -> !DEPENDENT_FQ_NAME.equals(s));
            if (hasNonDependentScope) {
                diagnostics.add(createDiagnostic(element, unit, Messages.getMessage(codes[2]),
                        codes[2], null, DiagnosticSeverity.Error));
            }
        }
    }

    /**
     * Returns {@code true} if {@code psiType} is a bare type variable (e.g. {@code T}) or
     * an array whose ultimate element type is a type variable (e.g. {@code T[]}).
     *
     * <p>PSI resolves type arguments fully, so a bare type parameter always resolves to a
     * {@link PsiTypeParameter} — no name comparison needed.
     *
     * <p>Examples (assuming the enclosing class declares {@code <T>}):
     * <ul>
     * <li>{@code T} (bare type variable) — returns {@code true}</li>
     * <li>{@code T[]} (array of type variable) — returns {@code true}</li>
     * <li>{@code List<T>} (parameterized, not bare) — returns {@code false}</li>
     * <li>{@code String} (concrete type) — returns {@code false}</li>
     * </ul>
     */
    private boolean isBareTypeVariable(PsiType psiType) {
        if (psiType instanceof PsiClassType) {
            return ((PsiClassType) psiType).resolve() instanceof PsiTypeParameter;
        }
        if (psiType instanceof PsiArrayType) {
            return isBareTypeVariable(((PsiArrayType) psiType).getComponentType());
        }
        return false;
    }

    /**
     * Returns {@code true} if {@code psiType} is a parameterized type that contains at
     * least one type variable in its type arguments (recursively).
     *
     * <p>Examples (assuming the enclosing class declares {@code <T>}):
     * <ul>
     * <li>{@code List<T>} — returns {@code true}</li>
     * <li>{@code Map<String, T>} — returns {@code true}</li>
     * <li>{@code List<Set<T>>} (nested) — returns {@code true}</li>
     * <li>{@code T} (bare type variable, not parameterized) — returns {@code false}</li>
     * <li>{@code List<String>} (concrete type argument) — returns {@code false}</li>
     * </ul>
     */
    private boolean containsTypeVariable(PsiType psiType) {
        if (psiType instanceof PsiClassType) {
            for (PsiType typeArg : ((PsiClassType) psiType).getParameters()) {
                if (isBareTypeVariable(typeArg) || containsTypeVariable(typeArg)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if {@code type} contains a wildcard ({@code ?},
     * {@code ? extends}, or {@code ? super}) anywhere in the type tree.
     *
     * <p>Examples:
     * <ul>
     * <li>{@code List<?>} — returns {@code true}</li>
     * <li>{@code List<? extends Number>} — returns {@code true}</li>
     * <li>{@code List<? super Number>} — returns {@code true}</li>
     * <li>{@code Map<String, ? extends Number>} — returns {@code true}</li>
     * <li>{@code List<?>[] } (array of {@code List<?>}) — returns {@code true}</li>
     * <li>{@code List<String>} — returns {@code false}</li>
     * </ul>
     */
    private boolean containsWildcard(PsiType type) {
        if (type instanceof PsiWildcardType) {
            return true;
        }
        if (type instanceof PsiClassType) {
            for (PsiType param : ((PsiClassType) type).getParameters()) {
                if (containsWildcard(param)) {
                    return true;
                }
            }
        }
        if (type instanceof PsiArrayType) {
            return containsWildcard(((PsiArrayType) type).getComponentType());
        }
        if (type instanceof PsiIntersectionType) {
            for (PsiType conjunct : ((PsiIntersectionType) type).getConjuncts()) {
                if (containsWildcard(conjunct)) {
                    return true;
                }
            }
        }
        return false;
    }
}
