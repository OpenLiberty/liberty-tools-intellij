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

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.ModifyAnnotationAttributesValueProposal;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.util.PsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.util.ExceptionUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Abstract quick fix for modifying annotation attribute values.
 * Subclasses should implement methods to provide the annotation name,
 * new attribute values, and code action label.
 */
public abstract class ModifyAnnotationAttributeValueQuickFix implements IJavaCodeActionParticipant {
    private static final Logger LOGGER = Logger.getLogger(ModifyAnnotationAttributeValueQuickFix.class.getName());

    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        List<CodeAction> codeActions = new ArrayList<>();
        String label = getLabel(context, diagnostic);
        codeActions.add(JDTUtils.createCodeAction(context, diagnostic, label, getParticipantId()));
        return codeActions;
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        String name = toResolve.getTitle();
        PsiElement node = context.getCoveringNode();
        PsiModifierListOwner binding = PsiUtils.getBinding(node);
        PsiAnnotation annotationNode = getAnnotation(node);

        if (annotationNode == null) {
            return toResolve;
        }

        String annotation = getAnnotationName();
        Map<String, PsiAnnotationMemberValue> attributeValues = getNewAttributeValues(context, annotationNode);

        ChangeCorrectionProposal proposal = new ModifyAnnotationAttributesValueProposal(
                name,
                context.getSource().getCompilationUnit(),
                context.getASTRoot(),
                binding,
                annotationNode,
                0,
                annotation,
                attributeValues
        );

        ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER,
                "Unable to create workspace edit for modifying annotation attribute value.");
        return toResolve;
    }

    /**
     * Gets the annotation to modify from the given node.
     *
     * @param node the PSI element
     * @return the annotation to modify
     */
    protected PsiAnnotation getAnnotation(PsiElement node) {
        if (node instanceof PsiAnnotation) {
            return (PsiAnnotation) node;
        }
        return PsiTreeUtil.getParentOfType(node, PsiAnnotation.class);
    }

    /**
     * Returns the fully qualified annotation name to modify.
     *
     * @return the annotation name
     */
    protected abstract String getAnnotationName();

    /**
     * Returns the new attribute values to set on the annotation.
     *
     * @param context the code action resolve context
     * @param annotation the annotation being modified
     * @return map of attribute names to their new values
     */
    protected abstract Map<String, PsiAnnotationMemberValue> getNewAttributeValues(
            JavaCodeActionResolveContext context, PsiAnnotation annotation);

    /**
     * Returns the label for the code action.
     *
     * @param context the code action context
     * @param diagnostic the diagnostic
     * @return the code action label
     */
    protected abstract String getLabel(JavaCodeActionContext context, Diagnostic diagnostic);
}

