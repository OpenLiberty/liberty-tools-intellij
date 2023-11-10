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

public class LibertyDevStartContainerAction extends LibertyGeneralAction {

    /**
     * Returns the name of the action command being processed.
     *
     * @return The name of the action command being processed.
     */
    protected String getActionCommandName() {
        return LocalizedResourceUtil.getMessage("start.liberty.dev.container");
    }

    @Override
    protected void executeLibertyAction(LibertyModule libertyModule) {
        Project project = libertyModule.getProject();
        VirtualFile buildFile = libertyModule.getBuildFile();
        ShellTerminalWidget widget = getTerminalWidget(true, project, buildFile, getActionCommandName());
        if (widget == null) {
            return;
        }

        String projectType = libertyModule.getProjectType();
        String startCmd;
        try {
            if(projectType.equals(Constants.LIBERTY_MAVEN_PROJECT)) {
                startCmd = LibertyMavenUtil.getMavenSettingsCmd(project, buildFile) + Constants.LIBERTY_MAVEN_START_CONTAINER_CMD;
            } else {
                startCmd = LibertyGradleUtil.getGradleSettingsCmd(project) + Constants.LIBERTY_GRADLE_START_CONTAINER_CMD;
            }
            startCmd += libertyModule.getCustomStartParams();
        } catch (LibertyException ex) {
            // in this case, the settings specified to mvn or gradle are invalid and an error was launched by getMavenSettingsCmd or getGradleSettingsCmd
            LOGGER.warn(ex.getMessage()); // Logger.error creates an entry on "IDE Internal Errors", which we do not want
            notifyError(ex.getTranslatedMessage(), project);
            return;
        }
        String cdToProjectCmd = "cd \"" + buildFile.getParent().getPath() + "\"";
        LibertyActionUtil.executeCommand(widget, cdToProjectCmd);
        LibertyActionUtil.executeCommand(widget, startCmd);
    }
}