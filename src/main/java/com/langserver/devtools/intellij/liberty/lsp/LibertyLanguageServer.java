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
package com.langserver.devtools.intellij.liberty.lsp;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.extensions.PluginId;
import com.langserver.devtools.intellij.lsp4mp.lsp4ij.server.ProcessStreamConnectionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Start Liberty Language Server
 * Adapted from https://github.com/redhat-developer/intellij-quarkus/blob/2585eb422beeb69631076d2c39196d6eca2f5f2e/src/main/java/com/redhat/devtools/intellij/quarkus/lsp/QuarkusServer.java
 */
public class LibertyLanguageServer extends ProcessStreamConnectionProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibertyXmlServer.class);

    public LibertyLanguageServer() {
        IdeaPluginDescriptor descriptor = PluginManager.getPlugin(PluginId.getId("open-liberty.intellij"));
        File libertyServerPath = new File(descriptor.getPath(), "lib/server/liberty-langserver-1.0-SNAPSHOT-jar-with-dependencies.jar");
        String javaHome = System.getProperty("java.home");
        if (libertyServerPath.exists()) {
            setCommands(Arrays.asList(javaHome + File.separator + "bin" + File.separator + "java", "-jar",
                    libertyServerPath.getAbsolutePath(), "-DrunAsync=true"));
        } else {
            LOGGER.warn("Unable to start the Liberty language server, liberty language server path does not exist");
        }

    }

    @Override
    public Object getInitializationOptions(URI rootUri) {
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> extendedClientCapabilities = new HashMap<>();
        extendedClientCapabilities.put("completion", new HashMap<>());
        extendedClientCapabilities.put("shouldLanguageServerExitOnShutdown", Boolean.TRUE);
        root.put("extendedClientCapabilities", extendedClientCapabilities);
        return root;
    }
}
