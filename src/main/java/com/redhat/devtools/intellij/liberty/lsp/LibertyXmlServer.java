package com.redhat.devtools.intellij.liberty.lsp;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.redhat.devtools.intellij.quarkus.lsp4ij.server.ProcessStreamConnectionProvider;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Starts the LemMinX Language Server
 */
public class LibertyXmlServer extends ProcessStreamConnectionProvider {

    public LibertyXmlServer() {
        IdeaPluginDescriptor descriptor = PluginManager.getPlugin(PluginId.getId("open-liberty.intellij"));
        File lemminxServerPath = new File(descriptor.getPath(), "lib/server/org.eclipse.lemminx-uber.jar");
        String javaHome = System.getProperty("java.home");
        setCommands(Arrays.asList(javaHome + File.separator + "bin" + File.separator + "java", "-jar",
                lemminxServerPath.getAbsolutePath(), "-DrunAsync=true"));

    }
    @Override
    public Object getInitializationOptions(URI rootUri) {
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> quarkus = new HashMap<>();
        Map<String, Object> tools = new HashMap<>();
        Map<String, Object> trace = new HashMap<>();
        trace.put("server", "verbose");
        tools.put("trace", trace);
        Map<String, Object> codeLens = new HashMap<>();
        codeLens.put("urlCodeLensEnabled", "true");
        tools.put("codeLens", codeLens);
        quarkus.put("tools", tools);
//        settings.put("microprofile", quarkus);
        root.put("settings", settings);
        Map<String, Object> extendedClientCapabilities = new HashMap<>();
//        Map<String, Object> commands = new HashMap<>();
//        Map<String, Object> commandsKind = new HashMap<>();
//        commandsKind.put("valueSet", Arrays.asList("microprofile.command.configuration.update", "microprofile.command.open.uri"));
//        commands.put("commandsKind", commandsKind);
//        extendedClientCapabilities.put("commands", commands);
        extendedClientCapabilities.put("completion", new HashMap<>());
        extendedClientCapabilities.put("shouldLanguageServerExitOnShutdown", Boolean.TRUE);
        root.put("extendedClientCapabilities", extendedClientCapabilities);
        return root;
    }
}
