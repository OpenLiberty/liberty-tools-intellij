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
}

