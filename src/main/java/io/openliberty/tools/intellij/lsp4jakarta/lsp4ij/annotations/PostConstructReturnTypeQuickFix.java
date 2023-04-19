/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Yijia Jing
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.annotations;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.ModifyReturnTypeProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.List;

/**
 * Quick fix for AnnotationDiagnosticsCollector that changes the return type of a method to void.
 * Uses ModifyReturnTypeProposal.
 *
 * @author Yijia Jing
 *
 */
public class PostConstructReturnTypeQuickFix {
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        List<CodeAction> codeActions = new ArrayList<>();
        PsiElement node = context.getCoveredNode();
        PsiMethod parentType = getBinding(node);
        String name = "Change return type to void";
        ChangeCorrectionProposal proposal = new ModifyReturnTypeProposal(name, context.getSource().getCompilationUnit(),
                context.getASTRoot(), parentType, 0, PsiPrimitiveType.VOID);
        CodeAction codeAction = context.convertToCodeAction(proposal, diagnostic);
        codeActions.add(codeAction);
        return codeActions;
    }

    protected PsiMethod getBinding(PsiElement node) {
        if (node instanceof PsiMethod) {
            return (PsiMethod) node;
        }
        return PsiTreeUtil.getParentOfType(node, PsiMethod.class);
    }
}
