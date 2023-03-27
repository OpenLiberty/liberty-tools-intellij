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
 * Tests Liberty Tools actions using a Maven application.
 */
public class MavenSingleModAppTest extends SingleModAppTestCommon {
    /**
     * Application Name
     */
    public static String PROJECT_NAME = "single-mod-maven-app";

    /**
     * The project path.
     */
    public static String PROJECT_PATH = Paths.get("src", "test", "resources", "apps", "maven", PROJECT_NAME).toAbsolutePath().toString();

    /**
     * Application resource URL.
     */
    public static String BASE_URL = "http://localhost:9080/";

    /**
     * Application response payload.
     */
    public static String APP_EXPECTED_OUTPUT = "Hello! Welcome to Open Liberty";

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
    public final Path pathToITReport = Paths.get(PROJECT_PATH, "target", "site", "failsafe-report.html");

    /**
     * The path to the unit test reports.
     */
    public final Path pathToUTReport = Paths.get(PROJECT_PATH, "target", "site", "surefire-report.html");


    /**
     * Tests Liberty Tool actions with a single module application that uses Maven as its build tool.
     */
    public MavenSingleModAppTest() {
        super(PROJECT_NAME, PROJECT_PATH, WLP_INSTALL_PATH, BASE_URL, APP_EXPECTED_OUTPUT);
    }

    /**
     * Prepares the environment for test execution.
     */
    @BeforeAll
    public static void setup() {
        StepWorker.registerProcessor(new StepLogger());
        prepareEnv(PROJECT_PATH, PROJECT_NAME);
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
     * this method should be moved to SingleModAppTestCommon.
     * 2. This test is restricted to Linux only because, on other platforms, the docker build process
     * driven by the Liberty Maven/Gradle plugins take longer than ten minutes. Ten minutes is the
     * timeout set by the plugins and there is currently no way to extend this timeout through the
     * Liberty Tools plugin(i.e. set dockerBuildTimeout).
     */
    @Test
    @Video
    @EnabledOnOs({OS.LINUX})
    public void testStartInContainerActionUsingDropDownMenu() {
        String testName = "testStartInContainerActionUsingDropDownMenu";
        String absoluteWLPPath = Paths.get(PROJECT_PATH, WLP_INSTALL_PATH).toString();

        // Start the start with parameters configuration dialog.
        UIBotTestUtils.runLibertyTWActionFromDropDownView(remoteRobot, "Start in container", false);
        try {
            // Validate that the application started.
            String url = appBaseURL + "api/resource";
            TestUtils.validateAppStarted(testName, url, appExpectedOutput, absoluteWLPPath);
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Stop dev mode.
                UIBotTestUtils.runLibertyTWActionFromDropDownView(remoteRobot, "Stop", false);

                // Validate that the server stopped.
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
            }
        }
    }
}
