package io.openliberty.tools.intellij.actions;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.LibertyPluginIcons;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;

import static io.openliberty.tools.intellij.util.Constants.LibertyRB;

public class LibertyGeneralAction extends AnAction {

    protected Logger log = Logger.getInstance(LibertyGeneralAction.class);
    protected Project project;
    protected String projectName;
    protected String projectType;
    protected VirtualFile buildFile;
    protected String actionCmd;

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        project = LibertyProjectUtil.getProject(e.getDataContext());
        if (project == null) {
            String msg = MessageFormat.format(LibertyRB.getString("liberty.project.does.not.resolve"), actionCmd);
            notifyError(msg);
            log.debug(msg);
            return;
        }

        buildFile = (VirtualFile) e.getDataContext().getData(Constants.LIBERTY_BUILD_FILE);
        if (buildFile == null) {
            String msg = MessageFormat.format(LibertyRB.getString("liberty.build.file.does.not.resolve"), actionCmd,project.getName());
            notifyError(msg);
            log.debug(msg);
            return;
        }

        projectName = (String) e.getDataContext().getData(Constants.LIBERTY_PROJECT_NAME);
        projectType = (String) e.getDataContext().getData(Constants.LIBERTY_PROJECT_TYPE);
        executeLibertyAction();
    }

    protected void setActionCmd(String actionCmd) {
        this.actionCmd = actionCmd;
    }

    protected void executeLibertyAction() {
        // must be implemented by individual actions
    }

    protected void notifyError(String errMsg) {
        Notification notif = new Notification(Constants.LIBERTY_DEV_DASHBOARD_ID
                , LibertyPluginIcons.libertyIcon
                , LibertyRB.getString("liberty.action.cannot.start")
                , ""
                , errMsg
                , NotificationType.WARNING
                , NotificationListener.URL_OPENING_LISTENER);
        Notifications.Bus.notify(notif, project);
    }

}
