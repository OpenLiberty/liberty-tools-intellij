/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation, Shaunak Tulshibagwale and others.
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

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.AddConstructorProposal;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.ModifyModifiersProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.WorkspaceEdit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quick fix for NoResourcePublicConstructorQuickFix that uses
 * ModifyModifiersProposal.
 *
 * @author Shaunak Tulshibagwale
 */
public class NoResourcePublicConstructorQuickFix implements IJavaCodeActionParticipant {

    private static final Logger LOGGER = Logger.getLogger(NoResourcePublicConstructorQuickFix.class.getName());

    @Override
    public String getParticipantId() {
        return NoResourcePublicConstructorQuickFix.class.getName();
    }

    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {

        PsiElement node = context.getCoveredNode();
        PsiMethod parentMethod = PsiTreeUtil.getParentOfType(node, PsiMethod.class);

        if (parentMethod != null) {
            List<CodeAction> codeActions = new ArrayList<>();

            codeActions.add(JDTUtils.createCodeAction(context, diagnostic, Messages.getMessage("MakeConstructorPublic"), getParticipantId()));
            codeActions.add(JDTUtils.createCodeAction(context, diagnostic, Messages.getMessage("NoargPublicConstructor"), getParticipantId()));

            return codeActions;
        }
        return Collections.emptyList();
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {

        final CodeAction toResolve = context.getUnresolved();
        PsiElement node = context.getCoveredNode();
        PsiMethod parentMethod = PsiTreeUtil.getParentOfType(node, PsiMethod.class);
        PsiClass parentType = PsiTreeUtil.getParentOfType(node, PsiClass.class);

        if (parentMethod != null) {

            String message = toResolve.getTitle();

            if (message.equals(Messages.getMessage("MakeConstructorPublic"))) {

                ChangeCorrectionProposal proposal = new ModifyModifiersProposal(message, context.getSource().getCompilationUnit(),
                        context.getASTRoot(), parentType, 0, parentMethod.getModifierList(), Collections.singletonList("public"));

                String warningMessage = "Unable to create workspace edit for code action to make constructor public";
                convertWorkspaceEdit(proposal, warningMessage, context);

            } else if (message.equals(Messages.getMessage("NoargPublicConstructor"))) {

                ChangeCorrectionProposal proposal = new AddConstructorProposal(message,
                        context.getSource().getCompilationUnit(), context.getASTRoot(), parentType, 0, "public");

                String warningMessage = "Unable to create workspace edit for code action to add no-arg public constructor to the class";
                convertWorkspaceEdit(proposal, warningMessage, context);

            }
        }
        return toResolve;
    }

    public void convertWorkspaceEdit(ChangeCorrectionProposal proposal, String warningMessage, JavaCodeActionResolveContext context) {
        try {
            WorkspaceEdit we = context.convertToWorkspaceEdit(proposal);
            context.getUnresolved().setEdit(we);
        } catch (ProcessCanceledException e) {
            //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
            //TODO delete block when minimum required version is 2024.2
            throw e;
        } catch (IndexNotReadyException | CancellationException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, warningMessage, e);
        }
    }
}
