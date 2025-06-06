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

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.*;
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText;
import com.intellij.remoterobot.search.locators.Locator;
import com.intellij.remoterobot.search.locators.Locators;
import com.intellij.remoterobot.utils.Keyboard;
import com.intellij.remoterobot.utils.RepeatUtilsKt;
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException;
import io.openliberty.tools.intellij.it.fixtures.DialogFixture;
import io.openliberty.tools.intellij.it.fixtures.ProjectFrameFixture;
import io.openliberty.tools.intellij.it.fixtures.WelcomeFrameFixture;
import org.assertj.swing.core.MouseButton;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static com.intellij.remoterobot.fixtures.dataExtractor.TextDataPredicatesKt.contains;
import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static java.awt.event.KeyEvent.*;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * UI helper function.
 */
public class UIBotTestUtils {

    /**
     * Print destination types.
     */
    public enum PrintTo {
        STDOUT, FILE
    }

    /**
     * server.xml Language server insertion types.
     */
    public enum InsertionType {
        FEATURE, ELEMENT
    }

    /**
     * UI Popup window types.
     */
    public enum PopupType {
        DOCUMENTATION, DIAGNOSTIC
    }

    /**
     * UI Frames.
     */
    public enum Frame {
        WELCOME, PROJECT
    }

    /**
     * Execution mode.
     */
    public enum ExecMode {
        DEBUG, RUN
    }

    /**
     * Action processing type.
     */
    public enum ActionExecType {
        SEARCH, LTWDROPDOWN, LTWPLAY, LTWPOPUP
    }

    /**
     * Liberty configuration entries.
     */
    public enum ConfigEntries {
        NAME, LIBERTYPROJ, PARAMS
    }

    /**
     * Imports a project using the IntelliJ API.
     *
     * @param remoteRobot  The RemoteRobot instance.
     * @param projectsPath The absolute path to the directory containing the projects.
     * @param projectName  The name of the project to be imported.
     */
    public static void importProject(RemoteRobot remoteRobot, String projectsPath, String projectName) {
        String projectFullPath = Paths.get(projectsPath, projectName).toAbsolutePath().toString().replace("\\", "/");

        remoteRobot.runJs("""
                importClass(com.intellij.openapi.application.ApplicationManager);
                importClass(com.intellij.ide.impl.ProjectUtil);
                
                const path = new java.io.File("%s").toPath();
                            const openProject = new Runnable({
                                run: function() {
                                    ProjectUtil.openOrImport(path.toString(), null, false);
                                }
                            });
                
                ApplicationManager.getApplication().invokeLater(openProject);
                """.formatted(projectFullPath));

        // Wait for the project frame to open, and make sure a few basic UI items are showing.
        // Note that at specific points in time, the window pane items will re-arrange themselves
        // as content is displayed. This, has an effect on the location of the items on the frame.
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofMinutes(2));

