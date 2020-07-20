package org.liberty.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.liberty.intellij.util.Constants;
import org.liberty.intellij.util.LibertyActionUtil;
import org.liberty.intellij.util.LibertyProjectUtil;

import java.io.IOException;

public class LibertyDevStopAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = LibertyProjectUtil.getProject(e.getDataContext());
        if (project == null) return;

        final String projectName = (String) e.getDataContext().getData(Constants.LIBERTY_PROJECT_NAME);

        String stopCmd = "exit";

        ShellTerminalWidget widget = LibertyProjectUtil.getTerminalWidget(project, projectName, false);

        if (widget == null) return;
        LibertyActionUtil.executeCommand(widget, stopCmd);
    }
}
