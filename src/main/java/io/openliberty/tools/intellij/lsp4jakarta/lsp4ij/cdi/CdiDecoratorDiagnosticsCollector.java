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
import java.util.stream.Stream;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
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
     * @param type
     * @param unit
     * @param diagnostics
     */
    private void validateDecorator(PsiClass type, PsiJavaFile unit, List<Diagnostic> diagnostics) {
        String[] typeAnnotations = Stream.of(type.getAnnotations())
                .map(PsiAnnotation::getQualifiedName)
                .toArray(String[]::new);
        if (getMatchedJavaElementNames(type, typeAnnotations, new String[] { ManagedBeanConstants.DECORATOR_FQ_NAME }).isEmpty()) {
            return;
        }

        List<PsiElement> delegateElements = new ArrayList<>();

        for (PsiField field : type.getFields()) {
            String[] fieldAnnotations = Stream.of(field.getAnnotations())
                    .map(PsiAnnotation::getQualifiedName)
                    .toArray(String[]::new);
            if (!getMatchedJavaElementNames(type, fieldAnnotations, new String[] { ManagedBeanConstants.DELEGATE_FQ_NAME }).isEmpty()) {
                delegateElements.add(field);
            }
        }

        for (PsiMethod method : type.getMethods()) {
            for (PsiParameter parameter : method.getParameterList().getParameters()) {
                String[] parameterAnnotations = Stream.of(parameter.getAnnotations())
                        .map(PsiAnnotation::getQualifiedName)
                        .toArray(String[]::new);
                if (!getMatchedJavaElementNames(type, parameterAnnotations, new String[] { ManagedBeanConstants.DELEGATE_FQ_NAME }).isEmpty()) {
                    delegateElements.add(parameter);
                }
            }
        }

        reportInvalidDelegateCountDiagnostics(type, unit, diagnostics, delegateElements);
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
}

