/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation.
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
import io.openliberty.tools.intellij.util.*;
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
        Project project = libertyModule.getProject();
        VirtualFile buildFile = libertyModule.getBuildFile();
        String projectType = libertyModule.getProjectType();
        String projectName = project.getName();

        String startCmd = null;
        int debugPort = -1;
        DebugModeHandler debugHandler = new DebugModeHandler();
        String buildSettingsCmd;
        try {
            if(projectType.equals(Constants.LIBERTY_MAVEN_PROJECT)) {
                buildSettingsCmd = LibertyMavenUtil.getMavenSettingsCmd(project, buildFile);
            } else {
                buildSettingsCmd = LibertyGradleUtil.getGradleSettingsCmd(project);
            }
        } catch (LibertyException ex) {
            // in this case, the settings specified to mvn or gradle are invalid and an error was launched by getMavenSettingsCmd or getGradleSettingsCmd.
            // Log a warning because a Logger.error creates an entry on "IDE Internal Errors", which we do not want.
            LOGGER.warn(ex.getMessage());
            notifyError(ex.getTranslatedMessage(), project);
            return;
        }

        String start = projectType.equals(Constants.LIBERTY_MAVEN_PROJECT) ? buildSettingsCmd + Constants.LIBERTY_MAVEN_START_CMD : buildSettingsCmd + Constants.LIBERTY_GRADLE_START_CMD;
        String startInContainer = projectType.equals(Constants.LIBERTY_MAVEN_PROJECT) ? buildSettingsCmd + Constants.LIBERTY_MAVEN_START_CONTAINER_CMD : buildSettingsCmd + Constants.LIBERTY_GRADLE_START_CONTAINER_CMD;
        startCmd = libertyModule.runInContainer() ? startInContainer : start;
        startCmd += libertyModule.getCustomStartParams();
        if (libertyModule.isDebugMode()) {
            try {
                String debugParam = projectType.equals(Constants.LIBERTY_MAVEN_PROJECT) ? Constants.LIBERTY_MAVEN_DEBUG_PARAM : Constants.LIBERTY_GRADLE_DEBUG_PARAM;
                debugPort = debugHandler.getDebugPort(libertyModule);
                String debugStr = debugParam + debugPort;
                // do not append if debug port is already specified as part of start command
                if (!startCmd.contains(debugStr)) {
                    startCmd += " " + debugParam + debugPort;
                }
            } catch (IOException e) {
                String msg = LocalizedResourceUtil.getMessage("liberty.debug.port.unresolved", getActionCommandName(), projectName);
                notifyError(msg, project);
                LOGGER.error(msg);
            }
        }

        ShellTerminalWidget widget = getTerminalWidget(true, project, buildFile, getActionCommandName());
        if (widget == null) {
            return;
        }

        String cdToProjectCmd = "cd \"" + buildFile.getParent().getPath() + "\"";
        LibertyActionUtil.executeCommand(widget, cdToProjectCmd);
        LibertyActionUtil.executeCommand(widget, startCmd);
        if (libertyModule.isDebugMode() && debugPort != -1) {
            // Create remote configuration to attach debugger
            debugHandler.createAndRunDebugConfiguration(libertyModule, debugPort);
            libertyModule.setDebugMode(false);
        }
    }
}