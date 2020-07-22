package io.openliberty.tools.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import org.jetbrains.annotations.NotNull;

public class ExecuteLibertyDevTask extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = LibertyProjectUtil.getProject(e.getDataContext());
        if (project == null) return;

//        RunAnythingManager runAnythingManager = RunAnythingManager.getInstance(project);
//        runAnythingManager.show("libertyDev" + " ", false, e);
    }
}
