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

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.LibertyPluginIcons;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;
import org.jetbrains.annotations.NotNull;

public class LibertyGeneralAction extends AnAction {

    protected Logger log = Logger.getInstance(LibertyGeneralAction.class);
    protected Project project;
    protected String projectName;
    protected String projectType;
    protected VirtualFile buildFile;
    protected String actionCmd;

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        project = LibertyProjectUtil.getProject(e.getDataContext());
        if (project == null) {
            String msg = LocalizedResourceUtil.getMessage("liberty.project.does.not.resolve", actionCmd);
            notifyError(msg);
            log.debug(msg);
            return;
        }

        buildFile = (VirtualFile) e.getDataContext().getData(Constants.LIBERTY_BUILD_FILE);
        if (buildFile == null) {
            String msg = LocalizedResourceUtil.getMessage("liberty.build.file.does.not.resolve", actionCmd, project.getName());
            notifyError(msg);
            log.debug(msg);
            return;
        }

        projectName = (String) e.getDataContext().getData(Constants.LIBERTY_PROJECT_NAME);
        projectType = (String) e.getDataContext().getData(Constants.LIBERTY_PROJECT_TYPE);
        executeLibertyAction();
    }

    protected void setActionCmd(String actionCmd) {
        this.actionCmd = actionCmd;
    }

    protected void executeLibertyAction() {
        // must be implemented by individual actions
    }

    protected void notifyError(String errMsg) {
        Notification notif = new Notification(Constants.LIBERTY_DEV_DASHBOARD_ID
                , LibertyPluginIcons.libertyIcon
                , LocalizedResourceUtil.getMessage("liberty.action.cannot.start")
                , ""
                , errMsg
                , NotificationType.WARNING
                , NotificationListener.URL_OPENING_LISTENER);
        Notifications.Bus.notify(notif, project);
    }

}
