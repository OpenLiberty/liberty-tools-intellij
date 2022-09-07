/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.langserver.devtools.intellij.liberty.lsp;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Custom file type for bootstrap.properties files
 */
public class BootstrapPropertiesFileType extends LanguageFileType {
    public static final BootstrapPropertiesFileType INSTANCE = new BootstrapPropertiesFileType();

    private BootstrapPropertiesFileType() {
        super(BootstrapPropertiesLanguage.INSTANCE, true);
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
