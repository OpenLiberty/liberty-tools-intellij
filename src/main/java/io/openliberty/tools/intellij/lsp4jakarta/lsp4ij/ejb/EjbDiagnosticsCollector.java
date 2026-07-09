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
            checkClass(type, unit, diagnostics);
        }
    }

    /**
     * Checks a class and all its inner classes for session bean violations.
     *
     * @param type the class to check
     * @param unit the compilation unit
     * @param diagnostics the list to add diagnostics to
     */
    private void checkClass(PsiClass type, PsiJavaFile unit, List<Diagnostic> diagnostics) {
        List<String> sessionBeanAnnotations = getMatchedJavaElementNames(type,
                Stream.of(type.getAnnotations())
                        .map(annotation -> annotation.getQualifiedName())
                        .toArray(String[]::new),
                SESSION_BEAN_ANNOTATIONS);

        if (!sessionBeanAnnotations.isEmpty()) {
            validateSessionBeanClass(type, unit, diagnostics);
            validateSessionBeanConstructor(type, unit, diagnostics);
        }

        for (PsiClass innerClass : type.getInnerClasses()) {
            checkClass(innerClass, unit, diagnostics);
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
                    DIAGNOSTIC_CODE_NOT_TOP_LEVEL_CLASS,
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