package org.liberty.intellij.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;
import org.liberty.intellij.LibertyExplorer;
import org.liberty.intellij.util.Constants;
import org.liberty.intellij.util.LibertyProjectUtil;

import javax.swing.*;
import java.awt.*;

public class RefreshLibertyToolbar extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = LibertyProjectUtil.getProject(e.getDataContext());
        if (project == null) return;

        ToolWindow libertyDevToolWindow = ToolWindowManager.getInstance(project).getToolWindow(Constants.LIBERTY_DEV_DASHBOARD_ID);

        Content content = libertyDevToolWindow.getContentManager().findContent("Projects");

        JComponent libertyWindow = content.getComponent();

        libertyWindow.list();
        Component[] components = libertyWindow.getComponents();

        for (Component comp: components) {
            if (comp.getName() != null && comp.getName().equals(Constants.LIBERTY_TREE)) {
                Tree tree = LibertyExplorer.buildTree(project, content.getComponent().getBackground());
                content.getComponent().remove(comp);
                content.getComponent().add(tree);
            }
        }

    }
}
