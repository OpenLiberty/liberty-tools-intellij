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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitForIgnoringError;

public abstract class SingleModLibertyLSTestCommon {
    public static final String REMOTEBOT_URL = "http://localhost:8082";
    public static final RemoteRobot remoteRobot = new RemoteRobot(REMOTEBOT_URL);

    String projectName;
    String projectsPath;


    public SingleModLibertyLSTestCommon(String projectName, String projectsPath) {
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
        UIBotTestUtils.closeFileEditorTab(remoteRobot, "server.xml", "5");
        UIBotTestUtils.closeFileEditorTab(remoteRobot, "server.env", "5");
        UIBotTestUtils.closeFileEditorTab(remoteRobot, "bootstrap.properties", "5");
        UIBotTestUtils.closeProjectView(remoteRobot);
        UIBotTestUtils.closeProjectFrame(remoteRobot);
        UIBotTestUtils.validateProjectFrameClosed(remoteRobot);
    }

    /**
     * Tests Liberty Lemminx Extension Hover support in server.xml for a
     * Liberty Server Feature
     */
    @Test
    @Video
    public void testServerXMLFeatureHover() {
        String testHoverTarget = "mpHealth-4.0";
        String hoverExpectedOutcome = "This feature provides support for the MicroProfile Health specification.";

        //mover cursor to hover point
        UIBotTestUtils.hoverInAppServerCfgFile(remoteRobot, testHoverTarget, "server.xml", UIBotTestUtils.PopupType.DOCUMENTATION);
        String hoverFoundOutcome = UIBotTestUtils.getHoverStringData(remoteRobot, UIBotTestUtils.PopupType.DOCUMENTATION);

        // Validate that the hover action raised the expected hint text
        TestUtils.validateHoverData(hoverExpectedOutcome, hoverFoundOutcome);
    }

    /**
     * Tests Liberty Lemminx Extension Hover support in server.xml for a
     * Liberty Server Attribute
     */
    @Test
    @Video
    public void testServerXMLNonFeatureHover() {
        String testHoverTarget = "httpEndpoint";
        String hoverExpectedOutcome = "Configuration properties for an HTTP endpoint.";

        //mover cursor to hover point
        UIBotTestUtils.hoverInAppServerCfgFile(remoteRobot, testHoverTarget, "server.xml", UIBotTestUtils.PopupType.DOCUMENTATION);
        String hoverFoundOutcome = UIBotTestUtils.getHoverStringData(remoteRobot, UIBotTestUtils.PopupType.DOCUMENTATION);

        // Validate that the hover action raised the expected hint text
        TestUtils.validateHoverData(hoverExpectedOutcome, hoverFoundOutcome);
    }

    /**
     * Tests Liberty Lemminx Extension type ahead support in server.xml for a
     * Liberty Server Feature
     */
    @Test
    @Video
    public void testInsertFeatureIntoServerXML() {
        String stanzaSnippet = "el-3";
        String insertedFeature = "<feature>el-3.0</feature>";

        // get focus on server.xml tab prior to copy
        UIBotTestUtils.clickOnFileTab(remoteRobot, "server.xml");

        // Save the current server.xml content.
        UIBotTestUtils.copyWindowContent(remoteRobot);

        // Insert a new element in server.xml.
        try {
            UIBotTestUtils.insertStanzaInAppServerXML(remoteRobot, stanzaSnippet, 18, 40, UIBotTestUtils.InsertionType.FEATURE, true);
            Path pathToServerXML = Paths.get(projectsPath, projectName, "src", "main", "liberty", "config", "server.xml");
            TestUtils.validateStanzaInConfigFile(pathToServerXML.toString(), insertedFeature);
        } finally {
            // Replace server.xml content with the original content
            UIBotTestUtils.pasteOnActiveWindow(remoteRobot, true);
        }
    }

