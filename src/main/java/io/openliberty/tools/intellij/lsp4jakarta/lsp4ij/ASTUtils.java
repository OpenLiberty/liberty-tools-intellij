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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
     * Checks whether the given method contains a call to the specified method on the specified parent type,
     * and that call is NOT enclosed within a try statement (i.e., not inside a try/catch or try/finally block).
     *
     * <p>A method may contain multiple matching invocations (e.g. one inside a try and one
     * outside). This method returns {@code true} if <em>any</em> of the matching invocations
     * is outside a try statement, because each such call is unprotected.
     *
     * @param psiMethod the method to inspect
     * @param targetMethod the method name to look for (e.g. "proceed")
     * @param parentFQN the fully qualified name of the declaring class (e.g. "jakarta.interceptor.InvocationContext")
     * @return true if at least one matching invocation exists AND is not inside a try statement;
     *         false if there are no matching invocations or all of them are inside a try statement
     */
    public static boolean isMethodInvocationOutsideTryCatch(PsiMethod psiMethod, String targetMethod, String parentFQN) {
        Collection<PsiMethodCallExpression> matches = collectMethodInvocations(psiMethod, targetMethod, parentFQN, false);
        for (PsiMethodCallExpression match : matches) {
            PsiElement parent = match.getParent();
            boolean insideTry = false;
            while (parent != null && parent != psiMethod) {
                if (parent instanceof PsiTryStatement) {
                    insideTry = true;
                    break;
                }
                parent = parent.getParent();
            }
            if (!insideTry) {
                return true;
            }
        }
        return false;
    }

    /**
     * Walks the body of {@code psiMethod} collecting invocations whose name matches
     * {@code targetMethod} and whose declaring class matches {@code parentFQN}.
     *
     * @param psiMethod      the method to search
     * @param targetMethod   the method name to look for
     * @param parentFQN      the fully qualified name of the declaring class
     * @param stopAfterFirst if {@code true}, stops after the first match (early-exit)
     * @return matching invocations; at most one element when {@code stopAfterFirst} is {@code true}
     */
    private static List<PsiMethodCallExpression> collectMethodInvocations(PsiMethod psiMethod, String targetMethod,
                                                                           String parentFQN, boolean stopAfterFirst) {
        if (psiMethod == null) {
            return Collections.emptyList();
        }
        PsiCodeBlock body = psiMethod.getBody();
        if (body == null) {
            return Collections.emptyList();
        }
        Collection<PsiMethodCallExpression> allCalls = PsiTreeUtil.findChildrenOfType(body, PsiMethodCallExpression.class);
        List<PsiMethodCallExpression> results = new ArrayList<>();
        for (PsiMethodCallExpression call : allCalls) {
            PsiReferenceExpression methodExpr = call.getMethodExpression();
            String methodName = methodExpr.getReferenceName();
            if (targetMethod.equals(methodName)) {
                PsiMethod resolved = call.resolveMethod();
                if (resolved != null) {
                    PsiClass declaringClass = resolved.getContainingClass();
                    if (declaringClass != null && parentFQN.equals(declaringClass.getQualifiedName())) {
                        results.add(call);
                        if (stopAfterFirst) {
                            return results;
                        }
                    }
                }
            }
        }
        return results;
    }
}
