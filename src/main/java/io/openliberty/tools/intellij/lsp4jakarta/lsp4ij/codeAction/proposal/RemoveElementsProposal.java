/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifierListOwner;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.PositionUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.Change;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeActionKind;

import java.util.List;

public abstract class RemoveElementsProposal extends ChangeCorrectionProposal {

    private final PsiFile sourceCU;
    private final PsiFile invocationNode;
    private final PsiModifierListOwner binding;
    private final List<? extends PsiElement> elementsToRemove;
    private boolean isFormatRequired = true;

    protected RemoveElementsProposal(String label, PsiFile sourceCU, PsiFile invocationNode,
                                PsiModifierListOwner binding, int relevance,
                                List<? extends PsiElement> elementsToRemove) {
        super(label, CodeActionKind.QuickFix, relevance);
        this.sourceCU = sourceCU;
        this.invocationNode = invocationNode;
        this.binding = binding;
        this.elementsToRemove = elementsToRemove;
    }

    protected RemoveElementsProposal(String label, PsiFile sourceCU, PsiFile invocationNode,
                                     PsiModifierListOwner binding, int relevance,
                                     List<? extends PsiElement> elementsToRemove, boolean isFormatRequired) {
        super(label, CodeActionKind.QuickFix, relevance);
        this.sourceCU = sourceCU;
        this.invocationNode = invocationNode;
        this.binding = binding;
        this.elementsToRemove = elementsToRemove;
        this.isFormatRequired = isFormatRequired;
    }

    @Override
    public final Change getChange() {
        elementsToRemove.forEach(PsiElement::delete);
        if(isFormatRequired) {
            PositionUtils.formatDocument(binding); // fix up whitespace
        }
        final Document document = invocationNode.getViewProvider().getDocument();
        return new Change(sourceCU.getViewProvider().getDocument(), document);
    }
}
