/*******************************************************************************
 * Copyright (c) 2020, 2026 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package io.openliberty.tools.intellij.lsp4mp.lsp;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.application.PluginPathManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.redhat.devtools.lsp4ij.server.OSProcessStreamConnectionProvider;
import io.openliberty.tools.intellij.lsp4mp4ij.settings.UserDefinedMicroProfileSettings;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.JavaVersionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Adapted from https://github.com/redhat-developer/intellij-quarkus/blob/2585eb422beeb69631076d2c39196d6eca2f5f2e/src/main/java/com/redhat/devtools/intellij/quarkus/lsp/QuarkusServer.java
 * to start LSP4MP, Language Server for MicroProfile
 */
public class MicroProfileServer extends OSProcessStreamConnectionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicroProfileServer.class);

    private final Project project;

    public MicroProfileServer(Project project) {
        this.project = project;
        String javaHome = System.getProperty("java.home");
        File lsp4mpServerPath = Objects.requireNonNull(PluginPathManager.getPluginResource(getClass(), "lib/server/org.eclipse.lsp4mp.ls-uber.jar"));
        if(!JavaVersionUtil.isJavaHomeValid(javaHome, Constants.MICROPROFILE_SERVER)){
            return;
        }
        if (lsp4mpServerPath.exists()) {
            setCommandLine(new GeneralCommandLine(Arrays.asList(javaHome + File.separator + "bin" + File.separator + "java", "-jar",
                    lsp4mpServerPath.getAbsolutePath(), "-DrunAsync=true")));
        } else {
            LOGGER.warn(String.format("Unable to start Eclipse LSP4MP. Eclipse LSP4MP server path: %s does not exist"), lsp4mpServerPath);
        }
    }

    @Override
    public Object getInitializationOptions(VirtualFile rootUri) {
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> settings = UserDefinedMicroProfileSettings.getInstance(project).toSettingsForMicroProfileLS();
        root.put("settings", settings);
        Map<String, Object> extendedClientCapabilities = new HashMap<>();
        Map<String, Object> commands = new HashMap<>();
        Map<String, Object> commandsKind = new HashMap<>();
        commandsKind.put("valueSet", Arrays.asList(/*"microprofile.command.configuration.update",*/ "microprofile.command.open.uri"));
        commands.put("commandsKind", commandsKind);
        extendedClientCapabilities.put("commands", commands);
        extendedClientCapabilities.put("completion", new HashMap<>());
        extendedClientCapabilities.put("shouldLanguageServerExitOnShutdown", Boolean.TRUE);
        root.put("extendedClientCapabilities", extendedClientCapabilities);
        return root;
    }
}
