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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence;


import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.AddConstructorProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.List;

/**
 * QuickFix for fixing {@link PersistenceConstants#DIAGNOSTIC_CODE_MISSING_ATTRIBUTES} error
 * by providing several code actions to remove incorrect modifiers or add missing constructor:
 *
 * {@link PersistenceConstants#DIAGNOSTIC_CODE_MISSING_EMPTY_CONSTRUCTOR}
 * <ul>
 * <li> Add a (no-arg) void constructor to this class if the class has other constructors
 * which do not conform to this
 * </ul>
 *
 * {@link PersistenceConstants#DIAGNOSTIC_CODE_FINAL_METHODS}
 * <ul>
 * <li> Remove the FINAL modifier from all methods in this class
 * </ul>
 *
 * {@link PersistenceConstants#DIAGNOSTIC_CODE_FINAL_VARIABLES}
 * <ul>
 * <li> Remove the FINAL modifier from all variables in this class
 * </ul>
 *
 * {@link PersistenceConstants#DIAGNOSTIC_CODE_FINAL_CLASS}
 * <ul>
 * <li> Remove the FINAL modifier from this class
 * </ul>
 *
 * @author Leslie Dawson (lamminade)
 *
 */
public class PersistenceEntityQuickFix {
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        PsiElement node = context.getCoveredNode();
        PsiClass parentType = getBinding(node);
        if (parentType != null) {
            List<CodeAction> codeActions = new ArrayList<>();

            // add constructor
            if (diagnostic.getCode().getLeft().equals(PersistenceConstants.DIAGNOSTIC_CODE_MISSING_EMPTY_CONSTRUCTOR)) {
                codeActions.addAll(addConstructor(diagnostic, context, parentType));
            }

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
