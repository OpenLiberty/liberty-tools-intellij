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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.ModifyAnnotationProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.util.ExceptionUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Quickfix for removing annotations attributes
 */
public abstract class RemoveAnnotationAttributesQuickFix implements IJavaCodeActionParticipant {

    private final String[] attributes;

    private final String[] annotations;

    private static final Logger LOGGER = Logger.getLogger(RemoveAnnotationAttributesQuickFix.class.getName());


    public RemoveAnnotationAttributesQuickFix(String[] annotations,
                                              String... attributes) {
        this.annotations = annotations;
        this.attributes = attributes;
    }

    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        PsiElement node = context.getCoveredNode();
        
        // The diagnostic is on the method, but the annotation is on a parameter
        // First try to find annotation as parent of current node
        PsiAnnotation annotationNode = PsiTreeUtil.getParentOfType(node, PsiAnnotation.class);
        String foundAnnotation = null;
        
        if (annotationNode != null && annotationNode.getQualifiedName() != null) {
            String qualifiedName = annotationNode.getQualifiedName();
            if (annotations != null && Arrays.asList(annotations).contains(qualifiedName)) {
                foundAnnotation = qualifiedName;
            }
        }
        
        // If not found, check if we're on a method and search its parameters
        if (foundAnnotation == null) {
            PsiMethod method = PsiTreeUtil.getParentOfType(node, PsiMethod.class);
            if (method != null) {
                for (PsiParameter param : method.getParameterList().getParameters()) {
                    for (PsiAnnotation annotation : param.getAnnotations()) {
                        String qualifiedName = annotation.getQualifiedName();
                        if (qualifiedName != null && annotations != null && Arrays.asList(annotations).contains(qualifiedName)) {
                            foundAnnotation = qualifiedName;
                            break;
                        }
                    }
                    if (foundAnnotation != null) break;
                }
            }
        }
        
        String label = getLabel(foundAnnotation, attributes);
        return List.of(JDTUtils.createCodeAction(context, diagnostic, label, getParticipantId()));
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        
        // The diagnostic is on the method, but the annotation is on a parameter
        // First try to find annotation as parent of current node
        PsiAnnotation annotationNode = PsiTreeUtil.getParentOfType(node, PsiAnnotation.class);
        PsiModifierListOwner binding = null;
        
        // If not found as parent, search method parameters
        if (annotationNode == null) {
            PsiMethod method = PsiTreeUtil.getParentOfType(node, PsiMethod.class);
            if (method != null) {
                for (PsiParameter param : method.getParameterList().getParameters()) {
                    for (PsiAnnotation annotation : param.getAnnotations()) {
                        String qualifiedName = annotation.getQualifiedName();
                        if (qualifiedName != null && annotations != null && Arrays.asList(annotations).contains(qualifiedName)) {
                            annotationNode = annotation;
                            binding = param;
                            break;
                        }
                    }
                    if (annotationNode != null) break;
                }
            }
        }
        
        // If we found annotation as parent, get the binding
        if (binding == null) {
            binding = getBinding(node);
        }
        
        String label = toResolve.getTitle();
        
        // Pass null for annotation parameter since we already have the annotationNode
        // This tells ModifyAnnotationProposal to use the existing annotation, not create a new one
        ChangeCorrectionProposal proposal = new ModifyAnnotationProposal(label, context.getSource().getCompilationUnit(),
                context.getASTRoot(), binding, annotationNode, 0, null, new ArrayList<>(), Arrays.asList(attributes));
        ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER, "Unable to create workspace edit for code action " + label);
        return toResolve;
    }

    /**
     * Finds the nearest PsiModifierListOwner parent of the given node.
     * Searches in priority order: PsiVariable, PsiMethod, then PsiClass.
     *
     * @param node the PSI element to start searching from
     * @return the nearest PsiModifierListOwner parent, or null if none found
     */
    protected static PsiModifierListOwner getBinding(PsiElement node) {
        return Stream.<Class<? extends PsiModifierListOwner>>of(
                        PsiVariable.class,
                        PsiMethod.class,
                        PsiClass.class
                )
                .map(clazz -> PsiTreeUtil.getParentOfType(node, clazz))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the label for the code action.
     *
     * @param annotation The fully qualified annotation name (or null if not found)
     * @param attributes The attributes to remove
     * @return The label for the code action
     */
    protected abstract String getLabel(String annotation, String[] attributes);

}