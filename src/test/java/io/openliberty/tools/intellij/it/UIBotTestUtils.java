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

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.fixtures.*;
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText;
import com.intellij.remoterobot.search.locators.Locator;
import com.intellij.remoterobot.utils.Keyboard;
import com.intellij.remoterobot.utils.RepeatUtilsKt;
import com.intellij.remoterobot.utils.WaitForConditionTimeoutException;
import io.openliberty.tools.intellij.it.fixtures.DialogFixture;
import io.openliberty.tools.intellij.it.fixtures.ProjectFrameFixture;
import io.openliberty.tools.intellij.it.fixtures.WelcomeFrameFixture;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;

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
     * UI Frames.
     */
    public enum Frame {
        WELCOME, PROJECT
    }

    /**
     * Imports a project using the UI.
     *
     * @param remoteRobot  The RemoteRobot instance.
     * @param projectsPath The absolute path to the directory containing the projects.
     */
    public static void importProject(RemoteRobot remoteRobot, String projectsPath, String projectName) {
        // Trigger the open project dialog.
        CommonContainerFixture commonFixture = null;
        Frame currentFrame = getCurrentFrame(remoteRobot);
        if (currentFrame == null) {
            fail("Unable to identify the current window frame (i.e. welcome/project)");
        }

        if (currentFrame == Frame.WELCOME) {
            // From the welcome dialog.
            WelcomeFrameFixture welcomePage = remoteRobot.find(WelcomeFrameFixture.class, Duration.ofSeconds(10));
            commonFixture = welcomePage;
            ComponentFixture cf = welcomePage.getOpenProjectComponentFixture("Open");
            cf.click();
        } else if (currentFrame == Frame.PROJECT) {
            // From the project frame.
            ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(30));
            commonFixture = projectFrame;
            ComponentFixture fileMenuEntry = projectFrame.getActionMenu("File");
            fileMenuEntry.click();
            ComponentFixture openFixture = projectFrame.getActionMenuItem("Open...");
            openFixture.click(new Point());
        }

        // Specify the project's path. The text field is pre-populated by default.
        DialogFixture newProjectDialog = commonFixture.find(DialogFixture.class, DialogFixture.byTitle("Open File or Project"), Duration.ofSeconds(10));
        JTextFieldFixture textField = newProjectDialog.getBorderLessTextField();
        JButtonFixture okButton = newProjectDialog.getButton("OK");

        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(1),
                "Waiting for the OK button on the open project dialog to be enabled",
                "The OK button on the open project dialog was not enabled",
                okButton::isEnabled);

        String projectFullPath = Paths.get(projectsPath, projectName).toString();
        textField.setText(projectFullPath);
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(1),
                "Waiting for the text box on the Open \"File or Project\" dialog to be populated with the given value",
                "The text box on the Open \"File or Project\" dialog was not populated with the given value",
                () -> textField.getText().equals(projectFullPath));

        ComponentFixture projectTree = newProjectDialog.getTree();
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(1),
                "Waiting for project tree on the Open \"File or Project\" dialog to show the set project",
                "The project tree on the \"File or Project\" dialog did not show the set project",
                () -> projectTree.getData().hasText(projectName));

        // Click OK.
        okButton.click();

        // If in a project frame, choose where to open the project.
        if (currentFrame == Frame.PROJECT) {
            DialogFixture openProjectDialog = getOpenProjectLocationDialog(commonFixture);
            JButtonFixture thisWinButton = openProjectDialog.getButton("This Window");
            RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                    Duration.ofSeconds(1),
                    "Waiting for The \"This window\" button on the \"Open Project\" dialog to be enabled",
                    "The \"This window\" button on the \"Open Project\" dialog was not enable",
                    thisWinButton::isEnabled);
            thisWinButton.click();
        }

        // Need a buffer here.
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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
        // Click on File on the Menu bar.
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofMinutes(2));
        ComponentFixture fileMenuEntry = projectFrame.getActionMenu("File");
        fileMenuEntry.click();

        // Click on Close Project in the menu.
        ComponentFixture closeFixture = projectFrame.getActionMenuItem("Close Project");
        closeFixture.click();
    }

    /**
     * Runs a Liberty tool window action using the drop-down tree view.
     *
     * @param remoteRobot   The RemoteRobot instance.
     * @param action        The action to run
     * @param usePlayButton The indicator that specifies if play button should be used to run the action or not.
     */
    public static void runLibertyTWActionFromDropDownView(RemoteRobot remoteRobot, String action, boolean usePlayButton) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));

        // Click on the Liberty toolbar to give it focus.
        ComponentFixture libertyTWBar = projectFrame.getBaseLabel("Liberty", "10");
        libertyTWBar.click();

        // Check if the project tree was expanded and the action is showing.
        ComponentFixture treeFixture = projectFrame.getTree("LibertyTree", action, "60");
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(2),
                "Waiting for " + action + " in tree fixture to show and come into focus",
                "Action " + action + " in tree fixture is not showing or not in focus",
                treeFixture::isShowing);

        // Run the action.
        List<RemoteText> rts = treeFixture.findAllText();
        for (RemoteText rt : rts) {
            if (action.equals(rt.getText())) {
                int maxRetries = 3;
                Exception error = null;
                for (int i = 0; i < maxRetries; i++) {
                    try {
                        error = null;
                        if (usePlayButton) {
                            rt.click();
                            clickOnLibertyTWToolbarPlayButton(remoteRobot);
                        } else {
                            rt.doubleClick();
                        }
                        break;
                    } catch (Exception e) {
                        // The content of the Liberty tool window may blink in and out of existence. Retry.
                        TestUtils.printTrace(TestUtils.TraceSevLevel.INFO,
                                "Double click or play button click on Liberty tool window drop down action failed (" + e.getMessage() + "). Retrying...");
                        TestUtils.sleepAndIgnoreException(1);
                        error = e;
                    }
                }

                // Report the last error if there is one.
                if (error != null) {
                    throw new RuntimeException("Unable to run action from dropdown view.", error);
                }

                break;
            }
        }
    }

    /**
     * Runs a Liberty tool window action using the pop-up action menu.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param projectName The name of the project.
     * @param action      The action to run.
     */
    public static void runLibertyTWActionFromMenuView(RemoteRobot remoteRobot, String projectName, String action) {
        RemoteText project = findProjectInLibertyToolWindow(remoteRobot, projectName, "60");
        project.rightClick();
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        ComponentFixture menuAction = projectFrame.getActionMenuItem(action);
        menuAction.click();
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
     * @param treeItem    The project name to wait for.
     */
    public static void validateLibertyTWProjectTreeItemIsShowing(RemoteRobot remoteRobot, String treeItem) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));

        // There is a window between which the Liberty tool window content may come and
        // go when intellij detects indexing starts and ends. Try to handle it.
        try {
            projectFrame.getTree("LibertyTree", treeItem, "60");
        } catch (Exception e) {
            // Do nothing.
        }

        // Wait a bit and re-try. Indexing is a long process right now.
        TestUtils.sleepAndIgnoreException(10);
        ComponentFixture treeFixture = projectFrame.getTree("LibertyTree", treeItem, "360");
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                Duration.ofSeconds(2),
                "Waiting for Tree fixture to show",
                "Tree fixture is not showing",
                treeFixture::isShowing);
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
     * Opens the Liberty tool window if it is not already open.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void openLibertyToolWindow(RemoteRobot remoteRobot) {
        int maxRetries = 6;
        Exception error = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                error = null;
                ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
                projectFrame.getBaseLabel("Liberty", "5");
                break;
            } catch (WaitForConditionTimeoutException wfcte) {
                // The Liberty tool window is closed. Open it.
                clickOnWindowPaneStripeButton(remoteRobot, "Liberty");
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
        int maxRetries = 6;
        Exception error = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                error = null;
                ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
                projectFrame.getContentComboLabel("Project", "5");
                break;
            } catch (WaitForConditionTimeoutException wfcte) {
                // The project view is closed. Open it.
                clickOnWindowPaneStripeButton(remoteRobot, "Project");
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
     * Clicks on the specified tool window pane stripe.
     *
     * @param remoteRobot      The RemoteRobot instance.
     * @param StripeButtonName The name of the window pane stripe button.
     */
    public static void clickOnWindowPaneStripeButton(RemoteRobot remoteRobot, String StripeButtonName) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        ComponentFixture wpStripe = projectFrame.getStripeButton(StripeButtonName);
        wpStripe.click();
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
                Locator locator = byXpath("//div[@class='LibertyExplorer']//div[@class='ActionButton' and contains(@myaction.key, 'action.ExpandAll.text')]");
                ComponentFixture actionButton = projectFrame.getActionButton(locator);
                actionButton.click();

                // Click on the project node to give it focus. This action opens the editor tab showing
                // the build file.
                RemoteText projectRootNode = findProjectInLibertyToolWindow(remoteRobot, projectName, "10");
                projectRootNode.click();
                break;
            } catch (Exception e) {
                // The Liberty tool window content may blink in and out or hang. Retry.
                TestUtils.printTrace(TestUtils.TraceSevLevel.INFO,
                        "Unable to expand the Liberty tool window project tree (" + e.getMessage() + "). Retrying...");
                TestUtils.sleepAndIgnoreException(10);
                error = e;
            }
        }

        // Report the last error if there is one.
        if (error != null) {
            throw new RuntimeException("Unable to expand the Liberty tool window project tree.", error);
        }
    }

    /**
     * Closes the Project Tree for a given appName
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param appName     The Name of the application in tree to close
     */
    public static void closeProjectViewTree(RemoteRobot remoteRobot, String appName) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofMinutes(2));
        ComponentFixture appNameEntry = projectFrame.getProjectViewTree(appName);
        appNameEntry.findText(appName).doubleClick();
    }

    /**
     * Opens a server.xml file for a given app
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param appName     The string application name
     */
    public static void openServerXMLFile(RemoteRobot remoteRobot, String appName) {
        // Click on File on the Menu bar.
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofMinutes(2));

        // hide the terminal window for now
        UIBotTestUtils.hideTerminalWindow(remoteRobot);

        // get a JTreeFixture reference to the file project viewer entry
        JTreeFixture projTree = projectFrame.getProjectViewJTree(appName);
        if (!projTree.hasText("server.xml")) {
            projTree.expand(appName, "src", "main", "liberty", "config");
            projTree.findText("server.xml").doubleClick();
        } else {
            projTree.findText("server.xml").doubleClick();
        }
    }

    public static void hideTerminalWindow(RemoteRobot remoteRobot) {
        try {
            ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(2));
            Locator toolWindowHideButton = byXpath("//div[@class='ToolWindowHeader'][.//div[@myaction.key='action.NewPredefinedSession.label']]//div[@myaction.key='tool.window.hide.action.name']");
            ComponentFixture hideActionButton = projectFrame.getActionButton(toolWindowHideButton);
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
     * @param timeToWait    The time to wait for the editior tab close button.
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
     * @param timeToWait    The time to wait for the editior tab close button.
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
        ComponentFixture windowMenuEntry = projectFrame.getActionMenu("Window");
        windowMenuEntry.click();

        // Click on Editor Tabs in the menu.
        ComponentFixture editorTabsFixture = projectFrame.getChildActionMenu("Window", "Editor Tabs");
        editorTabsFixture.click();

        // Click on Close Project in the menu.
        ComponentFixture closeAllTabsFixture = projectFrame.getChildActionMenuItem("Window", "Close All Tabs");
        if (closeAllTabsFixture.callJs("component.isEnabled();", false)) {
            closeAllTabsFixture.click();
        }
    }

    /**
     * Moves the mouse cursor to a specific string target in server.xml
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param hoverTarget The string to hover over in server.xml
     */
    public static void hoverInAppServerXML(RemoteRobot remoteRobot, String hoverTarget) {

        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(30));
        EditorFixture editorNew = remoteRobot.find(EditorFixture.class, EditorFixture.Companion.getLocator());
        // click on editor pane to regain focus
        editorNew.click();


        // try to hover over target text
        editorNew.findText(hoverTarget).moveMouse();

        // jitter the cursor
        Point p = editorNew.findText(hoverTarget).getPoint();

        String jitterScript = "const x = %d;" +
                "const y = %d;" +
                "java.util.List.of(5, 20, 5, 15).forEach((i)=> {" +
                "const point = new Point(x + i, y);" +
                "robot.moveMouse(component, point);})";

        // run the jitter mouse script remotely in the idea
        editorNew.runJs(String.format(jitterScript, p.x, p.y));

        // monitor the popup window for the LS Hint text - there can be a delay getting it from the LS
        for (int i = 0; i < 3; i++) {
            // first get the contents of the popup - put in a String
            ContainerFixture popup = remoteRobot.find(ContainerFixture.class, byXpath("//div[@class='HeavyWeightWindow']"), Duration.ofSeconds(20));
            List<RemoteText> rts = popup.findAllText();
            String remoteString = "";
            for (RemoteText rt : rts) {
                remoteString = remoteString + rt.getText();
            }

            // Check for "Fetching Documentation" message indicating there is a delay in getting hint
            // allow some time for the LS hint to appear in the popup
            if (remoteString.contains("Fetching Documentation")) {
                TestUtils.sleepAndIgnoreException(2);
            } else {
                break;
            }
        }
    }

    /**
     * Moves the mouse cursor to a specific string target in server.xml
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void insertStanzaInAppServerXML(RemoteRobot remoteRobot, String projName, String stanzaSnippet, int line, int col, InsertionType type) {

        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(30));
        EditorFixture editorNew = remoteRobot.find(EditorFixture.class, EditorFixture.Companion.getLocator());
        editorNew.click();

        Keyboard keyboard = new Keyboard(remoteRobot);
        // find the location in the file to begin the stanza insertion
        goToLineAndColumn(remoteRobot, keyboard, line, col);

        if (type.name().equals("FEATURE")) {
            // if this is a feature stanza, hit enter to place it on the next line
            // in the featureManager Block
            keyboard.hotKey(VK_ENTER);
            // add the feature stanza using completion
            goToLineAndColumn(remoteRobot, keyboard, line + 1, col);
            keyboard.hotKey(VK_CONTROL, VK_SPACE);

            ContainerFixture popup = projectFrame.getDocumentationHintPopup();
            popup.findText("feature").doubleClick();


            // add a feature from the list of features using completion
            // col = 18 to take into account spacing + "<feature>"
            goToLineAndColumn(remoteRobot, keyboard, line + 1, 18);
        }

        // for either a FEATURE or a CONFIG stanza, insert where the cursor is currently located.
        keyboard.enterText(stanzaSnippet);
        // trigger type ahead popup
        keyboard.hotKey(VK_CONTROL, VK_SPACE);
        // select the completion suggestion in the popup
        keyboard.enter();

        // let the auto-save function of intellij save the file before testing it
        if (remoteRobot.isMac()) {
            keyboard.hotKey(VK_META, VK_S);
        } else {
            // linux + windows
            keyboard.hotKey(VK_CONTROL, VK_S);
        }

        TestUtils.sleepAndIgnoreException(5);
    }

    /**
     * Places the cursor at an exact location for text entry in file
     *
     * @param remoteRobot the remote robot instance
     * @param stanza      target stanza (fully formed) to remove
     */
    public static void deleteStanzaInAppServerXML(RemoteRobot remoteRobot, String stanza) {

        // remove the stanza that was added - keeps server.xml a known layout for any additional tests yet to come
        EditorFixture editorNew = remoteRobot.find(EditorFixture.class, EditorFixture.Companion.getLocator());
        Keyboard keyboard = new Keyboard(remoteRobot);
        editorNew.click();

        // select the target stanza text
        editorNew.selectText(stanza);
        TestUtils.sleepAndIgnoreException(5);

        // backspace/delete to remove it
        if (remoteRobot.isMac()) {
            keyboard.hotKey(VK_DELETE);
            keyboard.hotKey(VK_DELETE);
        } else {
            //win or linux
            keyboard.hotKey(VK_BACK_SPACE);
            keyboard.hotKey(VK_BACK_SPACE);
        }
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
     * Validates the expected hover string message was raised in popup.
     *
     * @param remoteRobot the remote robot instance
     */
    public static String getHoverStringData(RemoteRobot remoteRobot) {

        boolean found = false;

        // get the text from the LS diagnostic hint popup
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofMinutes(2));
        ContainerFixture popup = projectFrame.getDocumentationHintPopup();
        List<RemoteText> rts = popup.findAllText();

        // print out the string data found in the popup window - for debugging
        popup.findAllText().forEach((it) -> System.out.println(it.getText()));

        String popupString = "";
        for (RemoteText rt : rts) {
            popupString = popupString + rt.getText();
        }

        return popupString;

    }

    /**
     * Runs the start parameters run configuration dialog.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param startParams The parameters to set in the configuration dialog.
     */
    public static void runStartParamsConfigDialog(RemoteRobot remoteRobot, String startParams) {
        DialogFixture dialog = remoteRobot.find(DialogFixture.class, Duration.ofSeconds(19));
        if (startParams != null) {
            // Update the parameter editor box.
            // TODO: Investigate this further:
            // Currently blocked by the dialog editor box behind the start parameter text box
            // holding the editor's write lock.
            // One can only write when the dialog is being closed because the write lock
            // is released at that point.

            // Save the changes made.
            JButtonFixture applyButton = dialog.getButton("Apply");
            RepeatUtilsKt.waitFor(Duration.ofSeconds(10),
                    Duration.ofSeconds(1),
                    "Waiting for the Apply button on the open project dialog to be enabled",
                    "The Apply button on the open project dialog to be enabled",
                    applyButton::isEnabled);
            applyButton.click();
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
        Locator locator = byXpath("//div[@class='LibertyExplorer']//div[@class='ActionButton' and @tooltiptext.key='action.io.openliberty.tools.intellij.actions.RunLibertyDevTask.text']");
        ComponentFixture actionButton = projectFrame.getActionButton(locator);
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

            URL url = new URL(MavenSingleModAppTest.REMOTEBOT_URL);
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
     * Opens the search everywhere dialog.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void runActionFromSearchEverywherePanel(RemoteRobot remoteRobot, String action) {
        // Click on Navigate on the Menu bar.
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofMinutes(2));
        ComponentFixture navigateMenuEntry = projectFrame.getActionMenu("Navigate");
        navigateMenuEntry.click();

        // Click on Search Everywhere in the menu.
        ComponentFixture searchFixture = projectFrame.getActionMenuItem("Search Everywhere");
        searchFixture.click();

        // Click on the Actions tab
        ComponentFixture actionsTabFixture = projectFrame.getSETabLabel("Actions");
        actionsTabFixture.click();

        // Type the search string in the search dialog box.
        JTextFieldFixture searchField = projectFrame.textField(JTextFieldFixture.Companion.byType(), Duration.ofSeconds(10));
        searchField.click();
        searchField.setText(action);

        // Wait for the desired action to show in the search output frame and click on it.
        RepeatUtilsKt.waitFor(Duration.ofSeconds(20),
                Duration.ofSeconds(1),
                "Waiting for the search to filter and show " + action + " in search output",
                "The search did not filter or show " + action + " in the search output",
                () -> findTextInListOutputPanel(projectFrame, action) != null);

        RemoteText foundAction = findTextInListOutputPanel(projectFrame, action);
        if (foundAction != null) {
            foundAction.click();
        } else {
            fail("Search everywhere found " + action + ", but it can no longer be found after a subsequent attempt to find it.");
        }
    }

    /**
     * Selects the specified project from the list of detected projects shown in the 'Add Liberty project'
     * dialog.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param projectName The name of the project to select.
     */
    public static void selectProjectFromAddLibertyProjectDialog(RemoteRobot remoteRobot, String projectName) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        DialogFixture addProjectDialog = projectFrame.find(DialogFixture.class,
                DialogFixture.byTitle("Add Liberty project"),
                Duration.ofSeconds(10));
        JButtonFixture jbf = addProjectDialog.getBasicArrowButton();
        jbf.click();

        RemoteText remoteProject = findTextInListOutputPanel(addProjectDialog, projectName);
        remoteProject.click();
        JButtonFixture okButton = addProjectDialog.getButton("OK");
        okButton.click();
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

        RemoteText remoteProject = findTextInListOutputPanel(removeProjectDialog, projectName);
        remoteProject.click();

        JButtonFixture okButton = removeProjectDialog.getButton("OK");
        okButton.click();
    }

    /**
     * Waits the "No Liberty Maven or Liberty Gradle projects detected in this workspace" text
     * in the Liberty tool window. This text comes from Liberty Tools.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param waitTime    The time to wait for the required message.
     */
    public static void waitForLTWNoProjectDetectedMsg(RemoteRobot remoteRobot, String waitTime) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        String text = "No Liberty Maven or Liberty Gradle projects detected in this workspace.";
        ComponentFixture textArea = projectFrame.getTextArea(waitTime);

        RepeatUtilsKt.waitFor(Duration.ofSeconds(60),
                Duration.ofSeconds(1),
                "Waiting for text " + text + " to appear in Liberty tool window",
                "Text " + text + " did not appear in Liberty tool window",
                () -> readAllText(textArea).equals(text));
    }

    /**
     * Returns a concatenated string of all text found in a ComponentFixture object.
     *
     * @param componentFixture The ComponentFixture onject.
     * @return A concatenated string of all text found in a ComponentFixture object.
     */
    public static String readAllText(ComponentFixture componentFixture) {
        List<RemoteText> lines = componentFixture.findAllText();
        String fullText = "";
        for (RemoteText line : lines) {
            fullText += line.getText();
        }

        return fullText;
    }

    /**
     * Responds to the `Remove Liberty project` dialog query asking if the project should be deleted.
     * The response is the affirmative.
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
     * Returns the RemoteText object representing the text found in the list panel, or
     * null if the text was not found.
     *
     * @param fixture The CommonContainerFixture under which the search is taking place.
     * @param text    The text to search.
     * @return The RemoteText object representing the text found in the list panel, or
     * null if the text was not found.
     */
    public static RemoteText findTextInListOutputPanel(CommonContainerFixture fixture, String text) {
        RemoteText foundText = null;

        List<JListFixture> searchLists = fixture.jLists(JListFixture.Companion.byType());
        if (!searchLists.isEmpty()) {
            JListFixture searchList = searchLists.get(0);
            try {
                foundText = searchList.findText(text);
            } catch (NoSuchElementException nsee) {
                // The list is empty.
                return null;
            }
        }

        return foundText;
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
            remoteRobot.find(WelcomeFrameFixture.class, Duration.ofSeconds(2));
            inWelcomeFrame = true;
        } catch (Exception e) {
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
            remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(2));
            inProjectFrame = true;
        } catch (Exception e) {
        }

        return inProjectFrame;
    }
}
