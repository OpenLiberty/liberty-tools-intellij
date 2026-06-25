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
 *     Hani Damlaj, Jianing Xu
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi;

import java.util.*;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils.getSimpleName;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.*;

public class ManagedBeanDiagnosticsCollector extends AbstractDiagnosticsCollector {

    private static final Logger LOGGER = Logger.getLogger(ManagedBeanDiagnosticsCollector.class.getName());

    public ManagedBeanDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit == null)
            return;

        PsiClass[] types = unit.getClasses();
        String[] scopeFQNames = SCOPE_FQ_NAMES.toArray(String[]::new);
        for (PsiClass type : types) {
            PsiAnnotation[] typeAnnotations = type.getAnnotations();
            List<String> managedBeanAnnotations = getMatchedJavaElementNames(type, Stream.of(typeAnnotations)
                            .map(annotation -> annotation.getQualifiedName()).toArray(String[]::new),
                    scopeFQNames);
            boolean isManagedBean = !managedBeanAnnotations.isEmpty();
            boolean isDependent = managedBeanAnnotations.stream().anyMatch(DEPENDENT_FQ_NAME::equals);
            boolean hasMultipleScopes = managedBeanAnnotations.size() > 1;
            // Check if the class is an interceptor or decorator
            boolean interceptorOrDecorator = !getMatchedJavaElementNames(type,
                    Stream.of(typeAnnotations).map(PsiAnnotation::getQualifiedName).toArray(String[]::new),
                    new String[]{
                            INTERCEPTOR_FQ_NAME,
                            DECORATOR_FQ_NAME
                    }).isEmpty();
            String[] injectAnnotations = { PRODUCES_FQ_NAME, INJECT_FQ_NAME };
            PsiField fields[] = type.getFields();
            boolean nonStaticPublicFieldPresent = false;
            for (PsiField field : fields) {
                String[] annotationNames = Stream.of(field.getAnnotations())
                        .map(annotation -> annotation.getQualifiedName()).toArray(String[]::new);
                List<String> fieldScopes = getMatchedJavaElementNames(type, annotationNames, scopeFQNames);

                /**
                 * If a managed bean has a non-static public field, it must have
                 * scope @Dependent. If a managed bean with a non-static public field declares
                 * any scope other than @Dependent, the container automatically detects the
                 * problem and treats it as a definition error.
                 *
                 * https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html#managed_beans
                 */
                if (validateNonStaticPublicField(isManagedBean, isDependent, hasMultipleScopes, field)) {
                    nonStaticPublicFieldPresent = true;
                    diagnostics.add(createDiagnostic(field, unit,
                            Messages.getMessage("ManagedBeanWithNonStaticPublicField"),
                            DIAGNOSTIC_CODE, null,
                            DiagnosticSeverity.Error));
                }

                /**
                 * https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html#declaring_bean_scope
                 * A bean class or producer method or field may specify at most one scope type
                 * annotation. If a bean class or producer method or field specifies multiple
                 * scope type annotations, the container automatically detects the problem and
                 * treats it as a definition error.
                 *
                 * Here we only look at the fields.
                 */
                List<String> fieldInjects = getMatchedJavaElementNames(type, annotationNames, injectAnnotations);
                boolean isProducerField = false, isInjectField = false;
                for (String annotation : fieldInjects) {
                    if (PRODUCES_FQ_NAME.equals(annotation))
                        isProducerField = true;
                    else if (INJECT_FQ_NAME.equals(annotation))
                        isInjectField = true;
                }
                if (isProducerField && fieldScopes.size() > 1) {
                    fieldScopes.add(PRODUCES_FQ_NAME);
                    diagnostics.add(createDiagnostic(field, unit,
                            Messages.getMessage("ScopeTypeAnnotationsProducerField"),
                            DIAGNOSTIC_CODE_SCOPEDECL, (JsonArray) (new Gson().toJsonTree(fieldScopes)),
                            DiagnosticSeverity.Error));
                }

                if (isProducerField && isInjectField) {
                    /*
                     * ========= Produces and Inject Annotations Checks =========
                     *
                     * go through each field and method to make sure @Produces and @Inject are not used together
                     *
                     * see: https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html#declaring_producer_field
                     * https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html#declaring_producer_method
                     * https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html#declaring_injected_field
                     * https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html#declaring_initializer
                     */

                    // A single field cannot have the same
                    diagnostics.add(createDiagnostic(field, unit,
                            Messages.getMessage("ManagedBeanProducesAndInject"),
                            ManagedBeanConstants.DIAGNOSTIC_CODE_PRODUCES_INJECT, null, DiagnosticSeverity.Error));
                }

                /**
                 * Producer fields must not declare a bean name using @Named annotation.
                 *
                 * https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html#declaring_producer_field
                 * Section 3.3.2: A producer field may have a bean name, specified using the @Named qualifier.
                 * However, the specification states that producer fields must not declare a bean name using @Named.
                 */
                if (isProducerField) {
                    for (PsiAnnotation annotation : field.getAnnotations()) {
                        if (isMatchedJavaElement(type, annotation.getQualifiedName(), NAMED_FQ_NAME)) {
                            diagnostics.add(createDiagnostic(annotation, unit,
                                    Messages.getMessage("ProducerFieldWithNamedAnnotation", field.getName()),
                                    DIAGNOSTIC_CODE_PRODUCER_FIELD_NAMED, null,
                                    DiagnosticSeverity.Error));
                            break;
                        }
                    }
                }

            }

            PsiMethod[] methods = type.getMethods();
            List<PsiMethod> constructorMethods = new ArrayList<PsiMethod>();
            for (PsiMethod method : methods) {

                // Find all methods on the type that are constructors.
                if (isConstructorMethod(method))
                    constructorMethods.add(method);

                /**
                 * https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html#declaring_bean_scope
                 * A bean class or producer method or field may specify at most one scope type
                 * annotation. If a bean class or producer method or field specifies multiple
                 * scope type annotations, the container automatically detects the problem and
                 * treats it as a definition error.
                 *
                 * Here we only look at the methods.
                 */
                String[] annotationNames = Stream.of(method.getAnnotations())
                        .map(annotation -> annotation.getQualifiedName()).toArray(String[]::new);
                List<String> methodScopes = getMatchedJavaElementNames(type, annotationNames, scopeFQNames);
                List<String> methodInjects = getMatchedJavaElementNames(type, annotationNames, injectAnnotations);
                boolean isProducerMethod = false, isInjectMethod = false;
                for (String annotation : methodInjects) {
                    if (PRODUCES_FQ_NAME.equals(annotation))
                        isProducerMethod = true;
                    else if (INJECT_FQ_NAME.equals(annotation))
                        isInjectMethod = true;
                }

                if (isProducerMethod && methodScopes.size() > 1) {
                    methodScopes.add(PRODUCES_FQ_NAME);
                    diagnostics.add(createDiagnostic(method, unit,
                            Messages.getMessage("ScopeTypeAnnotationsProducerMethod"),
                            DIAGNOSTIC_CODE_SCOPEDECL, (JsonArray) (new Gson().toJsonTree(methodScopes)),
                            DiagnosticSeverity.Error));
                }

                if (isProducerMethod && isInjectMethod) {
                    /*
                     * ========= Produces and Inject Annotations Checks =========
                     *
                     * go through each field and method to make sure @Produces and @Inject are not used together
                     *
                     * see: https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html#declaring_producer_field
                     * https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html#declaring_producer_method
                     * https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html#declaring_injected_field
                     * https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html#declaring_initializer
                     */

                    // A single method cannot have the same
                    diagnostics.add(createDiagnostic(method, unit,
                            Messages.getMessage("ManagedBeanProducesAndInject"),
                            ManagedBeanConstants.DIAGNOSTIC_CODE_PRODUCES_INJECT, null, DiagnosticSeverity.Error));
                }
                // Generate diagnostics for mutually exclusive observes and observesAsync annotations
                //
                // see: https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0#
                // observer_methods
                Set<String> conflictParams = new HashSet<>();
                List<PsiParameter> paramsWithObserverAnnotations = new ArrayList<>();
                for (PsiParameter param : method.getParameterList().getParameters()) {
                    String[] annotationQualifiedNames = Stream.of(param.getAnnotations()).map(annotation -> annotation.getQualifiedName()).toArray(String[]::new);
                    String[] conflictedParamAnnotations = INVALID_OBSERVES_OBSERVES_ASYNC_CONFLICTED_PARAMS.toArray(String[]::new);
                    Set<String> observesObservesAsync = new HashSet<>(getMatchedJavaElementNames(type, annotationQualifiedNames, conflictedParamAnnotations));
                    if (observesObservesAsync.equals(INVALID_OBSERVES_OBSERVES_ASYNC_CONFLICTED_PARAMS)) {
                        conflictParams.add(param.getName());
                    }
                    // Track parameters with @Observes or @ObservesAsync annotations
                    if (!observesObservesAsync.isEmpty()) {
                        paramsWithObserverAnnotations.add(param);
                    }
                }
                if (interceptorOrDecorator && !paramsWithObserverAnnotations.isEmpty()) {
                    diagnostics.add(createDiagnostic(method, unit,
                            Messages.getMessage("InvalidInterceptorOrDecoratorWithObserverMethod"),
                            DIAGNOSTIC_CODE_INTERCEPTOR_DECORATOR_OBSERVER,
                            null,
                            DiagnosticSeverity.Error));
                } else if (!conflictParams.isEmpty()) {
                    diagnostics.add(createDiagnostic(method, unit, Messages.getMessage("ManagedBeanObservesAndObservesAsyncParam", String.join(", ", conflictParams)),
                            DIAGNOSTIC_OBSERVES_OBSERVESASYNC_PARAM_CONFLICT, null, DiagnosticSeverity.Error));
                } else if (paramsWithObserverAnnotations.size() > 1) {
                    // Generate diagnostic for multiple observer parameters
                    // A method cannot have more than one parameter annotated with @Observes or @ObservesAsync
                    String paramNames = paramsWithObserverAnnotations.stream()
                            .map(PsiParameter::getName)
                            .collect(Collectors.joining(", "));
                    diagnostics.add(createDiagnostic(method, unit, Messages.getMessage("ManagedBeanMultipleObserverParams", paramNames),
                            DIAGNOSTIC_MULTIPLE_OBSERVER_PARAMS, null, DiagnosticSeverity.Error));
                } else if (isDependent && hasConditionalObserverAnnotation(type, method)) {
                    // Check for conditional observer methods on @Dependent scoped beans
                    // Beans with scope @Dependent may not have conditional observer methods.
                    // If a bean with scope @Dependent has an observer method declared notifyObserver=IF_EXISTS,
                    // the container automatically detects the problem and treats it as a definition error.
                    diagnostics.add(createDiagnostic(method, unit,
                            Messages.getMessage("ManagedBeanDependentScopeConditionalObserver", method.getName()),
                            DIAGNOSTIC_CODE_DEPENDENT_CONDITIONAL_OBSERVER, null, DiagnosticSeverity.Error));
                }
                // Check for @Disposes in interceptors/decorators
                if (interceptorOrDecorator) {
                    List<String> disposesParams = getDisposesParamNames(type, method);
                    if (!disposesParams.isEmpty()) {
                        String paramNames = String.join(", ", disposesParams);
                        diagnostics.add(createDiagnostic(method, unit,
                                Messages.getMessage("InvalidInterceptorOrDecoratorWithDisposerMethod", paramNames),
                                DIAGNOSTIC_CODE_INTERCEPTOR_DECORATOR_DISPOSER, null, DiagnosticSeverity.Error));
                    }
                }
                
            }

            if (isManagedBean && constructorMethods.size() > 0) {
                /**
                 * If the managed bean does not have a constructor that takes no parameters, it
                 * must have a constructor annotated @Inject. No additional special annotations
                 * are required.
                 */

                // If there are no constructor methods, there is an implicit empty constructor
                // generated by the compiler.
                List<PsiMethod> methodsNeedingDiagnostics = new ArrayList<PsiMethod>();
                for (PsiMethod m : constructorMethods) {
                    if (m.getParameterList().getParametersCount() == 0) {
                        methodsNeedingDiagnostics.clear();
                        break;
                    }
                    PsiAnnotation[] annotations = m.getAnnotations();
                    boolean hasParameterizedInjectConstructor = false;
                    // look up '@Inject' annotation
                    for (PsiAnnotation annotation : annotations) {
                        if (isMatchedJavaElement(type, annotation.getQualifiedName(), INJECT_FQ_NAME)) {
                            hasParameterizedInjectConstructor = true;
                            break;
                        }
                    }
                    if (hasParameterizedInjectConstructor) {
                        methodsNeedingDiagnostics.clear();
                        break;
                    } else
                        methodsNeedingDiagnostics.add(m);
                }

                // Deliver a diagnostic on all parameterized constructors that they must add an
                // @Inject annotation
                for (PsiMethod m : methodsNeedingDiagnostics) {
                    diagnostics.add(createDiagnostic(m, unit, Messages.getMessage("ManagedBeanConstructorWithParameters"),
                            CONSTRUCTOR_DIAGNOSTIC_CODE, null, DiagnosticSeverity.Error));
                }
            }

            /**
             * If a managed bean class is of generic type, it must be annotated with @Dependent
             */
            if (isManagedBean) {
                validateSingletonSessionBean(unit, diagnostics, type, managedBeanAnnotations);
                boolean isStateless = !getMatchedJavaElementNames(type, Stream.of(typeAnnotations)
                                .map(PsiAnnotation::getQualifiedName).toArray(String[]::new),
                        new String[]{STATELESS_FQ_NAME}).isEmpty();
                boolean isClassGeneric = type.getTypeParameters().length != 0;
                if (isClassGeneric && (!isDependent || hasMultipleScopes)) {
                    diagnostics.add(createDiagnostic(type, unit, Messages.getMessage("ManagedBeanGenericType"),
                            DIAGNOSTIC_CODE, null, DiagnosticSeverity.Error));
                } else if (nonStaticPublicFieldPresent) {
                    diagnostics.add(createDiagnostic(type, unit, Messages.getMessage("ManagedBeanWithNonStaticPublicField"),
                            DIAGNOSTIC_CODE, null, DiagnosticSeverity.Error));
                } else if (isStateless && (!isDependent || hasMultipleScopes)) {
                    /**
                     * A stateless session bean must belong to the @Dependent scope.
                     * If a session bean specifies an illegal scope, the container automatically detects
                     * the problem and treats it as a definition error.
                     *
                     * https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html#stateless_session_beans
                     */
                    diagnostics.add(createDiagnostic(type, unit,
                            Messages.getMessage("StatelessSessionBeanWithIllegalScope"),
                            DIAGNOSTIC_CODE_STATELESS_ILLEGAL_SCOPE, null, DiagnosticSeverity.Error));
                } else if (hasMultipleScopes) {
                    diagnostics.add(createDiagnostic(type, unit,
                            Messages.getMessage("ScopeTypeAnnotationsManagedBean"),
                            DIAGNOSTIC_CODE_SCOPEDECL, new Gson().toJsonTree(managedBeanAnnotations),
                            DiagnosticSeverity.Error));
                }
            }

            /*
             * ========= Inject and Disposes, Observes, ObservesAsync Annotations Checks=========
             */
            /*
             * go through each method to make sure @Inject
             * and @Disposes, @Observes, @ObservesAsync are not used together
             *
             * see: https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html#declaring_bean_constructor
             * https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html#declaring_initializer
             *
             */
            invalidParamsCheck(unit, diagnostics, type, INJECT_FQ_NAME,
                    ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_INJECT_PARAM);
            // Interceptors and decorators must not have normal scopes (ApplicationScoped, SessionScoped, etc.)
            // They should only use @Dependent scope
            if (interceptorOrDecorator) {
                List<String> foundInvalidScopes = validateInterceptorDecoratorScopes(type, typeAnnotations);
                if (!foundInvalidScopes.isEmpty()) {
                    diagnostics.add(createDiagnostic(type, unit,
                            Messages.getMessage("InterceptorOrDecoratorWithIllegalScope"),
                            DIAGNOSTIC_CODE_INTERCEPTOR_DECORATOR_ILLEGAL_SCOPE,
                            new Gson().toJsonTree(foundInvalidScopes),
                            DiagnosticSeverity.Error));
                }
            }
            if (isManagedBean) {
                /*
                 * ========= Produces and Disposes, Observes, ObservesAsync Annotations Checks=========
                 */
                /*
                 * go through each method to make sure @Produces
                 * and @Disposes, @Observes, @ObservesAsync are not used together
                 *
                 * see: https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0.html#declaring_producer_method
                 *
                 * note:
                 * we need to check for bean defining annotations first to make sure the managed bean is discovered.
                 *
                 */
                invalidParamsCheck(unit, diagnostics, type, PRODUCES_FQ_NAME,
                        ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_PRODUCES_PARAM);

                for (PsiMethod method : methods) {
                    int numDisposes = 0;
                    Set<String> invalidAnnotations = new TreeSet<>();
                    PsiParameter[] params = method.getParameterList().getParameters();

                    for (PsiParameter param : params) {
                        PsiAnnotation[] annotations = param.getAnnotations();
                        for (PsiAnnotation annotation : annotations) {
                            String matchedAnnotation = getMatchedJavaElementName(type, annotation.getQualifiedName(),
                                    INVALID_INJECT_PARAMS_FQ);
                            if (DISPOSES_FQ_NAME.equals(matchedAnnotation)) {
                                numDisposes++;
                            } else if (OBSERVES_FQ_NAME.equals(matchedAnnotation)
                                    || OBSERVES_ASYNC_FQ_NAME.equals(matchedAnnotation)) {
                                invalidAnnotations.add("@" + annotation.getQualifiedName());
                            }
                        }
                    }

                    if(numDisposes == 0) continue;
                    if(numDisposes > 1) {
                        diagnostics.add(createDiagnostic(method, unit,
                                Messages.getMessage("ManagedBeanDisposeOneParameter"),
                                ManagedBeanConstants.DIAGNOSTIC_CODE_REDUNDANT_DISPOSES, null,
                                DiagnosticSeverity.Error));
                    }

                    if(!invalidAnnotations.isEmpty()) {
                        diagnostics.add(createDiagnostic(method, unit,
                                createInvalidDisposesLabel(invalidAnnotations),
                                ManagedBeanConstants.DIAGNOSTIC_CODE_INVALID_DISPOSES_PARAM, null,
                                DiagnosticSeverity.Error));
                    }
                }
            }
        }
    }

    /**
     * validateSingletonSessionBean
     * Singleton session bean scope validation
     * A singleton session bean must be annotated with either @ApplicationScoped or @Dependent.
     * If a singleton bean declares any other scope, the container must treat it as a definition error.
     *
     * @param unit
     * @param diagnostics
     * @param type
     * @param managedBeanAnnotations
     */
    private void validateSingletonSessionBean(PsiJavaFile unit, List<Diagnostic> diagnostics, PsiClass type, List<String> managedBeanAnnotations) {
        boolean isSingletonSessionBean = Stream.of(type.getAnnotations())
                .anyMatch(annotation -> isMatchedJavaElement(type, annotation.getQualifiedName(), SINGLETON_FQ_NAME));
        if (isSingletonSessionBean) {
            boolean hasInvalidSingletonScope = managedBeanAnnotations.stream()
                    .anyMatch(annotation -> !APPLICATION_SCOPED_FQ_NAME.equals(annotation)
                            && !DEPENDENT_FQ_NAME.equals(annotation));
            if (hasInvalidSingletonScope) {
                diagnostics.add(createDiagnostic(type, unit,
                        Messages.getMessage("SingletonSessionBeanInvalidScope"),
                        DIAGNOSTIC_CODE_INVALID_SINGLETON_SCOPE,
                        new Gson().toJsonTree(managedBeanAnnotations),
                        DiagnosticSeverity.Error));
            }
        }
    }

    private void invalidParamsCheck(PsiJavaFile unit, List<Diagnostic> diagnostics, PsiClass type, String target,
                                    String diagnosticCode) {
        Set<String> paramScopesSet;
        for (PsiMethod method : type.getMethods()) {
            boolean mutuallyExclusive = false;
            PsiAnnotation targetAnnotation = null;

            for (PsiAnnotation annotation : method.getAnnotations()) {
                if (isMatchedJavaElement(type, annotation.getQualifiedName(), target)) {
                    targetAnnotation = annotation;
                    break;
                }
            }

            if (targetAnnotation == null)
                continue;

            Set<String> invalidAnnotations = new TreeSet<>();
            PsiParameter[] params = method.getParameterList().getParameters();
            for (PsiParameter param : params) {
                List<String> paramScopes = getMatchedJavaElementNames(type, Stream.of(param.getAnnotations())
                                .map(annotation -> annotation.getQualifiedName()).toArray(String[]::new),
                        INVALID_INJECT_PARAMS_FQ);
                for (String annotation : paramScopes) {
                    invalidAnnotations.add("@" + getSimpleName(annotation));
                }
                paramScopesSet = new LinkedHashSet<>(paramScopes);
                if (paramScopesSet.size() == INVALID_INJECT_PARAMS_FQ.length && paramScopesSet.equals(Set.of(INVALID_INJECT_PARAMS_FQ))) {
                    mutuallyExclusive = true;
                }
            }

            if (!invalidAnnotations.isEmpty()) {
                if(PRODUCES_FQ_NAME.equals(target)) {
                    diagnostics.add(createDiagnostic(method, unit, createInvalidProducesLabel(invalidAnnotations),
                            diagnosticCode, null, DiagnosticSeverity.Error));
                } else {
                    if (mutuallyExclusive) {
                        diagnostics.add(createDiagnostic(method, unit, createInvalidInjectLabel(invalidAnnotations),
                                DIAGNOSTIC_INJECT_MULTIPLE_METHOD_PARAM, null, DiagnosticSeverity.Error));
                    } else {
                        diagnostics.add(createDiagnostic(method, unit, createInvalidInjectLabel(invalidAnnotations),
                                diagnosticCode, null, DiagnosticSeverity.Error));
                    }
                }
            }
        }
    }

    private String createInvalidInjectLabel(Set<String> invalidAnnotations) {
        return Messages.getMessage("ManagedBeanInvalidInject", String.join(", ", invalidAnnotations)); // assuming comma delimited list is ok
    }

    private String createInvalidProducesLabel(Set<String> invalidAnnotations) {
        return Messages.getMessage("ManagedBeanInvalidProduces", String.join(", ", invalidAnnotations)); // assuming comma delimited list is ok
    }

    private String createInvalidDisposesLabel(Set<String> invalidAnnotations) {
        return Messages.getMessage("ManagedBeanInvalidDisposer", String.join(", ", invalidAnnotations)); // assuming comma delimited list is ok
    }

    /**
     * validateNonStaticPublicField
     * This is to verify whether the @Dependent annotation must be the only scope applied to a managed bean
     * that contains a non-static public field.
     *
     * @param isManagedBean
     * @param isDependent
     * @param hasMultipleScopes
     * @param field
     * @return
     */
    private boolean validateNonStaticPublicField(boolean isManagedBean, boolean isDependent, boolean hasMultipleScopes,
                                                 PsiField field) {
        return isManagedBean && field.hasModifierProperty(PsiModifier.PUBLIC) && !field.hasModifierProperty(PsiModifier.STATIC)
                && (!isDependent || hasMultipleScopes);
    }

    /**
     * isConditionalObserver
     * Checks if the annotation is a conditional observer (notifyObserver=IF_EXISTS).
     *
     * @param type the type
     * @param annotation the annotation to check
     * @return true if the annotation is @Observes or @ObservesAsync with notifyObserver=IF_EXISTS
     */
    private boolean isConditionalObserver(PsiClass type, PsiAnnotation annotation) {
        String matched = getMatchedJavaElementName(type, annotation.getQualifiedName(),
                new String[] { OBSERVES_FQ_NAME, OBSERVES_ASYNC_FQ_NAME });
        if (matched != null) {
            String notifyObserverValue = AnnotationUtils.getAnnotationMemberValue(annotation, "notifyObserver");
            return notifyObserverValue != null && notifyObserverValue.endsWith("IF_EXISTS");
        }
        return false;
    }

    /**
     * hasConditionalObserverAnnotation
     * Checks if any parameter in the method has a conditional observer annotation.
     *
     * @param type the type
     * @param method the method to check
     * @return true if any parameter has a conditional observer annotation
     */
    private boolean hasConditionalObserverAnnotation(PsiClass type, PsiMethod method) {
        return Stream.of(method.getParameterList().getParameters())
                .flatMap(param -> Stream.of(param.getAnnotations()))
                .anyMatch(annotation -> isConditionalObserver(type, annotation));
    }

    /**
     * getDisposesParamNames
     * Returns a list of parameter names that are annotated with @Disposes.
     *
     * @param type   the type
     * @param method the method to check
     * @return list of parameter names annotated with @Disposes
     */
    private List<String> getDisposesParamNames(PsiClass type, PsiMethod method) {
        List<String> paramNames = new ArrayList<>();
        for (PsiParameter param : method.getParameterList().getParameters()) {
            for (PsiAnnotation annotation : param.getAnnotations()) {
                if (isMatchedJavaElement(type, annotation.getQualifiedName(), DISPOSES_FQ_NAME)) {
                    paramNames.add(param.getName());
                    break;
                }
            }
        }
        return paramNames;
    }

    /**
     * validateInterceptorDecoratorScopes
     * Validates that interceptors and decorators do not declare invalid scope
     * annotations.
     * Interceptors and decorators must not have normal scopes (ApplicationScoped,
     * SessionScoped, etc.)
     * and should only use @Dependent scope. Detects both built-in CDI scopes and
     * custom @NormalScope annotations.
     *
     * @param type            the Java type being validated
     * @param typeAnnotations the annotations on the type
     * @return list of invalid scope annotation fully qualified names
     */
    private List<String> validateInterceptorDecoratorScopes(PsiClass type, PsiAnnotation[] typeAnnotations) {
        List<String> foundInvalidScopes = new ArrayList<>();

        // Check each annotation to see if it's an invalid scope
        for (PsiAnnotation annotation : typeAnnotations) {
            String annotationName = annotation.getQualifiedName();

            // Check if it's a built-in invalid scope
            String matchedBuiltInScope = getMatchedJavaElementName(type, annotationName,
                    INVALID_INTERCEPTOR_DECORATOR_SCOPES);
            if (matchedBuiltInScope != null) {
                foundInvalidScopes.add(matchedBuiltInScope);
                // Skip @Interceptor, @Decorator, and @Dependent annotations - these are not
                // scopes we're checking
            } else if (null == getMatchedJavaElementName(type, annotationName,
                    new String[] {
                            INTERCEPTOR_FQ_NAME,
                            DECORATOR_FQ_NAME,
                            DEPENDENT_FQ_NAME
                    })) {
                // Check if it's a custom @NormalScope annotation using AnnotationUtil
                try {
                    PsiClass annotationType = JavaPsiFacade.getInstance(type.getProject())
                            .findClass(annotationName, type.getResolveScope());
                    if (annotationType != null && AnnotationUtil.isAnnotated(annotationType, NORMAL_SCOPE_FQ_NAME, 0)) {
                        foundInvalidScopes.add(annotationName);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Exception during annotation type resolution for: " + annotationName, e);
                }
            }
        }
        return foundInvalidScopes;
    }
}