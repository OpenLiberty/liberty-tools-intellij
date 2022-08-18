/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package com.langserver.devtools.intellij.lsp4mp;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.langserver.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;

import java.util.HashSet;
import java.util.Set;

/**
 * Adapted from https://github.com/redhat-developer/intellij-quarkus/blob/2585eb422beeb69631076d2c39196d6eca2f5f2e/src/main/java/com/redhat/devtools/intellij/quarkus/QuarkusModuleUtil.java
 */
public class MicroProfileModuleUtil {
    public static Set<String> getModulesURIs(Project project) {
        Set<String> uris = new HashSet<>();
        for(Module module : ModuleManager.getInstance(project).getModules()) {
            uris.add(PsiUtilsLSImpl.getProjectURI(module));
        }
        return uris;
    }

}
