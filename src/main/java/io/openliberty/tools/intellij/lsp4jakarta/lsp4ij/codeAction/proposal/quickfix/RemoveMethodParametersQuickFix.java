/*******************************************************************************
* Copyright (c) 2021, 2023 IBM Corporation and others.
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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.RemoveParamsProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.ExtendedCodeAction;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4mp.commons.CodeActionResolveData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quickfix for removing all parameters from a method
 * 
 * @author Zijian Pei
 */
public class RemoveMethodParametersQuickFix implements IJavaCodeActionParticipant {

    private static final String NAME = Messages.getMessage("RemoveAllParameters");
    private static final Logger LOGGER = Logger.getLogger(RemoveMethodParametersQuickFix.class.getName());

    @Override
    public String getParticipantId() {
        return RemoveMethodParametersQuickFix.class.getName();
    }

    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {

        List<CodeAction> codeActions = new ArrayList<>();
        final PsiElement node = context.getCoveredNode();
        final PsiMethod parentMethod = PsiTreeUtil.getParentOfType(node, PsiMethod.class);
        if (parentMethod != null) {
            codeActions.add(createCodeAction(context, diagnostic));
        }
        return codeActions;
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        final PsiClass parentType = PsiTreeUtil.getParentOfType(node, PsiClass.class);
        final PsiMethod parentMethod = PsiTreeUtil.getParentOfType(node, PsiMethod.class);

        assert parentMethod != null;
        final PsiParameterList parameterList = parentMethod.getParameterList();
        ChangeCorrectionProposal proposal = new RemoveParamsProposal(NAME, context.getSource().getCompilationUnit(),
                context.getASTRoot(), parentType, 0, Arrays.asList(parameterList.getParameters()), false);
        try {
            WorkspaceEdit we = context.convertToWorkspaceEdit(proposal);
            toResolve.setEdit(we);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to create workspace edit for code action", e);
        }
        return toResolve;
    }

    private CodeAction createCodeAction(JavaCodeActionContext context, Diagnostic diagnostic) {
        ExtendedCodeAction codeAction = new ExtendedCodeAction(NAME);
        codeAction.setRelevance(0);
        codeAction.setDiagnostics(Collections.singletonList(diagnostic));
        codeAction.setKind(CodeActionKind.QuickFix);
        codeAction.setData(new CodeActionResolveData(context.getUri(), getParticipantId(),
                context.getParams().getRange(), Collections.emptyMap(),
                context.getParams().isResourceOperationSupported(),
                context.getParams().isCommandConfigurationUpdateSupported()));
        return codeAction;
    }
}
