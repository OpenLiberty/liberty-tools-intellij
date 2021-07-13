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
            String msg = "Unable to " + actionCmd + ": Could not resolve project. Ensure you run the Liberty action from the Liberty tool window";
            notifyError(msg);
            log.debug(msg);
            return;
        }

        buildFile = (VirtualFile) e.getDataContext().getData(Constants.LIBERTY_BUILD_FILE);
        if (buildFile == null) {
            String msg = "Unable to " + actionCmd + ": Could not resolve configuration file for  " + project.getName() + ". Ensure you run the Liberty action from the Liberty tool window";
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
        Notification notif = new Notification("Liberty"
                , LibertyPluginIcons.libertyIcon
                , "Liberty action was not able to start"
                , ""
                , errMsg
                , NotificationType.WARNING
                , NotificationListener.URL_OPENING_LISTENER);
        Notifications.Bus.notify(notif, project);
    }

}
