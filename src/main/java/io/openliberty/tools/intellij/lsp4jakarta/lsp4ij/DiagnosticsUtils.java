/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation
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

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for common IntelliJ PSI-based diagnostic logic.
 */
public class DiagnosticsUtils {
    
    /**
     * inheritsFrom
     * find super class and Check
     *
     * @param clazz
     * @param fqSuperType
     * @return
     */
    public static boolean inheritsFrom(PsiClass clazz, String fqSuperType) {
        Project project = clazz.getProject();
        PsiClass superClass = JavaPsiFacade.getInstance(project)
                .findClass(fqSuperType, GlobalSearchScope.allScope(project));
        return inheritsFrom(clazz, superClass);
    }

    /**
     * inheritsFrom
     * Check if specified superClass is present or not in the type hierarchy
     *
     * @param clazz
     * @param superClass
     * @return
     */
    public static boolean inheritsFrom(PsiClass clazz, PsiClass superClass) {
        if (clazz == null || superClass == null) {
            return false;
        }
        return clazz.isEquivalentTo(superClass) || clazz.isInheritor(superClass, true);
    }

    /**
     * isPublic
     * Check if the given method is public or not
     *
     * @param method
     * @return
     */
    public static boolean isPublic(PsiMethod method) {
        return method.hasModifierProperty(PsiModifier.PUBLIC);
    }

    /**
     * hasField
     * Checks if the given type has a field matching the method name.
     *
     * @param methodName
     * @param type
     * @return
     */
    public static boolean hasField(String methodName, PsiClass type)  {
        if (methodName == null || methodName.length() <= 3) {
            return false;
        }
        String expectedFieldName = Introspector.decapitalize(methodName.substring(3));
        if (expectedFieldName.isEmpty()) {
            return false;
        }
        PsiField field = type.findFieldByName(expectedFieldName, false);
        return field!= null;
    }


    /**
     * validateSetterMethod
     * This is to check whether a method is a valid setter.
     *
     * @param method
     * @param parentType
     * @return
     */
    public static List<String> validateSetterMethod(PsiMethod method, PsiClass parentType) {
        List<String> errorCodes = new ArrayList<>();
        String methodName = method.getName();
        PsiType returnType = method.getReturnType();
        if (!methodName.startsWith("set")) {
            errorCodes.add(CommonConstants.DIAGNOSTIC_CODE_METHOD_NAME_START_WITH_SET);
        }
        if (!hasField(methodName, parentType)) {
            errorCodes.add(CommonConstants.DIAGNOSTIC_CODE_FIELD_MUST_EXIST_IN_SETTER);
        }
        if (!(returnType == null || returnType.equals(PsiTypes.voidType()))) {
            errorCodes.add(CommonConstants.DIAGNOSTIC_CODE_RETURN_TYPE_MUST_BE_VOID);
        }
        if (method.getParameterList().getParametersCount() != 1) {
            errorCodes.add(CommonConstants.DIAGNOSTIC_CODE_MUST_DECLARE_EXACTLY_ONE_PARAM);
        }
        if (!isPublic(method)) {
            errorCodes.add(CommonConstants.DIAGNOSTIC_CODE_METHOD_MUST_BE_PUBLIC);
        }
        return errorCodes;
    }

    /**
     * Check the given PsiClass is a Java Class
     * @param psiClass
     * @return Returns true if the given PsiClass is a Java Class
     */
    public static boolean isClass(PsiClass psiClass) {
        return psiClass != null &&
                !psiClass.isInterface() &&
                !psiClass.isEnum() &&
                !psiClass.isAnnotationType() &&
                !psiClass.isRecord();
    }

    /**
     * Returns true if the java element name matches the given fully qualified java
     * element name and false otherwise.
     *
     * @param type              Java class.
     * @param javaElementName   given object name.
     * @param javaElementFQName the fully qualified name.
     * @return true if the java element name matches the given fully qualified java
     *         element name and false otherwise.
     */
    public static boolean isMatchedJavaElement(PsiClass type, String javaElementName, String javaElementFQName) {
        if (javaElementFQName.equals(javaElementName)) {
            JavaPsiFacade facade = JavaPsiFacade.getInstance(type.getProject());
            Object o = facade.findClass(javaElementFQName, GlobalSearchScope.allScope(type.getProject()));
            return (o != null);
        }
        return false;
    }

    /**
     * getAnnotationMemberValue
     * Get the value of an annotation member attribute.
     *
     * @param annotation    the annotation
     * @param attributeName the attribute name
     * @param expectedType  the expected type of the attribute value
     * @param <T>           the type parameter
     * @return the attribute value, or null if not found or wrong type
     */
    public static <T> T getAnnotationMemberValue(PsiAnnotation annotation, String attributeName, Class<T> expectedType) {
        PsiAnnotationMemberValue value = annotation.findAttributeValue(attributeName);
        if (!(value instanceof PsiLiteral)) {
            return null;
        }

        Object constantValue = ((PsiLiteral) value).getValue();
        return expectedType.isInstance(constantValue) ? expectedType.cast(constantValue) : null;
    }
}
