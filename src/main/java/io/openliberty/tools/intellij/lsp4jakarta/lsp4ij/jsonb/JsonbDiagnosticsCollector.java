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

import com.intellij.psi.*;
import com.intellij.psi.impl.PsiClassImplUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JsonPropertyUtils;
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
        if (unit == null)
            return;
        PsiClass[] types = unit.getClasses();
        PsiClass[] innerClasses;
        PsiMethod[] methods;
        PsiAnnotation[] allAnnotations;
        //No-Args for Parent and Child
        boolean parentHasValidNoArgsConstructor;
        boolean childHasValidNoArgsConstructor;
        boolean missingParentNoArgsConstructor;
        boolean missingChildNoArgsConstructor;
        boolean hasUserDefinedParentConstructor; //To check for existence of explicit constructors
        boolean hasUserDefinedChildConstructor; //To check for existence of explicit constructors
        for (PsiClass type : types) {
            parentHasValidNoArgsConstructor = false;
            hasUserDefinedParentConstructor = false;
            innerClasses = type.getInnerClasses();

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
                if (isConstructorMethod(method)) {
                    hasUserDefinedParentConstructor = true;
                    PsiParameterList params = method.getParameterList();
                    boolean isPubOrPro = method.hasModifierProperty(PsiModifier.PUBLIC) || method.hasModifierProperty(PsiModifier.PROTECTED);
                    if (params.getParametersCount() == 0 && isPubOrPro) {
                        parentHasValidNoArgsConstructor = true;
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
            //Checks for class level JSONB Annotations
            boolean jsonbtypeParent = isJsonbtypeParent(type);
            // Check if class has Jsonb field for closeable diagnostics
            boolean jsonbTypeClosable = hasJsonbField(type);
            for (PsiField field : type.getFields()) {
                //If class not annotated with JSONB, find if fields are.
                if (!jsonbtypeParent) {
                 jsonbtypeParent = isJsonbtypeParent(field);
                }

                collectJsonbTransientFieldDiagnostics(unit, type, diagnostics, field);
                collectJsonbTransientAccessorDiagnostics(unit, type, diagnostics, field);
                collectJsonbUniquePropertyNames(uniquePropertyNames, field);
            }
            // Collect diagnostics for Jsonb closeable thread safety issues
            // Only check classes that have Jsonb fields or Jsonb annotations
            if (jsonbTypeClosable || jsonbtypeParent) {
                collectClosableDiagnostics(unit, diagnostics);
            }
			for (PsiClass innerClass : innerClasses) {
				childHasValidNoArgsConstructor = false;
				hasUserDefinedChildConstructor = false;
				for (PsiMethod innerMethod : innerClass.getMethods()) {
					// Checks if parent class has public or protected no-args constructor
					if (isConstructorMethod(innerMethod)) {
						hasUserDefinedChildConstructor = true;
						PsiParameterList params = innerMethod.getParameterList();
						boolean isPubOrPro = innerMethod.hasModifierProperty(PsiModifier.PUBLIC) || innerMethod.hasModifierProperty(PsiModifier.PROTECTED);
						if (params.getParametersCount() == 0 && isPubOrPro) {
							childHasValidNoArgsConstructor = true;
						}
					}
				}
				// Child class conditions for no-args
				missingChildNoArgsConstructor = jsonbtypeParent && !childHasValidNoArgsConstructor
						&& hasUserDefinedChildConstructor;
				// Jsonb deseriazation diagnostics
				generateJsonbDeserializerDiagnostics(unit, diagnostics, jsonbtypeParent, true,
						false, missingChildNoArgsConstructor, innerClass);
			}
            // Collect diagnostics for duplicate property names with fields annotated @JsonbProperty
            collectJsonbPropertyUniquenessDiagnostics(unit, diagnostics, uniquePropertyNames, type);
			// Parent class conditions for no-args
			missingParentNoArgsConstructor = jsonbtypeParent && !parentHasValidNoArgsConstructor
					&& hasUserDefinedParentConstructor;
			// Jsonb deseriazation diagnostics
			generateJsonbDeserializerDiagnostics(unit, diagnostics, jsonbtypeParent, false,
					missingParentNoArgsConstructor, false, type);
        }
	}

    /**
     * @param element
     * @description This method checks if the Psi element passed has annotations of JSON Binding and declares it JSONB Type class or not.
     */
	private static boolean isJsonbtypeParent(PsiModifierListOwner element) {
		return Arrays.stream(element.getAnnotations())
				.map(PsiAnnotation::getQualifiedName)
				.filter(Objects::nonNull)
				.anyMatch(JsonbConstants.JSONB_ANNOTATIONS::contains);
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
                        JsonbConstants.DIAGNOSTIC_CODE_NON_STATIC_INNER_CLASS, null, DiagnosticSeverity.Warning));
            }
            // Check if static nested class is not public or protected (spec requires public or protected)
            if (type.hasModifierProperty(PsiModifier.STATIC) && jsonbtypeParent) {
                // Flag if not public and not protected (covers private and package-private/default)
                if (!type.hasModifierProperty(PsiModifier.PUBLIC) && !type.hasModifierProperty(PsiModifier.PROTECTED)) {
                    diagnostics.add(createDiagnostic(type, unit, Messages.getMessage("ErrorMessageJsonbNonPublicProtectedStaticNestedClass", type.getName()),
                            JsonbConstants.DIAGNOSTIC_CODE_NON_PUBLIC_PROTECTED_STATIC_NESTED_CLASS, null, DiagnosticSeverity.Error));
                }
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

    /**
     * Checks if a class has a field of type jakarta.json.bind.Jsonb.
     *
     * @param type the class to check
     * @return true if the class has a Jsonb field
     */
    private boolean hasJsonbField(PsiClass type) {
        for (PsiField field : type.getFields()) {
            PsiType fieldType = field.getType();
            if (fieldType instanceof PsiClassType) {
                PsiClass fieldClass = ((PsiClassType) fieldType).resolve();
                if (fieldClass != null) {
                    String qualifiedName = fieldClass.getQualifiedName();
                    if (JsonbConstants.JAKARTA_JSON_BIND_JSONB.equals(qualifiedName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Collects diagnostics for Jsonb closeable thread safety issues.
     * Detects methods that use thread sources without properly closing Jsonb instances.
     *
     * <p><b>Current Scope:</b> Method-level analysis only. Detects thread sources
     * and close() calls within the same method.
     *
     * <p><b>Known Limitations:</b>
     * <ul>
     * <li>Does not track Jsonb instances stored in fields</li>
     * <li>Does not perform inter-procedural analysis (close in different method)</li>
     * <li>Does not track Jsonb instances passed as parameters</li>
     * </ul>
     *
     * <p>These limitations are acceptable trade-offs for performance and complexity.
     * Most thread safety issues occur within a single method scope.
     *
     * @param unit the compilation unit
     * @param diagnostics the list to add diagnostics to
     */
    private void collectClosableDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        // Find all method call expressions in the file
        Collection<PsiMethodCallExpression> allMethodInvocations =
            com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(unit, PsiMethodCallExpression.class);
        
        Map<PsiMethod, JsonbThreadSafetyAnalysis> analysisMap = new HashMap<>();
        
        // Cache method resolutions to avoid repeated expensive lookups
        Map<PsiMethodCallExpression, PsiMethod> methodCache = new HashMap<>(allMethodInvocations.size());
        
        // Pre-resolve all methods in one pass for performance
        for (PsiMethodCallExpression mi : allMethodInvocations) {
            PsiMethod method = mi.resolveMethod();
            if (method != null) {
                methodCache.put(mi, method);
            }
        }
        
        // Analyze all method invocations and group by enclosing method
        for (PsiMethodCallExpression mi : allMethodInvocations) {
            PsiMethod enclosingMethod = com.intellij.psi.util.PsiTreeUtil.getParentOfType(mi, PsiMethod.class);
            if (enclosingMethod != null) {
                JsonbThreadSafetyAnalysis analysis = analysisMap.computeIfAbsent(
                    enclosingMethod, k -> new JsonbThreadSafetyAnalysis());
                
                PsiMethod resolvedMethod = methodCache.get(mi);
                if (resolvedMethod != null) {
                    PsiClass declaringClass = resolvedMethod.getContainingClass();
                    if (declaringClass != null) {
                        String fqName = declaringClass.getQualifiedName();
                        
                        // Check if this method uses Jsonb
                        if (!analysis.methodUsesJsonb && JsonbConstants.JAKARTA_JSON_BIND_JSONB.equals(fqName)) {
                            analysis.methodUsesJsonb = true;
                        }
                        
                        // Check if this is a close() invocation
                        if (!analysis.hasClose && isCloseInvocation(mi, resolvedMethod)) {
                            analysis.hasClose = true;
                        }
                        
                        // Check if this is a thread source invocation
                        if (isThreadSourceInvocation(mi, resolvedMethod)) {
                            analysis.threadSourceCount++;
                        }
                    }
                }
            }
        }
        
        // Generate diagnostics for methods that use Jsonb, have thread sources, but no close
        for (Map.Entry<PsiMethod, JsonbThreadSafetyAnalysis> entry : analysisMap.entrySet()) {
            PsiMethod method = entry.getKey();
            JsonbThreadSafetyAnalysis analysis = entry.getValue();
            if (analysis.methodUsesJsonb && !analysis.hasClose && analysis.threadSourceCount > 0) {
                diagnostics.add(createDiagnostic(
                    method.getNameIdentifier(),
                    unit,
                    Messages.getMessage("ErrorMessageJsonbCloseableThreadSafety", method.getName()),
                    JsonbConstants.DIAGNOSTIC_CODE_CLOSABLE_CLOSE,
                    null,
                    DiagnosticSeverity.Warning
                ));
            }
        }
    }

    /**
     * Checks if a method invocation is a close() call on Jsonb or related closeable types.
     *
     * @param mi the method invocation to check
     * @param resolvedMethod the pre-resolved method (for performance)
     * @return true if this is a close invocation on Jsonb, Closeable, or AutoCloseable
     */
    private boolean isCloseInvocation(PsiMethodCallExpression mi, PsiMethod resolvedMethod) {
        String name = mi.getMethodExpression().getReferenceName();
        if (!JsonbConstants.CLOSE_METHOD.equals(name)) {
            return false;
        }
        
        PsiClass declaringClass = resolvedMethod.getContainingClass();
        if (declaringClass == null) {
            return false;
        }
        
        String fqName = declaringClass.getQualifiedName();
        return JsonbConstants.JAKARTA_JSON_BIND_JSONB.equals(fqName) ||
               JsonbConstants.CLOSABLE_CLOSE.equals(fqName) ||
               JsonbConstants.AUTOCLOSABLE_CLOSE.equals(fqName);
    }

    /**
     * Checks if a method invocation is a thread source operation.
     * Detects both known thread classes and custom implementations through type hierarchy.
     *
     * @param mi the method invocation to check
     * @param resolvedMethod the pre-resolved method (for performance)
     * @return true if this invocation creates or uses a thread source
     */
    private boolean isThreadSourceInvocation(PsiMethodCallExpression mi, PsiMethod resolvedMethod) {
        PsiClass declaringClass = resolvedMethod.getContainingClass();
        if (declaringClass == null) {
            return false;
        }
        
        String name = mi.getMethodExpression().getReferenceName();
        String fqName = declaringClass.getQualifiedName();
        
        // Check known thread methods and classes
        if (isThreadSource(name, fqName)) {
            return true;
        }
        
        // Check if declaring class extends/implements thread-related types
        return isThreadRelatedType(declaringClass);
    }

    /**
     * Determines if a method name and fully qualified class name represent a thread source.
     * Uses constants from JsonbConstants for thread methods and classes.
     *
     * @param methodName the method name
     * @param fqName the fully qualified class name
     * @return true if this is a thread source method
     */
    private boolean isThreadSource(String methodName, String fqName) {
        if (methodName == null || fqName == null) {
            return false;
        }
        
        // Check if method name is in the list of thread methods from constants
        if (!JsonbConstants.THREAD_METHODS.contains(methodName)) {
            return false;
        }
        
        // Check if class is in the list of thread classes from constants
        // Use contains() to match class names that may be part of a longer qualified name
        return JsonbConstants.THREAD_CLASSES.stream().anyMatch(fqName::contains);
    }

    /**
     * Checks if a type is thread-related by examining its type hierarchy.
     * Detects custom implementations of ExecutorService, Runnable, Callable, TimerTask, etc.
     * Uses constants from JsonbConstants for thread hierarchy types.
     *
     * @param psiClass the class to check
     * @return true if the type extends/implements thread-related interfaces
     */
    private boolean isThreadRelatedType(PsiClass psiClass) {
        if (psiClass == null) {
            return false;
        }
        
        // Check if implements thread-related interfaces
        for (PsiClass iface : psiClass.getInterfaces()) {
            String ifaceName = iface.getQualifiedName();
            if (ifaceName != null && JsonbConstants.THREAD_HIERARCHY_TYPES.contains(ifaceName)) {
                return true;
            }
            // Recursively check interface hierarchy
            if (isThreadRelatedType(iface)) {
                return true;
            }
        }
        
        // Check if extends thread-related classes (Thread, TimerTask, etc.)
        PsiClass superClass = psiClass.getSuperClass();
        if (superClass != null) {
            String superName = superClass.getQualifiedName();
            if (superName != null && JsonbConstants.THREAD_HIERARCHY_TYPES.contains(superName)) {
                return true;
            }
            // Recursively check superclass hierarchy
            return isThreadRelatedType(superClass);
        }
        
        return false;
    }
}
