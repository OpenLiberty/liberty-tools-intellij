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

import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LibertyActionUtil;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

public class LibertyDevStartAction extends LibertyGeneralAction {

    public LibertyDevStartAction() {
        setActionCmd(LocalizedResourceUtil.getMessage("start.liberty.dev"));
    }
    
    @Override
    protected void executeLibertyAction() {
        ShellTerminalWidget widget = LibertyProjectUtil.getTerminalWidget(project, projectName, true);
        String startCmd = null;

        if (projectType.equals(Constants.LIBERTY_MAVEN_PROJECT)) {
            startCmd = "mvn io.openliberty.tools:liberty-maven-plugin:dev -f \"" + buildFile.getCanonicalPath() + "\"";
        } else if (projectType.equals(Constants.LIBERTY_GRADLE_PROJECT)) {
            startCmd = "gradle libertyDev -b=" + buildFile.getCanonicalPath();
        }
        if (widget == null) {
            log.debug("Unable to start Liberty dev mode, could not get or create terminal widget for " + projectName);
            return;
        }
        LibertyActionUtil.executeCommand(widget, startCmd);
    }
}