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

    private record AnnotationInfo(PsiAnnotation annotation, PsiModifierListOwner binding) {
    }

    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        PsiElement node = context.getCoveredNode();
        
        // Try to find annotation in method parameters first (for @Observes/@ObservesAsync)
        AnnotationInfo info = findAnnotationWithBinding(node);
        
        if (info != null) {
            String label = getLabel(info.annotation.getQualifiedName(), attributes);
            return List.of(JDTUtils.createCodeAction(context, diagnostic, label, getParticipantId()));
        }
        
        // Fall back to direct annotation lookup (for @Resource on fields/variables)
        PsiAnnotation annotationNode = PsiTreeUtil.getParentOfType(node, PsiAnnotation.class);
        if (annotationNode != null && isTargetAnnotation(annotationNode)) {
            String label = getLabel(annotationNode.getQualifiedName(), attributes);
            return List.of(JDTUtils.createCodeAction(context, diagnostic, label, getParticipantId()));
        }
        
        return List.of();
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        String label = toResolve.getTitle();
        
        // Try to find annotation in method parameters first (for @Observes/@ObservesAsync)
        AnnotationInfo info = findAnnotationWithBinding(node);
        
        if (info != null) {
            ChangeCorrectionProposal proposal = new ModifyAnnotationProposal(label, context.getSource().getCompilationUnit(),
                    context.getASTRoot(), info.binding, info.annotation, 0, null, new ArrayList<>(), Arrays.asList(attributes));
            ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER, "Unable to create workspace edit for code action " + label);
            return toResolve;
        }
        
        // Fall back to direct annotation lookup (for @Resource on fields/variables)
        PsiModifierListOwner binding = getBinding(node);
        PsiAnnotation annotationNode = PsiTreeUtil.getParentOfType(node, PsiAnnotation.class);
        
        if (binding != null && annotationNode != null) {
            // For single annotation mode, use the annotation name; for multiple, use null
            String targetAnnotation = annotations.length == 1 ? annotations[0] : null;
            ChangeCorrectionProposal proposal = new ModifyAnnotationProposal(label, context.getSource().getCompilationUnit(),
                    context.getASTRoot(), binding, annotationNode, 0, targetAnnotation, new ArrayList<>(), Arrays.asList(attributes));
            ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER, "Unable to create workspace edit for code action " + label);
        }
        
        return toResolve;
    }

    /**
     * Finds the target annotation with its binding.
     * Searches in method parameters for annotations matching the criteria.
     * Used primarily for @Observes/@ObservesAsync annotations on method parameters.
     *
     * @param node the PSI element to start searching from
     * @return AnnotationInfo containing the annotation and its binding, or null if not found
     */
    private AnnotationInfo findAnnotationWithBinding(PsiElement node) {
        PsiMethod method = PsiTreeUtil.getParentOfType(node, PsiMethod.class);
        if (method != null) {
            for (PsiParameter param : method.getParameterList().getParameters()) {
                for (PsiAnnotation annotation : param.getAnnotations()) {
                    if (isTargetAnnotation(annotation)) {
                        return new AnnotationInfo(annotation, param);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Finds the nearest PsiModifierListOwner parent of the given node.
     * Searches in priority order: PsiVariable, PsiMethod, then PsiClass.
     * Used primarily for @Resource annotations on fields/variables.
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
     * Checks if the annotation is a target annotation that should have its attributes removed.
     * Default implementation checks if the annotation's qualified name matches any of the configured annotations.
     * Subclasses can override this to add additional filtering logic.
     *
     * @param annotation the annotation to check
     * @return true if it's a target annotation
     */
    protected boolean isTargetAnnotation(PsiAnnotation annotation) {
        String qualifiedName = annotation.getQualifiedName();
        return qualifiedName != null && Arrays.asList(annotations).contains(qualifiedName);
    }

    /**
     * Returns the label for the code action.
     *
     * @param annotation The fully qualified annotation name
     * @param attributes The attributes to remove
     * @return The label for the code action
     */
    protected abstract String getLabel(String annotation, String[] attributes);

    /**
     * Gets the annotations array.
     *
     * @return the annotations array
     */
    protected String[] getAnnotations() {
        return annotations;
    }

    /**
     * Gets the attributes array.
     *
     * @return the attributes array
     */
    protected String[] getAttributes() {
        return attributes;
    }

}
