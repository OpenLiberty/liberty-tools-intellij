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

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
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
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4mp.commons.codeaction.CodeActionResolveData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

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

public class ListenerImplementationQuickFix implements IJavaCodeActionParticipant {

    private static final Logger LOGGER = Logger.getLogger(ListenerImplementationQuickFix.class.getName());

    private final static String INTERFACE_NAME_KEY = "interface";

    @Override
    public String getParticipantId() {
        return ListenerImplementationQuickFix.class.getName();
    }

    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        List<CodeAction> codeActions = new ArrayList<>();
        PsiElement node = context.getCoveredNode();
        PsiClass parentType = getBinding(node);

        String[] listenerConstants = {
                ServletConstants.SERVLET_CONTEXT_LISTENER_FQ_NAME,
                ServletConstants.SERVLET_CONTEXT_ATTRIBUTE_LISTENER_FQ_NAME,
                ServletConstants.SERVLET_REQUEST_LISTENER_FQ_NAME,
                ServletConstants.SERVLET_REQUEST_ATTRIBUTE_LISTENER_FQ_NAME,
                ServletConstants.HTTP_SESSION_LISTENER_FQ_NAME,
                ServletConstants.HTTP_SESSION_ATTRIBUTE_LISTENER_FQ_NAME,
                ServletConstants.HTTP_SESSION_ID_LISTENER_FQ_NAME
        };

        for (String interfaceType : listenerConstants) {
            Map<String, Object> extendedData = new HashMap<>();
            extendedData.put(INTERFACE_NAME_KEY, interfaceType);
            String title = getLabel(interfaceType, parentType);
            codeActions.add(JDTUtils.createCodeAction(context, diagnostic, title, getParticipantId(), extendedData));
        }

        return codeActions;
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        final PsiClass parentType = getBinding(node);
        CodeActionResolveData data = (CodeActionResolveData) toResolve.getData();
        String interfaceType = (String) data.getExtendedDataEntry(INTERFACE_NAME_KEY);

        assert parentType != null;
        ChangeCorrectionProposal proposal = new ImplementInterfaceProposal(
                context.getCompilationUnit(), parentType, context.getASTRoot(),
                interfaceType, 0, context.getSource().getCompilationUnit());
        try {
            WorkspaceEdit we = context.convertToWorkspaceEdit(proposal);
            toResolve.setEdit(we);
        } catch (ProcessCanceledException e) {
            //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
            //TODO delete block when minimum required version is 2024.2
            throw e;
        } catch (IndexNotReadyException | CancellationException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to create workspace edit for code action to listener implementation", e);
        }
        return toResolve;
    }

    private PsiClass getBinding(PsiElement node) {
        return PsiTreeUtil.getParentOfType(node, PsiClass.class);
    }

    private static String getLabel(String fqAnnotation, PsiClass parentType) {
        String annotationName = fqAnnotation.substring(fqAnnotation.lastIndexOf('.') + 1, fqAnnotation.length());
        return Messages.getMessage("LetClassImplement", parentType.getName(), annotationName);
    }
}
