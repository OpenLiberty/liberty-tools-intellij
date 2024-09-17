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

public class ViewIntegrationTestReport extends LibertyGeneralAction {

    /**
     * Returns the name of the action command being processed.
     *
     * @return The name of the action command being processed.
     */
    protected String getActionCommandName() {
        return LocalizedResourceUtil.getMessage("view.integration.test.report");
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

        // Dev mode runs the tests and it may have selected a report generator that uses one location and filename or another depending on the version number
        // Maven plugin maven-surefire-report-plugin v3.5 and above use this location and filename
        File failsafeReportFile = getReportFile(parentFile, "reports", "failsafe.html");
        List<String> reportNames = new ArrayList<>();
        reportNames.add(parentFile.toNioPath().relativize(failsafeReportFile.toPath()).toString());
        VirtualFile failsafeReportVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(failsafeReportFile);
        if (failsafeReportVirtualFile == null || !failsafeReportVirtualFile.exists()) {
            // Maven plugin maven-surefire-report-plugin v3.4 and below use this location and filename
            failsafeReportFile = getReportFile(parentFile, "site", "failsafe-report.html");
            reportNames.add(parentFile.toNioPath().relativize(failsafeReportFile.toPath()).toString());
            failsafeReportVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(failsafeReportFile);
        }

        if (failsafeReportVirtualFile == null || !failsafeReportVirtualFile.exists()) {
            Notification notif = new Notification(Constants.LIBERTY_DEV_DASHBOARD_ID,
                    LocalizedResourceUtil.getMessage("integration.test.report.does.not.exist.notification.title"),
                    LocalizedResourceUtil.getMessage("test.report.does.not.exist", reportNames),
                    NotificationType.ERROR);
            notif.setIcon(LibertyPluginIcons.libertyIcon);
            Notifications.Bus.notify(notif, project);
            LOGGER.debug("Integration test report does not exist at : " + failsafeReportFile.getAbsolutePath());
            return;
        }

        // open test report in browser
        BrowserUtil.browse(failsafeReportVirtualFile.getUrl());
    }

    @NotNull
    private File getReportFile(VirtualFile parentFile, String dir, String filename) {
        return Paths.get(parentFile.getPath(), "target", dir, filename).normalize().toAbsolutePath().toFile();
    }

}