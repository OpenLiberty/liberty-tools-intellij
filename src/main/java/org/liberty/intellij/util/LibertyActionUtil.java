package org.liberty.intellij.util;

import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.ui.TerminalSession;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LibertyActionUtil {

    public static void executeCommand(ShellTerminalWidget widget, String cmd) {

        widget.grabFocus();
        TerminalSession session = widget.getCurrentSession();
        TtyConnector connector = widget.getTtyConnector();
        String enterCode = new String(widget.getTerminalStarter().getCode(KeyEvent.VK_ENTER, 0), StandardCharsets.UTF_8);
        StringBuilder result = new StringBuilder();
        result.append(cmd).append(enterCode);
        try {
            connector.write(result.toString());
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }
}
