/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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

import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.jsonrpc.messages.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

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
            String[] validAnnotations = { AnnotationConstants.GENERATED_FQ_NAME };
            String[] validTypeAnnotations = { AnnotationConstants.GENERATED_FQ_NAME,
                    AnnotationConstants.RESOURCE_FQ_NAME };
            String[] validMethodAnnotations = { AnnotationConstants.GENERATED_FQ_NAME,
                    AnnotationConstants.POST_CONSTRUCT_FQ_NAME, AnnotationConstants.PRE_DESTROY_FQ_NAME,
                    AnnotationConstants.RESOURCE_FQ_NAME };

            PsiPackage psiPackage = JavaPsiFacade.getInstance(unit.getProject())
                    .findPackage(unit.getPackageName());
            if (psiPackage != null) {
                PsiAnnotation[] pkgAnnotations = psiPackage.getAnnotations();
                for (PsiAnnotation annotation : pkgAnnotations) {
                    if (isValidAnnotation(annotation.getQualifiedName(), validAnnotations))
                        annotatables.add(new Tuple.Two<>(annotation, psiPackage));
                }
            }

            PsiClass[] types = unit.getClasses();
            for (PsiClass type : types) {
                // Type
                PsiAnnotation[] annotations = type.getAnnotations();
                for (PsiAnnotation annotation : annotations) {
                    if (isValidAnnotation(annotation.getQualifiedName(), validTypeAnnotations))
                        annotatables.add(new Tuple.Two<>(annotation, type));
                }
                // Method
                PsiMethod[] methods = type.getMethods();
                for (PsiMethod method : methods) {
                    annotations = method.getAnnotations();
                    for (PsiAnnotation annotation : annotations) {
                        if (isValidAnnotation(annotation.getQualifiedName(), validMethodAnnotations))
                            annotatables.add(new Tuple.Two<>(annotation, method));
                    }
                    // method parameters
                    PsiParameter[] parameters = method.getParameterList().getParameters();
                    for (PsiParameter parameter : parameters) {
                        annotations = parameter.getAnnotations();
                        for (PsiAnnotation annotation : annotations) {
                            if (isValidAnnotation(annotation.getQualifiedName(), validAnnotations))
                                annotatables.add(new Tuple.Two<>(annotation, parameter));
                        }
                    }
                }
                // Field
                PsiField[] fields = type.getFields();
                for (PsiField field : fields) {
                    annotations = field.getAnnotations();
                    for (PsiAnnotation annotation : annotations) {
                        if (isValidAnnotation(annotation.getQualifiedName(), validTypeAnnotations))
                            annotatables.add(new Tuple.Two<>(annotation, field));
                    }
                }
            }

            for (Tuple.Two<PsiAnnotation, PsiElement> annotatable : annotatables) {
                PsiAnnotation annotation = annotatable.getFirst();
                PsiElement element = annotatable.getSecond();
                PsiClass topLevel = PsiUtil.getTopLevelClass(element);

                if (isMatchedAnnotation(topLevel, annotation, AnnotationConstants.GENERATED_FQ_NAME)) {
                    for (PsiNameValuePair pair : annotation.getParameterList().getAttributes()) {
                        // If date element exists and is non-empty, it must follow ISO 8601 format.
                        if (pair.getAttributeName().equals("date")) {
                            String date = pair.getLiteralValue();
                            if (!date.equals("")) {
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
                } else if (isMatchedAnnotation(topLevel, annotation, AnnotationConstants.RESOURCE_FQ_NAME)) {
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
                if (isMatchedAnnotation(topLevel, annotation, AnnotationConstants.POST_CONSTRUCT_FQ_NAME)) {
                    if (element instanceof PsiMethod) {
                        PsiMethod method = (PsiMethod) element;
                        if (method.getParameters().length != 0) {
                            String diagnosticMessage = generateDiagnosticMethod("PostConstruct",
                                    "not have any parameters.");
                            diagnostics.add(createDiagnostic(method, unit, diagnosticMessage,
                                    AnnotationConstants.DIAGNOSTIC_CODE_POSTCONSTRUCT_PARAMS, null,
                                    DiagnosticSeverity.Error));
                        }

                        if (!method.getReturnType().equals(PsiType.VOID)) {
                            String diagnosticMessage = generateDiagnosticMethod("PostConstruct", "be void.");
                            diagnostics.add(createDiagnostic(method, unit, diagnosticMessage,
                                    AnnotationConstants.DIAGNOSTIC_CODE_POSTCONSTRUCT_RETURN_TYPE, null,
                                    DiagnosticSeverity.Error));
                        }

                        if (method.getThrowsTypes().length != 0) {
                            String diagnosticMessage = generateDiagnosticMethod("PostConstruct",
                                    "not throw checked exceptions.");
                            diagnostics.add(createDiagnostic(method, unit, diagnosticMessage,
                                    AnnotationConstants.DIAGNOSTIC_CODE_POSTCONSTRUCT_EXCEPTION, null,
                                    DiagnosticSeverity.Warning));
                        }
                    }
                } else if (isMatchedAnnotation(topLevel, annotation, AnnotationConstants.PRE_DESTROY_FQ_NAME)) {
                    if (element instanceof PsiMethod) {
                        PsiMethod method = (PsiMethod) element;
                        if (method.getParameters().length != 0) {
                            String diagnosticMessage = generateDiagnosticMethod("PreDestroy",
                                    "not have any parameters.");
                            diagnostics.add(createDiagnostic(method, unit, diagnosticMessage,
                                    AnnotationConstants.DIAGNOSTIC_CODE_PREDESTROY_PARAMS, null,
                                    DiagnosticSeverity.Error));
                        }

                        if (method.hasModifierProperty(PsiModifier.STATIC)) {
                            String diagnosticMessage = generateDiagnosticMethod("PreDestroy", "not be static.");
                            diagnostics.add(createDiagnostic(method, unit, diagnosticMessage,
                                    AnnotationConstants.DIAGNOSTIC_CODE_PREDESTROY_STATIC, method.getName(),
                                    DiagnosticSeverity.Error));
                        }

                        if (method.getThrowsTypes().length != 0) {
                            String diagnosticMessage = generateDiagnosticMethod("PreDestroy",
                                    "not throw checked exceptions.");
                            diagnostics.add(createDiagnostic(method, unit, diagnosticMessage,
                                    AnnotationConstants.DIAGNOSTIC_CODE_PREDESTROY_EXCEPTION, null,
                                    DiagnosticSeverity.Warning));
                        }
                    }
                }
            }
        }
    }

    private static String generateDiagnosticMethod(String annotation, String message) {
        String finalMessage = "A method with the annotation @" + annotation + " must " + message;
        return finalMessage;
    }

    private static boolean isValidAnnotation(String annotationName, String[] validAnnotations) {
        for (String fqName : validAnnotations) {
            if (fqName.endsWith(annotationName)) {
                return true;
            }
        }
        return false;
    }
}
