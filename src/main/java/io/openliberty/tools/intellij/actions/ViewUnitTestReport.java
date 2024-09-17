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
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ViewUnitTestReport extends LibertyGeneralAction {

    /**
     * Returns the name of the action command being processed.
     *
     * @return The name of the action command being processed.
     */
    protected String getActionCommandName() {
        return LocalizedResourceUtil.getMessage("view.unit.test.report");
    }

    @Override
    protected List<String> getSupportedProjectTypes() {
        return List.of(Constants.LIBERTY_MAVEN_PROJECT);
    }

    @Override
    protected void executeLibertyAction(LibertyModule libertyModule) {
        Project project = libertyModule.getProject();
        VirtualFile buildFile = libertyModule.getBuildFile();

        // get path to project folder
        final VirtualFile parentFile = buildFile.getParent();

        // Dev mode runs the tests and it may have selected a report generator that uses one location or another depending on the version number
        // Maven plugin maven-surefire-report-plugin v3.5 and above use this location
        String reportNameNo1 = "", reportNameNo2 = "";
        File surefireReportFile = getReportFile(parentFile, "reports", "surefire.html");
        reportNameNo1 = parentFile.toNioPath().relativize(surefireReportFile.toPath()).toString();
        VirtualFile surefireReportVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(surefireReportFile);
        if (surefireReportVirtualFile == null || !surefireReportVirtualFile.exists()) {
            // Maven plugin maven-surefire-report-plugin v3.4 and below use this location
            surefireReportFile = getReportFile(parentFile,"site", "surefire-report.html");
            reportNameNo2 = parentFile.toNioPath().relativize(surefireReportFile.toPath()).toString();
            surefireReportVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(surefireReportFile);
        }

        if (surefireReportVirtualFile == null || !surefireReportVirtualFile.exists()) {
            String displayNames = reportNameNo1 + " or " + reportNameNo2;
            Notification notif = new Notification(Constants.LIBERTY_DEV_DASHBOARD_ID,
                    LocalizedResourceUtil.getMessage("unit.test.report.does.not.exist"),
                    LocalizedResourceUtil.getMessage("test.report.does.not.exist", displayNames),
                    NotificationType.ERROR);
            notif.setIcon(LibertyPluginIcons.libertyIcon);

            Notifications.Bus.notify(notif, project);
            LOGGER.debug("Unit test report does not exist at : " + surefireReportFile.getAbsolutePath());
            return;
        }

        // open test report in browser
        BrowserUtil.browse(surefireReportVirtualFile.getUrl());
    }

    @NotNull
    private File getReportFile(VirtualFile parentFile, String dir, String filename) {
        return Paths.get(parentFile.getPath(), "target", dir, filename).normalize().toAbsolutePath().toFile();
    }
}