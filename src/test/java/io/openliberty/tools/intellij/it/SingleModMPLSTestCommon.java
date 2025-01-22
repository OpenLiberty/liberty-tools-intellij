/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation.
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
import io.openliberty.tools.intellij.it.Utils.ItConstants;
import io.openliberty.tools.intellij.it.fixtures.ProjectFrameFixture;
import org.junit.jupiter.api.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitForIgnoringError;

public abstract class SingleModMPLSTestCommon {
    public static final String REMOTEBOT_URL = "http://localhost:8082";
    public static final RemoteRobot remoteRobot = new RemoteRobot(REMOTEBOT_URL);

    String projectName;
    String projectsPath;

    public SingleModMPLSTestCommon(String projectName, String projectsPath) {
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
        projectFrame.findText("health").doubleClick();
        projectFrame.findText("META-INF").doubleClick();
        projectFrame.findText("resources").doubleClick();

        UIBotTestUtils.closeFileEditorTab(remoteRobot, ItConstants.SERVICE_LIVE_HEALTH_CHECK_JAVA, "5");
        UIBotTestUtils.closeFileEditorTab(remoteRobot, ItConstants.MPG_PROPERTIES, "5");
        if (!remoteRobot.isMac()) {
            UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, ItConstants.COMPACT_MODE, 3);
        }
        UIBotTestUtils.closeProjectView(remoteRobot);
        UIBotTestUtils.closeProjectFrame(remoteRobot);
        UIBotTestUtils.validateProjectFrameClosed(remoteRobot);
    }

    /**
     * Tests MicroProfile Language Server code snippet support in a Java source file
     */
    @Test
    @Video
    public void testInsertCodeSnippetIntoJavaPart() {
        String snippetStr = "mp";
        String snippetChooser = "liveness";
        String insertedCode = "public class ServiceLiveHealthCheck implements HealthCheck {";

        // get focus on file tab prior to copy
        UIBotTestUtils.clickOnFileTab(remoteRobot, ItConstants.SERVICE_LIVE_HEALTH_CHECK_JAVA);

        // Save the current content.
        UIBotTestUtils.copyWindowContent(remoteRobot);

        // Delete the current src code
        UIBotTestUtils.clearWindowContent(remoteRobot);

        // Insert a code snippet into java part
        try {
            UIBotTestUtils.insertCodeSnippetIntoSourceFile(remoteRobot, ItConstants.SERVICE_LIVE_HEALTH_CHECK_JAVA, snippetStr, snippetChooser);
            Path pathToSrc = Paths.get(projectsPath, projectName, ItConstants.HEALTH_DIR_PATH, ItConstants.SERVICE_LIVE_HEALTH_CHECK_JAVA);
            TestUtils.validateCodeInJavaSrc(pathToSrc.toString(), insertedCode);
        } finally {
            // Replace modified content with the original content
            UIBotTestUtils.pasteOnActiveWindow(remoteRobot);
        }
    }

    /**
     * Tests MicroProfile Language Server diagnostic support in a Java source file
     */
    @Test
    @Video
    public void testMPDiagnosticsInJavaPart() {

        String livenessString = "@Liveness";
        String flaggedString = "ServiceLiveHealthCheck";
        String expectedHoverData = "The class `io.openliberty.mp.sample.health.ServiceLiveHealthCheck` implementing the HealthCheck interface should use the @Liveness, @Readiness or @Health annotation.";

        // get focus on file tab prior to copy
        UIBotTestUtils.clickOnFileTab(remoteRobot, ItConstants.SERVICE_LIVE_HEALTH_CHECK_JAVA);

        // Save the current content.
        UIBotTestUtils.copyWindowContent(remoteRobot);

        // Delete the liveness annotation
        UIBotTestUtils.selectAndDeleteTextInJavaPart(remoteRobot, ItConstants.SERVICE_LIVE_HEALTH_CHECK_JAVA, livenessString);
        Path pathToSrc = Paths.get(projectsPath, projectName, ItConstants.HEALTH_DIR_PATH, ItConstants.SERVICE_LIVE_HEALTH_CHECK_JAVA);

        try {
            // validate @Liveness no longer found in java part
            TestUtils.validateStringNotInFile(pathToSrc.toString(), livenessString);
            TestUtils.sleepAndIgnoreException(1);

            String foundHoverData = null;
            int maxWait = 60, delay = 5; // in some cases it can take 35s for the diagnostic to appear
            for (int i = 0; i <= maxWait; i += delay) {
                //there should be a diagnostic - move cursor to hover point
                UIBotTestUtils.hoverInAppServerCfgFile(remoteRobot, flaggedString, ItConstants.SERVICE_LIVE_HEALTH_CHECK_JAVA, UIBotTestUtils.PopupType.DIAGNOSTIC);

                foundHoverData = UIBotTestUtils.getHoverStringData(remoteRobot, UIBotTestUtils.PopupType.DIAGNOSTIC);
                if (!foundHoverData.isBlank()) {
                    break;
                }
                TestUtils.sleepAndIgnoreException(delay);
            }
            TestUtils.validateHoverData(expectedHoverData, foundHoverData);

        } finally {
            // Replace modified content with the original content
            UIBotTestUtils.pasteOnActiveWindow(remoteRobot);
        }
    }

    /**
     * Tests MicroProfile Language Server quick fix support in a Java source file
     */
    @Test
    @Video
    public void testMPQuickFixInJavaFile() {
        String livenessString = "@Liveness";
        String flaggedString = "ServiceLiveHealthCheck";
        String quickfixChooserString = "Insert " + livenessString;
        String mainQuickFixActionStr  = "Generate OpenAPI Annotations for 'ServiceLiveHealthCheck'";

        // get focus on file tab prior to copy
        UIBotTestUtils.clickOnFileTab(remoteRobot, ItConstants.SERVICE_LIVE_HEALTH_CHECK_JAVA);

        // Save the current content.
        UIBotTestUtils.copyWindowContent(remoteRobot);

        // Delete the liveness annotation
        UIBotTestUtils.selectAndDeleteTextInJavaPart(remoteRobot,ItConstants.SERVICE_LIVE_HEALTH_CHECK_JAVA, livenessString);
        Path pathToSrc = Paths.get(projectsPath, projectName, ItConstants.HEALTH_DIR_PATH, ItConstants.SERVICE_LIVE_HEALTH_CHECK_JAVA);

        try {
            // validate @Liveness no longer found in java part
            TestUtils.validateStringNotInFile(pathToSrc.toString(), livenessString);

            //there should be a diagnostic - move cursor to hover point
            UIBotTestUtils.hoverForQuickFixInAppFile(remoteRobot, flaggedString, ItConstants.SERVICE_LIVE_HEALTH_CHECK_JAVA, quickfixChooserString);

            // trigger and use the quickfix popup attached to the diagnostic
            UIBotTestUtils.chooseQuickFix(remoteRobot, quickfixChooserString);

            TestUtils.validateCodeInJavaSrc(pathToSrc.toString(), livenessString);
        } finally {
            // Replace modified content with the original content
            UIBotTestUtils.pasteOnActiveWindow(remoteRobot);
        }
    }

    /**
     * Tests MicroProfile Language Server completion support for microprofile config entries
     * in the microprofile-config.properties file
     */
    @Test
    @Video
    public void testInsertMicroProfileProperty() {
        String cfgSnippet = "mp";
        String cfgNameChooserSnippet = "default-procedures";
        String cfgValueSnippet = "tr";
        String expectedMpCfgPropertiesString = "mp.health.disable-default-procedures=true";

        // get focus on file tab prior to copy
        UIBotTestUtils.clickOnFileTab(remoteRobot, ItConstants.MPG_PROPERTIES);

        // Save the current microprofile-config.properties content.
        UIBotTestUtils.copyWindowContent(remoteRobot);

        try {
            UIBotTestUtils.insertConfigIntoMPConfigPropertiesFile(remoteRobot, ItConstants.MPG_PROPERTIES, cfgSnippet, cfgNameChooserSnippet, cfgValueSnippet, true);
            Path pathToMpCfgProperties = Paths.get(projectsPath, projectName, String.join(File.separator, ItConstants.META_INF_DIR_PATH), ItConstants.MPG_PROPERTIES);
            TestUtils.validateStringInFile(pathToMpCfgProperties.toString(), expectedMpCfgPropertiesString);
        } finally {
            // Replace modified microprofile-config.properties with the original content
            UIBotTestUtils.pasteOnActiveWindow(remoteRobot, true);
        }
    }

    /**
     * Tests MicroProfile Language Server hover support for microprofile config entries
     * in the microprofile-config.properties file
     */
    @Test
    @Video
    public void testMicroProfileConfigHover() {

        String testHoverTarget = "client.Service";
        String hoverExpectedOutcome = "io.openliberty.mp.sample.client.Service/mp-rest/urlThe base URL to use for " +
                "this service, the equivalent of the baseUrl method. This property (or */mp-rest/uri) is " +
                "considered required, however implementations may have other ways to define these URLs/URIs.Type: " +
                "java.lang.StringValue: http://localhost:9081/data/client/service";

        //mover cursor to hover point
        UIBotTestUtils.hoverInAppServerCfgFile(remoteRobot, testHoverTarget, ItConstants.MPG_PROPERTIES, UIBotTestUtils.PopupType.DOCUMENTATION);
        String hoverFoundOutcome = UIBotTestUtils.getHoverStringData(remoteRobot, UIBotTestUtils.PopupType.DOCUMENTATION);

        // if the LS has not yet poulated the popup, re-get the popup data
        for (int i = 0; i<=5; i++){
            if (hoverFoundOutcome.contains("Fetching Documentation...")) {
                hoverFoundOutcome = UIBotTestUtils.getHoverStringData(remoteRobot, UIBotTestUtils.PopupType.DOCUMENTATION);
            }
            else {
                break;
            }
        }

        // Validate that the hover action raised the expected hint text
        TestUtils.validateHoverData(hoverExpectedOutcome, hoverFoundOutcome);
    }

    /**
     * Tests MicroProfile Language Server diagnostic support for microprofile config entries
     * in the microprofile-config.properties file
     */
    @Test
    @Video
    public void testDiagnosticInMicroProfileConfigProperties() {
        String MPCfgSnippet = "mp.health.disable";
        String MPCfgNameChooserSnippet = "procedures";
        String incorrectValue = "none";
        String expectedHoverData = "Type mismatch: boolean expected. By default, this value will be interpreted as 'false'";

        // get focus on file tab prior to copy
        UIBotTestUtils.clickOnFileTab(remoteRobot, ItConstants.MPG_PROPERTIES);

        // Save the current content.
        UIBotTestUtils.copyWindowContent(remoteRobot);

        try {
            UIBotTestUtils.insertConfigIntoMPConfigPropertiesFile(remoteRobot, ItConstants.MPG_PROPERTIES, MPCfgSnippet, MPCfgNameChooserSnippet, incorrectValue, false);

            //move cursor to hover point
            UIBotTestUtils.hoverInAppServerCfgFile(remoteRobot, incorrectValue, ItConstants.MPG_PROPERTIES, UIBotTestUtils.PopupType.DIAGNOSTIC);
            String foundHoverData = UIBotTestUtils.getHoverStringData(remoteRobot, UIBotTestUtils.PopupType.DIAGNOSTIC);
            TestUtils.validateHoverData(expectedHoverData, foundHoverData);
        } finally {
            // Replace modified microprofile-config.properties with the original content
            UIBotTestUtils.pasteOnActiveWindow(remoteRobot, true);
        }

    }

    /**
     * Tests MicroProfile Language Server quick-fix support for microprofile config entries
     * in the microprofile-config.properties file
     */
    @Test
    @Video
    public void testQuickFixInMicroProfileConfigProperties() {
        String MPCfgSnippet = "mp.health.disable";
        String MPCfgNameChooserSnippet = "procedures";
        String incorrectValue = "none";
        String quickfixChooserString = "Replace with 'true'?";
        String correctedValue = "mp.health.disable-default-procedures=true";
        String expectedHoverData = "Type mismatch: boolean expected. By default, this value will be interpreted as 'false'";

        Path pathToMpCfgProperties = Paths.get(projectsPath, projectName, String.join(File.separator, ItConstants.META_INF_DIR_PATH), ItConstants.MPG_PROPERTIES);

        // get focus on file tab prior to copy
        UIBotTestUtils.clickOnFileTab(remoteRobot, ItConstants.MPG_PROPERTIES);

        // Save the current content.
        UIBotTestUtils.copyWindowContent(remoteRobot);

        try {
            UIBotTestUtils.insertConfigIntoMPConfigPropertiesFile(remoteRobot, ItConstants.MPG_PROPERTIES, MPCfgSnippet, MPCfgNameChooserSnippet, incorrectValue, false);

            //move cursor to hover point
            UIBotTestUtils.hoverForQuickFixInAppFile(remoteRobot, incorrectValue, ItConstants.MPG_PROPERTIES, quickfixChooserString);

            UIBotTestUtils.chooseQuickFix(remoteRobot, quickfixChooserString);
            TestUtils.validateStanzaInConfigFile(pathToMpCfgProperties.toString(), correctedValue);

        } finally {
            // Replace modified microprofile-config.properties with the original content
            UIBotTestUtils.pasteOnActiveWindow(remoteRobot, true);
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
        UIBotTestUtils.findWelcomeFrame(remoteRobot);

        UIBotTestUtils.importProject(remoteRobot, projectPath, projectName);
        UIBotTestUtils.openProjectView(remoteRobot);
        if (!remoteRobot.isMac()) {
            UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, ItConstants.COMPACT_MODE, 3);
        }
        // IntelliJ does not start building and indexing until the Project View is open
        UIBotTestUtils.waitForIndexing(remoteRobot);
        UIBotTestUtils.openAndValidateLibertyToolWindow(remoteRobot, projectName);
        UIBotTestUtils.closeLibertyToolWindow(remoteRobot);

        // pre-open project tree before attempting to open files needed by testcases
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofMinutes(2));
        JTreeFixture projTree = projectFrame.getProjectViewJTree(projectName);

        UIBotTestUtils.openFile(remoteRobot, projectName, ItConstants.SERVICE_LIVEHEALTH_CHECK, TestUtils.combinePath(projectName, ItConstants.HEALTH_DIR_PATH_ARR));
        UIBotTestUtils.openFile(remoteRobot, projectName, ItConstants.MPG_PROPERTIES, TestUtils.combinePath(projectName, ItConstants.META_INF_DIR_PATH));

        // Removes the build tool window if it is opened. This prevents text to be hidden by it.
        UIBotTestUtils.removeToolWindow(remoteRobot, "Build:");
    }
}

