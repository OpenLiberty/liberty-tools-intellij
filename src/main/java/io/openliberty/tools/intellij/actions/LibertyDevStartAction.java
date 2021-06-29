package io.openliberty.tools.intellij.actions;

import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LibertyActionUtil;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

public class LibertyDevStartAction extends LibertyGeneralAction {

    @Override
    protected void executeLibertyAction() {
        setActionCmd("start Liberty dev mode");
        ShellTerminalWidget widget = LibertyProjectUtil.getTerminalWidget(project, projectName, true);
        String startCmd = null;
        if (projectType.equals(Constants.LIBERTY_MAVEN_PROJECT)) {
            startCmd = "mvn io.openliberty.tools:liberty-maven-plugin:dev -f \"" + buildFile.getCanonicalPath() + "\"";
        } else if (projectType.equals(Constants.LIBERTY_GRADLE_PROJECT)) {
            startCmd = "gradle libertyDev -b=" + buildFile.getCanonicalPath();
        }
        if (widget == null) {
            log.debug("Unable to start Liberty dev mode, could not get or create terminal widget for " + projectName);
            return;
        }
        LibertyActionUtil.executeCommand(widget, startCmd);
    }
}