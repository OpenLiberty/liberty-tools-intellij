package io.openliberty.tools.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LibertyActionUtil;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;

public class LibertyDevCustomStartAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = LibertyProjectUtil.getProject(e.getDataContext());
        if (project == null) return;

        final VirtualFile file = (VirtualFile) e.getDataContext().getData(Constants.LIBERTY_BUILD_FILE);
        if (file == null) return;

        final String projectName = (String) e.getDataContext().getData(Constants.LIBERTY_PROJECT_NAME);
        final String projectType = (String) e.getDataContext().getData(Constants.LIBERTY_PROJECT_TYPE);

        String msg;
        if (projectType.equals(Constants.LIBERTY_MAVEN_PROJECT)) {
            msg = "Specify custom parameters for the liberty dev command (e.g. -DhotTests=true)";
        } else {
            msg = "Specify custom parameters for the liberty dev command (e.g. --hotTests)";
        }
        String customParams = Messages.showInputDialog(msg, "Liberty Dev Custom Parameters",  Messages.getQuestionIcon());


        String startCmd = null;
        if (customParams == null) { return; }
        if (projectType.equals(Constants.LIBERTY_MAVEN_PROJECT)) {
            startCmd = "mvn io.openliberty.tools:liberty-maven-plugin:dev " + customParams + " -f \"" + file.getCanonicalPath() + "\"";
        } else if (projectType.equals(Constants.LIBERTY_GRADLE_PROJECT)) {
            startCmd = "gradle libertyDev " + customParams + " -b=" + file.getCanonicalPath();
        }

        ShellTerminalWidget widget = LibertyProjectUtil.getTerminalWidget(project, projectName, true);
        if (widget == null) return;
        LibertyActionUtil.executeCommand(widget, startCmd);
    }
}
