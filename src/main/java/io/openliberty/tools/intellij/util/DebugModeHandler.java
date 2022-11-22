/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 *  SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package io.openliberty.tools.intellij.util;

import com.intellij.execution.DefaultExecutionTarget;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.remote.RemoteConfiguration;
import com.intellij.execution.remote.RemoteConfigurationType;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.Messages;
import io.openliberty.tools.intellij.LibertyModule;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Used for creating a debug configuration, connecting IntelliJ debugger to Liberty server JVM
 */
public class DebugModeHandler {

    protected static Logger LOGGER = Logger.getInstance(DebugModeHandler.class);

    // Default host name
    private static String DEFAULT_ATTACH_HOST = "localhost";

    // Regex captures the debug port value from the custom Maven parameters input
    private static final Pattern MAVEN_DEBUG_REGEX = Pattern.compile("(?<=" + Constants.LIBERTY_MAVEN_DEBUG_PARAM + ")([^\\s]+)");

    // Regex captures the debug port value from the custom Gradle parameters input
    private static final Pattern GRADLE_DEBUG_REGEX = Pattern.compile("(?<=" + Constants.LIBERTY_GRADLE_DEBUG_PARAM + ")([^\\s]+)");

    // Debug address key in Liberty server.env files
    private static String WLP_ENV_DEBUG_ADDRESS = "WLP_DEBUG_ADDRESS";

    /**
     * Gets a debug port for the Liberty module. First checks if the debug port was specified as part of the start parameters,
     * otherwise allocates a random port.
     *
     * @param libertyModule Liberty module
     * @return JVM port to connect to
     * @throws IOException
     */
    public int getDebugPort(LibertyModule libertyModule) throws IOException {
        // 1. Check if debug port was specified as part of start parameters, if so try that port first
        String configParams = libertyModule.getCustomStartParams();
        Matcher m;
        if (libertyModule.getProjectType().equals(Constants.LIBERTY_MAVEN_PROJECT)) {
            m = MAVEN_DEBUG_REGEX.matcher(configParams);
        } else {
            m = GRADLE_DEBUG_REGEX.matcher(configParams);
        }
        if (m.find()) {
            // get first match only, if for some reason more than one debug port is specified in the params, try with the first port
            String userDebugPortStr = m.group(1);
            if (userDebugPortStr != null) {
                try {
                    return Integer.parseInt(userDebugPortStr);
                } catch (NumberFormatException e) {
                    LOGGER.warn(String.format("Unable to parse debug port from user configured params: %s",userDebugPortStr));
                }
            }
        }

        // 2. Get a random port that is not in use
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /**
     * Creates a new debug configuration for the corresponding Liberty module
     *
     * @param libertyModule Liberty module
     * @param debugPort JVM port to connect to
     */
    public void createDebugConfiguration(LibertyModule libertyModule, int debugPort) {
        ProgressManager.getInstance().run(new Task.Backgroundable(libertyModule.getProject(), LocalizedResourceUtil.getMessage("liberty.run.config.title"), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                createDebugConfiguration(indicator, libertyModule, debugPort);
            }
        });
    }

