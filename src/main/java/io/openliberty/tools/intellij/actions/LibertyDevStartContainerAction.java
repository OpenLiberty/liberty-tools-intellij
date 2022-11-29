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

import io.openliberty.tools.intellij.util.*;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

public class LibertyDevStartContainerAction extends LibertyGeneralAction {

    public LibertyDevStartContainerAction() {
        setActionCmd(LocalizedResourceUtil.getMessage("start.liberty.dev.container"));
    }

    @Override
    protected void executeLibertyAction() {
        String startCmd = projectType.equals(Constants.LIBERTY_MAVEN_PROJECT) ? LibertyMavenUtil.getMavenSettingsCmd(project) + Constants.LIBERTY_MAVEN_START_CONTAINER_CMD : LibertyGradleUtil.getGradleSettingsCmd(project) + Constants.LIBERTY_GRADLE_START_CONTAINER_CMD;
        ShellTerminalWidget widget = getTerminalWidget(true);
        if (widget == null) {
            return;
        }
        String cdToProjectCmd = "cd \"" + buildFile.getParent().getCanonicalPath() + "\"";
        LibertyActionUtil.executeCommand(widget, cdToProjectCmd);
        LibertyActionUtil.executeCommand(widget, startCmd);
    }
}