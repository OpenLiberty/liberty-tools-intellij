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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationConflictQuickFix;

/**
 * Quick fix for removing @Dependent annotation from a class
 */
public class RemoveDependentAnnotationQuickFix extends RemoveAnnotationConflictQuickFix {

    public RemoveDependentAnnotationQuickFix() {
        super(ManagedBeanConstants.DEPENDENT_FQ_NAME);
    }

    @Override
    public String getParticipantId() {
        return RemoveDependentAnnotationQuickFix.class.getName();
    }

    /**
     * Override getBinding to return the class instead of the method,
     * since the diagnostic is on the method but we need to remove
     * the annotation from the class.
     */
    @Override
    protected PsiElement getBinding(PsiElement node) {
        return PsiTreeUtil.getParentOfType(node, PsiClass.class);
    }
}
