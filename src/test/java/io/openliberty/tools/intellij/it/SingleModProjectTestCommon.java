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

import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;

import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitForIgnoringError;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Common component for running both Maven and Gradle tests.
 */
public abstract class SingleModProjectTestCommon {

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
        UIBotTestUtils.runActionLTWPopupMenu(remoteRobot, getSmMPProjectName(), getBuildFileOpenCommand());

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
        UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Start...", false);

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
                TestUtils.sleepAndIgnoreException(30);

                // Stop Liberty dev mode and validates that the Liberty server is down.
                UIBotTestUtils.runStopAction(remoteRobot, testName, UIBotTestUtils.ActionExecType.LTWDROPDOWN, absoluteWLPPath, getSmMPProjectName(), 3);
            }
        }

        // Validate that the start with params action brings up the configuration previously used.
        try {
            UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Start...", false);
            Map<String, String> cfgEntries = UIBotTestUtils.getLibertyConfigEntries(remoteRobot);
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
        UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Start", false);

        // Validate that the project started.
        TestUtils.validateProjectStarted(testName, getSmMpProjResURI(), getSmMpProjPort(), getSmMPProjOutput(), absoluteWLPPath, false);

        try {
            // Run the project's tests.
            UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Run tests", false);

            // Validate that the report was generated.
            validateTestReportsExist();
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
     * Tests the Add/Remove project from the tool window actions run from the search everywhere panel .
     */
    @Test
    @Video
    public void testManualProjectAddRemoveActionUsingSearch() {
        // Import the project that is not automatically detected by Liberty Tools.
        UIBotTestUtils.importProject(remoteRobot, getProjectsDirPath(), getSmNLTRestProjectName());

        // Open the dashboard and wait for the project to complete indexing.
        UIBotTestUtils.openLibertyToolWindow(remoteRobot);

        // Validate that the project tree is showing. Note that indexing may start at any time
        // and automatically remove the existing content from the window (project/message/etc.).
        TestUtils.sleepAndIgnoreException(10);
        UIBotTestUtils.waitForLTWNoProjectDetectedMsg(remoteRobot, "300");

        try {
            // Add the project to the Liberty tool window.
            UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Add project to the tool window");

            // Select project from the 'Add Liberty project' dialog.
            UIBotTestUtils.selectProjectFromAddLibertyProjectDialog(remoteRobot, getSmNLTRestProjectName());

            try {
                // Validate that the project is displayed in the Liberty tool window.
                UIBotTestUtils.findProjectInLibertyToolWindow(remoteRobot, getSmNLTRestProjectName(), "10");
            } finally {
                // Remove the project from the Liberty tool window.
                UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Remove project from the tool window");

                // Select project from the 'Remote Liberty project' dialog.
                UIBotTestUtils.selectProjectFromRemoveLibertyProjectDialog(remoteRobot, getSmNLTRestProjectName());

                // Answer the 'Remove Liberty project' dialog in the affirmative.
                UIBotTestUtils.respondToRemoveProjectQueryDialog(remoteRobot);

                // Validate that the project was removed. An exception is expected.
                try {
                    for (int i = 0; i < 5; i++) {
                        UIBotTestUtils.findProjectInLibertyToolWindow(remoteRobot, getSmNLTRestProjectName(), "2");
                        try {
                            Thread.sleep(2000);
                        } catch (Exception ee) {
                            // Nothing to do. Continue to the next iteration.
                        }
                    }

                    fail("Project " + getSmNLTRestProjectName() + " is still present in the Liberty tool window despite it being removed.");
                } catch (Exception e) {
                    // The project was not found. Success.
                }
            }
        } finally {
            // Import the original project.
            Exception error = null;
            int maxRetries = 3;
            for (int i = 0; i < maxRetries; i++) {
                error = null;
                try {
                    UIBotTestUtils.importProject(remoteRobot, getProjectsDirPath(), getSmMPProjectName());
                    UIBotTestUtils.openLibertyToolWindow(remoteRobot);
                    UIBotTestUtils.expandLibertyToolWindowProjectTree(remoteRobot, getSmMPProjectName());
                    break;
                } catch (Exception e) {
                    // There are instances in which the import actions are successful, but the
                    // project view never comes up. Instead, the welcome view is shown. If that is the
                    // case retry.
                    error = e;
                }
            }

            if (error != null) {
                fail("Unable to open project " + getSmMPProjectName(), error);
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
    @Disabled("Until this issue is resolved: https://github.com/OpenLiberty/liberty-tools-intellij/issues/348")
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

            // Open the terminal window.
            UIBotTestUtils.openTerminalWindow(remoteRobot);
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
     * - Using Run->Debug... menu options to select the configuration and run in the project in dev mode.
     */
    @Test
    @Video
    @Disabled("Until this issue is resolved: https://github.com/OpenLiberty/liberty-tools-intellij/issues/348")
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

            // Open the terminal window.
            UIBotTestUtils.openTerminalWindow(remoteRobot);
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
        UIBotTestUtils.validateLibertyTWProjectTreeItemIsShowing(remoteRobot, projectName);
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
     * Returns the name of the single module REST project that does not meet
     * the requirements needed to automatically show in the Liberty tool window.
     * This project's Liberty config file does not have the expected default name,
     * and the build file does not have any Liberty plugin related entries.
     *
     * @return The name of the single module REST project that does not meet the
     * requirements needed to automatically show in the Liberty tool window.
     */
    public abstract String getSmNLTRestProjectName();

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