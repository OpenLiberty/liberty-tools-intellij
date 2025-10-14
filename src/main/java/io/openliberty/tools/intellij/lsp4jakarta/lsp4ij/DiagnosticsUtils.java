/*******************************************************************************
 * Copyright (c) 2025 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * 	Contributors:
 * 		 IBM Inc. - initial API and implementation and/or initial documentation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;

/**
 * Utility class for common IntelliJ PSI-based diagnostic logic.
 */
public class DiagnosticsUtils {

    /**
     * inheritsFrom
     * Check if specified superType is present or not in the type hierarchy
     *
     * @param clazz
     * @param fqSuperType
     * @return
     */
    public static boolean inheritsFrom(PsiClass clazz, String fqSuperType) {
        Project project = clazz.getProject();
        PsiClass superClass = JavaPsiFacade.getInstance(project)
                .findClass(fqSuperType, GlobalSearchScope.allScope(project));
        return superClass != null &&
                (clazz.isEquivalentTo(superClass) || clazz.isInheritor(superClass, true));
    }
}
