/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copied from https://github.com/eclipse/lsp4jakarta/blob/main/jakarta.jdt/org.eclipse.lsp4jakarta.jdt.core/src/main/java/org/eclipse/lsp4jakarta/jdt/codeAction/proposal/ModifyModifiersProposal.java
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.PositionUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.Change;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeActionKind;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Code action proposal for modifiers reused from
 * https://github.com/eclipse/lsp4jakarta/blob/main/jakarta.jdt/org.eclipse.lsp4jakarta.jdt.core/src/main/java/org/eclipse/lsp4jakarta/jdt/codeAction/proposal/ModifyModifiersProposal.java
 */
public class ModifyModifiersProposal extends ChangeCorrectionProposal {

    private final PsiFile invocationNode;
    private final PsiModifierListOwner binding;
    private final PsiModifierList modifiers;
    
    // list of modifiers to add
    private final List<String> modifiersToAdd;

    // list of modifiers (if they exist) to remove
    private final List<String> modifiersToRemove;

    /**
     *  Constructor for ModifyModifiersProposal that accepts both a list of modifiers to remove as well as to add
     * 
     * @param modifiersToAdd        list of valid modifiers as strings to be added
     * @param modifiersToRemove     list of modifiers as strings to be removed
     */
    public ModifyModifiersProposal(String label, PsiFile targetCU, PsiFile invocationNode,
                PsiModifierListOwner binding, int relevance, PsiModifierList modifiers, List<String> modifiersToAdd, List<String> modifiersToRemove) {
        super(label, CodeActionKind.QuickFix, relevance);
        this.invocationNode = invocationNode;
        this.binding = binding;
        this.modifiers = modifiers;
        this.modifiersToAdd = modifiersToAdd;        
        this.modifiersToRemove = modifiersToRemove;
    }
    
    /**
     *  Constructor for ModifyModifiersProposal that accepts only a list of modifiers to add
     *  If a visibility modifier is specified to be added, existing visibility modifiers will be removed
     *
     * @param modifiersToAdd        list of valid modifiers as strings to be added
     */
    public ModifyModifiersProposal(String label, PsiFile targetCU, PsiFile invocationNode,
                                   PsiModifierListOwner binding, int relevance, PsiModifierList modifiers, List<String> modifiersToAdd) {
        this(label, targetCU, invocationNode, binding, relevance, modifiers, modifiersToAdd, Collections.emptyList());
    }

    @Override
    public Change getChange() {
        // Remove modifiers
        modifiersToRemove.forEach(modifier -> {
            if (modifiers.hasExplicitModifier(modifier)) {
                modifiers.setModifierProperty(modifier, false);
            }
        });
        // Add modifiers
        modifiersToAdd.forEach(modifier -> {
            if (!modifiers.hasExplicitModifier(modifier)) {
                // if adding a visibility modifier, need to remove the existing one
                if (PsiModifier.PUBLIC.equals(modifier) ||
                        PsiModifier.PROTECTED.equals(modifier) ||
                        PsiModifier.PRIVATE.equals(modifier)) {
                    // check if the existing visibility modifier is public, protected or private
                    final String[] removeExistingVisibilityModifiers = new String[] {PsiModifier.PUBLIC, PsiModifier.PROTECTED, PsiModifier.PRIVATE};
                    Arrays.stream(removeExistingVisibilityModifiers).forEach(modifierToRemove -> {
                        if (modifiers.hasExplicitModifier(modifierToRemove)) {
                            modifiers.setModifierProperty(modifierToRemove, false);
                        }
                    });
                }
                modifiers.setModifierProperty(modifier, true);
            }
        });
        PositionUtils.formatDocument(binding); // add the necessary new lines, must use 'binding,' it's already in the document
        final Document document = invocationNode.getViewProvider().getDocument();
        return new Change(document, document);
    }
}
