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

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.RemoveMethodProposal;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb.EjbConstants.FINALIZE_METHOD_NAME;
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
 * QuickFix for removing the finalize() method from session beans.
 */
public class RemoveFinalizeMethodQuickFix implements IJavaCodeActionParticipant {

    private static final String NAME = Messages.getMessage("RemoveFinalizeMethod");
    private static final Logger LOGGER = Logger.getLogger(RemoveFinalizeMethodQuickFix.class.getName());

    @Override
    public String getParticipantId() {
        return RemoveFinalizeMethodQuickFix.class.getName();
    }

    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        List<CodeAction> codeActions = new ArrayList<>();
        final PsiElement node = context.getCoveredNode();
        final PsiMethod parentMethod = PsiTreeUtil.getParentOfType(node, PsiMethod.class);
        
        if (parentMethod != null && FINALIZE_METHOD_NAME.equals(parentMethod.getName()) &&
                parentMethod.getParameterList().getParametersCount() == 0) {
            codeActions.add(JDTUtils.createCodeAction(context, diagnostic, NAME, getParticipantId()));
        }
        return codeActions;
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        final PsiClass parentType = PsiTreeUtil.getParentOfType(node, PsiClass.class);
        final PsiMethod parentMethod = PsiTreeUtil.getParentOfType(node, PsiMethod.class);

        if (parentMethod != null && FINALIZE_METHOD_NAME.equals(parentMethod.getName())) {
            ChangeCorrectionProposal proposal = new RemoveMethodProposal(NAME,
                    context.getSource().getCompilationUnit(),
                    context.getASTRoot(),
                    parentType,
                    0,
                    Collections.singletonList(parentMethod));

            ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER,
                    "Unable to create workspace edit for removing finalize() method");
        }
        return toResolve;
    }
}