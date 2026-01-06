/*******************************************************************************
 * Copyright (c) 2020, 2026 IBM Corporation, Matheus Cruz and others.
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

import java.util.*;
import java.util.stream.Collectors;

import com.intellij.psi.*;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.util.InheritanceUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JsonPropertyUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.jetbrains.annotations.NotNull;

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
        if (unit == null)
            return;
        PsiClass[] types = unit.getClasses();
        PsiClass[] innerClasses;
        PsiMethod[] methods;
        PsiAnnotation[] allAnnotations;
        //Jsonb type for Parent
        boolean jsonbtypeParent;
        //No-Args for Parent and Child
        boolean parentHasNoArgsConstructor;
        boolean parentHasParameterizedConrtuctor;
        boolean childHasNoArgsConstructor;
        boolean childHasParameterizedConrtuctor;
        boolean missingParentNoArgs;
        boolean missingChildNoArgs;

        for (PsiClass type : types) {
            parentHasNoArgsConstructor = false;
            parentHasParameterizedConrtuctor = false;
            innerClasses = type.getInnerClasses();
            jsonbtypeParent = Arrays.stream(type.getAnnotations())
                        .map(PsiAnnotation::getQualifiedName)
                        .filter(Objects::nonNull)
                        .anyMatch(JsonbConstants.JSONB_ANNOTATIONS::contains);
            methods = type.getMethods();
            List<PsiMethod> jonbMethods = new ArrayList<PsiMethod>();
            // methods
            for (PsiMethod method : type.getMethods()) {
                if (isConstructorMethod(method) || method.hasModifierProperty(PsiModifier.STATIC)) {
                    allAnnotations = method.getAnnotations();
                    for (PsiAnnotation annotation : allAnnotations) {
                        if (isMatchedJavaElement(type, annotation.getQualifiedName(), JsonbConstants.JSONB_CREATOR))
                            jonbMethods.add(method);
                    }
                }
                //Checks if parent class has public or protected no-args constructor
                if(isConstructorMethod(method)){
                    PsiParameterList params = method.getParameterList();
                    boolean isPubOrPro = method.hasModifierProperty(PsiModifier.PUBLIC) || method.hasModifierProperty(PsiModifier.PROTECTED);
                    if(params.getParametersCount()==0 && isPubOrPro){
                        parentHasNoArgsConstructor = true;
                    }else{
                        parentHasParameterizedConrtuctor = true;
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
            //Changes to detect if Jsonb property names are not unique
            Set<String> uniquePropertyNames = new LinkedHashSet<String>();
            for (PsiField field : type.getFields()) {
                collectJsonbTransientFieldDiagnostics(unit, type, diagnostics, field);
                collectJsonbTransientAccessorDiagnostics(unit, type, diagnostics, field);
                collectJsonbUniquePropertyNames(uniquePropertyNames, field);
                //If class not annotated with JSONB, find if fields are.
                if(!jsonbtypeParent){
                    jsonbtypeParent = Arrays.stream(field.getAnnotations())
                            .map(PsiAnnotation::getQualifiedName)
                            .filter(Objects::nonNull)
                            .anyMatch(JsonbConstants.JSONB_ANNOTATIONS::contains);
                }
            }
            for(PsiClass innerClass: innerClasses){
                childHasNoArgsConstructor = false;
                childHasParameterizedConrtuctor = false;
                for (PsiMethod innerMethod : innerClass.getMethods()) {
                    //Checks if parent class has public or protected no-args constructor
                    if (isConstructorMethod(innerMethod)) {
                        PsiParameterList params = innerMethod.getParameterList();
                        boolean isPubOrPro = innerMethod.hasModifierProperty(PsiModifier.PUBLIC) || innerMethod.hasModifierProperty(PsiModifier.PROTECTED);
                        if (params.getParametersCount() == 0 && isPubOrPro) {
                            childHasNoArgsConstructor = true;
                        } else {
                            childHasParameterizedConrtuctor = true;
                        }
                    }
                }
                //Child class conditions for no-args
                missingChildNoArgs = jsonbtypeParent && !childHasNoArgsConstructor && childHasParameterizedConrtuctor;
                //Jsonb deseriazation diagnostics
                generateJsonbDeserializerDiagnostics(unit, diagnostics, jsonbtypeParent, true,
                        false, missingChildNoArgs, innerClass);
            }
            // Collect diagnostics for duplicate property names with fields annotated @JsonbProperty
            collectJsonbPropertyUniquenessDiagnostics(unit, diagnostics, uniquePropertyNames, type);
            //Parent class conditions for no-args
            missingParentNoArgs = jsonbtypeParent && !parentHasNoArgsConstructor && parentHasParameterizedConrtuctor;
            //Jsonb deseriazation diagnostics
            generateJsonbDeserializerDiagnostics(unit, diagnostics, jsonbtypeParent, false,
                    missingParentNoArgs, false, type);
        }
    }
    
    /**
     * @param unit
     * @param diagnostics
     * @param jsonbtypeParent
     * @param isInnerClass
     * @param missingParentNoArgs
     * @param missingChildNoArgs
     * @param type
     * @description This method generates diagnostics which deals with deserialization
     */
    private void generateJsonbDeserializerDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics, boolean jsonbtypeParent, boolean isInnerClass, boolean missingParentNoArgs,
                                                      boolean missingChildNoArgs, PsiClass type) {
        //Parent class diagnostics
        if (!isInnerClass) {
            if (missingParentNoArgs) {
                diagnostics.add(createDiagnostic(type, unit,  Messages.getMessage("ErrorMessageJsonbNoArgConstructorMissing", type.getName()),
                        JsonbConstants.DIAGNOSTIC_CODE_NO_ARGS_CONSTRUCTOR_MISSING, null, DiagnosticSeverity.Error));
            }
        } else {
            //Child class non-static and No-args diagnostics
            if (!type.hasModifierProperty(PsiModifier.STATIC) && jsonbtypeParent) {
                diagnostics.add(createDiagnostic(type, unit,  Messages.getMessage("ErrorMessageJsonbInnerNonStatic", type.getName()),
                        JsonbConstants.DIAGNOSTIC_CODE_NON_STATIC_INNER_CLASS, null, DiagnosticSeverity.Error));
            }
            if (type.hasModifierProperty(PsiModifier.STATIC) && missingChildNoArgs)
                diagnostics.add(createDiagnostic(type, unit,  Messages.getMessage("ErrorMessageJsonbNoArgConstructorMissing", type.getName()),
                        JsonbConstants.DIAGNOSTIC_CODE_NO_ARGS_CONSTRUCTOR_MISSING, null, DiagnosticSeverity.Error));
        }
    }


    /**
     * @param uniquePropertyNames
     * @param field
     * @description Method collects distinct property name values to be referenced for finding duplicates
     */
    private void collectJsonbUniquePropertyNames(Set<String> uniquePropertyNames, PsiField field) {
        for (PsiAnnotation annotation : field.getAnnotations()) {
            if (isMatchedAnnotation(annotation, JsonbConstants.JSONB_PROPERTY)) { // Checks whether annotation is JsonbProperty
                String propertyName = JsonPropertyUtils.extractPropertyNameFromJsonField(annotation);
                if (propertyName != null) {
                    uniquePropertyNames.add(JsonPropertyUtils.decodeUnicodeName(propertyName));
                }
            }
        }
    }


    /**
     * @param unit
     * @param diagnostics
     * @param uniquePropertyNames
     * @param type
     * @description Method to collect JsonbProperty uniqueness diagnostics
     */
    private void collectJsonbPropertyUniquenessDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics,
                                                           Set<String> uniquePropertyNames, PsiClass type) {
        Set<PsiClass> hierarchy = new LinkedHashSet<>(PsiClassImplUtil.getAllSuperClassesRecursively(type));
        Map<String, List<PsiField>> jsonbMap = buildPropertyMap(uniquePropertyNames, hierarchy);

        for (Map.Entry<String, List<PsiField>> entry : jsonbMap.entrySet()) { // Iterates through set of all key values pairs inside the map
            List<PsiField> fields = entry.getValue();
            if (fields.size() > JsonbConstants.MAX_PROPERTY_COUNT) {
                for (PsiField f : fields) {
                    if (f.getContainingClass().equals(type)) {// Creates diagnostics in the subclass
                        createJsonbPropertyUniquenessDiagnostics(unit, diagnostics, f, type);
                    }
                }
            }
        }
    }

    /**
     * @param unit
     * @param diagnostics
     * @param field
     * @param type
     * @description Method creates diagnostics with appropriate message and cursor context
     */
    private void createJsonbPropertyUniquenessDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics,
                                                          PsiField field, PsiClass type) {
        List<String> jsonbAnnotationsForField = getJsonbAnnotationNames(type, field);
        String diagnosticErrorMessage = Messages.getMessage("ErrorMessageJsonbPropertyUniquenessField");
        diagnostics.add(createDiagnostic(field, unit, diagnosticErrorMessage, JsonbConstants.DIAGNOSTIC_CODE_ANNOTATION_DUPLICATE_NAME,
                (JsonArray) (new Gson().toJsonTree(jsonbAnnotationsForField)), DiagnosticSeverity.Error));
    }

    /**
     * @param uniquePropertyNames
     * @param hierarchy
     * @return Map<String, List < IField>> jsonbMap
     * @description This method collects the property name and fields using the same name if it's duplicated and builds it into a Map.
     */
    private Map<String, List<PsiField>> buildPropertyMap(Set<String> uniquePropertyNames, Set<PsiClass> hierarchy) {
        Map<String, List<PsiField>> jsonbMap = new HashMap<>();
        hierarchy.stream()
                .flatMap(finalType -> Arrays.stream(finalType.getFields())) // flatten PsiFields
                .flatMap(field -> Arrays.stream(field.getAnnotations())
                        .filter(annotation -> isMatchedAnnotation(annotation, JsonbConstants.JSONB_PROPERTY))
                        .map(annotation -> Map.entry(annotation, field))) // pair annotation with its field
                .map(entry -> {
                    String propertyName = JsonPropertyUtils.extractPropertyNameFromJsonField(entry.getKey());
                    propertyName = propertyName != null ? JsonPropertyUtils.decodeUnicodeName(propertyName) : null;
                    return Map.entry(propertyName, entry.getValue());
                })
                .filter(entry -> entry.getKey() != null && uniquePropertyNames.contains(entry.getKey()))
                .forEach(entry ->
                        jsonbMap.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(entry.getValue())
                );
        return jsonbMap;
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
            if (hasAccessorConflict || hasJsonbAnnotationOtherThanTransient(jsonbAnnotationsForField))
                createJsonbTransientDiagnostic(unit, diagnostics, field, jsonbAnnotationsForField,
                        JsonbConstants.DIAGNOSTIC_CODE_ANNOTATION_TRANSIENT_FIELD);
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
                if (hasFieldConflict || hasJsonbAnnotationOtherThanTransient(jsonbAnnotationsForAccessor))
                    createJsonbTransientDiagnostic(unit, diagnostics, accessor, jsonbAnnotationsForAccessor,
                            JsonbConstants.DIAGNOSTIC_CODE_ANNOTATION_TRANSIENT_ACCESSOR);

            }
        }
        if (createDiagnosticForField)
            createJsonbTransientDiagnostic(unit, diagnostics, field, jsonbAnnotationsForField,
                    JsonbConstants.DIAGNOSTIC_CODE_ANNOTATION_TRANSIENT_ACCESSOR);
    }

    private boolean createJsonbTransientDiagnostic(PsiJavaFile unit, List<Diagnostic> diagnostics, PsiElement member,
                                                   List<String> jsonbAnnotations, String code) {
        String diagnosticErrorMessage = null;
        if (code.equals(JsonbConstants.DIAGNOSTIC_CODE_ANNOTATION_TRANSIENT_FIELD))
            diagnosticErrorMessage = Messages.getMessage("ErrorMessageJsonbTransientOnField");
        else if (code.equals(JsonbConstants.DIAGNOSTIC_CODE_ANNOTATION_TRANSIENT_ACCESSOR))
            diagnosticErrorMessage = Messages.getMessage("ErrorMessageJsonbTransientOnAccessor");

        diagnostics.add(createDiagnostic(member, unit, diagnosticErrorMessage, code,
                (JsonArray) (new Gson().toJsonTree(jsonbAnnotations)), DiagnosticSeverity.Error));
        return true;
    }

    private List<String> getJsonbAnnotationNames(PsiClass type, PsiJvmModifiersOwner annotable) {
        List<String> jsonbAnnotationNames = new ArrayList<String>();
        PsiAnnotation annotations[] = annotable.getAnnotations();
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
                    && !annotationName.equals(JsonbConstants.JSONB_TRANSIENT_FQ_NAME))
                return true;
        return false;
    }
}
