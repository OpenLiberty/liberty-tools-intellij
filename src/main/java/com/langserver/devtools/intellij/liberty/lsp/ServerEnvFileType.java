package com.langserver.devtools.intellij.liberty.lsp;

import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.util.NlsContexts;
import io.openliberty.tools.intellij.LibertyPluginIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Custom file type for server.env files
 */
public class ServerEnvFileType extends LanguageFileType {
    public static final ServerEnvFileType INSTANCE = new ServerEnvFileType();

    private ServerEnvFileType() {
        super(ServerEnvLanguage.INSTANCE);
    }

    @Override
    public @NotNull String getName() {
        return "server.env file";
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
