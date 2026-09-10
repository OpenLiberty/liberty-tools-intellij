/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation.
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
import org.junit.jupiter.api.*;

import java.io.File;
import java.time.Duration;

import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitForIgnoringError;

/**
 * Holds common tests that use a single module MicroProfile project.
 */
public abstract class SingleModMPProjectCfgTestCommon extends BaseOSUtilities {

    // In this test case the environment has been set up so that there is a new project
    // that has not been used in a previous execution of IntelliJ. Also, the Liberty explorer
    // or dashboard has not been opened as it is in all other tests. This means the LibertyModules
    // object is not yet populated.
    // When we create a new Run/Debug configuration, the "Liberty project" field is populated by
    // default with one of the build files from one of the Liberty projects in the workspace. If
    // there is no default build file then there will be a Null Pointer Exception if we press Run.

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
     * Single module Microprofile project name.
     */
    private String smMpProjectName = null;

    /**
     * The path to the folder containing the test projects.
     */
    private String projectsPath = null;

    /**
     * Relative location of the WLP installation.
     */
    private String wlpInstallPath = null;

    /**
     * Returns the path where the Liberty server was installed.
     *
     * @return The path where the Liberty server was installed.
     */
    public String getWLPInstallPath() {
        return wlpInstallPath;
    }
    public void setWLPInstallPath(String path) {
        wlpInstallPath = path;
    }

    /**
     * Returns the projects directory path.
     *
     * @return The projects directory path.
     */
    public String getProjectsDirPath() {
        return projectsPath;
    }
    public void setProjectsDirPath(String path) {
        projectsPath = path;
    }

    /**
     * Returns the name of the single module MicroProfile project.
     *
     * @return The name of the single module MicroProfile project.
     */
    public String getSmMPProjectName() {
        return smMpProjectName;
    }
    public void setSmMPProjectName(String name) {
        smMpProjectName = name;
    }

    /**
     * Processes actions before each test.
     *
     * @param info Test information.
     */
    @BeforeEach
    public void beforeEach(TestInfo info) {
        TestUtils.printTrace(TestUtils.TraceSevLevel.INFO, this.getClass().getSimpleName() + "." + info.getDisplayName() + ". Entry");
        // IntelliJ does not start building and indexing until the Project View is open
        UIBotTestUtils.waitForIndexing(remoteRobot);
    }

    /**
     * Processes actions after each test.
     *
     * @param info Test information.
     */
    @AfterEach
    public void afterEach(TestInfo info) {
        TestUtils.printTrace(TestUtils.TraceSevLevel.INFO, this.getClass().getSimpleName() + "." + info.getDisplayName() + ". Exit");
        TestUtils.detectFatalError();
    }

    /**
     * Cleanup.
     */
    @AfterAll
    public static void cleanup() {
        closeProjectView();
    }

    /**
     * Close project.
     */
    protected static void closeProjectView() {
        if (!remoteRobot.isMac()) {
            UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Close All Tabs", 3);
            UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Compact Mode", 3);
        }
        UIBotTestUtils.closeLibertyToolWindow(remoteRobot);
        UIBotTestUtils.closeProjectView(remoteRobot);
        UIBotTestUtils.closeProjectFrame(remoteRobot);
        UIBotTestUtils.validateProjectFrameClosed(remoteRobot);
    }

    /**
     * Create a run configuration and see if it caused a null pointer exception
     */
    @Test
    @Video
    public void testCreateRunConfigAction() {
        String testName = "testCreateRunConfigAction";
        // Remove all other configurations first.
        UIBotTestUtils.deleteLibertyRunConfigurations(remoteRobot);

        // Add a new Liberty configurations. Throws an exception if there is an error.
        // Note: the method will throw a NoSuchElementException if there are no Liberty projects
        // detected when the Edit Liberty Run/Debug Configuration dialog is opened.
        UIBotTestUtils.createLibertyConfiguration(remoteRobot, "newCfg1", false, null);
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
        UIBotTestUtils.findWelcomeFrame(remoteRobot);
        UIBotTestUtils.importProject(remoteRobot, projectPath, projectName);
        UIBotTestUtils.openProjectView(remoteRobot);
        if (!remoteRobot.isMac()) {
            UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Compact Mode", 3);
        }
        // IntelliJ does not start building and indexing until the Project View is open
        UIBotTestUtils.waitForIndexing(remoteRobot);
        TestUtils.printTrace(TestUtils.TraceSevLevel.INFO,
                "prepareEnv. Exit. ProjectName: " + projectName);
    }

    /**
     * Deletes the directory specified by dirPath if it exists.
     *
     * @param dirPath The path to the directory that may be deleted.
     */
    public static void deleteDirectoryIfExists(String dirPath) {
        File dir = new File(dirPath);
        if (dir.exists()) {
            TestUtils.deleteDirectory(dir);
        }
    }
}