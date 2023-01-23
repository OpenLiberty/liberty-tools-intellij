/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 *  SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package io.openliberty.tools.intellij.liberty.lsp;

import com.intellij.codeInspection.InspectionSuppressor;
import com.intellij.codeInspection.SuppressQuickFix;
import com.intellij.lang.properties.codeInspection.unused.UnusedPropertyInspection;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Suppress UnusedProperty inspections for ServerEnv file types as they are not "Properties" files but are using the Properties language parser
 */
public class ServerEnvInspectionSuppressor implements InspectionSuppressor {

    @Override
    public boolean isSuppressedFor(@NotNull PsiElement element, @NotNull String toolId) {
        // suppress UnusedPropertyInspection on server.env files
        if (toolId.equals(UnusedPropertyInspection.SHORT_NAME) && element.getContainingFile() != null
                && element.getContainingFile().getVirtualFile() != null
                && element.getContainingFile().getVirtualFile().getFileType() instanceof ServerEnvFileType) {
            return true;
        }
        return false;
    }

    @Override
    public SuppressQuickFix @NotNull [] getSuppressActions(@Nullable PsiElement element, @NotNull String toolId) {
        return new SuppressQuickFix[0];
    }
}
