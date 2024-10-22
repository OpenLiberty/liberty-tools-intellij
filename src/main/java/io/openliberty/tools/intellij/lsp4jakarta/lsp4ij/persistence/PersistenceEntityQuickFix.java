/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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
import org.eclipse.lsp4j.WorkspaceEdit;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
public class PersistenceEntityQuickFix implements IJavaCodeActionParticipant {
    private static final Logger LOGGER = Logger.getLogger(PersistenceEntityQuickFix.class.getName());

    @Override
    public String getParticipantId() {
        return PersistenceEntityQuickFix.class.getName();
    }

    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        List<CodeAction> codeActions = new ArrayList<>();
        PsiElement node = context.getCoveredNode();
        PsiClass parentType = getBinding(node);

        if (parentType != null) {
            codeActions.addAll(addConstructor(diagnostic, context));
        }
        return codeActions;
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        PsiElement node = context.getCoveredNode();
        PsiClass parentType = getBinding(node);

        String constructorName = toResolve.getTitle();
        ChangeCorrectionProposal proposal = new AddConstructorProposal(constructorName,
                context.getSource().getCompilationUnit(), context.getASTRoot(), parentType, 0,
                constructorName.equals(Messages.getMessage("AddNoArgProtectedConstructor")) ? "protected" : "public");

        Boolean success = ExceptionUtil.executeWithExceptionHandling(
                () -> {
                    WorkspaceEdit we = context.convertToWorkspaceEdit(proposal);
                    toResolve.setEdit(we);
                    return true;
                },
                e -> LOGGER.log(Level.WARNING, "Unable to create workspace edit for code actions to add constructors", e)
        );
        if (success == null || !success) {
            System.out.println("An error occurred during the code action resolution.");
        }
        return toResolve;
    }

    protected PsiClass getBinding(PsiElement node) {
        return PsiTreeUtil.getParentOfType(node, PsiClass.class);
    }

    private List<CodeAction> addConstructor(Diagnostic diagnostic, JavaCodeActionContext context) {
        List<CodeAction> codeActions = new ArrayList<>();
        String[] constructorNames = {Messages.getMessage("AddNoArgProtectedConstructor"), Messages.getMessage("AddNoArgPublicConstructor")};

        for (String name : constructorNames) {
            CodeAction codeAction = JDTUtils.createCodeAction(context, diagnostic, name, getParticipantId());
            codeActions.add(codeAction);
        }
        return codeActions;
    }
}