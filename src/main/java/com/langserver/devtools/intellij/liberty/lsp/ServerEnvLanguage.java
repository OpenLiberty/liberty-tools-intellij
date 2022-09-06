package com.langserver.devtools.intellij.liberty.lsp;

import com.intellij.lang.Language;
import org.jetbrains.annotations.NotNull;

/**
 * Custom language for server.env files
 */
public class ServerEnvLanguage extends Language {

    public static final ServerEnvLanguage INSTANCE = new ServerEnvLanguage();

    protected ServerEnvLanguage() {
        super("ServerEnv");
    }
}