    /**
     * Tests Liberty Lemminx Extension type ahead support in server.xml for a
     * Liberty Server Configuration Stanza
     */
    @Test
    @Video
    public void testInsertLibertyConfigElementIntoServerXML() {
        String stanzaSnippet = "log";
        String insertedConfig = "<logging></logging>";

        // get focus on server.xml tab prior to copy
        UIBotTestUtils.clickOnFileTab(remoteRobot, "server.xml");

        // Save the current server.xml content.
        UIBotTestUtils.copyWindowContent(remoteRobot);

        // Insert a new element in server.xml.
        try {
            UIBotTestUtils.insertStanzaInAppServerXML(remoteRobot, stanzaSnippet, 20, 0, UIBotTestUtils.InsertionType.ELEMENT, true);
            TestUtils.sleepAndIgnoreException(2); // wait for editor to update
            Path pathToServerXML = Paths.get(projectsPath, projectName, "src", "main", "liberty", "config", "server.xml");
            TestUtils.validateStanzaInConfigFile(pathToServerXML.toString(), insertedConfig);
        } finally {
            // Replace server.xml content with the original content
            UIBotTestUtils.pasteOnActiveWindow(remoteRobot, true);
        }
    }

    /**
     * Tests liberty-ls type ahead support in server.env for a
     * Liberty Server Configuration Stanza
     */
    @Test
    @Video
    public void testInsertLibertyConfigIntoServerEnv() {
        String envCfgSnippet = "WLP_LOGGING_CON";
        String envCfgNameChooserSnippet = "FORMAT";
        String envCfgValueSnippet = "SIM";
        String expectedServerEnvString = "WLP_LOGGING_CONSOLE_FORMAT=SIMPLE";

        // get focus on server.env tab prior to copy
        UIBotTestUtils.clickOnFileTab(remoteRobot, "server.env");

        // Save the current server.env content.
        UIBotTestUtils.copyWindowContent(remoteRobot);

        try {
            UIBotTestUtils.insertConfigIntoConfigFile(remoteRobot, "server.env", envCfgSnippet, envCfgNameChooserSnippet, envCfgValueSnippet, true);
            Path pathToServerEnv = Paths.get(projectsPath, projectName, "src", "main", "liberty", "config", "server.env");
            TestUtils.validateStringInFile(pathToServerEnv.toString(), expectedServerEnvString);
        } finally {
            // Replace server.xml content with the original content
            UIBotTestUtils.pasteOnActiveWindow(remoteRobot);
        }
    }

    /**
     * Tests liberty-ls type ahead support in bootstrap.properties for a
     * Liberty Server Configuration booststrap.properties entry
     */
    @Test
    @Video
    public void testInsertLibertyConfigIntoBootstrapProps() {
        String configNameSnippet = "com.ibm.ws.logging.con";
        String configNameChooserSnippet = "format";
        String configValueSnippet = "TBA";
        String expectedBootstrapPropsString = "com.ibm.ws.logging.console.format=TBASIC";

        // get focus on bootstrap.properties tab prior to copy
        UIBotTestUtils.clickOnFileTab(remoteRobot, "bootstrap.properties");

        // Save the current bootstrap.properties content.
        UIBotTestUtils.copyWindowContent(remoteRobot);

        try {
            UIBotTestUtils.insertConfigIntoConfigFile(remoteRobot, "bootstrap.properties", configNameSnippet, configNameChooserSnippet, configValueSnippet, true);
            Path pathToBootstrapProps = Paths.get(projectsPath, projectName, "src", "main", "liberty", "config", "bootstrap.properties");
            TestUtils.validateStringInFile(pathToBootstrapProps.toString(), expectedBootstrapPropsString);
        } finally {
            // Replace server.xml content with the original content
            UIBotTestUtils.pasteOnActiveWindow(remoteRobot);
        }
    }

    /**
     * Tests liberty-ls Hover support in server.env for a
     * Liberty Server Config setting
     */
    @Test
    @Video
    public void testServerEnvCfgHover() {
        String testHoverTarget = "LOGLEVEL";
        String hoverExpectedOutcome = "This setting controls the granularity of messages that go to the console. The valid values are INFO, AUDIT, WARNING, ERROR, and OFF. The default is AUDIT. If using with the Eclipse developer tools this must be set to the default.";

        //mover cursor to hover point
        UIBotTestUtils.hoverInAppServerCfgFile(remoteRobot, testHoverTarget, "server.env", UIBotTestUtils.PopupType.DOCUMENTATION);
        String hoverFoundOutcome = UIBotTestUtils.getHoverStringData(remoteRobot, UIBotTestUtils.PopupType.DOCUMENTATION);

        // Validate that the hover action raised the expected hint text
        TestUtils.validateHoverData(hoverExpectedOutcome, hoverFoundOutcome);
    }

