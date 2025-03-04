/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.LibertyModule;
import io.openliberty.tools.intellij.LibertyModules;
import io.openliberty.tools.intellij.LibertyPluginIcons;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public abstract class LibertyGeneralAction extends AnAction {
    protected static final Logger LOGGER = Logger.getInstance(LibertyGeneralAction.class);

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // Schedule actions on the event dispatching thread.
        // See: https://plugins.jetbrains.com/docs/intellij/basic-action-system.html#principal-implementation-overrides.
        return ActionUpdateThread.EDT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String actionCmd = getActionCommandName();
        Project project = e.getProject();
        if (project == null) {
            // TODO prompt user to select project
            String msg = LocalizedResourceUtil.getMessage("liberty.project.does.not.resolve", actionCmd);
            notifyError(msg, project);
            LOGGER.warn(msg);
            return;
        }

        // Obtain the liberty module associated to the current action.
        LibertyModule libertyModule = null;
        //Using DataKey instead of a string because the getData(String) method is deprecated
        // and does not work as expected in IntelliJ 2024.2 and later versions.
        // This ensures compatibility and proper retrieval of the build file across all supported versions.
        VirtualFile buildFile = e.getDataContext().getData(Constants.LIBERTY_BUILD_FILE_DATAKEY);
        if (buildFile != null) {
            // The action is being driven from the project drop-down tree menu or from the project context menu.
            libertyModule = LibertyModules.getInstance().getLibertyModule(buildFile);
        } else {
            // The action is being driven from the shift-shift dialog.
            List<LibertyModule> libertyModules = LibertyModules.getInstance().getLibertyModules(project, getSupportedProjectTypes());
            if (!libertyModules.isEmpty()) {
                if (libertyModules.size() == 1) {
                    libertyModule = libertyModules.get(0);
                } else {
                    // projectNames can contain entries that are of the same name if there are two
                    // or more projects found using the same name but differentiated by existing in separate
                    // project folders.
                    final String[] projectNames = toProjectNames(libertyModules);

                    // tooltip strings will appear when user hovers over one of the projects in the dialog list
                    // they will display the fully qualified path to the build file so that users can
                    // differentiate in the case of same named application names in the chooser list
                    final String[] projectNamesTooltips = toProjectNamesTooltips(libertyModules);

                    // Create a Chooser Dialog for multiple projects. User can select which one they
                    // are interested in from list
                    LibertyProjectChooserDialog libertyChooserDiag = new LibertyProjectChooserDialog(
                            project,
                            LocalizedResourceUtil.getMessage("liberty.project.file.selection.dialog.message", actionCmd),
                            LocalizedResourceUtil.getMessage("liberty.project.file.selection.dialog.title"),
                            LibertyPluginIcons.libertyIcon_40,
                            projectNames,
                            projectNamesTooltips,
                            projectNames[0]);
                    libertyChooserDiag.show();
                    final int ret = libertyChooserDiag.getSelectedIndex();

                    if (ret >= 0 && ret < libertyModules.size()) {
                        libertyModule = libertyModules.get(ret);
                    } else {
                        // The user pressed cancel on the dialog. No need to show an error message.
                        return;
                    }
                }
            }
        }

        // If the module associated with this action could not be found, deliver error message.
        if (libertyModule == null) {
            String msg = LocalizedResourceUtil.getMessage("liberty.build.file.does.not.resolve", actionCmd, project.getName());
            notifyError(msg, project);
            LOGGER.warn(msg);
            return;
        }

        Constants.ProjectType projectType = libertyModule.getProjectType();
        if (projectType == null) {
            String msg = LocalizedResourceUtil.getMessage("liberty.project.type.invalid", actionCmd, libertyModule.getName());
            notifyError(msg, project);
            LOGGER.warn(msg);
            return;
        }

        executeLibertyAction(libertyModule);
    }

    /* Returns project type(s) applicable to this action. */
    protected List<Constants.ProjectType> getSupportedProjectTypes() {
        return Arrays.asList(Constants.ProjectType.LIBERTY_MAVEN_PROJECT, Constants.ProjectType.LIBERTY_GRADLE_PROJECT);
    }

    protected final String[] toProjectNames(@NotNull List<LibertyModule> list) {
        return toArray(list, item -> {
            // We need a differentiator for the Shift-Shift Dialog Chooser in the event two projects have the
            // same name. get the build dir name where the build file resides to be that differentiator.
            String parentFolderName = item.getBuildFile().getParent().getName();

            // Use the parent folder name as a differentiator - add it to the string entry as
            // part of the project name
            // Entry in list will be of the form: "<app-name> : <buildfile parent folder>"
            return item.getName() + ": " + parentFolderName;
        });
    }

    protected final String[] toProjectNamesTooltips(@NotNull List<LibertyModule> list) {
        return toArray(list, item -> String.valueOf(item.getBuildFile().toNioPath()));
    }

    private String[] toArray(@NotNull List<LibertyModule> list, @NotNull Function<LibertyModule, String> mapper) {
        return list.stream().map(mapper).toArray(String[]::new);
    }

    /**
     * Displays error message dialog to user
     *
     * @param errMsg
     */
    protected void notifyError(String errMsg, Project project) {
        Notification notif = new Notification(Constants.LIBERTY_DEV_DASHBOARD_ID,
                LocalizedResourceUtil.getMessage("liberty.action.cannot.start"), errMsg, NotificationType.WARNING);
        notif.setIcon(LibertyPluginIcons.libertyIcon);
        Notifications.Bus.notify(notif, project);
    }

    /**
     * Returns the Terminal widget for the corresponding Liberty module
     *
     * @param createWidget create Terminal widget if it does not already exist
     * @return ShellTerminalWidget
     */
    protected ShellTerminalWidget getTerminalWidgetWithFocus(boolean createWidget, Project project, VirtualFile buildFile, String actionCmd) {
        LibertyModule libertyModule = LibertyModules.getInstance().getLibertyModule(buildFile);
        TerminalToolWindowManager terminalToolWindowManager = TerminalToolWindowManager.getInstance(project);
        // look for existing terminal tab
        ShellTerminalWidget existingWidget = LibertyProjectUtil.getTerminalWidget(libertyModule, terminalToolWindowManager);
        // look for creating new terminal tab
        ShellTerminalWidget widget = LibertyProjectUtil.getTerminalWidget(project, libertyModule, createWidget, terminalToolWindowManager, existingWidget);
        // Set Focus to existing terminal widget
        LibertyProjectUtil.setFocusToWidget(project, existingWidget);

        // Shows error for actions where terminal widget does not exist or action requires a terminal to already exist and expects "Start" to be running
        if (widget == null || (!createWidget && !widget.hasRunningCommands())) {
            String msg;
            if (createWidget) {
                msg = LocalizedResourceUtil.getMessage("liberty.terminal.cannot.resolve", actionCmd, project.getName());
            } else {
                msg = LocalizedResourceUtil.getMessage("liberty.dev.not.started.notification.content", actionCmd, project.getName(), System.lineSeparator());
            }
            notifyError(msg, project);
            LOGGER.warn(msg);
            return null;
        }
        return widget;
    }

    /**
     * Processes the action associated with the specified Liberty module.
     *
     * @param libertyModule The Liberty module object on which the action is processed.
     */
    protected abstract void executeLibertyAction(LibertyModule libertyModule);

    /**
     * Returns the name of the action command being processed.
     *
     * @return The string representation of the action command being processed.
     */
    protected abstract String getActionCommandName();
}
