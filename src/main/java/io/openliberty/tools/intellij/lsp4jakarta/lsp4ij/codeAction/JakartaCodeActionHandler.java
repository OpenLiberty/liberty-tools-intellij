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
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.beanvalidation.BeanValidationConstants;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.beanvalidation.BeanValidationQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstructorQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanNoArgConstructorQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.RemoveInvalidInjectParamAnnotationQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.annotations.AddResourceMissingNameQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.annotations.AddResourceMissingTypeQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveMethodParametersQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jax_rs.Jax_RSConstants;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jax_rs.NoResourcePublicConstructorQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jax_rs.NonPublicResourceMethodQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jax_rs.ResourceMethodMultipleEntityParamsQuickFix;
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

            HttpServletQuickFix HttpServletQuickFix = new HttpServletQuickFix();
            FilterImplementationQuickFix FilterImplementationQuickFix = new FilterImplementationQuickFix();
            ListenerImplementationQuickFix ListenerImplementationQuickFix = new ListenerImplementationQuickFix();
            CompleteServletAnnotationQuickFix CompleteServletAnnotationQuickFix = new CompleteServletAnnotationQuickFix();
            CompleteFilterAnnotationQuickFix CompleteFilterAnnotationQuickFix = new CompleteFilterAnnotationQuickFix();
            PersistenceAnnotationQuickFix PersistenceAnnotationQuickFix = new PersistenceAnnotationQuickFix();
//            DeleteConflictMapKeyQuickFix DeleteConflictMapKeyQuickFix = new DeleteConflictMapKeyQuickFix();
            NonPublicResourceMethodQuickFix NonPublicResourceMethodQuickFix = new NonPublicResourceMethodQuickFix();
            ResourceMethodMultipleEntityParamsQuickFix ResourceMethodMultipleEntityParamsQuickFix = new ResourceMethodMultipleEntityParamsQuickFix();
            NoResourcePublicConstructorQuickFix NoResourcePublicConstructorQuickFix = new NoResourcePublicConstructorQuickFix();
            ManagedBeanQuickFix ManagedBeanQuickFix = new ManagedBeanQuickFix();
            PersistenceEntityQuickFix PersistenceEntityQuickFix = new PersistenceEntityQuickFix();
//            ConflictProducesInjectQuickFix ConflictProducesInjectQuickFix = new ConflictProducesInjectQuickFix();
            BeanValidationQuickFix BeanValidationQuickFix = new BeanValidationQuickFix();
            ManagedBeanConstructorQuickFix ManagedBeanConstructorQuickFix = new ManagedBeanConstructorQuickFix();
            ManagedBeanNoArgConstructorQuickFix ManagedBeanNoArgConstructorQuickFix = new ManagedBeanNoArgConstructorQuickFix();
//            JsonbAnnotationQuickFix JsonbAnnotationQuickFix = new JsonbAnnotationQuickFix();
//            JsonbTransientAnnotationQuickFix JsonbTransientAnnotationQuickFix = new JsonbTransientAnnotationQuickFix();
//            ScopeDeclarationQuickFix ScopeDeclarationQuickFix = new ScopeDeclarationQuickFix();
//            RemovePreDestroyAnnotationQuickFix RemovePreDestroyAnnotationQuickFix = new RemovePreDestroyAnnotationQuickFix();
//            RemovePostConstructAnnotationQuickFix RemovePostConstructAnnotationQuickFix = new RemovePostConstructAnnotationQuickFix();
            PostConstructReturnTypeQuickFix PostConstructReturnTypeQuickFix = new PostConstructReturnTypeQuickFix();
            RemoveFinalModifierQuickFix RemoveFinalModifierQuickFix = new RemoveFinalModifierQuickFix();
            RemoveStaticModifierQuickFix RemoveStaticModifierQuickFix = new RemoveStaticModifierQuickFix();
            RemoveMethodParametersQuickFix RemoveMethodParametersQuickFix = new RemoveMethodParametersQuickFix();
            AddResourceMissingNameQuickFix AddResourceMissingNameQuickFix = new AddResourceMissingNameQuickFix();
            AddResourceMissingTypeQuickFix AddResourceMissingTypeQuickFix = new AddResourceMissingTypeQuickFix();
            RemoveAbstractModifierQuickFix RemoveAbstractModifierQuickFix = new RemoveAbstractModifierQuickFix();
