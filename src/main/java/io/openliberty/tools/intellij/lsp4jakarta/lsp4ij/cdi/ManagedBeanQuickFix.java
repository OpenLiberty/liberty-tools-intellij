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
 *     Hani Damlaj
 *     IBM Corporation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.InsertAnnotationMissingQuickFix;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ReplaceAnnotationProposal;
import io.openliberty.tools.intellij.util.ExceptionUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.SCOPE_FQ_NAMES;

public class ManagedBeanQuickFix extends InsertAnnotationMissingQuickFix {
    private static final Logger LOGGER = Logger.getLogger(ManagedBeanQuickFix.class.getName());
    private static final String ADD_ANNOTATION = "jakarta.enterprise.context.Dependent";

    public ManagedBeanQuickFix() {
        super(ADD_ANNOTATION);
    }

    private static final String[] REMOVE_ANNOTATION_NAMES = new ArrayList<>(SCOPE_FQ_NAMES).toArray(new String[SCOPE_FQ_NAMES.size()]);

    @Override
    protected void insertAnnotations(Diagnostic diagnostic, JavaCodeActionContext context,
                                     List<CodeAction> codeActions) {
        String[] annotations = getAnnotations();
        for (String annotation : annotations) {
            // Pass a copy of the context so each annotation gets a separate code action.
            insertAndReplaceAnnotation(diagnostic, context.copy(), codeActions, annotation);
        }
    }

    private void insertAndReplaceAnnotation(Diagnostic diagnostic, JavaCodeActionContext context,
                                            List<CodeAction> codeActions, String annotation) {
        String name = getLabel(annotation);
        codeActions.add(JDTUtils.createCodeAction(context, diagnostic, name, getParticipantId()));
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        String name = toResolve.getTitle();
        PsiElement node = context.getCoveringNode();
        PsiModifierListOwner parentType = PsiTreeUtil.getParentOfType(node, PsiClass.class);
        ChangeCorrectionProposal proposal = new ReplaceAnnotationProposal(name, context.getCompilationUnit(),
                context.getASTRoot(), parentType, 0, ADD_ANNOTATION, context.getSource().getCompilationUnit(),
                REMOVE_ANNOTATION_NAMES);

        ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER, "Unable to create workspace edit for code action");
        return toResolve;
    }

    private static String getLabel(String annotation) {
        String annotationName = annotation.substring(annotation.lastIndexOf('.') + 1, annotation.length());
        return Messages.getMessage("ReplaceCurrentScope", "@" + annotationName);
    }

    @Override
    public String getParticipantId() {
        return ManagedBeanQuickFix.class.getName();
    }
}
