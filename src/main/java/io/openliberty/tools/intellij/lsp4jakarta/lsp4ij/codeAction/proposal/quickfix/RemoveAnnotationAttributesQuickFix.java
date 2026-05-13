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

    private final String annotation;

    private static final Logger LOGGER = Logger.getLogger(RemoveAnnotationAttributesQuickFix.class.getName());


    public RemoveAnnotationAttributesQuickFix(String annotation,
                                              String... attributes) {
        this.annotation = annotation;
        this.attributes = attributes;
    }

    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        return List.of(JDTUtils.createCodeAction(context, diagnostic, getLabel(), getParticipantId()));
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        PsiModifierListOwner binding = getBinding(node);
        PsiAnnotation annotationNode = PsiTreeUtil.getParentOfType(node, PsiAnnotation.class);
        String label = getLabel();
        ChangeCorrectionProposal proposal = new ModifyAnnotationProposal(label, context.getSource().getCompilationUnit(),
                context.getASTRoot(), binding, annotationNode, 0, this.annotation, new ArrayList<>(), Arrays.asList(attributes));
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

    protected abstract String getLabel();

}
