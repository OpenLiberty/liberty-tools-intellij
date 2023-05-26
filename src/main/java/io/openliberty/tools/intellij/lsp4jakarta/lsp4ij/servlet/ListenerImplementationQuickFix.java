/*******************************************************************************
 * Copyright (c) 2020, 2023 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *     IBM Corporation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.servlet;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ImplementInterfaceProposal;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.List;

/**
 * QuickFix for fixing HttpServlet extension error by providing the code actions
 * which implements IJavaCodeActionParticipant
 *
 * Adapted from
 * https://github.com/eclipse/lsp4mp/blob/master/microprofile.jdt/org.eclipse.lsp4mp.jdt.core/src/main/java/org/eclipse/lsp4mp/jdt/internal/health/java/ImplementHealthCheckQuickFix.java
 *
 * @author Credit to Angelo ZERR
 *
 */

public class ListenerImplementationQuickFix {
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        List<CodeAction> codeActions = new ArrayList<>();
        // Create code action
        // interface

        setUpCodeAction(codeActions, diagnostic, context, ServletConstants.SERVLET_CONTEXT_LISTENER,
                "jakarta.servlet.ServletContextListener");
        setUpCodeAction(codeActions, diagnostic, context,
                ServletConstants.SERVLET_CONTEXT_ATTRIBUTE_LISTENER,
                "jakarta.servlet.ServletContextAttributeListener");
        setUpCodeAction(codeActions, diagnostic, context, ServletConstants.SERVLET_REQUEST_LISTENER,
                "jakarta.servlet.ServletRequestListener");
        setUpCodeAction(codeActions, diagnostic, context,
                ServletConstants.SERVLET_REQUEST_ATTRIBUTE_LISTENER,
                "jakarta.servlet.ServletRequestAttributeListener");
        setUpCodeAction(codeActions, diagnostic, context, ServletConstants.HTTP_SESSION_LISTENER,
                "jakarta.servlet.http.HttpSessionListener");
        setUpCodeAction(codeActions, diagnostic, context,
                ServletConstants.HTTP_SESSION_ATTRIBUTE_LISTENER,
                "jakarta.servlet.http.HttpSessionAttributeListener");
        setUpCodeAction(codeActions, diagnostic, context, ServletConstants.HTTP_SESSION_ID_LISTENER,
                "jakarta.servlet.http.HttpSessionIdListener");

        return codeActions;
    }

    private PsiClass getBinding(PsiElement node) {
        return PsiTreeUtil.getParentOfType(node, PsiClass.class);
    }

    private void setUpCodeAction(List<CodeAction> codeActions, Diagnostic diagnostic, JavaCodeActionContext sourceContext,
                                       String interfaceName, String interfaceType) {
        JavaCodeActionContext targetContext = sourceContext.copy();
        PsiElement node = targetContext.getCoveredNode(); // find covered node in the new context
        PsiClass targetType = getBinding(node);
        if (targetType != null) {
            ChangeCorrectionProposal proposal = new ImplementInterfaceProposal(
                    null, targetType, targetContext.getASTRoot(), interfaceType, 0,
                    sourceContext.getCompilationUnit());
            CodeAction codeAction = targetContext.convertToCodeAction(proposal, diagnostic);
            codeActions.add(codeAction);
        }
    }
}
