package io.openliberty.tools.intellij.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import io.openliberty.tools.intellij.LibertyPluginIcons;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LibertyActionUtil;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

import java.text.MessageFormat;

import static io.openliberty.tools.intellij.util.Constants.LibertyRB;

public class LibertyDevStopAction extends LibertyGeneralAction {

    public LibertyDevStopAction() {
        setActionCmd(LibertyRB.getString("stop.liberty.dev"));
    }

    @Override
    protected void executeLibertyAction() {
        ShellTerminalWidget widget = LibertyProjectUtil.getTerminalWidget(project, projectName, false);
        String stopCmd = "q";
        if (widget == null) {
            Notification notif = new Notification(Constants.LIBERTY_DEV_DASHBOARD_ID
                    , LibertyPluginIcons.libertyIcon
                    , LibertyRB.getString("liberty.dev.not.started.notification.title")
                    , ""
                    , MessageFormat.format(LibertyRB.getString("liberty.dev.not.started.notification.content"), projectName)
                    , NotificationType.WARNING
                    , NotificationListener.URL_OPENING_LISTENER);
            Notifications.Bus.notify(notif, project);
            log.error("Cannot stop Liberty dev mode, corresponding project terminal does not exist.");
            return;
        }
        LibertyActionUtil.executeCommand(widget, stopCmd);
    }
}
