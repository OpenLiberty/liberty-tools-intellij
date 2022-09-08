package io.openliberty.tools.intellij.actions;

import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import io.openliberty.tools.intellij.LibertyPluginIcons;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LibertyActionUtil;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

import static io.openliberty.tools.intellij.util.Constants.LibertyRB;

public class LibertyDevCustomStartAction extends LibertyGeneralAction {

    public LibertyDevCustomStartAction() {
        setActionCmd(LibertyRB.getString("start.liberty.dev.custom.params"));
    }

    @Override
    protected void executeLibertyAction() {
        String msg;
        String initialVal;
        if (projectType.equals(Constants.LIBERTY_MAVEN_PROJECT)) {
            msg = LibertyRB.getString("start.liberty.dev.custom.params.message.maven");
            initialVal = "-DhotTests=true";
        } else {
            msg = LibertyRB.getString("start.liberty.dev.custom.params.message.gradle");
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

        String customParams = Messages.showInputDialog(project, msg,
                LibertyRB.getString("liberty.dev.custom.params"),
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
