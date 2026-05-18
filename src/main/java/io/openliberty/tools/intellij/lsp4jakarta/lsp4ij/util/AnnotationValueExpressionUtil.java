/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.util;

import com.intellij.psi.*;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for handling annotation value expressions.
 * Provides methods to create and manipulate annotation attribute values.
 */
public class AnnotationValueExpressionUtil {

    /** Logger object to record events for this class. */
    private static final Logger LOGGER = Logger.getLogger(AnnotationValueExpressionUtil.class.getName());

    /**
     * Creates an annotation attribute value expression for an enum constant.
     * 
     * @param annotation the annotation to create the value for
     * @param enumClassName the fully qualified enum class name (e.g., "jakarta.persistence.TemporalType")
     * @param enumConstantName the enum constant name (e.g., "DATE")
     * @return the created annotation member value
     */
    public static PsiAnnotationMemberValue createEnumValueExpression(PsiAnnotation annotation, 
                                                                      String enumClassName, 
                                                                      String enumConstantName) {
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(annotation.getProject());
        
        // Create the enum reference expression (e.g., "TemporalType.DATE")
        String simpleEnumName = getSimpleName(enumClassName);
        String enumReference = simpleEnumName + "." + enumConstantName;
        
        PsiAnnotationMemberValue value = factory.createExpressionFromText(enumReference, annotation);
        
        // Add import if needed
        addImportIfNeeded(annotation, enumClassName);
        
        return value;
    }

    /**
     * Creates an annotation attribute value expression for a string literal.
     * 
     * @param annotation the annotation to create the value for
     * @param stringValue the string value
     * @return the created annotation member value
     */
    public static PsiAnnotationMemberValue createStringValueExpression(PsiAnnotation annotation, 
                                                                        String stringValue) {
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(annotation.getProject());
        return factory.createExpressionFromText("\"" + stringValue + "\"", annotation);
    }

    /**
     * Extracts the simple name from a fully qualified class name.
     * 
     * @param fullyQualifiedName the fully qualified class name
     * @return the simple name
     */
    private static String getSimpleName(String fullyQualifiedName) {
        int lastDot = fullyQualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? fullyQualifiedName.substring(lastDot + 1) : fullyQualifiedName;
    }

    /**
     * Adds an import statement for the given class if it's not already imported.
     * 
     * @param annotation the annotation context
     * @param className the fully qualified class name to import
     */
    private static void addImportIfNeeded(PsiAnnotation annotation, String className) {
        PsiFile containingFile = annotation.getContainingFile();
        if (containingFile instanceof PsiJavaFile) {
            PsiJavaFile javaFile = (PsiJavaFile) containingFile;
            PsiImportList importList = javaFile.getImportList();
            
            if (importList != null) {
                // Check if import already exists
                boolean importExists = false;
                for (PsiImportStatement importStatement : importList.getImportStatements()) {
                    if (className.equals(importStatement.getQualifiedName())) {
                        importExists = true;
                        break;
                    }
                }
                
                // Add import if it doesn't exist
                if (!importExists) {
                    PsiElementFactory factory = JavaPsiFacade.getElementFactory(annotation.getProject());
                    PsiClass classToImport = JavaPsiFacade.getInstance(annotation.getProject())
                            .findClass(className, annotation.getResolveScope());
                    if (classToImport != null) {
                        PsiImportStatement importStatement = factory.createImportStatement(classToImport);
                        importList.add(importStatement);
                    }
                }
            }
        }
    }

    /**
     * Checks if an annotation member value is a reference to a specific enum constant.
     * 
     * @param value the annotation member value to check
     * @param expectedEnumReference the expected enum reference (e.g., "TemporalType.DATE")
     * @return true if the value matches the expected enum reference
     */
    public static boolean isEnumConstantReference(PsiAnnotationMemberValue value, String expectedEnumReference) {
        if (value instanceof PsiReferenceExpression) {
            PsiReferenceExpression ref = (PsiReferenceExpression) value;
            PsiElement resolved = ref.resolve();
            return resolved instanceof PsiEnumConstant && expectedEnumReference.equals(value.getText());
        }
        return false;
    }

