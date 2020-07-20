package org.liberty.intellij.util;

import com.intellij.openapi.diagnostic.Logger;
import com.jediterm.terminal.TtyConnector;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LibertyActionUtil {

    /**
     * Send the given command to the given ShellTerminalWidget
     * @param widget
     * @param cmd
     */
    public static void executeCommand(ShellTerminalWidget widget, String cmd) {
        Logger log = Logger.getInstance(LibertyActionUtil.class);;
        widget.grabFocus();
        TtyConnector connector = widget.getTtyConnector();
        String enterCode = new String(widget.getTerminalStarter().getCode(KeyEvent.VK_ENTER, 0), StandardCharsets.UTF_8);
        StringBuilder result = new StringBuilder();
        result.append(cmd).append(enterCode);
        try {
            connector.write(result.toString());
        } catch (IOException ex) {
            log.error(ex.getMessage());
        }

    }
}
