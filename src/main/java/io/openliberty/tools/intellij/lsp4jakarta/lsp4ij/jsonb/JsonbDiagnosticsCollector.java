/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation, Matheus Cruz and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation, Matheus Cruz, Yijia Jing - initial API and implementation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jsonb;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

/**
 * This class contains logic for Jsonb diagnostics:
 * 1) Multiple JsonbCreator annotations on constructors will cause a diagnostic.
 * 2) JsonbTransient not being a mutually exclusive Jsonb annotation will cause a diagnostic.
 */
public class JsonbDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public JsonbDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return JsonbConstants.DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit == null) {
            return;
        }
        PsiClass[] types = unit.getClasses();
        PsiMethod[] methods;
        PsiAnnotation[] allAnnotations;

        for (PsiClass type : types) {
            methods = type.getMethods();
            List<PsiMethod> jonbMethods = new ArrayList<>();
            // methods
            for (PsiMethod method : type.getMethods()) {
                if (isConstructorMethod(method) || method.hasModifierProperty(PsiModifier.STATIC)) {
                    allAnnotations = method.getAnnotations();
                    for (PsiAnnotation annotation : allAnnotations) {
                        if (isMatchedJavaElement(type, annotation.getQualifiedName(), JsonbConstants.JSONB_CREATOR)) {
                            jonbMethods.add(method);
                        }
                    }
                }
            }
            if (jonbMethods.size() > JsonbConstants.MAX_METHOD_WITH_JSONBCREATOR) {
                for (PsiMethod method : methods) {
                    diagnostics.add(createDiagnostic(method, unit, Messages.getMessage("ErrorMessageJsonbCreator"),
                            JsonbConstants.DIAGNOSTIC_CODE_ANNOTATION, null, DiagnosticSeverity.Error));
                }
            }
            // fields
            for (PsiField field : type.getFields()) {
                collectJsonbTransientFieldDiagnostics(unit, type, diagnostics, field);
                collectJsonbTransientAccessorDiagnostics(unit, type, diagnostics, field);
            }
        }
    }

    private void collectJsonbTransientFieldDiagnostics(PsiJavaFile unit, PsiClass type, List<Diagnostic> diagnostics, PsiField field) {
        List<String> jsonbAnnotationsForField = getJsonbAnnotationNames(type, field);
        if (jsonbAnnotationsForField.contains(JsonbConstants.JSONB_TRANSIENT_FQ_NAME)) {
            boolean hasAccessorConflict = false;
            // Diagnostics on the accessors of the field are created when they are
            // annotated with Jsonb annotations other than JsonbTransient.
            List<PsiMethod> accessors = JDTUtils.getFieldAccessors(unit, field);
            for (PsiMethod accessor : accessors) {
                List<String> jsonbAnnotationsForAccessor = getJsonbAnnotationNames(type, accessor);
                if (hasJsonbAnnotationOtherThanTransient(jsonbAnnotationsForAccessor)) {
                    createJsonbTransientDiagnostic(unit, diagnostics, accessor, jsonbAnnotationsForAccessor,
                            JsonbConstants.DIAGNOSTIC_CODE_ANNOTATION_TRANSIENT_FIELD);
                    hasAccessorConflict = true;
                }
            }
            // Diagnostic is created on the field if @JsonbTransient is not mutually
            // exclusive or
            // accessor has annotations other than JsonbTransient
            if (hasAccessorConflict || hasJsonbAnnotationOtherThanTransient(jsonbAnnotationsForField)) {
                createJsonbTransientDiagnostic(unit, diagnostics, field, jsonbAnnotationsForField,
                        JsonbConstants.DIAGNOSTIC_CODE_ANNOTATION_TRANSIENT_FIELD);
            }
        }
    }

    private void collectJsonbTransientAccessorDiagnostics(PsiJavaFile unit, PsiClass type, List<Diagnostic> diagnostics, PsiField field) {
        boolean createDiagnosticForField = false;
        List<String> jsonbAnnotationsForField = getJsonbAnnotationNames(type, field);
        List<PsiMethod> accessors = JDTUtils.getFieldAccessors(unit, field);
        for (PsiMethod accessor : accessors) {
            List<String> jsonbAnnotationsForAccessor = getJsonbAnnotationNames(type, accessor);
            boolean hasFieldConflict = false;
            if (jsonbAnnotationsForAccessor.contains(JsonbConstants.JSONB_TRANSIENT_FQ_NAME)) {
                // Diagnostic is created if the field of this accessor has a annotation other
                // then JsonbTransient
                if (hasJsonbAnnotationOtherThanTransient(jsonbAnnotationsForField)) {
                    createDiagnosticForField = true;
                    hasFieldConflict = true;
                }

                // Diagnostic is created on the accessor if field has annotation other than
                // JsonbTransient
                // or if @JsonbTransient is not mutually exclusive
                if (hasFieldConflict || hasJsonbAnnotationOtherThanTransient(jsonbAnnotationsForAccessor)) {
                    createJsonbTransientDiagnostic(unit, diagnostics, accessor, jsonbAnnotationsForAccessor,
                            JsonbConstants.DIAGNOSTIC_CODE_ANNOTATION_TRANSIENT_ACCESSOR);
                }

            }
        }
        if (createDiagnosticForField) {
            createJsonbTransientDiagnostic(unit, diagnostics, field, jsonbAnnotationsForField,
                    JsonbConstants.DIAGNOSTIC_CODE_ANNOTATION_TRANSIENT_ACCESSOR);
        }
    }

    private boolean createJsonbTransientDiagnostic(PsiJavaFile unit, List<Diagnostic> diagnostics, PsiElement member,
                                                   List<String> jsonbAnnotations, String code) {
        String diagnosticErrorMessage = null;
        if (code.equals(JsonbConstants.DIAGNOSTIC_CODE_ANNOTATION_TRANSIENT_FIELD)) {
            diagnosticErrorMessage = Messages.getMessage("ErrorMessageJsonbTransientOnField");
        } else if (code.equals(JsonbConstants.DIAGNOSTIC_CODE_ANNOTATION_TRANSIENT_ACCESSOR)) {
            diagnosticErrorMessage = Messages.getMessage("ErrorMessageJsonbTransientOnAccessor");
        }
        // convert to simple name for current tests
        List<String> diagnosticData = jsonbAnnotations.stream().map(AbstractDiagnosticsCollector::getSimpleName)
                .collect(Collectors.toList());
        diagnostics.add(createDiagnostic(member, unit, diagnosticErrorMessage, code,
                (JsonArray) (new Gson().toJsonTree(diagnosticData)), DiagnosticSeverity.Error));
        return true;
    }

    private List<String> getJsonbAnnotationNames(PsiClass type, PsiJvmModifiersOwner annotable) {
        List<String> jsonbAnnotationNames = new ArrayList<>();
        PsiAnnotation[] annotations = annotable.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            String matchedAnnotation = getMatchedJavaElementName(type, annotation.getQualifiedName(), JsonbConstants.JSONB_ANNOTATIONS.toArray(String[]::new));
            if (matchedAnnotation != null) {
                jsonbAnnotationNames.add(matchedAnnotation);
            }
        }
        return jsonbAnnotationNames;
    }

    private boolean hasJsonbAnnotationOtherThanTransient(List<String> jsonbAnnotations) {
        for (String annotationName : jsonbAnnotations)
            if (JsonbConstants.JSONB_ANNOTATIONS.contains(annotationName)
                    && !annotationName.equals(JsonbConstants.JSONB_TRANSIENT_FQ_NAME)) {
                return true;
            }
        return false;
    }
}
