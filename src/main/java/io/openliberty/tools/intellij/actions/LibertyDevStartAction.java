/*******************************************************************************
 * Copyright (c) 2020, 2026 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.LibertyModule;
import io.openliberty.tools.intellij.LibertyModules;
import io.openliberty.tools.intellij.util.*;
import io.openliberty.tools.intellij.util.LibertyTerminalWatcher;
import static io.openliberty.tools.intellij.util.Constants.ProjectType.*;
import static io.openliberty.tools.intellij.util.Constants.*;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

import java.io.IOException;

/**
 * Runs the dev mode start command on the corresponding Liberty module.
 */
public class LibertyDevStartAction extends LibertyGeneralAction {

    /**
     * Returns the name of the action command being processed.
     *
     * @return The name of the action command being processed.
     */
    protected String getActionCommandName() {
        return LocalizedResourceUtil.getMessage("start.liberty.dev");
    }

    @Override
    protected void executeLibertyAction(LibertyModule libertyModule) {
        runInTerminal(libertyModule, false);
    }

    protected void runInTerminal(LibertyModule libertyModule, boolean runInContainer) {
        Project project = libertyModule.getProject();
        VirtualFile buildFile = libertyModule.getBuildFile();
        Constants.ProjectType projectType = libertyModule.getProjectType();
        ShellTerminalWidget widget = getTerminalWidgetWithFocus(true, project, buildFile, getActionCommandName());
        if (widget == null) {
            return;
        }

        String startCmd;
        int debugPort = -1;
        DebugModeHandler debugHandler = new DebugModeHandler();
        // For child modules the wrapper (mvnw/gradlew) lives in the parent directory.
        // Pass the parent's build file so that the settings command lookup finds it there.
        LibertyModule parentModule = libertyModule.getParentModule();
        VirtualFile settingsBuildFile = (parentModule != null && parentModule.getBuildFile() != null)
                ? parentModule.getBuildFile()
                : buildFile;

        String buildSettingsCmd;
        try {
            if (projectType.equals(LIBERTY_MAVEN_PROJECT)) {
                buildSettingsCmd = LibertyMavenUtil.getMavenSettingsCmd(project, settingsBuildFile);
            } else {
                buildSettingsCmd = LibertyGradleUtil.getGradleSettingsCmd(project, settingsBuildFile);
            }
        } catch (LibertyException ex) {
            // in this case, the settings specified to mvn or gradle are invalid and an error was launched by getMavenSettingsCmd or getGradleSettingsCmd.
            // Log a warning because a Logger.error creates an entry on "IDE Internal Errors", which we do not want.
            LOGGER.warn(ex.getMessage());
            notifyError(ex.getTranslatedMessage(), project);
            return;
        }

        // Handle Liberty Explorer (dashboard) Start action
        // Also handle Start... action when LibertyRunConfiguration calls this
        String start = buildSettingsCmd + (projectType.equals(LIBERTY_MAVEN_PROJECT) ? LIBERTY_MAVEN_START_CMD : LIBERTY_GRADLE_START_CMD);
        String startInContainer = buildSettingsCmd + (projectType.equals(LIBERTY_MAVEN_PROJECT) ? LIBERTY_MAVEN_START_CONTAINER_CMD : LIBERTY_GRADLE_START_CONTAINER_CMD);
        if (runInContainer) {
            startCmd = startInContainer;
        } else if (libertyModule.isCustom()) {
            startCmd = libertyModule.runInContainer() ? startInContainer : start;
            startCmd += libertyModule.getCustomStartParams();
        } else {
            startCmd = start;
        }
        if (libertyModule.isDebugMode()) {
            try {
                String debugParam = projectType.equals(LIBERTY_MAVEN_PROJECT) ? LIBERTY_MAVEN_DEBUG_PARAM : LIBERTY_GRADLE_DEBUG_PARAM;
                debugPort = debugHandler.getDebugPort(libertyModule);
                String debugStr = debugParam + debugPort;
                // do not append if debug port is already specified as part of start command
                if (!startCmd.contains(debugStr)) {
                    startCmd += " " + debugParam + debugPort;
                }
            } catch (IOException e) {
                String msg = LocalizedResourceUtil.getMessage("liberty.debug.port.unresolved", getActionCommandName(), project.getName());
                notifyError(msg, project);
                LOGGER.error(msg);
            }
        }

        // Do not use the custom parameters in the future unless we get here via the run configuration dialog
        libertyModule.setUseCustom(false);

        // For child modules in a multi-module build, run from the parent directory and
        // append the module selector argument. The buildSettingsCmd already contains the
        // correct wrapper/executable; we only need to adjust the working directory and
        // append the module selector.
        String executionDir;
        if (projectType.equals(LIBERTY_MAVEN_PROJECT)) {
            executionDir = LibertyMavenUtil.getMavenExecutionDir(libertyModule);
            String moduleArgs = LibertyMavenUtil.getMavenModuleArgs(libertyModule);
            if (!moduleArgs.isEmpty()) {
                startCmd = startCmd + moduleArgs;
            }
        } else {
            executionDir = LibertyGradleUtil.getGradleExecutionDir(libertyModule);
            // For Gradle child modules the task is already qualified via getGradleTaskForModule
            // inside the start command constants — re-build the start cmd with a qualified task.
            if (libertyModule.getParentModule() != null) {
                String baseTask = runInContainer ? "libertyDevc" : "libertyDev";
                String qualifiedTask = LibertyGradleUtil.getGradleTaskForModule(libertyModule, baseTask);
                String qualifiedContainerTask = LibertyGradleUtil.getGradleTaskForModule(libertyModule, "libertyDevc");
                if (runInContainer) {
                    startCmd = buildSettingsCmd + " " + qualifiedContainerTask;
                } else if (libertyModule.isCustom()) {
                    String containerTask = libertyModule.runInContainer() ? qualifiedContainerTask : qualifiedTask;
                    startCmd = buildSettingsCmd + " " + containerTask + libertyModule.getCustomStartParams();
                } else {
                    startCmd = buildSettingsCmd + " " + qualifiedTask;
                }
                // Re-attach debug param if needed
                if (libertyModule.isDebugMode() && debugPort != -1) {
                    startCmd += " " + LIBERTY_GRADLE_DEBUG_PARAM + debugPort;
                }
            }
        }

        // Mark the module as STARTING before launching the command so the tree
        // icon updates immediately. A background watcher will promote the state to
        // RUNNING once Liberty logs CWWKF0011I.
        libertyModule.setAppState(LibertyModule.AppState.STARTING);
        LibertyModules.getInstance().cacheState(libertyModule.getName(), LibertyModule.AppState.STARTING);

        String cdToProjectCmd = "cd \"" + executionDir + "\"";
        LibertyActionUtil.executeCommand(widget, cdToProjectCmd, startCmd);

        // Register the terminal watcher AFTER the command is sent so the widget's
        // TtyConnector is in the right state. The watcher runs on a pooled thread.
        LibertyTerminalWatcher.watchForRunning(widget, libertyModule);

        if (libertyModule.isDebugMode() && debugPort != -1) {
            // Create remote configuration to attach debugger
            debugHandler.createAndRunDebugConfiguration(libertyModule, debugPort);
            libertyModule.setDebugMode(false);
        }
    }
}