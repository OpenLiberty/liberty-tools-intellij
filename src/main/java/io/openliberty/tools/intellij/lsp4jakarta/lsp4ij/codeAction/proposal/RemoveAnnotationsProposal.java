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

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifierListOwner;

import java.util.List;

public class RemoveAnnotationsProposal extends RemoveElementsProposal {

    public RemoveAnnotationsProposal(String label, PsiFile sourceCU, PsiFile invocationNode,
                                     PsiModifierListOwner binding, int relevance,
                                     List<PsiAnnotation> annotationsToRemove) {
        super(label, sourceCU, invocationNode, binding, relevance, annotationsToRemove);
    }
}