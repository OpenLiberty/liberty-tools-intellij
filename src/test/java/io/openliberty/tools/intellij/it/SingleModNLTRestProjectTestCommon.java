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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitForIgnoringError;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Holds common tests that use a single module non Liberty Tools compliant REST project.
 */
public abstract class SingleModNLTRestProjectTestCommon {

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
     * Tests manually Adding/Removing project from the tool window using the Liberty add/remove
     * options available through search everywhere panel.
     */
    @Test
    @Video
    public void testManualProjectAddRemoveActionUsingSearch() {
        // Validate that the Liberty tool window project tree is not showing. No projects are expected.
        UIBotTestUtils.waitForLTWNoProjectDetectedMsg(remoteRobot, 10);

        // Add the project to the Liberty tool window.
        UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Add project to the tool window", 3);

        // Select project from the 'Add Liberty project' dialog.
        UIBotTestUtils.selectProjectFromAddLibertyProjectDialog(remoteRobot, getSmNLTRestProjectName());

        try {
            // Validate that the project is displayed in the Liberty tool window.
            UIBotTestUtils.findProjectInLibertyToolWindow(remoteRobot, getSmNLTRestProjectName(), "10");
        } finally {
            // Remove the project from the Liberty tool window.
            UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Remove project from the tool window", 3);

            // Select project from the 'Remote Liberty project' dialog.
            UIBotTestUtils.selectProjectFromRemoveLibertyProjectDialog(remoteRobot, getSmNLTRestProjectName());

            // Answer the 'Remove Liberty project' dialog in the affirmative.
            UIBotTestUtils.respondToRemoveProjectQueryDialog(remoteRobot);

            // Refresh the Liberty tool window using the refresh icon on the toolbar.
            UIBotTestUtils.refreshLibertyToolWindow(remoteRobot);

            // Validate that the Liberty tool window project tree is not showing. No projects are expected.
            UIBotTestUtils.waitForLTWNoProjectDetectedMsg(remoteRobot, 10);
        }
    }

    /**
     * Tests:
     * - Refresh button on Liberty tool window toolbar.
     * - Detecting a project with a src/main/liberty/config/server.xml file only.
     */
    @Test
    @Video
    public void testsRefreshProjectWithServerXmlOnly() {
        // Validate that the Liberty tool window project tree is not showing. No projects are expected.
        UIBotTestUtils.waitForLTWNoProjectDetectedMsg(remoteRobot, 10);

        // Copy a valid server.xml file to this project's src/main/liberty/config directory.
        Path validServerXml = Paths.get(getHelperFilesDirPath(), "server.xml");
        Path destination = Paths.get(getProjectsDirPath(), getSmNLTRestProjectName(), "src", "main", "liberty", "config", "server.xml");

        try {
            Files.copy(validServerXml, destination, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            fail("Unable to copy " + validServerXml + " to " + destination + ".", e);
        }

        // Refresh the Liberty tool window using the refresh icon on the toolbar.
        UIBotTestUtils.refreshLibertyToolWindow(remoteRobot);

        // Validate that the project is displayed in the Liberty tool window.
        try {
            UIBotTestUtils.findProjectInLibertyToolWindow(remoteRobot, getSmNLTRestProjectName(), "10");
        } finally {
            // Remove the previously added server.xml file.
            if (!TestUtils.deleteFile(destination)) {
                fail("Unable to delete " + destination);
            }

            // Refresh the Liberty tool window using the refresh icon on the toolbar.
            UIBotTestUtils.refreshLibertyToolWindow(remoteRobot);

            // Validate that the Liberty tool window project tree is not showing. No projects are expected.
            UIBotTestUtils.waitForLTWNoProjectDetectedMsg(remoteRobot, 10);
        }
    }

    /**
     * Tests:
     * - Refresh button on Liberty tool window toolbar.
     * - Detecting a project with a valid Liberty M/G plugin configuration in build file only.
     * The build file in this case uses a buildscript block to customize the version and the
     * location of the Liberty Tools binary dependency.
     */
    @Test
    @Video
    public void testsRefreshProjectWithLTBuildCfgOnlyWithBldScriptBlock() {
        testsRefreshProjectWithLTBuildCfgOnly(getBuildFileName());
    }

    /**
     * Tests:
     * - Refresh button on Liberty tool window toolbar.
     * - Detecting a project with a valid Liberty M/G plugin configuration in build file only.
     */
    public void testsRefreshProjectWithLTBuildCfgOnly(String buildFile) {
        // Validate that the Liberty tool window project tree is not showing. No projects are expected.
        UIBotTestUtils.waitForLTWNoProjectDetectedMsg(remoteRobot, 10);

        // Replace the current build file with the file containing Liberty plugin config.
        Path newCfg = Paths.get(getHelperFilesDirPath(), buildFile);
        Path originalCfg = Paths.get(getProjectsDirPath(), getSmNLTRestProjectName(), getBuildFileName());
        Path backupCfg = Paths.get(getProjectsDirPath(), getSmNLTRestProjectName(), getBuildFileName() + ".bak");
        try {
            Files.copy(originalCfg, backupCfg, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            fail("Unable to backup " + originalCfg + " to " + backupCfg + ".", e);
        }

        try {
            Files.copy(newCfg, originalCfg, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            fail("Unable to copy " + newCfg + " to " + originalCfg + ".", e);
        }

        // User the refresh icon on the Liberty tool window.
        UIBotTestUtils.refreshLibertyToolWindow(remoteRobot);

        try {
            // Validate that the project is displayed in the Liberty tool window.
            UIBotTestUtils.findProjectInLibertyToolWindow(remoteRobot, getSmNLTRestProjectName(), "10");
        } finally {
            // Replace the current build file with the backup file.
            try {
                Files.copy(backupCfg, originalCfg, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                fail("Unable to copy " + backupCfg + " to " + originalCfg + ".", e);
            }

            // Delete the backup file.
            if (!TestUtils.deleteFile(backupCfg)) {
                fail("Unable to delete " + backupCfg);
            }

            // Refresh the Liberty tool window using the refresh icon on the toolbar.
            UIBotTestUtils.refreshLibertyToolWindow(remoteRobot);

            // Validate that the Liberty tool window project tree is not showing. No projects are expected.
            UIBotTestUtils.waitForLTWNoProjectDetectedMsg(remoteRobot, 10);
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
        UIBotTestUtils.waitForIndexing(remoteRobot);
        UIBotTestUtils.openProjectView(remoteRobot);
        UIBotTestUtils.openLibertyToolWindow(remoteRobot);

        // Wait for indexing to complete. Once indexing completes and Liberty Tools to take control
        // of the window, it will display the project it detected, or a message stating that no
        // projects were detected. A message stating that no projects were detected is what is expected here.
        UIBotTestUtils.waitForLTWNoProjectDetectedMsg(remoteRobot, 300);

        TestUtils.printTrace(TestUtils.TraceSevLevel.INFO,
                "prepareEnv. Exit. ProjectName: " + projectName);
    }

    /**
     * Returns the directory path containing helper files.
     *
     * @return The directory path containing helper files.
     */
    public abstract String getHelperFilesDirPath();

    /**
     * Returns the projects directory path.
     *
     * @return The projects directory path.
     */
    public abstract String getProjectsDirPath();

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
     * Returns the name of the build file used by the project.
     *
     * @return The name of the build file used by the project.
     */
    public abstract String getBuildFileName();
}
