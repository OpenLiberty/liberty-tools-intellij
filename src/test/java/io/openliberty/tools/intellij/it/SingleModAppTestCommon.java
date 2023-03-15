package io.openliberty.tools.intellij.it;

import com.automation.remarks.junit5.Video;
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
        UIBotTestUtils.closeDashboardView(remoteRobot);
        UIBotTestUtils.closeProjectView(remoteRobot);
        UIBotTestUtils.closeProjectFrame(remoteRobot);
        UIBotTestUtils.validateProjectFrameClosed(remoteRobot);
    }

    /**
     * Tests Liberty Tools start/stop actions run from the project drop-down menu.
     */
    @Test
    @Video
    public void testStartActionUsingDropDownMenu() {
        String testName = "testDropDownStartAction";
        String absoluteWLPPath = projectPath + wlpInstallPath;

        // Start dev mode.
        UIBotTestUtils.runDashboardActionFromDropDownView(remoteRobot, "Start");

        // Validate that the application started.
        String url = appBaseURL + "api/resource";
        TestUtils.validateAppStarted(testName, url, appExpectedOutput, absoluteWLPPath);

        // Stop dev mode.
        UIBotTestUtils.runDashboardActionFromDropDownView(remoteRobot, "Stop");

        // Validate that the server stopped.
        TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
    }

    /**
     * Tests Liberty Tools start/RunTests/stop actions run from the project's drop-down action menu.
     */
    @Test
    public void testRunTestsActionUsingDropDownMenu() {
        String testName = "testDropDownStartAction";
        String absoluteWLPPath = projectPath + wlpInstallPath;

        // Delete any existing test report files.
        deleteTestReports();

        // Start dev mode.
        UIBotTestUtils.runDashboardActionFromDropDownView(remoteRobot, "Start");

        // Validate that the application started.
        String url = appBaseURL + "api/resource";
        TestUtils.validateAppStarted(testName, url, appExpectedOutput, absoluteWLPPath);

        try {
            // Run the application's tests.
            UIBotTestUtils.runDashboardActionFromDropDownView(remoteRobot, "Run tests");

            // Validate that the report was generated.
            validateTestReportsExist();
        } finally {
            // Stop dev mode.
            UIBotTestUtils.runDashboardActionFromDropDownView(remoteRobot, "Stop");

            // Validate that the server stopped.
            TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
        }
    }

    /**
     * Tests Liberty Tools start/stop actions run from the project's pop-up action menu.
     */
    @Test
    @Disabled("Until https://github.com/OpenLiberty/liberty-tools-intellij/issues/272 is fixed.")
    public void testStartActionUsingPopUpMenu() {
        String testName = "testMenuStartAction";
        String absoluteWLPPath = projectPath + wlpInstallPath;

        // Start dev mode.
        UIBotTestUtils.runDashboardActionFromMenuView(remoteRobot, projectName, "Liberty: Start");

        // Validate that the application started.
        String url = appBaseURL + "api/resource";
        TestUtils.validateAppStarted(testName, url, appExpectedOutput, absoluteWLPPath);

        // Stop dev mode.
        UIBotTestUtils.runDashboardActionFromMenuView(remoteRobot, projectName, "Liberty: Stop");

        // Validate that the server stopped.
        TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
    }
    
    /**
     * Tests Liberty Tools start/runTests/stop actions run from the project's pop-up action menu.
     */
    @Test
    @Disabled("Until https://github.com/OpenLiberty/liberty-tools-intellij/issues/272 is fixed.")
    public void testRunTestsActionUsingPopUpMenu() {
        String testName = "testDropDownStartAction";
        String absoluteWLPPath = projectPath + wlpInstallPath;

        // Delete any existing test report files.
        deleteTestReports();

        // Start dev mode.
        UIBotTestUtils.runDashboardActionFromMenuView(remoteRobot, projectName, "Liberty: Start");

        // Validate that the application started.
        String url = appBaseURL + "api/resource";
        TestUtils.validateAppStarted(testName, url, appExpectedOutput, absoluteWLPPath);

        try {
            // Run the application's tests.
            UIBotTestUtils.runDashboardActionFromMenuView(remoteRobot, projectName, "Liberty: Run tests");

            // Validate that the reports were generated.
            validateTestReportsExist();
        } finally {
            // Stop dev mode.
            UIBotTestUtils.runDashboardActionFromMenuView(remoteRobot, projectName, "Liberty: Stop");

            // Validate that the server stopped.
            TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
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
        UIBotTestUtils.openDashboardView(remoteRobot);
        UIBotTestUtils.validateDashboardProjectTreeItemIsShowing(remoteRobot, projectName);
        UIBotTestUtils.expandDashboardProjectTree(remoteRobot);
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