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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.InsertAnnotationProposal;
import io.openliberty.tools.intellij.util.ExceptionUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.WorkspaceEdit;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * QuickFix for inserting annotations.
 * Reused from https://github.com/eclipse/lsp4mp/blob/6f2d700a88a3262e39cc2ba04beedb429e162246/microprofile.jdt/org.eclipse.lsp4mp.jdt.core/src/main/java/org/eclipse/lsp4mp/jdt/core/java/codeaction/InsertAnnotationMissingQuickFix.java
 *
 * @author Angelo ZERR
 *
 */
public abstract class InsertAnnotationMissingQuickFix implements IJavaCodeActionParticipant {
    private static final Logger LOGGER = Logger.getLogger(InsertAnnotationMissingQuickFix.class.getName());

    private final String[] annotations;

    private final boolean generateOnlyOneCodeAction;

    /**
     * Constructor for insert annotation quick fix.
     *
     * <p>
     * The participant will generate a CodeAction per annotation.
     * </p>
     *
     * @param annotations list of annotation to insert.
     */
    public InsertAnnotationMissingQuickFix(String... annotations) {
        this(false, annotations);
    }

    /**
     * Constructor for insert annotation quick fix.
     *
     * @param generateOnlyOneCodeAction true if the participant must generate a
     *                                  CodeAction which insert the list of
     *                                  annotation and false otherwise.
     * @param annotations               list of annotation to insert.
     */
    public InsertAnnotationMissingQuickFix(boolean generateOnlyOneCodeAction, String... annotations) {
        this.generateOnlyOneCodeAction = generateOnlyOneCodeAction;
        this.annotations = annotations;
    }

    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        List<CodeAction> codeActions = new ArrayList<>();
        insertAnnotations(diagnostic, context, codeActions);
        return codeActions;
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        String name = toResolve.getTitle();
        PsiElement node = context.getCoveringNode();
        PsiModifierListOwner parentType = getBinding(node);

        ChangeCorrectionProposal proposal = new InsertAnnotationProposal(name, context.getCompilationUnit(),
                context.getASTRoot(), parentType, 0, context.getSource().getCompilationUnit(),
                annotations);

        Boolean success = ExceptionUtil.executeWithExceptionHandling(
                () -> {
                    WorkspaceEdit we = context.convertToWorkspaceEdit(proposal);
                    toResolve.setEdit(we);
                    return true;
                },
                e -> LOGGER.log(Level.WARNING, "Unable to create workspace edit for code action.", e)
        );
        if (success == null || !success) {
            System.out.println("An error occurred during the code action resolution.");
        }
        return toResolve;
    }

    protected static PsiModifierListOwner getBinding(PsiElement node) {
        PsiModifierListOwner binding = PsiTreeUtil.getParentOfType(node, PsiVariable.class);
        if (binding != null) {
            return binding;
        }
        binding = PsiTreeUtil.getParentOfType(node, PsiMethod.class);
        if (binding != null) {
            return binding;
        }
        return PsiTreeUtil.getParentOfType(node, PsiClass.class);
    }

    protected String[] getAnnotations() {
        return this.annotations;
    }

    protected void insertAnnotations(Diagnostic diagnostic, JavaCodeActionContext context,
                                     List<CodeAction> codeActions) {
        if (generateOnlyOneCodeAction) {
            insertAnnotation(diagnostic, context, codeActions, annotations);
        } else {
            for (String annotation : annotations) {
                JavaCodeActionContext annotationContext = context.copy();
                insertAnnotation(diagnostic, annotationContext, codeActions, annotation);
            }
        }
    }

    protected void insertAnnotation(Diagnostic diagnostic, JavaCodeActionContext context,
            List<CodeAction> codeActions, String... annotations) {
            String label = getLabel(annotations);
            codeActions.add(JDTUtils.createCodeAction(context, diagnostic, label, getParticipantId()));
    }

    private static String getLabel(String[] annotations) {
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < annotations.length; i++) {
            String annotation = annotations[i];
            String annotationName = annotation.substring(annotation.lastIndexOf('.') + 1, annotation.length());
            if (i > 0) {
                list.append(", "); // assume comma list is ok: @A, @B, @C
            }
            list.append("@"); // Java syntax
            list.append(annotationName);
        }
        return Messages.getMessage("InsertItem", list.toString());
    }
}
