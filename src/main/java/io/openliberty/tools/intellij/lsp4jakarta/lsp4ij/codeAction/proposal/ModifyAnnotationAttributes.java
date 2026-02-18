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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal;

import com.intellij.psi.*;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 Utility class for synthesizing default values for annotation attributes in IntelliJ PSI.
 *
 */
public class ModifyAnnotationAttributes  {

    /** Logger object to record events for this class. */
    private static final Logger LOGGER = Logger.getLogger(ModifyAnnotationAttributes.class.getName());

    /**
     * newDefaultExpression
     * Generates a default value for a given annotation attribute.
     *
     * @param annotation
     * @param attributeName
     * @return
     */
    public static PsiAnnotationMemberValue newDefaultExpression(PsiAnnotation annotation,
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

}
