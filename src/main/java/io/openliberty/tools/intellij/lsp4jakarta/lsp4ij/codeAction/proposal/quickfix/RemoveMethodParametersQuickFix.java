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
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.RemoveParamsProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Quickfix for removing all parameters from a method
 * 
 * @author Zijian Pei
 */
public class RemoveMethodParametersQuickFix {

    private static final String NAME = "Remove all parameters";

    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {

        final PsiElement node = context.getCoveredNode();
        final PsiClass parentType = PsiTreeUtil.getParentOfType(node, PsiClass.class);
        final PsiMethod parentMethod = PsiTreeUtil.getParentOfType(node, PsiMethod.class);

        if (parentMethod != null) {
            final PsiParameterList parameterList = parentMethod.getParameterList();
            if (parameterList != null && parameterList.getParametersCount() > 0) {
                ChangeCorrectionProposal proposal = new RemoveParamsProposal(NAME, context.getSource().getCompilationUnit(),
                        context.getASTRoot(), parentType, 0, Arrays.asList(parameterList.getParameters()));
                CodeAction codeAction = context.convertToCodeAction(proposal, diagnostic);
                return Collections.singletonList(codeAction);
            }
        }
        return Collections.emptyList();
    }
}
