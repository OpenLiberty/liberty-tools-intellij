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

import io.openliberty.tools.intellij.util.LibertyActionUtil;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

public class LibertyDevStopAction extends LibertyGeneralAction {

    public LibertyDevStopAction() {
        setActionCmd(LocalizedResourceUtil.getMessage("stop.liberty.dev"));
    }

    @Override
    protected void executeLibertyAction() {
        ShellTerminalWidget widget = getTerminalWidget(false);
        String stopCmd = "q";
        if (widget == null) {
            return;
        }
        LibertyActionUtil.executeCommand(widget, stopCmd);
    }
}
