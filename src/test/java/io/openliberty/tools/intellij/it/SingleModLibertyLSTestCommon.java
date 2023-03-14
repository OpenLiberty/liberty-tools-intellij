package io.openliberty.tools.intellij.it;

import com.intellij.remoterobot.RemoteRobot;
import io.openliberty.tools.intellij.it.fixtures.WelcomeFrameFixture;
import org.junit.jupiter.api.*;

import java.time.Duration;

import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitForIgnoringError;

public abstract class SingleModLibertyLSTestCommon {
    public static final String REMOTEBOT_URL = "http://localhost:8082";
    public static final RemoteRobot remoteRobot = new RemoteRobot(REMOTEBOT_URL);

    public static String projectName;
    String projectPath;
    String wlpInstallPath;
    String appBaseURL;
    String appExpectedOutput;


    public SingleModLibertyLSTestCommon(String projectName, String projectPath, String wlpInstallPath, String appBaseURL, String appExpectedOutput) {
        this.projectName = projectName;
        this.projectPath = projectPath;
        this.wlpInstallPath = wlpInstallPath;
        this.appBaseURL = appBaseURL;
        this.appExpectedOutput = appExpectedOutput;
    }

    @BeforeEach
    public void beforeEach(TestInfo info) {
        System.out.println(
                "INFO: Test " + this.getClass().getSimpleName() + "#" + info.getDisplayName() + " entry: " + java.time.LocalDateTime.now());
    }

    @AfterEach
    public void afterEach(TestInfo info) {
        System.out.println(
                "INFO: Test " + this.getClass().getSimpleName() + "#" + info.getDisplayName() + " exit: " + java.time.LocalDateTime.now());
    }

    @AfterAll
    public static void cleanup() {
        UIBotTestUtils.closeSourceFile(remoteRobot, "server.xml");
        UIBotTestUtils.closeProjectView(remoteRobot);
        UIBotTestUtils.closeProjectFrame(remoteRobot);
    }

    /**
     * Tests Liberty Lemminx Extension Hover support in server.xml for a
     * Liberty Server Feature
     */
    @Test
    public void testServerXMLFeatureHover() {
        String testName = "testServerXMLFeatureHover";
        String testHoverTarget = "mpHealth-4.0";
        String testAppName = "gradle-app";
        String absoluteWLPPath = projectPath + wlpInstallPath;
        String hoverExpectedOutcome = "This feature provides support for the MicroProfile Health specification.";

        // open server.xml file
        //UIBotTestUtils.openServerXMLFile(remoteRobot, projectName);

        //mover cursor to hover point
        UIBotTestUtils.hoverInGradleAppServerXML(remoteRobot, testHoverTarget);

        // Validate that the hover action raised the expected hint text
        TestUtils.validateHoverAction(remoteRobot, hoverExpectedOutcome, "Health");
    }

    /**
     * Tests Liberty Lemminx Extension Hover support in server.xml for a
     * Liberty Server Attribute
     */
    @Test
    public void testServerXMLNonFeatureHover() {
        String testName = "testServerXMLNonFeatureHover";
        String testHoverTarget = "httpEndpoint";
        String testAppName = "gradle-app";
        String absoluteWLPPath = projectPath + wlpInstallPath;
        String hoverExpectedOutcome = "Configuration properties for an HTTP endpoint.";

        //mover cursor to hover point
        UIBotTestUtils.hoverInGradleAppServerXML(remoteRobot, testHoverTarget);

        // Validate that the hover action raised the expected hint text
        TestUtils.validateHoverAction(remoteRobot, hoverExpectedOutcome, "HTTP");
    }

    public static void prepareEnv(String projectPath, String projectName) {
        waitForIgnoringError(Duration.ofMinutes(4), Duration.ofSeconds(5), "Wait for IDE to start", "IDE did not start", () -> remoteRobot.callJs("true"));
        remoteRobot.find(WelcomeFrameFixture.class, Duration.ofMinutes(2));

        UIBotTestUtils.importProject(remoteRobot, projectPath, projectName);
        UIBotTestUtils.openProjectView(remoteRobot);

        UIBotTestUtils.openServerXMLFile(remoteRobot, projectName);
    }


}
