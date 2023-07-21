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
 *     Hani Damlaj
 *     IBM Corporation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.InsertAnnotationMissingQuickFix;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ReplaceAnnotationProposal;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.List;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.SCOPE_FQ_NAMES;

public class ManagedBeanQuickFix extends InsertAnnotationMissingQuickFix {
    public ManagedBeanQuickFix() {
        super("jakarta.enterprise.context.Dependent");
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

    private static void insertAndReplaceAnnotation(Diagnostic diagnostic, JavaCodeActionContext context,
                                                   List<CodeAction> codeActions, String annotation) {
        // Diagnostic is reported on the variable declaration, however the
        // annotations that need to be replaced are on the type declaration (class
        // definition) containing the variable declaration. We retrieve the type
        // declaration container here.
        PsiElement parentNode = context.getCoveredNode();
        PsiClass classBinding = PsiTreeUtil.getParentOfType(parentNode, PsiClass.class);

        // Insert the annotation and the proper import by using JDT Core Manipulation
        // API
        String name = getLabel(annotation);
        ChangeCorrectionProposal proposal = new ReplaceAnnotationProposal(name, context.getCompilationUnit(),
                context.getASTRoot(), classBinding, 0, annotation, context.getCompilationUnit(), REMOVE_ANNOTATION_NAMES);
        // Convert the proposal to LSP4J CodeAction
        CodeAction codeAction = context.convertToCodeAction(proposal, diagnostic);
        if (codeAction != null) {
            codeActions.add(codeAction);
        }
    }

    private static String getLabel(String annotation) {
        String annotationName = annotation.substring(annotation.lastIndexOf('.') + 1, annotation.length());
        return Messages.getMessage("ReplaceCurrentScope", "@" + annotationName);
    }
}