    /**
     * Tests liberty-ls Hover support in bootstrap.properties for a
     * Liberty Server properties setting
     */
    @Test
    @Video
    public void testBootstrapPropsCfgHover() {

        String testHoverTarget = "log.level";
        String hoverExpectedOutcome = "This setting controls the granularity of messages that go to the console. The valid values are INFO, AUDIT, WARNING, ERROR, and OFF. The default is AUDIT. If using with the Eclipse developer tools this must be set to the default.";

        //mover cursor to hover point
        UIBotTestUtils.hoverInAppServerCfgFile(remoteRobot, testHoverTarget, "bootstrap.properties", UIBotTestUtils.PopupType.DOCUMENTATION);
        String hoverFoundOutcome = UIBotTestUtils.getHoverStringData(remoteRobot, UIBotTestUtils.PopupType.DOCUMENTATION);

        // Validate that the hover action raised the expected hint text
        TestUtils.validateHoverData(hoverExpectedOutcome, hoverFoundOutcome);
    }

    /**
     * Tests liberty-ls support in server.xml for
     * diagnostic and quickfix
     */
    @Test
    @Video
    public void testDiagnosticInServerXML() {
        String stanzaSnippet = "<logging appsWriteJson=wrong\" />";
        String flaggedString = "wrong";
        String expectedHoverData = "cvc-datatype-valid.1.2.3: 'wrong' is not a valid value of union type 'booleanType'.";

        Path pathToServerXML = null;
        pathToServerXML = Paths.get(projectsPath, projectName, "src", "main", "liberty", "config", "server.xml");

        // get focus on server.xml tab prior to copy
        UIBotTestUtils.clickOnFileTab(remoteRobot, "server.xml");

        // Save the current server.xml content.
        UIBotTestUtils.copyWindowContent(remoteRobot);

        try {
            UIBotTestUtils.insertStanzaInAppServerXML(remoteRobot, stanzaSnippet, 20, 0, UIBotTestUtils.InsertionType.ELEMENT, false);

            //move cursor to hover point
            UIBotTestUtils.hoverInAppServerCfgFile(remoteRobot, flaggedString, "server.xml", UIBotTestUtils.PopupType.DIAGNOSTIC);
            String foundHoverData = UIBotTestUtils.getHoverStringData(remoteRobot, UIBotTestUtils.PopupType.DIAGNOSTIC);
            TestUtils.validateHoverData(expectedHoverData, foundHoverData);

        } finally {
            // Replace server.xml content with the original content
            UIBotTestUtils.pasteOnActiveWindow(remoteRobot);
        }

    }

    /**
     * Tests liberty-ls support in server.xml for
     * diagnostic and quickfix
     */
    @Test
    @Video
    public void testQuickFixInServerXML() {
        String stanzaSnippet = "<logging appsWriteJson=wrong\" />";
        String flaggedString = "wrong";
        String correctedStanza = "<logging appsWriteJson=\"true\" />";
        String quickfixChooserString = "Replace with 'true'";
        String expectedHoverData = "cvc-datatype-valid.1.2.3: 'wrong' is not a valid value of union type 'booleanType'.";

        Path pathToServerXML = null;
        pathToServerXML = Paths.get(projectsPath, projectName, "src", "main", "liberty", "config", "server.xml");

        // get focus on server.xml tab prior to copy
        UIBotTestUtils.clickOnFileTab(remoteRobot, "server.xml");

        // Save the current server.xml content.
        UIBotTestUtils.copyWindowContent(remoteRobot);

        try {
            UIBotTestUtils.insertStanzaInAppServerXML(remoteRobot, stanzaSnippet, 20, 0, UIBotTestUtils.InsertionType.ELEMENT, false);

            //there should be a diagnostic - move cursor to hover point
            UIBotTestUtils.hoverForQuickFixInAppFile(remoteRobot, flaggedString, "server.xml", quickfixChooserString);

            UIBotTestUtils.chooseQuickFix(remoteRobot, quickfixChooserString);
            TestUtils.validateStanzaInConfigFile(pathToServerXML.toString(), correctedStanza);

        } finally {
            // Replace server.xml content with the original content
            UIBotTestUtils.pasteOnActiveWindow(remoteRobot, true);
        }


    }

