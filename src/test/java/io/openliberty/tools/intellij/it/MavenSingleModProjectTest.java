/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.it;

import com.automation.remarks.junit5.Video;
import com.intellij.remoterobot.stepsProcessing.StepLogger;
import com.intellij.remoterobot.stepsProcessing.StepWorker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tests Liberty Tools actions using a Maven project.
 */
public class MavenSingleModProjectTest extends SingleModProjectTestCommon {

    /**
     * Single module Microprofile project name.
     */
    public static String SM_MP_PROJECT_NAME = "singleModMavenMP";

    /**
     * Single module REST project that lacks the configuration to be recognized by Liberty tools.
     */
    public static String SM_NLT_REST_PROJECT_NAME = "singleModMavenRESTNoLTXmlCfg";

    /**
     * The path to the folder containing the test projects.
     */
    public static String PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "maven").toAbsolutePath().toString();

    /**
     * Project port.
     */
    public static int SM_MP_PROJECT_PORT = 9080;

    /**
     * Project resource URI.
     */
    public static String SM_MP_PROJECT_RES_URI = "api/resource";

    /**
     * Project resource URL.
     */
    public static String SM_MP_PROJECT_BASE_URL = "http://localhost:" + SM_MP_PROJECT_PORT + "/";

    /**
     * Project response.
     */
    public static String SM_MP_PROJECT_OUTPUT = "Hello! Welcome to Open Liberty";

    /**
     * Relative location of the WLP installation.
     */
    public static String WLP_INSTALL_PATH = Paths.get("target", "liberty").toString();

    /**
     * Build file name.
     */
    public final String BUILD_FILE_NAME = "pom.xml";

    /**
     * Action command to open the build file.
     */
    public final String BUILD_FILE_OPEN_CMD = "Liberty: View effective POM";

    /**
     * The path to the integration test reports.
     */
    public final Path pathToITReport = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, "target", "site", "failsafe-report.html");

    /**
     * The path to the unit test reports.
     */
    public final Path pathToUTReport = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, "target", "site", "surefire-report.html");


    /**
     * Constructor.
     */
    public MavenSingleModProjectTest() {
        super(PROJECTS_PATH, SM_MP_PROJECT_NAME, SM_NLT_REST_PROJECT_NAME, SM_MP_PROJECT_BASE_URL, SM_MP_PROJECT_OUTPUT);
    }

    /**
     * Prepares the environment for test execution.
     */
    @BeforeAll
    public static void setup() {
        StepWorker.registerProcessor(new StepLogger());
        prepareEnv(PROJECTS_PATH, SM_MP_PROJECT_NAME);
    }

    /**
     * Returns the path where the Liberty server was installed.
     *
     * @return The path where the Liberty server was installed.
     */
    @Override
    public String getWLPInstallPath() {
        return WLP_INSTALL_PATH;
    }

    /**
     * Returns the port number associated with the single module MicroProfile project.
     *
     * @return The port number associated with the single module MicroProfile project.
     */
    @Override
    public int getSmMpProjPort() {
        return SM_MP_PROJECT_PORT;
    }

    /**
     * Return the Resource URI associated with the single module MicroProfile project.
     *
     * @return The Resource URI associated with the single module MicroProfile project.
     */
    @Override
    public String getSmMpProjResURI() {
        return SM_MP_PROJECT_RES_URI;
    }

    /**
     * Returns the name of the build file used by the project.
     *
     * @return The name of the build file used by the project.
     */
    @Override
    public String getBuildFileName() {
        return BUILD_FILE_NAME;
    }

    /**
     * Returns the name of the custom action command used to open the build file.
     *
     * @return The name of the custom action command used to open the build file.
     */
    @Override
    public String getBuildFileOpenCommand() {
        return BUILD_FILE_OPEN_CMD;
    }

    /**
     * Deletes test reports.
     */
    @Override
    public void deleteTestReports() {
        boolean itReportDeleted = TestUtils.deleteFile(pathToITReport.toFile());
        Assertions.assertTrue(itReportDeleted, () -> "Test report file: " + pathToITReport + " was not be deleted.");

        boolean utReportDeleted = TestUtils.deleteFile(pathToUTReport.toFile());
        Assertions.assertTrue(utReportDeleted, () -> "Test report file: " + pathToUTReport + " was not be deleted.");
    }

    /**
     * Validates that test reports were generated.
     */
    @Override
    public void validateTestReportsExist() {
        TestUtils.validateTestReportExists(pathToITReport);
        TestUtils.validateTestReportExists(pathToUTReport);
    }

    /**
     * Tests dashboard startInContainer/stop actions run from the project's drop-down action menu.
     * Notes:
     * 1, Once issue https://github.com/OpenLiberty/liberty-tools-intellij/issues/299 is resolved,
     * this method should be moved to SingleModProjectTestCommon.
     * 2. This test is restricted to Linux only because, on other platforms, the docker build process
     * driven by the Liberty Maven/Gradle plugins take longer than ten minutes. Ten minutes is the
     * timeout set by the plugins and there is currently no way to extend this timeout through the
     * Liberty Tools plugin(i.e. set dockerBuildTimeout).
     */
    @Test
    @EnabledOnOs({OS.LINUX})
    public void testStartInContainerActionUsingDropDownMenu() {
        String testName = "testStartInContainerActionUsingDropDownMenu";
        String absoluteWLPPath = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, WLP_INSTALL_PATH).toString();

        // Start dev mode in a container.
        UIBotTestUtils.runLibertyTWActionFromDropDownView(remoteRobot, "Start in container", false);
        try {
            // Validate that the project started.
            TestUtils.validateProjectStarted(testName, SM_MP_PROJECT_RES_URI, SM_MP_PROJECT_PORT, SM_MP_PROJECT_OUTPUT, absoluteWLPPath, true);
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Stop dev mode.
                UIBotTestUtils.runLibertyTWActionFromDropDownView(remoteRobot, "Stop", false);

                // Validate that the server stopped.
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
            }
        }
    }

    /**
     * Tests dashboard startInContainer/stop actions run from the project's drop-down action menu
     * and the Liberty tool window toolbar play button.
     * Notes:
     * 1, Once issue https://github.com/OpenLiberty/liberty-tools-intellij/issues/299 is resolved,
     * this method should be moved to SingleModProjectTestCommon.
     * 2. This test is restricted to Linux only because, on other platforms, the docker build process
     * driven by the Liberty Maven/Gradle plugins take longer than ten minutes. Ten minutes is the
     * timeout set by the plugins and there is currently no way to extend this timeout through the
     * Liberty Tools plugin(i.e. set dockerBuildTimeout).
     */
    @Test
    @EnabledOnOs({OS.LINUX})
    public void testStartInContainerActionUsingPlayToolbarButton() {
        String testName = "testStartInContainerActionUsingPlayToolbarButton";
        String absoluteWLPPath = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, WLP_INSTALL_PATH).toString();

        // Start dev mode in a container.
        UIBotTestUtils.runLibertyTWActionFromDropDownView(remoteRobot, "Start in container", true);
        try {
            // Validate that the project started.
            TestUtils.validateProjectStarted(testName, SM_MP_PROJECT_RES_URI, SM_MP_PROJECT_PORT, SM_MP_PROJECT_OUTPUT, absoluteWLPPath, true);
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Stop dev mode.
                UIBotTestUtils.runLibertyTWActionFromDropDownView(remoteRobot, "Stop", true);

                // Validate that the server stopped.
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
            }
        }
    }

    /**
     * Tests dashboard startInContainer/stop actions run from the project's pop-up action menu.
     * Notes:
     * 1, Once issue https://github.com/OpenLiberty/liberty-tools-intellij/issues/299 is resolved,
     * this method should be moved to SingleModProjectTestCommon.
     * 2. This test is restricted to Linux only because, on other platforms, the docker build process
     * driven by the Liberty Maven/Gradle plugins take longer than ten minutes. Ten minutes is the
     * timeout set by the plugins and there is currently no way to extend this timeout through the
     * Liberty Tools plugin(i.e. set dockerBuildTimeout).
     */
    @Test
    @EnabledOnOs({OS.LINUX})
    public void testStartInContainerActionUsingPopUpMenu() {
        String testName = "testStartInContainerActionUsingPopUpMenu";
        String absoluteWLPPath = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, WLP_INSTALL_PATH).toString();

        // Start dev mode in a container.
        UIBotTestUtils.runLibertyTWActionFromMenuView(remoteRobot, SM_MP_PROJECT_NAME, "Liberty: Start in container");

        try {
            // Validate that the project started.
            TestUtils.validateProjectStarted(testName, SM_MP_PROJECT_RES_URI, SM_MP_PROJECT_PORT, SM_MP_PROJECT_OUTPUT, absoluteWLPPath, true);
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Stop dev mode.
                UIBotTestUtils.runLibertyTWActionFromMenuView(remoteRobot, SM_MP_PROJECT_NAME, "Liberty: Stop");

                // Validate that the server stopped.
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
            }
        }
    }

    /**
     * Tests dashboard startInContainer/stop actions run from the search everywhere panel.
     * Notes:
     * 1, Once issue https://github.com/OpenLiberty/liberty-tools-intellij/issues/299 is resolved,
     * this method should be moved to SingleModProjectTestCommon.
     * 2. This test is restricted to Linux only because, on other platforms, the docker build process
     * driven by the Liberty Maven/Gradle plugins take longer than ten minutes. Ten minutes is the
     * timeout set by the plugins and there is currently no way to extend this timeout through the
     * Liberty Tools plugin(i.e. set dockerBuildTimeout).
     */
    @Test
    @Video
    @EnabledOnOs({OS.LINUX})
    public void testStartInContainerActionUsingSearch() {
        String testName = "testStartInContainerActionUsingSearch";
        String absoluteWLPPath = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, WLP_INSTALL_PATH).toString();

        // Start dev mode in a container.
        UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Start in container");

        try {
            // Validate that the project started.
            TestUtils.validateProjectStarted(testName, SM_MP_PROJECT_RES_URI, SM_MP_PROJECT_PORT, SM_MP_PROJECT_OUTPUT, absoluteWLPPath, true);
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Stop dev mode.
                UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Stop");

                // Validate that the server stopped.
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
            }
        }
    }
}
