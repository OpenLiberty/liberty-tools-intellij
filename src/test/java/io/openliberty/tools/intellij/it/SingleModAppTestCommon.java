package io.openliberty.tools.intellij.it;

import com.automation.remarks.junit5.Video;
import com.intellij.remoterobot.RemoteRobot;
import io.openliberty.tools.intellij.it.fixtures.WelcomeFrameFixture;
import org.junit.jupiter.api.*;

import java.nio.file.Paths;
import java.time.Duration;

import static com.intellij.remoterobot.utils.RepeatUtilsKt.waitForIgnoringError;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Common component for running both Maven and Gradle tests.
 */
public abstract class SingleModAppTestCommon {

    /**
     * URL to display the UI Component hierarchy. This is used to obtain xPath related
     * information to find UI components.
     */
    public static final String REMOTEBOT_URL = "http://localhost:8082";

    /**
     * The remote robot object.
     */
    public static final RemoteRobot remoteRobot = new RemoteRobot(REMOTEBOT_URL);

    /**
     * The path to the directory holding all projects.
     */
    String projectsPath;

    /**
     * Single module MicroProfile application.
     */
    String smMPProjectName;

    /**
     * Single module REST application that does not meet the requirements needed to automatically
     * show in the Liberty tool window. Liberty config file is not the expected and the build
     * file does not have any Liberty plugin related entries.
     */
    String smNLTRestProjectName;

    /**
     * The base URL to call the single module MicroProfile application.
     */
    String smMPAppBaseURL;

    /**
     * Single module MicroProfile application expected output.
     */
    String smMPAppOutput;

    /**
     * Constructor.
     */
    public SingleModAppTestCommon(String projectsPath, String smMPProjectName, String smNLTRestProjectName, String smMPAppBaseURL, String smMPAppOutput) {
        this.projectsPath = projectsPath;
        this.smMPProjectName = smMPProjectName;
        this.smNLTRestProjectName = smNLTRestProjectName;
        this.smMPAppBaseURL = smMPAppBaseURL;
        this.smMPAppOutput = smMPAppOutput;
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
        UIBotTestUtils.closeLibertyToolWindow(remoteRobot);
        UIBotTestUtils.closeProjectView(remoteRobot);
        UIBotTestUtils.closeProjectFrame(remoteRobot);
        UIBotTestUtils.validateProjectFrameClosed(remoteRobot);
    }

    /**
     * Tests the liberty: View <project build file> action run from the project's pop-up action menu.
     */
    @Test
    @Video
    public void testOpenBuildFileActionUsingPopUpMenu() {
        String editorTabName = getBuildFileName() + " (" + smMPProjectName + ")";

        // Close the editor tab if it was previously opened.
        UIBotTestUtils.closeFileEditorTab(remoteRobot, editorTabName, "5");

        // Open the build file.
        UIBotTestUtils.runLibertyTWActionFromMenuView(remoteRobot, smMPProjectName, getBuildFileOpenCommand());

        // Verify that build file tab is opened.
        Assertions.assertNotNull(UIBotTestUtils.getEditorTabCloseButton(remoteRobot, editorTabName, "10"),
                "Editor tab with the name of " + editorTabName + " could not be found.");

        // Close the editor tab.
        UIBotTestUtils.closeFileEditorTab(remoteRobot, editorTabName, "10");
    }

