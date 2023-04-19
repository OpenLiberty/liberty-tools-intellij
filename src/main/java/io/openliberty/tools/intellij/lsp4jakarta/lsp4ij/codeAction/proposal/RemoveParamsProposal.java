/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation, Bera Sogut and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation, Bera Sogut - initial API and implementation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;

import java.util.List;

/**
 * Code action proposal for removing parameters of a method. Used by
 * JAX-RS ResourceMethodMultipleEntityParamsQuickFix.
 *
 * @author Bera Sogut
 */
public class RemoveParamsProposal extends RemoveElementsProposal {

    /**
     * Constructor for RemoveParamsProposal that accepts parameters to remove.
     *
     * @param parametersToRemove the parameters of the function to remove
     */
    public RemoveParamsProposal(String label, PsiFile sourceCU, PsiFile invocationNode,
                                PsiModifierListOwner binding, int relevance,
                                List<PsiParameter> parametersToRemove) {
        super(label, sourceCU, invocationNode, binding, relevance, parametersToRemove);
    }
}
