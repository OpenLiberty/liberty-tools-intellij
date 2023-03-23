/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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

    // list of attributes to add to the annotations
    private final List<String> attributesToAdd;

    // list of attributes (if they exist) to remove from the annotations
    private final List<String> attributesToRemove;

    public ModifyAnnotationProposal(String label, PsiFile targetCU, PsiFile invocationNode,
                                    PsiModifierListOwner binding, int relevance, PsiAnnotation annotation, List<String> attributesToAdd,
                                    List<String> attributesToRemove) {
        super(label, targetCU, invocationNode, binding, relevance, annotation);
        this.attributesToAdd = attributesToAdd;
        this.attributesToRemove = attributesToRemove;
    }
    public ModifyAnnotationProposal(String label, PsiFile targetCU, PsiFile invocationNode,
                                    PsiModifierListOwner binding, int relevance, PsiAnnotation annotation, List<String> attributesToAdd) {
        super(label, targetCU, invocationNode, binding, relevance, annotation);
        this.attributesToAdd = attributesToAdd;
        this.attributesToRemove = new ArrayList<>();
    }
    public ModifyAnnotationProposal(String label, PsiFile targetCU, PsiFile invocationNode,
                                    PsiModifierListOwner binding, int relevance, List<String> attributesToAdd, PsiAnnotation... annotations) {
        super(label, targetCU, invocationNode, binding, relevance, annotations);
        this.attributesToAdd = attributesToAdd;
        this.attributesToRemove = new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Change getChange() {
        PsiFile fInvocationNode = getInvocationNode();
        PsiModifierListOwner fBinding = getBinding();
        PsiAnnotation[] annotations = getAnnotations();

        // get short name of annotations
        String[] annotationShortNames = new String[annotations.length];
        for (int i = 0; i < annotations.length; i++) {
            String qName = annotations[i].getQualifiedName();
            String shortName = qName.substring(qName.lastIndexOf(".") + 1, qName.length());
            annotationShortNames[i] = shortName;
        }

        for (PsiAnnotation annotation : annotations) {
            var parameters = annotation.getParameterList();
            var values = parameters.getAttributes();
            // add new attributes
            for (String newAttr : this.attributesToAdd) {
                // don't add duplicate attributes to an annotation
                if (Arrays.stream(values).noneMatch(v -> v.getName().equals(newAttr))) {
                    annotation.setDeclaredAttributeValue(newAttr, newDefaultExpression(annotation));
                }
            }
        }

        return  new Change(fInvocationNode.getViewProvider().getDocument(), fInvocationNode.getViewProvider().getDocument());
    }

    private PsiAnnotationMemberValue newDefaultExpression(PsiAnnotation annotation) {
        return PsiElementFactory.getInstance(annotation.getProject()).
                createExpressionFromText("\"\"", annotation);
    }
}
