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

        UIBotTestUtils.closeFileEditorTab(remoteRobot, "ServiceLiveHealthCheck.java", "5");
        UIBotTestUtils.closeFileEditorTab(remoteRobot, "microprofile-config.properties", "5");
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
        UIBotTestUtils.clickOnFileTab(remoteRobot, "ServiceLiveHealthCheck.java");

        // Save the current content.
        UIBotTestUtils.copyWindowContent(remoteRobot);

        // Delete the current src code
        UIBotTestUtils.clearWindowContent(remoteRobot);

        // Insert a code snippet into java part
        try {
            UIBotTestUtils.insertCodeSnippetIntoSourceFile(remoteRobot, "ServiceLiveHealthCheck.java", snippetStr, snippetChooser);
            Path pathToSrc = Paths.get(projectsPath, projectName, "src", "main", "java", "io", "openliberty", "mp", "sample", "health","ServiceLiveHealthCheck.java");
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
        UIBotTestUtils.clickOnFileTab(remoteRobot, "ServiceLiveHealthCheck.java");

        // Save the current content.
        UIBotTestUtils.copyWindowContent(remoteRobot);

        // Delete the liveness annotation
        UIBotTestUtils.selectAndDeleteTextInJavaPart(remoteRobot, "ServiceLiveHealthCheck.java", livenessString);
        Path pathToSrc = Paths.get(projectsPath, projectName, "src", "main", "java", "io", "openliberty", "mp", "sample", "health", "ServiceLiveHealthCheck.java");

        try {
            // validate @Liveness no longer found in java part
            TestUtils.validateStringNotInFile(pathToSrc.toString(), livenessString);

            //there should be a diagnostic - move cursor to hover point
            UIBotTestUtils.hoverInAppServerCfgFile(remoteRobot, flaggedString, "ServiceLiveHealthCheck.java", UIBotTestUtils.PopupType.DIAGNOSTIC);

            String foundHoverData = UIBotTestUtils.getHoverStringData(remoteRobot, UIBotTestUtils.PopupType.DIAGNOSTIC);
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
        UIBotTestUtils.clickOnFileTab(remoteRobot, "ServiceLiveHealthCheck.java");

        // Save the current content.
        UIBotTestUtils.copyWindowContent(remoteRobot);

        // Delete the liveness annotation
        UIBotTestUtils.selectAndDeleteTextInJavaPart(remoteRobot,"ServiceLiveHealthCheck.java", livenessString);
        Path pathToSrc = Paths.get(projectsPath, projectName, "src", "main", "java", "io", "openliberty", "mp", "sample", "health", "ServiceLiveHealthCheck.java");

        try {
            // validate @Liveness no longer found in java part
            TestUtils.validateStringNotInFile(pathToSrc.toString(), livenessString);

            //there should be a diagnostic - move cursor to hover point
            UIBotTestUtils.hoverForQuickFixInAppFile(remoteRobot, flaggedString, "ServiceLiveHealthCheck.java", quickfixChooserString);

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
        UIBotTestUtils.clickOnFileTab(remoteRobot, "microprofile-config.properties");

        // Save the current microprofile-config.properties content.
        UIBotTestUtils.copyWindowContent(remoteRobot);

        try {
            UIBotTestUtils.insertConfigIntoMPConfigPropertiesFile(remoteRobot, "microprofile-config.properties", cfgSnippet, cfgNameChooserSnippet, cfgValueSnippet, true);
            Path pathToMpCfgProperties = Paths.get(projectsPath, projectName, "src", "main", "resources", "META-INF", "microprofile-config.properties");
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
        UIBotTestUtils.hoverInAppServerCfgFile(remoteRobot, testHoverTarget, "microprofile-config.properties", UIBotTestUtils.PopupType.DOCUMENTATION);
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
        UIBotTestUtils.clickOnFileTab(remoteRobot, "microprofile-config.properties");

        // Save the current content.
        UIBotTestUtils.copyWindowContent(remoteRobot);

        try {
            UIBotTestUtils.insertConfigIntoMPConfigPropertiesFile(remoteRobot, "microprofile-config.properties", MPCfgSnippet, MPCfgNameChooserSnippet, incorrectValue, false);

            //move cursor to hover point
            UIBotTestUtils.hoverInAppServerCfgFile(remoteRobot, incorrectValue, "microprofile-config.properties", UIBotTestUtils.PopupType.DIAGNOSTIC);
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

        Path pathToMpCfgProperties = Paths.get(projectsPath, projectName,"src", "main", "resources", "META-INF", "microprofile-config.properties");

        // get focus on file tab prior to copy
        UIBotTestUtils.clickOnFileTab(remoteRobot, "microprofile-config.properties");

        // Save the current content.
        UIBotTestUtils.copyWindowContent(remoteRobot);

        try {
            UIBotTestUtils.insertConfigIntoMPConfigPropertiesFile(remoteRobot, "microprofile-config.properties", MPCfgSnippet, MPCfgNameChooserSnippet, incorrectValue, false);

            //move cursor to hover point
            UIBotTestUtils.hoverForQuickFixInAppFile(remoteRobot, incorrectValue, "microprofile-config.properties", quickfixChooserString);

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
        remoteRobot.find(WelcomeFrameFixture.class, Duration.ofMinutes(2));

        UIBotTestUtils.importProject(remoteRobot, projectPath, projectName);
        UIBotTestUtils.openProjectView(remoteRobot);
        // IntelliJ does not start building and indexing until the project is open in the UI
        UIBotTestUtils.waitForIndexing(remoteRobot);
        UIBotTestUtils.openAndValidateLibertyToolWindow(remoteRobot, projectName);
        UIBotTestUtils.closeLibertyToolWindow(remoteRobot);

        // pre-open project tree before attempting to open files needed by testcases
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofMinutes(2));
        JTreeFixture projTree = projectFrame.getProjectViewJTree(projectName);

        UIBotTestUtils.openFile(remoteRobot, projectName, "ServiceLiveHealthCheck", projectName, "src", "main", "java", "io.openliberty.mp.sample", "health");
        UIBotTestUtils.openFile(remoteRobot, projectName, "microprofile-config.properties", projectName, "src", "main", "resources", "META-INF");

        // Removes the build tool window if it is opened. This prevents text to be hidden by it.
        UIBotTestUtils.removeToolWindow(remoteRobot, "Build:");
    }
}

