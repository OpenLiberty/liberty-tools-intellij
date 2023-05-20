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
import io.openliberty.tools.intellij.LibertyModule;
import io.openliberty.tools.intellij.util.BuildFile;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;

public class AddLibertyProjectAction extends LibertyProjectAction {

    /**
     * Returns the name of the action command being processed.
     *
     * @return The name of the action command being processed.
     */
    protected String getActionCommandName() {
        return LocalizedResourceUtil.getMessage("liberty.project.add");
    }

    @Override
    protected ArrayList<BuildFile> getMavenBuildFiles(Project project) throws IOException, SAXException, ParserConfigurationException {
        return LibertyProjectUtil.getAddableMavenBuildFiles(project);
    }

    @Override
    protected ArrayList<BuildFile> getGradleBuildFiles(Project project) throws IOException, SAXException, ParserConfigurationException {
        return LibertyProjectUtil.getAddableGradleBuildFiles(project);
    }

    @Override
    protected void executeLibertyAction(LibertyModule libertyModule) {
        LibertyProjectUtil.addCustomLibertyProject(libertyModule);
    }

    @Override
    public String getChooseDialogTitle() {
        return LocalizedResourceUtil.getMessage("liberty.project.add.dialog.title");
    }

    @Override
    public String getChooseDialogMessage() {
        return LocalizedResourceUtil.getMessage("liberty.project.add.dialog.message");
    }
}
