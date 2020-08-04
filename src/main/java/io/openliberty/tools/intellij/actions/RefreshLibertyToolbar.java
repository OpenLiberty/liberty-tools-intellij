package io.openliberty.tools.intellij.actions;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.treeStructure.Tree;
import io.openliberty.tools.intellij.LibertyExplorer;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class RefreshLibertyToolbar extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Logger log = Logger.getInstance(RefreshLibertyToolbar.class);


        final Project project = LibertyProjectUtil.getProject(e.getDataContext());
        if (project == null) {
            log.debug("Unable to refresh Liberty toolbar, could not resolve project");
            return;
        }
        ProjectView.getInstance(project).refresh();

        ToolWindow libertyDevToolWindow = ToolWindowManager.getInstance(project).getToolWindow(Constants.LIBERTY_DEV_DASHBOARD_ID);

        Content content = libertyDevToolWindow.getContentManager().findContent("Projects");

        SimpleToolWindowPanel simpleToolWindowPanel = (SimpleToolWindowPanel) content.getComponent();

        JComponent libertyWindow = content.getComponent();

        Component[] components = libertyWindow.getComponents();

        Component existingTree = null;
        Component existingActionToolbar = null;
        for (Component comp: components) {
            if (comp.getName() != null && comp.getName().equals(Constants.LIBERTY_TREE)) {
                existingTree = comp;
            }
            if (comp.getName() != null && comp.getName().equals(Constants.LIBERTY_ACTION_TOOLBAR)) {
                existingActionToolbar = comp;
            }
        }

        if (existingTree != null && existingActionToolbar != null) {
            Tree tree = LibertyExplorer.buildTree(project, content.getComponent().getBackground());
            ActionToolbar actionToolbar = LibertyExplorer.buildActionToolbar(tree);

            simpleToolWindowPanel.remove(existingActionToolbar);
            simpleToolWindowPanel.remove(existingTree);

            simpleToolWindowPanel.setToolbar(actionToolbar.getComponent());
            simpleToolWindowPanel.setContent(tree);
            simpleToolWindowPanel.revalidate();
            simpleToolWindowPanel.repaint();
        }
    }
}
