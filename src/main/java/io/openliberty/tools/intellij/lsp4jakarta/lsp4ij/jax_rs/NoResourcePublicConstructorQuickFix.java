/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation, Shaunak Tulshibagwale and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation, Shaunak Tulshibagwale
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jax_rs;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.AddConstructorProposal;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.ModifyModifiersProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Quick fix for NoResourcePublicConstructorQuickFix that uses
 * ModifyModifiersProposal.
 * 
 * @author Shaunak Tulshibagwale
 *
 */
public class NoResourcePublicConstructorQuickFix {

    private final static String TITLE_MESSAGE = "Make constructor public";

    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {

        PsiElement node = context.getCoveredNode();
        PsiMethod parentMethod = PsiTreeUtil.getParentOfType(node, PsiMethod.class);
        
        if (parentMethod != null) {
            List<CodeAction> codeActions = new ArrayList<>();

            JavaCodeActionContext targetContext = context.copy();
            node = targetContext.getCoveredNode();
            PsiClass parentType = PsiTreeUtil.getParentOfType(node, PsiClass.class);
            parentMethod = PsiTreeUtil.getParentOfType(node, PsiMethod.class);

            ChangeCorrectionProposal proposal = new ModifyModifiersProposal(TITLE_MESSAGE, targetContext.getSource().getCompilationUnit(),
                    targetContext.getASTRoot(), parentType, 0, parentMethod.getModifierList(), Collections.singletonList("public"));

            // Convert the proposal to LSP4J CodeAction
            CodeAction codeAction = targetContext.convertToCodeAction(proposal, diagnostic);
            codeAction.setTitle(TITLE_MESSAGE);
            codeActions.add(codeAction);

            final PsiParameterList list = parentMethod.getParameterList();
            if (list != null && list.getParametersCount() > 0) {
                targetContext = context.copy();
                node = targetContext.getCoveredNode();
                parentType = PsiTreeUtil.getParentOfType(node, PsiClass.class);

                final String name = "Add a no-arg public constructor to this class";
                proposal = new AddConstructorProposal(name,
                        targetContext.getSource().getCompilationUnit(), targetContext.getASTRoot(), parentType, 0, "public");
                codeAction = targetContext.convertToCodeAction(proposal, diagnostic);
                codeActions.add(codeAction);
            }
            return codeActions;
        }
        return Collections.emptyList();
    }
}
