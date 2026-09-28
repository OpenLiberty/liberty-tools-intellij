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
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiAnnotation;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.Change;

import java.util.Map;

/**
 * Code action proposal for modifying an existing annotation's attribute value.
 * This proposal allows changing the value of a specific attributes in an annotation.
 */
public class ModifyAnnotationAttributesValueProposal extends NewAnnotationProposal {

    private final PsiFile sourceCU;
    private final Map<String, PsiAnnotationMemberValue> attributeValues;

    /**
     * Constructor for modifying annotation attributes values.
     *
     * @param label the label for the code action
     * @param sourceCU the source compilation unit
     * @param invocationNode the invocation node
     * @param binding the modifier list owner (field, method, or class)
     * @param annotationNode the existing annotation to modify
     * @param relevance the relevance of the proposal
     * @param annotation the annotation name
     * @param attributeValues map of attribute names to their new values
     */
    public ModifyAnnotationAttributesValueProposal(String label, PsiFile sourceCU, PsiFile invocationNode,
                                                   PsiModifierListOwner binding, PsiAnnotation annotationNode,
                                                   int relevance, String annotation,
                                                   Map<String, PsiAnnotationMemberValue> attributeValues) {
        super(label, null, invocationNode, binding, annotationNode, relevance, annotation);
        this.sourceCU = sourceCU;
        this.attributeValues = attributeValues;
    }

    @Override
    public Change getChange() {
        PsiFile fInvocationNode = getInvocationNode();
        PsiAnnotation annotation = getAnnotation();

        if (annotation != null && attributeValues != null && !attributeValues.isEmpty()) {
            // Modify attribute values
            for (Map.Entry<String, PsiAnnotationMemberValue> entry : attributeValues.entrySet()) {
                String attributeName = entry.getKey();
                PsiAnnotationMemberValue newValue = entry.getValue();
                
                // Set or update the attribute value
                annotation.setDeclaredAttributeValue(attributeName, newValue);
            }
        }

        final Document changed = fInvocationNode.getViewProvider().getDocument();
        return new Change(sourceCU.getViewProvider().getDocument(), changed);
    }
}

