/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package io.openliberty.tools.intellij.lsp4mp.lsp;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.lsp4ij.AbstractDocumentMatcher;
import com.redhat.devtools.lsp4ij.LSPIJUtils;

import java.util.regex.Pattern;

/**
 * Adapted from https://github.com/redhat-developer/intellij-quarkus/blob/main/src/main/java/com/redhat/devtools/intellij/quarkus/lsp/QuarkusDocumentMatcherForPropertiesFile.java
 */
public class MicroProfileMatcher extends AbstractDocumentMatcher {

    public static final Pattern MICROPROFILE_CONFIG_PROPERTIES = Pattern.compile("microprofile-config(-.+)?\\.properties");

    @Override
    public boolean match(VirtualFile file, Project fileProject) {
        if (!matchFile(file)) {
            return false;
        }
        return matchModule(file, fileProject);
    }

    public boolean matchModule(VirtualFile file, Project fileProject) {
        Module module = LSPIJUtils.getModule(file, fileProject);
        return module != null; // TODO: check for Liberty module
    }
    private boolean matchFile(VirtualFile file) {
        return MICROPROFILE_CONFIG_PROPERTIES.matcher(file.getName()).matches();
    }
}
