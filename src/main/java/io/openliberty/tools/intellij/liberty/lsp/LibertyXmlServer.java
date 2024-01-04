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
package io.openliberty.tools.intellij.liberty.lsp;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import io.openliberty.tools.intellij.util.JavaVersionUtil;
import io.openliberty.tools.intellij.util.Constants;
import com.redhat.devtools.lsp4ij.server.ProcessStreamConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Start LemMinX language server with Liberty LemMinX ext
 * Adapted from https://github.com/redhat-developer/intellij-quarkus/blob/2585eb422beeb69631076d2c39196d6eca2f5f2e/src/main/java/com/redhat/devtools/intellij/quarkus/lsp/QuarkusServer.java
 */
public class LibertyXmlServer extends ProcessStreamConnectionProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibertyXmlServer.class);

    public LibertyXmlServer() {
        String javaHome = System.getProperty("java.home");
        IdeaPluginDescriptor descriptor = PluginManagerCore.getPlugin(PluginId.getId("open-liberty.intellij"));
        File lemminxServerPath = new File(descriptor.getPluginPath().toFile(), "lib/server/org.eclipse.lemminx-uber.jar");
        File libertyServerPath = new File(descriptor.getPluginPath().toFile(), "lib/server/liberty-langserver-lemminx-jar-with-dependencies.jar");
        if(!JavaVersionUtil.isJavaHomeValid(javaHome, Constants.LIBERTY_XML_SERVER)){
            return;
        }
        if (lemminxServerPath.exists() && libertyServerPath.exists()) {
            ArrayList<String> params = new ArrayList<>();
            params.add(javaHome + File.separator + "bin" + File.separator + "java");
            // TODO enable debugging via IntelliJ configuration without manually changing this line of code
            // Comment out line 46 and replace with the one below for debugging LemMinX, will pause server until debugger attaches to port 1054
            // params.add("-agentlib:jdwp=transport=dt_socket,server=y,address=1054");
            // params.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1054,quiet=y");
            params.add("-cp");
            params.add(lemminxServerPath.getAbsolutePath() + File.pathSeparator + libertyServerPath.getAbsolutePath());
            params.add("org.eclipse.lemminx.XMLServerLauncher");
            setCommands(params);
        } else {
            LOGGER.warn(String.format("Unable to start the LemMinX language server. LemMinX server path: %s or Liberty LemMinX extension server path: %s does not exist"), lemminxServerPath, libertyServerPath);
        }
    }

    // JSON settings object needed by the language server.
    //        "settings": {
    //            "xml": {
    //                "trace": {
    //                    "server": "verbose"
    //                }
    //            },
    //        "extendedClientCapabilities": {
    //            "completion": {},
    //            "shouldLanguageServerExitOnShutdown": true
    //            }
    @Override
    public Object getInitializationOptions(URI rootUri) {
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> xml = new HashMap<>();
        Map<String, Object> trace = new HashMap<>();
        Map<String, Object> extendedClientCapabilities = new HashMap<>();

        trace.put("server", "verbose");
        xml.put("trace", trace); // TODO enable tracing so LemMinX stdout and stderr are redirected to IntelliJ log
        settings.put("xml", xml);
        root.put("settings", settings);
        // Additional configuration
        extendedClientCapabilities.put("completion", new HashMap<>());
        extendedClientCapabilities.put("shouldLanguageServerExitOnShutdown", Boolean.TRUE);
        root.put("extendedClientCapabilities", extendedClientCapabilities);

        return root;
    }
}
