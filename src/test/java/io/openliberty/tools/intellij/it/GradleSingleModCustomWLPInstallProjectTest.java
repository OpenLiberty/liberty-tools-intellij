/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.it;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Tests Liberty Tools actions using a single module Gradle project.
 */
public class GradleSingleModCustomWLPInstallProjectTest extends SingleModMPProjectTestCommon {
    /**
     * Single module project name.
     */
    private static final String SM_MP_PROJECT_NAME = "singleModGradleCustomInstall";

    /**
     * The path to the folder containing the test projects.
     */
    private static final String PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "gradle").toAbsolutePath().toString();

    /**
     * Prepares the environment for test execution.
     */
    @BeforeAll
    public static void setup() {
        prepareEnv(PROJECTS_PATH, SM_MP_PROJECT_NAME);
    }

    GradleSingleModCustomWLPInstallProjectTest() {
        setProjectsDirPath(PROJECTS_PATH);
        setSmMPProjectName(SM_MP_PROJECT_NAME);
        setBuildCategory(BuildType.GRADLE_TYPE);
        setSmMpProjPort(9080);
        setSmMpProjResURI("api/resource");
        setSmMPProjOutput("Hello! Welcome to Open Liberty");
        setWLPInstallPath("build");
        setTestReportPath(Paths.get(getProjectsDirPath(), getSmMPProjectName(), "build", "reports", "tests", "test", "index.html"));
        setBuildFileName("build.gradle");
        setBuildFileOpenCommand("Liberty: View Gradle config");
        setStartParams("--hotTests");
        setStartParamsDebugPort("--libertyDebugPort=9876");
        setProjectTypeIsMultiple(false);
        setBuildDirectory("build");
    }

    /**
     * Checks if the debug port is set to the specified value in the server.env file.
     *
     * @param absoluteWLPPath The absolute path to the WLP directory.
     * @param debugPort The debug port to check in the server.env file.
     * @throws IOException If an I/O error occurs while reading the server.env file.
     */
    @Override
    public void checkDebugPort(String absoluteWLPPath, int debugPort) throws IOException {
        boolean fileExists = checkFileExists("liberty-plugin-config.xml");
        if (fileExists) {
            absoluteWLPPath = getCustomWLPPath();
        }
        // Retrieve the WLP server.env file path
        Path serverEnvPath = Paths.get(absoluteWLPPath, "wlp", "usr", "servers", "defaultServer", "server.env");
        // Read all lines from server.env
        List<String> lines = Files.readAllLines(serverEnvPath);
        // Check if Debug Port is set to the specified port
        boolean debugPortIsSet = lines.stream().anyMatch(line -> line.contains("WLP_DEBUG_ADDRESS=" + debugPort));
        Assertions.assertTrue(debugPortIsSet, "Debug Port is not set to " + debugPort);
    }
}