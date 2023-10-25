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
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.ExtendedCodeAction;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4mp.commons.CodeActionResolveData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quick fix for the ResourceMethodMultipleEntityParams diagnostic in
 * ResourceMethodDiagnosticsCollector. This class adds a quick fix for each
 * entity parameter which removes all entity parameters except the chosen one.
 *
 * @author Bera Sogut
 *
 */
public class ResourceMethodMultipleEntityParamsQuickFix implements IJavaCodeActionParticipant {

    private static final Logger LOGGER = Logger.getLogger(ResourceMethodMultipleEntityParamsQuickFix.class.getName());

    @Override
    public String getParticipantId() {
        return ResourceMethodMultipleEntityParamsQuickFix.class.getName();
    }

    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {

        final PsiElement node = context.getCoveredNode();
        final PsiMethod parentMethod = PsiTreeUtil.getParentOfType(node, PsiMethod.class);
        final List<CodeAction> codeActions = new ArrayList<>();

        if (parentMethod != null) {
            return addCodeActions(context, diagnostic, parentMethod, codeActions);
        }
        return codeActions;
    }

    private List<CodeAction> addCodeActions(JavaCodeActionContext context, Diagnostic diagnostic, PsiMethod parentMethod, List<CodeAction> codeActions) {
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
        return iterateAndCreateCodeAction(context, diagnostic, codeActions, entityParamIndexes);
    }

    private List<CodeAction> iterateAndCreateCodeAction(JavaCodeActionContext context, Diagnostic diagnostic, List<CodeAction> codeActions, List<Integer> entityParamIndexes) {
        entityParamIndexes.forEach(entityParamIndex -> {
            addCreateCodeAction(context, diagnostic, codeActions, entityParamIndexes, entityParamIndex);
        });
        return codeActions;
    }

    private void addCreateCodeAction(JavaCodeActionContext context, Diagnostic diagnostic, List<CodeAction> codeActions, List<Integer> entityParamIndexes, Integer entityParamIndex) {
        final JavaCodeActionContext targetContext = context.copy();
        final PsiElement targetNode = targetContext.getCoveredNode();
        final PsiParameter[] parameters = PsiTreeUtil.getParentOfType(targetNode, PsiMethod.class).getParameterList().getParameters();

        final String title = getTitle(parameters[entityParamIndex]);

        codeActions.add(createCodeAction(context, diagnostic, title));
    }

    private static String getTitle(PsiParameter parameters) {
        return Messages.getMessage("RemoveAllEntityParametersExcept",
                parameters.getName());
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        final PsiClass parentType = PsiTreeUtil.getParentOfType(node, PsiClass.class);
        final PsiMethod parentMethod = PsiTreeUtil.getParentOfType(node, PsiMethod.class);
        assert parentMethod != null;
        final PsiParameterList parameterList = parentMethod.getParameterList();
        final List<PsiParameter> entityParams = new ArrayList<>();
        String title = toResolve.getTitle();

        if (parameterList.getParametersCount() > 0) {
            final PsiParameter[] parameters = parameterList.getParameters();
            for (PsiParameter parameter : parameters) {
                if (isEntityParam(parameter)) {
                    if (!title.contains(parameter.getName())) {
                        entityParams.add(parameter);
                    }
                }
            }
        }

        ChangeCorrectionProposal proposal = new RemoveParamsProposal(title, context.getSource().getCompilationUnit(),
                context.getASTRoot(), parentType, 0,  entityParams, false);

        try {
            WorkspaceEdit we = context.convertToWorkspaceEdit(proposal);
            toResolve.setEdit(we);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to create workspace edit for code action", e);
        }
        return toResolve;
    }

    private CodeAction createCodeAction(JavaCodeActionContext context, Diagnostic diagnostic, String label) {
        ExtendedCodeAction codeAction = new ExtendedCodeAction(label);
        codeAction.setRelevance(0);
        codeAction.setDiagnostics(Collections.singletonList(diagnostic));
        codeAction.setKind(CodeActionKind.QuickFix);
        codeAction.setData(new CodeActionResolveData(context.getUri(), getParticipantId(),
                context.getParams().getRange(), Collections.emptyMap(),
                context.getParams().isResourceOperationSupported(),
                context.getParams().isCommandConfigurationUpdateSupported()));
        return codeAction;
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
