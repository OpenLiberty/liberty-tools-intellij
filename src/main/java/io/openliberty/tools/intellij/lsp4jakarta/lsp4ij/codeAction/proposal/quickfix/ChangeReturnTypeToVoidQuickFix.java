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
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.ModifyReturnTypeProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.util.ExceptionUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Generic quick fix that changes the return type of a method to void.
 */
public class ChangeReturnTypeToVoidQuickFix implements IJavaCodeActionParticipant {

    private static final Logger LOGGER = Logger.getLogger(ChangeReturnTypeToVoidQuickFix.class.getName());
    private static final String TITLE_MESSAGE = Messages.getMessage("ChangeReturnTypeToVoid");

    @Override
    public String getParticipantId() {
        return ChangeReturnTypeToVoidQuickFix.class.getName();
    }

    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        List<CodeAction> codeActions = new ArrayList<>();
        final PsiElement node = context.getCoveredNode();
        final PsiMethod parentType = getBinding(node);
        if (parentType != null) {
            codeActions.add(JDTUtils.createCodeAction(context, diagnostic, TITLE_MESSAGE, getParticipantId()));
        }
        return codeActions;
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        final PsiMethod parentType = getBinding(node);

        assert parentType != null;
        ChangeCorrectionProposal proposal = new ModifyReturnTypeProposal(TITLE_MESSAGE,
                context.getSource().getCompilationUnit(),
                context.getASTRoot(), parentType, 0, PsiTypes.voidType());

        ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER,
                "Unable to create workspace edit for code action to change return type to void");
        return toResolve;
    }

    protected PsiMethod getBinding(PsiElement node) {
        if (node instanceof PsiMethod) {
            return (PsiMethod) node;
        }
        return PsiTreeUtil.getParentOfType(node, PsiMethod.class);
    }
}
