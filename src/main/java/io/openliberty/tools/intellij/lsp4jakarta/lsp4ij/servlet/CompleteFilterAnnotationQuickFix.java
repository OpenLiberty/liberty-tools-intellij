/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.ModifyAnnotationProposal;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.InsertAnnotationMissingQuickFix;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.util.ExceptionUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4mp.commons.codeaction.CodeActionResolveData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * QuickFix for fixing {@link ServletConstants#DIAGNOSTIC_CODE_FILTER_MISSING_ATTRIBUTE} error
 * and {@link ServletConstants#DIAGNOSTIC_CODE_FILTER_MISSING_ATTRIBUTE} error
 * by providing several code actions:
 *
 *
 * {@link ServletConstants#DIAGNOSTIC_CODE_FILTER_MISSING_ATTRIBUTE}
 * <ul>
 * <li> Add the `value` attribute to the `@WebFilter` annotation
 * <li> Add the `urlPatterns` attribute to the `@WebFilter` annotation
 * <li> Add the `servletNames` attribute to the `@WebFilter` annotation
 * </ul>
 *
 * {@link ServletConstants#DIAGNOSTIC_CODE_FILTER_MISSING_ATTRIBUTE}
 * <ul>
 * <li> Remove the `value` attribute from the `@WebFilter` annotation
 * <li> Remove the `urlPatterns` attribute from the `@WebFilter` annotation
 * </ul>
 *
 * @author Kathryn Kodama
 *
 */
public class CompleteFilterAnnotationQuickFix extends InsertAnnotationMissingQuickFix {
    private static final Logger LOGGER = Logger.getLogger(CompleteFilterAnnotationQuickFix.class.getName());
    private static final String DIAGNOSTIC_CODE_KEY = "diagnosticCode";
    private static final String ATTRIBUTE_KEY = "attribute";
    private static final String ANNOTATION_KEY = "annotation";

    public CompleteFilterAnnotationQuickFix() {
        super("jakarta.servlet.annotation.WebFilter");
    }

    @Override
    protected void insertAnnotations(Diagnostic diagnostic, JavaCodeActionContext context,
                                     List<CodeAction> codeActions) {
        String[] annotations = getAnnotations();
        for (String annotation : annotations) {
            insertAndReplaceAnnotation(diagnostic, context, codeActions, annotation);
        }
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        PsiElement node = null;
        PsiModifierListOwner parentType = null;
        PsiAnnotation annotationNode = null;
        CodeActionResolveData data = (CodeActionResolveData) toResolve.getData();
        String diagnosticCode = (String) data.getExtendedDataEntry(DIAGNOSTIC_CODE_KEY);
        String attribute = (String) data.getExtendedDataEntry(ATTRIBUTE_KEY);
        String annotation = (String) data.getExtendedDataEntry(ANNOTATION_KEY);

        if (diagnosticCode.equals(ServletConstants.DIAGNOSTIC_CODE_FILTER_MISSING_ATTRIBUTE)) {
            node = context.getCoveringNode();
            parentType = getBinding(node);
            annotationNode = getAnnotation(node);

            ArrayList<String> attributesToAdd = new ArrayList<>();
            attributesToAdd.add(attribute);
            String name = toResolve.getTitle();
            ChangeCorrectionProposal proposal = new ModifyAnnotationProposal(name, context.getSource().getCompilationUnit(),
                    context.getASTRoot(), parentType, annotationNode, 0, annotation, attributesToAdd);

            ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER, "Unable to create workspace edit for code action.");
        }

        if (diagnosticCode.equals(ServletConstants.DIAGNOSTIC_CODE_FILTER_DUPLICATE_ATTRIBUTES)) {
            node = context.getCoveringNode();
            parentType = getBinding(node);
            annotationNode = getAnnotation(node);

            ArrayList<String> attributesToRemove = new ArrayList<>();
            attributesToRemove.add(attribute);
            String name = toResolve.getTitle();
            ChangeCorrectionProposal proposal = new ModifyAnnotationProposal(name, context.getSource().getCompilationUnit(),
                    context.getASTRoot(), parentType, annotationNode, 0, annotation, new ArrayList<String>(), attributesToRemove);

            ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER, "Unable to create workspace edit for code action.");
        }
        return toResolve;
    }

    private void insertAndReplaceAnnotation(Diagnostic diagnostic, JavaCodeActionContext context,
                                            List<CodeAction> codeActions, String annotation) {

        ArrayList<String> attributes = new ArrayList<>();
        attributes.add("value");
        attributes.add("urlPatterns");
        attributes.add("servletNames");

        String diagnosticCode = diagnostic.getCode().getLeft();
        if (diagnosticCode.equals(ServletConstants.DIAGNOSTIC_CODE_FILTER_MISSING_ATTRIBUTE)) {
            for (String attribute : attributes) {
                String name = getLabel(annotation, attribute, "Add");
                Map<String, Object> extendedData = new HashMap<>();
                extendedData.put(DIAGNOSTIC_CODE_KEY, diagnosticCode);
                extendedData.put(ATTRIBUTE_KEY, attribute);
                extendedData.put(ANNOTATION_KEY, annotation);
                codeActions.add(JDTUtils.createCodeAction(context, diagnostic, name, getParticipantId(), extendedData));
            }
        }
        if (diagnosticCode.equals(ServletConstants.DIAGNOSTIC_CODE_FILTER_DUPLICATE_ATTRIBUTES)) {
            for (String attribute : attributes) {
                if (!attribute.equals("servletNames")) {
                    String name = getLabel(annotation, attribute, "Remove");
                    Map<String, Object> extendedData = new HashMap<>();
                    extendedData.put(DIAGNOSTIC_CODE_KEY, diagnosticCode);
                    extendedData.put(ATTRIBUTE_KEY, attribute);
                    extendedData.put(ANNOTATION_KEY, annotation);
                    codeActions.add(JDTUtils.createCodeAction(context, diagnostic, name, getParticipantId(), extendedData));
                }
            }
        }
    }

    private static String getLabel(String annotation, String attribute, String labelType) {
        String annotationName = annotation.substring(annotation.lastIndexOf('.') + 1, annotation.length());
        annotationName = "@" + annotationName;
        if (labelType.equals("Remove")) {
            return Messages.getMessage("RemoveTheAttributeFrom", attribute, annotationName);
        }
        return Messages.getMessage("AddTheAttributeTo", attribute, annotationName);
    }

    private static PsiAnnotation getAnnotation(PsiElement e) {
        if (e instanceof PsiAnnotation) {
            return (PsiAnnotation) e;
        }
        return PsiTreeUtil.getParentOfType(e, PsiAnnotation.class);
    }

    @Override
    public String getParticipantId() {
        return CompleteFilterAnnotationQuickFix.class.getName();
    }
}
