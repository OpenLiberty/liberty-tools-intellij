package com.langserver.devtools.intellij.lsp4mp.lsp;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Custom properties file type for microprofile-config.properties
 */
public class MicroProfileConfigFileType extends LanguageFileType {

    public static final MicroProfileConfigFileType INSTANCE = new MicroProfileConfigFileType();

    protected MicroProfileConfigFileType() {
        super(MicroProfileConfigLanguage.INSTANCE, true);
    }

    @Override
    public @NotNull String getName() {
        return "microprofile-config.properties file";
    }

    @Override
    public @NotNull @NlsContexts.Label String getDescription() {
        return "microprofile-config.properties file";
    }

    public @NotNull String getDefaultExtension() {
        return "properties";
    }

    @Override
    public @Nullable Icon getIcon() {
        return AllIcons.FileTypes.Properties;
    }
}
