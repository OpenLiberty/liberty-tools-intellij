package io.openliberty.tools.intellij.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import io.openliberty.tools.intellij.LibertyPluginIcons;
import io.openliberty.tools.intellij.util.LibertyActionUtil;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

public class LibertyDevStopAction extends LibertyGeneralAction {

    @Override
    protected void executeLibertyAction() {
        setActionCmd("stop Liberty dev mode");
        ShellTerminalWidget widget = LibertyProjectUtil.getTerminalWidget(project, projectName, false);
        String stopCmd = "q";
        if (widget == null) {
            Notification notif = new Notification("Liberty"
                    , LibertyPluginIcons.libertyIcon
                    , "Liberty dev mode has not been started"
                    , ""
                    , "Liberty dev mode has not been started on " + projectName
                    + ". \nStart Liberty dev mode from the Liberty tool window."
                    , NotificationType.WARNING
                    , NotificationListener.URL_OPENING_LISTENER);
            Notifications.Bus.notify(notif, project);
            log.error("Cannot stop Liberty dev mode, corresponding project terminal does not exist.");
            return;
        }
        LibertyActionUtil.executeCommand(widget, stopCmd);
    }
}
