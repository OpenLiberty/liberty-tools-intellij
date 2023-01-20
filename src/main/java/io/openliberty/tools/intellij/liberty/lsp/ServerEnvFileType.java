/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.liberty.lsp;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

import javax.swing.Icon;

import com.intellij.openapi.fileTypes.PlainTextLanguage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;

import io.openliberty.tools.intellij.LibertyPluginIcons;

import static io.openliberty.tools.intellij.util.Constants.SERVER_ENV_GLOB_PATTERN;

/**
 * Custom file type for server.env files
 */
public class ServerEnvFileType extends LanguageFileType implements FileTypeIdentifiableByVirtualFile {

    public static final ServerEnvFileType INSTANCE = new ServerEnvFileType();


    private ServerEnvFileType() {
        super(PlainTextLanguage.INSTANCE);
    }

    @Override
    public boolean isMyFileType(@NotNull VirtualFile file) {
        Path path = Paths.get(file.getCanonicalPath());
        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + SERVER_ENV_GLOB_PATTERN);
        return matcher.matches(path);
    }

    @Override
    public @NotNull String getName() {
        return "server.env";
    }

    @Override
    public @NotNull @NlsContexts.Label String getDescription() {
        return "server.env file";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "env";
    }

    @Override
    public @Nullable Icon getIcon() {
        return LibertyPluginIcons.libertyIcon;
    }
}