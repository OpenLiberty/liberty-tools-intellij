/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation.
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
import com.jediterm.terminal.TtyConnector;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LibertyActionUtil {

    static Logger LOGGER = Logger.getInstance(LibertyActionUtil.class);
    /**
     * Send the given two commands to the given ShellTerminalWidget
     * but ensure the first command has at least started executing
     * before typing the second command on the terminal.
     *
     * @param widget
     * @param cmd1
     * @param cmd2
     */
    public static void executeCommand(ShellTerminalWidget widget, String cmd1, String cmd2) {
        // Perform these commands on the same pooled thread or else the event thread will be blocked
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            executeCommand(widget, cmd1);
            // Do not run the second command until the execution of the first command has begun.
            // This is required because IntelliJ batches commands and runs them out of order.
            int i = 0;
            try {
                while (widget.getProcessTtyConnector() == null) {
                    if (i > 100) {
                        LOGGER.warn("Time out waiting to execute command: " + cmd1);
                        return;
                    }
                    LOGGER.warn("Waiting for cd to execute: " + i++);
                    Thread.sleep(100);
                }
            } catch (InterruptedException x) {
                LOGGER.error(String.format("Interrupted waiting to execute command: %s", cmd1), x);
            }
            executeCommand(widget, cmd2);
        });
    }

    /**
     * Send the given command to the given ShellTerminalWidget
     *
     * @param widget
     * @param cmd
     */
    public static void executeCommand(ShellTerminalWidget widget, String cmd) {
        try {
            widget.grabFocus();
            TtyConnector connector = widget.getTtyConnector();
            if (connector == null) {
                // new terminal, use built in execute command function
                widget.executeCommand(cmd);
                return;
            }
            // existing terminal, add a new line character and send command through connector
            String enterCode = new String(widget.getTerminalStarter().getCode(KeyEvent.VK_ENTER, 0), StandardCharsets.UTF_8);
            StringBuilder result = new StringBuilder();
            result.append(cmd).append(enterCode);
            connector.write(result.toString());
        } catch (IOException e) {
            LOGGER.error(String.format("Failed to execute command: %s", cmd), e);
        }

    }
}
