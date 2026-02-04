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

/**
 * Utility class for common IntelliJ PSI-based diagnostic logic.
 */
public class DiagnosticsUtils {

    public static final String NAME_MUST_START_WITH_SET = "NameMustStartWithSet";
    public static final String MUST_DECLARE_EXACTLY_ONE_PARAM = "MustDeclareExactlyOneParam";
    public static final String RETURN_TYPE_MUST_BE_VOID = "ReturnTypeMustBeVoid";
    public static final String VALID_SETTER_METHOD = "ValidSetterMethod";
    /**
     * inheritsFrom
     * find super class and Check if it is present or not in the type hierarchy
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
     * validateSetterMethod
     * This is to check whether a method is a valid setter.
     *
     * @param element
     * @return
     */
    public static String validateSetterMethod(PsiMethod element) {
        String methodName = element.getName();
        PsiType returnType = element.getReturnType();
        if(!methodName.startsWith("set")){
            return NAME_MUST_START_WITH_SET;
        } else if(!(returnType == null || returnType.equals(PsiTypes.voidType()))){
            return RETURN_TYPE_MUST_BE_VOID;
        } else if(element.getParameterList().getParametersCount() != 1){
            return MUST_DECLARE_EXACTLY_ONE_PARAM;
        }
        return VALID_SETTER_METHOD;
    }
}
