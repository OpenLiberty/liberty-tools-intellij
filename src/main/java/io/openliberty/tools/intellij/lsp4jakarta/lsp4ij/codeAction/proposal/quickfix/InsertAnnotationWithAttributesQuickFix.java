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
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiModifierListOwner;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.InsertAnnotationWithAttributesProposal;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.util.PsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.util.ExceptionUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Abstract quick fix for inserting annotations with specific attribute values.
 * Subclasses should implement methods to provide the annotation name, attributes,
 * and code action label.
 */
public abstract class InsertAnnotationWithAttributesQuickFix implements IJavaCodeActionParticipant {
    private static final Logger LOGGER = Logger.getLogger(InsertAnnotationWithAttributesQuickFix.class.getName());

    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        List<CodeAction> codeActions = new ArrayList<>();
        String label = getLabel(context, diagnostic);
        codeActions.add(JDTUtils.createCodeAction(context, diagnostic, label, getParticipantId()));
        return codeActions;
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        String name = toResolve.getTitle();
        PsiElement node = context.getCoveringNode();
        PsiModifierListOwner binding = PsiUtils.getBinding(node);

        String annotation = getAnnotation();
        Map<String, PsiAnnotationMemberValue> attributeValues = getAttributeValues(context);

        ChangeCorrectionProposal proposal = new InsertAnnotationWithAttributesProposal(
                name,
                context.getSource().getCompilationUnit(),
                context.getASTRoot(),
                binding,
                0,
                annotation,
                attributeValues
        );

        ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER,
                "Unable to create workspace edit for inserting annotation with attributes.");
        return toResolve;
    }



    /**
     * Returns the fully qualified annotation name to insert.
     *
     * @return the annotation name
     */
    protected abstract String getAnnotation();

    /**
     * Returns the attribute values to set on the annotation.
     *
     * @param context the code action resolve context
     * @return map of attribute names to their values
     */
    protected abstract Map<String, PsiAnnotationMemberValue> getAttributeValues(JavaCodeActionResolveContext context);

    /**
     * Returns the label for the code action.
     *
     * @param context the code action context
     * @param diagnostic the diagnostic
     * @return the code action label
     */
    protected abstract String getLabel(JavaCodeActionContext context, Diagnostic diagnostic);
}

