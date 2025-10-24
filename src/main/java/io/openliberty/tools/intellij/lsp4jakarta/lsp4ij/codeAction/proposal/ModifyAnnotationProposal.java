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
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.Change;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * Code action proposal for modifying an existing annotation. Option for adding
 * additional string attributes and option for removing string attributes from
 * specified annotation.
 *
 * @author Kathryn Kodama
 *
 */
public class ModifyAnnotationProposal extends NewAnnotationProposal {

    // The original file or compilation unit
    private final PsiFile sourceCU;

    // list of attributes to add to the annotations
    private final List<String> attributesToAdd;

    // list of attributes (if they exist) to remove from the annotations
    private final List<String> attributesToRemove;

    public ModifyAnnotationProposal(String label, PsiFile sourceCU, PsiFile invocationNode,
                                    PsiModifierListOwner binding, PsiAnnotation annotationNode,
                                    int relevance, String annotation, List<String> attributesToAdd,
                                    List<String> attributesToRemove) {
        super(label, null, invocationNode, binding, annotationNode, relevance, annotation);
        this.sourceCU = sourceCU;
        this.attributesToAdd = attributesToAdd;
        this.attributesToRemove = attributesToRemove;
    }
    public ModifyAnnotationProposal(String label, PsiFile sourceCU, PsiFile invocationNode,
                                    PsiModifierListOwner binding, PsiAnnotation annotationNode,
                                    int relevance, String annotation, List<String> attributesToAdd) {
        super(label, null, invocationNode, binding, annotationNode, relevance, annotation);
        this.sourceCU = sourceCU;
        this.attributesToAdd = attributesToAdd;
        this.attributesToRemove = new ArrayList<>();
    }
    public ModifyAnnotationProposal(String label, PsiFile sourceCU, PsiFile invocationNode,
                                    PsiModifierListOwner binding, PsiAnnotation annotationNode,
                                    int relevance, List<String> attributesToAdd, String... annotations) {
        super(label, null, invocationNode, binding, annotationNode, relevance, annotations);
        this.sourceCU = sourceCU;
        this.attributesToAdd = attributesToAdd;
        this.attributesToRemove = new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Change getChange() {
        PsiFile fInvocationNode = getInvocationNode();
        PsiAnnotation annotation = getAnnotation();
        if (annotation == null) {
            super.performUpdate();
            annotation = getAnnotation();
        }

        if (annotation != null) {
            var parameters = annotation.getParameterList();
            var values = parameters.getAttributes();
            // add new attributes
            for (String newAttr : this.attributesToAdd) {
                // don't add duplicate attributes to an annotation
                if (Arrays.stream(values).noneMatch(v -> v.getName().equals(newAttr))) {
                    annotation.setDeclaredAttributeValue(newAttr, newDefaultExpression(annotation, newAttr));
                }
            }
            // remove attributes
            for (String oldAttr : this.attributesToRemove) {
                // remove existing attribute
                PsiAnnotationMemberValue value = annotation.findDeclaredAttributeValue(oldAttr);
                if (value != null) {
                    value.getParent().delete(); // remove member/value pair from the AST
                }
            }
        }

        final Document changed = fInvocationNode.getViewProvider().getDocument();
        return  new Change(sourceCU.getViewProvider().getDocument(), changed);
    }

    /**
     * newDefaultExpression
     * Add new attributes of type String or Class.
     * For initial values, we use empty strings for String types and Object.class for Class types,
     * since the user's intended values are unknown at this stage,
     * These placeholders (e.g., name = "", type = Object.class) must be updated by the user as needed.
     * when an annotation in Jakarta EE declares an attribute named type, it’s always of the form of Class<?>
     *
     * @param annotation
     * @param attributeName
     * @return
     */
    private PsiAnnotationMemberValue newDefaultExpression(PsiAnnotation annotation, String attributeName) {
        if ("type".equals(attributeName)) {
            return PsiElementFactory.getInstance(annotation.getProject())
                    .createExpressionFromText("Object.class", annotation);
        } else {
            return PsiElementFactory.getInstance(annotation.getProject()).
                    createExpressionFromText("\"\"", annotation);
        }

    }
}
