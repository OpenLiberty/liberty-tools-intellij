/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.actions;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.treeStructure.Tree;
import io.openliberty.tools.intellij.LibertyPluginIcons;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;

public class RunLibertyDevTask extends AnAction {
    private static final Logger LOGGER = Logger.getInstance(RunLibertyDevTask.class);

    /**
     * Enables/disables the play/action button for the Liberty projects and action options in the Liberty Tool Window tree
     * @param e - AnActionEvent passed in by IntelliJ
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        if (e.getPlace().equalsIgnoreCase(Constants.GO_TO_ACTION_TRIGGERED)) {
            // always enable through shift+shift/Search Everywhere window
            e.getPresentation().setEnabled(true);
            return;
        } else {
            e.getPresentation().setEnabled(false);
        }
        final Project project = LibertyProjectUtil.getProject(e.getDataContext());
        if (project == null) {
            //if the action event data context returns a null project, just return and let successive update calls modify the presentation
            return;
        }
        ToolWindow libertyDevToolWindow = ToolWindowManager.getInstance(project).getToolWindow(Constants.LIBERTY_DEV_DASHBOARD_ID);
        if (libertyDevToolWindow != null) {
            Content content = libertyDevToolWindow.getContentManager().findContent(LocalizedResourceUtil.getMessage("liberty.tool.window.display.name"));
            JComponent libertyWindow = content.getComponent();
            Component[] components = libertyWindow.getComponents();
            for (Component comp : components) {
                if (comp.getName() != null && comp.getName().equals(Constants.LIBERTY_TREE)) {
                    Tree libertyTree = (Tree) comp;

                    TreePath[] selectionPaths = libertyTree.getSelectionPaths();

                    // when only one child node is selected, enable this action
                    if (selectionPaths != null && selectionPaths.length == 1) {
                        String lastPathComponent = selectionPaths[0].getLastPathComponent().toString();
                        if (Constants.FULL_ACTIONS_MAP.containsKey(lastPathComponent)) {
                            e.getPresentation().setEnabled(true);
                        }
                    }
                } else {
                    LOGGER.debug("Tree view not built, no valid projects to run Liberty dev actions on");
                }
            }
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = LibertyProjectUtil.getProject(e.getDataContext());
        if (project == null) return;

        // triggered through shift+shift/Search Everywhere window
        if (Constants.GO_TO_ACTION_TRIGGERED.equalsIgnoreCase(e.getPlace())) {
            // prompt user to select action to run
            final String[] libertyActions = Constants.FULL_ACTIONS_MAP.keySet().toArray(new String[0]);
            final int ret = Messages.showChooseDialog(project, LocalizedResourceUtil.getMessage("liberty.action.selection.dialog.message"), LocalizedResourceUtil.getMessage("liberty.action.selection.dialog.title"), LibertyPluginIcons.libertyIcon_40, libertyActions, libertyActions[0]);
            // ret < 0  the user pressed cancel on the dialog, take no action
            if (ret >= 0) {
                String selectedAction = libertyActions[ret];
                // run selected action
                AnAction action = ActionManager.getInstance().getAction(Constants.FULL_ACTIONS_MAP.get(selectedAction));
                action.actionPerformed(new AnActionEvent(null, e.getDataContext(), e.getPlace(), e.getPresentation(), ActionManager.getInstance(), 0));
            }
        } else {
            // triggered through icon on tool window
            ToolWindow libertyDevToolWindow = ToolWindowManager.getInstance(project).getToolWindow(Constants.LIBERTY_DEV_DASHBOARD_ID);

            Content content = libertyDevToolWindow.getContentManager().findContent(LocalizedResourceUtil.getMessage("liberty.tool.window.display.name"));

            JComponent libertyWindow = content.getComponent();

            Component[] components = libertyWindow.getComponents();

            for (Component comp : components) {
                if (comp.getName() != null && comp.getName().equals(Constants.LIBERTY_TREE)) {
                    Tree libertyTree = (Tree) comp;
                    TreePath[] selectionPaths = libertyTree.getSelectionPaths();
                    if (selectionPaths != null && selectionPaths.length == 1) {
                        String lastPathComponent = selectionPaths[0].getLastPathComponent().toString();
                        if (Constants.FULL_ACTIONS_MAP.containsKey(lastPathComponent)) {
                            // calls selected action
                            AnAction action = ActionManager.getInstance().getAction(Constants.FULL_ACTIONS_MAP.get(lastPathComponent));
                            action.actionPerformed(new AnActionEvent(null, DataManager.getInstance().getDataContext(libertyTree), e.getPlace(), e.getPresentation(), ActionManager.getInstance(), 0));
                        }
                    }
                } else {
                    LOGGER.debug("Tree view not built, no valid projects to run Liberty dev actions on");
                }
            }
        }
    }
}
