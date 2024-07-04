/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation.
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

import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitForIgnoringError;

/**
 * Holds common tests that use a single module MicroProfile project.
 */
public abstract class SingleModMPSIDProjectTestCommon {

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
     * Tests dashboard start/stop actions run from the project's drop-down action menu.
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

        try {
            // Validate that the application started.
            TestUtils.validateProjectStarted(testName, getSmMpProjResURI(), getSmMpProjPort(), getSmMPProjOutput(), absoluteWLPPath, false);

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
     * Deletes test reports.
     */
    public abstract void deleteTestReports();

}