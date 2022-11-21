/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package io.openliberty.tools.intellij.lsp4mp.lsp;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import io.openliberty.tools.intellij.liberty.lsp.LibertyXmlServer;
import io.openliberty.tools.intellij.lsp4mp.lsp4ij.server.ProcessStreamConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Adapted from https://github.com/redhat-developer/intellij-quarkus/blob/2585eb422beeb69631076d2c39196d6eca2f5f2e/src/main/java/com/redhat/devtools/intellij/quarkus/lsp/QuarkusServer.java
 * to start LSP4MP, Language Server for MicroProfile
 */
public class MicroProfileServer extends ProcessStreamConnectionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicroProfileServer.class);

    public MicroProfileServer() {
        IdeaPluginDescriptor descriptor = PluginManagerCore.getPlugin(PluginId.getId("open-liberty.intellij"));
        File lsp4mpServerPath = new File(descriptor.getPluginPath().toFile(), "lib/server/org.eclipse.lsp4mp.ls-uber.jar");
        String javaHome = System.getProperty("java.home");
        if (javaHome == null) {
            LOGGER.error("Unable to launch Eclipse LSP4MP language server. Could not resolve the java home system property");
            return;
        }
        if (lsp4mpServerPath.exists()) {
            setCommands(Arrays.asList(javaHome + File.separator + "bin" + File.separator + "java", "-jar",
                    lsp4mpServerPath.getAbsolutePath(), "-DrunAsync=true"));
        } else {
            LOGGER.warn(String.format("Unable to start Eclipse LSP4MP. Eclipse LSP4MP server path: %s does not exist"), lsp4mpServerPath);
        }
    }

    @Override
    public Object getInitializationOptions(URI rootUri) {
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> microprofile = new HashMap<>();
        Map<String, Object> tools = new HashMap<>();
        Map<String, Object> trace = new HashMap<>();
        trace.put("server", "verbose");
        tools.put("trace", trace);
        Map<String, Object> codeLens = new HashMap<>();
        codeLens.put("urlCodeLensEnabled", "true");
        tools.put("codeLens", codeLens);
        microprofile.put("tools", tools);
        settings.put("microprofile", microprofile);
        root.put("settings", settings);
        Map<String, Object> extendedClientCapabilities = new HashMap<>();
        Map<String, Object> commands = new HashMap<>();
        Map<String, Object> commandsKind = new HashMap<>();
        commandsKind.put("valueSet", Arrays.asList("microprofile.command.configuration.update", "microprofile.command.open.uri"));
        commands.put("commandsKind", commandsKind);
        extendedClientCapabilities.put("commands", commands);
        extendedClientCapabilities.put("completion", new HashMap<>());
        extendedClientCapabilities.put("shouldLanguageServerExitOnShutdown", Boolean.TRUE);
        root.put("extendedClientCapabilities", extendedClientCapabilities);
        return root;
    }
}
