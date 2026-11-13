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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.ReplaceParametersWithSingleParamProposal;
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
 * Generic abstract quickfix that replaces a method's parameter list so it
 * contains exactly one parameter of a subclass-specified primitive type.
 *
 * <p>Subclasses supply:
 * <ul>
 *   <li>{@link #getRequiredTypeName()} — Java keyword for the required type
 *       (e.g. {@code "boolean"})</li>
 *   <li>{@link #getDefaultParamName()} — the name used when inserting a
 *       brand-new parameter</li>
 *   <li>{@link #getLabel()} — the label shown to the user</li>
 * </ul>
 *
 * <p>Delegates all PSI rewriting to
 * {@link ReplaceParametersWithSingleParamProposal}.
 */
public abstract class ReplaceParametersWithSingleParamQuickFix implements IJavaCodeActionParticipant {

    private static final Logger LOGGER =
            Logger.getLogger(ReplaceParametersWithSingleParamQuickFix.class.getName());

    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context,
                                                     Diagnostic diagnostic) {
        List<CodeAction> codeActions = new ArrayList<>();
        final PsiElement node = context.getCoveredNode();
        final PsiMethod method = PsiTreeUtil.getParentOfType(node, PsiMethod.class);
        if (method != null) {
            codeActions.add(JDTUtils.createCodeAction(context, diagnostic, getLabel(), getParticipantId()));
        }
        return codeActions;
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        final PsiMethod method = PsiTreeUtil.getParentOfType(node, PsiMethod.class);

        if (method != null) {
            ChangeCorrectionProposal proposal = new ReplaceParametersWithSingleParamProposal(
                    getLabel(),
                    context.getSource().getCompilationUnit(),
                    context.getASTRoot(),
                    method,
                    0,
                    getRequiredTypeName(),
                    getDefaultParamName());
            ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER,
                    "Unable to create workspace edit to replace method parameters");
        }
        return toResolve;
    }

    /**
     * Returns the Java primitive type keyword the single parameter must have
     * (e.g. {@code "boolean"}, {@code "int"}).
     *
     * @return the required type name
     */
    protected abstract String getRequiredTypeName();

    /**
     * Returns the parameter name to use when inserting a brand-new parameter
     * (i.e. the method currently has no parameters at all).
     *
     * @return the default parameter name
     */
    protected abstract String getDefaultParamName();

    /**
     * Returns the label shown to the user for this code action.
     *
     * @return the code action label
     */
    protected abstract String getLabel();
}
