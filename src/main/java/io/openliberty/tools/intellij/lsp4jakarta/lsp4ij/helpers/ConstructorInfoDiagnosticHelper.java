/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.helpers;

import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameterList;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;

/**
 * Constructor information diagnostics helper for a given method.
 *
 * @author Archana Iyer
 *
 */
public final class ConstructorInfoDiagnosticHelper {

    private boolean hasConstructor;
    private boolean hasValidPublicNoArgsConstructor;
    private boolean hasValidProtectedNoArgsConstructor;

    private ConstructorInfoDiagnosticHelper(boolean hasConstructor,
                                            boolean hasValidPublicNoArgsConstructor,
                                            boolean hasValidProtectedNoArgsConstructor) {
        this.hasConstructor = hasConstructor;
        this.hasValidPublicNoArgsConstructor = hasValidPublicNoArgsConstructor;
        this.hasValidProtectedNoArgsConstructor = hasValidProtectedNoArgsConstructor;
    }

    public boolean hasConstructor() {
        return hasConstructor;
    }

    public boolean hasValidPublicNoArgsConstructor() {
        return hasValidPublicNoArgsConstructor;
    }

    public boolean hasValidProtectedNoArgsConstructor() {
        return hasValidProtectedNoArgsConstructor;
    }

    @Override
    public String toString() {
        return "ConstructorInfoDiagnosticHelper [hasConstructor=" + hasConstructor
                + ", hasValidPublicNoArgsConstructor=" + hasValidPublicNoArgsConstructor
                + ", hasValidProtectedNoArgsConstructor=" + hasValidProtectedNoArgsConstructor + "]";
    }

    /**
     * Factory utility method checks the constructor existence and returns the constructor information
     *
     * @param method
     * @return
     */
    public static ConstructorInfoDiagnosticHelper getConstructorInfo(PsiMethod method) {
        boolean isUserDefinedConstructor = false;
        boolean isPublicNoArgsConstructor = false;
        boolean isProtectedNoArgsConstructor = false;

        if (AbstractDiagnosticsCollector.isConstructorMethod((method))) {
            isUserDefinedConstructor = true; // Check explicit constructor declaration
            PsiParameterList params = method.getParameterList();
            if (params.getParametersCount() == 0) { // Checks for user defined no-args constructor
                if (method.hasModifierProperty(PsiModifier.PUBLIC)) {
                    isPublicNoArgsConstructor = true;
                }
                if (method.hasModifierProperty(PsiModifier.PROTECTED)) {
                    isProtectedNoArgsConstructor = true;
                }
            }
        }
        return new ConstructorInfoDiagnosticHelper(isUserDefinedConstructor, isPublicNoArgsConstructor, isProtectedNoArgsConstructor); // This ensures that the values don't get changed
    }

    /**
     * This method merges the constructor check info and retains the constructor information
     *
     * @param calculatedValue
     * @return
     */
    public ConstructorInfoDiagnosticHelper mergeConstructorInfo(ConstructorInfoDiagnosticHelper calculatedValue) {
        this.hasConstructor = this.hasConstructor || calculatedValue.hasConstructor;
        this.hasValidPublicNoArgsConstructor = this.hasValidPublicNoArgsConstructor || calculatedValue.hasValidPublicNoArgsConstructor;
        this.hasValidProtectedNoArgsConstructor = this.hasValidProtectedNoArgsConstructor || calculatedValue.hasValidProtectedNoArgsConstructor;
        return this;
    }

    /**
     * This is to return default values for constructor information
     *
     * @return
     */
    public static ConstructorInfoDiagnosticHelper initialize() {
        return new ConstructorInfoDiagnosticHelper(false, false, false);
    }
}