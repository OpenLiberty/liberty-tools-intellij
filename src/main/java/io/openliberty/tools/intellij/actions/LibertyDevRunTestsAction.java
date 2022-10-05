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
import io.openliberty.tools.intellij.LibertyPluginIcons;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LibertyActionUtil;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

public class LibertyDevRunTestsAction extends LibertyGeneralAction {

    public LibertyDevRunTestsAction() {
        setActionCmd(LocalizedResourceUtil.getMessage("run.tests.liberty.dev"));
    }

    @Override
    protected void executeLibertyAction() {
        String runTestsCommand = " ";
        ShellTerminalWidget widget = LibertyProjectUtil.getTerminalWidget(project, projectName, false);

        if (widget == null) {
            Notification notif = new Notification(Constants.LIBERTY_DEV_DASHBOARD_ID
                    , LibertyPluginIcons.libertyIcon
                    , LocalizedResourceUtil.getMessage("liberty.dev.not.started.notification.title")
                    , ""
                    , LocalizedResourceUtil.getMessage("liberty.dev.not.started.notification.content", projectName)
                    , NotificationType.WARNING
                    , NotificationListener.URL_OPENING_LISTENER);
            Notifications.Bus.notify(notif, project);
            LOGGER.error("Cannot run tests, corresponding project terminal does not exist.");
            return;
        }
        LibertyActionUtil.executeCommand(widget, runTestsCommand);
    }

}