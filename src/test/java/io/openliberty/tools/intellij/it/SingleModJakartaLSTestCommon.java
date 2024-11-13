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
import com.intellij.remoterobot.fixtures.JTreeFixture;
import io.openliberty.tools.intellij.it.fixtures.ProjectFrameFixture;
import io.openliberty.tools.intellij.it.fixtures.WelcomeFrameFixture;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Scanner;

import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitForIgnoringError;

public abstract class SingleModJakartaLSTestCommon {
    public static final String REMOTEBOT_URL = "http://localhost:8082";
    public static final RemoteRobot remoteRobot = new RemoteRobot(REMOTEBOT_URL);

    String projectName;
    String projectsPath;


    public SingleModJakartaLSTestCommon(String projectName, String projectsPath) {
        this.projectName = projectName;
        this.projectsPath = projectsPath;
    }

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
        TestUtils.detectFatalError();
    }

    /**
     * Cleanup.
     */
    @AfterAll
    public static void cleanup() {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofMinutes(2));

        UIBotTestUtils.closeFileEditorTab(remoteRobot, "SystemResource.java", "5");
        UIBotTestUtils.closeFileEditorTab(remoteRobot, "SystemResource2.java", "5");

        UIBotTestUtils.closeProjectView(remoteRobot);
        UIBotTestUtils.closeProjectFrame(remoteRobot);
        UIBotTestUtils.validateProjectFrameClosed(remoteRobot);
    }

    /**
     * Tests Jakarta Language Server code snippet support in a Java source file
     */
    @Test
    @Video
    public void testInsertJakartaCodeSnippetIntoJavaPart() {
        String snippetStr = "res";
        String snippetChooser = "class";
        String insertedCode = "public String methodname() {";

        // get focus on file tab prior to copy
        UIBotTestUtils.clickOnFileTab(remoteRobot, "SystemResource.java");

        // Save the current content.
        UIBotTestUtils.copyWindowContent(remoteRobot);

        // Delete the current src code
        UIBotTestUtils.clearWindowContent(remoteRobot);

        // Insert a code snippet into java part
        try {
            UIBotTestUtils.insertCodeSnippetIntoSourceFile(remoteRobot, "SystemResource.java", snippetStr, snippetChooser);
            Path pathToSrc = Paths.get(projectsPath, projectName, "src", "main", "java", "io", "openliberty", "mp", "sample", "system", "SystemResource.java");
            TestUtils.validateCodeInJavaSrc(pathToSrc.toString(), insertedCode);
        }
        finally {
            UIBotTestUtils.pasteOnActiveWindow(remoteRobot, true);
        }
    }

    /**
     * Tests Jakarta Language Server diagnostic support in a Java source file
     */
    @Test
    @Video
    public void testJakartaDiagnosticsInJavaPart() {
        String publicString = "public Response getProperties() {";
        String privateString = "private Response getProperties() {";
        String flaggedString = "getProperties";
        String expectedHoverData = "Only public methods can be exposed as resource methods";
        Path pathToSrc = Paths.get(projectsPath, projectName, "src", "main", "java", "io", "openliberty", "mp", "sample", "system", "SystemResource2.java");

        // get focus on file tab prior to copy
        UIBotTestUtils.clickOnFileTab(remoteRobot, "SystemResource2.java");

        // Modify the method signature
        UIBotTestUtils.selectAndModifyTextInJavaPart(remoteRobot, "SystemResource2.java", publicString, privateString);

        try {
            // validate the method signature is no longer set to public
            TestUtils.validateStringNotInFile(pathToSrc.toString(), publicString);

            //there should be a diagnostic for "private" on method signature - move cursor to hover point
            UIBotTestUtils.hoverInAppServerCfgFile(remoteRobot, flaggedString, "SystemResource2.java", UIBotTestUtils.PopupType.DIAGNOSTIC);

            String foundHoverData = UIBotTestUtils.getHoverStringData(remoteRobot, UIBotTestUtils.PopupType.DIAGNOSTIC);
            TestUtils.validateHoverData(expectedHoverData, foundHoverData);
            UIBotTestUtils.clickOnFileTab(remoteRobot, "SystemResource2.java");

        } finally {
            // Replace modified content with the original content
            UIBotTestUtils.selectAndModifyTextInJavaPart(remoteRobot, "SystemResource2.java", privateString, publicString);
        }
    }

    /**
     * Tests Jakarta Language Server quick fix support in a Java source file
     */
