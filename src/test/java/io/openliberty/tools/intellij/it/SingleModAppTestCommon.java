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


    public SingleModAppTestCommon(String projectName, String projectPath, String wlpInstallPath, String appBaseURL, String appExpectedOutput) {
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
        // close project window.
        UIBotTestUtils.closeProject(remoteRobot);
    }

    @Test
    public void testDropDownStartAction() {
        String testName = "testDropDownStartAction";
        String absoluteWLPPath = projectPath + wlpInstallPath;

        // Start dev mode.
        UIBotTestUtils.runDashboardActionFromDropDownView(remoteRobot, "Start");

        try {
            // Validate that the application started.
            String url = appBaseURL + "api/resource";
            TestUtils.validateAppStarted(testName, url, appExpectedOutput, absoluteWLPPath);
        } finally {
            // Stop dev mode.
            UIBotTestUtils.runDashboardActionFromDropDownView(remoteRobot, "Stop");

            // Validate that the server stopped.
            TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
        }
    }

    public static void prepareEnv(String projectPath, String projectName) {
        waitForIgnoringError(Duration.ofMinutes(4), Duration.ofSeconds(5), "Wait for IDE to start", "IDE did not start", () -> remoteRobot.callJs("true"));
        remoteRobot.find(WelcomeFrameFixture.class, Duration.ofMinutes(2));

        // Import project.
        UIBotTestUtils.importProject(remoteRobot, projectPath, projectName);

        // Open the project and the Liberty plugin views.
        UIBotTestUtils.openViewUsingToolWindowPaneStripe(remoteRobot, "Project");
        UIBotTestUtils.openViewUsingToolWindowPaneStripe(remoteRobot, "Liberty");
        UIBotTestUtils.waitForProjectToShownInDashboard(remoteRobot, "LibertyTree", projectName);

        // Expand the Liberty dashboard view
        UIBotTestUtils.clickOnActionButton(remoteRobot, "LIBERTY_ACTION_TOOLBAR", "action.ExpandAll.text");
    }


}
