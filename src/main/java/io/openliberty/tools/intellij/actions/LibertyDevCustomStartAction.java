package io.openliberty.tools.intellij.actions;

import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import io.openliberty.tools.intellij.LibertyPluginIcons;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LibertyActionUtil;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

public class LibertyDevCustomStartAction extends LibertyGeneralAction {

    public LibertyDevCustomStartAction() {
        setActionCmd("start Liberty dev mode with custom parameters");
    }

    @Override
    protected void executeLibertyAction() {
        String msg;
        String initialVal;
        if (projectType.equals(Constants.LIBERTY_MAVEN_PROJECT)) {
            msg = "Specify custom parameters for the Liberty dev command (e.g. -DhotTests=true)";
            initialVal = "-DhotTests=true";
        } else {
            msg = "Specify custom parameters for the Liberty dev command (e.g. --hotTests)";
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

        String customParams = Messages.showInputDialog(project, msg, "Liberty dev mode custom parameters",
                LibertyPluginIcons.libertyIcon_40, initialVal, validator);

        String startCmd = null;
        if (customParams == null) {
            return;
        }
        if (projectType.equals(Constants.LIBERTY_MAVEN_PROJECT)) {
            startCmd = "mvn io.openliberty.tools:liberty-maven-plugin:dev " + customParams + " -f \"" + buildFile.getCanonicalPath() + "\"";
        } else if (projectType.equals(Constants.LIBERTY_GRADLE_PROJECT)) {
            startCmd = "gradle libertyDev " + customParams + " -b=" + buildFile.getCanonicalPath();
        }

        ShellTerminalWidget widget = LibertyProjectUtil.getTerminalWidget(project, projectName, true);
        if (widget == null) {
            log.debug("Unable to start Liberty dev mode with custom parameters, could not get or create terminal widget for " + projectName);
            return;
        }
        LibertyActionUtil.executeCommand(widget, startCmd);
    }
}
