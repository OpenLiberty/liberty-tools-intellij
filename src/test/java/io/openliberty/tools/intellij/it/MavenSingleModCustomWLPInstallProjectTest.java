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

import com.intellij.remoterobot.stepsProcessing.StepLogger;
import com.intellij.remoterobot.stepsProcessing.StepWorker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Tests Liberty Tools actions using a single module Maven project.
 */
public class MavenSingleModCustomWLPInstallProjectTest extends SingleModMPProjectTestCommon {

    /**
     * Single module project name.
     */
    private static final String SM_MP_PROJECT_NAME = "singleModMavenCustomInstall";

    /**
     * The path to the folder containing the test projects.
     */
    private static final String PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "maven").toAbsolutePath().toString();

    /**
     * The paths to the integration test reports. The first is used when maven-surefire-report-plugin 3.4 is used and the second when version 3.5 is used.
     */
    private final Path pathToITReport34 = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, "target", "site", "failsafe-report.html");
    private final Path pathToITReport35 = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, "target", "reports", "failsafe.html");

    /**
     * The paths to the unit test reports. The first is used when maven-surefire-report-plugin 3.4 is used and the second when version 3.5 is used.
     */
    private final Path pathToUTReport34 = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, "target", "site", "surefire-report.html");
    private final Path pathToUTReport35 = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, "target", "reports", "surefire.html");

    /**
     * Prepares the environment for test execution.
     */
    @BeforeAll
    public static void setup() {
        StepWorker.registerProcessor(new StepLogger());
        prepareEnv(PROJECTS_PATH, SM_MP_PROJECT_NAME);
    }

    MavenSingleModCustomWLPInstallProjectTest() {
        setProjectsDirPath(PROJECTS_PATH);
        setSmMPProjectName(SM_MP_PROJECT_NAME);
        setBuildCategory(BuildType.MAVEN_TYPE);
        setSmMpProjPort(9080);
        setSmMpProjResURI("api/resource");
        setSmMPProjOutput("Hello! Welcome to Open Liberty");
        setWLPInstallPath(Paths.get("target", "liberty").toString());
        setTestReportPath(Paths.get(getProjectsDirPath(), getSmMPProjectName(), "build", "reports", "tests", "test", "index.html"));
        setBuildFileName("pom.xml");
        setBuildFileOpenCommand("Liberty: View pom.xml");
        setStartParams("-DhotTests=true");
        setStartParamsDebugPort("-DdebugPort=9876");
        setProjectTypeIsMultiple(false);
        setBuildDirectory("target");
    }
    /**
     * Deletes test reports.
     */
    @Override
    public void deleteTestReports() {
        boolean itReportDeleted = TestUtils.deleteFile(pathToITReport34);
        Assertions.assertTrue(itReportDeleted, () -> "Test report file: " + pathToITReport34 + " was not be deleted.");
        itReportDeleted = TestUtils.deleteFile(pathToITReport35);
        Assertions.assertTrue(itReportDeleted, () -> "Test report file: " + pathToITReport35 + " was not be deleted.");

        boolean utReportDeleted = TestUtils.deleteFile(pathToUTReport34);
        Assertions.assertTrue(utReportDeleted, () -> "Test report file: " + pathToUTReport34 + " was not be deleted.");
        utReportDeleted = TestUtils.deleteFile(pathToUTReport35);
        Assertions.assertTrue(utReportDeleted, () -> "Test report file: " + pathToUTReport35 + " was not be deleted.");
    }

    /**
     * Validates that test reports were generated.
     */
    @Override
    public void validateTestReportsExist() {
        TestUtils.validateTestReportExists(pathToITReport34, pathToITReport35);
        TestUtils.validateTestReportExists(pathToUTReport34, pathToUTReport35);
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