    /**
     * Creates a new remote Java application debug configuration
     *
     * @param indicator progress monitor
     * @param libertyModule Liberty module
     * @param debugPort JVM port to connect to
     */
    private void createDebugConfiguration(ProgressIndicator indicator, LibertyModule libertyModule, int debugPort) {
        indicator.setText(LocalizedResourceUtil.getMessage("attaching.debugger"));
        try {
            String debugPortStr = waitForSocketActivation(indicator, libertyModule, DEFAULT_ATTACH_HOST, debugPort);
            RunnerAndConfigurationSettings settings = RunManager.getInstance(libertyModule.getProject()).createConfiguration(libertyModule.getName() + " (Remote)", RemoteConfigurationType.class);
            RemoteConfiguration remoteConfiguration = (RemoteConfiguration) settings.getConfiguration();
            remoteConfiguration.PORT = debugPortStr;
            long groupId = ExecutionEnvironment.getNextUnusedExecutionId();
            LOGGER.debug(String.format("%s: attempting to attach debugger to port %s"), libertyModule.getName(), debugPortStr);
            ExecutionUtil.runConfiguration(settings, DefaultDebugExecutor.getDebugExecutorInstance(), DefaultExecutionTarget.INSTANCE, groupId);
        } catch (Exception e) {
            // do not show error if debug attachment was cancelled by user
            if (!(e instanceof ProcessCanceledException)) {
                LOGGER.error(String.format("Cannot connect debugger to port %s", debugPort), e);
                ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(LocalizedResourceUtil.getMessage("cannot.connect.debug.port", debugPort), "Liberty"));
            }
        }
    }

    /**
     * Waits for the JDWP socket on the JVM to start listening for connections
     *
     * @param monitor progress monitor
     * @param libertyModule Liberty module
     * @param host JVM host to connect to
     * @param debugPort JVM port to connect to
     * @return debug port as a String
     * @throws Exception
     */
    private String waitForSocketActivation(ProgressIndicator monitor, LibertyModule libertyModule, String host, int debugPort) throws Exception {
        byte[] handshakeString = "JDWP-Handshake".getBytes(StandardCharsets.US_ASCII);
        int retryLimit = 120;
        int envReadMinLimit = 30;
        int envReadMaxLimit = 60;
        int envReadInterval = 5;

        Path serverEnvPath = getServerEnvPath(libertyModule);

        for (int retryCount = 0; retryCount < retryLimit; retryCount++) {
            // check if cancelled
            if (monitor.isCanceled()) {
                return null;
            }

            if (serverEnvPath == null) {
                serverEnvPath = getServerEnvPath(libertyModule);
                TimeUnit.SECONDS.sleep(1);
                continue;
            }

            // Check the server.env at the default location for any updates to WLP_DEBUG_ADDRESS for any changes.
            // There is a small window in which the allocated port could have been taken by another process.
            // If the port is already in use, dev mode will allocate a random port and reflect that by updating the server.env file.
            if (retryCount >= envReadMinLimit && retryCount < envReadMaxLimit && (retryCount % envReadInterval == 0)) {
                String envPortStr = readDebugPortFromServerEnv(serverEnvPath.toFile());
                if (envPortStr != null) {
                    int envPort = Integer.parseInt(envPortStr);
                    if (envPort != debugPort) {
                        debugPort = envPort;
                    }
                }
            }

            try (Socket socket = new Socket(host, debugPort)) {
                socket.getOutputStream().write(handshakeString);
                return String.valueOf(debugPort);
            } catch (ConnectException e) {
                TimeUnit.SECONDS.sleep(1);
            }
        }

        throw new Exception(String.format("Unable to connect to JVM on host: %s and port: %s", host, debugPort));
    }

    /**
     * Returns the default path of the server.env file after Liberty server deployment.
     *
     * @param libertyModule The Liberty module for which this operations is being performed.
     *
     * @return The default path of the server.env file after Liberty server deployment.
     *
     * @throws Exception
     */
    private Path getServerEnvPath(LibertyModule libertyModule) throws Exception {
        String projectPath = libertyModule.getBuildFile().getParent().getPath();
        Path basePath = null;
        if (libertyModule.getProjectType().equals(Constants.LIBERTY_MAVEN_PROJECT)) {
            basePath = Paths.get(projectPath, "target", "liberty", "wlp", "usr", "servers");
        } else if (libertyModule.getProjectType().equals(Constants.LIBERTY_GRADLE_PROJECT)) {
            basePath = Paths.get(projectPath, "build", "wlp", "usr", "servers");
        } else {
            throw new Exception(String.format("Unexpected project build type: %s. Liberty module %s does not appear to be a Maven or Gradle built project",
                    libertyModule.getProjectType(), libertyModule.getName()));
        }

        // Make sure the base path exists. If not return null.
        File basePathFile = new File(basePath.toString());
        if (!basePathFile.exists()) {
            return null;
        }

        try (Stream<Path> matchedStream = Files.find(basePath, 2, (path, basicFileAttribute) -> {
            if (basicFileAttribute.isRegularFile()) {
                return path.getFileName().toString().equalsIgnoreCase("server.env");
            }
            return false;
        });) {
            List<Path> matchedPaths = matchedStream.collect(Collectors.toList());
            int numberOfFilesFound = matchedPaths.size();

            if (numberOfFilesFound != 1) {
                if (numberOfFilesFound == 0) {
                    LOGGER.trace(String.format("Unable to find the server.env file for project %s", libertyModule.getName()));
                    return null;
                } else {
                    throw new Exception(String.format("More than one server.env files were found for project %s. Unable to determine the server.env file to use", libertyModule.getName()));
                }
            }
            return matchedPaths.get(0);
        }
    }

    /**
     * Returns the last debug port entry in server.env. Null if not found.
     *
     * @param serverEnv The server.env file object.
     *
     * @return The last debug port entry in server.env. Null if not found.]]
     *
     * @throws Exception
     */
    private String readDebugPortFromServerEnv(File serverEnv) throws Exception {
        String port = null;

        if (serverEnv.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(serverEnv))) {
                String line = null;
                String lastEntry = null;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(WLP_ENV_DEBUG_ADDRESS)) {
                        lastEntry = line;
                    }
                }
                if (lastEntry != null) {
                    String[] parts = lastEntry.split("=");
                    port = parts[1].trim();
                }
            }
        }
        return port;
    }
}

