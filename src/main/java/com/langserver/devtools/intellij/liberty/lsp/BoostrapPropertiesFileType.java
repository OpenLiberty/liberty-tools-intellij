package com.langserver.devtools.intellij.liberty.lsp;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.NlsContexts;
import io.openliberty.tools.intellij.LibertyPluginIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Custom file type for bootstrap.properties files
 */
public class BoostrapPropertiesFileType extends LanguageFileType {
    public static final BoostrapPropertiesFileType INSTANCE = new BoostrapPropertiesFileType();

    private BoostrapPropertiesFileType() {
        super(BoostrapPropertiesLanguage.INSTANCE, true);
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
