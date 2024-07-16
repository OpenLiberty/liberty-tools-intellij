/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.LibertyModule;
import io.openliberty.tools.intellij.LibertyPluginIcons;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LibertyGradleUtil;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ViewTestReport extends LibertyGeneralAction {

    /**
     * Returns the name of the action command being processed.
     *
     * @return The name of the action command being processed.
     */
    protected String getActionCommandName() {
        return LocalizedResourceUtil.getMessage("view.gradle.test.report");
    }

    @Override
    protected List<String> getSupportedProjectTypes() {
        return List.of(Constants.LIBERTY_GRADLE_PROJECT);
    }

    @Override
    protected void executeLibertyAction(LibertyModule libertyModule) {
        Project project = libertyModule.getProject();
        VirtualFile buildFile = libertyModule.getBuildFile();

        // get path to project folder
        final VirtualFile parentFile = buildFile.getParent();

        // parse the build.gradle for test.reports.html.destination
        File testReportFile = null;
        String testReportDest = null;
        try {
            testReportDest = getTestReportDestination(buildFile);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        if (testReportDest != null) {
            testReportFile = new File(testReportDest);
            if (!testReportFile.exists()) {
                try {
                    testReportFile = findCustomTestReport(parentFile);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }

        if (testReportFile == null || !testReportFile.exists()) {
            // if does not exist look in default location: "build", "reports", "tests", "test", "index.html"
            testReportFile = Paths.get(parentFile.getPath(), "build", "reports", "tests", "test", "index.html").normalize().toAbsolutePath().toFile();
        }

        VirtualFile testReportVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(testReportFile);
        if (testReportVirtualFile == null || !testReportVirtualFile.exists()) {
            Notification notif = new Notification(Constants.LIBERTY_DEV_DASHBOARD_ID,
                    LocalizedResourceUtil.getMessage("gradle.test.report.does.not.exist"),
                    LocalizedResourceUtil.getMessage("test.report.does.not.exist", testReportFile.getAbsolutePath()),
                    NotificationType.ERROR);
            notif.setIcon(LibertyPluginIcons.libertyIcon);

            Notifications.Bus.notify(notif, project);
            LOGGER.debug("Gradle test report does not exist at : " + testReportFile.getAbsolutePath());
            return;
        }

        // open test report in browser
        BrowserUtil.browse(testReportVirtualFile.getUrl());
    }

    private String getTestReportDestination(VirtualFile file) throws IOException {
        String buildFile = LibertyGradleUtil.fileToString(file.getPath());
        String testReportRegex = "(?<=reports.html.destination[\\s\\=|\\=]).*([\"|'])(.*)([\"|'])";

        Pattern pattern = Pattern.compile(testReportRegex);
        Matcher matcher = pattern.matcher(buildFile);
        if (matcher.find()) {
            if (!matcher.group(2).isEmpty()) {
                // group 2 is the string enclosed in quotation marks
                return matcher.group(2);
            }
        }
        return null;
    }


    private File findCustomTestReport(VirtualFile parentFile) throws IOException {
        // look for the most recently modified index.html files in the workspace
        ArrayList<File> customTestReports = new ArrayList<File>();
        try (Stream<Path> walk = Files.walk(Paths.get(parentFile.getPath()))
                .filter(Files::isRegularFile)) {
            List<String> result = walk.map(x -> x.toString())
                    // exclude files from {bin, classes, target} dirs
                    .filter(f -> !f.contains("bin") || !f.contains("classes") || !f.contains("target"))
                    .filter(f -> f.endsWith("index.html")).collect(Collectors.toList());

            // check to see if the index.html contains the TEST_REPORT_STRING
            for (String s : result) {
                String file = LibertyGradleUtil.fileToString(s);
                if (file.contains(Constants.TEST_REPORT_STRING)) {
                    File newFile = new File(s);
                    if (newFile.exists()) {
                        customTestReports.add(newFile);
                    }
                }
            }

            if (customTestReports.size() > 1) {
                File mostRecentlyModified = customTestReports.get(0);
                for (File f : customTestReports) {
                    if (f.lastModified() > mostRecentlyModified.lastModified()) {
                        mostRecentlyModified = f;
                    }
                }
                // return the most recently modified file
                return mostRecentlyModified;
            } else if (customTestReports.size() == 1) {
                return customTestReports.get(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}