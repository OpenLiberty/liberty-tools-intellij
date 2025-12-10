/*******************************************************************************
* Copyright (c) 2025 IBM Corporation and others.
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.ModifyThrownExceptionsProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.util.ExceptionUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4mp.commons.codeaction.CodeActionResolveData;

import java.util.*;
import java.util.logging.Logger;

/**
 * Quickfix for removing all exceptions from a throws clause
 */
public class RemoveExceptionsInThrowsQuickFix implements IJavaCodeActionParticipant {

    private static final Logger LOGGER = Logger.getLogger(RemoveExceptionsInThrowsQuickFix.class.getName());

    private final String messageIdentifier;
    public static final String EXCEPTIONS_TYPE = "exceptions.name";

    /**
     * Constructor.
     *
     * @param messageIdentifier as label
     */
    public RemoveExceptionsInThrowsQuickFix(String messageIdentifier) {
        this.messageIdentifier = messageIdentifier;
    }

    @Override
    public String getParticipantId() {
        return RemoveExceptionsInThrowsQuickFix.class.getName();
    }

    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        List<String> exceptions = getExceptions((JsonArray) diagnostic.getData());
        List<CodeAction> codeActions = new ArrayList<>();
        Map<String, Object> extendedData = new HashMap<>();
        extendedData.put(EXCEPTIONS_TYPE, exceptions);
        codeActions.add(JDTUtils.createCodeAction(context, diagnostic, getLabel(), getParticipantId(), extendedData));
       return codeActions;
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        CodeActionResolveData data = (CodeActionResolveData) toResolve.getData();
        final PsiMethod parentMethod = PsiTreeUtil.getParentOfType(context.getCoveredNode(), PsiMethod.class);
        List<String> exceptions = (List<String>) data.getExtendedDataEntry(EXCEPTIONS_TYPE);
        ChangeCorrectionProposal proposal = new ModifyThrownExceptionsProposal(getLabel(), context.getSource().getCompilationUnit(),
                context.getASTRoot(), parentMethod, 0, new ArrayList<>(), exceptions);
        ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER, "Unable to create workspace edit for code action");
        return toResolve;
    }
    /**
     * getExceptions
     *
     * @param diagnosticData as JsonArray
     * @return Get the exception list from diagnosticData
     */
    private List<String> getExceptions(JsonArray diagnosticData) {
        List<String> exceptions = new ArrayList<>(diagnosticData.size());
        for (JsonElement element : diagnosticData) {
            exceptions.add(element.getAsString());
        }
        return exceptions;
    }
    /**
     * Returns the code action label.
     *
     * @return The code action label.
     */
    public String getLabel() {
        return Messages.getMessage(messageIdentifier);
    }
}
