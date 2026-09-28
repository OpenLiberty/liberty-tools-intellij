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
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.codeStyle.CodeStyleManager;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.Change;

import java.util.Map;

/**
 * Code action proposal for inserting an annotation with specific attribute values.
 * This proposal allows adding annotations with predefined attribute values.
 */
public class InsertAnnotationWithAttributesProposal extends NewAnnotationProposal {

    private final PsiFile sourceCU;
    private final Map<String, PsiAnnotationMemberValue> attributeValues;

    /**
     * Constructor for inserting an annotation with attributes.
     *
     * @param label the label for the code action
     * @param sourceCU the source compilation unit
     * @param invocationNode the invocation node
     * @param binding the modifier list owner (field, method, or class)
     * @param relevance the relevance of the proposal
     * @param annotation the annotation to insert
     * @param attributeValues map of attribute names to their values
     */
    public InsertAnnotationWithAttributesProposal(String label, PsiFile sourceCU, PsiFile invocationNode,
                                                   PsiModifierListOwner binding, int relevance,
                                                   String annotation, Map<String, PsiAnnotationMemberValue> attributeValues) {
        super(label, null, invocationNode, binding, null, relevance, annotation);
        this.sourceCU = sourceCU;
        this.attributeValues = attributeValues;
    }

    @Override
    public Change getChange() {
        PsiFile fInvocationNode = getInvocationNode();
        PsiAnnotation annotation = getAnnotation();
        PsiModifierListOwner binding = getBinding();
        
        // If annotation doesn't exist, create it using parent's performUpdate
        // This also handles adding the annotation import
        if (annotation == null) {
            super.performUpdate();
            annotation = getAnnotation();
        }
        
        // Set attribute values on the annotation
        // Note: Imports for enum types in attribute values are handled by
        // AnnotationValueExpressionUtil.createEnumValueExpression()
        if (annotation != null && attributeValues != null && !attributeValues.isEmpty()) {
            for (Map.Entry<String, PsiAnnotationMemberValue> entry : attributeValues.entrySet()) {
                annotation.setDeclaredAttributeValue(entry.getKey(), entry.getValue());
            }
        }
        
        CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(fInvocationNode.getProject());
        
        // Reformat the modifier list to ensure proper line breaks between annotations
        if (binding != null && binding.getModifierList() != null) {
            codeStyleManager.reformat(binding.getModifierList());
        }
        
        // Reformat the import list to ensure proper line breaks between imports
        if (fInvocationNode instanceof PsiJavaFile) {
            PsiImportList importList = ((PsiJavaFile) fInvocationNode).getImportList();
            if (importList != null) {
                codeStyleManager.reformat(importList);
            }
        }

        final Document changed = fInvocationNode.getViewProvider().getDocument();
        return new Change(sourceCU.getViewProvider().getDocument(), changed);
    }
}

