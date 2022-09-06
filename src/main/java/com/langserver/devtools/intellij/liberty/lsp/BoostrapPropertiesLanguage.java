package com.langserver.devtools.intellij.liberty.lsp;

import com.intellij.lang.Language;

/**
 * Custom language for bootstrap.properties files
 */
public class BoostrapPropertiesLanguage extends Language {

    public static final BoostrapPropertiesLanguage INSTANCE = new BoostrapPropertiesLanguage();

    protected BoostrapPropertiesLanguage() {
        super("BootstrapProperties", "text/properties");
    }
}
