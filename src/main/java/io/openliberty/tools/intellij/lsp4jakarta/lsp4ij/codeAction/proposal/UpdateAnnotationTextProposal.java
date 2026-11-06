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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiFile;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.Change;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeActionKind;

/**
 * Proposal that updates an annotation by replacing its text.
 * This is useful when you need to control the exact syntax of the replacement,
 * such as using explicit attribute names instead of shorthand syntax.
 */
public class UpdateAnnotationTextProposal extends ChangeCorrectionProposal {
    private final PsiFile sourceCU;
    private final PsiAnnotation annotation;
    private final String replacementText;

    /**
     * Constructor for replacing an annotation with new text.
     *
     * @param name the label for the code action
     * @param sourceCU the source compilation unit
     * @param invocationNode the invocation node (can be the annotation itself)
     * @param annotation the annotation to replace
     * @param relevance the relevance of the proposal
     * @param replacementText the new text to replace the annotation with
     */
    public UpdateAnnotationTextProposal(String name, PsiFile sourceCU, PsiFile invocationNode,
                                       PsiAnnotation annotation, int relevance, String replacementText) {
        super(name, CodeActionKind.QuickFix, relevance);
        this.sourceCU = sourceCU;
        this.annotation = annotation;
        this.replacementText = replacementText;
    }

    @Override
    public Change getChange() {
        if (annotation != null && replacementText != null) {
            Document document = annotation.getContainingFile().getViewProvider().getDocument();
            if (document != null) {
                int startOffset = annotation.getTextRange().getStartOffset();
                int endOffset = annotation.getTextRange().getEndOffset();
                document.replaceString(startOffset, endOffset, replacementText);
                return new Change(sourceCU.getViewProvider().getDocument(), document);
            }
        }
        return null;
    }
}
