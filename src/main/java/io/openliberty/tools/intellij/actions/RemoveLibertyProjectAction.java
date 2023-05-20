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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import io.openliberty.tools.intellij.LibertyModule;
import io.openliberty.tools.intellij.LibertyPluginIcons;
import io.openliberty.tools.intellij.util.BuildFile;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;

public class RemoveLibertyProjectAction extends LibertyProjectAction {

    /**
     * Returns the name of the action command being processed.
     *
     * @return The name of the action command being processed.
     */
    protected String getActionCommandName() {
        return LocalizedResourceUtil.getMessage("liberty.project.remove");
    }

    @Override
    protected ArrayList<BuildFile> getMavenBuildFiles(Project project) throws IOException, SAXException, ParserConfigurationException {
        return LibertyProjectUtil.getRemovableMavenBuildFiles(project);
    }

    @Override
    protected ArrayList<BuildFile> getGradleBuildFiles(Project project) throws IOException, SAXException, ParserConfigurationException {
        return LibertyProjectUtil.getRemovableGradleBuildFiles(project);
    }

    @Override
    protected void executeLibertyAction(LibertyModule libertyModule) {
        Project project = libertyModule.getProject();
        String projectName = project.getName();
        final int result = Messages.showYesNoDialog(
                LocalizedResourceUtil.getMessage("liberty.project.remove.confirmation.dialog.message", projectName),
                getChooseDialogTitle(),
                LibertyPluginIcons.libertyIcon_40);
        // Remove the project only if the user confirms it.
        if (result == Messages.YES) {
            LibertyProjectUtil.removeCustomLibertyProject(libertyModule);
        }
    }

    @Override
    public String getChooseDialogTitle() {
        return LocalizedResourceUtil.getMessage("liberty.project.remove.dialog.title");
    }

    @Override
    public String getChooseDialogMessage() {
        return LocalizedResourceUtil.getMessage("liberty.project.remove.dialog.message");
    }
}
