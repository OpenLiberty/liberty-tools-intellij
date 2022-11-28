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

import com.intellij.openapi.diagnostic.Logger;
import com.jediterm.terminal.TtyConnector;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LibertyActionUtil {

    /**
     * Send the given command to the given ShellTerminalWidget
     *
     * @param widget
     * @param cmd
     */
    public static void executeCommand(ShellTerminalWidget widget, String cmd) {
        Logger LOGGER = Logger.getInstance(LibertyActionUtil.class);
        try {
            widget.grabFocus();
            TtyConnector connector = widget.getTtyConnector();
            if (connector == null) {
                // new terminal, use build in execute command function
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
