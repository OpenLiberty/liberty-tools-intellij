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
import com.intellij.psi.util.PsiTreeUtil;

import java.util.List;

/**
 * Utility class for PSI element operations.
 */
public class PsiUtils {

    /**
     * Gets the binding (field, method, or class) from the given PSI element.
     * This method traverses up the PSI tree to find the nearest modifier list owner.
     *
     * @param node the PSI element
     * @return the modifier list owner (PsiVariable, PsiMethod, or PsiClass), or null if not found
     */
    public static PsiModifierListOwner getBinding(PsiElement node) {
        // Try to find a variable (field or local variable)
        PsiModifierListOwner binding = PsiTreeUtil.getParentOfType(node, PsiVariable.class);
        if (binding != null) {
            return binding;
        }
        
        // Try to find a method
        binding = PsiTreeUtil.getParentOfType(node, PsiMethod.class);
        if (binding != null) {
            return binding;
        }
        
        // Try to find a class
        return PsiTreeUtil.getParentOfType(node, PsiClass.class);
    }

    /**
     * Recursively collects all classes including nested (inner) classes.
     *
     * @param classes   array of top-level or inner classes to start from
     * @param allClasses list that accumulates every class encountered
     */
    public static void collectAllClasses(PsiClass[] classes, List<PsiClass> allClasses) {
        for (PsiClass clazz : classes) {
            allClasses.add(clazz);
            collectAllClasses(clazz.getInnerClasses(), allClasses);
        }
    }

    /**
     * Returns {@code true} if the given fully qualified class name is imported
     * (explicitly or via on-demand) in the given Java file.
     *
     * <p>This mirrors {@code DiagnosticUtils.isImportedJavaElement} from lsp4jakarta,
     * and is intended as the canonical import-check utility for LTI diagnostics.
     *
     * @param unit          the Java file to inspect
     * @param javaElementFQName the fully qualified name to look for
     * @return {@code true} if the name is imported; {@code false} otherwise
     */
    public static boolean isImportedJavaElement(PsiJavaFile unit, String javaElementFQName) {
        PsiImportList importList = unit.getImportList();
        if (importList == null) {
            return false;
        }
        String packageName = javaElementFQName.substring(0, javaElementFQName.lastIndexOf('.'));
        for (PsiImportStatement stmt : importList.getImportStatements()) {
            String name = stmt.getQualifiedName();
            if (name == null) continue;
            if (javaElementFQName.equals(name)) return true;
            if (stmt.isOnDemand() && packageName.equals(name)) return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if the given {@link PsiType} is the raw (unparameterized)
     * {@code Event} type from {@code jakarta.enterprise.event}.
     *
     * <p>A raw {@code Event} has no type arguments. Parameterized forms such as
     * {@code Event<String>} are valid and return {@code false}.
     *
     * <p>Callers should guard by checking that {@code jakarta.enterprise.event.Event}
     * is imported in the file before calling this method, to avoid false positives
     * from user-defined classes named {@code Event}.
     *
     * @param type the PSI type to check
     * @return {@code true} if the type is a raw {@code Event}; {@code false} otherwise
     */
    public static boolean isRawEventType(PsiType type) {
        if (!(type instanceof PsiClassType)) {
            return false;
        }
        PsiClassType classType = (PsiClassType) type;
        // Raw type has no type arguments
        if (classType.getParameterCount() != 0) {
            return false;
        }
        // Import confirmed by caller — check simple name only
        return "Event".equals(classType.getClassName());
    }
}

