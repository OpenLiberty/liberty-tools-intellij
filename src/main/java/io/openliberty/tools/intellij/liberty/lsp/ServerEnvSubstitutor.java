/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 *  SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package io.openliberty.tools.intellij.liberty.lsp;

import com.intellij.lang.Language;
import com.intellij.lang.properties.PropertiesLanguage;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.LanguageSubstitutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Language Substitutor for Liberty server.env files
 * To re-use the IntelliJ parsing for Properties files on server.env files, categorize server.env files that are in a recognized
 * Liberty directory as Properties files. This enables language server capabilities (completion, hover, etc.) on server.env files.
 */
public class ServerEnvSubstitutor extends LanguageSubstitutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerEnvSubstitutor.class);

    @Override
    public @Nullable Language getLanguage(@NotNull VirtualFile file, @NotNull Project project) {
        if (isLibertyServerEnvFile(file)) {
            LOGGER.trace("Substituting Properties language for Liberty server.env file: " + file.getPath());
            // treat Liberty server.env files as Properties files
            return PropertiesLanguage.INSTANCE;
        }
        return PlainTextLanguage.INSTANCE;
    }

    private boolean isLibertyServerEnvFile(VirtualFile file) {
        return file.getPath().endsWith(".env");
    }
}
