/*******************************************************************************
 * Copyright (c) 2020, 2024 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij;

import com.intellij.openapi.module.Module;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.IProjectLabelProvider;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.jaxrs.JaxRsConstants;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;

import java.util.Collections;
import java.util.List;

/**
 * Provides a Jakarta-specific label to a project if the project is a Jakarta
 * project.
 *
 * Based on:
 * https://github.com/eclipse/lsp4mp/blob/0.9.0/microprofile.jdt/org.eclipse.lsp4mp.jdt.core/src/main/java/org/eclipse/lsp4mp/jdt/internal/core/providers/MicroProfileProjectLabelProvider.java
 *
 * @author Angelo ZERR
 *
 */
public class JakartaProjectLabelProvider implements IProjectLabelProvider {

    /** Jakarta project label. */
    public static final String JAKARTA_LABEL = "jakarta";

    @Override
    public List<String> getProjectLabels(Module project) {
        if (isJakartaProject(project)) {
            return Collections.singletonList(JAKARTA_LABEL);
        }
        return Collections.emptyList();
    }

    /**
     * Returns true if <code>javaProject</code> is a Jakarta project. Returns
     * false otherwise.
     *
     * @param javaProject the Java project to check
     * @return true only if <code>javaProject</code> is a Jakarta project.
     */
    public static boolean isJakartaProject(Module javaProject) {
        return PsiUtilsLSImpl.getInstance(javaProject.getProject()).findClass(javaProject, JaxRsConstants.JAKARTA_WS_RS_GET_ANNOTATION) != null;
    }
}
