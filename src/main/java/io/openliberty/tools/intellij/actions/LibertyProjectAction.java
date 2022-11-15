/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import io.openliberty.tools.intellij.LibertyPluginIcons;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class LibertyProjectAction extends LibertyGeneralAction {

    private static final Logger LOGGER = Logger.getInstance(LibertyProjectAction.class);

    private final RefreshLibertyToolbar refreshLibertyToolbar = new RefreshLibertyToolbar();

    public abstract String getChooseDialogTitle();
    public abstract String getChooseDialogMessage();

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        project = LibertyProjectUtil.getProject(e.getDataContext());
        if (project == null) {
            // TODO prompt user to select project
            String msg = LocalizedResourceUtil.getMessage("liberty.project.does.not.resolve", actionCmd);
            notifyError(msg);
            LOGGER.debug(msg);
            return;
        }
        List<BuildFileInfo> buildFileInfoList = getBuildFileInfoList();
        if (!buildFileInfoList.isEmpty()) {
            final String[] projectNames = toProjectNames(buildFileInfoList);
            final int ret = Messages.showChooseDialog(project,
                    getChooseDialogMessage(),
                    getChooseDialogTitle(),
                    LibertyPluginIcons.libertyIcon_40,
                    projectNames,
                    projectNames[0]);
            // Execute the action if a project was selected.
            if (ret >= 0 && ret < buildFileInfoList.size()) {
                buildFileInfoList.get(ret).writeTo(this);
                executeLibertyAction();
                refreshLibertyToolbar.actionPerformed(e);
            }
        }
        else {
            // Notify the user that no projects were detected that apply to this action.
            Messages.showMessageDialog(project,
                    LocalizedResourceUtil.getMessage("liberty.project.no.projects.detected.dialog.message"),
                    getChooseDialogTitle(),
                    LibertyPluginIcons.libertyIcon_40);
        }
    }
}
