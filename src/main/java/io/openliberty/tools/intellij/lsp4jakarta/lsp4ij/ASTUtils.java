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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiTryStatement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Class ASTUtils - used for AST related util functionalities
 */
public class ASTUtils {
    /**
     * Method used to fetch all method declarations
     * @param unit
     * @return
     */
    public static @NotNull Collection<PsiMethod> getAllMethodDeclarations(PsiJavaFile unit) {
        Collection<PsiMethod> allMethodDeclarations =  PsiTreeUtil.findChildrenOfType(unit, PsiMethod.class);
        return allMethodDeclarations;
    }

    /**
     * Checks whether the given method contains a call to the specified method on the specified parent type.
     *
     * @param psiMethod the method to inspect
     * @param targetMethod the method name to look for (e.g. "proceed")
     * @param parentFQN the fully qualified name of the declaring class (e.g. "jakarta.interceptor.InvocationContext")
     * @return true if the matching method invocation exists; false otherwise
     */
    public static boolean containsMethodInvocation(PsiMethod psiMethod, String targetMethod, String parentFQN) {
        return findMethodInvocation(psiMethod, targetMethod, parentFQN) != null;
    }

    /**
     * Checks whether the given method contains a call to the specified method on the specified parent type,
     * and that call is enclosed within a try statement (i.e., inside a try/catch or try/finally block).
     *
     * @param psiMethod the method to inspect
     * @param targetMethod the method name to look for (e.g. "proceed")
     * @param parentFQN the fully qualified name of the declaring class (e.g. "jakarta.interceptor.InvocationContext")
     * @return true if the matching method invocation exists AND is inside a try statement; false otherwise
     */
    public static boolean isProceedWrappedInTryCatch(PsiMethod psiMethod, String targetMethod, String parentFQN) {
        PsiMethodCallExpression match = findMethodInvocation(psiMethod, targetMethod, parentFQN);
        if (match == null) {
            return false;
        }
        PsiElement parent = match.getParent();
        while (parent != null && parent != psiMethod) {
            if (parent instanceof PsiTryStatement) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    /**
     * Finds the first PsiMethodCallExpression within the given method declaration whose name
     * matches targetMethod and whose declaring class matches parentFQN.
     *
     * @param psiMethod the method to search
     * @param targetMethod the method name to look for
     * @param parentFQN the fully qualified name of the declaring class
     * @return the first matching PsiMethodCallExpression, or null
     */
    private static PsiMethodCallExpression findMethodInvocation(PsiMethod psiMethod, String targetMethod, String parentFQN) {
        if (psiMethod == null) {
            return null;
        }
        PsiCodeBlock body = psiMethod.getBody();
        if (body == null) {
            return null;
        }
        Collection<PsiMethodCallExpression> methodCalls = PsiTreeUtil.findChildrenOfType(body, PsiMethodCallExpression.class);
        for (PsiMethodCallExpression call : methodCalls) {
            PsiReferenceExpression methodExpr = call.getMethodExpression();
            String methodName = methodExpr.getReferenceName();
            if (targetMethod.equals(methodName)) {
                PsiMethod resolved = call.resolveMethod();
                if (resolved != null) {
                    PsiClass declaringClass = resolved.getContainingClass();
                    if (declaringClass != null && parentFQN.equals(declaringClass.getQualifiedName())) {
                        return call;
                    }
                }
            }
        }
        return null;
    }
}
