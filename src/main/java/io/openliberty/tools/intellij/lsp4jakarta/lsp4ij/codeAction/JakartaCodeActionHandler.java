/*******************************************************************************
 * Copyright (c) 2019, 2023 Red Hat, Inc. and others
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 * IBM Corporation
 ******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction;

import com.intellij.psi.PsiFile;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.annotations.AnnotationConstants;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.annotations.PostConstructReturnTypeQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.annotations.RemovePostConstructAnnotationQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.annotations.RemovePreDestroyAnnotationQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.beanvalidation.BeanValidationConstants;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.beanvalidation.BeanValidationQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.annotations.AddResourceMissingNameQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.annotations.AddResourceMissingTypeQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveInjectAnnotationQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveMethodParametersQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jax_rs.Jax_RSConstants;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jax_rs.NoResourcePublicConstructorQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jax_rs.NonPublicResourceMethodQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jax_rs.ResourceMethodMultipleEntityParamsQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jsonb.JsonbAnnotationQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jsonb.JsonbConstants;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jsonb.JsonbTransientAnnotationQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence.DeleteConflictMapKeyQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence.PersistenceEntityQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence.PersistenceAnnotationQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.servlet.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.websocket.AddPathParamQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.websocket.WebSocketConstants;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAbstractModifierQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveFinalModifierQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveStaticModifierQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.di.DependencyInjectionConstants;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence.PersistenceConstants;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.java.corrections.DiagnosticsHelper;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeActionParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Code action handler. Partially reused from
 * https://github.com/eclipse/lsp4mp/blob/b88710cc54170844717f655b9bff8bb4c4649a8d/microprofile.jdt/org.eclipse.lsp4mp.jdt.core/src/main/java/org/eclipse/lsp4mp/jdt/internal/core/java/codeaction/CodeActionHandler.java
 * and
 * https://github.com/eclipse/lsp4jakarta/blob/main/jakarta.jdt/org.eclipse.lsp4jakarta.jdt.core/src/main/java/org/eclipse/lsp4jakarta/jdt/codeAction/CodeActionHandler.java
 * Modified to fit the purposes of the Jakarta Language Server with deletions of
 * some unnecessary methods and modifications.
 *
 * @author credit to Angelo ZERR
 *
 */
