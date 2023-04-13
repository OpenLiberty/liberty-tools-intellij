/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence;


import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.ModifyAnnotationProposal;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.InsertAnnotationMissingQuickFix;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * QuickFix for fixing {@link PersistenceConstants#DIAGNOSTIC_CODE_MISSING_ATTRIBUTES} error
 * by providing several code actions* to add the missing elements to the existing annotations:
 *
 * {@link PersistenceConstants#DIAGNOSTIC_CODE_MISSING_ATTRIBUTES}
 * <ul>
 * <li> Add the `name` attribute to the `@MapKeyJoinColumn` annotation
 * <li> Add the `referencedColumnName` attribute to the `@MapKeyJoinColumn` annotation
 * </ul>
 *
 * @author Leslie Dawson (lamminade)
 *
 * * Or only one code action to fix all annotations.
 */
public class PersistenceAnnotationQuickFix extends InsertAnnotationMissingQuickFix {

    public PersistenceAnnotationQuickFix() {
        super("jakarta.persistence.MapKeyJoinColumn");
    }

    @Override
    protected void insertAnnotations(Diagnostic diagnostic, JavaCodeActionContext context,
                                     List<CodeAction> codeActions) {
        String[] annotations = getAnnotations();
        insertAndReplaceAnnotation(diagnostic, context, codeActions, annotations);
    }

    private static void insertAndReplaceAnnotation(Diagnostic diagnostic, JavaCodeActionContext context,
                                                   List<CodeAction> codeActions, String... annotations) {
        ArrayList<String> attributes = new ArrayList<>();
        attributes.add("name");
        attributes.add("referencedColumnName");
        String name = "Add the missing attributes to the @MapKeyJoinColumn annotation";
        PsiElement node = context.getCoveredNode();
        PsiModifierListOwner binding = getBinding(node); // field or method in this case
        List<PsiAnnotation> annotationNodes = getAnnotations(binding, annotations);
        CodeAction codeAction = null;

        for (PsiAnnotation annotationNode : annotationNodes) {
            ChangeCorrectionProposal proposal = new ModifyAnnotationProposal(name, context.getCompilationUnit(),
                    context.getASTRoot(), binding, annotationNode, 0, attributes, annotations);

            // Convert the proposal to LSP4J CodeAction
            // We need to fix all the annotations so all the changes are combined in this one context.
            // Therefore, we only need to save the last code action.
            codeAction = context.convertToCodeAction(proposal, diagnostic);
        }
        if (codeAction != null) {
            codeActions.add(codeAction);
        }
    }

    private static List<PsiAnnotation> getAnnotations(PsiElement e, String... names) {
        List<PsiAnnotation> result = new ArrayList<>();
        if (e instanceof PsiField) {
            PsiField field = ((PsiField) e);
            PsiAnnotation[] annotations = field.getAnnotations();
            for (String name : names) {
                List<PsiAnnotation> partial = Arrays.stream(annotations)
                        .filter(n -> n.getQualifiedName().equals(name))
                        .collect(Collectors.toList());
                result.addAll(partial);
            }
        }
        return result;
    }
}
