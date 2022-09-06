package com.langserver.devtools.intellij.lsp4mp.lsp;

import com.intellij.lang.Language;

/**
 * Custom language for microprofile-config.properties files
 */
public class MicroProfileConfigLanguage extends Language {

    public static final MicroProfileConfigLanguage INSTANCE = new MicroProfileConfigLanguage();

    private MicroProfileConfigLanguage() {
        super("MicroProfileConfigProperties", "text/properties");
    }
}
