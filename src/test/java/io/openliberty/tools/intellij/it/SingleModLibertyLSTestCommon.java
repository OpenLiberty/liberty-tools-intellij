package io.openliberty.tools.intellij.it;

import com.automation.remarks.junit5.Video;
import com.intellij.remoterobot.RemoteRobot;
import io.openliberty.tools.intellij.it.fixtures.WelcomeFrameFixture;
import org.junit.jupiter.api.*;

import java.time.Duration;

import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitForIgnoringError;

public abstract class SingleModLibertyLSTestCommon {
    public static final String REMOTEBOT_URL = "http://localhost:8082";
    public static final RemoteRobot remoteRobot = new RemoteRobot(REMOTEBOT_URL);

    String projectName;
    String projectPath;


    public SingleModLibertyLSTestCommon(String projectName, String projectPath) {
        this.projectName = projectName;
        this.projectPath = projectPath;
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
        UIBotTestUtils.closeSourceFile(remoteRobot, "server.xml");
        UIBotTestUtils.closeProjectView(remoteRobot);
        UIBotTestUtils.closeProjectFrame(remoteRobot);
    }

    /**
     * Tests Liberty Lemminx Extension Hover support in server.xml for a
     * Liberty Server Feature
     */
    @Test
    @Video
    public void testServerXMLFeatureHover() {
        String testName = "testServerXMLFeatureHover";
        String testHoverTarget = "mpHealth-4.0";
        String testAppName = "gradle-app";
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
    @Video
    public void testServerXMLNonFeatureHover() {
        String testHoverTarget = "httpEndpoint";
        String hoverExpectedOutcome = "Configuration properties for an HTTP endpoint.";

        //mover cursor to hover point
        UIBotTestUtils.hoverInGradleAppServerXML(remoteRobot, testHoverTarget);

        // Validate that the hover action raised the expected hint text
        TestUtils.validateHoverAction(remoteRobot, hoverExpectedOutcome, "HTTP");
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
        //UIBotTestUtils.openDashboardView(remoteRobot);
        //UIBotTestUtils.validateDashboardProjectTreeItemIsShowing(remoteRobot, projectName);
        //UIBotTestUtils.expandDashboardProjectTree(remoteRobot);
        UIBotTestUtils.openServerXMLFile(remoteRobot, projectName);
    }

    /**
     * Deletes test reports.
     */
    public abstract void deleteTestReports();

    /**
     * Validates that test reports were generated.
     */
    public abstract void validateTestReportsExist();
}
