/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.actions;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import io.openliberty.tools.intellij.LibertyExplorer;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;
import org.jetbrains.annotations.NotNull;

public class RefreshLibertyToolbar extends AnAction {
    private static final Logger LOGGER = Logger.getInstance(RefreshLibertyToolbar.class);

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // Schedule actions on the event dispatching thread.
        // See: https://plugins.jetbrains.com/docs/intellij/basic-action-system.html#principal-implementation-overrides.
        return ActionUpdateThread.EDT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = LibertyProjectUtil.getProject(e.getDataContext());
        refreshDashboard(project);
    }

    public static void refreshDashboard(Project project) {
        if (project == null) {
            LOGGER.debug("Unable to refresh Liberty toolbar, could not resolve project");
            return;
        }
        ProjectView.getInstance(project).refresh();

        ToolWindow libertyDevToolWindow = ToolWindowManager.getInstance(project).getToolWindow(Constants.LIBERTY_DEV_DASHBOARD_ID);
        if (libertyDevToolWindow == null) {
            LOGGER.debug("Unable to refresh Liberty toolbar, could not find tool window");
            return;
        }

        Content content = libertyDevToolWindow.getContentManager().findContent(
                LocalizedResourceUtil.getMessage("liberty.tool.window.display.name"));
        if (content == null) {
            LOGGER.debug("Unable to refresh Liberty toolbar, could not find tool window content");
            return;
        }

        if (content.getComponent() instanceof LibertyExplorer explorer) {
            explorer.refresh(project);
        }
    }
}
