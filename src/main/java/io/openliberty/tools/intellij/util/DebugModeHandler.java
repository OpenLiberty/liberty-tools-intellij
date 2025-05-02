/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation.
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
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
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // WLP server environment file name.
    public static String WLP_SERVER_ENV_FILE_NAME = "server.env";

    // WLP server environment file backup name.
    public static String WLP_SERVER_ENV_BAK_FILE_NAME = "server.env.bak";

    /**
     * Gets a debug port for the Liberty module. First checks if the debug port was specified as part of the start parameters,
     * otherwise allocates a random port.
     *
     * @param libertyModule Liberty module
     * @return JVM port to connect to
     * @throws IOException
     */
    public int getDebugPort(LibertyModule libertyModule) throws IOException {
        // 1. Check if debug port was specified as part of start parameters, if so use that port first
        String configParams = libertyModule.getCustomStartParams();
        Matcher m;
        if (libertyModule.getProjectType().equals(Constants.ProjectType.LIBERTY_MAVEN_PROJECT)) {
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
     * Creates and runs new debug configuration for the corresponding Liberty module
     *
     * @param libertyModule Liberty module
     * @param debugPort JVM port to connect to
     */
    public void createAndRunDebugConfiguration(LibertyModule libertyModule, int debugPort) {
        ProgressManager.getInstance().run(new Task.Backgroundable(libertyModule.getProject(), LocalizedResourceUtil.getMessage("liberty.run.config.title"), true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                createAndRunDebugConfiguration(indicator, libertyModule, debugPort);
            }
        });
    }

    /**
     * Creates a new remote Java application debug configuration
     *
     * @param indicator     progress monitor
     * @param libertyModule Liberty module
     * @param debugPort     JVM port to connect to
     */
    private void createAndRunDebugConfiguration(ProgressIndicator indicator, LibertyModule libertyModule, int debugPort) {
        indicator.setText(LocalizedResourceUtil.getMessage("attaching.debugger"));
        try {
            String debugPortStr = waitForSocketActivation(indicator, libertyModule, DEFAULT_ATTACH_HOST, debugPort);
            if (debugPortStr == null) {
                return;
            }
            RunnerAndConfigurationSettings settings = RunManager.getInstance(libertyModule.getProject()).createConfiguration(libertyModule.getName() + " (Remote)", RemoteConfigurationType.class);
            RemoteConfiguration remoteConfiguration = (RemoteConfiguration) settings.getConfiguration();
            remoteConfiguration.PORT = debugPortStr;
            long groupId = ExecutionEnvironment.getNextUnusedExecutionId();
            LOGGER.debug(String.format("%s: attempting to attach debugger to port %s", libertyModule.getName(), debugPortStr));
            ExecutionUtil.runConfiguration(settings, DefaultDebugExecutor.getDebugExecutorInstance(), DefaultExecutionTarget.INSTANCE, groupId);
        } catch (Exception e) {
            // do not show error if debug attachment was cancelled by user
            if (!(e instanceof ProcessCanceledException)) {
                LOGGER.warn(LocalizedResourceUtil.getMessage("cannot.attach.debugger"), e);
                ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(e.getMessage(), LocalizedResourceUtil.getMessage("cannot.attach.debugger")));
            }
        }
    }

    /**
     * Waits for the Liberty runtime JVM to start listening for connections on the JDWP socket.
     * Server.env is created by the Liberty Maven/Gradle Tool several seconds after it starts up
     * but note well that there may exist such a file from a previous start-up. Also note that
     * if the tool determines the first port is taken by another process a new port number will
     * be used for debugging and will be written into the file.
     *
     * @param monitor progress monitor
     * @param libertyModule Liberty module
     * @param host JVM host to connect to
     * @param debugPort JVM port to connect to
     * @return port that the debugger actually connected to as a String
     * @throws Exception
     */
    private String waitForSocketActivation(ProgressIndicator monitor, LibertyModule libertyModule, String host, int debugPort) throws Exception {
        byte[] handshakeString = "JDWP-Handshake".getBytes(StandardCharsets.US_ASCII);
        int retryLimit = getDebuggerTimeoutInSeconds();
        int retryInc = 3; // time to wait after each try

        // Retrieve the location of the server.env in the liberty installation at the default location (wpl/usr/servers/<serverName>).
        Path serverEnvPath = getServerEnvPath(libertyModule);
        // If the server.env has not been created yet then someone did a 'clean' before starting
        boolean cleanStart = serverEnvPath == null;
        Path serverEnvBakPath = null;

        for (int retryCount = 0; retryCount < retryLimit; retryCount+=retryInc) {
            // check if cancelled
            if (monitor.isCanceled()) {
                return null;
            }

            // Detect if dev mode has started
            if (serverEnvPath == null) {
                serverEnvPath = getServerEnvPath(libertyModule);
            } else if (serverEnvBakPath == null) {
                serverEnvBakPath = serverEnvPath.resolveSibling(WLP_SERVER_ENV_BAK_FILE_NAME);
            }

            // There is a small window in which the allocated random port could have been taken by another process.
            // Check the deployed server.env at the default deployment location (wlp/usr/servers/<serverName>) for the WLP_DEBUG_ADDRESS
            // property. If the port is already in use, dev mode will allocate a random debug port and reflect that by updating the
            // server.env file. Therefore, first we detect if dev mode has started.
            if ((cleanStart && serverEnvPath != null) ||
                    (serverEnvBakPath != null && serverEnvBakPath.toFile().exists())) {
                // Dev mode has started and updated server.env. server.env.bak only exists when dev mode is running assuming
                // dev mode did not crash. If it did crash we will be trying the old port number until dev mode really
                // updates the server.env. This is the risk we assume in the case of catastrophic failure.
                String envPortStr = readDebugPortFromServerEnv(serverEnvPath.toFile());
                if (envPortStr != null) {
                    int envPort = Integer.parseInt(envPortStr);
                    if (envPort != debugPort) {
                        debugPort = envPort;
                    }
                }
                try (Socket socket = new Socket(host, debugPort)) {
                    socket.getOutputStream().write(handshakeString);
                    return String.valueOf(debugPort);
                } catch (ConnectException e) {
                    // After dev mode starts it still takes a few seconds for the runtime to start.
                    LOGGER.trace(String.format("ConnectException waiting for runtime to start on port %d", debugPort));
                }
            }
            TimeUnit.SECONDS.sleep(retryInc);
        }
        throw new Exception(LocalizedResourceUtil.getMessage("cannot.attach.debugger.host.port", host, String.format("%d",debugPort)));
    }

    /**
     * Check an environment variable to see if the user specifies a timeout to use for the debugger.
     * This is not an exposed environment variable, it is only used for testing.
     */
    private int getDebuggerTimeoutInSeconds() {
        // Number of seconds to wait for Liberty dev mode to start. Note that Gradle can take a while
        // to get going so we default to 3 minutes. During testing it can take even longer, 4-5 minutes.
        int defaultTimeout = 180;
        String userSpecifiedTimeout = System.getenv("LIBERTY_TOOLS_INTELLIJ_DEBUGGER_TIMEOUT");
        if (userSpecifiedTimeout != null) {
            try {
                return Integer.parseInt(userSpecifiedTimeout);
            } catch (NumberFormatException n) {
                // return the default below
            }
        }
        return defaultTimeout;
    }

    /**
     * Returns the path to the `server.env` file for the given Liberty module.
     *
     * This method retrieves the server directory using the Liberty plugin config and
     * constructs the expected path to the `server.env` file. If the file exists, the path is returned;
     * otherwise, a trace log is recorded and {@code null} is returned.
     *
     * @param libertyModule the Liberty module to resolve the server environment file path for.
     * @return the path to the `server.env` file if it exists, or null if not found.
     */
    private Path getServerEnvPath(LibertyModule libertyModule) {
        String serverDirectory = getServerDirectoryFromLibertyPluginConfig(libertyModule);
        if (serverDirectory == null || serverDirectory.isEmpty()) {
            LOGGER.trace(String.format("Server directory is null or empty for project %s", libertyModule.getName()));
            return null;
        }
        Path serverEnvPath = Paths.get(serverDirectory, WLP_SERVER_ENV_FILE_NAME);
        if (Files.exists(serverEnvPath)) {
            return serverEnvPath;
        } else {
            LOGGER.trace(String.format("Unable to find the server.env file for project %s", libertyModule.getName()));
            return null;
        }
    }

    /**
     * Retrieves the server directory path from the Liberty plugin configuration file (`liberty-plugin-config.xml`)
     * for the given Liberty module.
     *
     * The method determines the build folder (`target` for Maven projects, `build` for Gradle projects)
     * based on the module's project type. It then parses the config file to extract the value of the
     * <serverDirectory> element.
     *
     * @param libertyModule the Liberty module containing build and project metadata.
     * @return the server directory path specified in the Liberty plugin config, or an empty string if not found or on error.
     */
    private String getServerDirectoryFromLibertyPluginConfig(LibertyModule libertyModule) {
        String serverDirectory = "";
        try {
            String projectPath = libertyModule.getBuildFile().getParent().getPath();
            String projectType = libertyModule.getProjectType().toString();
            String buildFolder = projectType.equals(Constants.ProjectType.LIBERTY_MAVEN_PROJECT.name()) ? "target" : "build";
            Path configPath = Paths.get(projectPath, buildFolder, "liberty-plugin-config.xml");

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newDefaultInstance();
            documentBuilderFactory.setAttribute(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(configPath.toString());
            document.getDocumentElement().normalize();

            NodeList nodeList = document.getElementsByTagName("serverDirectory");
            if (nodeList.getLength() > 0) {
                Element element = (Element) nodeList.item(0);
                serverDirectory = element.getTextContent();
            }
        } catch (Exception e) {
            LOGGER.trace("Unable to find serverDirectory from liberty-plugin-config file");
            return null;
        }
        return serverDirectory;
    }

    /**
     * Returns the port value associated with the WLP_DEBUG_ADDRESS entry in server.env. Null if not found. If there are multiple
     * WLP_DEBUG_ADDRESS entries, the last entry is returned.
     *
     * @param serverEnv The server.env file object.
     *
     * @return Returns the port value associated with the WLP_DEBUG_ADDRESS entry in server.env. Null if not found. If there are
     * multiple WLP_DEBUG_ADDRESS entries, the last entry is returned.
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
                    if (parts.length > 1) {
                        port = parts[1].trim();
                    }
                }
            }
        }
        return port;
    }
}

