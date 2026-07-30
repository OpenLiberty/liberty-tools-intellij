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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.UpdateAnnotationTextProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.util.ExceptionUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.NAMED_FQ_NAME;

/**
 * Quick fix for adding 'value' attribute to @Named annotation on constructor or method parameters.
 * Generates explicit syntax: @Named(value = "")
 *
 * According to CDI specification, @Named on non-field injection points must specify a value.
 *
 * https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0#named_at_injection_point
 */
public class InsertNamedValueAttributeQuickFix implements IJavaCodeActionParticipant {

    private static final Logger LOGGER = Logger.getLogger(InsertNamedValueAttributeQuickFix.class.getName());

    @Override
    public String getParticipantId() {
        return InsertNamedValueAttributeQuickFix.class.getName();
    }

    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        List<CodeAction> codeActions = new ArrayList<>();
        String label = Messages.getMessage("InsertNamedValueAttribute");
        codeActions.add(JDTUtils.createCodeAction(context, diagnostic, label, getParticipantId()));
        return codeActions;
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        
        // Find the @Named annotation
        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(node, PsiAnnotation.class);
        if (annotation == null) {
            return toResolve;
        }

        String label = Messages.getMessage("InsertNamedValueAttribute");
        
        // Create proposal that replaces @Named with @Named(value = "")
        ChangeCorrectionProposal proposal = new UpdateAnnotationTextProposal(
                label,
                context.getSource().getCompilationUnit(),
                context.getASTRoot(),
                annotation,
                0,
                "@Named(value = \"\")"
        );

        ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER,
                "Unable to create workspace edit for code action " + label);
        return toResolve;
    }
}
