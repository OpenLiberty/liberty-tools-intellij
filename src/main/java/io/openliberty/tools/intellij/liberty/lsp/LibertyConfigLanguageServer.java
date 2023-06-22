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
import io.openliberty.tools.intellij.lsp4mp.lsp4ij.server.ProcessStreamConnectionProvider;
import io.openliberty.tools.intellij.util.Constants;
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
public class LibertyConfigLanguageServer extends ProcessStreamConnectionProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(LibertyConfigLanguageServer.class);

    public LibertyConfigLanguageServer() {
        IdeaPluginDescriptor descriptor = PluginManagerCore.getPlugin(PluginId.getId("open-liberty.intellij"));
        File libertyServerPath = new File(descriptor.getPluginPath().toFile(), "lib/server/liberty-langserver-2.0-jar-with-dependencies.jar");
        String javaHome = System.getProperty("java.home");
        if (javaHome == null) {
            LOGGER.error("Unable to launch the Liberty language server. Could not resolve the java home system property");
            return;
        }
        if (!checkJavaVersion(javaHome, Constants.REQUIRED_JAVA_VERSION)) {
            LOGGER.error("Unable to launch the Liberty language server." +
                    " Java " + Constants.REQUIRED_JAVA_VERSION + " or more recent is required to run 'Liberty Tools for IntelliJ'." +
                    " Change the boot Java runtime of the IDE as documented here:" +
                    " https://www.jetbrains.com/help/idea/switching-boot-jdk.html");
            return;
        }
        if (libertyServerPath.exists()) {
            ArrayList<String> params = new ArrayList<>();
            params.add(javaHome + File.separator + "bin" + File.separator + "java");

            // Uncomment next line to attach debugger to LCLS at port 1064, debug params must come before -jar
            // params.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1064");
            params.add("-jar");
            params.add(libertyServerPath.getAbsolutePath());
            setCommands(params);
        } else {
            LOGGER.warn(String.format("Unable to start the Liberty language server, Liberty language server path: %s does not exist", libertyServerPath));
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
