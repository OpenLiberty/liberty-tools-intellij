/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation, Matthew Shocrylas and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation, Matthew Shocrylas - initial API and implementation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jax_rs;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.ModifyModifiersProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.util.ExceptionUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Quick fix for ResourceMethodDiagnosticsCollector that uses
 * ModifyModifiersProposal.
 * 
 * @author Matthew Shocrylas
 *
 */
public class NonPublicResourceMethodQuickFix implements IJavaCodeActionParticipant {

    private static final Logger LOGGER = Logger.getLogger(NonPublicResourceMethodQuickFix.class.getName());

    private final static String TITLE_MESSAGE = Messages.getMessage("MakeMethodPublic");

    @Override
    public String getParticipantId() {
        return NonPublicResourceMethodQuickFix.class.getName();
    }

    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic)  {

        final PsiElement node = context.getCoveredNode();
        final PsiMethod parentMethod = PsiTreeUtil.getParentOfType(node, PsiMethod.class);

        if (parentMethod != null) {
            return Collections.singletonList(JDTUtils.createCodeAction(context, diagnostic, TITLE_MESSAGE,
                    getParticipantId()));
        }
        return Collections.emptyList();
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {

        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        final PsiClass parentType = PsiTreeUtil.getParentOfType(node, PsiClass.class);
        final PsiMethod parentMethod = PsiTreeUtil.getParentOfType(node, PsiMethod.class);

        assert parentMethod != null;
        ChangeCorrectionProposal proposal = new ModifyModifiersProposal(TITLE_MESSAGE, context.getSource().getCompilationUnit(),
                context.getASTRoot(), parentType, 0, parentMethod.getModifierList(), Collections.singletonList("public"));

        ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER, "Unable to create workspace edit for code action to make method public");
        return toResolve;
    }
}