//            RemoveInjectAnnotationQuickFix RemoveInjectAnnotationQuickFix = new RemoveInjectAnnotationQuickFix();
//            RemoveProduceAnnotationQuickFix RemoveProduceAnnotationQuickFix = new RemoveProduceAnnotationQuickFix();
            RemoveInvalidInjectParamAnnotationQuickFix RemoveInvalidInjectParamAnnotationQuickFix = new RemoveInvalidInjectParamAnnotationQuickFix();
            AddPathParamQuickFix AddPathParamQuickFix = new AddPathParamQuickFix();

            for (Diagnostic diagnostic : params.getContext().getDiagnostics()) {
                try {
                    if (diagnostic.getCode().getLeft().equals(ServletConstants.DIAGNOSTIC_CODE)) {
                        codeActions.addAll(HttpServletQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(ServletConstants.DIAGNOSTIC_CODE_FILTER)) {
                        codeActions.addAll(FilterImplementationQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(ServletConstants.DIAGNOSTIC_CODE_LISTENER)) {
                        codeActions.addAll(ListenerImplementationQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(AnnotationConstants.DIAGNOSTIC_CODE_MISSING_RESOURCE_NAME_ATTRIBUTE)) {
                        codeActions.addAll(AddResourceMissingNameQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(AnnotationConstants.DIAGNOSTIC_CODE_MISSING_RESOURCE_TYPE_ATTRIBUTE)) {
                        codeActions.addAll(AddResourceMissingTypeQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(AnnotationConstants.DIAGNOSTIC_CODE_POSTCONSTRUCT_RETURN_TYPE)) {
                        codeActions.addAll(PostConstructReturnTypeQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(ServletConstants.DIAGNOSTIC_CODE_MISSING_ATTRIBUTE)
                            || diagnostic.getCode().getLeft()
                            .equals(ServletConstants.DIAGNOSTIC_CODE_DUPLICATE_ATTRIBUTES)) {
                        codeActions
                                .addAll(CompleteServletAnnotationQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(ServletConstants.DIAGNOSTIC_CODE_FILTER_MISSING_ATTRIBUTE)
                            || diagnostic.getCode().getLeft()
                            .equals(ServletConstants.DIAGNOSTIC_CODE_FILTER_DUPLICATE_ATTRIBUTES)) {
                        codeActions
                                .addAll(CompleteFilterAnnotationQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(Jax_RSConstants.DIAGNOSTIC_CODE_NON_PUBLIC)) {
                        codeActions.addAll(NonPublicResourceMethodQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(Jax_RSConstants.DIAGNOSTIC_CODE_MULTIPLE_ENTITY_PARAMS)) {
                        codeActions.addAll(ResourceMethodMultipleEntityParamsQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(Jax_RSConstants.DIAGNOSTIC_CODE_NO_PUBLIC_CONSTRUCTORS)) {
                        codeActions.addAll(NoResourcePublicConstructorQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft()
                            .equals(PersistenceConstants.DIAGNOSTIC_CODE_MISSING_ATTRIBUTES)) {
                        codeActions.addAll(PersistenceAnnotationQuickFix.getCodeActions(context, diagnostic));
                    }
//                    if (diagnostic.getCode().getLeft()
//                            .equals(PersistenceConstants.DIAGNOSTIC_CODE_INVALID_ANNOTATION)) {
//                        codeActions.addAll(DeleteConflictMapKeyQuickFix.getCodeActions(context, diagnostic, monitor));
//                    }
                    if (diagnostic.getCode().getLeft().equals(PersistenceConstants.DIAGNOSTIC_CODE_MISSING_EMPTY_CONSTRUCTOR)) {
                        codeActions.addAll(PersistenceEntityQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(PersistenceConstants.DIAGNOSTIC_CODE_FINAL_METHODS)
                            || diagnostic.getCode().getLeft().equals(PersistenceConstants.DIAGNOSTIC_CODE_FINAL_VARIABLES)
                            || diagnostic.getCode().getLeft().equals(PersistenceConstants.DIAGNOSTIC_CODE_FINAL_CLASS)) {
                        codeActions.addAll(RemoveFinalModifierQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(ManagedBeanConstants.DIAGNOSTIC_CODE)) {
                        codeActions.addAll(ManagedBeanQuickFix.getCodeActions(context, diagnostic));
                    }
//                    if (diagnostic.getCode().getLeft().equals(ManagedBeanConstants.DIAGNOSTIC_CODE_PRODUCES_INJECT)) {
//                        codeActions.addAll(ConflictProducesInjectQuickFix.getCodeActions(context, diagnostic, monitor));
//                    }
                    if (diagnostic.getCode().getLeft().equals(ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_INJECT_PARAM)) {
//                        codeActions.addAll(RemoveInjectAnnotationQuickFix.getCodeActions(context, diagnostic, monitor));
                        codeActions.addAll(RemoveInvalidInjectParamAnnotationQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_PRODUCES_PARAM)) {
//                        codeActions.addAll(RemoveProduceAnnotationQuickFix.getCodeActions(context, diagnostic, monitor));
                        codeActions.addAll(RemoveInvalidInjectParamAnnotationQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(BeanValidationConstants.DIAGNOSTIC_CODE_STATIC)
                            || diagnostic.getCode().getLeft().equals(BeanValidationConstants.DIAGNOSTIC_CODE_INVALID_TYPE)) {
                        codeActions.addAll(BeanValidationQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(ManagedBeanConstants.CONSTRUCTOR_DIAGNOSTIC_CODE)) {
                        codeActions.addAll(ManagedBeanConstructorQuickFix.getCodeActions(context, diagnostic));
                        codeActions.addAll(ManagedBeanNoArgConstructorQuickFix.getCodeActions(context, diagnostic));
                    }
//                    if (diagnostic.getCode().getLeft().equals(JsonbConstants.DIAGNOSTIC_CODE_ANNOTATION)) {
//                        codeActions.addAll(JsonbAnnotationQuickFix.getCodeActions(context, diagnostic, monitor));
//                    }
//                    if (diagnostic.getCode().getLeft().equals(JsonbConstants.DIAGNOSTIC_CODE_ANNOTATION_TRANSIENT_FIELD)) {
//                        codeActions.addAll(JsonbTransientAnnotationQuickFix.getCodeActions(context, diagnostic, monitor));
//                    }
//                    if (diagnostic.getCode().getLeft().equals(ManagedBeanConstants.DIAGNOSTIC_CODE_SCOPEDECL)) {
//                        codeActions.addAll(ScopeDeclarationQuickFix.getCodeActions(context, diagnostic, monitor));
//                    }
                    if (diagnostic.getCode().getLeft().equals(DependencyInjectionConstants.DIAGNOSTIC_CODE_INJECT_FINAL)) {
//                        codeActions.addAll(RemoveInjectAnnotationQuickFix.getCodeActions(context, diagnostic, monitor));
                        codeActions.addAll(RemoveFinalModifierQuickFix.getCodeActions(context, diagnostic));
                    }
//                    if (diagnostic.getCode().getLeft().equals(DependencyInjectionConstants.DIAGNOSTIC_CODE_INJECT_CONSTRUCTOR) ||
//                            diagnostic.getCode().getLeft().equals(DependencyInjectionConstants.DIAGNOSTIC_CODE_INJECT_GENERIC)) {
//                        codeActions.addAll(RemoveInjectAnnotationQuickFix.getCodeActions(context, diagnostic, monitor));
//                    }
                    if (diagnostic.getCode().getLeft().equals(DependencyInjectionConstants.DIAGNOSTIC_CODE_INJECT_ABSTRACT)) {
//                        codeActions.addAll(RemoveInjectAnnotationQuickFix.getCodeActions(context, diagnostic, monitor));
                        codeActions.addAll(RemoveAbstractModifierQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(DependencyInjectionConstants.DIAGNOSTIC_CODE_INJECT_STATIC)) {
//                        codeActions.addAll(RemoveInjectAnnotationQuickFix.getCodeActions(context, diagnostic, monitor));
                        codeActions.addAll(RemoveStaticModifierQuickFix.getCodeActions(context, diagnostic));
                    }

                    if (diagnostic.getCode().getLeft().equals(AnnotationConstants.DIAGNOSTIC_CODE_POSTCONSTRUCT_PARAMS)) {
//                        codeActions.addAll(RemovePostConstructAnnotationQuickFix.getCodeActions(context, diagnostic, monitor));
                        codeActions.addAll(RemoveMethodParametersQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(AnnotationConstants.DIAGNOSTIC_CODE_PREDESTROY_STATIC)) {
//                        codeActions.addAll(RemovePreDestroyAnnotationQuickFix.getCodeActions(context, diagnostic, monitor));
                        codeActions.addAll(RemoveStaticModifierQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(AnnotationConstants.DIAGNOSTIC_CODE_PREDESTROY_PARAMS)) {
//                        codeActions.addAll(RemovePreDestroyAnnotationQuickFix.getCodeActions(context, diagnostic, monitor));
                        codeActions.addAll(RemoveMethodParametersQuickFix.getCodeActions(context, diagnostic));
                    }
                    if (diagnostic.getCode().getLeft().equals(WebSocketConstants.DIAGNOSTIC_CODE_PATH_PARAMS_ANNOT)) {
                        codeActions.addAll(AddPathParamQuickFix.getCodeActions(context, diagnostic));
                    }
                } catch (Exception e) {
                    LOGGER.warn("Exception scanning diagnostics", e); // TODO do we need this? Remove if possible
                }
            }
            return codeActions;
        } catch (IOException x) {
            return Collections.emptyList();
        }
    }

    private static PsiFile getASTRoot(PsiFile unit) {
        return unit;
    }
}
