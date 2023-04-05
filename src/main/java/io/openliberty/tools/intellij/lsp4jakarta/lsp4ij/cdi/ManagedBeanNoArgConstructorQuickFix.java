/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hani Damlaj
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.AddConstructorProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Quick fix for adding a `protected`/`public` no argument constructor
 * for a managed bean that do not have:
 * - a no argument constructor
 * - a constructor annotated with `@Inject`
 *
 */

public class ManagedBeanNoArgConstructorQuickFix {

    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        PsiElement node = context.getCoveredNode();
        PsiClass parentType = getBinding(node);
        if (parentType != null) {
            List<CodeAction> codeActions = new ArrayList<>();

            codeActions.addAll(addConstructor(diagnostic, context, parentType));

            return codeActions;
        }
        return null;
    }

    protected PsiClass getBinding(PsiElement node) {
        return PsiTreeUtil.getParentOfType(node, PsiClass.class);
    }

    private List<CodeAction> addConstructor(Diagnostic diagnostic, JavaCodeActionContext context, PsiClass notUsed) {
        List<CodeAction> codeActions = new ArrayList<>();
        JavaCodeActionContext targetContext = null;
        PsiElement node = null;
        PsiClass parentType = null;

        // option for protected constructor
        targetContext = context.copy();
        node = targetContext.getCoveredNode();
        parentType = getBinding(node);
        String name = "Add a no-arg protected constructor to this class";
        ChangeCorrectionProposal proposal = new AddConstructorProposal(name,
                targetContext.getCompilationUnit(), targetContext.getASTRoot(), parentType, 0);
        CodeAction codeAction = targetContext.convertToCodeAction(proposal, diagnostic);

        if (codeAction != null) {
            codeActions.add(codeAction);
        }

        // option for public constructor
        targetContext = context.copy();
        node = targetContext.getCoveredNode();
        parentType = getBinding(node);
        name = "Add a no-arg public constructor to this class";
        proposal = new AddConstructorProposal(name,
                targetContext.getCompilationUnit(), targetContext.getASTRoot(), parentType, 0, "public");
        codeAction = targetContext.convertToCodeAction(proposal, diagnostic);

        if (codeAction != null) {
            codeActions.add(codeAction);
        }

        return codeActions;
    }
}
