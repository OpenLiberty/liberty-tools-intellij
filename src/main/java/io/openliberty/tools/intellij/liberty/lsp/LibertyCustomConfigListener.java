package io.openliberty.tools.intellij.liberty.lsp;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import io.openliberty.tools.intellij.lsp4mp.lsp4ij.LSPIJUtils;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LibertyCustomConfigListener implements BulkFileListener {
    private final static Logger LOGGER = Logger.getInstance(LibertyCustomConfigListener.class);

    private final LibertyCustomConfigManager manager;
    public static final String LIBERTY_PLUGIN_CONFIG_XML = "liberty-plugin-config.xml"; 

    public LibertyCustomConfigListener(LibertyCustomConfigManager manager) {
        this.manager = manager;
    }

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        // filter file events to only liberty-plugin-config.xml
        List<String> pluginConfigList = events.stream()
                .map(event -> LSPIJUtils.toUri(event.getFile()).toString())
                .filter(this::isPluginConfigXml)
                .toList();
        manager.handleProcessConfigXml(pluginConfigList);
    }

    private boolean isPluginConfigXml(String uri) {
        return uri.endsWith(LIBERTY_PLUGIN_CONFIG_XML);
    }
}
