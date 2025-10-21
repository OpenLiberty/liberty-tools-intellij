/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation.
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
import java.util.stream.Stream;

import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils.getSimpleName;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.*;
import static org.eclipse.lsp4jakarta.jdt.internal.cdi.Constants.INVALID_INITIALIZER_PARAMS_FQ;

public class ManagedBeanDiagnosticsCollector extends AbstractDiagnosticsCollector {

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
            List<String> managedBeanAnnotations = getMatchedJavaElementNames(type, Stream.of(type.getAnnotations())
                            .map(annotation -> annotation.getQualifiedName()).toArray(String[]::new),
                    scopeFQNames);
            boolean isManagedBean = managedBeanAnnotations.size() > 0;

            if (managedBeanAnnotations.size() > 1) {
                diagnostics.add(createDiagnostic(type, unit,
                        Messages.getMessage("ScopeTypeAnnotationsManagedBean"),
                        DIAGNOSTIC_CODE_SCOPEDECL, (JsonArray) (new Gson().toJsonTree(managedBeanAnnotations)),
                        DiagnosticSeverity.Error));
            }

            String[] injectAnnotations = { PRODUCES_FQ_NAME, INJECT_FQ_NAME };
            PsiField fields[] = type.getFields();
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
                if (isManagedBean
                        && field.hasModifierProperty(PsiModifier.PUBLIC)
                        && !field.hasModifierProperty(PsiModifier.STATIC)
                        && !managedBeanAnnotations.contains(DEPENDENT_FQ_NAME)) {
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
                boolean isClassGeneric = type.getTypeParameters().length != 0;
                boolean isDependent = managedBeanAnnotations.stream()
                            .anyMatch(annotation -> DEPENDENT_FQ_NAME.equals(annotation));

                if (isClassGeneric && !isDependent) {
                    diagnostics.add(createDiagnostic(type, unit, Messages.getMessage("ManagedBeanGenericType"),
                            DIAGNOSTIC_CODE, null, DiagnosticSeverity.Error));
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

    private void invalidParamsCheck(PsiJavaFile unit, List<Diagnostic> diagnostics, PsiClass type, String target,
                                    String diagnosticCode) {
        Set<String> paramScopesSet;
        boolean mutuallyExclusive = false;
        for (PsiMethod method : type.getMethods()) {
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
                if (paramScopesSet.size() == INVALID_INITIALIZER_PARAMS_FQ.length && paramScopesSet.equals(Set.of(INVALID_INITIALIZER_PARAMS_FQ))) {
                    mutuallyExclusive = true;
                    diagnosticCode = DIAGNOSTIC_INJECT_MULTIPLE_METHOD_PARAM;
                }
            }

            if (!invalidAnnotations.isEmpty()) {
                String label = PRODUCES_FQ_NAME.equals(target) ?
                        createInvalidProducesLabel(invalidAnnotations) :
                        createInvalidInjectLabel(invalidAnnotations, mutuallyExclusive);
                diagnostics.add(createDiagnostic(method, unit, label, diagnosticCode, null, DiagnosticSeverity.Error));
            }

        }
    }

    private String createInvalidInjectLabel(Set<String> invalidAnnotations, boolean mutuallyExclusive) {
        if (mutuallyExclusive) {
            return Messages.getMessage("ManagedBeanInvalidInject", String.join(", ", invalidAnnotations)); // assuming comma delimited list is ok
        } else {
            return Messages.getMessage("ManagedBeanInvalidInject", String.join(", ", invalidAnnotations)); // assuming comma delimited list is ok
        }
    }

    private String createInvalidProducesLabel(Set<String> invalidAnnotations) {
        return Messages.getMessage("ManagedBeanInvalidProduces", String.join(", ", invalidAnnotations)); // assuming comma delimited list is ok
    }

    private String createInvalidDisposesLabel(Set<String> invalidAnnotations) {
        return Messages.getMessage("ManagedBeanInvalidDisposer", String.join(", ", invalidAnnotations)); // assuming comma delimited list is ok
    }
}
