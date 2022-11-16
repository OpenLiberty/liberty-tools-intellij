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

import io.openliberty.tools.intellij.util.BuildFile;
import io.openliberty.tools.intellij.util.LibertyProjectUtil;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;

public class AddLibertyProjectAction extends LibertyProjectAction {

    public AddLibertyProjectAction() {
        setActionCmd(LocalizedResourceUtil.getMessage("liberty.project.add"));
    }

    @Override
    protected ArrayList<BuildFile> getMavenBuildFiles() throws IOException, SAXException, ParserConfigurationException {
        return LibertyProjectUtil.getAddableMavenBuildFiles(project);
    }

    @Override
    protected ArrayList<BuildFile> getGradleBuildFiles() throws IOException, SAXException, ParserConfigurationException {
        return LibertyProjectUtil.getAddableGradleBuildFiles(project);
    }

    @Override
    protected void executeLibertyAction() {
        LibertyProjectUtil.addCustomLibertyProject(buildFile);
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