    /**
     * Tests liberty-ls diagnostic support in server.env
     */
    @Test
    @Video
    public void testDiagnosticInServerEnv() {
        String envCfgSnippet = "WLP_LOGGING_CON";
        String envCfgNameChooserSnippet = "FORMAT";
        String incorrectValue = "NONE";
        String expectedHoverData = "The value `NONE` is not valid for the variable `WLP_LOGGING_CONSOLE_FORMAT`.";

        // get focus on server.env tab prior to copy
        UIBotTestUtils.clickOnFileTab(remoteRobot, "server.env");

        // Save the current server.env content.
        UIBotTestUtils.copyWindowContent(remoteRobot);

        try {
            UIBotTestUtils.insertConfigIntoConfigFile(remoteRobot, "server.env", envCfgSnippet, envCfgNameChooserSnippet, incorrectValue, false);
            //move cursor to hover point
            UIBotTestUtils.hoverInAppServerCfgFile(remoteRobot, "NONE", "server.env", UIBotTestUtils.PopupType.DIAGNOSTIC);
            String foundHoverData = UIBotTestUtils.getHoverStringData(remoteRobot, UIBotTestUtils.PopupType.DIAGNOSTIC);
            TestUtils.validateHoverData(expectedHoverData, foundHoverData);

        } finally {
            // Replace server.xml content with the original content
            UIBotTestUtils.pasteOnActiveWindow(remoteRobot);
        }

    }

    /**
     * Tests liberty-ls diagnostic support in boostrap.properties
     */
    @Test
    @Video
    public void testDiagnosticInBootstrapProperties() {
        String configNameSnippet = "com.ibm.ws.logging.con";
        String configNameChooserSnippet = "format";
        String incorrectValue = "none";
        String expectedHoverData = "The value `none` is not valid for the property `com.ibm.ws.logging.console.format`.";

        // get focus on bootstrap.properties tab prior to copy
        UIBotTestUtils.clickOnFileTab(remoteRobot, "bootstrap.properties");

        // Save the current bootstrap.properties content.
        UIBotTestUtils.copyWindowContent(remoteRobot);

        try {
            UIBotTestUtils.insertConfigIntoConfigFile(remoteRobot, "bootstrap.properties", configNameSnippet, configNameChooserSnippet, incorrectValue, false);

            //move cursor to hover point
            UIBotTestUtils.hoverInAppServerCfgFile(remoteRobot, "none", "bootstrap.properties", UIBotTestUtils.PopupType.DIAGNOSTIC);
            String foundHoverData = UIBotTestUtils.getHoverStringData(remoteRobot, UIBotTestUtils.PopupType.DIAGNOSTIC);
            TestUtils.validateHoverData(expectedHoverData, foundHoverData);
        } finally {
            // Replace server.xml content with the original content
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

        // get a JTreeFixture reference to the file project viewer entry
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofMinutes(2));
        JTreeFixture projTree = projectFrame.getProjectViewJTree(projectName);
        projTree.expand(projectName, "src", "main", "liberty", "config");

        // open server.xml file
        UIBotTestUtils.openFile(remoteRobot, projectName, "server.xml", projectName, "src", "main", "liberty", "config");

        // open server.env file
        UIBotTestUtils.openFile(remoteRobot, projectName, "server.env", projectName, "src", "main", "liberty", "config");

        // open bootstrap.properties file
        UIBotTestUtils.openFile(remoteRobot, projectName, "bootstrap.properties", projectName, "src", "main", "liberty", "config");

        // Removes the build tool window if it is opened. This prevents text to be hidden by it.
        UIBotTestUtils.removeToolWindow(remoteRobot, "Build:");
    }
}
