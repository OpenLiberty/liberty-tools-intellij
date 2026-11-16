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

import com.google.gson.Gson;
import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.DiagnosticsUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.util.PsiUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb.EjbConstants.*;

/**
 * EJB diagnostic collector for session beans and session synchronization methods.
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

        List<PsiClass> allClasses = new ArrayList<>();
        PsiUtils.collectAllClasses(unit.getClasses(), allClasses);

        for (PsiClass type : allClasses) {
            String[] typeAnnotations = getAnnotationNames(type);
            List<String> sessionBeanAnnotations = getMatchedJavaElementNames(type,
                    typeAnnotations,
                    SESSION_BEAN_ANNOTATIONS);

            if (!sessionBeanAnnotations.isEmpty()) {
                validateSessionBeanClass(type, unit, diagnostics);
                validateSessionBeanInterceptorDecorator(type, typeAnnotations, unit, diagnostics);
                if (sessionBeanAnnotations.size() > 1) {
                    validateConflictingSessionBeanAnnotations(type, unit, sessionBeanAnnotations, diagnostics);
                }
                validateSessionBeanConstructor(type, unit, diagnostics);
                validateSessionBeanFinalizeMethod(type, unit, diagnostics);

                // Validate session synchronization methods (@AfterBegin, @BeforeCompletion, @AfterCompletion)
                for (PsiMethod method : type.getMethods()) {
                    validateSessionSyncMethod(type, method, unit, diagnostics);
                }
            }
        }
    }

    /**
     * Validates that a session bean class is public, not final, not abstract, and top-level.
     *
     * @param type the class to validate
     * @param unit the compilation unit
     * @param diagnostics the list to add diagnostics to
     */
    private void validateSessionBeanClass(PsiClass type, PsiJavaFile unit, List<Diagnostic> diagnostics) {
        // Must be a top-level class
        if (type.getContainingClass() != null) {
            diagnostics.add(createDiagnostic(type, unit,
                    Messages.getMessage("SessionBeanMustBeTopLevel"),
                    DIAGNOSTIC_CODE_NON_TOP_LEVEL_CLASS,
                    null,
                    DiagnosticSeverity.Error));
        }

        // Must be public
        if (!type.hasModifierProperty(PsiModifier.PUBLIC)) {
            diagnostics.add(createDiagnostic(type, unit,
                    Messages.getMessage("SessionBeanMustBePublic"),
                    DIAGNOSTIC_CODE_NOT_PUBLIC_CLASS,
                    null,
                    DiagnosticSeverity.Error));
        }

        // Must not be final
        if (type.hasModifierProperty(PsiModifier.FINAL)) {
            diagnostics.add(createDiagnostic(type, unit,
                    Messages.getMessage("SessionBeanMustNotBeFinal"),
                    DIAGNOSTIC_CODE_IS_FINAL_CLASS,
                    null,
                    DiagnosticSeverity.Error));
        }

        // Must not be abstract
        if (type.hasModifierProperty(PsiModifier.ABSTRACT)) {
            diagnostics.add(createDiagnostic(type, unit,
                    Messages.getMessage("SessionBeanMustNotBeAbstract"),
                    DIAGNOSTIC_CODE_IS_ABSTRACT_CLASS,
                    null,
                    DiagnosticSeverity.Error));
        }
    }

    /**
     * Validates that a method annotated with a session synchronization annotation
     * ({@code @AfterBegin}, {@code @BeforeCompletion}, {@code @AfterCompletion}) is
     * not declared final, not declared static, and returns void.
     *
     * @param type        the declaring class
     * @param method      the method to validate
     * @param unit        the compilation unit
     * @param diagnostics the list to add diagnostics to
     */
    private void validateSessionSyncMethod(PsiClass type, PsiMethod method, PsiJavaFile unit, List<Diagnostic> diagnostics) {
        List<String> matchedAnnotations = getMatchedJavaElementNames(type,
                Stream.of(method.getAnnotations())
                        .map(PsiAnnotation::getQualifiedName)
                        .toArray(String[]::new),
                SESSION_SYNC_ANNOTATIONS);

        if (matchedAnnotations.isEmpty()) {
            return;
        }

        String annotationNames = DiagnosticsUtils.getSimpleAnnotationNames(matchedAnnotations, "@");

        if (method.hasModifierProperty(PsiModifier.FINAL)) {
            diagnostics.add(createDiagnostic(method, unit,
                    Messages.getMessage("InvalidSessionSyncMethodFinal", annotationNames),
                    DIAGNOSTIC_CODE_INVALID_SESSION_SYNC_FINAL,
                    null,
                    DiagnosticSeverity.Error));
        }

        if (method.hasModifierProperty(PsiModifier.STATIC)) {
            diagnostics.add(createDiagnostic(method, unit,
                    Messages.getMessage("InvalidSessionSyncMethodStatic", annotationNames),
                    DIAGNOSTIC_CODE_INVALID_SESSION_SYNC_STATIC,
                    null,
                    DiagnosticSeverity.Error));
        }

        PsiType returnType = method.getReturnType();
        if (returnType != null && !returnType.equals(PsiTypes.voidType())) {
            diagnostics.add(createDiagnostic(method, unit,
                    Messages.getMessage("InvalidSessionSyncMethodNonVoid", annotationNames),
                    DIAGNOSTIC_CODE_INVALID_SESSION_SYNC_NON_VOID,
                    null,
                    DiagnosticSeverity.Error));
        }
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
                    Messages.getMessage("InvalidSessionBeanWithInterceptorOrDecorator"),
                    DIAGNOSTIC_CODE_SESSION_BEAN_INTERCEPTOR_DECORATOR,
                    null,
                    DiagnosticSeverity.Error));
        }
    }

    /**
     * Validates that a session bean class does not have more than one session bean stereotype annotation.
     *
     * @param type                   the class to validate
     * @param unit                   the compilation unit
     * @param sessionBeanAnnotations the list of conflicting session bean annotations found
     * @param diagnostics            the list to add diagnostics to
     */
    private void validateConflictingSessionBeanAnnotations(PsiClass type, PsiJavaFile unit,
                                                           List<String> sessionBeanAnnotations,
                                                           List<Diagnostic> diagnostics) {
        String annotationNames = DiagnosticsUtils.getSimpleAnnotationNames(sessionBeanAnnotations, "@");
        String message = Messages.getMessage("SessionBeanConflictingAnnotations", annotationNames);
        diagnostics.add(createDiagnostic(type, unit, message,
                DIAGNOSTIC_CODE_CONFLICTING_ANNOTATIONS,
                new Gson().toJsonTree(sessionBeanAnnotations),
                DiagnosticSeverity.Error));
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

    /**
     * Validates that a session bean does not define or override the finalize() method.
     *
     * According to the Jakarta EE Enterprise Beans specification, session bean classes
     * must not override or define the finalize() method. The container manages the
     * lifecycle and cleanup of session beans.
     *
     * @param type the class to validate
     * @param unit the compilation unit
     * @param diagnostics the list to add diagnostics to
     */
    private void validateSessionBeanFinalizeMethod(PsiClass type, PsiJavaFile unit, List<Diagnostic> diagnostics) {
        // Check all methods in the class
        for (PsiMethod method : type.getMethods()) {
            // Check if this is the finalize() method
            if (FINALIZE_METHOD_NAME.equals(method.getName()) &&
                    method.getParameterList().getParametersCount() == 0) {
                // Report diagnostic for finalize() method
                diagnostics.add(createDiagnostic(method, unit,
                        Messages.getMessage("SessionBeanFinalizeMethod"),
                        DIAGNOSTIC_CODE_FINALIZE_METHOD,
                        null,
                        DiagnosticSeverity.Error));
            }
        }
    }
}
