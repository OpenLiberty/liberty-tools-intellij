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

/**
 * Utility class for handling annotation value expressions.
 * Provides methods to create and manipulate annotation attribute values.
 */
public class AnnotationValueExpressionUtil {

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
}

