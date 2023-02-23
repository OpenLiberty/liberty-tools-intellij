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
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.JButtonFixture;
import com.intellij.remoterobot.fixtures.JTextFieldFixture;
import com.intellij.remoterobot.fixtures.dataExtractor.RemoteText;
import com.intellij.remoterobot.utils.RepeatUtilsKt;
import io.openliberty.tools.intellij.it.fixtures.DialogFixture;
import io.openliberty.tools.intellij.it.fixtures.ProjectFrameFixture;
import io.openliberty.tools.intellij.it.fixtures.WelcomeFrameFixture;
import org.junit.Assert;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.List;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;
import static com.intellij.remoterobot.stepsProcessing.StepWorkerKt.step;

/**
 * Helper function.
 */
public class UIBotTestUtils {

    enum PrintTo {
        STDOUT, FILE
    }

    /**
     * Imports a project using the UI.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param projectPath The project's absolute path.
     */
    public static void importProject(RemoteRobot remoteRobot, String projectPath, String projectName) {
        step("Import Project", () -> {
            // Start the open project dialog.
            WelcomeFrameFixture welcomePage = remoteRobot.find(WelcomeFrameFixture.class, Duration.ofSeconds(10));
            ComponentFixture cf = welcomePage.getOpenProjectComponentFixture("Open");
            cf.click();

            // Specify the project's path.
            DialogFixture newProjectDialog = welcomePage.find(DialogFixture.class, DialogFixture.byTitle("Open File or Project"), Duration.ofSeconds(10));
            JTextFieldFixture jtf = newProjectDialog.find(JTextFieldFixture.class, byXpath("//div[@class='BorderlessTextField']"), Duration.ofSeconds(10));
            jtf.setText(projectPath);

            RepeatUtilsKt.waitFor(Duration.ofSeconds(10), Duration.ofSeconds(1), "Waiting for text box to be populated", "Text box was not populated", () -> jtf.getText().equals(projectPath));
            ComponentFixture treeFixture = newProjectDialog.getFixtureFromDialog(projectName, DialogFixture.Type.TREE);
            RepeatUtilsKt.waitFor(Duration.ofSeconds(10), Duration.ofSeconds(1), "Waiting for Tree fixture to show", "Tree fixture is not showing", () -> treeFixture.isShowing());

            // Click OK.
            JButtonFixture jbf = newProjectDialog.button("OK");
            RepeatUtilsKt.waitFor(Duration.ofSeconds(10), Duration.ofSeconds(1), "Waiting for OK button to be enabled", "OK button was not enabled", () -> jbf.isEnabled());
            jbf.click();
        });
    }

    /**
     * Closes the project frame.
     *
     * @param remoteRobot The RemoteRobot instance.
     */
    public static void closeProject(RemoteRobot remoteRobot) {

        // Click on File on the Menu bar.
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofMinutes(2));
        ComponentFixture fileMenuEntry = projectFrame.getFixtureFromFrame(ProjectFrameFixture.Type.ACTIONMENU, "File");
        fileMenuEntry.click();

        // Click on Close Project in the menu.
        ComponentFixture closeFixture = projectFrame.getFixtureFromFrame(ProjectFrameFixture.Type.ACTIONMENUITEM, "Close Project");
        closeFixture.click();
    }

    /**
     * Runs a dashboard action using the project's the drop-down menu view.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param action      The action to run
     */
    public static void runDashboardActionFromDropDownView(RemoteRobot remoteRobot, String action) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        ComponentFixture treeFixture = projectFrame.getFixtureFromFrame(ProjectFrameFixture.Type.TREE, action);
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10), Duration.ofSeconds(2), "Waiting for " + action + " in tree fixture to show", "Action " + action + " in tree fixture is not showing", () -> treeFixture.isShowing());
        List<RemoteText> rts = treeFixture.findAllText();
        for (RemoteText rt : rts) {
            if (action.equals(rt.getText())) {
                rt.doubleClick();
                break;
            }
        }
    }

    /**
     * Waits for the specified project to appear in the dashboard.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param projectName The project name to wait for.
     */
    public static void waitForProjectToShownInDashboard(RemoteRobot remoteRobot, String treeName, String projectName) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        ComponentFixture treeFixture = projectFrame.getFixtureFromFrame(ProjectFrameFixture.Type.TREE, treeName, projectName);
        RepeatUtilsKt.waitFor(Duration.ofSeconds(10), Duration.ofSeconds(2), "Waiting for Tree fixture to show", "Tree fixture is not showing", () -> treeFixture.isShowing());
    }

    /**
     * Opens a view using the specified tool window pane stripe
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param text        The name of the window pane stripe.
     */
    public static void openViewUsingToolWindowPaneStripe(RemoteRobot remoteRobot, String text) {
        // Click on File on the Menu bar.
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        ComponentFixture wpStripe = projectFrame.getFixtureFromFrame(ProjectFrameFixture.Type.SRIPEBUTTON, text);
        wpStripe.click();
    }

    /**
     * Clicks on an action button with the specified name and text.
     *
     * @param remoteRobot The RemoteRobot instance.
     * @param name        The name to use in search.
     * @param text        The text to use in search.
     */
    public static void clickOnActionButton(RemoteRobot remoteRobot, String name, String text) {
        ProjectFrameFixture projectFrame = remoteRobot.find(ProjectFrameFixture.class, Duration.ofSeconds(10));
        ComponentFixture actionButton = projectFrame.getFixtureFromFrame(ProjectFrameFixture.Type.ACTIONBUTTON, name, text);
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

            BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            try {
                FileWriter fw = new FileWriter("botCH.html");
                try {
                    String inputLine;
                    while ((inputLine = br.readLine()) != null) {
                        switch (printTo) {
                            case STDOUT:
                                System.out.println(inputLine);
                                break;
                            case FILE:
                                fw.write(inputLine);
                                fw.write("\n");
                                break;
                            default:
                                Assert.fail("Invalid format to write : ");
                        }
                    }
                } finally {
                    fw.close();
                }
            } finally {
                br.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Failed to collect UI Compponent Hierarchy information: " + e.getCause());
        }
    }
}
