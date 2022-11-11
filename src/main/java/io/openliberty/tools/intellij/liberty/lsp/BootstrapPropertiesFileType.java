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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * Custom file type for bootstrap.properties files
 */
public class BootstrapPropertiesFileType extends LanguageFileType implements FileTypeIdentifiableByVirtualFile {
    public static final BootstrapPropertiesFileType INSTANCE = new BootstrapPropertiesFileType();
    public static final String BOOTSTRAP_GLOB_PATTERN = "**/{src/main/liberty/config,usr/servers/**}/bootstrap.properties";

    private BootstrapPropertiesFileType() {
        super(BootstrapPropertiesLanguage.INSTANCE, true);
    }

    @Override
    public boolean isMyFileType(@NotNull VirtualFile file) {
        Path path = Paths.get(file.getCanonicalPath());
        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + BOOTSTRAP_GLOB_PATTERN);
        return matcher.matches(path);
    }

    @Override
    public @NotNull String getName() {
        return "bootstrap.properties file";
    }

    @Override
    public @NotNull @NlsContexts.Label String getDescription() {
        return "bootstrap.properties file";
    }

    @Override
    public @NotNull String getDefaultExtension() {
        return "properties";
    }

    @Override
    public @Nullable Icon getIcon() {
        return AllIcons.FileTypes.Properties;
    }
}
