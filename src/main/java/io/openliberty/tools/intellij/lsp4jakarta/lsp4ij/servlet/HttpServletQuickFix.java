/*******************************************************************************
 * Copyright (c) 2020, 2024 Red Hat Inc. and others.
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
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.ExtendClassProposal;
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

/**
 * QuickFix for fixing HttpServlet extension error by providing the code actions
 * which implements IJavaCodeActionParticipant
 * <p>
 * Adapted from
 * https://github.com/eclipse/lsp4mp/blob/master/microprofile.jdt/org.eclipse.lsp4mp.jdt.core/src/main/java/org/eclipse/lsp4mp/jdt/internal/health/java/ImplementHealthCheckQuickFix.java
 *
 * @author Credit to Angelo ZERR
 */
public class HttpServletQuickFix implements IJavaCodeActionParticipant {

    private static final Logger LOGGER = Logger.getLogger(HttpServletQuickFix.class.getName());

    @Override
    public String getParticipantId() {
        return HttpServletQuickFix.class.getName();
    }

    /**
     * Generates a list of code actions based on the provided JavaCodeActionContext and Diagnostic.
     *
     * @param context    the java code action context.
     * @param diagnostic the diagnostic which must be fixed and null otherwise.
     * @return
     */
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        List<CodeAction> codeActions = new ArrayList<>();
        PsiElement node = context.getCoveredNode();
        PsiClass parentType = getBinding(node);
        
        if (parentType != null) {
            String title = Messages.getMessage("LetClassExtend",
                    parentType.getName(),
                    ServletConstants.HTTP_SERVLET);
            codeActions.add(JDTUtils.createCodeAction(context, diagnostic, title, getParticipantId()));
        }
        return codeActions;
    }

    /**
     * this method will resolve the code for the quick actions
     *
     * @param context the code action context to resolve
     * @return
     */
    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        final PsiClass parentType = getBinding(node);

        assert parentType != null;
        String title = Messages.getMessage("LetClassExtend",
                parentType.getName(),
                ServletConstants.HTTP_SERVLET);
        ChangeCorrectionProposal proposal = new ExtendClassProposal(title, context.getCompilationUnit(),
                context.getSource().getCompilationUnit(), parentType,
                "jakarta.servlet.http.HttpServlet", 0);

        ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER, "Unable to create workspace edit for code action to extend the HttpServlet class.");
        return toResolve;
    }

    private PsiClass getBinding(PsiElement node) {
        return PsiTreeUtil.getParentOfType(node, PsiClass.class);
    }
}
