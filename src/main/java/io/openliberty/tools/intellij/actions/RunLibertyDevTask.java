package io.openliberty.tools.intellij.actions;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.ConfirmationDialog;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import io.openliberty.tools.intellij.util.TreeDataProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

public class RunLibertyDevTask extends AnAction {

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
                    // if one node is selected confirm that you would like to run that task
                    String lastPathComponent = selectionPaths[0].getLastPathComponent().toString();
                    if (Constants.getFullActionMap().containsKey(lastPathComponent)) {
                        // verify user would like to run this action
                        final String projectName = (String) e.getDataContext().getData(Constants.LIBERTY_PROJECT_NAME);
                        boolean confirm = ConfirmationDialog.requestForConfirmation(
                                VcsShowConfirmationOption.STATIC_SHOW_CONFIRMATION, project
                                , "Run Liberty Dev " + lastPathComponent + " on " + projectName + "?"
                                , "Confirm Liberty Dev " + lastPathComponent
                                , Constants.libertyIcon_40);
                        if (confirm) {
                            // calls action
                            AnAction action = ActionManager.getInstance().getAction(Constants.getFullActionMap().get(lastPathComponent));
                            action.actionPerformed(new AnActionEvent(null,
                                    DataManager.getInstance().getDataContext(libertyTree),
                                    ActionPlaces.UNKNOWN, new Presentation(),
                                    ActionManager.getInstance(), 0));
                        }
                    } else {
                        // if node selected is not an action show dialog to choose action to run
                        chooseActionToRun(libertyTree);
                    }
                } else {
                    chooseActionToRun(libertyTree);
                }
            } else {
                log.debug("Tree view not built, no valid projects to Run Liberty Dev actions on");
            }
        }
    }

    private void chooseActionToRun(Tree libertyTree) {
        // if 0 or multiple nodes are selected display all possible nodes and allow users to select talk
        HashMap<String, ArrayList<Object>> map = (HashMap<String, ArrayList<Object>>) DataManager.getInstance()
                .getDataContext(libertyTree).getData(Constants.LIBERTY_PROJECT_MAP);
        String[] projectNamesArr = map.keySet().toArray(new String[map.keySet().size()]);

        int projectSelected = Messages.showChooseDialog(
                "Choose a project to run a Liberty Dev task on"
                , "Choose a project"
                , projectNamesArr, projectNamesArr[0]
                , Constants.libertyIcon_40);
        if (projectSelected == -1) {
            return;
        }

        String project = projectNamesArr[projectSelected];

        TreeDataProvider treeDataProvider = (TreeDataProvider) DataManager.getDataProvider(libertyTree);
        ArrayList<Object> settings = map.get(project);
        VirtualFile file = (VirtualFile) settings.get(0);
        String projectType = (String) settings.get(1);

        HashMap<String, String> actionsMap = new HashMap<String, String>();
        if (projectType.equals(Constants.LIBERTY_GRADLE_PROJECT)) {
            actionsMap = Constants.getGradleMap();
        } else if (projectType.equals(Constants.LIBERTY_MAVEN_PROJECT)) {
            actionsMap = Constants.getMavenMap();
        }
        String[] keyArray = actionsMap.keySet().toArray(new String[actionsMap.keySet().size()]);

        int taskSelected = Messages.showChooseDialog("Choose a Liberty Dev task to run on " + project
                , "Run Liberty Dev Task"
                , keyArray, keyArray[0]
                , Constants.libertyIcon_40);
        if (taskSelected == -1) {
            return; // -1 indicates users cancelled dialog
        }

        String task = keyArray[taskSelected];

        treeDataProvider.saveData(file, project, projectType);

        // run selected task on selected project
        AnAction action = ActionManager.getInstance().getAction(actionsMap.get(task));
        action.actionPerformed(new AnActionEvent(null,
                DataManager.getInstance().getDataContext(libertyTree),
                ActionPlaces.UNKNOWN, new Presentation(),
                ActionManager.getInstance(), 0));
    }
}
