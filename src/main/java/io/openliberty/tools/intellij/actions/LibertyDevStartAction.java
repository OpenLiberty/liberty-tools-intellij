package io.openliberty.tools.intellij.actions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LibertyActionUtil;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

public class LibertyDevStartAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Logger log = Logger.getInstance(LibertyDevStartAction.class);;

        final Project project = LibertyProjectUtil.getProject(e.getDataContext());
        if (project == null) {
            log.debug("Unable to start dev mode, could not resolve project");
            return;
        }

        final VirtualFile file = (VirtualFile) e.getDataContext().getData(Constants.LIBERTY_BUILD_FILE);
        if (file == null) {
            log.debug("Unable to start dev mode, could not resolve configuration file for  " + project.getName());
            return;
        }

        final String projectName = (String) e.getDataContext().getData(Constants.LIBERTY_PROJECT_NAME);
        final String projectType = (String) e.getDataContext().getData(Constants.LIBERTY_PROJECT_TYPE);

        String startCmd = null;
        if (projectType.equals(Constants.LIBERTY_MAVEN_PROJECT)) {
            startCmd = "mvn io.openliberty.tools:liberty-maven-plugin:dev -f \"" + file.getCanonicalPath() + "\"";
        } else if (projectType.equals(Constants.LIBERTY_GRADLE_PROJECT)) {
            startCmd = "gradle libertyDev -b=" + file.getCanonicalPath();
        }

        ShellTerminalWidget widget = LibertyProjectUtil.getTerminalWidget(project, projectName, true);
        if (widget == null) {
            log.debug("Unable to start dev mode, could not get or create terminal widget for " + projectName);
            return;
        } else {
            LibertyActionUtil.executeCommand(widget, startCmd);
        }
    }
}