    /**
     * createDefaultValueForType
     * Synthesizes a custom default expression for a given annotation attribute type.
     *
     * @param factory
     * @param context
     * @param type
     * @return
     */
    private static PsiAnnotationMemberValue createDefaultValueForType(PsiElementFactory factory,
                                                                      PsiAnnotation context,
                                                                      PsiType type) {
        String canonical = type.getCanonicalText();
        switch (canonical) {
            case "boolean": return factory.createExpressionFromText("false", context);
            case "byte":
            case "short":
            case "int":     return factory.createExpressionFromText("0", context);
            case "long":    return factory.createExpressionFromText("0L", context);
            case "float":   return factory.createExpressionFromText("0f", context);
            case "double":  return factory.createExpressionFromText("0d", context);
            case "char":    return factory.createExpressionFromText("'\\u0000'", context);
            case "java.lang.String": return factory.createExpressionFromText("\"\"", context);
            default:
                if (type instanceof PsiArrayType) {
                    return factory.createExpressionFromText("{}", context);
                }
                if (type instanceof PsiClassType) {
                    PsiClass resolved = ((PsiClassType) type).resolve();
                    if (resolved != null) {
                        if ("java.lang.Class".equals(resolved.getQualifiedName())) {
                            // Covers Class, Class<?>, Class<? extends T>
                            return factory.createExpressionFromText("Object.class", context);
                        }
                        if (resolved.isEnum()) {
                            for (PsiField f : resolved.getFields()) {
                                if (f instanceof PsiEnumConstant) {
                                    return factory.createExpressionFromText(resolved.getName() + "." + f.getName(), context);
                                }
                            }
                        }
                        if (resolved.isAnnotationType()) {
                            return factory.createAnnotationFromText("@" + resolved.getQualifiedName(), context);
                        }
                    }
                }
                logUnableToCreateDefaultValue();
                return factory.createExpressionFromText("null", context);
        }
    }

    /**
     * log if Unable To Create DefaultValue
     */
    private static void logUnableToCreateDefaultValue() {
        LOGGER.log(Level.WARNING, "Unable to create Default Attribute Value");
    }

    /**
     * createAnnotationAttributeDefault
     * Generates a default value for a given annotation attribute.
     *
     * @param annotation
     * @param attributeName
     * @return
     */
    public static PsiAnnotationMemberValue createAnnotationAttributeDefault(PsiAnnotation annotation,
                                                                            String attributeName) {
        PsiElementFactory factory = PsiElementFactory.getInstance(annotation.getProject());
        PsiJavaCodeReferenceElement ref = annotation.getNameReferenceElement();
        if (ref == null){
            logUnableToCreateDefaultValue();
            return factory.createExpressionFromText("null", annotation);
        }
        PsiElement resolved = ref.resolve();
        if (!(resolved instanceof PsiClass annotationClass)){
            logUnableToCreateDefaultValue();
            return factory.createExpressionFromText("null", annotation);
        }
        for (PsiMethod method : annotationClass.getMethods()) {
            if (method.getName().equals(attributeName) && method instanceof PsiAnnotationMethod annotationMethod) {
                // Use declared default if present
                PsiAnnotationMemberValue declaredDefault = annotationMethod.getDefaultValue();
                if (declaredDefault != null) {
                    return declaredDefault;
                }
                // Otherwise create a default value based on the annotation attribute type
                PsiType returnType = annotationMethod.getReturnType();
                return returnType != null
                        ? createDefaultValueForType(factory, annotation, returnType)
                        : factory.createExpressionFromText("null", annotation);
            }
        }
        logUnableToCreateDefaultValue();
        return factory.createExpressionFromText("null", annotation);
    }
}

