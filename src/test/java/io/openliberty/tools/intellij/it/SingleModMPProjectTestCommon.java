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
import com.intellij.remoterobot.RemoteRobot;
import io.openliberty.tools.intellij.it.fixtures.WelcomeFrameFixture;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;

import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitForIgnoringError;

/**
 * Holds common tests that use a single module MicroProfile project.
 */
public abstract class SingleModMPProjectTestCommon {

    /**
     * URL to display the UI Component hierarchy. This is used to obtain xPath related
     * information to find UI components.
     */
    public static final String REMOTE_BOT_URL = "http://localhost:8082";

    /**
     * The remote robot object.
     */
    public static final RemoteRobot remoteRobot = new RemoteRobot(REMOTE_BOT_URL);

    /**
     * Processes actions before each test.
     *
     * @param info Test information.
     */
    @BeforeEach
    public void beforeEach(TestInfo info) {
        TestUtils.printTrace(TestUtils.TraceSevLevel.INFO, this.getClass().getSimpleName() + "." + info.getDisplayName() + ". Entry");
    }

    /**
     * Processes actions after each test.
     *
     * @param info Test information.
     */
    @AfterEach
    public void afterEach(TestInfo info) {
        TestUtils.printTrace(TestUtils.TraceSevLevel.INFO, this.getClass().getSimpleName() + "." + info.getDisplayName() + ". Exit");
    }

    /**
     * Cleanup.
     */
    @AfterAll
    public static void cleanup() {
        UIBotTestUtils.closeLibertyToolWindow(remoteRobot);
        UIBotTestUtils.closeProjectView(remoteRobot);
        UIBotTestUtils.closeProjectFrame(remoteRobot);
        UIBotTestUtils.validateProjectFrameClosed(remoteRobot);
    }

    /**
     * Tests the liberty: View <project build file> action run from the project's pop-up action menu.
     */
    @Test
    @Video
    public void testOpenBuildFileActionUsingPopUpMenu() {
        String editorTabName = getBuildFileName() + " (" + getSmMPProjectName() + ")";

        // Close the editor tab if it was previously opened.
        UIBotTestUtils.closeFileEditorTab(remoteRobot, editorTabName, "5");

        // Open the build file.
        UIBotTestUtils.openLibertyToolWindow(remoteRobot);
        UIBotTestUtils.runActionLTWPopupMenu(remoteRobot, getSmMPProjectName(), getBuildFileOpenCommand(), 3);

        // Verify that build file tab is opened.
        Assertions.assertNotNull(UIBotTestUtils.getEditorTabCloseButton(remoteRobot, editorTabName, "10"),
                "Editor tab with the name of " + editorTabName + " could not be found.");

        // Close the editor tab.
        UIBotTestUtils.closeFileEditorTab(remoteRobot, editorTabName, "10");
    }

    /**
     * Tests dashboard start.../stop actions run from the project's drop-down action menu.
     */
    @Test
    @Video
    public void testStartWithParamsActionUsingDropDownMenu() {
        String testName = "testStartWithParamsActionUsingDropDownMenu";
        String absoluteWLPPath = Paths.get(getProjectsDirPath(), getSmMPProjectName(), getWLPInstallPath()).toString();

        // Remove all other configurations first.
        UIBotTestUtils.deleteLibertyRunConfigurations(remoteRobot);

        // Delete any existing test report files.
        deleteTestReports();

        // Trigger the start with parameters configuration dialog.
        UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Start...", false, 3);

        // Run the configuration dialog.
        UIBotTestUtils.runStartParamsConfigDialog(remoteRobot, getStartParams());

        try {
            // Validate that the project started.
            TestUtils.validateProjectStarted(testName, getSmMpProjResURI(), getSmMpProjPort(), getSmMPProjOutput(), absoluteWLPPath, false);

            // Validate that the report was generated.
            validateTestReportsExist();
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Sleep for a few seconds to allow dev mode to finish running the tests. Specially
                // for those times when the tests are run twice. Not waiting, opens up a window
                // that leads to false negative results, and the Liberty server being left active.
                // If the Liberty server is left active, subsequent tests will fail.
                TestUtils.sleepAndIgnoreException(60);

                // Stop Liberty dev mode and validates that the Liberty server is down.
                UIBotTestUtils.runStopAction(remoteRobot, testName, UIBotTestUtils.ActionExecType.LTWDROPDOWN, absoluteWLPPath, getSmMPProjectName(), 3);
            }
        }