//    @Test
//    @Video
    public void testJakartaQuickFixInJavaPart() {
        String publicString = "public Response getProperties() {";
        String privateString = "private Response getProperties() {";
        String flaggedString = "getProperties";

        Path pathToSrc = Paths.get(projectsPath, projectName, "src", "main", "java", "io", "openliberty", "mp", "sample", "system", "SystemResource2.java");
        String quickfixChooserString = "Make method public";

        // get focus on file tab prior to copy
        UIBotTestUtils.clickOnFileTab(remoteRobot, "SystemResource2.java");

        // Save the current content.
        UIBotTestUtils.copyWindowContent(remoteRobot);

        // Modify the method signature
        UIBotTestUtils.selectAndModifyTextInJavaPart(remoteRobot, "SystemResource2.java", publicString, privateString);

        try {
            // validate public signature no longer found in java part
            TestUtils.validateStringNotInFile(pathToSrc.toString(), publicString);

            //there should be a diagnostic - move cursor to hover point
            UIBotTestUtils.hoverForQuickFixInAppFile(remoteRobot, flaggedString, "SystemResource2.java", quickfixChooserString);

            // trigger and use the quickfix popup attached to the diagnostic
            UIBotTestUtils.chooseQuickFix(remoteRobot, quickfixChooserString);

            TestUtils.validateCodeInJavaSrc(pathToSrc.toString(), publicString);
        }
        finally {
            // Replace modified content with the original content
            UIBotTestUtils.pasteOnActiveWindow(remoteRobot);
        }
    }

    /**
     * Prepares the environment to run the tests.
     *
     * @param projectPath The path of the project.
     * @param projectName The name of the project being used.
     */

    public static void prepareEnv(String projectPath, String projectName) {
        waitForIgnoringError(Duration.ofMinutes(4), Duration.ofSeconds(5), "Wait for IDE to start", "IDE did not start", () -> remoteRobot.callJs("true"));
        remoteRobot.find(WelcomeFrameFixture.class, Duration.ofMinutes(2));

        UIBotTestUtils.importProject(remoteRobot, projectPath, projectName);
        UIBotTestUtils.openProjectView(remoteRobot);
        // IntelliJ does not start building and indexing until the Project View is open
        UIBotTestUtils.waitForIndexing(remoteRobot);
        UIBotTestUtils.openAndValidateLibertyToolWindow(remoteRobot, projectName);
        UIBotTestUtils.closeLibertyToolWindow(remoteRobot);

        // pre-open project tree before attempting to open files needed by testcases
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofMinutes(2));
        JTreeFixture projTree = projectFrame.getProjectViewJTree(projectName);

        // expand project directories that are specific to this test app being used by these testcases
        // must be expanded here before trying to open specific files
        projTree.expand(projectName, "src", "main", "java", "io.openliberty.mp.sample", "system");

        UIBotTestUtils.openFile(remoteRobot, projectName, "SystemResource", projectName, "src", "main", "java", "io.openliberty.mp.sample", "system");
        UIBotTestUtils.openFile(remoteRobot, projectName, "SystemResource2", projectName, "src", "main", "java", "io.openliberty.mp.sample", "system");


        // Removes the build tool window if it is opened. This prevents text to be hidden by it.
        UIBotTestUtils.removeToolWindow(remoteRobot, "Build:");
    }
}

