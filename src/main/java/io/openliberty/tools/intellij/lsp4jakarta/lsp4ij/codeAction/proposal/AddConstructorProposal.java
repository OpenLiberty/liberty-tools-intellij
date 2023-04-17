/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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
import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.PositionUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.Change;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeActionKind;

/**
 * Code action proposal for adding a no-arg constructor to a class
 *
 * @author  Leslie Dawson
 * @ see     PersistenceEntityQuickFix
 *
 */
public class AddConstructorProposal extends ChangeCorrectionProposal {

    private final PsiFile sourceCU;
    private final PsiFile invocationNode;
    private final PsiClass binding;
    private final String visibility;

    /**
     * Constructor for AddMethodProposal
     *
     */
    public AddConstructorProposal(String label, PsiFile sourceCU, PsiFile invocationNode,
                                  PsiClass binding, int relevance) {
        super(label, CodeActionKind.QuickFix, relevance);
        this.sourceCU = sourceCU;
        this.invocationNode = invocationNode;
        this.binding = binding;
        this.visibility = "protected";
    }

    /**
     * Constructor for AddMethodProposal
     *
     * @param visibility    a valid visibility modifier for the constructor, defaults to protected
     */
    public AddConstructorProposal(String label, PsiFile sourceCU, PsiFile invocationNode,
                                  PsiClass binding, int relevance, String visibility) {
        super(label, CodeActionKind.QuickFix, relevance);
        this.sourceCU = sourceCU;
        this.invocationNode = invocationNode;
        this.binding = binding;
        this.visibility = visibility;
    }

    @Override
    public Change getChange() {
        PsiMethod newConstructor = JavaPsiFacade.getElementFactory(binding.getProject()).createConstructor();
        // set visibility
        PsiModifierList modifiers = newConstructor.getModifierList();
        if (visibility.equals("protected")) {
            modifiers.setModifierProperty(PsiModifier.PROTECTED, true);
        } else if (visibility.equals("public")) {
            modifiers.setModifierProperty(PsiModifier.PUBLIC, true);
        }
        // Find a place in the user program to insert the new constructor.
        PsiElement bestSpot = null;
        PsiMethod[] constructors = binding.getConstructors();
        if (constructors.length > 0) {
            bestSpot = constructors[0]; // make the new one the first constructor
        } else {
            bestSpot = binding.getFirstChild(); // put the new c'tor at the start of the class
        }
        binding.addBefore(newConstructor, bestSpot);
        PositionUtils.formatDocument(binding); // add the necessary new lines, must use 'binding,' it's already in the document
        final Document document = invocationNode.getViewProvider().getDocument();
        return new Change(sourceCU.getViewProvider().getDocument(), document);
    }
}