public class JakartaCodeActionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JakartaCodeActionHandler.class);

    public List<CodeAction> codeAction(JakartaJavaCodeActionParams params, IPsiUtils utils) {
        try {
            String uri = params.getUri();
            PsiFile unit = utils.resolveCompilationUnit(uri);
            if (unit == null) {
                return Collections.emptyList();
            }
            utils = utils.refine(utils.getModule(uri));

            // Prepare the code action invocation context
            int start = DiagnosticsHelper.getStartOffset(unit, params.getRange(), utils);
            int end = DiagnosticsHelper.getEndOffset(unit, params.getRange(), utils);
            var mpParams = new MicroProfileJavaCodeActionParams(params.getTextDocument(), params.getRange(), params.getContext());
            // We need to clone the contents of the editor and rebuild the PSI so that we can modify it in our quick fixes.
            // If we do not then we receive a write access exception because we are in a runReadAction() context.
            JavaCodeActionContext context = new JavaCodeActionContext(unit, start, end - start, utils, mpParams).copy();
            context.setASTRoot(getASTRoot(unit));

            List<CodeAction> codeActions = new ArrayList<>();

            HttpServletQuickFix httpServletQuickFix = new HttpServletQuickFix();
            FilterImplementationQuickFix filterImplementationQuickFix = new FilterImplementationQuickFix();
            ListenerImplementationQuickFix listenerImplementationQuickFix = new ListenerImplementationQuickFix();
            CompleteServletAnnotationQuickFix completeServletAnnotationQuickFix = new CompleteServletAnnotationQuickFix();
            CompleteFilterAnnotationQuickFix completeFilterAnnotationQuickFix = new CompleteFilterAnnotationQuickFix();
            PersistenceAnnotationQuickFix persistenceAnnotationQuickFix = new PersistenceAnnotationQuickFix();
            DeleteConflictMapKeyQuickFix deleteConflictMapKeyQuickFix = new DeleteConflictMapKeyQuickFix();
            NonPublicResourceMethodQuickFix nonPublicResourceMethodQuickFix = new NonPublicResourceMethodQuickFix();
            ResourceMethodMultipleEntityParamsQuickFix resourceMethodMultipleEntityParamsQuickFix = new ResourceMethodMultipleEntityParamsQuickFix();
            NoResourcePublicConstructorQuickFix noResourcePublicConstructorQuickFix = new NoResourcePublicConstructorQuickFix();
            ManagedBeanQuickFix managedBeanQuickFix = new ManagedBeanQuickFix();
            PersistenceEntityQuickFix persistenceEntityQuickFix = new PersistenceEntityQuickFix();
            ConflictProducesInjectQuickFix conflictProducesInjectQuickFix = new ConflictProducesInjectQuickFix();
            BeanValidationQuickFix beanValidationQuickFix = new BeanValidationQuickFix();
            ManagedBeanConstructorQuickFix managedBeanConstructorQuickFix = new ManagedBeanConstructorQuickFix();
            ManagedBeanNoArgConstructorQuickFix managedBeanNoArgConstructorQuickFix = new ManagedBeanNoArgConstructorQuickFix();
            JsonbAnnotationQuickFix jsonbAnnotationQuickFix = new JsonbAnnotationQuickFix();
            JsonbTransientAnnotationQuickFix jsonbTransientAnnotationQuickFix = new JsonbTransientAnnotationQuickFix();
            ScopeDeclarationQuickFix scopeDeclarationQuickFix = new ScopeDeclarationQuickFix();
            RemovePreDestroyAnnotationQuickFix removePreDestroyAnnotationQuickFix = new RemovePreDestroyAnnotationQuickFix();
            RemovePostConstructAnnotationQuickFix removePostConstructAnnotationQuickFix = new RemovePostConstructAnnotationQuickFix();
            PostConstructReturnTypeQuickFix postConstructReturnTypeQuickFix = new PostConstructReturnTypeQuickFix();
            RemoveFinalModifierQuickFix removeFinalModifierQuickFix = new RemoveFinalModifierQuickFix();
            RemoveStaticModifierQuickFix removeStaticModifierQuickFix = new RemoveStaticModifierQuickFix();
            RemoveMethodParametersQuickFix removeMethodParametersQuickFix = new RemoveMethodParametersQuickFix();
            AddResourceMissingNameQuickFix addResourceMissingNameQuickFix = new AddResourceMissingNameQuickFix();
            AddResourceMissingTypeQuickFix addResourceMissingTypeQuickFix = new AddResourceMissingTypeQuickFix();
            RemoveAbstractModifierQuickFix removeAbstractModifierQuickFix = new RemoveAbstractModifierQuickFix();
            RemoveInjectAnnotationQuickFix removeInjectAnnotationQuickFix = new RemoveInjectAnnotationQuickFix();
            RemoveProduceAnnotationQuickFix removeProduceAnnotationQuickFix = new RemoveProduceAnnotationQuickFix();
            RemoveInvalidInjectParamAnnotationQuickFix removeInvalidInjectParamAnnotationQuickFix = new RemoveInvalidInjectParamAnnotationQuickFix();
            AddPathParamQuickFix addPathParamQuickFix = new AddPathParamQuickFix();

            for (Diagnostic diagnostic : params.getContext().getDiagnostics()) {
                try {
                    if (diagnostic.getCode().getLeft().equals(ServletConstants.DIAGNOSTIC_CODE)) {
                        codeActions.addAll(httpServletQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(ServletConstants.DIAGNOSTIC_CODE_FILTER)) {
                        codeActions.addAll(filterImplementationQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(ServletConstants.DIAGNOSTIC_CODE_LISTENER)) {
                        codeActions.addAll(listenerImplementationQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(AnnotationConstants.DIAGNOSTIC_CODE_MISSING_RESOURCE_NAME_ATTRIBUTE)) {
                        codeActions.addAll(addResourceMissingNameQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(AnnotationConstants.DIAGNOSTIC_CODE_MISSING_RESOURCE_TYPE_ATTRIBUTE)) {
                        codeActions.addAll(addResourceMissingTypeQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(AnnotationConstants.DIAGNOSTIC_CODE_POSTCONSTRUCT_RETURN_TYPE)) {
                        codeActions.addAll(postConstructReturnTypeQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(ServletConstants.DIAGNOSTIC_CODE_MISSING_ATTRIBUTE)
                            || diagnostic.getCode().getLeft()
                            .equals(ServletConstants.DIAGNOSTIC_CODE_DUPLICATE_ATTRIBUTES)) {
                        codeActions
                                .addAll(completeServletAnnotationQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(ServletConstants.DIAGNOSTIC_CODE_FILTER_MISSING_ATTRIBUTE)
                            || diagnostic.getCode().getLeft()
                            .equals(ServletConstants.DIAGNOSTIC_CODE_FILTER_DUPLICATE_ATTRIBUTES)) {
                        codeActions
                                .addAll(completeFilterAnnotationQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(Jax_RSConstants.DIAGNOSTIC_CODE_NON_PUBLIC)) {
                        codeActions.addAll(nonPublicResourceMethodQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(Jax_RSConstants.DIAGNOSTIC_CODE_MULTIPLE_ENTITY_PARAMS)) {
                        codeActions.addAll(resourceMethodMultipleEntityParamsQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(Jax_RSConstants.DIAGNOSTIC_CODE_NO_PUBLIC_CONSTRUCTORS)) {
                        codeActions.addAll(noResourcePublicConstructorQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft()
                            .equals(PersistenceConstants.DIAGNOSTIC_CODE_MISSING_ATTRIBUTES)) {
                        codeActions.addAll(persistenceAnnotationQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft()
                            .equals(PersistenceConstants.DIAGNOSTIC_CODE_INVALID_ANNOTATION)) {
                        codeActions.addAll(deleteConflictMapKeyQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(PersistenceConstants.DIAGNOSTIC_CODE_MISSING_EMPTY_CONSTRUCTOR)) {
                        codeActions.addAll(persistenceEntityQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(PersistenceConstants.DIAGNOSTIC_CODE_FINAL_METHODS)
                            || diagnostic.getCode().getLeft().equals(PersistenceConstants.DIAGNOSTIC_CODE_FINAL_VARIABLES)
                            || diagnostic.getCode().getLeft().equals(PersistenceConstants.DIAGNOSTIC_CODE_FINAL_CLASS)) {
                        codeActions.addAll(removeFinalModifierQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(ManagedBeanConstants.DIAGNOSTIC_CODE)) {
                        codeActions.addAll(managedBeanQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(ManagedBeanConstants.DIAGNOSTIC_CODE_PRODUCES_INJECT)) {
                        codeActions.addAll(conflictProducesInjectQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_INJECT_PARAM)) {
                        JavaCodeActionContext contextCopy = context.copy(); // each code action needs its own context.
                        codeActions.addAll(removeInjectAnnotationQuickFix.getCodeActions(context, diagnostic));
                        codeActions.addAll(removeInvalidInjectParamAnnotationQuickFix.getCodeActions(contextCopy, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_PRODUCES_PARAM)) {
                        JavaCodeActionContext contextCopy = context.copy(); // each code action needs its own context.
                        codeActions.addAll(removeProduceAnnotationQuickFix.getCodeActions(context, diagnostic));
                        codeActions.addAll(removeInvalidInjectParamAnnotationQuickFix.getCodeActions(contextCopy, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(BeanValidationConstants.DIAGNOSTIC_CODE_STATIC)
                            || diagnostic.getCode().getLeft().equals(BeanValidationConstants.DIAGNOSTIC_CODE_INVALID_TYPE)) {
                        codeActions.addAll(beanValidationQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(ManagedBeanConstants.CONSTRUCTOR_DIAGNOSTIC_CODE)) {
                        JavaCodeActionContext contextCopy = context.copy(); // each code action needs its own context.
                        codeActions.addAll(managedBeanConstructorQuickFix.getCodeActions(context, diagnostic));
                        codeActions.addAll(managedBeanNoArgConstructorQuickFix.getCodeActions(contextCopy, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(JsonbConstants.DIAGNOSTIC_CODE_ANNOTATION)) {
                        codeActions.addAll(jsonbAnnotationQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(JsonbConstants.DIAGNOSTIC_CODE_ANNOTATION_TRANSIENT_FIELD)) {
                        codeActions.addAll(jsonbTransientAnnotationQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(ManagedBeanConstants.DIAGNOSTIC_CODE_SCOPEDECL)) {
                        codeActions.addAll(scopeDeclarationQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(DependencyInjectionConstants.DIAGNOSTIC_CODE_INJECT_FINAL)) {
                        JavaCodeActionContext contextCopy = context.copy(); // each code action needs its own context.
                        codeActions.addAll(removeInjectAnnotationQuickFix.getCodeActions(context, diagnostic));
                        codeActions.addAll(removeFinalModifierQuickFix.getCodeActions(contextCopy, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(DependencyInjectionConstants.DIAGNOSTIC_CODE_INJECT_CONSTRUCTOR) ||
                            diagnostic.getCode().getLeft().equals(DependencyInjectionConstants.DIAGNOSTIC_CODE_INJECT_GENERIC)) {
                        codeActions.addAll(removeInjectAnnotationQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(DependencyInjectionConstants.DIAGNOSTIC_CODE_INJECT_ABSTRACT)) {
                        JavaCodeActionContext contextCopy = context.copy(); // each code action needs its own context.
                        codeActions.addAll(removeInjectAnnotationQuickFix.getCodeActions(context, diagnostic));
                        codeActions.addAll(removeAbstractModifierQuickFix.getCodeActions(contextCopy, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(DependencyInjectionConstants.DIAGNOSTIC_CODE_INJECT_STATIC)) {
                        JavaCodeActionContext contextCopy = context.copy(); // each code action needs its own context.
                        codeActions.addAll(removeInjectAnnotationQuickFix.getCodeActions(context, diagnostic));
                        codeActions.addAll(removeStaticModifierQuickFix.getCodeActions(contextCopy, diagnostic));
                    }

                    if (diagnostic.getCode().getLeft().equals(AnnotationConstants.DIAGNOSTIC_CODE_POSTCONSTRUCT_PARAMS)) {
                        JavaCodeActionContext contextCopy = context.copy(); // each code action needs its own context.
                        codeActions.addAll(removePostConstructAnnotationQuickFix.getCodeActions(context, diagnostic));
                        codeActions.addAll(removeMethodParametersQuickFix.getCodeActions(contextCopy, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(AnnotationConstants.DIAGNOSTIC_CODE_PREDESTROY_STATIC)) {
                        JavaCodeActionContext contextCopy = context.copy(); // each code action needs its own context.
                        codeActions.addAll(removePreDestroyAnnotationQuickFix.getCodeActions(context, diagnostic));
                        codeActions.addAll(removeStaticModifierQuickFix.getCodeActions(contextCopy, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(AnnotationConstants.DIAGNOSTIC_CODE_PREDESTROY_PARAMS)) {
                        JavaCodeActionContext contextCopy = context.copy(); // each code action needs its own context.
                        codeActions.addAll(removePreDestroyAnnotationQuickFix.getCodeActions(context, diagnostic));
                        codeActions.addAll(removeMethodParametersQuickFix.getCodeActions(contextCopy, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(WebSocketConstants.DIAGNOSTIC_CODE_PATH_PARAMS_ANNOT)) {
                        codeActions.addAll(addPathParamQuickFix.getCodeActions(context, diagnostic));
                    }
                } catch (Exception e) {
                    LOGGER.warn("Exception scanning diagnostics", e); // TODO do we need this? Remove if possible
                }
            }
            return Collections.emptyList();//codeActions;
        } catch (IOException x) {
            return Collections.emptyList();
        }
    }

    private static PsiFile getASTRoot(PsiFile unit) {
        return unit;
    }
}
