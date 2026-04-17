/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.di;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.RemoveAnnotationAttributesProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.util.ExceptionUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Quick fix for removing all attributes from @Scope annotated interfaces.
 *
 * <p>Jakarta EE Dependency Injection specification states that scope annotations
 * must not declare attributes. This quick fix removes all methods and fields
 * from annotation interfaces annotated with @Scope.</p>
 */
public class RemoveScopeAttributesQuickFix implements IJavaCodeActionParticipant {

    private static final Logger LOGGER = Logger.getLogger(RemoveScopeAttributesQuickFix.class.getName());
    private static final String LABEL = "RemoveScopeAttributes";

    @Override
    public String getParticipantId() {
        return RemoveScopeAttributesQuickFix.class.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        PsiElement node = context.getCoveredNode();
        PsiClass annotationClass = PsiTreeUtil.getParentOfType(node, PsiClass.class);

        if (annotationClass != null && annotationClass.isAnnotationType()) {
            List<CodeAction> codeActions = new ArrayList<>();
            codeActions.add(JDTUtils.createCodeAction(context, diagnostic,
                    Messages.getMessage(LABEL), getParticipantId()));
            return codeActions;
        }
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     *
     * Resolves the code action by removing all attributes (methods and fields)
     * from the @Scope annotated interface.
     */
    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        final PsiClass annotationClass = PsiTreeUtil.getParentOfType(node, PsiClass.class);
        if (annotationClass != null && annotationClass.isAnnotationType()) {
            List<PsiElement> attributesToRemove = new ArrayList<>();
            attributesToRemove.addAll(Arrays.asList(annotationClass.getMethods()));
            attributesToRemove.addAll(Arrays.asList(annotationClass.getFields()));
            if (!attributesToRemove.isEmpty()) {
                final String label = Messages.getMessage(LABEL);
                final ChangeCorrectionProposal proposal = new RemoveAnnotationAttributesProposal(
                        label,
                        context.getSource().getCompilationUnit(),
                        context.getASTRoot(),
                        annotationClass,
                        0,
                        attributesToRemove
                );
                ExceptionUtil.executeWithWorkspaceEditHandling(
                        context, proposal, toResolve, LOGGER,
                        "Unable to create workspace edit for removing @Scope attributes"
                );
            }
        }
        return toResolve;
    }
}
