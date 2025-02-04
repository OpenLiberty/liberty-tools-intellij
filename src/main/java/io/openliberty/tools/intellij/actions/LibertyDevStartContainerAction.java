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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.LibertyModule;
import io.openliberty.tools.intellij.util.*;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

public class LibertyDevStartContainerAction extends LibertyDevStartAction {

    /**
     * Returns the name of the action command being processed.
     *
     * @return The name of the action command being processed.
     */
    @Override
    protected String getActionCommandName() {
        return LocalizedResourceUtil.getMessage("start.liberty.dev.container");
    }

    @Override
    protected void executeLibertyAction(LibertyModule libertyModule) {
        runInTerminal(libertyModule, true);
    }
}