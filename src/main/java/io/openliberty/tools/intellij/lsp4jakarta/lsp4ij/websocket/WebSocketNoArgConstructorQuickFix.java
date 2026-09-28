/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Joseph Bineesh
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.websocket;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.AddConstructorProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.util.ExceptionUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * Quick fix for adding a `public` no argument constructor
 * for a websocket endpoint that do not have:
 * - a no argument constructor
 */

public class WebSocketNoArgConstructorQuickFix implements IJavaCodeActionParticipant {
    private static final Logger LOGGER = Logger.getLogger(WebSocketNoArgConstructorQuickFix.class.getName());

    @Override
    public String getParticipantId() {
        return WebSocketNoArgConstructorQuickFix.class.getName();
    }

    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        PsiElement node = context.getCoveredNode();
        PsiClass parentType = getBinding(node);

        if (parentType != null) {
            return addConstructor(diagnostic, context);
        }
        return Collections.emptyList();
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        PsiElement node = context.getCoveredNode();
        PsiClass parentType = getBinding(node);

        String constructorName = toResolve.getTitle();
        ChangeCorrectionProposal proposal = new AddConstructorProposal(constructorName, context.getSource().getCompilationUnit(), context.getASTRoot(), parentType, 0, "public");

        ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER, "Unable to create workspace edit for code actions to add constructors");

        return toResolve;
    }

    protected PsiClass getBinding(PsiElement node) {
        return PsiTreeUtil.getParentOfType(node, PsiClass.class);
    }

    private List<CodeAction> addConstructor(Diagnostic diagnostic, JavaCodeActionContext context) {
        List<CodeAction> codeActions = new ArrayList<>();
        CodeAction codeAction = JDTUtils.createCodeAction(context, diagnostic, Messages.getMessage("AddPublicConstructor"), getParticipantId());
        codeActions.add(codeAction);
        return codeActions;
    }
}