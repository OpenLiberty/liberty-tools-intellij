/*******************************************************************************
 * Copyright (c) 2021, 2026 IBM Corporation and others.
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

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.annotations;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.DiagnosticsUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.jsonrpc.messages.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.annotations.AnnotationConstants.EXCEPTION;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.annotations.AnnotationConstants.RUNTIME_EXCEPTION;

/**
 *
 * jararta.annotation Diagnostics
 *
 * <li>Diagnostic 1: @Generated 'date' attribute does not follow ISO 8601.</li>
 * <li>Diagnostic 2: @Resource 'name' attribute missing (when annotation is used
 * on a class).</li>
 * <li>Diagnostic 3: @Resource 'type' attribute missing (when annotation is used
 * on a class).</li>
 * <li>Diagnostic 4: @PostConstruct method has parameters.</li>
 * <li>Diagnostic 5: @PostConstruct method is not void.</li>
 * <li>Diagnostic 6: @PostConstruct method throws checked exception(s).</li>
 * <li>Diagnostic 7: @PreDestroy method has parameters.</li>
 * <li>Diagnostic 8: @PreDestroy method is static.</li>
 * <li>Diagnostic 9: @PreDestroy method throws checked exception(s).</li>
 *
 * @see <a href="https://jakarta.ee/specifications/annotations/2.0/annotations-spec-2.0.html#annotations">...</a>
 *
 */
public class AnnotationDiagnosticsCollector extends AbstractDiagnosticsCollector {

    private static final String[] VALID_ANNOTATIONS = { AnnotationConstants.GENERATED_FQ_NAME };
    private static final String[] VALID_TYPE_ANNOTATIONS = { AnnotationConstants.GENERATED_FQ_NAME,
            AnnotationConstants.RESOURCE_FQ_NAME };
    private static final String[] VALID_METHOD_ANNOTATIONS = { AnnotationConstants.GENERATED_FQ_NAME,
            AnnotationConstants.POST_CONSTRUCT_FQ_NAME, AnnotationConstants.PRE_DESTROY_FQ_NAME,
            AnnotationConstants.RESOURCE_FQ_NAME };

