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
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifierListOwner;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.Change;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeActionKind;

import java.util.List;

public class RemoveAnnotationsProposal extends ChangeCorrectionProposal {

    private final PsiFile invocationNode;
    private final PsiModifierListOwner binding;
    private final List<PsiAnnotation> annotationsToRemove;

    public RemoveAnnotationsProposal(String label, PsiFile targetCU, PsiFile invocationNode,
                                     PsiModifierListOwner binding, int relevance,
                                     List<PsiAnnotation> annotationsToRemove) {
        super(label, CodeActionKind.QuickFix, relevance);
        this.invocationNode = invocationNode;
        this.binding = binding;
        this.annotationsToRemove = annotationsToRemove;
    }

    @Override
    public Change getChange() {
        annotationsToRemove.forEach(a -> {
            a.delete();
        });
        final Document document = invocationNode.getViewProvider().getDocument();
        return new Change(document, document);
    }
}