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
package com.redhat.devtools.intellij.liberty.lsp;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
//import com.redhat.devtools.intellij.quarkus.TelemetryService;
import com.redhat.devtools.intellij.quarkus.lsp4ij.server.ProcessStreamConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LibertyXmlServer extends ProcessStreamConnectionProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibertyXmlServer.class);

    public LibertyXmlServer() {
//        IdeaPluginDescriptor descriptor = PluginManager.getPlugin(PluginId.getId("com.redhat.devtools.intellij.quarkus"));
        IdeaPluginDescriptor descriptor = PluginManager.getPlugin(PluginId.getId("open-liberty.intellij"));
//        File lsp4mpServerPath = new File(descriptor.getPath(), "lib/server/org.eclipse.lsp4mp.ls-uber.jar");
        File lemminxServerPath = new File(descriptor.getPath(), "lib/server/org.eclipse.lemminx-uber.jar");
//        File lsp4mpServerPath = new File(descriptor.getPath(), "lib/org.eclipse.lsp4mp.ls-0.4.0.jar");

        File libertyServerPath = new File(descriptor.getPath(), "lib/server/liberty-langserver-lemminx-1.0-SNAPSHOT-jar-with-dependencies.jar");
        String javaHome = System.getProperty("java.home");
        LOGGER.warn("lemminxServerPath.exists(): " + lemminxServerPath.exists() + " ;" + lemminxServerPath.getAbsolutePath());
        LOGGER.warn("libertyServerPath.exists(): " + libertyServerPath.exists() + " ;" + libertyServerPath.getAbsolutePath());

        if (lemminxServerPath.exists() && libertyServerPath.exists()) {
            ArrayList<String> params = new ArrayList<>();
            params.add(javaHome + File.separator + "bin" + File.separator + "java");
//            params.add("-agentlib:jdwp=transport=dt_socket,server=y,address=1054"); // use for debugging Lemminx, will pause server until debugger attaches to port 1054
            params.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1054,quiet=y");
            params.add("-cp");
            params.add(lemminxServerPath.getAbsolutePath() + ":" + libertyServerPath.getAbsolutePath());
            params.add("org.eclipse.lemminx.XMLServerLauncher");
            setCommands(params);
            LOGGER.warn("Commands for starting LemMinX LS: " + params.toString());
        } else {
            LOGGER.warn("lemminxServerPath or libertyServerPath does not exist");
        }

//        setCommands(Arrays.asList(javaHome + File.separator + "bin" + File.separator + "java", "-jar",
//                lemminxServerPath.getAbsolutePath(), "-DrunAsync=true"));
//        setCommands(Arrays.asList(javaHome + File.separator + "bin" + File.separator + "java", "-jar",
//                lemminxServerPath.getAbsolutePath(), "-cp", libertyServerPath.getAbsolutePath(), "-DrunAsync=true"));
//        TelemetryService.instance().action(TelemetryService.LSP_PREFIX + "start").send();
    }

    @Override
    public Object getInitializationOptions(URI rootUri) {
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> settings = new HashMap<>();
//        Map<String, Object> quarkus = new HashMap<>();
        Map<String, Object> xml = new HashMap<>();
//        Map<String, Object> tools = new HashMap<>();
        Map<String, Object> trace = new HashMap<>();
        trace.put("server", "verbose");
//        tools.put("trace", trace);
        Map<String, Object> codeLens = new HashMap<>();
        codeLens.put("urlCodeLensEnabled", "true");
//        tools.put("codeLens", codeLens);
//        quarkus.put("tools", tools);
//        settings.put("microprofile", quarkus);
        xml.put("trace", trace);
        settings.put("xml", xml);
        root.put("settings", settings);
        Map<String, Object> extendedClientCapabilities = new HashMap<>();
//        Map<String, Object> commands = new HashMap<>();
        Map<String, Object> commandsKind = new HashMap<>();
//        commandsKind.put("valueSet", Arrays.asList("microprofile.command.configuration.update", "microprofile.command.open.uri"));
//        commands.put("commandsKind", commandsKind);
//        extendedClientCapabilities.put("commands", commands);
        extendedClientCapabilities.put("completion", new HashMap<>());
        extendedClientCapabilities.put("shouldLanguageServerExitOnShutdown", Boolean.TRUE);
        root.put("extendedClientCapabilities", extendedClientCapabilities);
        return root;
    }
}
