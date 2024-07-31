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

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.LibertyModule;
import io.openliberty.tools.intellij.LibertyPluginIcons;
import io.openliberty.tools.intellij.util.*;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;

public abstract class LibertyProjectAction extends LibertyGeneralAction {
    private static final Logger LOGGER = Logger.getInstance(LibertyProjectAction.class);

    public abstract String getChooseDialogTitle();

    public abstract String getChooseDialogMessage();

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        String actionCmd = e.getPresentation().getText();
        if (project == null) {
            // TODO prompt user to select project
            String msg = LocalizedResourceUtil.getMessage("liberty.project.does.not.resolve", actionCmd);
            notifyError(msg, project);
            LOGGER.debug(msg);
            return;
        }
        List<BuildFile> buildFileList = getBuildFileList(project);
        if (!buildFileList.isEmpty()) {
            String[] projectName = buildFileList.stream()
                    .map(BuildFile::getProjectName)
                    .toArray(String[]::new);
            String[] projectPath = buildFileList.stream()
                    .map(buildFile -> buildFile.getBuildFile().getPath())
                    .toArray(String[]::new);

            LibertyProjectChooserDialog dialog = new LibertyProjectChooserDialog(
                    project,
                    getChooseDialogMessage(),
                    getChooseDialogTitle(),
                    LibertyPluginIcons.libertyIcon_40,
                    projectName,
                    projectPath,
                    projectName[0]);
            dialog.show();
            final int ret = dialog.getSelectedIndex();
            // Execute the action if a project was selected.
            if (ret >= 0 && ret < buildFileList.size()) {
                BuildFile selectedBuildFile = buildFileList.get(ret);
                LibertyModule module = new LibertyModule(project, selectedBuildFile);
                executeLibertyAction(module);
                RefreshLibertyToolbar.refreshDashboard(project);
            }
        } else {
            // Notify the user that no projects were detected that apply to this action.
            Messages.showMessageDialog(project,
                    LocalizedResourceUtil.getMessage("liberty.project.no.projects.detected.dialog.message"),
                    getChooseDialogTitle(),
                    LibertyPluginIcons.libertyIcon_40);
        }
    }

    /* Returns an aggregated list containing info for all Maven and Gradle build files. */
    protected final List<BuildFile> getBuildFileList(Project project) {
        final List<BuildFile> buildFiles = new ArrayList<BuildFile>();
        final List<BuildFile> mavenBuildFiles;
        final List<BuildFile> gradleBuildFiles;
        try {
            mavenBuildFiles = getMavenBuildFiles(project);
            gradleBuildFiles = getGradleBuildFiles(project);
        } catch (IOException | SAXException | ParserConfigurationException e) {
            LOGGER.error("Could not find Maven or Gradle projects in workspace",
                    e.getMessage());
            return Collections.emptyList();
        }
        if (mavenBuildFiles.isEmpty() && gradleBuildFiles.isEmpty()) {
            return Collections.emptyList();
        }
        // Resolve project names and add to list
        mavenBuildFiles.forEach(mavenBuildFile -> {
            // resolve project name
            VirtualFile virtualFile = mavenBuildFile.getBuildFile();
            if (virtualFile == null) {
                LOGGER.error(String.format("Could not resolve Maven project for build file: %s", mavenBuildFile.getBuildFile()));
            } else {
                try {
                    mavenBuildFile.setProjectName(LibertyMavenUtil.getProjectNameFromPom(virtualFile));
                    mavenBuildFile.setProjectType(Constants.LIBERTY_MAVEN_PROJECT);
                    buildFiles.add(mavenBuildFile);
                } catch (Exception e) {
                    LOGGER.error(String.format("Could not resolve project name from pom.xml: %s", virtualFile), e.getMessage());
                }
            }

        });
        gradleBuildFiles.forEach(gradleBuildFile -> {
            VirtualFile virtualFile = gradleBuildFile.getBuildFile();
            if (virtualFile == null) {
                LOGGER.error(String.format("Could not resolve Gradle project for build file: %s", gradleBuildFile.getBuildFile()));
            } else {
                try {
                    gradleBuildFile.setProjectName(LibertyGradleUtil.getProjectName(virtualFile));
                    gradleBuildFile.setProjectType(Constants.LIBERTY_GRADLE_PROJECT);
                    buildFiles.add(gradleBuildFile);
                } catch (Exception e) {
                    LOGGER.error(String.format("Could not resolve project name from settings.gradle: %s", virtualFile), e.getMessage());
                }
            }

        });
        return buildFiles;
    }


    protected ArrayList<BuildFile> getGradleBuildFiles(Project project) throws IOException, ParserConfigurationException, SAXException {
        return LibertyProjectUtil.getGradleBuildFiles(project);
    }

    protected ArrayList<BuildFile> getMavenBuildFiles(Project project) throws IOException, SAXException, ParserConfigurationException {
        return LibertyProjectUtil.getMavenBuildFiles(project);
    }
}
