/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation.
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
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.LibertyPluginIcons;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class ViewUnitTestReport extends LibertyGeneralAction {

    public ViewUnitTestReport() {
        setActionCmd(LocalizedResourceUtil.getMessage("view.unit.test.report"));
    }

    @Override
    protected List<String> getSupportedProjectTypes() {
        return Arrays.asList(Constants.LIBERTY_MAVEN_PROJECT);
    }

    @Override
    protected void executeLibertyAction() {
        // get path to project folder
        final VirtualFile parentFile = buildFile.getParent();
        File surefireReportFile = Paths.get(parentFile.getCanonicalPath(), "target", "site", "surefire-report.html").normalize().toAbsolutePath().toFile();
        VirtualFile surefireReportVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(surefireReportFile);

        if (surefireReportVirtualFile == null || !surefireReportVirtualFile.exists()) {
            Notification notif = new Notification(Constants.LIBERTY_DEV_DASHBOARD_ID
                    , LibertyPluginIcons.libertyIcon
                    , LocalizedResourceUtil.getMessage("unit.test.report.does.not.exist")
                    , ""
                    , LocalizedResourceUtil.getMessage("test.report.does.not.exist", surefireReportFile.getAbsolutePath())
                    , NotificationType.ERROR
                    , NotificationListener.URL_OPENING_LISTENER);
            Notifications.Bus.notify(notif, project);
            LOGGER.debug("Unit test report does not exist at : " + surefireReportFile.getAbsolutePath());
            return;
        }

        // open test report in browser
        BrowserUtil.browse(surefireReportVirtualFile.getUrl());
    }

}