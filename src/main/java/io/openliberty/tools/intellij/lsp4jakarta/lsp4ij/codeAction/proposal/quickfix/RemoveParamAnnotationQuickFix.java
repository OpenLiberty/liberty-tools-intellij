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
*******************************************************************************/
 package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix;

 import com.intellij.psi.*;
 import com.intellij.psi.util.PsiTreeUtil;
 import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
 import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.RemoveAnnotationsProposal;
 import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
 import org.eclipse.lsp4j.CodeAction;
 import org.eclipse.lsp4j.Diagnostic;

 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.List;

 /**
  * QuickFix for removing parameter annotations
  */
public class RemoveParamAnnotationQuickFix {

	private final String[] annotations;
	
    public RemoveParamAnnotationQuickFix(String ...annotations) {
        this.annotations = annotations;
    }

    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {

        final PsiElement node = context.getCoveredNode();
        final PsiMethod method = PsiTreeUtil.getParentOfType(node, PsiMethod.class);

        final List<CodeAction> codeActions = new ArrayList<>();
        final PsiParameterList parameters = method.getParameterList();
        int parametersCount = parameters.getParametersCount();
        for (int i = 0; i < parametersCount; ++i) {
            final PsiParameter parameter = parameters.getParameter(i);
            final PsiAnnotation[] psiAnnotations = parameter.getAnnotations();
            final List<String> annotationsToRemove = new ArrayList<>();
            // Search for annotations to remove from the current method parameter.
            if (psiAnnotations != null) {
                Arrays.stream(psiAnnotations).forEach(a -> {
                    if (Arrays.stream(annotations).anyMatch(m -> m.equals(a.getQualifiedName()))) {
                        annotationsToRemove.add(a.getQualifiedName());
                    }
                });
            }
            if (!annotationsToRemove.isEmpty()) {
                // Create label
                final StringBuilder sb = new StringBuilder();
                // Java annotations in comma delimited list, assume that is ok.
                sb.append("'@").append(getShortName(annotationsToRemove.get(0))).append("'");
                for (int j = 1; j < annotationsToRemove.size(); ++j) {
                    sb.append(", '@").append(getShortName(annotationsToRemove.get(j))).append("'");
                }
                String label = Messages.getMessage("RemoveTheModifierFromParameter", sb.toString(), parameter.getName().toString());
                // Remove annotations
                removeAnnotations(diagnostic, context.copy(), codeActions, i, label, annotationsToRemove);
            }
        }
        return codeActions;
    }

    protected void removeAnnotations(Diagnostic diagnostic, JavaCodeActionContext context,
                                     List<CodeAction> codeActions, int parameterIndex,
                                     String label, List<String> annotationsToRemove) {

        final PsiElement node = context.getCoveredNode();
        final PsiClass parentType = getBinding(node);
        final PsiMethod method = PsiTreeUtil.getParentOfType(node, PsiMethod.class);

        final PsiParameter parameter = method.getParameterList().getParameter(parameterIndex);
        final PsiAnnotation[] psiAnnotations = parameter.getAnnotations();
        final List<PsiAnnotation> psiAnnotationsToRemove = new ArrayList<>();
        Arrays.stream(psiAnnotations).forEach(a -> {
            if (annotationsToRemove.stream().anyMatch(m -> m.equals(a.getQualifiedName()))) {
                psiAnnotationsToRemove.add(a);
            }
        });

        RemoveAnnotationsProposal proposal = new RemoveAnnotationsProposal(label, context.getSource().getCompilationUnit(),
                context.getASTRoot(), parentType, 0, psiAnnotationsToRemove);
        CodeAction codeAction = context.convertToCodeAction(proposal, diagnostic);
        if (codeAction != null) {
            codeActions.add(codeAction);
        }
    }

    protected PsiClass getBinding(PsiElement node) {
        return PsiTreeUtil.getParentOfType(node, PsiClass.class);
    }

    protected static String getShortName(String qualifiedName) {
        final int i = qualifiedName.lastIndexOf('.');
        if (i != -1) {
            return qualifiedName.substring(i+1);
        }
        return qualifiedName;
    }
}
