/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lidia Ataupillco Ramos
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.ModifyAnnotationProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.util.ExceptionUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Quickfix for adding new annotations with or without attributes
 *
 * @author Zijian Pei
 * @author Lidia Ataupillco Ramos
 *
 */
public abstract class InsertAnnotationQuickFix implements IJavaCodeActionParticipant {

    private final String[] attributes;

    private final String annotation;

    protected final boolean generateOnlyOneCodeAction;

    private static final Logger LOGGER = Logger.getLogger(InsertAnnotationQuickFix.class.getName());

    public InsertAnnotationQuickFix(String annotation, String... attributes) {
        this(annotation, false, attributes);
    }

    /**
     * Constructor for add missing attributes quick fix.
     *
     * @param generateOnlyOneCodeAction true if the participant must generate a
     *                                  CodeAction which add the list of attributes
     *                                  and false otherwise.
     * @param attributes                list of attributes to add.
     */
    public InsertAnnotationQuickFix(String annotation, boolean generateOnlyOneCodeAction,
                                    String... attributes) {
        this.annotation = annotation;
        this.generateOnlyOneCodeAction = generateOnlyOneCodeAction;
        this.attributes = attributes;
    }

    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        PsiElement node = context.getCoveredNode();
        List<CodeAction> codeActions = new ArrayList<>();
        addAttributes(diagnostic, context, codeActions, this.annotation);
        return codeActions;
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        PsiModifierListOwner binding = getBinding(node);
        // annotationNode is null when adding an annotation and non-null when adding attributes.
        PsiAnnotation annotationNode = PsiTreeUtil.getParentOfType(node, PsiAnnotation.class);

        assert binding != null;
        String label = getLabel(this.annotation, attributes);
        ChangeCorrectionProposal proposal = new ModifyAnnotationProposal(label, context.getSource().getCompilationUnit(),
                context.getASTRoot(), binding, annotationNode, 0, this.annotation, Arrays.asList(attributes));

        ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER, "Unable to create workspace edit for code action " + label);
        return toResolve;
    }

    protected void addAttributes(Diagnostic diagnostic, JavaCodeActionContext context, List<CodeAction> codeActions, String name) {
        if (generateOnlyOneCodeAction) {
            addAttribute(diagnostic, context, codeActions, name, attributes);
        } else {
            for (String attribute : attributes) {
                addAttribute(diagnostic, context, codeActions, name, attribute);
            }
        }
    }

    /**
     * use setData() API with diagnostic to pass in ElementType in diagnostic
     * collector class.
     *
     */
    private void addAttribute(Diagnostic diagnostic, JavaCodeActionContext context, List<CodeAction> codeActions, String name, String... attributes) {
        String label = getLabel(name, attributes);
        codeActions.add(JDTUtils.createCodeAction(context, diagnostic, label, getParticipantId()));
    }

    protected PsiModifierListOwner getBinding(PsiElement node) {
        // handle annotation insertions for a variable declaration or a class
        PsiModifierListOwner element = PsiTreeUtil.getParentOfType(node, PsiModifierListOwner.class);
        if (element != null) {
            return element;
        }
        return PsiTreeUtil.getParentOfType(node, PsiClass.class);
    }

    protected String getLabel(String annotationName, String... attributes) {
        return Messages.getMessage("InsertItem", "@" + annotation); // uses Java syntax
    }
}
