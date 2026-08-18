/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     IBM Corporation, Jianing Xu - initial API and implementation
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.Change;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeActionKind;

import java.util.Arrays;

/**
 *
 * Code action proposal for deleting an existing annotation for
 * MethodDeclaration/Field.
 *
 * Author: Jianing Xu
 *
 */
public class DeleteAnnotationProposal extends ChangeCorrectionProposal {
    private final PsiFile fSourceCU;
    private final PsiFile fInvocationNode;
    private final PsiElement fBinding;

    private final String[] annotations;
    private final PsiElement declaringNode;

    /**
     * Constructor for DeleteAnnotationProposal
     *
     * @param label          - annotation label
     * @param sourceCU       - the entire Java compilation unit
     * @param invocationNode
     * @param binding
     * @param relevance
     * @param declaringNode  - declaringNode covered node of diagnostic
     * @param annotations
     *
     */
    public DeleteAnnotationProposal(String label, PsiFile sourceCU, PsiFile invocationNode,
                                    PsiElement binding, int relevance, PsiElement declaringNode, String... annotations) {
        super(label, CodeActionKind.QuickFix, relevance);
        this.fSourceCU = sourceCU;
        this.fInvocationNode = invocationNode;
        this.fBinding = binding;
        this.declaringNode = declaringNode;
        this.annotations = annotations;
    }

    @Override
    public Change getChange() {
        if (declaringNode instanceof PsiModifierListOwner) {
            PsiModifierListOwner targetNode = ((PsiModifierListOwner) declaringNode);
            PsiAnnotation[] targetAnnotations = targetNode.getAnnotations();
            for (var annotation : targetAnnotations) {
                // Allow the names in targetAnnotations to be fully qualified or short (no package name).
                if (Arrays.stream(annotations).anyMatch(a -> annotation.getQualifiedName().equals(a))) {
                    annotation.delete();
                }
            }
        }
        final Document document = fInvocationNode.getViewProvider().getDocument();
        return new Change(fSourceCU.getViewProvider().getDocument(), document);
    }
}
