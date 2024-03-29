/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copied from https://github.com/eclipse/lsp4mp/blob/6f2d700a88a3262e39cc2ba04beedb429e162246/microprofile.jdt/org.eclipse.lsp4mp.jdt.core/src/main/java/org/eclipse/lsp4mp/jdt/core/java/corrections/proposal/NewAnnotationProposal.java
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal;

import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.Change;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeActionKind;

/**
 * Code action proposal for annotation reused from
 * https://github.com/eclipse/lsp4mp/blob/6f2d700a88a3262e39cc2ba04beedb429e162246/microprofile.jdt/org.eclipse.lsp4mp.jdt.core/src/main/java/org/eclipse/lsp4mp/jdt/core/java/corrections/proposal/NewAnnotationProposal.java
 */
public class NewAnnotationProposal extends ChangeCorrectionProposal {

    private final PsiFile fInvocationNode;
    private final PsiModifierListOwner fBinding;
    private PsiAnnotation fAnnotation;

    protected final String[] annotations;

    public NewAnnotationProposal(String label, PsiFile targetCU, PsiFile invocationNode, PsiModifierListOwner binding,
                                 PsiAnnotation annotation, int relevance, String... annotations) {
        super(label, CodeActionKind.QuickFix, relevance);
        fInvocationNode = invocationNode;
        fBinding = binding;
        fAnnotation = annotation;
        this.annotations = annotations;
    }

    @Override
    public Change getChange() {
        return null;
    }

    public void performUpdate() {
        for(String annotation : annotations) {
            PsiClass annotationClass = JavaPsiFacade.getInstance(fBinding.getProject()).
                    findClass(annotation, GlobalSearchScope.allScope(fBinding.getProject()));
            if (annotationClass != null) {
                // Need to update this if 'annotations' is ever longer than 1.
                fAnnotation = fBinding.getModifierList().addAnnotation(annotationClass.getName());
                if (fBinding.getContainingFile() instanceof PsiJavaFile) {
                    ((PsiJavaFile) fBinding.getContainingFile()).getImportList().
                            add(PsiElementFactory.getInstance(fBinding.getProject()).
                                    createImportStatement(annotationClass));
                }
            }
        }
    }
    /**
     * Returns the Compilation Unit node
     *
     * @return the invocation node for the Compilation Unit
     */
    protected PsiFile getInvocationNode() {
        return this.fInvocationNode;
    }

    /**
     * Returns the Binding object associated with the new annotation change
     *
     * @return the binding object
     */
    protected PsiModifierListOwner getBinding() {
        return this.fBinding;
    }

    /**
     * Returns the Annotation object associated with the new annotation change
     *
     * @return the model object representing the annotation (existing or to be created)
     */
    protected PsiAnnotation getAnnotation() {
        return this.fAnnotation;
    }

    /**
     * Returns the annotations list
     *
     * @return the list of new annotations to add
     */
    protected String[] getAnnotations() {
        return this.annotations;
    }
}
