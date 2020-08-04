package io.openliberty.tools.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LibertyActionUtil;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

public class LibertyDevCustomStartAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Logger log = Logger.getInstance(LibertyDevCustomStartAction.class);;

        final Project project = LibertyProjectUtil.getProject(e.getDataContext());
        if (project == null) {
            log.debug("Unable to custom start dev mode, could not resolve project");
            return;
        }

        final VirtualFile file = (VirtualFile) e.getDataContext().getData(Constants.LIBERTY_BUILD_FILE);
        if (file == null) {
            log.debug("Unable to custom start dev mode, could not resolve configuration file for  " + project.getName());
            return;
        }

        final String projectName = (String) e.getDataContext().getData(Constants.LIBERTY_PROJECT_NAME);
        final String projectType = (String) e.getDataContext().getData(Constants.LIBERTY_PROJECT_TYPE);

        String msg;
        String initialVal;
        if (projectType.equals(Constants.LIBERTY_MAVEN_PROJECT)) {
            msg = "Specify custom parameters for the liberty dev command (e.g. -DhotTests=true)";
            initialVal = "-DhotTests=true";
        } else {
            msg = "Specify custom parameters for the liberty dev command (e.g. --hotTests)";
            initialVal = "--hotTests";
        }

        InputValidator validator = new InputValidator() {
            @Override
            public boolean checkInput(String inputString) {
                if (inputString != null && !inputString.startsWith("-")) {
                    return false;
                }
                return true;
            }

            @Override
            public boolean canClose(String inputString) {
                return true;
            }
        };

        String customParams = Messages.showInputDialog(project, msg, "Liberty Dev Custom Parameters",
                Constants.libertyIcon_40, initialVal, validator);

        String startCmd = null;
        if (customParams == null) { return; }
        if (projectType.equals(Constants.LIBERTY_MAVEN_PROJECT)) {
            startCmd = "mvn io.openliberty.tools:liberty-maven-plugin:dev " + customParams + " -f \"" + file.getCanonicalPath() + "\"";
        } else if (projectType.equals(Constants.LIBERTY_GRADLE_PROJECT)) {
            startCmd = "gradle libertyDev " + customParams + " -b=" + file.getCanonicalPath();
        }

        ShellTerminalWidget widget = LibertyProjectUtil.getTerminalWidget(project, projectName, true);
        if (widget == null) {
            log.debug("Unable to custom start dev mode, could not get or create terminal widget for " + projectName);
            return;
        }
        LibertyActionUtil.executeCommand(widget, startCmd);
    }
}
