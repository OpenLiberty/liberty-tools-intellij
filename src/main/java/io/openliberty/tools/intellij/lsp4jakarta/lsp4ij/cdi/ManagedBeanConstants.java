/*******************************************************************************
 * Copyright (c) 2021, 2026 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Hani Damlaj
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ManagedBeanConstants {
    /* Annotation Constants */
    public static final String PRODUCES_FQ_NAME = "jakarta.enterprise.inject.Produces";
    public static final String INJECT_FQ_NAME = "jakarta.inject.Inject";
    public static final String DISPOSES_FQ_NAME = "jakarta.enterprise.inject.Disposes";
    public static final String OBSERVES_FQ_NAME = "jakarta.enterprise.event.Observes";
    public static final String OBSERVES_ASYNC_FQ_NAME = "jakarta.enterprise.event.ObservesAsync";
    public static final String INTERCEPTOR_FQ_NAME = "jakarta.interceptor.Interceptor";
    public static final String DECORATOR_FQ_NAME = "jakarta.decorator.Decorator";
    public static final String DEPENDENT_FQ_NAME = "jakarta.enterprise.context.Dependent";
    public static final String SINGLETON_FQ_NAME = "jakarta.ejb.Singleton";
    public static final String APPLICATION_SCOPED_FQ_NAME = "jakarta.enterprise.context.ApplicationScoped";
    public static final String STATELESS_FQ_NAME = "jakarta.ejb.Stateless";
    public static final String NORMAL_SCOPE_FQ_NAME = "jakarta.enterprise.context.NormalScope";
    public static final String NAMED_FQ_NAME = "jakarta.inject.Named";
    public static final String DELEGATE_FQ_NAME = "jakarta.decorator.Delegate";

    public static final String DIAGNOSTIC_SOURCE = "jakarta-cdi";
    public static final String DIAGNOSTIC_CODE = "InvalidManagedBeanAnnotation";
    public static final String DIAGNOSTIC_CODE_SCOPEDECL = "InvalidScopeDecl";
    public static final String DIAGNOSTIC_CODE_INVALID_SINGLETON_SCOPE = "InvalidSingletonSessionBeanScope";
    public static final String DIAGNOSTIC_CODE_PRODUCES_INJECT = "RemoveProducesOrInject";
    public static final String DIAGNOSTIC_CODE_STATELESS_ILLEGAL_SCOPE = "InvalidStatelessSessionBeanScope";
    public static final String DIAGNOSTIC_CODE_PRODUCER_FIELD_NAMED = "InvalidProducerFieldWithNamedAnnotation";

    public static final String CONSTRUCTOR_DIAGNOSTIC_CODE = "InvalidManagedBeanConstructor";

    public static final String DIAGNOSTIC_CODE_INVALID_INJECT_PARAM = "RemoveInjectOrConflictedAnnotations";
    public static final String DIAGNOSTIC_CODE_INVALID_PRODUCES_PARAM = "RemoveProducesOrConflictedAnnotations";
    public static final String DIAGNOSTIC_CODE_INVALID_DISPOSES_PARAM = "RemoveDisposesOrConflictedAnnotations";
    public static final String DIAGNOSTIC_INJECT_MULTIPLE_METHOD_PARAM = "InvalidInjectAnnotationOnMultipleMethodParams";
    public static final String DIAGNOSTIC_OBSERVES_OBSERVESASYNC_PARAM_CONFLICT = "InvalidObservesObservesAsyncMethodParams";
    public static final String DIAGNOSTIC_CODE_INTERCEPTOR_DECORATOR_OBSERVER = "InvalidInterceptorOrDecoratorWithObserverMethod";
    public static final String DIAGNOSTIC_CODE_INVALID_DECORATOR_DELEGATE = "InvalidDecoratorDelegateInjectionPoints";
    public static final String DIAGNOSTIC_CODE_INVALID_DELEGATE_INJECTION_POINT = "InvalidDelegateInjectionPoint";
    public static final String DIAGNOSTIC_CODE_DEPENDENT_CONDITIONAL_OBSERVER = "InvalidDependentScopeWithConditionalObserver";
    public static final String DIAGNOSTIC_MULTIPLE_OBSERVER_PARAMS = "InvalidMultipleObserverParams";
    public static final String DIAGNOSTIC_CODE_INTERCEPTOR_DECORATOR_ILLEGAL_SCOPE = "InvalidInterceptorOrDecorator";
    public static final String DIAGNOSTIC_CODE_REDUNDANT_DISPOSES = "RemoveExtraDisposes";
    //Added as part of fix that adds two quick fixes which are mutually exclusive issue #540
    public static final String[] INVALID_DISPOSER_FQ_PARAMS = { DISPOSES_FQ_NAME };
    public static final String[] INVALID_DISPOSER_FQ_CONFLICTED_PARAMS = { OBSERVES_FQ_NAME, OBSERVES_ASYNC_FQ_NAME };
    public static final String[] INVALID_INJECT_PARAMS_FQ = { DISPOSES_FQ_NAME, OBSERVES_FQ_NAME,
            OBSERVES_ASYNC_FQ_NAME };

    // List can be found in the cdi doc here:
    // https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html#bean_defining_annotations
    public static final Set<String> SCOPE_FQ_NAMES = new HashSet<String>(
            Arrays.asList(DEPENDENT_FQ_NAME, "jakarta.enterprise.context.ApplicationScoped",
                    "jakarta.enterprise.context.ConversationScoped", "jakarta.enterprise.context.RequestScoped",
                    "jakarta.enterprise.context.SessionScoped", "jakarta.enterprise.context.NormalScope",
                    "jakarta.Interceptor", "jakarta.Decorator", "jakarta.enterprise.inject.Stereotype"));

    // Scopes that are invalid for interceptors and decorators (they must use @Dependent only)
    public static final String[] INVALID_INTERCEPTOR_DECORATOR_SCOPES = {
            "jakarta.enterprise.context.ApplicationScoped",
            "jakarta.enterprise.context.SessionScoped",
            "jakarta.enterprise.context.ConversationScoped",
            "jakarta.enterprise.context.NormalScope",
            "jakarta.enterprise.context.RequestScoped"
    };

    public static final Set<String> INVALID_OBSERVES_OBSERVES_ASYNC_CONFLICTED_PARAMS = Set.of(OBSERVES_FQ_NAME, OBSERVES_ASYNC_FQ_NAME);
}