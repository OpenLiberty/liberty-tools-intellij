/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Yijia Jing
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.annotations.PostConstructReturnTypeQuickFix;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.Change;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeActionKind;

/**
 * Code action proposal for changing the return type of a method.
 *
 * @author Yijia Jing
 * @see PostConstructReturnTypeQuickFix
 *
 */
public class ModifyReturnTypeProposal extends ChangeCorrectionProposal {

    private final PsiFile sourceCU;
    private final PsiFile invocationNode;
    private final PsiElement binding;
    private final PsiType newReturnType;

    /**
     * Constructor for ModifyReturnTypeProposal that accepts the new return type of a method.
     *
     * @param newReturnType the new return type to change to
     */
    public ModifyReturnTypeProposal(String label, PsiFile sourceCU, PsiFile invocationNode,
                                    PsiElement binding, int relevance, PsiType newReturnType) {
        super(label, CodeActionKind.QuickFix, relevance);
        this.sourceCU = sourceCU;
        this.invocationNode = invocationNode;
        this.binding = binding;
        this.newReturnType = newReturnType;
    }

    @SuppressWarnings("restriction")
    @Override
    public Change getChange() {
        if (binding instanceof PsiMethod) {
            PsiMethod method = ((PsiMethod) binding);
            PsiTypeElement oldType = method.getReturnTypeElement();
            PsiElementFactory factory = JavaPsiFacade.getInstance(binding.getProject()).getElementFactory();
            PsiTypeElement newType = factory.createTypeElement(newReturnType);
            if (oldType != null) {
                oldType.replace(newType);
            }
        }
        final Document changed = invocationNode.getViewProvider().getDocument();
        return new Change(sourceCU.getViewProvider().getDocument(), changed);
    }
}
