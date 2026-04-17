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
 * Proposal for removing attributes (methods and fields) from annotation type declarations.
 *
 * <p>This is specifically used for removing attributes from @Scope annotated interfaces
 * to comply with Jakarta EE Dependency Injection specifications.</p>
 */
public class RemoveAnnotationAttributesProposal extends RemoveElementsProposal {

    /**
     * Constructor.
     *
     * @param label The label for this proposal
     * @param sourceCU The source compilation unit
     * @param invocationNode The invocation node
     * @param binding The modifier list owner (the annotation class)
     * @param relevance The relevance score
     * @param attributesToRemove List of attributes (methods and fields) to remove
     */
    public RemoveAnnotationAttributesProposal(
            String label,
            PsiFile sourceCU,
            PsiFile invocationNode,
            PsiModifierListOwner binding,
            int relevance,
            List<? extends PsiElement> attributesToRemove) {
        super(label, sourceCU, invocationNode, binding, relevance, attributesToRemove);
    }
}