    public AnnotationDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return AnnotationConstants.DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit != null) {
            ArrayList<Tuple.Two<PsiAnnotation, PsiElement>> annotatables = new ArrayList<Tuple.Two<PsiAnnotation, PsiElement>>();

            PsiPackage psiPackage = JavaPsiFacade.getInstance(unit.getProject())
                    .findPackage(unit.getPackageName());
            if (psiPackage != null) {
                processAnnotations(psiPackage, annotatables, VALID_ANNOTATIONS);
            }

            PsiClass[] types = unit.getClasses();
            for (PsiClass type : types) {
                // Type
                processAnnotations(type, annotatables, VALID_TYPE_ANNOTATIONS);
                // Method
                PsiMethod[] methods = type.getMethods();
                for (PsiMethod method : methods) {
                    processAnnotations(method, annotatables, VALID_METHOD_ANNOTATIONS);
                    // method parameters
                    PsiParameter[] parameters = method.getParameterList().getParameters();
                    for (PsiParameter parameter : parameters) {
                        processAnnotations(parameter, annotatables, VALID_ANNOTATIONS);
                    }
                }
                // Field
                PsiField[] fields = type.getFields();
                for (PsiField field : fields) {
                    processAnnotations(field, annotatables, VALID_TYPE_ANNOTATIONS);
                }
            }

            for (Tuple.Two<PsiAnnotation, PsiElement> annotatable : annotatables) {
                PsiAnnotation annotation = annotatable.getFirst();
                PsiElement element = annotatable.getSecond();

                if (isMatchedAnnotation(annotation, AnnotationConstants.GENERATED_FQ_NAME)) {
                    for (PsiNameValuePair pair : annotation.getParameterList().getAttributes()) {
                        // If date element exists and is non-empty, it must follow ISO 8601 format.
                        if (pair.getAttributeName().equals("date")) {
                            String date = pair.getLiteralValue();
                            if (date != null && !date.equals("")) {
                                if (!Pattern.matches(AnnotationConstants.ISO_8601_REGEX, date)) {
                                    String diagnosticMessage = Messages.getMessage(
                                            "AnnotationMustDefineAttributeFollowing8601", "@Generated", "date");
                                    diagnostics.add(createDiagnostic(annotation, unit, diagnosticMessage,
                                            AnnotationConstants.DIAGNOSTIC_CODE_DATE_FORMAT, null,
                                            DiagnosticSeverity.Error));
                                }
                            }
                        }
                    }
                } else if (isMatchedAnnotation(annotation, AnnotationConstants.RESOURCE_FQ_NAME)) {
                    if (element instanceof PsiClass) {
                        PsiClass type = (PsiClass) element;
                        Boolean nameEmpty = true;
                        Boolean typeEmpty = true;
                        for (PsiNameValuePair pair : annotation.getParameterList().getAttributes()) {
                            if (pair.getAttributeName().equals("name")) {
                                nameEmpty = false;
                            }
                            if (pair.getAttributeName().equals("type")) {
                                typeEmpty = false;
                            }
                        }
                        String diagnosticMessage;
                        if (nameEmpty) {
                            diagnosticMessage = Messages.getMessage("AnnotationMustDefineAttribute",
                                    "@Resource", "name");
                            diagnostics.add(createDiagnostic(annotation, unit, diagnosticMessage,
                                    AnnotationConstants.DIAGNOSTIC_CODE_MISSING_RESOURCE_NAME_ATTRIBUTE, null,
                                    DiagnosticSeverity.Error));
                        }

                        if (typeEmpty) {
                            diagnosticMessage = Messages.getMessage("AnnotationMustDefineAttribute",
                                    "@Resource", "type");
                            diagnostics.add(createDiagnostic(annotation, unit, diagnosticMessage,
                                    AnnotationConstants.DIAGNOSTIC_CODE_MISSING_RESOURCE_TYPE_ATTRIBUTE, null,
                                    DiagnosticSeverity.Error));
                        }
                    }
                }
                if (isMatchedAnnotation(annotation, AnnotationConstants.POST_CONSTRUCT_FQ_NAME)) {
                    if (element instanceof PsiMethod) {
                        PsiMethod method = (PsiMethod) element;
                        List<String> checkedExceptions = getCheckedExceptionPresent(method);
                        if (!checkedExceptions.isEmpty()) {
                            String diagnosticMessage = Messages.getMessage("MethodMustNotThrow",
                                    "@PostConstruct");
                            diagnostics.add(createDiagnostic(method, unit, diagnosticMessage,
                                    AnnotationConstants.DIAGNOSTIC_CODE_POSTCONSTRUCT_EXCEPTION,
                                    (JsonArray) (new Gson().toJsonTree(checkedExceptions)),
                                    DiagnosticSeverity.Error));
                        }
                        if (method.getParameters().length != 0) {
                            String diagnosticMessage = Messages.getMessage("MethodMustNotHaveParameters",
                                    "@PostConstruct");
                            diagnostics.add(createDiagnostic(method, unit, diagnosticMessage,
                                    AnnotationConstants.DIAGNOSTIC_CODE_POSTCONSTRUCT_PARAMS, null,
                                    DiagnosticSeverity.Error));
                        }

                        if (!method.getReturnType().equals(PsiTypes.voidType())) {
                            String diagnosticMessage = Messages.getMessage("MethodMustBeVoid",
                                    "@PostConstruct");
                            diagnostics.add(createDiagnostic(method, unit, diagnosticMessage,
                                    AnnotationConstants.DIAGNOSTIC_CODE_POSTCONSTRUCT_RETURN_TYPE, null,
                                    DiagnosticSeverity.Error));
                        }
                    }
                } else if (isMatchedAnnotation(annotation, AnnotationConstants.PRE_DESTROY_FQ_NAME)) {
                    if (element instanceof PsiMethod) {
                        PsiMethod method = (PsiMethod) element;
                        List<String> checkedExceptions = getCheckedExceptionPresent(method);
                        if (!checkedExceptions.isEmpty()) {
                            String diagnosticMessage = Messages.getMessage("MethodMustNotThrow",
                                    "@PreDestroy");
                            diagnostics.add(createDiagnostic(method, unit, diagnosticMessage,
                                    AnnotationConstants.DIAGNOSTIC_CODE_PREDESTROY_EXCEPTION,
                                    (JsonArray) (new Gson().toJsonTree(checkedExceptions)),
                                    DiagnosticSeverity.Error));
                        }
                        if (method.getParameters().length != 0) {
                            String diagnosticMessage = Messages.getMessage("MethodMustNotHaveParameters",
                                    "@PreDestroy");
                            diagnostics.add(createDiagnostic(method, unit, diagnosticMessage,
                                    AnnotationConstants.DIAGNOSTIC_CODE_PREDESTROY_PARAMS, null,
                                    DiagnosticSeverity.Error));
                        }

                        if (method.hasModifierProperty(PsiModifier.STATIC)) {
                            String diagnosticMessage = Messages.getMessage("MethodMustNotBeStatic",
                                    "@PreDestroy");
                            diagnostics.add(createDiagnostic(method, unit, diagnosticMessage,
                                    AnnotationConstants.DIAGNOSTIC_CODE_PREDESTROY_STATIC, method.getName(),
                                    DiagnosticSeverity.Error));
                        }
                    }
                }
            }
        }
    }

    private void processAnnotations(PsiJvmModifiersOwner psiModifierOwner,
                                    ArrayList<Tuple.Two<PsiAnnotation, PsiElement>> annotatables,
                                    String[] validAnnotations) {
        PsiAnnotation[] annotations = psiModifierOwner.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            if (isValidAnnotation(annotation.getQualifiedName(), validAnnotations))
                annotatables.add(new Tuple.Two<>(annotation, psiModifierOwner));
        }
    }

    private static boolean isValidAnnotation(String annotationName, String[] validAnnotations) {
        if (annotationName != null) {
            for (String fqName : validAnnotations) {
                if (fqName.equals(annotationName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * getCheckedExceptionPresent
     * This method scans the exception signatures to identify if any checked exceptions are declared.
     *
     * @param method the PSI method
     * @return list of fully qualified names of checked exceptions
     */
    private List<String> getCheckedExceptionPresent(PsiMethod method) {
        List<String> checkedExceptions = new ArrayList<>();
        for (PsiClassType type : method.getThrowsList().getReferencedTypes()) {
            PsiClass exceptionClass = type.resolve();
            /*
             * A checked exception is any class that extends java.lang.Exception but not
             * java.lang.RuntimeException.
             * An unchecked exception is any class that extends java.lang.RuntimeException
             * or java.lang.Error.
             */
            if (exceptionClass != null && extendsException(exceptionClass) && notExtendsRuntimeException(exceptionClass)) {
                checkedExceptions.add(exceptionClass.getQualifiedName());
            }
        }
        return checkedExceptions;
    }

    /**
     * extendsException
     *
     * @param exceptionClass The root type of which the super-types are checked.
     * @return true if Exception is the superType of the given exception type.
     */
    private static boolean extendsException(PsiClass exceptionClass) {
        return DiagnosticsUtils.inheritsFrom(exceptionClass, EXCEPTION);
    }

    /**
     * notExtendsRuntimeException
     *
     * @param exceptionClass The root type of which the super-types are checked.
     * @return true if RuntimeException is not the superType of the given exception type.
     */
    private static boolean notExtendsRuntimeException(PsiClass exceptionClass) {
        return !DiagnosticsUtils.inheritsFrom(exceptionClass, RUNTIME_EXCEPTION);
    }



}
