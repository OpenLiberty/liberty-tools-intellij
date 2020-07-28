package io.openliberty.tools.intellij.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LibertyActionUtil;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;

public class LibertyDevStopAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Logger log = Logger.getInstance(LibertyDevStopAction.class);;

        final Project project = LibertyProjectUtil.getProject(e.getDataContext());
        if (project == null) {
            log.debug("Unable to stop liberty dev, could not resolve project");
            return;
        }

        final String projectName = (String) e.getDataContext().getData(Constants.LIBERTY_PROJECT_NAME);

        String stopCmd = "q";

        ShellTerminalWidget widget = LibertyProjectUtil.getTerminalWidget(project, projectName, false);

        if (widget == null) {
            Notification notif = new Notification("Liberty Dev Dashboard"
                    , Constants.libertyIcon
                    , "Liberty dev has not been started"
                    , ""
                    , "Liberty dev has not been started on " + projectName
                    + ". \nStart liberty dev from the Liberty Dev Dashboard."
                    , NotificationType.WARNING
                    , NotificationListener.URL_OPENING_LISTENER);
            Notifications.Bus.notify(notif, project);
            log.error("Cannot stop liberty dev, corresponding project terminal does not exist.");
            return;
        }
        LibertyActionUtil.executeCommand(widget, stopCmd);
    }
}