    /**
     * Tests dashboard start.../stop actions run from the project's drop-down action menu.
     */
    @Test
    public void testStartWithParamsActionUsingDropDownMenu() {
        String testName = "testStartWithParamsActionUsingDropDownMenu";
        String absoluteWLPPath = Paths.get(projectsPath, smMPProjectName, getWLPInstallPath()).toString();

        // Trigger the start with parameters configuration dialog.
        UIBotTestUtils.runLibertyTWActionFromDropDownView(remoteRobot, "Start...", false);

        // Run the configuration dialog.
        UIBotTestUtils.runStartParamsConfigDialog(remoteRobot, null);

        try {
            // Validate that the application started.
            String url = smMPAppBaseURL + "api/resource";
            TestUtils.validateAppStarted(testName, url, smMPAppOutput, absoluteWLPPath);

        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Stop dev mode.
                UIBotTestUtils.runLibertyTWActionFromDropDownView(remoteRobot, "Stop", false);

                // Validate that the server stopped.
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
            }
        }
    }

    /**
     * Tests dashboard start/RunTests/stop actions run from the project's drop-down action menu.
     */
    @Test
    public void testRunTestsActionUsingDropDownMenu() {
        String testName = "testRunTestsActionUsingDropDownMenu";
        String absoluteWLPPath = Paths.get(projectsPath, smMPProjectName, getWLPInstallPath()).toString();

        // Delete any existing test report files.
        deleteTestReports();

        // Start dev mode.
        UIBotTestUtils.runLibertyTWActionFromDropDownView(remoteRobot, "Start", false);

        // Validate that the application started.
        String url = smMPAppBaseURL + "api/resource";
        TestUtils.validateAppStarted(testName, url, smMPAppOutput, absoluteWLPPath);

        try {
            // Run the application's tests.
            UIBotTestUtils.runLibertyTWActionFromDropDownView(remoteRobot, "Run tests", false);

            // Validate that the report was generated.
            validateTestReportsExist();
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Stop dev mode.
                UIBotTestUtils.runLibertyTWActionFromDropDownView(remoteRobot, "Stop", false);

                // Validate that the server stopped.
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
            }
        }
    }

    /**
     * Tests Liberty tool window start.../stop actions selected on the project's drop-down action
     * menu and run using the play action button on the Liberty tool window's toolbar.
     */
    @Test
    @Disabled("Until https://github.com/OpenLiberty/liberty-tools-intellij/issues/272 is fixed.")
    public void testStartWithParamsActionUsingPlayToolbarButton() {
        String testName = "testStartWithParamsActionUsingPlayToolbarButton";
        String absoluteWLPPath = Paths.get(projectsPath, smMPProjectName, getWLPInstallPath()).toString();

        // Trigger the start with parameters configuration dialog.
        UIBotTestUtils.runLibertyTWActionFromDropDownView(remoteRobot, "Start...", true);

        // Run the configuration dialog.
        UIBotTestUtils.runStartParamsConfigDialog(remoteRobot, null);

        try {
            // Validate that the application started.
            String url = smMPAppBaseURL + "api/resource";
            TestUtils.validateAppStarted(testName, url, smMPAppOutput, absoluteWLPPath);
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Stop dev mode.
                UIBotTestUtils.runLibertyTWActionFromDropDownView(remoteRobot, "Stop", true);

                // Validate that the server stopped.
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
            }
        }
    }

    /**
     * Tests Liberty tool window start/RunTests/stop actions selected on the project's drop-down action
     * menu and run using the play action button on the Liberty tool window's toolbar.
     */
    @Test
    @Disabled("Until https://github.com/OpenLiberty/liberty-tools-intellij/issues/272 is fixed.")
    public void testRunTestsActionUsingPlayToolbarButton() {
        String testName = "testRunTestsActionUsingPlayToolbarButton";
        String absoluteWLPPath = Paths.get(projectsPath, smMPProjectName, getWLPInstallPath()).toString();

        // Delete any existing test report files.
        deleteTestReports();

        // Start dev mode.
        UIBotTestUtils.runLibertyTWActionFromDropDownView(remoteRobot, "Start", true);

        // Validate that the application started.
        String url = smMPAppBaseURL + "api/resource";
        TestUtils.validateAppStarted(testName, url, smMPAppOutput, absoluteWLPPath);

        try {
            // Run the application's tests.
            UIBotTestUtils.runLibertyTWActionFromDropDownView(remoteRobot, "Run tests", true);

            // Validate that the report was generated.
            validateTestReportsExist();
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Stop dev mode.
                UIBotTestUtils.runLibertyTWActionFromDropDownView(remoteRobot, "Stop", true);

                // Validate that the server stopped.
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
            }
        }
    }

    /**
     * Tests Liberty tool window start.../stop actions run from the project's pop-up action menu.
     */
    @Test
    @Disabled("Until https://github.com/OpenLiberty/liberty-tools-intellij/issues/272 is fixed.")
    public void testStartWithParamsActionUsingPopUpMenu() {
        String testName = "testStartWithParamsActionUsingPopUpMenu";
        String absoluteWLPPath = Paths.get(projectsPath, smMPProjectName, getWLPInstallPath()).toString();

        // Trigger the start with parameters configuration dialog.
        UIBotTestUtils.runLibertyTWActionFromMenuView(remoteRobot, smMPProjectName, "Liberty: Start...");

        // Run the configuration dialog.
        UIBotTestUtils.runStartParamsConfigDialog(remoteRobot, null);

        try {
            // Validate that the application started.
            String url = smMPAppBaseURL + "api/resource";
            TestUtils.validateAppStarted(testName, url, smMPAppOutput, absoluteWLPPath);
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Stop dev mode.
                UIBotTestUtils.runLibertyTWActionFromMenuView(remoteRobot, smMPProjectName, "Liberty: Stop");

                // Validate that the server stopped.
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
            }
        }
    }

    /**
     * Tests Liberty tool window start/runTests/stop actions run from the project's pop-up action menu.
     */
    @Test
    @Disabled("Until https://github.com/OpenLiberty/liberty-tools-intellij/issues/272 is fixed.")
    public void testRunTestsActionUsingPopUpMenu() {
        String testName = "testRunTestsActionUsingPopUpMenu";
        String absoluteWLPPath = Paths.get(projectsPath, smMPProjectName, getWLPInstallPath()).toString();

        // Delete any existing test report files.
        deleteTestReports();

        // Start dev mode.
        UIBotTestUtils.runLibertyTWActionFromMenuView(remoteRobot, smMPProjectName, "Liberty: Start");

        try {
            // Validate that the application started.
            String url = smMPAppBaseURL + "api/resource";
            TestUtils.validateAppStarted(testName, url, smMPAppOutput, absoluteWLPPath);

            // Run the application's tests.
            UIBotTestUtils.runLibertyTWActionFromMenuView(remoteRobot, smMPProjectName, "Liberty: Run tests");

            // Validate that the reports were generated.
            validateTestReportsExist();
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Stop dev mode.
                UIBotTestUtils.runLibertyTWActionFromMenuView(remoteRobot, smMPProjectName, "Liberty: Stop");

                // Validate that the server stopped.
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
            }
        }
    }

    /**
     * Tests start.../stop actions run from the search everywhere panel.
     */
    @Test
    @Disabled("Until https://github.com/OpenLiberty/liberty-tools-intellij/issues/272 is fixed.")
    public void testStartWithParamsActionUsingSearch() {
        String testName = "testStartWithParamsActionUsingSearch";
        String absoluteWLPPath = Paths.get(projectsPath, smMPProjectName, getWLPInstallPath()).toString();

        // Trigger the start with parameters configuration dialog.
        UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Start...");

        // Run the configuration dialog.
        UIBotTestUtils.runStartParamsConfigDialog(remoteRobot, null);

        try {
            // Validate that the application started.
            String url = smMPAppBaseURL + "api/resource";
            TestUtils.validateAppStarted(testName, url, smMPAppOutput, absoluteWLPPath);
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Stop dev mode.
                UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Stop");

                // Validate that the server stopped.
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
            }
        }
    }

    /**
     * Tests start/runTests/stop actions run from the search everywhere panel .
     */
    @Test
    @Disabled("Until https://github.com/OpenLiberty/liberty-tools-intellij/issues/272 is fixed.")
    public void testRunTestsActionUsingSearch() {
        String testName = "testRunTestsActionUsingSearch";
        String absoluteWLPPath = Paths.get(projectsPath, smMPProjectName, getWLPInstallPath()).toString();

        // Delete any existing test report files.
        deleteTestReports();

        // Start dev mode.
        UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Start");

        try {
            // Validate that the application started.
            String url = smMPAppBaseURL + "api/resource";
            TestUtils.validateAppStarted(testName, url, smMPAppOutput, absoluteWLPPath);

            // Run the application's tests.
            UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Run tests");

            // Validate that the reports were generated.
            validateTestReportsExist();
        } finally {
            if (TestUtils.isServerStopNeeded(absoluteWLPPath)) {
                // Stop dev mode.
                UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Stop");

                // Validate that the server stopped.
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath);
            }
        }
    }

    /**
     * Tests the Add/Remove project from the tool window actions run from the search everywhere panel .
     */
    @Test
    @Video
    public void testManualAppAddRemoveActionUsingSearch() {
        // Import the project that is not automatically detected by Liberty Tools.
        UIBotTestUtils.importProject(remoteRobot, projectsPath, smNLTRestProjectName);

        // Open the dashboard and wait for the project to complete indexing.
        UIBotTestUtils.openLibertyToolWindow(remoteRobot);

        // Validate that the project tree is showing. Note that indexing may start at any time
        // and automatically remove the existing content from the window (project/message/etc.).
        TestUtils.sleepAndIgnoreException(10);
        UIBotTestUtils.waitForLTWNoProjectDetectedMsg(remoteRobot, "300");

        try {
            // Add the project to the Liberty tool window.
            UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Add project to the tool window");

            // Select project from the 'Add Liberty project' dialog.
            UIBotTestUtils.selectProjectFromAddLibertyProjectDialog(remoteRobot, smNLTRestProjectName);

            try {
                // Validate that the project is displayed in the Liberty tool window.
                UIBotTestUtils.findProjectInLibertyToolWindow(remoteRobot, smNLTRestProjectName, "10");
            } finally {
                // Remove the application from the Liberty tool window.
                UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Remove project from the tool window");

                // Select project from the 'Remote Liberty project' dialog.
                UIBotTestUtils.selectProjectFromRemoveLibertyProjectDialog(remoteRobot, smNLTRestProjectName);

                // Answer the 'Remove Liberty project' dialog in the affirmative.
                UIBotTestUtils.respondToRemoveProjectQueryDialog(remoteRobot);

                // Validate that the project was removed. An exception is expected.
                try {
                    for (int i = 0; i < 5; i++) {
                        UIBotTestUtils.findProjectInLibertyToolWindow(remoteRobot, smNLTRestProjectName, "2");
                        try {
                            Thread.sleep(2000);
                        } catch (Exception ee) {
                            // Nothing to do. Continue to the next iteration.
                        }
                    }

                    fail("Project " + smNLTRestProjectName + " is still present in the Liberty tool window despite it being removed.");
                } catch (Exception e) {
                    // The project was not found. Success.
                }
            }
        } finally {
            // Import the original project.
            Exception error = null;
            int maxRetries = 3;
            for (int i = 0; i < maxRetries; i++) {
                error = null;
                try {
                    UIBotTestUtils.importProject(remoteRobot, projectsPath, smMPProjectName);
                    UIBotTestUtils.expandLibertyToolWindowProjectTree(remoteRobot, smMPProjectName);
                    break;
                } catch (Exception e) {
                    // There are instances in which the import actions are successful, but the
                    // project view never comes up. Instead, the welcome view is shown. If that is the
                    // case retry.
                    error = e;
                }
            }

            if (error != null) {
                fail("Unable to open project " + smMPProjectName, error);
            }
        }
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
        remoteRobot.find(WelcomeFrameFixture.class, Duration.ofMinutes(2));
        UIBotTestUtils.importProject(remoteRobot, projectPath, projectName);
        UIBotTestUtils.openProjectView(remoteRobot);
        UIBotTestUtils.openLibertyToolWindow(remoteRobot);
        UIBotTestUtils.validateLibertyTWProjectTreeItemIsShowing(remoteRobot, projectName);
        UIBotTestUtils.expandLibertyToolWindowProjectTree(remoteRobot, projectName);

        // Close all open editors.
        // The expansion of the project tree in the Liberty tool window causes the editor tab for
        // the project's build file to open. That is the result of clicking on the project to give it
        // focus. The action of clicking on the project causes the build file to be opened automatically.
        // Closing the build file editor here prevents it from opening automatically when the project
        // in the Liberty tool window is clicked or right-clicked again. This is done on purpose to
        // prevent false negative tests related to the build file editor tab.
        UIBotTestUtils.closeAllEditorTabs(remoteRobot);

        TestUtils.printTrace(TestUtils.TraceSevLevel.INFO,
                "prepareEnv. Exit. ProjectName: " + projectName);
    }

    /**
     * Returns the path where the Liberty server was installed.
     *
     * @return The path where the Liberty server was installed.
     */
    public abstract String getWLPInstallPath();

    /**
     * Returns the name of the build file used by the project.
     *
     * @return The name of the build file used by the project.
     */
    public abstract String getBuildFileName();

    /**
     * Returns the name of the custom action command used to open the build file.
     *
     * @return The name of the custom action command used to open the build file.
     */
    public abstract String getBuildFileOpenCommand();

    /**
     * Deletes test reports.
     */
    public abstract void deleteTestReports();

    /**
     * Validates that test reports were generated.
     */
    public abstract void validateTestReportsExist();
}