package io.openliberty.tools.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
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
            log.debug("Unable to " + actionCmd + ", could not resolve project");
            return;
        }

        buildFile = (VirtualFile) e.getDataContext().getData(Constants.LIBERTY_BUILD_FILE);
        if (buildFile == null) {
            log.debug("Unable to " + actionCmd + ", could not resolve configuration file for  " + project.getName());
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


}