        // Ensure Liberty button is available (or any other validation that project is fully loaded)
        ComponentFixture wpStripeButton = projectFrame.getStripeButton("Liberty", "60");
        RepeatUtilsKt.waitFor(Duration.ofSeconds(30),
                Duration.ofSeconds(1),
                "Waiting for the Liberty button on the main window pane stripe to be enabled",
                "The Liberty button on then main window pane stripe is not enabled",
                () -> projectFrame.isComponentEnabled(wpStripeButton));
    }

    /**
     * Returns the dialog that queries the user for the location where the project is to be opened.
     *
     * @return The dialog that queries the user for the location where the project is to be opened.
     */
    public static DialogFixture getOpenProjectLocationDialog(CommonContainerFixture baseFixture) {
        DialogFixture openProjectDialog;
        try {
            openProjectDialog = baseFixture.find(DialogFixture.class, DialogFixture.byTitle("New Project"), Duration.ofSeconds(5));
        } catch (WaitForConditionTimeoutException e) {
            openProjectDialog = baseFixture.find(DialogFixture.class, DialogFixture.byTitle("Open Project"), Duration.ofSeconds(5));
        }

        return openProjectDialog;
    }

    /**
     * Closes the project frame.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void closeProjectFrame(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));

        if (remoteRobot.isMac()) {
            // Click on File on the Menu bar.
            projectFrame.clickOnMainMenuWithActions(remoteRobot, "File", "Close Project");
        }
        else {
            // minimize windows os intellij ide to default state
            if (remoteRobot.isWin()) {
                minimizeWindow(remoteRobot);
            }
            clickOnMainMenu(remoteRobot);
            // Click on File on the Menu bar.
            ComponentFixture fileMenuEntry = projectFrame.getActionMenu("File", "10");
            fileMenuEntry.moveMouse();

            // Click on Close Project in the menu.
            ComponentFixture closeFixture = projectFrame.getActionMenuItem("Close Project");
            closeFixture.click();
        }
    }

    /**
     * Runs a Liberty tool window action using the drop-down tree view.
     *
     * @param remoteRobot   The RemoteRobot instance.
     * @param action        The action to run
     * @param usePlayButton The indicator that specifies if play button should be used to run the action or not.
     */
    public static void runLibertyActionFromLTWDropDownMenu(RemoteRobot remoteRobot, String action, String projectName, boolean usePlayButton, int maxRetries) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));

        // Click on the Liberty toolbar to give it focus.
        ComponentFixture libertyTWBar = projectFrame.getBaseLabel("Liberty", "10");
        libertyTWBar.click();

        // Process the action.
        Exception error = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                error = null;
                ComponentFixture treeFixture = projectFrame.getTree("LibertyTree", action, "60");
                RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                        Duration.ofSeconds(2),
                        "Waiting for " + action + " in tree fixture to show and come into focus",
                        "Action " + action + " in tree fixture is not showing or not in focus",
                        treeFixture::isShowing);

                List<RemoteText> rts = treeFixture.findAllText();
                // Add a flag variable to confirm that the Liberty action is selecting exactly the same option under the specified project.
                // Otherwise, the robot will click on the first appearance of the Liberty action text.
                boolean flag = false;
                for (RemoteText rt : rts) {
                    if (projectName.equals(rt.getText())) {
                        flag = true;
                    }
                    if (action.equals(rt.getText()) && flag) {
                        if (usePlayButton) {
                            rt.click();
                            clickOnLibertyTWToolbarPlayButton(remoteRobot);
                        } else {
                            rt.doubleClick();
                        }
                        break;
                    }
                }

                // If the Start... action was selected, make sure the Edit Configuration dialog is displayed.
                if (action.equals("Start...")) {
                    // Finding the dialog may take a quite some time on Windows.
                    // This call will fail if the expected dialog is not displayed.
                    projectFrame.find(DialogFixture.class, DialogFixture.byTitle("Edit Configuration"), Duration.ofSeconds(30));
                }

                break;
            } catch (Exception e) {
                // Catch indexing related issues that make the Liberty tool window content disappear,
                // or invalidate the tree fixture that was previously obtained.
                // For example, this may cause errors stating:
                // "component must be showing on the screen to determine its location"
                error = e;
                TestUtils.printTrace(TestUtils.TraceSevLevel.INFO,
                        "Failed to process the " + action + " action using Liberty tool window drop down (" + e.getMessage() + "). Retrying...");
                TestUtils.sleepAndIgnoreException(5);
            }
        }

        // Report the last error if there is one.
        if (error != null) {
            throw new RuntimeException("Unable to run the " + action + " action from Liberty Tool window project dropdown.", error);
        }
    }

    /**
     * Runs a Liberty tool window action using the pop-up action menu.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param projectName The name of the project.
     * @param action      The action to run.
     */
    public static void runActionLTWPopupMenu(RemoteRobot remoteRobot, String projectName, String action, int maxRetries) {
        Exception error = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                error = null;
                RemoteText project = findProjectInLibertyToolWindow(remoteRobot, projectName, "60");
                project.rightClick();
                ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
                ComponentFixture menuAction = projectFrame.getActionMenuItem(action);
                menuAction.click();

                // If the Liberty: Start... action was selected, make sure the Edit Configuration dialog is displayed.
                if (action.equals("Liberty: Start...")) {
                    // Finding the dialog may take a quite some time on Windows.
                    // This call will fail if the expected dialog is not displayed.
                    projectFrame.find(DialogFixture.class, DialogFixture.byTitle("Edit Configuration"), Duration.ofSeconds(30));
                }

                break;
            } catch (Exception e) {
                error = e;
                TestUtils.printTrace(TestUtils.TraceSevLevel.INFO,
                        "Failed to run the " + action + " action using the Liberty tool window pop-up menu option (" + e.getMessage() + "). Retrying...");
                TestUtils.sleepAndIgnoreException(5);
            }
        }

        // Report the last error if there is one.
        if (error != null) {
            throw new RuntimeException("Failed to run the " + action + " action using theLiberty tool window pop-up menu option", error);
        }
    }

    /**
     * Returns the RemoteText object representing the project in the Liberty tool window.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param projectName The name of the project.
     * @return The RemoteText object representing the project in the Liberty tools window.
     */
    public static RemoteText findProjectInLibertyToolWindow(RemoteRobot remoteRobot, String projectName, String secsToWait) {
        RemoteText projectRootNode = null;
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        ComponentFixture treeFixture = projectFrame.getTree("LibertyTree", projectName, secsToWait);
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(2),
                "Waiting for project " + projectName + " to appear in the Liberty tool window",
                "Project " + projectName + " was not found in the Liberty tool window",
                treeFixture::isShowing);

        List<RemoteText> rts = treeFixture.findAllText();
        for (RemoteText rt : rts) {
            if (projectName.equals(rt.getText())) {
                projectRootNode = rt;
                break;
            }
        }

        if (projectRootNode == null) {
            fail("Project " + projectName + " was not found in Liberty tool window.");
        }

        return projectRootNode;
    }

    /**
     * Waits for the specified project tree item to appear in the Liberty tool window.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param treeItem    The name of tree item to look for.
     */
    public static boolean validateImportedProjectShowsInLTW(RemoteRobot remoteRobot, String treeItem) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));

        // The following comment refers to indexing but more recent changes wait for indexing to stop
        // There is a window between which the Liberty tool window content may show
        // and suddenly disappear when indexing starts. It is not known when indexing may start.
        // It can be immediate or take a few seconds (10+). Wait a bit for it to start.
        try {
            UIBotTestUtils.waitForLTWIndexingMsg(remoteRobot, 20);
        } catch (WaitForConditionTimeoutException wfcte) {
            // Indexing never started, or it completed. Proceed to validate
            // that the project is displayed in the Liberty tool window.
        }

        // Wait for the project to appear in the Liberty tool window. This line is sensitive to indexing and depends on the code that waits for indexing before continuing.
        ComponentFixture treeFixture = projectFrame.getTree("LibertyTree", treeItem, "10");
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(2),
                "Waiting for tree item" + treeItem + " to show in the Liberty tool window.",
                "Tree item " + treeItem + " did not show in Liberty tool window.",
                treeFixture::isShowing);
        return treeFixture.isShowing();
    }

    /**
     * Waits for the Welcome page, which is shown when the project frame is closed.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void validateProjectFrameClosed(RemoteRobot remoteRobot) {
        remoteRobot.find(WelcomeFrameFixture.class, Duration.ofMinutes(2));
    }

    /**
     * Open and validate the Liberty tool window is open
     */
    public static void openAndValidateLibertyToolWindow(RemoteRobot remoteRobot, String treeItem) {
        // Try multiple times in case the O/S is displaying a modal dialog that blocks the button.
        for (int i = 1; i <=3; i++) {
            try {
                UIBotTestUtils.openLibertyToolWindow(remoteRobot);
                if (UIBotTestUtils.validateImportedProjectShowsInLTW(remoteRobot, treeItem)) {
                    break;
                }
            } catch (Exception e) {
                // Any of the operations above could end up in an exception if the element is
                // not found etc. Wait and retry.
            }
            TestUtils.sleepAndIgnoreException(3);
        }
    }

    /**
     * Opens the Liberty tool window if it is not already open.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void openLibertyToolWindow(RemoteRobot remoteRobot) {
        TestUtils.printTrace(TestUtils.TraceSevLevel.INFO, "UIBotTestUtils.openLibertyToolWindow Entry");
        int maxRetries = 6;
        Exception error = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                TestUtils.sleepAndIgnoreException(10); // wait for UI to be rendered before searching for a button
                error = null;
                ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
                projectFrame.getBaseLabel("Liberty", "5");
                break;
            } catch (WaitForConditionTimeoutException wfcte) {
                // The Liberty tool window is closed. Open it.
                clickOnWindowPaneStripeButton(remoteRobot, "Liberty");
                // After clicking it can take seconds for the window to open on a slow cloud machine.
                // Important since this is in a loop and you may click twice if there is no sleep.
                TestUtils.sleepAndIgnoreException(5);
                break;
            } catch (Exception e) {
                // The project frame may hang for a bit while loading/processing work. Retry.
                TestUtils.printTrace(TestUtils.TraceSevLevel.INFO,
                        "Unable to open the Liberty tool window (" + e.getMessage() + "). Retrying...");
                TestUtils.sleepAndIgnoreException(10);
                error = e;
            }
        }

        // Report the last error if there is one.
        if (error != null) {
            throw new RuntimeException("Unable to open the Liberty tool window.", error);
        }
        TestUtils.printTrace(TestUtils.TraceSevLevel.INFO, "UIBotTestUtils.openLibertyToolWindow Exit");
    }

    /**
     * Closes the Liberty tool window if it is not already closed.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void closeLibertyToolWindow(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        try {
            projectFrame.getBaseLabel("Liberty", "2");
            clickOnWindowPaneStripeButton(remoteRobot, "Liberty");
        } catch (WaitForConditionTimeoutException e) {
            // The Liberty tool window is already closed. Nothing to do.
        }
    }

    /**
     * Opens the project tree view if it is not already opened.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void openProjectView(RemoteRobot remoteRobot) {
        // maximize windows os intellij ide to avoid failures accessing menu
        if (remoteRobot.isWin()) {
            maximizeWindow(remoteRobot);
        }
        TestUtils.printTrace(TestUtils.TraceSevLevel.INFO, "UIBotTestUtils.openProjectView Entry");
        int maxRetries = 6;
        Exception error = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                TestUtils.sleepAndIgnoreException(10); // wait for UI to be rendered before searching for a button
                error = null;
                ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
                projectFrame.getContentComboLabel("Project", "5");
                break;
            } catch (WaitForConditionTimeoutException wfcte) {
                // The project view is closed. Open it.
                clickOnWindowPaneStripeButton(remoteRobot, "Project");
                // After clicking it can take seconds for the window to open on a slow cloud machine.
                // Important since this is in a loop and you may click twice if there is no sleep.
                TestUtils.sleepAndIgnoreException(5);
                break;
            } catch (Exception e) {
                // The project frame may hang for a bit while loading/processing work. Retry.
                TestUtils.printTrace(TestUtils.TraceSevLevel.INFO,
                        "Unable to open the project tool window (" + e.getMessage() + "). Retrying...");
                TestUtils.sleepAndIgnoreException(10);
                error = e;
            }
        }

        // Report the last error if there is one.
        if (error != null) {
            throw new RuntimeException("Unable to open the project tool window.", error);
        }
        TestUtils.printTrace(TestUtils.TraceSevLevel.INFO, "UIBotTestUtils.openProjectView Exit");
    }

    /**
     * Closes the project tree view if it is not already closed.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void closeProjectView(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        try {
            projectFrame.getContentComboLabel("Project", "2");
            clickOnWindowPaneStripeButton(remoteRobot, "Project");
        } catch (WaitForConditionTimeoutException e) {
            // Project view is already closed. Nothing to do.
        }
    }

    /**
     * Opens the terminal window if it is not already open.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void openTerminalWindow(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(5));
        try {
            projectFrame.getBaseLabel("Terminal", "5");
        } catch (WaitForConditionTimeoutException e) {
            // The Liberty tool window is closed. Open it.
            clickOnWindowPaneStripeButton(remoteRobot, "Terminal");
        }
    }

    /**
     * Clicks on the specified tool window pane stripe.
     *
     * @param remoteRobot      The RemoteRobot instance.
     * @param StripeButtonName The name of the window pane stripe button.
     */
    public static void clickOnWindowPaneStripeButton(RemoteRobot remoteRobot, String StripeButtonName) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        ComponentFixture wpStripeButton = projectFrame.getStripeButton(StripeButtonName, "10");
        RepeatUtilsKt.waitFor(Duration.ofSeconds(30),
                Duration.ofSeconds(1),
                "Waiting for the " + StripeButtonName + " button on the main window pane stripe to be enabled",
                "The " + StripeButtonName + " button on then main window pane stripe is not enabled",
                () -> projectFrame.isComponentEnabled(wpStripeButton));
        wpStripeButton.click();
    }

    /**
     * Clicks on the expand action button on the Liberty tool window.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void expandLibertyToolWindowProjectTree(RemoteRobot remoteRobot, String projectName) {
        int maxRetries = 6;
        Exception error = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
                error = null;

                // Click on the Liberty tool window toolbar to give it focus.
                ComponentFixture LibertyTWBar = projectFrame.getBaseLabel("Liberty", "10");
                LibertyTWBar.click();

                // Expand the project tree to show the available actions.
                String xPath = "//div[@class='LibertyExplorer']//div[@class='ActionButton' and contains(@myaction.key, 'action.ExpandAll.text')]";
                ComponentFixture actionButton = projectFrame.getActionButton(xPath, "10");
                actionButton.click();

                // Click on the project node to give it focus. This action opens the editor tab showing
                // the build file.
                RemoteText projectRootNode = findProjectInLibertyToolWindow(remoteRobot, projectName, "10");
                projectRootNode.click();
                break;
            } catch (Exception e) {
                // The Liberty tool window content may blink in and out or hang. Retry.
                error = e;
                TestUtils.printTrace(TestUtils.TraceSevLevel.INFO,
                        "Unable to expand the Liberty tool window project tree (" + e.getMessage() + "). Retrying...");
                TestUtils.sleepAndIgnoreException(10);
            }
        }

        // Report the last error if there is one.
        if (error != null) {
            throw new RuntimeException("Unable to expand the Liberty tool window project tree.", error);
        }
    }

    /**
     * Opens the specified file located in directory <project>/src/main/liberty/config.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param projectName The name of the project that contains the file to open.
     * @param fileName    The name of the file to open.
     */
    public static void openConfigFile(RemoteRobot remoteRobot, String projectName, String fileName) {
        int maxRetries = 3;
        Exception error = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                error = null;

                // Click on File on the Menu bar.
                ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofMinutes(2));

                // hide the terminal window for now
                hideTerminalWindow(remoteRobot);

                // get a JTreeFixture reference to the file project viewer entry
                JTreeFixture projTree = projectFrame.getProjectViewJTree(remoteRobot, projectName);

                projTree.findText(fileName).doubleClick();
                break;
            } catch (Exception e) {
                error = e;
                TestUtils.printTrace(TestUtils.TraceSevLevel.INFO, "Unable to open file " + fileName + " (" + e.getMessage() + "). Retrying...");
                TestUtils.sleepAndIgnoreException(5);
            }
        }

        // Report the last error if there is one.
        if (error != null) {
            throw new RuntimeException("Unable to open file " + fileName, error);
        }
    }

    /**
     * Opens the specified file located in directory <project>/src/main/liberty/config.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param projectName The name of the project that contains the file to open.
     * @param fileName    The name of the file to open.
     */
    public static void openFile(RemoteRobot remoteRobot, String projectName, String fileName, String... filePath) {
        int maxRetries = 15;
        Exception error = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                error = null;
                TestUtils.printTrace(TestUtils.TraceSevLevel.INFO, "openFile: iteration = " + i);

                // find the Project Frame
                ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofMinutes(2));

                // hide the terminal window for now
                hideTerminalWindow(remoteRobot);

                // get a JTreeFixture reference to the file project viewer entry
                JTreeFixture projTree = projectFrame.getProjectViewJTree(remoteRobot, projectName);

                // expand project directories that are specific to this test app being used by these testcases
                // must be expanded here before trying to open specific

                projTree.expand(filePath);

                projTree.findText(fileName).doubleClick();
                TestUtils.printTrace(TestUtils.TraceSevLevel.INFO, "openFile: double clicked on file name");
                break;

            } catch (Exception e) {
                error = e;
                TestUtils.printTrace(TestUtils.TraceSevLevel.INFO, "Unable to open file " + fileName + " (" + e.getMessage() + "). Retrying...");
                TestUtils.sleepAndIgnoreException(2);
            }
        }

        // Report the last error if there is one.
        if (error != null) {
            throw new RuntimeException("Unable to open file " + fileName, error);
        }
    }
    
    public static void hideTerminalWindow(RemoteRobot remoteRobot) {
        try {
            ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(2));
            String xPath = "//div[@class='ToolWindowHeader'][.//div[@myaction.key='action.NewPredefinedSession.label']]//div[@myaction.key='tool.window.hide.action.name']";
            ComponentFixture hideActionButton = projectFrame.getActionButton(xPath, "10");
            hideActionButton.click();
        } catch (WaitForConditionTimeoutException e) {
            // not open, nothing to do, so proceed
        }
    }

    /**
     * Returns the editor tab close button for the specified editor tab name or null if one is not found within the
     * specified wait time.
     *
     * @param remoteRobot   The RemoteRobot instance.
     * @param editorTabName The name of the editor tab to close.
     * @param timeToWait    The time to wait for the editor tab close button.
     * @return Returns the editor tab close button for the specified editor tab name or null if one is not found within the
     * * specified wait time.
     */
    public static ComponentFixture getEditorTabCloseButton(RemoteRobot remoteRobot, String editorTabName, String timeToWait) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        ComponentFixture editorTabCloseButton = null;
        try {
            editorTabCloseButton = projectFrame.getInplaceButton(editorTabName, timeToWait);
        } catch (WaitForConditionTimeoutException e) {
            // The editor is most likely closed.
        }

        return editorTabCloseButton;
    }

    /**
     * Closes a file that is open in the editor pane.
     *
     * @param remoteRobot   The RemoteRobot instance.
     * @param editorTabName The name of the editor tab to close.
     * @param timeToWait    The time to wait for the editor tab close button.
     */
    public static void closeFileEditorTab(RemoteRobot remoteRobot, String editorTabName, String timeToWait) {
        ComponentFixture editorTabCloseButton = getEditorTabCloseButton(remoteRobot, editorTabName, timeToWait);
        if (editorTabCloseButton != null) {
            editorTabCloseButton.click();

            // Wait until the tab closes.
            RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                    Duration.ofSeconds(1),
                    "Waiting Editor tab " + editorTabName + " to close.",
                    "Editor tab " + editorTabName + " did not close.",
                    () -> getEditorTabCloseButton(remoteRobot, editorTabName, "1") == null);
        }
    }

    /**
     * Closes all opened editor tabs.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void closeAllEditorTabs(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        projectFrame.clickOnMainMenuWithActions(remoteRobot, "Window", "Editor Tabs", "Close All Tabs");
    }

    /**
     * Click on the Problems tab to open the Problems View
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void clickOnProblemsTab(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));

        try {
            String xPath = "//div[@text.key='toolwindow.stripe.Problems_View']";
            ComponentFixture actionButton = projectFrame.getActionButton(xPath, "10");
            actionButton.click();

        } catch (WaitForConditionTimeoutException e) {
            // Problems tab open, nothing to do
        }
    }

    /**
     * Click on the editor file tab for a file that is open in the editor pane to gain focus to that file
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param fileName    The string file name
     */

    public static void clickOnFileTab(RemoteRobot remoteRobot, String fileName) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));

        try {
            String xPath = "//div[@accessiblename='" + fileName + "' and @class='EditorTabLabel']";
            ComponentFixture actionButton = projectFrame.getActionButton(xPath, "10");
            actionButton.click();

        } catch (WaitForConditionTimeoutException e) {
            // file not open, nothing to do
        }
    }


    /**
     * Moves the mouse cursor to a specific string target in a liberty config file
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param hoverTarget The string to hover over in the config file
     * @param hoverFile   The string path to the config file
     * @param popupType   the type of popup window that is expected from the hover action
     */
    public static void hoverInAppServerCfgFile(RemoteRobot remoteRobot, String hoverTarget, String hoverFile, PopupType popupType) {

        Keyboard keyboard = new Keyboard(remoteRobot);

        Locator locator = byXpath("//div[@class='EditorCompositePanel']//div[@class='EditorComponentImpl']");
        clickOnFileTab(remoteRobot, hoverFile);
        EditorFixture editorNew = remoteRobot.find(EditorFixture.class, locator, Duration.ofSeconds(20));

        Exception error = null;
        for (int i = 0; i < 15; i++) {
            error = null;
            try {
                // move the cursor to the origin of the editor
                goToLineAndColumn(remoteRobot, keyboard, 1, 1);

                // Find the target text on the editor and move the move to it.
                editorNew.findText(contains(hoverTarget)).moveMouse();
                // clear and "lightbulb" icons?
                if (!hoverFile.equals("server.xml")) {
                    keyboard.hotKey(VK_ESCAPE);
                }

                // jitter the cursor
                Point p = editorNew.findText(contains(hoverTarget)).getPoint();

                // provoke the hint popup with a cursor jitter
                jitterCursor(editorNew, p.x, p.y);

                // first get the contents of the popup - put in a String
                ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
                switch (popupType.name()) {
                    case "DOCUMENTATION":
                        projectFrame.getDocumentationHintEditorPane();
                        break;
                    case "DIAGNOSTIC":
                        projectFrame.getDiagnosticPane();
                        break;
                    default:
                        // no known popup type provided, return
                        return;
                }

                break;
            } catch (WaitForConditionTimeoutException wftoe) {
                error = wftoe;
                TestUtils.sleepAndIgnoreException(10);
                // click on center of editor pane - allow hover to work on next attempt
                editorNew.click();
            }
        }

        // Report the last error if there is one.
        if (error != null) {
            throw new RuntimeException("Hover on text: " + hoverTarget + " did not trigger a pop-up window to open", error);
        }
    }

    /**
     * Moves the mouse cursor to a specific string target in an application file
     *
     * @param remoteRobot           The RemoteRobot instance.
     * @param hoverTarget           The string to hover over in the config file
     * @param hoverFile             The string path to the config file
     * @param quickfixChooserString the string to use when choosing a quickfix action
     */
    public static void hoverForQuickFixInAppFile(RemoteRobot remoteRobot, String hoverTarget, String hoverFile, String quickfixChooserString) {

        Keyboard keyboard = new Keyboard(remoteRobot);

        Locator locator = byXpath("//div[@class='EditorCompositePanel']//div[@class='EditorComponentImpl']");
        clickOnFileTab(remoteRobot, hoverFile);
        EditorFixture editorNew = remoteRobot.find(EditorFixture.class, locator, Duration.ofSeconds(20));
        Point originPt = new Point(1, 1);

        Exception error = null;
        for (int i = 0; i < 15; i++) {
            error = null;
            try {
                // move the cursor to the origin of the editor
                goToLineAndColumn(remoteRobot, keyboard, 1, 1);

                editorNew.click(originPt);

                // Find the target text on the editor and move the move to it.
                editorNew.findText(contains(hoverTarget)).moveMouse();

                // jitter the cursor
                Point p = editorNew.findText(contains(hoverTarget)).getPoint();

                // provoke the hint popup with a cursor jitter
                jitterCursor(editorNew, p.x, p.y);

                // first get the contents of the popup - put in a String
                ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));

                projectFrame.getDiagnosticPane();
                ContainerFixture quickFixPopupLink = projectFrame.getQuickFixMoreActionsLink();
                quickFixPopupLink.click();

                ContainerFixture quickFixPopup = projectFrame.getQuickFixPane();

                RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                        Duration.ofSeconds(2),
                        "Waiting for the The quickfix popup to contain " + quickfixChooserString,
                        "The quickfix popup did not contain " + quickfixChooserString,
                        () -> quickFixPopup.hasText(quickfixChooserString));

                break;
            } catch (WaitForConditionTimeoutException wftoe) {
                error = wftoe;
                TestUtils.sleepAndIgnoreException(5);
                // click on upper left corner of editor pane - allow hover to work on next attempt
                editorNew.click(originPt);
            }
        }

        // Report the last error if there is one.
        if (error != null) {
            throw new RuntimeException("Hover on text: '" + hoverTarget + "' did not trigger a pop-up window to open", error);
        }
    }

    public static void jitterCursor(EditorFixture editor, int pointX, int pointY) {

        String jitterScript = "const x = %d;" +
                "const y = %d;" +
                "java.util.List.of(5, 20, 5, 15).forEach((i)=> {" +
                "const point = new Point(x + i, y);" +
                "robot.moveMouse(component, point);})";

        // run the jitter mouse script remotely in the idea
        editor.runJs(String.format(jitterScript, pointX, pointY));
    }

    /**
     * insert a snippet into a source part using a completion
     *
     * @param remoteRobot          The RemoteRobot instance.
     * @param fileName             The string to hover over in the config file
     * @param snippetSubString     The string path to the config file
     * @param snippetChooserString the string to use when choosing a quickfix action
     */
    public static void insertCodeSnippetIntoSourceFile(RemoteRobot remoteRobot, String fileName, String snippetSubString, String snippetChooserString) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(30));
        clickOnFileTab(remoteRobot, fileName);
        EditorFixture editorNew = remoteRobot.find(EditorFixture.class, EditorFixture.Companion.getLocator());
        editorNew.click();

        Exception error = null;

        for (int i = 0; i < 10; i++) {
            error = null;
            try {
                Keyboard keyboard = new Keyboard(remoteRobot);
                // find the location in the file to begin the stanza insertion
                // since we know this is a new empty file, go to position 1,1
                goToLineAndColumn(remoteRobot, keyboard, 1, 1);

                keyboard.enterText(snippetSubString);

                // Select the appropriate completion suggestion in the pop-up window that is automatically
                // opened as text is typed. Avoid hitting ctrl + space as it has the side effect of selecting
                // and entry automatically if the completion suggestion windows has one entry only.
                ComponentFixture namePopupWindow = projectFrame.getLookupList();
                RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                        Duration.ofSeconds(1),
                        "Waiting for text " + snippetSubString + " to appear in the completion suggestion pop-up window",
                        "Text " + snippetSubString + " did not appear in the completion suggestion pop-up window",
                        () -> namePopupWindow.hasText(snippetSubString));

                namePopupWindow.findText(contains(snippetChooserString)).doubleClick();

                // let the auto-save function of intellij save the file before testing it
                if (remoteRobot.isMac()) {
                    keyboard.hotKey(VK_META, VK_S);
                } else {
                    // linux + windows
                    keyboard.hotKey(VK_CONTROL, VK_S);
                }
                break;
            } catch (WaitForConditionTimeoutException wftoe) {
                error = wftoe;

                // The source content may have been corrupted in the process. Replace it before re-trying it.
                UIBotTestUtils.clearWindowContent(remoteRobot);
                TestUtils.sleepAndIgnoreException(2);
            }
        }

        // Report the last error if there is one.
        if (error != null) {
            throw new RuntimeException("Unable to insert entry in config file : " + fileName + " using text: " + snippetSubString, error);
        }
    }

    /**
     * Inserts a configuration name value pair into a config file via text typing
     * and popup menu completion (if required)
     *
     * @param remoteRobot              The RemoteRobot instance.
     * @param fileName                 The string path to the config file
     * @param configNameSnippet        the portion of the name to type
     * @param configNameChooserSnippet the portion of the name to use for selecting from popup menu
     * @param configValueSnippet       the value to type into keyboard - could be a snippet or a whole word
     * @param completeWithPopup        use popup to complete value selection or type in an entire provided value string
     */
    public static void insertConfigIntoMPConfigPropertiesFile(RemoteRobot remoteRobot, String fileName, String configNameSnippet, String configNameChooserSnippet, String configValueSnippet, boolean completeWithPopup) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(30));
        clickOnFileTab(remoteRobot, fileName);
        EditorFixture editorNew = remoteRobot.find(EditorFixture.class, EditorFixture.Companion.getLocator());
        Exception error = null;

        editorNew.click();

        for (int i = 0; i < 10; i++) {
            error = null;
            try {
                Keyboard keyboard = new Keyboard(remoteRobot);
                // find the location in the file to begin the stanza insertion
                // we will put new config at the end of the config file
                // (after the last line already in the file)
                keyboard.hotKey(VK_CONTROL, VK_END);
                keyboard.enter();

                keyboard.enterText(configNameSnippet);

                // Narrow down the config name completion suggestions in the pop-up window that is automatically
                // opened as text is typed based on the value of configNameSnippet. Avoid hitting ctrl + space as it has the side effect of selecting
                // and entry automatically if the completion suggestion windows has one entry only.
                ComponentFixture namePopupWindow = projectFrame.getLookupList();
                RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                        Duration.ofSeconds(1),
                        "Waiting for text " + configNameSnippet + " to appear in the completion suggestion pop-up window",
                        "Text " + configNameSnippet + " did not appear in the completion suggestion pop-up window",
                        () -> namePopupWindow.hasText(configNameSnippet));

                // now choose the specific item based on the chooser string
                namePopupWindow.findText(contains(configNameChooserSnippet)).doubleClick();

                editorNew.findText("false").doubleClick();
                keyboard.hotKey(VK_DELETE);

                keyboard.enterText(configValueSnippet);

                if (completeWithPopup) {
                    // Select the appropriate value completion suggestion in the pop-up window that is automatically
                    // opened as text is typed. Avoid hitting ctrl + space as it has the side effect of selecting
                    // and entry automatically if the completion suggestion windows has one entry only.
                    ComponentFixture valuePopupWindow = projectFrame.getLookupList();
                    RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                            Duration.ofSeconds(1),
                            "Waiting for text " + configValueSnippet + " to appear in the completion suggestion pop-up window",
                            "Text " + configValueSnippet + " did not appear in the completion suggestion pop-up window",
                            () -> valuePopupWindow.hasText(configValueSnippet));

                    valuePopupWindow.findText(contains(configValueSnippet)).doubleClick();
                }
                // let the auto-save function of intellij save the file before testing it
                if (remoteRobot.isMac()) {
                    keyboard.hotKey(VK_META, VK_S);
                } else {
                    // linux + windows
                    keyboard.hotKey(VK_CONTROL, VK_S);
                }
                break;
            } catch (WaitForConditionTimeoutException wftoe) {
                error = wftoe;

                // The server.xml content may have been corrupted in the process. Replace it before re-trying it.
                UIBotTestUtils.pasteOnActiveWindow(remoteRobot);
                TestUtils.sleepAndIgnoreException(2);
            }
        }

        // Report the last error if there is one.
        if (error != null) {
            throw new RuntimeException("Unable to insert entry in config file : " + fileName + " using text: " + configNameSnippet, error);
        }
    }

    /**
     * Inserts a configuration name value pair into a config file via text typing
     * and popup menu completion (if required)
     *
     * @param remoteRobot                   The RemoteRobot instance.
     * @param fileName                      The string path to the config file
     * @param configNameSnippetCaseSpecific the portion of the name to type
     * @param configNameChooserSnippet      the portion of the name to use for selecting from popup menu
     * @param configValueSnippet            the value to type into keyboard - could be a snippet or a whole word
     * @param completeWithPopup             use popup to complete value selection or type in an entire provided value string
     */
    public static void insertConfigIntoConfigFile(RemoteRobot remoteRobot, String fileName, String configNameSnippetCaseSpecific, String configNameChooserSnippet, String configValueSnippet, boolean completeWithPopup) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(30));
        clickOnFileTab(remoteRobot, fileName);
        EditorFixture editorNew = remoteRobot.find(EditorFixture.class, EditorFixture.Companion.getLocator());
        Exception error = null;

        String configNameSnippet = "";
        if (fileName.equals("server.env")) {
            configNameSnippet = configNameSnippetCaseSpecific.toUpperCase(java.util.Locale.ROOT);
        }
        else if (fileName.equals("bootstrap.properties")) {
            configNameSnippet = configNameSnippetCaseSpecific.toLowerCase(java.util.Locale.ROOT);
        }
        else {
            configNameSnippet = configNameSnippetCaseSpecific;
        }
        String configValueSnippetUpperCase = configValueSnippet.toUpperCase(java.util.Locale.ROOT);

        for (int i = 0; i < 10; i++) {
            error = null;
            try {
                editorNew.click();
                Keyboard keyboard = new Keyboard(remoteRobot);
                // find the location in the file to begin the stanza insertion
                // we will put new config at the end of the config file
                // (after the last line already in the file)
                keyboard.hotKey(VK_CONTROL, VK_END);
                keyboard.enter();

                keyboard.enterText(configNameSnippetCaseSpecific);
                // After typing it can take 1 or 2s for IntelliJ to render diagnostics etc. Must wait before continuing.
                TestUtils.sleepAndIgnoreException(5);

                // Narrow down the config name completion suggestions in the pop-up window that is automatically
                // opened as text is typed based on the value of configNameSnippet. Avoid hitting ctrl + space as it has the side effect of selecting
                // and entry automatically if the completion suggestion windows has one entry only.
                ComponentFixture namePopupWindow = projectFrame.getLookupList();
                String finalConfigNameSnippet = configNameSnippet;
                RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                        Duration.ofSeconds(1),
                        "Waiting for text " + configNameSnippet + " to appear in the completion suggestion pop-up window",
                        "Text " + configNameSnippet + " did not appear in the completion suggestion pop-up window",
                        () -> namePopupWindow.hasText(finalConfigNameSnippet));

                // now choose the specific item based on the chooser string
                namePopupWindow.findText(contains(configNameChooserSnippet)).doubleClick();

                keyboard.hotKey(VK_END);
                keyboard.enterText("=");
                keyboard.hotKey(VK_END);

                keyboard.enterText(configValueSnippet);
                // After typing it can take 1 or 2s for IntelliJ to render diagnostics etc. Must wait before continuing.
                TestUtils.sleepAndIgnoreException(5);

                if (completeWithPopup) {
                    // Select the appropriate value completion suggestion in the pop-up window that is automatically
                    // opened as text is typed. Avoid hitting ctrl + space as it has the side effect of selecting
                    // and entry automatically if the completion suggestion windows has one entry only.
                    ComponentFixture valuePopupWindow = projectFrame.getLookupList();
                    RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                            Duration.ofSeconds(1),
                            "Waiting for text " + configValueSnippetUpperCase + " to appear in the completion suggestion pop-up window",
                            "Text " + configValueSnippetUpperCase + " did not appear in the completion suggestion pop-up window",
                            () -> valuePopupWindow.hasText(configValueSnippetUpperCase));

                    valuePopupWindow.findText(contains(configValueSnippetUpperCase)).doubleClick();
                }
                // let the auto-save function of intellij save the file before testing it
                if (remoteRobot.isMac()) {
                    keyboard.hotKey(VK_META, VK_S);
                } else {
                    // linux + windows
                    keyboard.hotKey(VK_CONTROL, VK_S);
                }
                break;
            } catch (WaitForConditionTimeoutException wftoe) {
                error = wftoe;

                // The server.xml content may have been corrupted in the process. Replace it before re-trying it.
                UIBotTestUtils.pasteOnActiveWindow(remoteRobot);
                TestUtils.sleepAndIgnoreException(2);
            }
        }

        // Report the last error if there is one.
        if (error != null) {
            throw new RuntimeException("Unable to insert entry in config file : " + fileName + " using text: " + configNameSnippetCaseSpecific, error);
        }
    }

    /**
     * Inserts content to server.xml. Callers are required to have done a UI copy of the server.xml
     * content prior to calling this method.
     *
     * @param remoteRobot       The RemoteRobot instance.
     * @param stanzaSnippet     truncated feature name to be used to select full name from popup
     * @param line              line number (in editor) to place cursor for text insertion in server.xml
     * @param col               column number (in editor) to place cursor for text insertion in server.xml
     * @param type              the type of stanza being inserted - FEATURE or CONFIG
     * @param completeWithPopup use the popup to complete the insertion (or the full text will be typed in)
     */
    public static void insertStanzaInAppServerXML(RemoteRobot remoteRobot, String stanzaSnippet, int line, int col, InsertionType type, boolean completeWithPopup) {

        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(30));
        clickOnFileTab(remoteRobot, "server.xml");
        Locator locator = byXpath("//div[@class='EditorCompositePanel']//div[@class='EditorComponentImpl']");
        EditorFixture editorNew = remoteRobot.find(EditorFixture.class, locator, Duration.ofSeconds(20));
        editorNew.click();

        Keyboard keyboard = new Keyboard(remoteRobot);
        Exception error = null;

        for (int i = 0; i < 3; i++) {
            error = null;
            try {
                // find the location in the file to begin the stanza insertion
                goToLineAndColumn(remoteRobot, keyboard, line, col);

                if (type.name().equals("FEATURE")) {
                    String textToFind = "feature";
                    // if this is a feature stanza, hit enter to place it on the next line
                    // in the featureManager Block
                    keyboard.hotKey(VK_ENTER);

                    // Add the feature element using completion.
                    goToLineAndColumn(remoteRobot, keyboard, line + 1, col);
                    keyboard.hotKey(VK_CONTROL, VK_SPACE);

                    ContainerFixture popupWindow = projectFrame.getLookupList();
                    RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                            Duration.ofSeconds(1),
                            "Waiting for text " + textToFind + " to appear in the completion suggestion pop-up window",
                            "Text " + textToFind + " did not appear in the completion suggestion pop-up window",
                            () -> popupWindow.hasText(textToFind));

                    popupWindow.findText(textToFind).doubleClick();

                    // get the full text of the editor contents
                    String editorText = editorNew.getText();

                    // find the location to click on for feature name entry
                    // this should put the entry cursor directly between <feature> and <feature/>
                    int offset = editorText.indexOf("<feature></feature>") + "<feature>".length();

                    editorNew.clickOnOffset(offset, MouseButton.LEFT_BUTTON, 1);

                    // small delay to allow the button to be fully clicked before typing
                    TestUtils.sleepAndIgnoreException(1);
                }

                // For either a FEATURE or a CONFIG stanza, insert where the cursor is currently located.
                // In Windows OS, text entry into a file is much faster compared to other operating systems, so adding some delays between each character helps ensure proper LS requests and responses.
                if (remoteRobot.isWin()) {
                    keyboard.enterText(stanzaSnippet, 200);
                }
                else {
                    keyboard.enterText(stanzaSnippet);
                }

                if (completeWithPopup) {
                    // Select the appropriate completion suggestion in the pop-up window that is automatically
                    // opened as text is typed. Avoid hitting ctrl + space as it has the side effect of selecting
                    // and entry automatically if the completion suggestion windows has one entry only.
                    ComponentFixture completionPopupWindow = projectFrame.getLookupList();
                    RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                            Duration.ofSeconds(1),
                            "Waiting for text " + stanzaSnippet + " to appear in the completion suggestion pop-up window",
                            "Text " + stanzaSnippet + " did not appear in the completion suggestion pop-up window",
                            () -> completionPopupWindow.hasText(stanzaSnippet));

                    completionPopupWindow.findText(stanzaSnippet).doubleClick();
                }

                // Save the file.
                if (remoteRobot.isMac()) {
                    keyboard.hotKey(VK_META, VK_S);
                } else {
                    // linux + windows
                    keyboard.hotKey(VK_CONTROL, VK_S);
                }

                break;
            } catch (WaitForConditionTimeoutException wftoe) {
                error = wftoe;

                // The server.xml content may have been corrupted in the process. Replace it before re-trying it.
                UIBotTestUtils.pasteOnActiveWindow(remoteRobot);
                TestUtils.sleepAndIgnoreException(2);
            }
        }

        // Report the last error if there is one.
        if (error != null) {
            throw new RuntimeException("Unable to insert entry in server.xml using text: " + stanzaSnippet, error);
        }
    }

    /**
     * Deletes a string of text from the currently focused editor
     *
     * @param remoteRobot  The RemoteRobot instance.
     * @param textToDelete The string to delete
     */
    public static void selectAndDeleteTextInJavaPart(RemoteRobot remoteRobot, String fileName, String textToDelete) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(30));
        clickOnFileTab(remoteRobot, fileName);
        Locator locator = byXpath("//div[@class='EditorCompositePanel']//div[@class='EditorComponentImpl']");
        EditorFixture editorNew = remoteRobot.find(EditorFixture.class, locator, Duration.ofSeconds(20));
        editorNew.click();

        Keyboard keyboard = new Keyboard(remoteRobot);

        // find the location to click on for feature name entry
        // this should put the entry cursor directly between <feature> and <feature/>
        //int offset = editorText.indexOf(textToDelete);
        editorNew.click();
        editorNew.selectText(textToDelete);
        keyboard.hotKey(VK_DELETE);


        // save the new content
        if (remoteRobot.isMac()) {
            keyboard.hotKey(VK_META, VK_S);
        } else {
            // linux + windows
            keyboard.hotKey(VK_CONTROL, VK_S);
        }

        // slight delay to allow the diagnotic/quick fix data to arrive?
        TestUtils.sleepAndIgnoreException(2);
    }

    /**
     * Deletes a string of text from the currently focused editor
     *
     * @param remoteRobot      The RemoteRobot instance.
     * @param fileName         The fileName to modify
     * @param textToModify     The string to modify
     * @param modificationText The string to use for modification
     */
    public static void selectAndModifyTextInJavaPart(RemoteRobot remoteRobot, String fileName, String textToModify, String modificationText){
        clickOnFileTab(remoteRobot, fileName);
        Locator locator = byXpath("//div[@class='EditorCompositePanel']//div[@class='EditorComponentImpl']");
        EditorFixture editorNew = remoteRobot.find(EditorFixture.class, locator, Duration.ofSeconds(20));
        editorNew.click();

        Keyboard keyboard = new Keyboard(remoteRobot);

        // find the location to click on for feature name entry
        // this should put the entry cursor directly between <feature> and <feature/>
        //int offset = editorText.indexOf(textToDelete);
        editorNew.click();
        editorNew.selectText(textToModify);
        keyboard.hotKey(VK_DELETE);
        keyboard.enterText(modificationText);

        // save the new content
        if (remoteRobot.isMac()) {
            keyboard.hotKey(VK_META, VK_S);
        } else {
            // linux + windows
            keyboard.hotKey(VK_CONTROL, VK_S);
        }

        // slight delay to allow the diagnotic/quick fix data to arrive?
        TestUtils.sleepAndIgnoreException(2);
    }

    /**
     * Places the cursor at an exact location for text entry in file
     *
     * @param remoteRobot the remote robot instance
     * @param keyboard    keyboard to interact with
     * @param line        target line number
     * @param column      target column number
     */
    public static void goToLineAndColumn(RemoteRobot remoteRobot, Keyboard keyboard, int line, int column) {
        // trigger the line:col popup window to place cursor at exact location in file
        if (remoteRobot.isMac())
            keyboard.hotKey(KeyEvent.VK_META, KeyEvent.VK_L);
        else
            keyboard.hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_G);
        keyboard.enterText(line + ":" + column);
        keyboard.enter();
    }

    /**
     * Gathers the hover string data from the popup
     *
     * @param remoteRobot the remote robot instance
     * @param popupType   the type of popup window expected
     */
    public static String getHoverStringData(RemoteRobot remoteRobot, PopupType popupType) {
        // get the text from the LS diagnostic hint popup
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(30));

        ContainerFixture popup;
        switch (popupType.name()) {
            case "DOCUMENTATION":
                popup = projectFrame.getDocumentationHintEditorPane();
                break;
            case "DIAGNOSTIC":
                popup = projectFrame.getDiagnosticPane();
                break;
            default:
                // no known pane type specified, return
                return "";
        }

        List<RemoteText> rts = popup.findAllText();

        // print out the string data found in the popup window - for debugging
        popup.findAllText().forEach((it) -> System.out.println(it.getText()));

        StringBuilder popupString = new StringBuilder();
        for (RemoteText rt : rts) {
            popupString.append(rt.getText());
        }

        return popupString.toString();
    }

    /**
     * Opens the quickfix menu popup and chooses the
     * appropriate fix according to the quickfix substring
     *
     * @param remoteRobot           the remote robot instance
     * @param quickfixChooserString the text to find in the quick fix menu
     */
    public static void chooseQuickFix(RemoteRobot remoteRobot, String quickfixChooserString) {
        // first trigger the quickfix popup by using the keyboard
        Keyboard keyboard = new Keyboard(remoteRobot);

        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(30));

        ContainerFixture quickFixPopup = projectFrame.getQuickFixPane();

        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(2),
                "Waiting for the The quickfix popup to contain " + quickfixChooserString,
                "The quickfix popup did not contain " + quickfixChooserString,
                () -> quickFixPopup.hasText(quickfixChooserString));

        // get the text from the quickfix popup
        quickFixPopup.findText(contains(quickfixChooserString)).click();
        // After you click IntelliJ can chug for a while before the edit is rendered
        TestUtils.sleepAndIgnoreException(5);

        // Save the file.
        if (remoteRobot.isMac()) {
            keyboard.hotKey(VK_META, VK_S);
        } else {
            // linux + windows
            keyboard.hotKey(VK_CONTROL, VK_S);
        }

    }

    /**
     * Copies the contents from the currently active window.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void copyWindowContent(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(30));
        if (remoteRobot.isMac()) {
            // Select the content.
            projectFrame.clickOnMainMenuWithActions(remoteRobot, "Edit", "Select All");
            // Copy the content.
            projectFrame.clickOnMainMenuWithActions(remoteRobot, "Edit", "Copy");
        }
        else {
            clickOnMainMenu(remoteRobot);
            // Select the content.
            ComponentFixture editMenuEntry = projectFrame.getActionMenu("Edit", "10");
            editMenuEntry.moveMouse();
            ComponentFixture slectAllEntry = projectFrame.getActionMenuItem("Select All");
            slectAllEntry.click();

            clickOnMainMenu(remoteRobot);
            // Copy the content.
            ComponentFixture editMenuEntryNew = projectFrame.getActionMenu("Edit", "10");
            editMenuEntryNew.moveMouse();
            ComponentFixture copyEntry = projectFrame.getActionMenuItem("Copy");
            copyEntry.click();
            projectFrame.click();
        }
    }

    /**
     * Deletes the contents from the currently active window.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void clearWindowContent(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(30));

        if (remoteRobot.isMac()) {
            // Select the content.
            projectFrame.clickOnMainMenuWithActions(remoteRobot, "Edit", "Select All");
            // Delete/Clear the content.
            projectFrame.clickOnMainMenuWithActions(remoteRobot, "Edit", "Delete");
        }
        else {
            clickOnMainMenu(remoteRobot);
            // Select the content.
            ComponentFixture editMenuEntry = projectFrame.getActionMenu("Edit", "10");
            editMenuEntry.moveMouse();
            ComponentFixture selectAllEntry = projectFrame.getActionMenuItem("Select All");
            selectAllEntry.click();
            clickOnMainMenu(remoteRobot);
            // Delete/Clear the content.
            ComponentFixture editMenuEntryNew = projectFrame.getActionMenu("Edit", "10");
            editMenuEntryNew.moveMouse();
            ComponentFixture deleteEntry = projectFrame.getActionMenuItem("Delete");
            deleteEntry.click();
        }
    }

    public static void pasteOnActiveWindow(RemoteRobot remoteRobot) {
        pasteOnActiveWindow(remoteRobot, false);
    }

    /**
     * Pastes previously copied content on the currently active window.
     * In some cases the cursor may be in a context where Select All selects only the text
     * in a specific area rather than the whole file. In this case the fix is to move the
     * cursor to the home position 1,1
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param homeCursor if true move the cursor to 1,1
     */
    public static void pasteOnActiveWindow(RemoteRobot remoteRobot, boolean homeCursor) {
        if (homeCursor) {
            goToLineAndColumn(remoteRobot, new Keyboard(remoteRobot), 1, 1);
        }
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(30));
        if (remoteRobot.isMac()) {
            // Select the content.
            projectFrame.clickOnMainMenuWithActions(remoteRobot, "Edit", "Select All");
            // Paste the content.
            projectFrame.clickOnMainMenuWithActions(remoteRobot, "Edit", "Paste", "Paste");
            //Save
            projectFrame.clickOnMainMenuWithActions(remoteRobot, "File", "Save All");
        }
        else {
            clickOnMainMenu(remoteRobot);
            // Select the content.
            ComponentFixture editMenuEntry = projectFrame.getActionMenu("Edit", "10");
            editMenuEntry.moveMouse();
            ComponentFixture selectAllEntry = projectFrame.getActionMenuItem("Select All");
            selectAllEntry.click();
            clickOnMainMenu(remoteRobot);
            // Paste the content.
            editMenuEntry = projectFrame.getActionMenu("Edit", "10");
            editMenuEntry.moveMouse();
            ComponentFixture pasteFixture = projectFrame.getChildActionMenu("Edit", "Paste");
            pasteFixture.click();
            ComponentFixture pasteChildEntry = projectFrame.getChildActionMenuItem("Edit", "Paste");
            pasteChildEntry.click();
            clickOnMainMenu(remoteRobot);
            //Save
            ComponentFixture fileMenuEntry = projectFrame.getActionMenu("File", "10");
            fileMenuEntry.moveMouse();
            ComponentFixture saveAllEntry = projectFrame.getActionMenuItem("Save All");
            saveAllEntry.click();
        }
    }

    /**
     * Runs The Liberty configuration with custom configuration.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param startParams The parameters to set in the configuration dialog if it is not null.
     */
    public static void runStartParamsConfigDialog(RemoteRobot remoteRobot, String startParams) {
        runStartParamsConfigDialog(remoteRobot, startParams, null);
    }

    /**
     * Runs The Liberty configuration with custom configuration.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param startParams The parameters to set in the configuration dialog if it is not null.
     * @param runInContainer If not null use the value to set the Run In Container check box
     */
    public static void runStartParamsConfigDialog(RemoteRobot remoteRobot, String startParams, Boolean runInContainer) {
        DialogFixture dialog = remoteRobot.find(DialogFixture.class, Duration.ofSeconds(10));
        if (startParams != null) {
            ComponentFixture startParamsTextField = dialog.find(CommonContainerFixture.class, byXpath("//div[@class='EditorTextField']"), Duration.ofSeconds(5));
            startParamsTextField.click();
            startParamsTextField.runJs(
                    "component.setText(\"" + startParams + "\")", true);

            RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                    Duration.ofSeconds(1),
                    "Waiting for the start parameters text field on the Liberty config dialog to contain " + startParams,
                    "The start parameters text field on the Liberty config dialog did not contain " + startParams,
                    () -> startParamsTextField.hasText(startParams));
        }

        if (runInContainer != null) {
            Locator locator = byXpath("//div[@class='DialogPanel']//div[@class='StateRestoringCheckBox']");
            JCheckboxFixture checkBox = dialog.checkBox(locator);
            boolean boxStatus = checkBox.isSelected();
            if (boxStatus ^ runInContainer) { // xor the two values
                checkBox.click(); // Change needed, click the check box to toggle
            }
            TestUtils.sleepAndIgnoreException(1); // allow Apply button to be refreshed
        }

        // Save the changes made if necessary. If the config is being reused, there will be no changes.
        JButtonFixture applyButton = dialog.getButton("Apply");
        try {
            RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                    Duration.ofSeconds(1),
                    "Waiting for the Apply button on the open project dialog to be enabled",
                    "The Apply button on the open project dialog to be enabled",
                    applyButton::isEnabled);
            applyButton.click();
        } catch (WaitForConditionTimeoutException wfcte) {
            // Apply button remains grey, config being re-used
        }

        // Run the configuration.
        JButtonFixture runButton = dialog.getButton("Run");
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(1),
                "Waiting for the Run button on the open project dialog to be enabled",
                "The run button on the open project dialog to be enabled",
                runButton::isEnabled);
        runButton.click();
    }

    /**
     * Clicks on the play action button located on the Liberty tool window toolbar.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void clickOnLibertyTWToolbarPlayButton(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));

        // Click on the play button.
        String xPath = "//div[@class='LibertyExplorer']//div[@class='ActionButton' and @accessiblename.key='action.io.openliberty.tools.intellij.actions.RunLibertyDevTask.text']";
        ComponentFixture actionButton = projectFrame.getActionButton(xPath, "10");
        actionButton.click();
    }

    /**
     * Prints the UI Component hierarchy to the specified destination.
     *
     * @param printTo       The indicator that specifies where the output should go: FILE or STDOUT.
     * @param secondsToWait The seconds to wait before output is collected.
     */
    public static void printUIComponentHierarchy(PrintTo printTo, int secondsToWait) {
        try {
            if (secondsToWait > 0) {
                System.out.println("!!! MARKER: The output will be collected in " + secondsToWait + " seconds. !!!");
                Thread.sleep(secondsToWait * 1000L);
            }

            URL url = new URL(MavenSingleModMPProjectTest.REMOTE_BOT_URL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                try (FileWriter fw = new FileWriter("botCompHierarchy.html")) {
                    String inputLine;
                    while ((inputLine = br.readLine()) != null) {
                        switch (printTo) {
                            case STDOUT -> System.out.println(inputLine);
                            case FILE -> {
                                fw.write(inputLine);
                                fw.write("\n");
                            }
                            default -> Assert.fail("Invalid format to write : ");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.fail("Failed to collect UI Component Hierarchy information: " + e.getCause());
        }
    }

    /**
     * Runs an action from the Search Everywhere panel with a default value for `isMultiple`.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param action      The action to be executed.
     * @param maxRetries  The number of retries in case of failure.
     */
    public static void runActionFromSearchEverywherePanel(RemoteRobot remoteRobot, String action, int maxRetries) {
        runActionFromSearchEverywherePanel(remoteRobot, action, maxRetries, false);
    }

    /**
     * Opens the search everywhere dialog.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void runActionFromSearchEverywherePanel(RemoteRobot remoteRobot, String action, int maxRetries, boolean isMultiple) {
        // Search everywhere UI actions may fail due to UI flickering/indexing on Windows. Retry in case of a failure.
        Exception error = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                error = null;

                // Click on Navigate on the Menu bar.
                ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofMinutes(2));
                if (remoteRobot.isMac()) {
                    projectFrame.clickOnMainMenuWithActions(remoteRobot, "Navigate", "Search Everywhere");
                }
                else {
                    clickOnMainMenu(remoteRobot);
                    ComponentFixture navigateMenuEntry = projectFrame.getActionMenu("Navigate", "20");
                    navigateMenuEntry.moveMouse();

                    // Click on Search Everywhere in the menu.
                    ComponentFixture searchFixture = projectFrame.getActionMenuItem("Search Everywhere");
                    searchFixture.click();
                }

                // Click on the Actions tab
                ComponentFixture actionsTabFixture = projectFrame.getSETabLabel("Actions");
                actionsTabFixture.click();

                // Type the search string in the search dialog box.
                JTextFieldFixture searchField = projectFrame.textField(JTextFieldFixture.Companion.byType(), Duration.ofSeconds(10));
                searchField.click();
                searchField.setText(action);
                TestUtils.sleepAndIgnoreException(10); // allow search time to resolve, 10s for slow machines

                // Wait for the desired action to show in the search output frame and click on it.
                RepeatUtilsKt.waitFor(Duration.ofSeconds(20),
                        Duration.ofSeconds(1),
                        "Waiting for the search to filter and show " + action + " in search output",
                        "The search did not filter or show " + action + " in the search output",
                        () -> findTextInListOutputPanel(projectFrame, action, true) != null);

                RemoteText foundAction = findTextInListOutputPanel(projectFrame, action, true);
                if (foundAction != null) {
                    foundAction.click();
                } else {
                    throw new RuntimeException("Search everywhere found " + action + ", but it can no longer be found after a subsequent attempt to find it.");
                }

                // If the Liberty: Start... action was selected, make sure the Edit Configuration dialog is displayed.
                // For multiple projects, an extra dialog appears to select the project before the "Edit Configuration" dialog is displayed.
                if (action.equals("Liberty: Start...") && !isMultiple) {
                    // This call will fail if the expected dialog is not displayed.
                    projectFrame.find(DialogFixture.class, DialogFixture.byTitle("Edit Configuration"), Duration.ofSeconds(30));
                }

                // If the Liberty: Add project to the tool window action was selected, make sure the Add Liberty project dialog is displayed.
                if (action.equals("Liberty: Add project to the tool window")) {
                    // This call will fail if the expected dialog is not displayed.
                    projectFrame.find(DialogFixture.class, DialogFixture.byTitle("Add Liberty project"), Duration.ofSeconds(30));
                }

                // If the Liberty: Add project to the tool window action was selected, make sure the Remove Liberty project dialog is displayed.
                if (action.equals("Liberty: Remove project from the tool window")) {
                    // This call will fail if the expected dialog is not displayed.
                    projectFrame.find(DialogFixture.class, DialogFixture.byTitle("Remove Liberty project"), Duration.ofSeconds(30));
                }
                break;
            } catch (Exception e) {
                error = e;
                TestUtils.printTrace(TestUtils.TraceSevLevel.INFO,
                        "Failed to run the " + action + " action using the search everywhere option (" + e.getMessage() + "). Retrying...");
                TestUtils.sleepAndIgnoreException(5);
            }
        }

        /*
         * We throw a RuntimeException with error only if the action is not "Close All Tabs" .The "Close All Tabs" action
         * is used as part of the cleanup process in the @AfterAll-annotated method within the SingleModMPProjectTestCommon class.
         *
         * When running individual tests in this class, it is not always guaranteed that at least one file tab will be open in the Editor Window.
         * If we are not specify a condition check for "Close All Tabs" action when no file tabs are open, the runActionFromSearchEverywherePanel method
         * will throw a RuntimeException, causing the test to fail.
         */
        if (error != null && !action.equals("Close All Tabs")) {
            throw new RuntimeException("Failed to run the " + action + " action using the search everywhere option", error);
        }
    }

    /**
     * Selects the specified project using the default 'Liberty project' dialog title.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param projectName The name of the project to select.
     */
    public static void selectProjectFromDialog(RemoteRobot remoteRobot, String projectName) {
        selectProjectFromDialog(remoteRobot, projectName, "Liberty project");
    }

    /**
     * Selects the specified project from the list of detected projects shown in the 'Add Liberty project'
     * dialog.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param projectName The name of the project to select.
     */
    public static void selectProjectFromDialog(RemoteRobot remoteRobot, String projectName, String dialogTitle) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        DialogFixture addProjectDialog = projectFrame.find(DialogFixture.class,
                DialogFixture.byTitle(dialogTitle),
                Duration.ofSeconds(10));
        JButtonFixture jbf = addProjectDialog.getBasicArrowButton();
        jbf.click();

        RemoteText remoteProject = findTextInListOutputPanel(addProjectDialog, projectName, false);
        if (remoteProject != null) {
            remoteProject.click();
        } else {
            fail("Unable to find " + projectName + " in the output list of the " + dialogTitle + " dialog.");
        }

        JButtonFixture okButton = addProjectDialog.getButton("OK");
        okButton.click();
    }

    /**
     * Selects the build file path for a Liberty project in the Run/Debug Configurations dialog.
     *
     * @param remoteRobot  The RemoteRobot instance.
     * @param buildFilePath The desired build file path to select.
     */
    public static void selectProjectBuildFilePath(RemoteRobot remoteRobot, String buildFilePath) {
        try {
            ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));

            // Find the "Run/Debug Configurations" dialog within the project frame.
            DialogFixture addProjectDialog = projectFrame.find(DialogFixture.class,
                    DialogFixture.byTitle("Run/Debug Configurations"),
                    Duration.ofSeconds(10));

            Locator locator = byXpath("//div[@class='DialogPanel']//div[@class='ComboBox']");
            ComboBoxFixture projBldFileBox = addProjectDialog.comboBox(locator, Duration.ofSeconds(10));

            // Initialize the keyboard object to simulate key presses.
            Keyboard keyboard = new Keyboard(remoteRobot);

            // Click on the ComboBox to focus it.
            projBldFileBox.click();

            // Copy the currently selected value in the ComboBox to the clipboard.
            if (remoteRobot.isMac()) {
                keyboard.hotKey(VK_META, VK_C); // Command + C on Mac.
            } else {
                keyboard.hotKey(VK_CONTROL, VK_C); // Ctrl + C on Linux/Windows.
            }

            // Retrieve the copied value from the system clipboard.
            String copiedValue = (String) Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .getData(DataFlavor.stringFlavor);

            // If the copied default build file path does not match the actual build file path of the project,
            // the second item in the list represents the correct build file path. Select that item.
            if (!(copiedValue.equals(buildFilePath))) {
                List<JListFixture> searchLists = addProjectDialog.jLists(JListFixture.Companion.byType());
                if (!searchLists.isEmpty()) {
                    JListFixture searchList = searchLists.get(1);
                    try {
                        List<RemoteText> entries = searchList.findAllText();
                        entries.get(1).click(); // Click on the second entry in the list.
                    } catch (NoSuchElementException nsee) {
                        // Handle the case where the list is empty.
                    }
                }
            }
        } catch (Exception e) {
            // Log the exception and fail the test if an error occurs.
            e.printStackTrace();
            Assertions.fail("Failed to select the exact " + buildFilePath + " : " + e.getMessage());
        }
    }


    /**
     * Checks if the "Liberty project" dialog is visible within the project frame.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @return true if the "Liberty project" dialog is visible; false otherwise.
     */
    public static boolean checkProjectDialog(RemoteRobot remoteRobot) {
        try {
            ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));

            // Search for a dialog titled "Liberty project" within the project frame, with a timeout of 10 seconds.
            DialogFixture addProject = projectFrame.find(
                    DialogFixture.class,
                    DialogFixture.byTitle("Liberty project"),
                    Duration.ofSeconds(10)
            );

            // Return true if the dialog is visible.
            return addProject.isShowing();
        } catch (Exception e) {
            // Return false to indicate the dialog is not visible.
            return false;
        }
    }


    /**
     * Selects the specified project from the list of detected projects shown in the 'Remove Liberty project'
     * dialog.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param projectName The name of the project to select.
     */
    public static void selectProjectFromRemoveLibertyProjectDialog(RemoteRobot remoteRobot, String projectName) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        DialogFixture removeProjectDialog = projectFrame.find(DialogFixture.class,
                DialogFixture.byTitle("Remove Liberty project"),
                Duration.ofSeconds(10));
        removeProjectDialog.getBasicArrowButton().click();

        RemoteText remoteProject = findTextInListOutputPanel(removeProjectDialog, projectName, true);
        if (remoteProject != null) {
            remoteProject.click();
        } else {
            fail("Unable to find " + projectName + " in the output list of the Remove Liberty project dialog.");
        }

        JButtonFixture okButton = removeProjectDialog.getButton("OK");
        okButton.click();
    }

    /**
     * Waits for the Liberty tool window message indicating that Liberty Tools could not
     * detect any projects.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param waitTime    The time (seconds) to wait for the required message to appear in the text area.
     */
    public static void waitForLTWNoProjectDetectedMsg(RemoteRobot remoteRobot, int waitTime) {
        String text = " 1. If no projects are open in the Project tool window, open or create a Liberty project using " +
                "the File menu.  2. If one or more existing Maven or Gradle projects are open in the Project tool " +
                "window, try one of the following actions:   a. Configure the Liberty build plugin in the build file " +
                "of an existing Maven or Gradle project. b. Add a server.xml file to an existing Maven or Gradle " +
                "project at 'src/main/liberty/config'. c. Manually add an existing Maven or Gradle project to the" +
                " Liberty tool window using the 'Liberty: Add project to the tool window' action through the " +
                "Search Everywhere window.";

        int maxRetries = 3;
        Exception error = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                error = null;
                waitForLTWTextAreaMessage(remoteRobot, text, waitTime);
                break;
            } catch (Exception e) {
                // Indexing may cause errors. Retry.
                error = e;
                TestUtils.printTrace(TestUtils.TraceSevLevel.INFO, "Unable to find LTW Text area message: " + text + ". (" + e.getMessage() + "). Retrying...");
                TestUtils.sleepAndIgnoreException(5);
            }
        }

        // Report the last error if there is one.
        if (error != null) {
            throw new RuntimeException("Unable to find LTW Text area message: " + text + ".", error);
        }
    }

    /**
     * Waits for the Liberty tool window message indicating that indexing is taking place.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param waitTime    The time (seconds) to wait for the required message to appear in the text area.
     */
    public static void waitForLTWIndexingMsg(RemoteRobot remoteRobot, int waitTime) {
        String text = "This view is not available until indexes are built";
        waitForLTWTextAreaMessage(remoteRobot, text, waitTime);
    }

    /**
     * Waits for the input message to appear in the Liberty Tool window message text area.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param message     The message to search for.
     * @param waitTime    The time (seconds) to wait for the required message to appear in the text area.
     */
    public static void waitForLTWTextAreaMessage(RemoteRobot remoteRobot, String message, int waitTime) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        ComponentFixture textArea = projectFrame.getTextArea("30");

        RepeatUtilsKt.waitFor(Duration.ofSeconds(waitTime),
                Duration.ofSeconds(1),
                "Waiting for message " + message + " to appear in the Liberty tool window",
                "Message " + message + " did not appear in the Liberty tool window",
                () -> readAllText(textArea).equals(message));
    }

    /**
     * Look for the indexing message and if it is found wait up to 10 minutes for it to stop.
     */
    public static void waitForIndexing(RemoteRobot remoteRobot) {
        TestUtils.printTrace(TestUtils.TraceSevLevel.INFO, "UIBotTestUtils.waitForIndexing Entry");
        String xPath = "//div[@class='InlineProgressPanel']";
        boolean needToWait = waitForIndexingToStart(remoteRobot, xPath, 60);
        if (needToWait) {
            waitForIndexingToStop(remoteRobot, xPath, 600);
        }
        TestUtils.printTrace(TestUtils.TraceSevLevel.INFO, "UIBotTestUtils.waitForIndexing Exit");
    }

    /**
     * Wait for the indexing message to appear. If it appears return true. If it does not appear
     * in the time specified we will assume indexing is not going to happen.
     */
    public static boolean waitForIndexingToStart(RemoteRobot remoteRobot, String xPath, int waitTime) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        // If indexing is needed the Inline Progress panel will contain a TextPanel object with text: 'Indexing JDK 17' or other names
        Locator progressPanelLocator = byXpath(xPath);
        JLabelFixture progressPanelFixture = projectFrame.find(JLabelFixture.class, progressPanelLocator, Duration.ofSeconds(10));

        List<RemoteText> l = progressPanelFixture.findAllText();
        try {
            RepeatUtilsKt.waitFor(Duration.ofSeconds(waitTime),
                    Duration.ofSeconds(1),
                    "Waiting for indexing message to appear e.g. Indexing Java 17...",
                    "Indexing did not appear in the Liberty tool window",
                    () -> !progressPanelFixture.findAllText().isEmpty());
        } catch (Exception e) {
            // Did not find the indexing message, just continue
        }
        return !progressPanelFixture.findAllText().isEmpty(); // not empty means it is running
    }

    /**
     * Wait for the indexing message to disappear
     */
    public static void waitForIndexingToStop(RemoteRobot remoteRobot, String xPath, int waitTime) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        // The Inline progress panel might contain a TextPanel object with text: 'Indexing JDK 17' or other indexing tasks
        Locator progressPanelLocator = byXpath(xPath);
        JLabelFixture progressPanelFixture = projectFrame.find(JLabelFixture.class, progressPanelLocator, Duration.ofSeconds(10));

        RepeatUtilsKt.waitFor(Duration.ofSeconds(waitTime),
                Duration.ofSeconds(1),
                "Waiting for indexing message to disappear e.g. Indexing Java 17...",
                "Indexing did not appear in the Liberty tool window",
                () -> progressPanelFixture.findAllText().isEmpty());
    }
    /**
     * Returns a concatenated string of all text found in a ComponentFixture object.
     *
     * @param componentFixture The ComponentFixture object.
     * @return A concatenated string of all text found in a ComponentFixture object.
     */
    public static String readAllText(ComponentFixture componentFixture) {
        List<RemoteText> lines = componentFixture.findAllText();
        StringBuilder fullText = new StringBuilder();
        for (RemoteText line : lines) {
            fullText.append(line.getText());
        }

        return fullText.toString();
    }

    /**
     * Responds to the `Remove Liberty project` dialog query asking if the project should be deleted.
     * The response is in the affirmative.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void respondToRemoveProjectQueryDialog(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        DialogFixture removeProjectDialog = projectFrame.find(DialogFixture.class,
                DialogFixture.byTitle("Remove Liberty project"),
                Duration.ofSeconds(10));
        JButtonFixture okButton = removeProjectDialog.getButton("Yes");
        okButton.click();
    }

    /**
     * Creates a new Liberty configuration.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param cfgName     The name of the new configuration.
     */
    public static void createLibertyConfiguration(RemoteRobot remoteRobot, String cfgName, boolean isMultiple, String buildFilePath) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        String editConfigurationAction= null;
        if (remoteRobot.isMac()) {
            editConfigurationAction = handleMenuBasedOnVersion(remoteRobot, "Edit Configurations...", "Edit Configurations…");
            projectFrame.clickOnMainMenuWithActions(remoteRobot, "Run", editConfigurationAction);
        }
        else {
            clickOnMainMenu(remoteRobot);
            ComponentFixture runMenu = projectFrame.getActionMenu("Run", "10");
            runMenu.moveMouse();
            editConfigurationAction = handleMenuBasedOnVersion(remoteRobot, "Edit Configurations...", "Edit Configurations…");
            ComponentFixture editCfgsMenuEntry = projectFrame.getActionMenuItem(editConfigurationAction);
            editCfgsMenuEntry.click();
        }

        // Find the Run/Debug Configurations dialog.
        DialogFixture addProjectDialog = projectFrame.find(DialogFixture.class,
                DialogFixture.byTitle("Run/Debug Configurations"),
                Duration.ofSeconds(10));

        String exitButtonText = "Cancel";
        try {
            // Click on the Add Configuration action button (+) to open the Add New Configuration window.
            Locator addButtonLocator = byXpath("//div[@accessiblename.key='add.new.run.configuration.action2.name']");
            ActionButtonFixture addCfgButton = addProjectDialog.actionButton(addButtonLocator);
            addCfgButton.click();

            // Look for the Liberty entry in the Add New configuration window and  create a new configuration.
            ComponentFixture pluginCfgTree = addProjectDialog.getMyTree();
            RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                    Duration.ofSeconds(2),
                    "Waiting for plugin config tree to show on the screen",
                    "Plugin config tree is not showing on the screen",
                    pluginCfgTree::isShowing);

            List<RemoteText> rts = pluginCfgTree.findAllText();
            for (RemoteText rt : rts) {
                if (rt.getText().equals("Liberty")) {
                    rt.click();
                    break;
                }
            }

            // The Run/Debug Configurations dialog should now contain the Liberty configuration that was
            // just created. Refresh it.
            addProjectDialog = projectFrame.find(DialogFixture.class,
                    DialogFixture.byTitle("Run/Debug Configurations"),
                    Duration.ofSeconds(10));

            // The new configuration should refer to one of the Liberty projects by default.
            // If there are no Liberty projects detected in the workspace then the combobox
            // will have zero values.
            Locator projectComboLocator = byXpath("//div[@class='ComboBox']");
            ComboBoxFixture projectCombo = addProjectDialog.comboBox(projectComboLocator);
            if (projectCombo.listValues().isEmpty()) {
                throw new NoSuchElementException("Liberty project field contains no elements");
            }

            // Find the new configuration's name text field and give it a name.
            Locator locator = byXpath("//div[@class='JTextField']");

            for (int i = 0; i < 3; i++) {
                try {
                    JTextFieldFixture nameTextField = addProjectDialog.textField(locator, Duration.ofSeconds(10));
                    RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                            Duration.ofSeconds(1),
                            "Waiting for the name text field to be enabled or populated by default",
                            "The name text field was not enabled or populated by default",
                            () -> nameTextField.isEnabled() && !(nameTextField.getText().isEmpty()) && nameTextField.getText().equals("Unnamed"));

                    nameTextField.click();
                } catch (Exception e) {
                    // Retry.
                    // The new config default name may take a bit to show up causing us to capture the previous config
                    // component before it is completely replaced with the new configuration. this may cause either a
                    // WaitForConditionTimeoutException or an IllegalComponentStateException because the right component
                    // or field was not captured.
                }
            }

            JTextFieldFixture newNameTextField = addProjectDialog.textField(locator, Duration.ofSeconds(10));
            newNameTextField.setText(cfgName);

            RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                    Duration.ofSeconds(1),
                    "Waiting for the name of the config to appear in the text box",
                    "The name of the config did not appear in the text box ",
                    () -> newNameTextField.getText().equals(cfgName));

            if (isMultiple) {
                UIBotTestUtils.selectProjectBuildFilePath(remoteRobot, buildFilePath);
            }

            // Save the new configuration by clicking on the Apply button.
            JButtonFixture applyButton = addProjectDialog.getButton("Apply");
            RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                    Duration.ofSeconds(1),
                    "Waiting for the Apply button on the add config project dialog to be enabled",
                    "The Apply button on the add config dialog was not enabled",
                    applyButton::isEnabled);
            applyButton.click();

            // Change which button we click to complete the operation
            exitButtonText = "OK";
        } finally {
            // Exit the Run/Debug Configurations dialog.
            JButtonFixture exitButton = addProjectDialog.getButton(exitButtonText);
            RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                    Duration.ofSeconds(1),
                    "Waiting for the " + exitButtonText + " button on the config dialog to be enabled",
                    "The " + exitButtonText + " button on the config dialog was not enabled",
                    exitButton::isEnabled);
            exitButton.click();
        }
    }

    /**
     * Edits a Liberty configuration using the Liberty Edit Configuration dialog. The dialog is displayed
     * using the start... action on the Liberty tool window's drop down menu.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param cfgName     The name of the new configuration.
     * @param startParams The dev mode start parameters.
     */
    public static void editLibertyConfigUsingEditConfigDialog(RemoteRobot remoteRobot, String projectName, String cfgName, String startParams) {
        // Display the Liberty Edit Configuration dialog.
        runLibertyActionFromLTWDropDownMenu(remoteRobot, "Start...", projectName, false, 3);

        // Get a hold of the Liberty Edit Configurations dialog.
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        DialogFixture dialog = projectFrame.find(DialogFixture.class, DialogFixture.byTitle("Edit Configuration"), Duration.ofSeconds(10));
        String exitButtonText = "Cancel";

        try {
            // Update the configuration name.
            if (cfgName != null) {
                // Find the new configuration's name text field and give it a name.
                Locator locator = byXpath("//div[@class='JTextField']");
                JTextFieldFixture nameTextField = dialog.textField(locator, Duration.ofSeconds(10));
                RepeatUtilsKt.waitFor(Duration.ofSeconds(30),
                        Duration.ofSeconds(1),
                        "Waiting for the name text field on the Liberty edit config dialog to be enabled or populated by default",
                        "The name text field on the Liberty edit config dialog was not enabled or populated by default",
                        () -> nameTextField.isEnabled() && (nameTextField.getText().length() != 0));

                nameTextField.click();
                nameTextField.setText(cfgName);
            }

            // Update the start parameters field.
            if (startParams != null) {
                ComponentFixture startParmTextField = dialog.find(CommonContainerFixture.class, byXpath("//div[@class='EditorTextField']"), Duration.ofSeconds(5));
                startParmTextField.click();
                startParmTextField.runJs(
                        "component.setText(\"" + startParams + "\")", true);

                RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                        Duration.ofSeconds(1),
                        "Waiting for the start parameters text field on the Liberty edit config dialog to contain " + startParams,
                        "The start parameters text field on the Liberty edit config dialog did not contain " + startParams,
                        () -> startParmTextField.hasText(startParams));
            }

            // Save the changes.
            JButtonFixture applyButton = dialog.getButton("Apply");
            try {
                RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                        Duration.ofSeconds(1),
                        "Waiting for the Apply button on the Liberty edit config dialog to be enabled",
                        "The Apply button on the Liberty edit config dialog to be enabled",
                        applyButton::isEnabled);
                applyButton.click();
                exitButtonText = "Close";
            } catch (WaitForConditionTimeoutException wfcte) {
                // Config did not change.
            }
        } finally {
            // Exit the Edit Configuration dialog.
            JButtonFixture exitButton = dialog.getButton(exitButtonText);
            RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                    Duration.ofSeconds(1),
                    "Waiting for the " + exitButtonText + " button on the Liberty edit config dialog to be enabled",
                    "The " + exitButtonText + " button on the Liberty edit config dialog was not enabled",
                    exitButton::isEnabled);
            exitButton.click();
        }
    }

    /**
     * Returns the configuration entries displayed on an opened Liberty Edit Configuration dialog.
     * The opened dialog is expected to have been displayed using a start... action.
     * The configuration is closed on exit.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @return The opened Liberty configuration entries.
     */
    public static Map<String, String> getOpenedLibertyConfigDataAndCloseOnExit(RemoteRobot remoteRobot) {
        HashMap<String, String> map = new HashMap<String, String>();

        // Get a hold of the Liberty Edit Configuration dialog.
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        DialogFixture libertyCfgDialog = projectFrame.find(DialogFixture.class, DialogFixture.byTitle("Edit Configuration"), Duration.ofSeconds(30));

        try {
            // Get the name of the configuration.
            Locator locator = byXpath("//div[@class='JTextField']");
            JTextFieldFixture nameTextField = libertyCfgDialog.textField(locator, Duration.ofSeconds(10));
            RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                    Duration.ofSeconds(1),
                    "Waiting for the name text field on the Liberty edit config dialog to be enabled or populated by default",
                    "The name text field on the Liberty edit config dialog was not enabled or populated by default",
                    () -> nameTextField.isEnabled() && (nameTextField.getText().length() != 0));
            String configName = nameTextField.getText();
            map.put(ConfigEntries.NAME.toString(), configName);

            // Get the project path
            locator = byXpath("//div[@class='DialogPanel']//div[@class='ComboBox']");
            ComboBoxFixture projBldFileBox = libertyCfgDialog.comboBox(locator, Duration.ofSeconds(10));

            RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                    Duration.ofSeconds(1),
                    "Waiting for the combo box labeled Liberty project on the Liberty edit config dialog to be populated",
                    "The combo box labeled Liberty project on the Liberty edit config dialog was not populated",
                    () -> projBldFileBox.listValues().size() != 0);
            List<String> entries = projBldFileBox.listValues();
            String projBldFilePath = entries.get(0);
            map.put(ConfigEntries.LIBERTYPROJ.toString(), projBldFilePath);

            // Get the dev mode parameters
            ComponentFixture startParamsTextField = libertyCfgDialog.find(CommonContainerFixture.class, byXpath("//div[@class='EditorTextField']"), Duration.ofSeconds(5));
            startParamsTextField.click();
            String params = startParamsTextField.callJs(
                    "component.getText()", true);
            map.put(ConfigEntries.PARAMS.toString(), params);
        } finally {
            // Close the configuration by clicking on the Cancel button.
            JButtonFixture cancelButton = libertyCfgDialog.getButton("Cancel");
            RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                    Duration.ofSeconds(1),
                    "Waiting for the cancel button on the Liberty edit config dialog to be enabled",
                    "The cancel button on the Liberty edit config dialog was not enabled",
                    cancelButton::isEnabled);
            cancelButton.click();
        }

        return map;
    }

    /**
     * Selects the liberty configuration using the Run/Debug configuration selection box on
     * the project frame toolbar.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param cfgName     The configuration name to select.
     */
    public static void selectConfigUsingToolbar(RemoteRobot remoteRobot, String cfgName) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        ComponentFixture cfgSelectBox = projectFrame.getRunConfigurationsComboBoxButton();

        boolean configFound = false;
        int retryCount = 0;
        int maxRetries = 5;

        while (!configFound && retryCount < maxRetries) {
            cfgSelectBox.click();

            ComponentFixture cfgSelectPaneList = projectFrame.getMyList();
            List<RemoteText> configs = cfgSelectPaneList.getData().getAll();

            if (configs != null && !configs.isEmpty()) {
                for (RemoteText cfg : configs) {
                    if (cfg.getText().equals(cfgName)) {
                        cfg.click();
                        configFound = true;
                        break;
                    }
                }
            }
            if (!configFound) {
                retryCount++;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted while waiting for configuration list", e);
                }
            }
        }
        if (!configFound) {
            throw new RuntimeException("Configuration '" + cfgName + "' not found after " + maxRetries + " attempts.");
        }
    }

    /**
     * Selects the specified configuration using the Run->Run.../Debug... menu options.
     * The configuration is chosen from panel that lists the available configurations.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param cfgName     The configuration name
     * @param execMode    The execution mode (RUN/DEBUG).
     */
    public static void selectConfigUsingMenu(RemoteRobot remoteRobot, String cfgName, ExecMode execMode) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        String debugOrRunAction = null;
        if (remoteRobot.isMac()) {
            for (int attempt = 0; attempt < 5; attempt++) { // Retry up to 5 times
                try {
                    debugOrRunAction = handleMenuBasedOnVersion(remoteRobot,  execMode == ExecMode.DEBUG ? "Debug..." : "Run...", execMode == ExecMode.DEBUG ? "Debug…" : "Run…");
                    projectFrame.clickOnMainMenuWithActions(remoteRobot, "Run", debugOrRunAction);
                    // Exit loop if successful
                    break;
                } catch (WaitForConditionTimeoutException e) {
                    TestUtils.printTrace(TestUtils.TraceSevLevel.INFO, "Attempt " + (attempt + 1) + " failed: Timeout while trying to find or interact with menu items. Retrying...");
                } catch (Exception e) {
                    TestUtils.printTrace(TestUtils.TraceSevLevel.INFO, "Attempt " + (attempt + 1) + " failed: " + e.getMessage() + " Retrying...");
                }

                if (attempt == 4) { // If the last attempt fails
                    throw new IllegalStateException("Failed to perform menu navigation after multiple attempts.");
                }
            }
        }
        else {
            clickOnMainMenu(remoteRobot);
            ComponentFixture menuOption = projectFrame.getActionMenu("Run", "10");
            menuOption.moveMouse();
            debugOrRunAction= handleMenuBasedOnVersion(remoteRobot, "Run...", "Run…");
            ComponentFixture menuCfgExecOption = projectFrame.getActionMenuItem(debugOrRunAction);
            if (execMode == ExecMode.DEBUG) {
                debugOrRunAction = handleMenuBasedOnVersion(remoteRobot, "Debug...", "Debug…");
                menuCfgExecOption = projectFrame.getActionMenuItem(debugOrRunAction);
            }

            menuCfgExecOption.click();
        }

        // Retrieve the list of configs from the config list window.
        ComponentFixture cfgSelectWindow = projectFrame.getMyList();
        List<RemoteText> configs = cfgSelectWindow.getData().getAll();

        // Open the specified configuration.
        for (RemoteText cfg : configs) {
            if (cfg.getText().equals(cfgName)) {
                cfg.click();
                break;
            }
        }
    }

    /**
     * Executes the configurations using the icon on the project frame toolbar.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param execMode    The execution mode (RUN/DEBUG).
     */
    public static void runConfigUsingIconOnToolbar(RemoteRobot remoteRobot, ExecMode execMode) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));

        Locator locator = byXpath("//div[@class='ActionButton' and @myaction='Run (Run selected configuration)']");
        if (execMode == ExecMode.DEBUG) {
            locator = byXpath("//div[@class='ActionButton' and @myicon='debug.svg']");
        }

        ActionButtonFixture iconButton = projectFrame.actionButton(locator, Duration.ofSeconds(10));
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(1),
                "Waiting for the start debugger button on the project frame toolbar to be enabled",
                "The start debugger button on the project frame toolbar was not enabled",
                iconButton::isEnabled);

        iconButton.click();
    }

    /**
     * Deletes all Liberty tool plugin configurations using the Run->EditConfigurations... dialog.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void deleteLibertyRunConfigurations(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        String editConfigurationAction = null;
        if (remoteRobot.isMac()) {
            editConfigurationAction = handleMenuBasedOnVersion(remoteRobot, "Edit Configurations...", "Edit Configurations…");
            projectFrame.clickOnMainMenuWithActions(remoteRobot, "Run", editConfigurationAction);
        }
        else {
            clickOnMainMenu(remoteRobot);
            ComponentFixture runMenu = projectFrame.getActionMenu("Run", "10");
            runMenu.moveMouse();
            editConfigurationAction = handleMenuBasedOnVersion(remoteRobot, "Edit Configurations...", "Edit Configurations…");
            ComponentFixture editCfgsMenuEntry = projectFrame.getActionMenuItem(editConfigurationAction);
            editCfgsMenuEntry.click();
        }

        // The Run/Debug configurations dialog could resize and reposition icons. Retry in case of a failure.
        int maxRetries = 3;
        Exception error = null;
        DialogFixture rdConfigDialog = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                error = null;

                // Find the run/debug configurations dialog.
                rdConfigDialog = projectFrame.find(DialogFixture.class,
                        DialogFixture.byTitle("Run/Debug Configurations"),
                        Duration.ofSeconds(10));

                // Expand and retrieve configuration tree content.
                JTreeFixture configTree = rdConfigDialog.jTree();
                configTree.expandAll();
                List<RemoteText> treeEntries = configTree.findAllText();

                // Find the Liberty tree node and delete all child configurations.
                boolean processEntries = false;
                boolean firstEntryClicked = false;
                Locator locator = byXpath("//div[@accessiblename.key='remove.run.configuration.action.name']");
                ActionButtonFixture removeCfgButton = rdConfigDialog.actionButton(locator);

                for (RemoteText treeEntry : treeEntries) {
                    if (treeEntry.getText().equals("Liberty")) {
                        processEntries = true;
                        continue;
                    }
                    if (processEntries) {
                        // Click on the first configuration in the tree only.
                        if (!firstEntryClicked) {
                            treeEntry.click();
                            firstEntryClicked = true;
                        }

                        // Delete the configuration.
                        try {
                            RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                                    Duration.ofSeconds(1),
                                    "Waiting for the remove config button on the run/debug configurations dialog to be enabled",
                                    "The remove config button on the config run/debug configurations was not enabled",
                                    removeCfgButton::isEnabled);
                            removeCfgButton.click();
                        } catch (WaitForConditionTimeoutException wfcte) {
                            // We have reached the end of the Liberty entries.
                            break;
                        }
                    }
                }

                // Press the OK button to persist the changes.
                JButtonFixture okButton = rdConfigDialog.getButton("OK");
                RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                        Duration.ofSeconds(1),
                        "Waiting for the OK button on the run/debug configurations dialog to be enabled",
                        "The OK button on the run/debug configurations dialog was not enabled",
                        okButton::isEnabled);
                okButton.click();
                break;
            } catch (Exception e) {
                error = e;
                TestUtils.printTrace(TestUtils.TraceSevLevel.INFO,
                        "Failed to delete run configurations shown in the run/debug configurations dialog (" + e.getMessage() + "). Retrying...");
                TestUtils.sleepAndIgnoreException(5);
            }
        }

        // Report the last error if there is one.
        if (error != null) {
            // If the dialog was opened, close it.
            if (rdConfigDialog != null) {
                JButtonFixture cancelButton = rdConfigDialog.getButton("Cancel");
                RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                        Duration.ofSeconds(1),
                        "Waiting for the cancel button on the run/debug configurations dialog to be enabled",
                        "The cancel button on the run/debug configurations dialog was not enabled",
                        cancelButton::isEnabled);
                cancelButton.click();
            }

            // Report the error.
            throw new RuntimeException("Failed to delete configurations shown in the run/debug configurations dialog", error);
        }
    }

    /**
     * Stops the debugger if it is running.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void stopDebugger(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        try {
            projectFrame.getBaseLabel("Debug", "5");
        } catch (WaitForConditionTimeoutException wfcte) {
            // The debug tab is not opened for some reason. Open it.
            ComponentFixture debugStripe = projectFrame.getStripeButton("Debug", "10");
            debugStripe.click();
        }

        Locator locator = byXpath("//div[@class='MyNonOpaquePanel']//div[contains(@myaction.key, 'action.Stop.text')]");
        ActionButtonFixture stopButton = projectFrame.actionButton(locator, Duration.ofSeconds(60));
        stopButton.click();
    }

    /**
     * Returns the RemoteText object representing the text found in the list panel, or
     * null if the text was not found.
     *
     * @param fixture The CommonContainerFixture under which the search is taking place.
     * @param text    The text to search.
     * @return The RemoteText object representing the text found in the list panel, or
     * null if the text was not found.
     */
    public static RemoteText findTextInListOutputPanel(CommonContainerFixture fixture, String text, boolean exactMatch) {
        RemoteText foundText = null;

        List<JListFixture> searchLists = fixture.jLists(JListFixture.Companion.byType());
        if (!searchLists.isEmpty()) {
            JListFixture searchList = searchLists.get(0);
            try {
                List<RemoteText> entries = searchList.findAllText();
                for (RemoteText entry : entries) {
                    if ((exactMatch && entry.getText().equals(text)) || (!exactMatch && entry.getText().contains(text))) {
                        foundText = entry;
                    }
                }
            } catch (NoSuchElementException nsee) {
                // The list is empty.
                return null;
            }
        }

        return foundText;
    }

    /**
     * Stops the Liberty server by running the action from the Liberty tool window dropdown action list.
     *
     * @param remoteRobot     The RemoteRobot instance.
     * @param testName        The name of the test calling this method.
     * @param absoluteWLPPath The absolute path of the Liberty installation.
     * @param maxRetries      The maximum amount of attempts to try to stop the server.
     */
    public static void runStopAction(RemoteRobot remoteRobot, String testName, ActionExecType execType, String absoluteWLPPath, String smMPProjName, int maxRetries, boolean isMultiple) {
        for (int i = 0; i < maxRetries; i++) {
            // Stop dev mode. Any failures during command processing are retried. If there are any
            // failures, this method will exit.
            switch (execType) {
                case LTWPLAY:
                    UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Stop", smMPProjName, true, maxRetries);
                    break;
                case LTWDROPDOWN:
                    UIBotTestUtils.runLibertyActionFromLTWDropDownMenu(remoteRobot, "Stop", smMPProjName, false, maxRetries);
                    break;
                case LTWPOPUP:
                    UIBotTestUtils.runActionLTWPopupMenu(remoteRobot, smMPProjName, "Liberty: Stop", maxRetries);
                    break;
                case SEARCH:
                    UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Liberty: Stop", maxRetries, isMultiple);
                    // For multiple projects, an additional dialog appears to select the project.
                    if (isMultiple) {
                        UIBotTestUtils.selectProjectFromDialog(remoteRobot, smMPProjName);
                    }
                    break;
                default:
                    fail("An invalid execution type of " + execType + " was requested.");
            }

            // Validate that the server stopped. Fail the test if the last iteration fails.
            try {
                boolean failOnError = (i == (maxRetries - 1));
                TestUtils.validateLibertyServerStopped(testName, absoluteWLPPath, 12, failOnError);
                break;
            } catch (Exception e) {
                // The Liberty tool window may flicker due to sudden indexing, which may cause an error.
                // Retry the action command and validation.
                TestUtils.printTrace(TestUtils.TraceSevLevel.INFO, "Retrying server stop. Cause: " + e.getMessage());
                TestUtils.sleepAndIgnoreException(5);
            }
        }
    }

    /**
     * Refreshed the Liberty tool window using the refresh icon.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void refreshLibertyToolWindow(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));

        // Click on the Liberty toolbar to give it focus.
        ComponentFixture libertyTWBar = projectFrame.getBaseLabel("Liberty", "10");
        libertyTWBar.click();

        String xPath = "//div[@class='LibertyExplorer']//div[@accessiblename.key='action.io.openliberty.tools.intellij.actions.RefreshLibertyToolbar.text']";
        ComponentFixture actionButton = projectFrame.getActionButton(xPath, "10");
        actionButton.click();
    }

    /**
     * Returns the frame type that is currently active.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @return The frame type that is currently active.
     */
    public static Frame getCurrentFrame(RemoteRobot remoteRobot) {
        Frame frame = null;
        int maxRetries = 10;
        for (int i = 0; i < maxRetries; i++) {
            if (inWelcomeFrame(remoteRobot)) {
                frame = Frame.WELCOME;
                break;
            } else if (inProjectFrame(remoteRobot)) {
                frame = Frame.PROJECT;
                break;
            }
        }

        return frame;
    }

    /**
     * Returns true if the frame currently active is the welcome frame. False, otherwise.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @return True if the frame currently active is the welcome frame. False, otherwise.
     */
    public static boolean inWelcomeFrame(RemoteRobot remoteRobot) {
        boolean inWelcomeFrame = false;
        try {
            remoteRobot.find(WelcomeFrameFixture.class, Duration.ofSeconds(5));
            inWelcomeFrame = true;
        } catch (Exception e) {
            // Not in welcome frame.
        }

        return inWelcomeFrame;
    }

    /**
     * Returns true if the frame currently active is the project frame. False, otherwise.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @return True if the frame currently active is the project frame. False, otherwise.
     */
    public static boolean inProjectFrame(RemoteRobot remoteRobot) {
        boolean inProjectFrame = false;
        try {
            remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(5));
            inProjectFrame = true;
        } catch (Exception e) {
            // Not in project frame.
        }

        return inProjectFrame;
    }

    /**
     * Removes the specified tool window from view.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param label       The tool window label.
     */
    public static void removeToolWindow(RemoteRobot remoteRobot, String label) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        try {
            ComponentFixture toolWindowLabel = projectFrame.getBaseLabel(label, "5");
            toolWindowLabel.rightClick();
            ComponentFixture removeSideBardAction = projectFrame.getActionMenuItem("Remove from Sidebar");
            removeSideBardAction.click();
        } catch (WaitForConditionTimeoutException e) {
            // The tool window is not active.
        }
    }

    /**
     * Closes an error dialog if it exists. The error dialog is identified by the error dialog icon.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void closeErrorDialog(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        DialogFixture errorDialog = null;
        ComponentFixture errorLabel = null;

        try {
            // Look for a dialog.
            Locator locator = byXpath("//div[@class='MyDialog']");
            errorDialog = projectFrame.find(DialogFixture.class, locator, Duration.ofSeconds(5));

            // Look for the error dialog label containing the error icon.
            Locator errorLabelLocator = byXpath("//div[@class='JLabel' and @defaulticon='errorDialog.svg']");
            errorLabel = errorDialog.find(ComponentFixture.class, errorLabelLocator, Duration.ofSeconds(5));
        } catch (WaitForConditionTimeoutException wftoe) {
            // A dialog was not found or it was not an error dialog.
        }

        // Close the error dialog if it was found.
        if (errorDialog != null && errorLabel != null) {
            JButtonFixture okButton = errorDialog.getButton("OK");
            RepeatUtilsKt.waitFor(Duration.ofSeconds(5),
                    Duration.ofSeconds(1),
                    "Waiting for the OK button on the error dialog to be enabled",
                    "The OK button on the error dialog was not enabled",
                    okButton::isEnabled);
            okButton.click();
        }
    }

    /**
     * Closes Terminal.
     */
    public static void closeTerminalTabs(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        try {
            ProjectFrameFixture.rightClickOnTerminalTab(projectFrame);
            ProjectFrameFixture.clickMenuOption(projectFrame, "action.CloseAllNotifications.text");

            while (true) {
                ComponentFixture terminateButton = projectFrame.find(ComponentFixture.class, byXpath("//div[@accessiblename='Terminate']"));
                if (terminateButton.callJs("component.isEnabled();", false)) {
                    terminateButton.click();
                    TestUtils.sleepAndIgnoreException(10);
                } else {
                    break; // Exit loop if no enabled "Terminate" button is found
                }
            }
        } catch (WaitForConditionTimeoutException e) {
            // The Terminal tab is most likely closed.
        }
    }

    public static void findWelcomeFrame(RemoteRobot remoteRobot) {
        try {
            remoteRobot.find(WelcomeFrameFixture.class, Duration.ofMinutes(2));
        } catch (WaitForConditionTimeoutException e) {
            // If the welcome frame is not found then there is a project loaded.
            // Close the editor files and close the project to get back to the welcome frame.
            if (remoteRobot.isMac()) {
                UIBotTestUtils.closeAllEditorTabs(remoteRobot);
            }
            else {
                UIBotTestUtils.runActionFromSearchEverywherePanel(remoteRobot, "Close All Tabs", 3);
            }
            UIBotTestUtils.closeProjectView(remoteRobot);
            UIBotTestUtils.closeProjectFrame(remoteRobot);
            TestUtils.sleepAndIgnoreException(30); // takes about 15s to render the whole Welcome page including the Open button on Mac.
            UIBotTestUtils.validateProjectFrameClosed(remoteRobot);
        }
    }

    /**
     * Clicks on the main menu button.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void clickOnMainMenu(RemoteRobot remoteRobot) {
        String mainMenuButton = "Main Menu";
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));

        try {
            // Clicking on the left toolbar ensures that the Main Menu button is active.
            String xPath = "//div[@accessiblename='Left Toolbar' and @class='ToolWindowLeftToolbar']";
            ComponentFixture actionButton = projectFrame.getActionButton(xPath, "10");
            actionButton.click();
            // Clicking on the Main Menu
            String xPathMainMenu = "//div[@accessiblename='" + mainMenuButton + "' and @class='ActionButton']";
            ComponentFixture actionButtonMainMenu = projectFrame.getActionButton(xPathMainMenu, "10");
            actionButtonMainMenu.click();

        } catch (WaitForConditionTimeoutException e) {
            // Main Menu button is not clicked, nothing to do
        }
    }

    /**
     * Right-clicks on a file tab and selects "Close" to close the file.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param fileName    The name of the file tab to be closed.
     */
    public static void rightClickCloseOnFileTab(RemoteRobot remoteRobot, String fileName) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));

        try {
            String xPath = "//div[@accessiblename='" + fileName + "' and @class='SimpleColoredComponent']";
            ComponentFixture actionButton = projectFrame.getActionButton(xPath, "10");
            actionButton.rightClick();
            ComponentFixture closeFixture = projectFrame.getActionMenuItem("Close");
            closeFixture.click();
            TestUtils.sleepAndIgnoreException(5);

        } catch (WaitForConditionTimeoutException e) {
            // file is not closed, nothing to do
        }
    }

    /**
     * Maximizes the Intellij ProjectFrame window in Windows.
     *
     * This method simulates pressing the Windows key + Up arrow to maximize the window,
     * but only if the window is not already maximized.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void maximizeWindow(RemoteRobot remoteRobot) {
        if (!isFullScreen(remoteRobot)) {
            Keyboard keyboard = new Keyboard(remoteRobot);
            keyboard.hotKey(VK_WINDOWS, VK_UP);
            keyboard.enter();
        }
    }

    /**
     * Minimizes the Intellij ProjectFrame window in Windows.
     *
     * This method simulates pressing the Windows key + Down arrow to minimize the window,
     * but only if the window is currently maximized.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void minimizeWindow(RemoteRobot remoteRobot) {
        if (isFullScreen(remoteRobot)) {
            Keyboard keyboard = new Keyboard(remoteRobot);
            keyboard.hotKey(VK_WINDOWS, VK_DOWN);
            keyboard.enter();
        }
    }

    /**
     * Determines if the IntelliJ window is currently full screen.
     *
     * This method checks the size of the IntelliJ window against the screen size to
     * decide if the window is maximized or not. If the window's width or height is
     * greater than or equal to the screen size, it is considered full screen.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @return true if the IntelliJ window is full screen, false otherwise.
     */
    public static boolean isFullScreen(RemoteRobot remoteRobot) {
        // Locate the IntelliJ IDEA main window
        ComponentFixture mainWindow = remoteRobot.find(
                ComponentFixture.class,
                Locators.byXpath("//div[@class='IdeFrameImpl']")
        );

        // Get the screen size
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        // Get the current dimensions (height and width) of the IntelliJ window.
        int intellijWindowHeight = mainWindow.getRemoteComponent().getHeight();
        int intellijWindowWidth = mainWindow.getRemoteComponent().getWidth();

        return intellijWindowWidth >= screenSize.width || intellijWindowHeight >= screenSize.height;
    }

    /**
     * Handles version-specific menu actions based on the IntelliJ IDEA version.
     *
     * @param remoteRobot        Instance of the RemoteRobot to interact with the IntelliJ UI.
     * @param menuAction2024_2   The submenu option for IntelliJ version 2024.2.
     * @param menuAction2024_3   The submenu option for IntelliJ version 2024.3.
     * @throws UnsupportedOperationException if the IntelliJ version is not supported.
     */
    public static String handleMenuBasedOnVersion(RemoteRobot remoteRobot, String menuAction2024_2, String menuAction2024_3) {
        // Using Remote robot's javascript API Retrieve the IntelliJ version
        String intellijVersion = remoteRobot.callJs("com.intellij.openapi.application.ApplicationInfo.getInstance().getFullVersion();");

        String menuAction2;
        if (intellijVersion.startsWith("2024.2")) {
            menuAction2 = menuAction2024_2;
        } else if (intellijVersion.startsWith("2024.3") || intellijVersion.startsWith("2025.1")) {
            menuAction2 = menuAction2024_3;
        } else {
            // If the version is unsupported, throw an exception to indicate the issue.
            throw new UnsupportedOperationException("Unsupported IntelliJ version: " + intellijVersion);
        }

        return menuAction2;
    }

    /**
     * Simulates a click on the "Load all" button.
     *
     * @param remoteRobot Instance of the RemoteRobot to interact with the IntelliJ UI.
     */
    public static void clickOnLoad(RemoteRobot remoteRobot) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));

        // XPath to locate the "Load all" button.
        String xPath = "//div[@accessiblename='Load all' and @class='JButton']";
        ComponentFixture actionButton = projectFrame.getActionButton(xPath, "10");
        actionButton.click();

        TestUtils.sleepAndIgnoreException(5);
    }
}
