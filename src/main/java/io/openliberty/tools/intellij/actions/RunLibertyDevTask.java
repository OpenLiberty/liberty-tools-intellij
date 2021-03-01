package io.openliberty.tools.intellij.actions;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.treeStructure.Tree;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;


public class RunLibertyDevTask extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        // default behaviour for this action is disabled
        e.getPresentation().setEnabled(false);
        final Project project = LibertyProjectUtil.getProject(e.getDataContext());
        ToolWindow libertyDevToolWindow = ToolWindowManager.getInstance(project).getToolWindow(Constants.LIBERTY_DEV_DASHBOARD_ID);
        if (libertyDevToolWindow != null) {
            Content content = libertyDevToolWindow.getContentManager().findContent("Projects");
            JComponent libertyWindow = content.getComponent();
            Component[] components = libertyWindow.getComponents();
            for (Component comp : components) {
                if (comp.getName() != null && comp.getName().equals(Constants.LIBERTY_TREE)) {
                    Tree libertyTree = (Tree) comp;

                    TreePath[] selectionPaths = libertyTree.getSelectionPaths();

                    // when only one child node is selected, enable this action
                    if (selectionPaths != null && selectionPaths.length == 1) {
                        String lastPathComponent = selectionPaths[0].getLastPathComponent().toString();
                        if (Constants.getFullActionMap().containsKey(lastPathComponent)) {
                            e.getPresentation().setEnabled(true);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Logger log = Logger.getInstance(RunLibertyDevTask.class);

        final Project project = LibertyProjectUtil.getProject(e.getDataContext());
        if (project == null) return;

        ToolWindow libertyDevToolWindow = ToolWindowManager.getInstance(project).getToolWindow(Constants.LIBERTY_DEV_DASHBOARD_ID);

        Content content = libertyDevToolWindow.getContentManager().findContent("Projects");

        JComponent libertyWindow = content.getComponent();

        Component[] components = libertyWindow.getComponents();

        for (Component comp : components) {
            if (comp.getName() != null && comp.getName().equals(Constants.LIBERTY_TREE)) {
                Tree libertyTree = (Tree) comp;

                TreePath[] selectionPaths = libertyTree.getSelectionPaths();
                if (selectionPaths != null && selectionPaths.length == 1) {
                    String lastPathComponent = selectionPaths[0].getLastPathComponent().toString();
                    if (Constants.getFullActionMap().containsKey(lastPathComponent)) {
                        String projectName = (String) e.getDataContext().getData(Constants.LIBERTY_PROJECT_NAME);
                        if (projectName == null) {
                            projectName = project.getName();
                        }
                        // calls selected action
                        AnAction action = ActionManager.getInstance().getAction(Constants.getFullActionMap().get(lastPathComponent));
                        action.actionPerformed(new AnActionEvent(null,
                                DataManager.getInstance().getDataContext(libertyTree),
                                ActionPlaces.UNKNOWN, new Presentation(),
                                ActionManager.getInstance(), 0));
                    }
                }
            } else {
                log.debug("Tree view not built, no valid projects to Run Liberty Dev actions on");
            }
        }
    }

}
