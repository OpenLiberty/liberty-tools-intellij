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
import com.intellij.terminal.frontend.view.TerminalViewSessionState;
import io.openliberty.tools.intellij.LibertyModule;

public class LibertyActionUtil {

    static Logger LOGGER = Logger.getInstance(LibertyActionUtil.class);

    /**
     * Returns {@code true} if no Liberty dev mode command is currently running in the terminal
     * tab associated with the given module.
     *
     * <p>Uses {@link TerminalView#getSessionState()} when a {@link TerminalView} is available.
     *
     * @param libertyModule the module to check
     */
    // TerminalView, TerminalViewSessionState are @Experimental,
    // but their usage is explicitly recommended by https://plugins.jetbrains.com/docs/intellij/embedded-terminal.html
    @SuppressWarnings("UnstableApiUsage")
    public static boolean isCommandNotRunning(LibertyModule libertyModule) {
        TerminalView view = libertyModule.getTerminalView();
        if (view != null) {
            // Reworked Terminal path: Running means a process is active; Terminated / NotStarted means it is not.
            TerminalViewSessionState sessionState = view.getSessionState().getValue();
            return !(sessionState instanceof TerminalViewSessionState.Running);
        }
        return true;
    }

    /**
     * Send two commands sequentially to the terminal associated with the given module.
     * {@code cmd1} is sent first; {@code cmd2} is sent once the terminal session is active.
     *
     * <p>Uses {@link TerminalView#sendText(String)} to send commands to the terminal.
     *
     * @param libertyModule the module whose terminal should receive the commands
     * @param cmd1 first command (e.g. {@code cd <project-dir>})
     * @param cmd2 second command (e.g. the Liberty dev mode start command)
     */
    // TerminalView is @Experimental, but its usage is explicitly recommended by
    // https://plugins.jetbrains.com/docs/intellij/embedded-terminal.html
    @SuppressWarnings("UnstableApiUsage")
    public static void executeCommand(LibertyModule libertyModule, String cmd1, String cmd2) {
        // Run on a pooled thread so the EDT is never blocked.
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            executeCommand(libertyModule, cmd1);
            // Wait until the terminal session has started before sending the second command.
            // This is required because IntelliJ batches commands and runs them out of order.
            int i = 0;
            try {
                while (isCommandNotRunning(libertyModule)) {
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
     * <p>Uses {@link TerminalView#sendText(String)} to send the command to the terminal.
     *
     * @param libertyModule the module whose terminal should receive the command
     * @param cmd the command to send (a newline is appended to trigger execution)
     */
    // TerminalView is @Experimental, but its usage is explicitly recommended by
    // https://plugins.jetbrains.com/docs/intellij/embedded-terminal.html
    @SuppressWarnings("UnstableApiUsage")
    public static void executeCommand(LibertyModule libertyModule, String cmd) {
        TerminalView view = libertyModule.getTerminalView();
        if (view != null) {
            // Append newline so the command is executed immediately.
            view.sendText(cmd + "\n");
        }
    }
}
