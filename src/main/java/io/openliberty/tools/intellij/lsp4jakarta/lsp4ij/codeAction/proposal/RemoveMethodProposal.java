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

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifierListOwner;

import java.util.List;

/**
 * Code action proposal for removing methods from a class.
 */
public class RemoveMethodProposal extends RemoveElementsProposal {

    /**
     * Constructor for RemoveMethodProposal.
     *
     * @param label The label for this proposal
     * @param sourceCU The source compilation unit
     * @param invocationNode The invocation node
     * @param binding The modifier list owner (the class)
     * @param relevance The relevance score
     * @param methodsToRemove List of methods to remove
     */
    public RemoveMethodProposal(
            String label,
            PsiFile sourceCU,
            PsiFile invocationNode,
            PsiModifierListOwner binding,
            int relevance,
            List<? extends PsiElement> methodsToRemove) {
        super(label, sourceCU, invocationNode, binding, relevance, methodsToRemove);
    }
}