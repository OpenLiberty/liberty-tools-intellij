/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.servlet;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.ModifyAnnotationProposal;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.InsertAnnotationMissingQuickFix;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.List;

/**
 * QuickFix for fixing
 * {@link ServletConstants#DIAGNOSTIC_CODE_MISSING_ATTRIBUTE} error and
 * {@link ServletConstants#DIAGNOSTIC_CODE_DUPLICATE_ATTRIBUTES} error by
 * providing several code actions:
 *
 * {@link ServletConstants#DIAGNOSTIC_CODE_MISSING_ATTRIBUTE}
 * <ul>
 * <li>Add the `value` attribute to the `@WebServlet` annotation
 * <li>Add the `urlPatterns` attribute to the `@WebServlet` annotation
 * </ul>
 *
 * {@link ServletConstants#DIAGNOSTIC_CODE_DUPLICATE_ATTRIBUTES}
 * <ul>
 * <li>Remove the `value` attribute to the `@WebServlet` annotation
 * <li>Remove the `urlPatterns` attribute to the `@WebServlet` annotation
 * </ul>
 *
 * @author Kathryn Kodama
 *
 */
public class CompleteServletAnnotationQuickFix extends InsertAnnotationMissingQuickFix {

    public CompleteServletAnnotationQuickFix() {
        super("jakarta.servlet.annotation.WebServlet");
    }

    @Override
    protected void insertAnnotations(Diagnostic diagnostic, JavaCodeActionContext context,
                                     List<CodeAction> codeActions) {
        String[] annotations = getAnnotations();
        for (String annotation : annotations) {
            insertAndReplaceAnnotation(diagnostic, context, codeActions, annotation);
        }
    }

    private static void insertAndReplaceAnnotation(Diagnostic diagnostic, JavaCodeActionContext context,
                                                   List<CodeAction> codeActions, String annotation) {

        // Insert the annotation and the proper import by using JDT Core Manipulation
        // API

        // if missing an attribute, do value insertion
        PsiElement node = null;
        PsiModifierListOwner parentType = null;
        PsiAnnotation annotationNode = null;

        if (diagnostic.getCode().getLeft().equals(ServletConstants.DIAGNOSTIC_CODE_MISSING_ATTRIBUTE)) {
            ArrayList<String> attributes = new ArrayList<>();
            attributes.add("value");
            attributes.add("urlPatterns");
            // Code Action 1: add value attribute to the WebServlet annotation
            // Code Action 2: add urlPatterns attribute to the WebServlet annotation
            for (int i = 0; i < attributes.size(); i++) {
                String attribute = attributes.get(i);
                JavaCodeActionContext targetContext = context.copy();
                node = targetContext.getCoveringNode();
                parentType = getBinding(node);
                annotationNode = getAnnotation(node);

                ArrayList<String> attributesToAdd = new ArrayList<>();
                attributesToAdd.add(attribute);
                String name = getLabel(annotation, attribute, "Add");
                ChangeCorrectionProposal proposal = new ModifyAnnotationProposal(name, targetContext.getSource().getCompilationUnit(),
                        targetContext.getASTRoot(), parentType, annotationNode, 0, annotation, attributesToAdd);
                // Convert the proposal to LSP4J CodeAction
                CodeAction codeAction = targetContext.convertToCodeAction(proposal, diagnostic);
                codeAction.setTitle(name);
                if (codeAction != null) {
                    codeActions.add(codeAction);
                }
            }
        }
        // if duplicate attributes exist in annotations, remove attributes from
        // annotation
        if (diagnostic.getCode().getLeft().equals(ServletConstants.DIAGNOSTIC_CODE_DUPLICATE_ATTRIBUTES)) {
            ArrayList<String> attributes = new ArrayList<>();
            attributes.add("value");
            attributes.add("urlPatterns");
            // Code Action 1: remove value attribute from the WebServlet annotation
            // Code Action 2: remove urlPatterns attribute from the WebServlet annotation
            for (int i = 0; i < attributes.size(); i++) {
                String attribute = attributes.get(i);
                JavaCodeActionContext targetContext = context.copy();
                node = targetContext.getCoveringNode();
                parentType = getBinding(node);
                annotationNode = getAnnotation(node);

                ArrayList<String> attributesToRemove = new ArrayList<>();
                attributesToRemove.add(attribute);
                String name = getLabel(annotation, attribute, "Remove");
                ChangeCorrectionProposal proposal = new ModifyAnnotationProposal(name, targetContext.getSource().getCompilationUnit(),
                        targetContext.getASTRoot(), parentType, annotationNode, 0, annotation, new ArrayList<String>(), attributesToRemove);
                // Convert the proposal to LSP4J CodeAction
                CodeAction codeAction = targetContext.convertToCodeAction(proposal, diagnostic);
                codeAction.setTitle(name);
                if (codeAction != null) {
                    codeActions.add(codeAction);
                }
            }
        }
    }

    private static String getLabel(String annotation, String attribute, String labelType) {
        StringBuilder name = new StringBuilder("Add the `" + attribute + "` attribute to ");
        if (labelType.equals("Remove")) {
            name = new StringBuilder("Remove the `" + attribute + "` attribute from ");
        }
        String annotationName = annotation.substring(annotation.lastIndexOf('.') + 1, annotation.length());
        name.append("@");
        name.append(annotationName);
        return name.toString();
    }

    private static PsiAnnotation getAnnotation(PsiElement e) {
        if (e instanceof PsiAnnotation) {
            return (PsiAnnotation) e;
        }
        return PsiTreeUtil.getParentOfType(e, PsiAnnotation.class);
    }
}
