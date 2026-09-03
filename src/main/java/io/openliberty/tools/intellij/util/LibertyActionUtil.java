/*******************************************************************************
 * Copyright (c) 2020, 2026 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.terminal.frontend.view.TerminalView;
import io.openliberty.tools.intellij.LibertyModule;

// TerminalView and related Reworked Terminal APIs are marked @Experimental by JetBrains, but their
// use is explicitly recommended over the Classic Terminal APIs (see https://youtrack.jetbrains.com/issue/IJPL-252504).
@SuppressWarnings("UnstableApiUsage")
public class LibertyActionUtil {

    static Logger LOGGER = Logger.getInstance(LibertyActionUtil.class);

    /**
     * Send two commands sequentially to the terminal associated with the given module.
     * {@code cmd1} is sent first; {@code cmd2} is sent once the terminal session is active.
     *
     * <p>Uses {@link TerminalView#sendText(String)} when a {@link TerminalView} is
     * available (IntelliJ 2025.3+). Falls back to
     * {@link com.intellij.terminal.ui.TerminalWidget#sendCommandToExecute(String)} otherwise.
     *
     * @param libertyModule the module whose terminal should receive the commands
     * @param cmd1          first command (e.g. {@code cd <project-dir>})
     * @param cmd2          second command (e.g. the Liberty dev mode start command)
     */
    public static void executeCommand(LibertyModule libertyModule, String cmd1, String cmd2) {
        // Run on a pooled thread so the EDT is never blocked.
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            executeCommand(libertyModule, cmd1);
            // Wait until the terminal session has an active TTY before sending the second command.
            // This is required because IntelliJ batches commands and runs them out of order.
            int i = 0;
            try {
                while (libertyModule.getTerminalWidget() != null
                        && libertyModule.getTerminalWidget().getTtyConnector() == null) {
                    if (i > 100) {
                        LOGGER.error("Timed out waiting to execute command: " + cmd1);
                        return;
                    }
                    LOGGER.debug("Waiting for cd to execute: " + i++);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                LOGGER.error(String.format("Interrupted waiting to execute command: %s", cmd1), e);
                Thread.currentThread().interrupt();
                return;
            }
            executeCommand(libertyModule, cmd2);
        });
    }

    /**
     * Send a single command to the terminal associated with the given module.
     *
     * <p>Uses {@link TerminalView#sendText(String)} when a {@link TerminalView} is
     * available (IntelliJ 2025.3+). Falls back to
     * {@link com.intellij.terminal.ui.TerminalWidget#sendCommandToExecute(String)} otherwise.
     *
     * @param libertyModule the module whose terminal should receive the command
     * @param cmd           the command to send (a newline is appended to trigger execution)
     */
    public static void executeCommand(LibertyModule libertyModule, String cmd) {
        TerminalView view = libertyModule.getTerminalView();
        if (view != null) {
            // Reworked Terminal path
            // Append newline so the command is executed immediately.
            view.sendText(cmd + "\n");
            return;
        }
        // Classic fallback.
        if (libertyModule.getTerminalWidget() != null) {
            libertyModule.getTerminalWidget().requestFocus();
            libertyModule.getTerminalWidget().sendCommandToExecute(cmd);
        }
    }
}
