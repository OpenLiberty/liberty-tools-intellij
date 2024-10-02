/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation, Bera Sogut and others.
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

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.RemoveParamsProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4mp.commons.codeaction.CodeActionResolveData;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
    private static final String ENTITY_PARAM_INDEX_KEY = "entityParamIndex";
    private static final String ENTITY_PARAM_INDEXES_KEY = "entityParamIndexes";

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
        final PsiParameter[] parameters = parameterList.getParameters();
        if (parameterList.getParametersCount() > 0) {
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
            addCreateCodeAction(context, diagnostic, codeActions, entityParamIndex, entityParamIndexes);
        });
        return codeActions;
    }

    private void addCreateCodeAction(JavaCodeActionContext context, Diagnostic diagnostic, List<CodeAction> codeActions, Integer entityParamIndex, List<Integer> entityParamIndexes) {
        final JavaCodeActionContext targetContext = context.copy();
        final PsiElement targetNode = targetContext.getCoveredNode();
        final PsiParameter[] parameters = Objects.requireNonNull(PsiTreeUtil.getParentOfType(targetNode, PsiMethod.class)).getParameterList().getParameters();
        final String title = getTitle(parameters[entityParamIndex]);
        Map<String, Object> extendedData = new HashMap<>();
        extendedData.put(ENTITY_PARAM_INDEX_KEY, entityParamIndex);
        extendedData.put(ENTITY_PARAM_INDEXES_KEY, entityParamIndexes);
        codeActions.add(JDTUtils.createCodeAction(context, diagnostic, title, getParticipantId(), extendedData));
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
        String title = toResolve.getTitle();
        CodeActionResolveData data = (CodeActionResolveData) toResolve.getData();
        Integer currentEntityParamIndex = ((Number) data.getExtendedDataEntry(ENTITY_PARAM_INDEX_KEY)).intValue();
        List<Integer> entityParamIndexes =
                ((List<Number>) data.getExtendedDataEntry(ENTITY_PARAM_INDEXES_KEY)).
                        stream().map(x -> Integer.valueOf(x.intValue())).collect(Collectors.toList());

        assert parentMethod != null;
        final PsiParameter[] parameters = parentMethod.getParameterList().getParameters();

        final List<PsiParameter> entityParams = new ArrayList<>();
        entityParamIndexes.forEach(x -> {
            if (!x.equals(currentEntityParamIndex)) {
                entityParams.add(parameters[x]);
            }
        });

        ChangeCorrectionProposal proposal = new RemoveParamsProposal(title, context.getSource().getCompilationUnit(),
                context.getASTRoot(), parentType, 0,  entityParams, false);

        try {
            WorkspaceEdit we = context.convertToWorkspaceEdit(proposal);
            toResolve.setEdit(we);
        } catch (IndexNotReadyException | ProcessCanceledException | CancellationException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to create workspace edit for code action", e);
        }
        return toResolve;
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
