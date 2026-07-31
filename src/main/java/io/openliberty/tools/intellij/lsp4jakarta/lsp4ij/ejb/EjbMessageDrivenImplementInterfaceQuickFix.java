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

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb.EjbConstants.MESSAGE_LISTENER_FQ_NAME;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ImplementInterfaceProposal;
import io.openliberty.tools.intellij.util.ExceptionUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Quick fix for adding MessageListener interface implementation to a class
 * annotated with @MessageDriven.
 *
 * This quick fix:
 * 1. Adds "implements MessageListener" to the class declaration
 * 2. Adds necessary import statements (jakarta.jms.MessageListener, jakarta.jms.Message)
 * 3. Generates the onMessage method stub if not present
 *
 * @see <a href="https://jakarta.ee/specifications/enterprise-beans/4.0/jakarta-enterprise-beans-spec-core-4.0#the-required-message-listener-interface">
 *      Jakarta EE Enterprise Beans Specification - Section 5.4.2</a>
 */
public class EjbMessageDrivenImplementInterfaceQuickFix implements IJavaCodeActionParticipant {
    private static final Logger LOGGER = Logger.getLogger(EjbMessageDrivenImplementInterfaceQuickFix.class.getName());

    @Override
    public String getParticipantId() {
        return EjbMessageDrivenImplementInterfaceQuickFix.class.getName();
    }

    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        List<CodeAction> codeActions = new ArrayList<>();
        PsiElement node = context.getCoveredNode();
        PsiClass parentType = getBinding(node);
        
        if (parentType != null) {
            String title = Messages.getMessage("LetClassImplement",
                    parentType.getName(),
                    "MessageListener");
            codeActions.add(JDTUtils.createCodeAction(context, diagnostic, title, getParticipantId()));
        }
        
        return codeActions;
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        final PsiClass parentType = getBinding(node);

        if (parentType == null) {
            return toResolve;
        }
        
        ChangeCorrectionProposal proposal = new ImplementInterfaceProposal(
                context.getCompilationUnit(), parentType, context.getASTRoot(),
                MESSAGE_LISTENER_FQ_NAME, 0, context.getSource().getCompilationUnit());

        ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER,
                "Unable to create workspace edit for code action to implement MessageListener interface");
        return toResolve;
    }

    private PsiClass getBinding(PsiElement node) {
        return PsiTreeUtil.getParentOfType(node, PsiClass.class);
    }
}