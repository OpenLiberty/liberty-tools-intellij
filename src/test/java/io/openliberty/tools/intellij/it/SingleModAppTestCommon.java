package io.openliberty.tools.intellij.it;

import com.intellij.remoterobot.RemoteRobot;
import io.openliberty.tools.intellij.it.fixtures.WelcomeFrameFixture;
import org.junit.jupiter.api.*;

import java.time.Duration;

import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitForIgnoringError;

public abstract class SingleModAppTestCommon {
    public static final String REMOTEBOT_URL = "http://localhost:8082";
    public static final RemoteRobot remoteRobot = new RemoteRobot(REMOTEBOT_URL);

    String projectName;
    String projectPath;
    String wlpInstallPath;
    String appBaseURL;
    String appExpectedOutput;


    /**
     * Constructor.
     */
    public SingleModAppTestCommon(String projectName, String projectPath, String wlpInstallPath, String appBaseURL, String appExpectedOutput) {
        this.projectName = projectName;
        this.projectPath = projectPath;
        this.wlpInstallPath = wlpInstallPath;
        this.appBaseURL = appBaseURL;
        this.appExpectedOutput = appExpectedOutput;
    }

    /**
     * Processes actions before each test.
     *
     * @param info Test information.
     */
    @BeforeEach
    public void beforeEach(TestInfo info) {
        System.out.println(
                "INFO: Test " + this.getClass().getSimpleName() + "#" + info.getDisplayName() + " entry: " + java.time.LocalDateTime.now());
    }

    /**
     * Processes actions after each test.
     *
     * @param info Test information.
     */
    @AfterEach
    public void afterEach(TestInfo info) {
        System.out.println(
                "INFO: Test " + this.getClass().getSimpleName() + "#" + info.getDisplayName() + " exit: " + java.time.LocalDateTime.now());
    }

    /**
     * Cleanup.
     */
    @AfterAll
    public static void cleanup() {
        UIBotTestUtils.closeDashboardView(remoteRobot);
        UIBotTestUtils.closeProjectView(remoteRobot);
        UIBotTestUtils.closeProjectFrame(remoteRobot);
    }

    /**
     * Tests Liberty Tools start action.
     */
    @Test
    public void testDropDownStartAction() {
        String testName = "testDropDownStartAction";
        String absoluteWLPPath = projectPath + wlpInstallPath;

        // Start dev mode.
        UIBotTestUtils.runDashboardActionFromDropDownView(remoteRobot, "LibertyTree", "Start");

        try {
            // Validate that the application started.
            String url = appBaseURL + "api/resource";
            TestUtils.validateAppStarted(testName, url, appExpectedOutput, absoluteWLPPath);
        } finally {
            // Stop dev mode.
            UIBotTestUtils.runDashboardActionFromDropDownView(remoteRobot, "LibertyTree", "Stop");

            // Validate that the server stopped.
            TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
        }
    }

    public static void prepareEnv(String projectPath, String projectName) {
        waitForIgnoringError(Duration.ofMinutes(4), Duration.ofSeconds(5), "Wait for IDE to start", "IDE did not start", () -> remoteRobot.callJs("true"));
        remoteRobot.find(WelcomeFrameFixture.class, Duration.ofMinutes(2));

        UIBotTestUtils.importProject(remoteRobot, projectPath, projectName);
        UIBotTestUtils.openProjectView(remoteRobot);
        UIBotTestUtils.openDashboardView(remoteRobot);
        UIBotTestUtils.validateProjectIsInDashboard(remoteRobot, projectName);
        UIBotTestUtils.expandProjectActionMenu(remoteRobot);
    }
}
