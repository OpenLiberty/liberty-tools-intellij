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
 *     IBM Corporation, Archana Iyer - initial API and implementation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.util;

import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jsonb.JsonbConstants;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jsonp.JsonpConstants;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for common PSI method call operations.
 */
public class PsiMethodCallUtils {

    /**
     * Checks if an expression is a null literal or a cast expression containing a null literal.
     * This is useful for detecting null arguments passed to methods that don't accept null parameters.
     *
     * @param arg the expression to check
     * @return true if the expression is null or a cast of null, false otherwise
     */
    public static boolean isInvalidNullArgument(PsiExpression arg) {
        return (arg instanceof PsiLiteralExpression lit && lit.getValue() == null)
                || (arg instanceof PsiTypeCastExpression cast
                && cast.getOperand() instanceof PsiLiteralExpression
                && ((PsiLiteralExpression) cast.getOperand()).getValue() == null);
    }

    /**
     * isMatchedMethodFQName
     * Method is used to identify passed method invocations
     *
     * @param mce
     * @param methodParentTypeFQ
     * @return boolean
     */
    public static boolean isMatchedMethodFQName(PsiMethodCallExpression mce, String methodParentTypeFQ) {
        PsiMethod method = mce.resolveMethod();
        String methodName = getMethodNameFromCallExpression(mce, method);
        String containingClassName = getContainingClassName(mce, method);
        if (methodName == null || containingClassName == null) {
            return false;
        }
        // Check if it's a createPointer method with specific argument count
        if (methodName.equals(JsonpConstants.CREATE_POINTER)) {
            return mce.getArgumentList().getExpressionCount() == JsonpConstants.EXPRESSION_COUNT_CREATE_POINTER
                    && methodParentTypeFQ.equals(containingClassName);
        }
        // Check if it's an add or fromJson method
        if (methodName.equals(JsonpConstants.JAKARTA_JSON_BUILDER_ADD_METHOD) ||
                methodName.equals(JsonbConstants.FROM_JSON_METHOD)) {
            return methodParentTypeFQ.equals(containingClassName);
        }
        return false;
    }
    
    /**
     * Get the method name from a method call expression.
     * If the method cannot be resolved, attempts to get the name from the method reference.
     *
     * @param mce the method call expression
     * @param method the resolved method (may be null)
     * @return the method name, or null if it cannot be determined
     */
    private static String getMethodNameFromCallExpression(PsiMethodCallExpression mce, PsiMethod method) {
        getMethodName(method);
        // Method couldn't be resolved, try to get name from the reference
        PsiReferenceExpression methodExpression = mce.getMethodExpression();
        return methodExpression.getReferenceName();
    }
    
    /**
     * Get the fully qualified name of the class containing the method.
     * If the method cannot be resolved, attempts to get it from the qualifier type.
     *
     * @param mce the method call expression
     * @param method the resolved method (may be null)
     * @return the fully qualified class name, or null if it cannot be determined
     */
    private static String getContainingClassName(PsiMethodCallExpression mce, PsiMethod method) {
        if (method != null && method.getContainingClass() != null) {
            return method.getContainingClass().getQualifiedName();
        }
        // Method couldn't be resolved, try to get the type from the qualifier
        PsiExpression qualifier = mce.getMethodExpression().getQualifierExpression();
        if (qualifier != null) {
            return qualifier.getType() != null? qualifier.getType().getCanonicalText(): null;
        }
        return null;
    }

    /**
     * Check if valid method exists
     *
     * @param method
     * @return
     */
    public static String getMethodName(PsiMethod method) {
        if(method != null){
            return method.getName();
        }
        return StringUtils.EMPTY;
    }
}
