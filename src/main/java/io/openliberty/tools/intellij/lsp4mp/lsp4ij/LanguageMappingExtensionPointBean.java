package io.openliberty.tools.intellij.lsp4mp.lsp4ij;

import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.xmlb.annotations.Attribute;

public class LanguageMappingExtensionPointBean extends AbstractExtensionPointBean {
    public static final ExtensionPointName<LanguageMappingExtensionPointBean> EP_NAME = ExtensionPointName.create("open-liberty.intellij.languageMapping");

    @Attribute("id")
    public String id;

    @Attribute("language")
    public String language;

    @Attribute("serverId")
    public String serverId;

    /**
     * Optional list of file patterns to narrow down the scope of the language server.
     */
    @Attribute("filePattern")
    public String filePattern;
}