        // Validate that the start with params action brings up the configuration previously used.
        try {
            UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Start...", false, 3);
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
     * Tests Liberty tool window start.../stop actions selected on the project's drop-down action
     * menu and run using the play action button on the Liberty tool window's toolbar.
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
        UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Start...", true, 3);

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
                TestUtils.sleepAndIgnoreException(60);

                // Stop Liberty dev mode and validates that the Liberty server is down.
                UIBotTestUtils.runStopAction(remoteRobot, testName, UIBotTestUtils.ActionExecType.LTWPLAY, absoluteWLPPath, getSmMPProjectName(), 3);
            }
        }

        // Validate that the start with params action brings up the configuration previously used.
        try {
            UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Start...", true, 3);
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
     * Tests Liberty tool window start.../stop actions run from the project's pop-up action menu.
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
        UIBotTestUtils.runActionLTWPopupMenu(remoteRobot, getSmMPProjectName(), "Liberty: Start...", 3);

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
                TestUtils.sleepAndIgnoreException(60);

                // Stop Liberty dev mode and validates that the Liberty server is down.
                UIBotTestUtils.runStopAction(remoteRobot, testName, UIBotTestUtils.ActionExecType.LTWPOPUP, absoluteWLPPath, getSmMPProjectName(), 3);
            }
        }

        // Validate that the start with params action brings up the configuration previously used.
        try {
            UIBotTestUtils.runActionLTWPopupMenu(remoteRobot, getSmMPProjectName(), "Liberty: Start...", 3);
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
     * Tests start.../stop actions run from the search everywhere panel.
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
        UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Start...", 3);

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
                TestUtils.sleepAndIgnoreException(60);

                // Stop Liberty dev mode and validates that the Liberty server is down.
                UIBotTestUtils.runStopAction(remoteRobot, testName, UIBotTestUtils.ActionExecType.SEARCH, absoluteWLPPath, getSmMPProjectName(), 3);
            }
        }

        // Validate that the start with params action brings up the configuration previously used.
        try {
            UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Start...", 3);
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
     * Tests dashboard start/RunTests/stop actions run from the project's drop-down action menu.
     */
    @Test
    @Video
    public void testRunTestsActionUsingDropDownMenu() {
        String testName = "testRunTestsActionUsingDropDownMenu";
        String absoluteWLPPath = Paths.get(getProjectsDirPath(), getSmMPProjectName(), getWLPInstallPath()).toString();

        // Delete any existing test report files.
        deleteTestReports();

        // Start dev mode.
        UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Start", false, 3);

        // Validate that the project started.
        TestUtils.validateProjectStarted(testName, getSmMpProjResURI(), getSmMpProjPort(), getSmMPProjOutput(), absoluteWLPPath, false);

        try {
            // Run the project's tests.
            UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Run tests", false, 3);

            // Validate that the report was generated.
            validateTestReportsExist();
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Sleep for a few seconds to allow dev mode to finish running the tests. Specially
                // for those times when the tests are run twice. Not waiting, opens up a window
                // that leads to false negative results, and the Liberty server being left active.
                // If the Liberty server is left active, subsequent tests will fail.
                TestUtils.sleepAndIgnoreException(60);

                // Stop Liberty dev mode and validates that the Liberty server is down.
                UIBotTestUtils.runStopAction(remoteRobot, testName, UIBotTestUtils.ActionExecType.LTWDROPDOWN, absoluteWLPPath, getSmMPProjectName(), 3);
            }
        }
    }

    /**
     * Tests Liberty tool window start/RunTests/stop actions selected on the project's drop-down action
     * menu and run using the play action button on the Liberty tool window's toolbar.
     */
    @Test
    @Video
    public void testRunTestsActionUsingPlayToolbarButton() {
        String testName = "testRunTestsActionUsingPlayToolbarButton";
        String absoluteWLPPath = Paths.get(getProjectsDirPath(), getSmMPProjectName(), getWLPInstallPath()).toString();

        // Delete any existing test report files.
        deleteTestReports();

        // Start dev mode.
        UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Start", true, 3);

        try {
            // Validate that the application started.
            TestUtils.validateProjectStarted(testName, getSmMpProjResURI(), getSmMpProjPort(), getSmMPProjOutput(), absoluteWLPPath, false);

            // Run the application's tests.
            UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Run tests", true, 3);

            // Validate that the report was generated.
            validateTestReportsExist();
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Sleep for a few seconds to allow dev mode to finish running the tests. Specially
                // for those times when the tests are run twice. Not waiting, opens up a window
                // that leads to false negative results, and the Liberty server being left active.
                // If the Liberty server is left active, subsequent tests will fail.
                TestUtils.sleepAndIgnoreException(60);

                // Stop Liberty dev mode and validates that the Liberty server is down.
                UIBotTestUtils.runStopAction(remoteRobot, testName, UIBotTestUtils.ActionExecType.LTWDROPDOWN, absoluteWLPPath, getSmMPProjectName(), 3);
            }
        }
    }

    /**
     * Tests Liberty tool window start/runTests/stop actions run from the project's pop-up action menu.
     */
    @Test
    @Video
    public void testRunTestsActionUsingPopUpMenu() {
        String testName = "testRunTestsActionUsingPopUpMenu";
        String absoluteWLPPath = Paths.get(getProjectsDirPath(), getSmMPProjectName(), getWLPInstallPath()).toString();

        // Delete any existing test report files.
        deleteTestReports();

        // Start dev mode.
        UIBotTestUtils.runActionLTWPopupMenu(remoteRobot, getSmMPProjectName(), "Liberty: Start", 3);

        try {
            // Validate that the application started.
            TestUtils.validateProjectStarted(testName, getSmMpProjResURI(), getSmMpProjPort(), getSmMPProjOutput(), absoluteWLPPath, false);

            // Run the application's tests.
            UIBotTestUtils.runActionLTWPopupMenu(remoteRobot, getSmMPProjectName(), "Liberty: Run tests", 3);

            // Validate that the reports were generated.
            validateTestReportsExist();
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Sleep for a few seconds to allow dev mode to finish running the tests. Specially
                // for those times when the tests are run twice. Not waiting, opens up a window
                // that leads to false negative results, and the Liberty server being left active.
                // If the Liberty server is left active, subsequent tests will fail.
                TestUtils.sleepAndIgnoreException(60);

                // Stop Liberty dev mode and validates that the Liberty server is down.
                UIBotTestUtils.runStopAction(remoteRobot, testName, UIBotTestUtils.ActionExecType.LTWPOPUP, absoluteWLPPath, getSmMPProjectName(), 3);
            }
        }
    }

    /**
     * Tests start/runTests/stop actions run from the search everywhere panel.
     */
    @Test
    @Video
    public void testRunTestsActionUsingSearch() {
        String testName = "testRunTestsActionUsingSearch";
        String absoluteWLPPath = Paths.get(getProjectsDirPath(), getSmMPProjectName(), getWLPInstallPath()).toString();

        // Delete any existing test report files.
        deleteTestReports();

        // Start dev mode.
        UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Start", 3);

        try {
            // Validate that the application started.
            TestUtils.validateProjectStarted(testName, getSmMpProjResURI(), getSmMpProjPort(), getSmMPProjOutput(), absoluteWLPPath, false);

            // Run the application's tests.
            UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Run tests", 3);

            // Validate that the reports were generated.
            validateTestReportsExist();
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Sleep for a few seconds to allow dev mode to finish running the tests. Specially
                // for those times when the tests are run twice. Not waiting, opens up a window
                // that leads to false negative results, and the Liberty server being left active.
                // If the Liberty server is left active, subsequent tests will fail.
                TestUtils.sleepAndIgnoreException(60);

                // Stop Liberty dev mode and validates that the Liberty server is down.
                UIBotTestUtils.runStopAction(remoteRobot, testName, UIBotTestUtils.ActionExecType.SEARCH, absoluteWLPPath, getSmMPProjectName(), 3);
            }
        }
    }

    /**
     * Tests:
     * - Creating a new Liberty tools configuration.
     * - Using project frame toolbar's config selection box and Debug icon to select a Liberty configuration and start dev mode.
     * - Automatic server JVM attachment to the debugger.
     */
    @Test
    @Video
    public void testStartWithConfigInDebugModeUsingToolbar() {
        String testName = "testStartWithConfigInDebugModeUsingToolbar";
        String absoluteWLPPath = Paths.get(getProjectsDirPath(), getSmMPProjectName(), getWLPInstallPath()).toString();

        // Remove all other configurations first.
        UIBotTestUtils.deleteLibertyRunConfigurations(remoteRobot);

        // Add a new Liberty config.
        String configName = "toolBarDebug-" + getSmMPProjectName();
        UIBotTestUtils.createLibertyConfiguration(remoteRobot, configName);

        // Find the newly created config in the config selection box on the project frame.
        UIBotTestUtils.selectConfigUsingToolbar(remoteRobot, configName);

        // Click on the debug icon for the selected configuration.
        UIBotTestUtils.runConfigUsingIconOnToolbar(remoteRobot, UIBotTestUtils.ExecMode.DEBUG);

        try {
            // Validate that the project started.
            TestUtils.validateProjectStarted(testName, getSmMpProjResURI(), getSmMpProjPort(), getSmMPProjOutput(), absoluteWLPPath, false);

            // Stop the debugger.
            // When the debugger is attached, the debugger window should open automatically.
            // If the debugger was not attached or if the debugger window was not opened,
            // the stop request will time out.
            UIBotTestUtils.stopDebugger(remoteRobot);
        } finally {
            try {
                // Open the terminal window.
                UIBotTestUtils.openTerminalWindow(remoteRobot);
            } finally {
                try {
                    // If the debugger did not attach, there might be an error dialog. Close it.
                    try {
                        UIBotTestUtils.closeErrorDialog(remoteRobot);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } finally {
                    try {
                        // Stop the server.
                        if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                            UIBotTestUtils.runStopAction(remoteRobot, testName, UIBotTestUtils.ActionExecType.LTWDROPDOWN, absoluteWLPPath, getSmMPProjectName(), 3);
                        }
                    } finally {
                        // Cleanup configurations.
                        UIBotTestUtils.deleteLibertyRunConfigurations(remoteRobot);
                    }
                }
            }
        }
    }

    /**
     * Tests:
     * - Creating a new Liberty tools configuration.
     * - Using Run->Debug... menu options to select the configuration and run in the project in dev mode.
     */
    @Test
    @Video
    public void testStartWithConfigInDebugModeUsingMenu() {
        String testName = "testStartWithConfigInDebugModeUsingMenu";
        String absoluteWLPPath = Paths.get(getProjectsDirPath(), getSmMPProjectName(), getWLPInstallPath()).toString();

        // Remove all other configurations first.
        UIBotTestUtils.deleteLibertyRunConfigurations(remoteRobot);

        // Add a new Liberty config.
        String configName = "menuDebug-" + getSmMPProjectName();
        UIBotTestUtils.createLibertyConfiguration(remoteRobot, configName);

        // Find the newly created config in the config selection box on the project frame.
        UIBotTestUtils.selectConfigUsingMenu(remoteRobot, configName, UIBotTestUtils.ExecMode.DEBUG);

        try {
            // Validate that the project started.
            TestUtils.validateProjectStarted(testName, getSmMpProjResURI(), getSmMpProjPort(), getSmMPProjOutput(), absoluteWLPPath, false);

            // Stop the debugger.
            // When the debugger is attached, the debugger window should open automatically.
            // If the debugger was not attached or if the debugger window was not opened,
            // the stop request will time out.
            UIBotTestUtils.stopDebugger(remoteRobot);
        } finally {
            try {
                // Open the terminal window.
                UIBotTestUtils.openTerminalWindow(remoteRobot);
            } finally {
                try {
                    // If the debugger did not attach, there might be an error dialog. Close it.
                    try {
                        UIBotTestUtils.closeErrorDialog(remoteRobot);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } finally {
                    try {
                        // Stop the server.
                        if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                            UIBotTestUtils.runStopAction(remoteRobot, testName, UIBotTestUtils.ActionExecType.LTWDROPDOWN, absoluteWLPPath, getSmMPProjectName(), 3);
                        }
                    } finally {
                        // Cleanup configurations.
                        UIBotTestUtils.deleteLibertyRunConfigurations(remoteRobot);
                    }
                }
            }
        }
    }

    /**
     * Tests:
     * - Creating a new Liberty tools configuration.
     * - Using project frame toolbar's config selection box and Run icon to select a Liberty configuration and start dev mode.
     */
    @Test
    @Video
    public void testStartWithConfigInRunModeUsingToolbar() {
        String testName = "testStartWithConfigInRunModeUsingToolbar";
        String absoluteWLPPath = Paths.get(getProjectsDirPath(), getSmMPProjectName(), getWLPInstallPath()).toString();

        // Remove all other configurations first.
        UIBotTestUtils.deleteLibertyRunConfigurations(remoteRobot);

        // Add a new Liberty config.
        String configName = "toolBarRun-" + getSmMPProjectName();
        UIBotTestUtils.createLibertyConfiguration(remoteRobot, configName);

        // Find the newly created config in the config selection box on the project frame.
        UIBotTestUtils.selectConfigUsingToolbar(remoteRobot, configName);

        // Click on the debug icon for the selected configuration.
        UIBotTestUtils.runConfigUsingIconOnToolbar(remoteRobot, UIBotTestUtils.ExecMode.RUN);

        try {
            // Validate that the project started.
            TestUtils.validateProjectStarted(testName, getSmMpProjResURI(), getSmMpProjPort(), getSmMPProjOutput(), absoluteWLPPath, false);
        } finally {
            try {
                if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                    UIBotTestUtils.runStopAction(remoteRobot, testName, UIBotTestUtils.ActionExecType.LTWDROPDOWN, absoluteWLPPath, getSmMPProjectName(), 3);
                }
            } finally {
                // Cleanup configurations.
                UIBotTestUtils.deleteLibertyRunConfigurations(remoteRobot);
            }
        }
    }

    /**
     * Tests:
     * - Creating a new Liberty tools configuration.
     * - Using Run->Run... menu options to select the configuration and run in the project in dev mode.
     */
    @Test
    @Video
    public void testStartWithConfigInRunModeUsingMenu() {
        String testName = "testStartWithConfigInRunModeUsingMenu";
        String absoluteWLPPath = Paths.get(getProjectsDirPath(), getSmMPProjectName(), getWLPInstallPath()).toString();

        // Remove all other configurations first.
        UIBotTestUtils.deleteLibertyRunConfigurations(remoteRobot);

        // Add a new Liberty config.
        String configName = "menuRun-" + getSmMPProjectName();
        UIBotTestUtils.createLibertyConfiguration(remoteRobot, configName);

        // Find the newly created config in the config selection box on the project frame.
        UIBotTestUtils.selectConfigUsingMenu(remoteRobot, configName, UIBotTestUtils.ExecMode.RUN);

        try {
            // Validate that the project started.
            TestUtils.validateProjectStarted(testName, getSmMpProjResURI(), getSmMpProjPort(), getSmMPProjOutput(), absoluteWLPPath, false);
        } finally {
            try {
                if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                    UIBotTestUtils.runStopAction(remoteRobot, testName, UIBotTestUtils.ActionExecType.LTWDROPDOWN, absoluteWLPPath, getSmMPProjectName(), 3);
                }
            } finally {
                // Cleanup configurations.
                UIBotTestUtils.deleteLibertyRunConfigurations(remoteRobot);
            }
        }
    }

    /**
     * Tests:
     * - Customized configuration history preservation across multiple configs.
     * - Customized configuration change preservation across multiple configs.
     */
    @Test
    @Video
    public void testMultipleConfigEditHistory() {
        String testName = "testMultipleConfigEditHistory";

        // The path of the project build file expected in the configuration. This path constant for this test.
        String projectBldFilePath = Paths.get(getProjectsDirPath(), getSmMPProjectName(), getBuildFileName()).toString();

        // Remove all other configurations first.
        UIBotTestUtils.deleteLibertyRunConfigurations(remoteRobot);

        // Add two a new Liberty configurations.
        String cfgUID1 = "mCfgHist1";
        String configName1 = cfgUID1 + "-" + getSmMPProjectName();
        String cfgPrjBldPath1 = projectBldFilePath;
        UIBotTestUtils.createLibertyConfiguration(remoteRobot, configName1);
        String cfgUID2 = "mCfgHist2";
        String configName2 = cfgUID2 + "-" + getSmMPProjectName();
        String cfgPrjBldPath2 = projectBldFilePath;
        UIBotTestUtils.createLibertyConfiguration(remoteRobot, configName2);

        try {
            // Find newly created config 1 in the config selection box on the toolbar of the project frame.
            UIBotTestUtils.selectConfigUsingToolbar(remoteRobot, configName1);

            // Validate that selected configuration 1 shows the expected data. The dialog is opened using the start... action.
            // Note that depending on the size of the dialog, the project config file path shown in the Liberty project
            // combo box, may be truncated. Therefore, this check is just a best effort approach to make sure that
            // there is a value in the box, and that it approximates the expected value.
            UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Start...", false, 3);
            Map<String, String> cfgEntries1 = UIBotTestUtils.getOpenedLibertyConfigDataAndCloseOnExit(remoteRobot);
            String activeCfgName1 = cfgEntries1.get(UIBotTestUtils.ConfigEntries.NAME.toString());
            Assertions.assertEquals(configName1, activeCfgName1, "The active config name " + activeCfgName1 + " does not match expected name of " + configName1);
            String activeCfgProjBldPath1 = cfgEntries1.get(UIBotTestUtils.ConfigEntries.LIBERTYPROJ.toString());
            activeCfgProjBldPath1 = (activeCfgProjBldPath1.endsWith("...")) ? activeCfgProjBldPath1.replace("...", "") : activeCfgProjBldPath1;
            Assertions.assertTrue(cfgPrjBldPath1.contains(activeCfgProjBldPath1), "The active config project build file path " + activeCfgProjBldPath1 + " is not contained in expected path of " + cfgPrjBldPath1);
            String activeCfgParams1 = cfgEntries1.get(UIBotTestUtils.ConfigEntries.PARAMS.toString());
            Assertions.assertEquals("", activeCfgParams1, "The active config params " + activeCfgParams1 + " does not match expected params of blank");

            // Find newly created config 2 in the config selection box on the toolbar of the project frame.
            UIBotTestUtils.selectConfigUsingToolbar(remoteRobot, configName2);

            // Validate that selected configuration 2 shows the expected data. The dialog is opened using the start... action.
            UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Start...", false, 3);
            Map<String, String> cfgEntries2 = UIBotTestUtils.getOpenedLibertyConfigDataAndCloseOnExit(remoteRobot);
            String activeCfgName2 = cfgEntries2.get(UIBotTestUtils.ConfigEntries.NAME.toString());
            Assertions.assertEquals(configName2, activeCfgName2, "The active config name " + activeCfgName2 + " does not match expected name of " + configName2);
            String activeCfgProjBldPath2 = cfgEntries2.get(UIBotTestUtils.ConfigEntries.LIBERTYPROJ.toString());
            activeCfgProjBldPath2 = (activeCfgProjBldPath2.endsWith("...")) ? activeCfgProjBldPath2.replace("...", "") : activeCfgProjBldPath2;
            Assertions.assertTrue(cfgPrjBldPath2.contains(activeCfgProjBldPath2), "The active config project build file path " + activeCfgProjBldPath2 + " is not contained expected path of " + cfgPrjBldPath2);
            String activeCfgParams2 = cfgEntries2.get(UIBotTestUtils.ConfigEntries.PARAMS.toString());
            Assertions.assertEquals("", activeCfgParams2, "The active config params " + activeCfgParams2 + " does not match expected params of blank");

            // Edit configuration 1
            UIBotTestUtils.selectConfigUsingToolbar(remoteRobot, configName1);
            String cfgUID11 = "mCfgHist11";
            String newCfgName1 = cfgUID11 + "-" + getSmMPProjectName();
            String newCfgStartParams1 = getStartParams() + " " + cfgUID11;
            String newCfgProjBldPath1 = projectBldFilePath;
            UIBotTestUtils.editLibertyConfigUsingEditConfigDialog(remoteRobot, newCfgName1, newCfgStartParams1);

            // Edit configuration 2
            UIBotTestUtils.selectConfigUsingToolbar(remoteRobot, configName2);
            String cfgUID22 = "mCfgHist22";
            String newCfgName2 = cfgUID22 + "-" + getSmMPProjectName();
            String newCfgProjBldPath2 = projectBldFilePath;
            String newCfgStartParams2 = getStartParams() + " " + cfgUID22;
            UIBotTestUtils.editLibertyConfigUsingEditConfigDialog(remoteRobot, newCfgName2, newCfgStartParams2);

            // Find newly created config 1 in the config selection box on the toolbar of the project frame.
            UIBotTestUtils.selectConfigUsingToolbar(remoteRobot, newCfgName1);

            // Validate that selected configuration 1 shows the expected data. The dialog is opened using the start... action.
            UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Start...", false, 3);
            Map<String, String> newCfgEntries1 = UIBotTestUtils.getOpenedLibertyConfigDataAndCloseOnExit(remoteRobot);
            String newActiveCfgName1 = newCfgEntries1.get(UIBotTestUtils.ConfigEntries.NAME.toString());
            Assertions.assertEquals(newCfgName1, newActiveCfgName1, "The active config name " + newActiveCfgName1 + " does not match expected name of " + newCfgName1);
            String newActiveCfgProjBldPath1 = newCfgEntries1.get(UIBotTestUtils.ConfigEntries.LIBERTYPROJ.toString());
            newActiveCfgProjBldPath1 = (newActiveCfgProjBldPath1.endsWith("...")) ? newActiveCfgProjBldPath1.replace("...", "") : newActiveCfgProjBldPath1;
            Assertions.assertTrue(newCfgProjBldPath1.contains(newActiveCfgProjBldPath1), "The active config project build file path " + newActiveCfgProjBldPath1 + " is not contained in expected path of " + newCfgProjBldPath1);
            String newActiveCfgParams1 = newCfgEntries1.get(UIBotTestUtils.ConfigEntries.PARAMS.toString());
            Assertions.assertEquals(newCfgStartParams1, newActiveCfgParams1, "The active config params " + newActiveCfgParams1 + " does not match expected params of " + newCfgStartParams1);

            // Find newly created config 2 in the config selection box on the toolbar of the project frame.
            UIBotTestUtils.selectConfigUsingToolbar(remoteRobot, newCfgName2);

            // Validate that selected configuration 2 shows the expected data. The dialog is opened using the start... action.
            UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Start...", false, 3);
            Map<String, String> newCfgEntries2 = UIBotTestUtils.getOpenedLibertyConfigDataAndCloseOnExit(remoteRobot);
            String newActiveCfgName2 = newCfgEntries2.get(UIBotTestUtils.ConfigEntries.NAME.toString());
            Assertions.assertEquals(newCfgName2, newActiveCfgName2, "The active config name " + newActiveCfgName2 + " does not match expected name of " + newCfgName2);
            String newActiveCfgProjBldPath2 = newCfgEntries2.get(UIBotTestUtils.ConfigEntries.LIBERTYPROJ.toString());
            newActiveCfgProjBldPath2 = (newActiveCfgProjBldPath2.endsWith("...")) ? newActiveCfgProjBldPath2.replace("...", "") : newActiveCfgProjBldPath2;
            Assertions.assertTrue(newCfgProjBldPath2.contains(newActiveCfgProjBldPath2), "The active config project build file path " + newActiveCfgProjBldPath2 + " does not contained in expected path of " + newCfgProjBldPath2);
            String newActiveCfgParams2 = newCfgEntries2.get(UIBotTestUtils.ConfigEntries.PARAMS.toString());
            Assertions.assertEquals(newCfgStartParams2, newActiveCfgParams2, "The active config params " + newActiveCfgParams2 + " does not match expected params of " + newCfgStartParams2);
        } finally {
            // clean up the created configurations.
            UIBotTestUtils.deleteLibertyRunConfigurations(remoteRobot);
        }
    }

    /**
     * Tests dashboard startInContainer/stop actions run from the project's drop-down action menu.
     * Notes:
     * 1. This test is restricted to Linux only because, on other platforms, the docker build process
     * driven by the Liberty Maven/Gradle plugins take longer than ten minutes. Ten minutes is the
     * timeout set by the plugins and there is currently no way to extend this timeout through the
     * Liberty Tools plugin(i.e. set dockerBuildTimeout).
     */
    @Test
    @Video
    @EnabledOnOs({OS.LINUX})
    public void testStartInContainerActionUsingDropDownMenu() {
        String testName = "testStartInContainerActionUsingDropDownMenu";
        String absoluteWLPPath = Paths.get(getProjectsDirPath(), getSmMPProjectName(), getWLPInstallPath()).toString();

        // Start dev mode in a container.
        UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Start in container", false, 3);
        try {
            // Validate that the project started.
            TestUtils.validateProjectStarted(testName, getSmMpProjResURI(), getSmMpProjPort(), getSmMPProjOutput(), absoluteWLPPath, true);
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Stop dev mode.
                UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Stop", false, 3);

                // Validate that the server stopped.
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
            }
        }
    }

    /**
     * Tests dashboard startInContainer/stop actions run from the project's drop-down action menu
     * and the Liberty tool window toolbar play button.
     * Notes:
     * 1. This test is restricted to Linux only because, on other platforms, the docker build process
     * driven by the Liberty Maven/Gradle plugins take longer than ten minutes. Ten minutes is the
     * timeout set by the plugins and there is currently no way to extend this timeout through the
     * Liberty Tools plugin(i.e. set dockerBuildTimeout).
     */
    @Test
    @Video
    @EnabledOnOs({OS.LINUX})
    public void testStartInContainerActionUsingPlayToolbarButton() {
        String testName = "testStartInContainerActionUsingPlayToolbarButton";
        String absoluteWLPPath = Paths.get(getProjectsDirPath(), getSmMPProjectName(), getBuildFileName()).toString();

        // Start dev mode in a container.
        UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Start in container", true, 3);
        try {
            // Validate that the project started.
            TestUtils.validateProjectStarted(testName, getSmMpProjResURI(), getSmMpProjPort(), getSmMPProjOutput(), absoluteWLPPath, true);
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Stop dev mode.
                UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Stop", true, 3);

                // Validate that the server stopped.
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
            }
        }
    }

    /**
     * Tests dashboard startInContainer/stop actions run from the project's pop-up action menu.
     * Notes:
     * 1. This test is restricted to Linux only because, on other platforms, the docker build process
     * driven by the Liberty Maven/Gradle plugins take longer than ten minutes. Ten minutes is the
     * timeout set by the plugins and there is currently no way to extend this timeout through the
     * Liberty Tools plugin(i.e. set dockerBuildTimeout).
     */
    @Test
    @Video
    @EnabledOnOs({OS.LINUX})
    public void testStartInContainerActionUsingPopUpMenu() {
        String testName = "testStartInContainerActionUsingPopUpMenu";
        String absoluteWLPPath = Paths.get(getProjectsDirPath(), getSmMPProjectName(), getWLPInstallPath()).toString();

        // Start dev mode in a container.
        UIBotTestUtils.runActionLTWPopupMenu(remoteRobot, getSmMPProjectName(), "Liberty: Start in container", 3);

        try {
            // Validate that the project started.
            TestUtils.validateProjectStarted(testName, getSmMpProjResURI(), getSmMpProjPort(), getSmMPProjOutput(), absoluteWLPPath, true);
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Stop dev mode.
                UIBotTestUtils.runActionLTWPopupMenu(remoteRobot, getSmMPProjectName(), "Liberty: Stop", 3);

                // Validate that the server stopped.
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
            }
        }
    }

    /**
     * Tests dashboard startInContainer/stop actions run from the search everywhere panel.
     * Notes:
     * 1. This test is restricted to Linux only because, on other platforms, the docker build process
     * driven by the Liberty Maven/Gradle plugins take longer than ten minutes. Ten minutes is the
     * timeout set by the plugins and there is currently no way to extend this timeout through the
     * Liberty Tools plugin(i.e. set dockerBuildTimeout).
     */
    @Test
    @Video
    @EnabledOnOs({OS.LINUX})
    public void testStartInContainerActionUsingSearch() {
        String testName = "testStartInContainerActionUsingSearch";
        String absoluteWLPPath = Paths.get(getProjectsDirPath(), getSmMPProjectName(), getWLPInstallPath()).toString();

        // Start dev mode in a container.
        UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Start in container", 3);

        try {
            // Validate that the project started.
            TestUtils.validateProjectStarted(testName, getSmMpProjResURI(), getSmMpProjPort(), getSmMPProjOutput(), absoluteWLPPath, true);
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Stop dev mode.
                UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Stop", 3);

                // Validate that the server stopped.
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
            }
        }
    }

    /**
     * Prepares the environment to run the tests.
     *
     * @param projectPath The path of the project.
     * @param projectName The name of the project being used.
     */
    public static void prepareEnv(String projectPath, String projectName) {
        TestUtils.printTrace(TestUtils.TraceSevLevel.INFO,
                "prepareEnv. Entry. ProjectPath: " + projectPath + ". ProjectName: " + projectName);
        waitForIgnoringError(Duration.ofMinutes(4), Duration.ofSeconds(5), "Wait for IDE to start", "IDE did not start", () -> remoteRobot.callJs("true"));
        remoteRobot.find(WelcomeFrameFixture.class, Duration.ofMinutes(2));
        UIBotTestUtils.importProject(remoteRobot, projectPath, projectName);
        UIBotTestUtils.openProjectView(remoteRobot);
        UIBotTestUtils.openLibertyToolWindow(remoteRobot);
        UIBotTestUtils.validateImportedProjectShowsInLTW(remoteRobot, projectName);
        UIBotTestUtils.expandLibertyToolWindowProjectTree(remoteRobot, projectName);

        // Close all open editors.
        // The expansion of the project tree in the Liberty tool window causes the editor tab for
        // the project's build file to open. That is the result of clicking on the project to give it
        // focus. The action of clicking on the project causes the build file to be opened automatically.
        // Closing the build file editor here prevents it from opening automatically when the project
        // in the Liberty tool window is clicked or right-clicked again. This is done on purpose to
        // prevent false negative tests related to the build file editor tab.
        UIBotTestUtils.closeAllEditorTabs(remoteRobot);

        TestUtils.printTrace(TestUtils.TraceSevLevel.INFO,
                "prepareEnv. Exit. ProjectName: " + projectName);
    }

    /**
     * Returns the projects directory path.
     *
     * @return The projects directory path.
     */
    public abstract String getProjectsDirPath();

    /**
     * Returns the name of the single module MicroProfile project.
     *
     * @return The name of the single module MicroProfile project.
     */
    public abstract String getSmMPProjectName();

    /**
     * Returns the expected HTTP response payload associated with the single module
     * MicroProfile project.
     *
     * @return The expected HTTP response payload associated with the single module
     * MicroProfile project.
     */
    public abstract String getSmMPProjOutput();

    /**
     * Returns the port number associated with the single module MicroProfile project.
     *
     * @return The port number associated with the single module MicroProfile project.
     */
    public abstract int getSmMpProjPort();

    /**
     * Return the Resource URI associated with the single module MicroProfile project.
     *
     * @return The Resource URI associated with the single module MicroProfile project.
     */
    public abstract String getSmMpProjResURI();

    /**
     * Returns the path where the Liberty server was installed.
     *
     * @return The path where the Liberty server was installed.
     */
    public abstract String getWLPInstallPath();

    /**
     * Returns the name of the build file used by the project.
     *
     * @return The name of the build file used by the project.
     */
    public abstract String getBuildFileName();

    /**
     * Returns the name of the custom action command used to open the build file.
     *
     * @return The name of the custom action command used to open the build file.
     */
    public abstract String getBuildFileOpenCommand();

    /**
     * Returns the custom start parameters to be used to start dev mode.
     *
     * @return The custom start parameters to be used to start dev mode.
     */
    public abstract String getStartParams();

    /**
     * Deletes test reports.
     */
    public abstract void deleteTestReports();

    /**
     * Validates that test reports were generated.
     */
    public abstract void validateTestReportsExist();
}