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

import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import io.openliberty.tools.intellij.LibertyPluginIcons;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LibertyActionUtil;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

public class LibertyDevCustomStartAction extends LibertyGeneralAction {

    public LibertyDevCustomStartAction() {
        setActionCmd(LocalizedResourceUtil.getMessage("start.liberty.dev.custom.params"));
    }

    @Override
    protected void executeLibertyAction() {
        String msg;
        String initialVal;
        if (projectType.equals(Constants.LIBERTY_MAVEN_PROJECT)) {
            msg = LocalizedResourceUtil.getMessage("start.liberty.dev.custom.params.message.maven");
            initialVal = "";
        } else {
            msg = LocalizedResourceUtil.getMessage("start.liberty.dev.custom.params.message.gradle");
            initialVal = "";
        }

        InputValidator validator = new InputValidator() {
            @Override
            public boolean checkInput(String inputString) {
                if (inputString != null && !inputString.startsWith("-")) {
                    return false;
                }
                return true;
            }

            @Override
            public boolean canClose(String inputString) {
                return true;
            }
        };

        String customParams = Messages.showInputDialog(project, msg,
                LocalizedResourceUtil.getMessage("liberty.dev.custom.params"),
                LibertyPluginIcons.libertyIcon_40, initialVal, validator);

        String startCmd = null;
        if (customParams == null) {
            return;
        }
        if (projectType.equals(Constants.LIBERTY_MAVEN_PROJECT)) {
            startCmd = "mvn io.openliberty.tools:liberty-maven-plugin:dev " + customParams;
        } else if (projectType.equals(Constants.LIBERTY_GRADLE_PROJECT)) {
            startCmd = "gradle libertyDev " + customParams;
        }

        ShellTerminalWidget widget = LibertyProjectUtil.getTerminalWidget(project, projectName, true);
        if (widget == null) {
            LOGGER.debug("Unable to start Liberty dev mode with custom parameters, could not get or create terminal widget for " + projectName);
            return;
        }
        String cdToProjectCmd = "cd " + buildFile.getParent().getCanonicalPath();
        LibertyActionUtil.executeCommand(widget, cdToProjectCmd);
        LibertyActionUtil.executeCommand(widget, startCmd);
    }
}
