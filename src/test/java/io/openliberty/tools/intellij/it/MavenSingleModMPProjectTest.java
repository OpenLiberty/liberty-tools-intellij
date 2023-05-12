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
import java.util.Map;

/**
 * Tests Liberty Tools actions using a single module MicroProfile Maven project.
 */
public class MavenSingleModMPProjectTest extends SingleModMPProjectTestCommon {

    /**
     * Single module Microprofile project name.
     */
    private static final String SM_MP_PROJECT_NAME = "singleModMavenMP";

    /**
     * The path to the folder containing the test projects.
     */
    private static final String PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "maven").toAbsolutePath().toString();

    /**
     * Project port.
     */
    private final int SM_MP_PROJECT_PORT = 9080;

    /**
     * Project resource URI.
     */
    private final String SM_MP_PROJECT_RES_URI = "api/resource";

    /**
     * Project response.
     */
    private final String SM_MP_PROJECT_OUTPUT = "Hello! Welcome to Open Liberty";

    /**
     * Relative location of the WLP installation.
     */
    private final String WLP_INSTALL_PATH = Paths.get("target", "liberty").toString();

    /**
     * Build file name.
     */
    private final String BUILD_FILE_NAME = "pom.xml";

    /**
     * Action command to open the build file.
     */
    private final String BUILD_FILE_OPEN_CMD = "Liberty: View effective POM";

    /**
     * The path to the integration test reports.
     */
    private final Path pathToITReport = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, "target", "site", "failsafe-report.html");

    /**
     * The path to the unit test reports.
     */
    private final Path pathToUTReport = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, "target", "site", "surefire-report.html");

    /**
     * Dev mode configuration start parameters.
     */
    private final String DEV_MODE_START_PARAMS = "-DhotTests=true";

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
     * Returns the projects directory path.
     *
     * @return The projects directory path.
     */
    @Override
    public String getProjectsDirPath() {
        return PROJECTS_PATH;
    }

    /**
     * Returns the name of the single module MicroProfile project.
     *
     * @return The name of the single module MicroProfile project.
     */
    @Override
    public String getSmMPProjectName() {
        return SM_MP_PROJECT_NAME;
    }

    /**
     * Returns the expected HTTP response payload associated with the single module
     * MicroProfile project.
     *
     * @return The expected HTTP response payload associated with the single module
     * MicroProfile project.
     */
    @Override
    public String getSmMPProjOutput() {
        return SM_MP_PROJECT_OUTPUT;
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
     * Returns the custom start parameters to be used to start dev mode.
     *
     * @return The custom start parameters to be used to start dev mode.
     */
    @Override
    public String getStartParams() {
        return DEV_MODE_START_PARAMS;
    }

    /**
     * Deletes test reports.
     */
    @Override
    public void deleteTestReports() {
        boolean itReportDeleted = TestUtils.deleteFile(pathToITReport);
        Assertions.assertTrue(itReportDeleted, () -> "Test report file: " + pathToITReport + " was not be deleted.");

        boolean utReportDeleted = TestUtils.deleteFile(pathToUTReport);
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
     * Tests Liberty tool window start.../stop actions selected on the project's drop-down action
     * menu and run using the play action button on the Liberty tool window's toolbar.
     * Note: SingleModAppTestCommon when <a href="https://github.com/OpenLiberty/liberty-tools-intellij/issues/272">...</a>
     * is fixed.
     */
    @Test
    @Video
    public void testStartWithParamsActionUsingPlayToolbarButton() {
        String testName = "testStartWithParamsActionUsingPlayToolbarButton";
        String absoluteWLPPath = Paths.get(getProjectsDirPath(), getSmMPProjectName(), getWLPInstallPath()).toString();

        // Delete any existing test report files.
        deleteTestReports();

        // Remove all other configurations first.
        UIBotTestUtils.deleteLibertyRunConfigurations(remoteRobot);

        // Trigger the start with parameters configuration dialog.
        UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Start...", true);

        // Run the configuration dialog.
        UIBotTestUtils.runStartParamsConfigDialog(remoteRobot, getStartParams());

        try {
            // Validate that the application started.
            TestUtils.validateProjectStarted(testName, getSmMpProjResURI(), getSmMpProjPort(), getSmMPProjOutput(), absoluteWLPPath, false);

            // Validate that the report was generated.
            validateTestReportsExist();
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Sleep for a few seconds to allow dev mode to finish running the tests. Specially
                // for those times when the tests are run twice. Not waiting, opens up a window
                // that leads to false negative results, and the Liberty server being left active.
                // If the Liberty server is left active, subsequent tests will fail.
                TestUtils.sleepAndIgnoreException(30);

                // Stop Liberty dev mode and validates that the Liberty server is down.
                UIBotTestUtils.runStopAction(remoteRobot, testName, UIBotTestUtils.ActionExecType.LTWPLAY, absoluteWLPPath, getSmMPProjectName(), 3);
            }
        }

        // Validate that the start with params action brings up the configuration previously used.
        try {
            UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Start...", true);
            Map<String, String> cfgEntries = UIBotTestUtils.getOpenedLibertyConfigDataAndCloseOnExit(remoteRobot);
            String activeCfgName = cfgEntries.get(UIBotTestUtils.ConfigEntries.NAME.toString());
            Assertions.assertEquals(getSmMPProjectName(), activeCfgName, "The active config name " + activeCfgName + " does not match expected name of " + getSmMPProjectName());
            String activeCfgParams = cfgEntries.get(UIBotTestUtils.ConfigEntries.PARAMS.toString());
            Assertions.assertEquals(getStartParams(), activeCfgParams, "The active config params " + activeCfgParams + " does not match expected params of " + getStartParams());
        } finally {
            // Cleanup configurations.
            UIBotTestUtils.deleteLibertyRunConfigurations(remoteRobot);
        }
    }

    /**
     * Tests Liberty tool window start/RunTests/stop actions selected on the project's drop-down action
     * menu and run using the play action button on the Liberty tool window's toolbar.
     * Note: SingleModAppTestCommon when <a href="https://github.com/OpenLiberty/liberty-tools-intellij/issues/272">...</a>
     * is fixed.
     */
    @Test
    @Video
    public void testRunTestsActionUsingPlayToolbarButton() {
        String testName = "testRunTestsActionUsingPlayToolbarButton";
        String absoluteWLPPath = Paths.get(getProjectsDirPath(), getSmMPProjectName(), getWLPInstallPath()).toString();

        // Delete any existing test report files.
        deleteTestReports();

        // Start dev mode.
        UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Start", true);

        try {
            // Validate that the application started.
            TestUtils.validateProjectStarted(testName, getSmMpProjResURI(), getSmMpProjPort(), getSmMPProjOutput(), absoluteWLPPath, false);

            // Run the application's tests.
            UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Run tests", true);

            // Validate that the report was generated.
            validateTestReportsExist();
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Stop dev mode.
                UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Stop", true);

                // Validate that the server stopped.
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
            }
        }
    }

    /**
     * Tests Liberty tool window start.../stop actions run from the project's pop-up action menu.
     * Note: SingleModAppTestCommon when <a href="https://github.com/OpenLiberty/liberty-tools-intellij/issues/272">...</a>
     * is fixed.
     */
    @Test
    @Video
    public void testStartWithParamsActionUsingPopUpMenu() {
        String testName = "testStartWithParamsActionUsingPopUpMenu";
        String absoluteWLPPath = Paths.get(getProjectsDirPath(), getSmMPProjectName(), getWLPInstallPath()).toString();

        // Delete any existing test report files.
        deleteTestReports();

        // Remove all other configurations first.
        UIBotTestUtils.deleteLibertyRunConfigurations(remoteRobot);

        // Trigger the start with parameters configuration dialog.
        UIBotTestUtils.runActionLTWPopupMenu(remoteRobot, getSmMPProjectName(), "Liberty: Start...");

        // Run the configuration dialog.
        UIBotTestUtils.runStartParamsConfigDialog(remoteRobot, getStartParams());

        try {
            // Validate that the application started.
            TestUtils.validateProjectStarted(testName, getSmMpProjResURI(), getSmMpProjPort(), getSmMPProjOutput(), absoluteWLPPath, false);

            // Validate that the report was generated.
            validateTestReportsExist();
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Sleep for a few seconds to allow dev mode to finish running the tests. Specially
                // for those times when the tests are run twice. Not waiting, opens up a window
                // that leads to false negative results, and the Liberty server being left active.
                // If the Liberty server is left active, subsequent tests will fail.
                TestUtils.sleepAndIgnoreException(30);

                // Stop Liberty dev mode and validates that the Liberty server is down.
                UIBotTestUtils.runStopAction(remoteRobot, testName, UIBotTestUtils.ActionExecType.LTWPOPUP, absoluteWLPPath, getSmMPProjectName(), 3);
            }
        }

        // Validate that the start with params action brings up the configuration previously used.
        try {
            UIBotTestUtils.runActionLTWPopupMenu(remoteRobot, getSmMPProjectName(), "Liberty: Start...");
            Map<String, String> cfgEntries = UIBotTestUtils.getOpenedLibertyConfigDataAndCloseOnExit(remoteRobot);
            String activeCfgName = cfgEntries.get(UIBotTestUtils.ConfigEntries.NAME.toString());
            Assertions.assertEquals(getSmMPProjectName(), activeCfgName, "The active config name " + activeCfgName + " does not match expected name of " + getSmMPProjectName());
            String activeCfgParams = cfgEntries.get(UIBotTestUtils.ConfigEntries.PARAMS.toString());
            Assertions.assertEquals(getStartParams(), activeCfgParams, "The active config params " + activeCfgParams + " does not match expected params of " + getStartParams());
        } finally {
            // Cleanup configurations.
            UIBotTestUtils.deleteLibertyRunConfigurations(remoteRobot);
        }
    }

    /**
     * Tests Liberty tool window start/runTests/stop actions run from the project's pop-up action menu.
     * Note: SingleModAppTestCommon when <a href="https://github.com/OpenLiberty/liberty-tools-intellij/issues/272">...</a>
     * is fixed.
     */
    @Test
    @Video
    public void testRunTestsActionUsingPopUpMenu() {
        String testName = "testRunTestsActionUsingPopUpMenu";
        String absoluteWLPPath = Paths.get(getProjectsDirPath(), getSmMPProjectName(), getWLPInstallPath()).toString();

        // Delete any existing test report files.
        deleteTestReports();

        // Start dev mode.
        UIBotTestUtils.runActionLTWPopupMenu(remoteRobot, getSmMPProjectName(), "Liberty: Start");

        try {
            // Validate that the application started.
            TestUtils.validateProjectStarted(testName, getSmMpProjResURI(), getSmMpProjPort(), getSmMPProjOutput(), absoluteWLPPath, false);

            // Run the application's tests.
            UIBotTestUtils.runActionLTWPopupMenu(remoteRobot, getSmMPProjectName(), "Liberty: Run tests");

            // Validate that the reports were generated.
            validateTestReportsExist();
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Stop dev mode.
                UIBotTestUtils.runActionLTWPopupMenu(remoteRobot, getSmMPProjectName(), "Liberty: Stop");

                // Validate that the server stopped.
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
            }
        }
    }

    /**
     * Tests start.../stop actions run from the search everywhere panel.
     * Note: SingleModAppTestCommon when <a href="https://github.com/OpenLiberty/liberty-tools-intellij/issues/272">...</a>
     * is fixed.
     */
    @Test
    @Video
    public void testStartWithParamsActionUsingSearch() {
        String testName = "testStartWithParamsActionUsingSearch";
        String absoluteWLPPath = Paths.get(getProjectsDirPath(), getSmMPProjectName(), getWLPInstallPath()).toString();

        // Delete any existing test report files.
        deleteTestReports();

        // Remove all other configurations first.
        UIBotTestUtils.deleteLibertyRunConfigurations(remoteRobot);

        // Trigger the start with parameters configuration dialog.
        UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Start...");

        // Run the configuration dialog.
        UIBotTestUtils.runStartParamsConfigDialog(remoteRobot, getStartParams());

        try {
            // Validate that the application started.
            TestUtils.validateProjectStarted(testName, getSmMpProjResURI(), getSmMpProjPort(), getSmMPProjOutput(), absoluteWLPPath, false);

            // Validate that the report was generated.
            validateTestReportsExist();
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Sleep for a few seconds to allow dev mode to finish running the tests. Specially
                // for those times when the tests are run twice. Not waiting, opens up a window
                // that leads to false negative results, and the Liberty server being left active.
                // If the Liberty server is left active, subsequent tests will fail.
                TestUtils.sleepAndIgnoreException(30);

                // Stop Liberty dev mode and validates that the Liberty server is down.
                UIBotTestUtils.runStopAction(remoteRobot, testName, UIBotTestUtils.ActionExecType.SEARCH, absoluteWLPPath, getSmMPProjectName(), 3);
            }
        }

        // Validate that the start with params action brings up the configuration previously used.
        try {
            UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Start...");
            Map<String, String> cfgEntries = UIBotTestUtils.getOpenedLibertyConfigDataAndCloseOnExit(remoteRobot);
            String activeCfgName = cfgEntries.get(UIBotTestUtils.ConfigEntries.NAME.toString());
            Assertions.assertEquals(getSmMPProjectName(), activeCfgName, "The active config name " + activeCfgName + " does not match expected name of " + getSmMPProjectName());
            String activeCfgParams = cfgEntries.get(UIBotTestUtils.ConfigEntries.PARAMS.toString());
            Assertions.assertEquals(getStartParams(), activeCfgParams, "The active config params " + activeCfgParams + " does not match expected params of " + getStartParams());
        } finally {
            // Cleanup configurations.
            UIBotTestUtils.deleteLibertyRunConfigurations(remoteRobot);
        }
    }

    /**
     * Tests start/runTests/stop actions run from the search everywhere panel.
     * Note: SingleModAppTestCommon when <a href="https://github.com/OpenLiberty/liberty-tools-intellij/issues/272">...</a>
     * is fixed.
     */
    @Test
    @Video
    public void testRunTestsActionUsingSearch() {
        String testName = "testRunTestsActionUsingSearch";
        String absoluteWLPPath = Paths.get(getProjectsDirPath(), getSmMPProjectName(), getWLPInstallPath()).toString();

        // Delete any existing test report files.
        deleteTestReports();

        // Start dev mode.
        UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Start");

        try {
            // Validate that the application started.
            TestUtils.validateProjectStarted(testName, getSmMpProjResURI(), getSmMpProjPort(), getSmMPProjOutput(), absoluteWLPPath, false);

            // Run the application's tests.
            UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Run tests");

            // Validate that the reports were generated.
            validateTestReportsExist();
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Stop dev mode.
                UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Stop");

                // Validate that the server stopped.
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
            }
        }
    }

    /**
     * Tests dashboard startInContainer/stop actions run from the project's drop-down action menu.
     * Notes:
     * 1, Once issue <a href="https://github.com/OpenLiberty/liberty-tools-intellij/issues/299">...</a> is resolved,
     * this method should be moved to SingleModProjectTestCommon.
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
        String absoluteWLPPath = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, WLP_INSTALL_PATH).toString();

        // Start dev mode in a container.
        UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Start in container", false);
        try {
            // Validate that the project started.
            TestUtils.validateProjectStarted(testName, SM_MP_PROJECT_RES_URI, SM_MP_PROJECT_PORT, SM_MP_PROJECT_OUTPUT, absoluteWLPPath, true);
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Stop dev mode.
                UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Stop", false);

                // Validate that the server stopped.
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
            }
        }
    }

    /**
     * Tests dashboard startInContainer/stop actions run from the project's drop-down action menu
     * and the Liberty tool window toolbar play button.
     * Notes:
     * 1, Once issue <a href="https://github.com/OpenLiberty/liberty-tools-intellij/issues/299">...</a> is resolved,
     * this method should be moved to SingleModProjectTestCommon.
     * 2. This test is restricted to Linux only because, on other platforms, the docker build process
     * driven by the Liberty Maven/Gradle plugins take longer than ten minutes. Ten minutes is the
     * timeout set by the plugins and there is currently no way to extend this timeout through the
     * Liberty Tools plugin(i.e. set dockerBuildTimeout).
     */
    @Test
    @Video
    @EnabledOnOs({OS.LINUX})
    public void testStartInContainerActionUsingPlayToolbarButton() {
        String testName = "testStartInContainerActionUsingPlayToolbarButton";
        String absoluteWLPPath = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, WLP_INSTALL_PATH).toString();

        // Start dev mode in a container.
        UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Start in container", true);
        try {
            // Validate that the project started.
            TestUtils.validateProjectStarted(testName, SM_MP_PROJECT_RES_URI, SM_MP_PROJECT_PORT, SM_MP_PROJECT_OUTPUT, absoluteWLPPath, true);
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Stop dev mode.
                UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Stop", true);

                // Validate that the server stopped.
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
            }
        }
    }

    /**
     * Tests dashboard startInContainer/stop actions run from the project's pop-up action menu.
     * Notes:
     * 1, Once issue <a href="https://github.com/OpenLiberty/liberty-tools-intellij/issues/299">...</a> is resolved,
     * this method should be moved to SingleModProjectTestCommon.
     * 2. This test is restricted to Linux only because, on other platforms, the docker build process
     * driven by the Liberty Maven/Gradle plugins take longer than ten minutes. Ten minutes is the
     * timeout set by the plugins and there is currently no way to extend this timeout through the
     * Liberty Tools plugin(i.e. set dockerBuildTimeout).
     */
    @Test
    @Video
    @EnabledOnOs({OS.LINUX})
    public void testStartInContainerActionUsingPopUpMenu() {
        String testName = "testStartInContainerActionUsingPopUpMenu";
        String absoluteWLPPath = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, WLP_INSTALL_PATH).toString();

        // Start dev mode in a container.
        UIBotTestUtils.runActionLTWPopupMenu(remoteRobot, SM_MP_PROJECT_NAME, "Liberty: Start in container");

        try {
            // Validate that the project started.
            TestUtils.validateProjectStarted(testName, SM_MP_PROJECT_RES_URI, SM_MP_PROJECT_PORT, SM_MP_PROJECT_OUTPUT, absoluteWLPPath, true);
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Stop dev mode.
                UIBotTestUtils.runActionLTWPopupMenu(remoteRobot, SM_MP_PROJECT_NAME, "Liberty: Stop");

                // Validate that the server stopped.
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
            }
        }
    }

    /**
     * Tests dashboard startInContainer/stop actions run from the search everywhere panel.
     * Notes:
     * 1, Once issue <a href="https://github.com/OpenLiberty/liberty-tools-intellij/issues/299">...</a> is resolved,
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
