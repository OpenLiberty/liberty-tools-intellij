/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation, Bera Sogut and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation, Bera Sogut - initial API and implementation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jax_rs;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.RemoveParamsProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Quick fix for the ResourceMethodMultipleEntityParams diagnostic in
 * ResourceMethodDiagnosticsCollector. This class adds a quick fix for each
 * entity parameter which removes all entity parameters except the chosen one.
 *
 * @author Bera Sogut
 *
 */
public class ResourceMethodMultipleEntityParamsQuickFix {

    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {

        final PsiElement node = context.getCoveredNode();
        final PsiMethod parentMethod = PsiTreeUtil.getParentOfType(node, PsiMethod.class);

        if (parentMethod != null) {
            final List<CodeAction> codeActions = new ArrayList<>();
            final List<Integer> entityParamIndexes = new ArrayList<>();

            final PsiParameterList parameterList = parentMethod.getParameterList();
            if (parameterList != null && parameterList.getParametersCount() > 0) {
                final PsiParameter[] parameters = parameterList.getParameters();
                for (int i = 0; i < parameters.length; ++i) {
                    if (isEntityParam(parameters[i])) {
                        entityParamIndexes.add(i);
                    }
                }
            }

            entityParamIndexes.forEach(entityParamIndex -> {
                final JavaCodeActionContext targetContext = context.copy();
                final PsiElement targetNode = targetContext.getCoveredNode();
                final PsiClass parentType = PsiTreeUtil.getParentOfType(targetNode, PsiClass.class);
                final PsiParameter[] parameters = PsiTreeUtil.getParentOfType(targetNode, PsiMethod.class).getParameterList().getParameters();

                final String titleMessage = Messages.getMessage("RemoveAllEntityParametersExcept",
                        parameters[entityParamIndex].getName());

                final List<PsiParameter> entityParams = new ArrayList<>();
                entityParamIndexes.forEach(x -> {
                    if (!x.equals(entityParamIndex)) {
                        entityParams.add(parameters[x]);
                    }
                });

                ChangeCorrectionProposal proposal = new RemoveParamsProposal(titleMessage, targetContext.getSource().getCompilationUnit(),
                        targetContext.getASTRoot(), parentType, 0, entityParams);

                // Convert the proposal to LSP4J CodeAction
                CodeAction codeAction = targetContext.convertToCodeAction(proposal, diagnostic);
                codeAction.setTitle(titleMessage);
                codeActions.add(codeAction);
            });
            return codeActions;
        }
        return Collections.emptyList();
    }

    /**
     * Returns a boolean variable that indicates whether the given parameter is an
     * entity parameter or not.
     *
     * @param param the parameter to check whether it is an entity parameter or not
     * @return true if the given parameter is an entity parameter, false otherwise
     */
    private boolean isEntityParam(PsiParameter param) {
        final PsiAnnotation[] psiAnnotations = param.getAnnotations();
        if (psiAnnotations != null) {
            for (int i = 0; i < psiAnnotations.length; ++i) {
                final String typeName = psiAnnotations[i].getQualifiedName();
                if (Arrays.stream(Jax_RSConstants.SET_OF_NON_ENTITY_PARAM_ANNOTATIONS).anyMatch(x -> x.equals(typeName))) {
                    return false;
                }
            }
        }
        return true;
    }
}
