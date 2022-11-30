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
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.LibertyModule;
import io.openliberty.tools.intellij.LibertyModules;
import io.openliberty.tools.intellij.LibertyPluginIcons;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

import java.util.Arrays;
import java.util.List;

public class LibertyGeneralAction extends AnAction {

    protected Logger LOGGER = Logger.getInstance(LibertyGeneralAction.class);
    protected Project project;
    protected LibertyModule libertyModule;
    protected String projectName;
    protected String projectType;
    protected VirtualFile buildFile;
    protected String actionCmd;

    /**
     * Set the LibertyModule that the action should run on
     *
     * @param libertyModule
     */
    public void setLibertyModule(LibertyModule libertyModule) {
        this.libertyModule = libertyModule;
        this.project = libertyModule.getProject();
        this.projectName = libertyModule.getName();
        this.projectType = libertyModule.getProjectType();
        this.buildFile = libertyModule.getBuildFile();
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (project == null) {
            project = LibertyProjectUtil.getProject(e.getDataContext());
            // if still null deliver error message
            if (project == null) {
                // TODO prompt user to select project
                String msg = LocalizedResourceUtil.getMessage("liberty.project.does.not.resolve", actionCmd);
                notifyError(msg);
                LOGGER.warn(msg);
                return;
            }
        }
        if (buildFile == null) {
            buildFile = (VirtualFile) e.getDataContext().getData(Constants.LIBERTY_BUILD_FILE);
        }
        boolean isActionFromShiftShift = "GoToAction".equalsIgnoreCase(e.getPlace());
        // if still null, or it is from shift-shift, then prompt for the user to select
        if (isActionFromShiftShift || buildFile == null) {
            List<LibertyModule> libertyModules = LibertyModules.getInstance().getLibertyModules(getSupportedProjectTypes());
            if (!libertyModules.isEmpty()) {
                // Only one project. Select it.
                if (libertyModules.size() == 1) {
                    setLibertyModule(libertyModules.get(0));
                }
                // Multiple projects. Pop up dialog for user to select.
                else {
                    final String[] projectNames = toProjectNames(libertyModules);
                    final int ret = Messages.showChooseDialog(project,
                            LocalizedResourceUtil.getMessage("liberty.project.file.selection.dialog.message", actionCmd),
                            LocalizedResourceUtil.getMessage("liberty.project.file.selection.dialog.title"),
                            LibertyPluginIcons.libertyIcon_40,
                            projectNames,
                            projectNames[0]);
                    if (ret >= 0 && ret < libertyModules.size()) {
                        setLibertyModule(libertyModules.get(ret));
                    }
                    // The user pressed cancel on the dialog.
                    else {
                        return;
                    }
                }
            }
            // if buildFile is still null, deliver error message
            if (buildFile == null) {
                String msg = LocalizedResourceUtil.getMessage("liberty.build.file.does.not.resolve", actionCmd, project.getName());
                notifyError(msg);
                LOGGER.warn(msg);
                return;
            }
        }


        if (projectName == null) {
            projectName = (String) e.getDataContext().getData(Constants.LIBERTY_PROJECT_NAME);
        }
        if (projectType == null) {
            projectType = (String) e.getDataContext().getData(Constants.LIBERTY_PROJECT_TYPE);
        }
        if (projectType == null || (!projectType.equals(Constants.LIBERTY_MAVEN_PROJECT) && !projectType.equals(Constants.LIBERTY_GRADLE_PROJECT))) {
            String msg = LocalizedResourceUtil.getMessage("liberty.project.type.invalid", actionCmd, projectName);
            notifyError(msg);
            LOGGER.warn(msg);
            return;
        }
        executeLibertyAction();
    }

    /* Returns project type(s) applicable to this action. */
    protected List<String> getSupportedProjectTypes() {
        return Arrays.asList(Constants.LIBERTY_MAVEN_PROJECT, Constants.LIBERTY_GRADLE_PROJECT);

    }

    protected final String[] toProjectNames(@NotNull List<LibertyModule> list) {
        final int size = list.size();
        final String[] projectNames = new String[size];
        for (int i = 0; i < size; ++i) {
            projectNames[i] = list.get(i).getName();
        }
        return projectNames;
    }

    protected void setActionCmd(String actionCmd) {
        this.actionCmd = actionCmd;
    }

    protected void executeLibertyAction() {
        // must be implemented by individual actions
    }

    /**
     * Displays error message dialog to user
     *
     * @param errMsg
     */
    protected void notifyError(String errMsg) {
        Notification notif = new Notification(Constants.LIBERTY_DEV_DASHBOARD_ID,
                LibertyPluginIcons.libertyIcon,
                LocalizedResourceUtil.getMessage("liberty.action.cannot.start"),
                "",
                errMsg,
                NotificationType.WARNING,
                NotificationListener.URL_OPENING_LISTENER);
        Notifications.Bus.notify(notif, project);
    }

    /**
     * Returns the Terminal widget for the corresponding Liberty module
     *
     * @param createWidget create Terminal widget if it does not already exist
     * @return ShellTerminalWidget
     */
    protected ShellTerminalWidget getTerminalWidget(boolean createWidget) {
        ShellTerminalWidget widget = LibertyProjectUtil.getTerminalWidget(project, libertyModule, createWidget);
        // Shows error for actions where terminal widget does not exist or action requires a terminal to already exist and expects "Start" to be running
        if (widget == null || (!createWidget && !widget.hasRunningCommands())) {
            String msg;
            if (createWidget) {
                msg = LocalizedResourceUtil.getMessage("liberty.terminal.cannot.resolve", actionCmd, projectName);
            } else {
                msg = LocalizedResourceUtil.getMessage("liberty.dev.not.started.notification.content", actionCmd, projectName);
            }
            notifyError(msg);
            LOGGER.warn(msg);
            return null;
        }
        widget.getComponent().setVisible(true);
        widget.getComponent().requestFocus();
        return widget;
    }
}
