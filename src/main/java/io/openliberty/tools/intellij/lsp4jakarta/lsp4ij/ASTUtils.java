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

import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
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
}
