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

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import io.openliberty.tools.intellij.LibertyPluginIcons;
import io.openliberty.tools.intellij.util.*;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LibertyGeneralAction extends AnAction {

    protected Logger LOGGER = Logger.getInstance(LibertyGeneralAction.class);
    protected Project project;
    protected String projectName;
    protected String projectType;
    protected VirtualFile buildFile;
    protected String actionCmd;

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

        buildFile = (VirtualFile) e.getDataContext().getData(Constants.LIBERTY_BUILD_FILE);
        if (buildFile == null) {
            List<BuildFileInfo> buildFileInfoList = getBuildFileInfoList();
            if (!buildFileInfoList.isEmpty()) {
                // Only one project. Select it.
                if (buildFileInfoList.size() == 1) {
                    buildFileInfoList.get(0).writeTo(this);
                }
                // Multiple projects. Pop up dialog for user to select.
                else {
                    final String[] projectNames = toProjectNames(buildFileInfoList);
                    final int ret = Messages.showChooseDialog(project,
                            LocalizedResourceUtil.getMessage("liberty.project.file.selection.dialog.message", actionCmd),
                            LocalizedResourceUtil.getMessage("liberty.project.file.selection.dialog.title"),
                            LibertyPluginIcons.libertyIcon_40,
                            projectNames,
                            projectNames[0]);
                    if (ret >= 0 && ret < buildFileInfoList.size()) {
                        buildFileInfoList.get(ret).writeTo(this);
                    }
                    // The user pressed cancel on the dialog.
                    else {
                        return;
                    }
                }
            }
            if (buildFile == null) {
                String msg = LocalizedResourceUtil.getMessage("liberty.build.file.does.not.resolve", actionCmd, project.getName());
                notifyError(msg);
                LOGGER.debug(msg);
                return;
            }
        }
        else {
            projectName = (String) e.getDataContext().getData(Constants.LIBERTY_PROJECT_NAME);
            projectType = (String) e.getDataContext().getData(Constants.LIBERTY_PROJECT_TYPE);
        }
        executeLibertyAction();
    }

    /* Returns true if the specified project type applies to this action. */
    protected boolean isProjectTypeSupported(String projectType) {
        return Constants.LIBERTY_MAVEN_PROJECT.equals(projectType) ||
                Constants.LIBERTY_GRADLE_PROJECT.equals(projectType);
    }

    protected ArrayList<BuildFile> getMavenBuildFiles() throws IOException, SAXException, ParserConfigurationException {
        return LibertyProjectUtil.getMavenBuildFiles(project);
    }

    protected ArrayList<BuildFile> getGradleBuildFiles() throws IOException, SAXException, ParserConfigurationException {
        return LibertyProjectUtil.getGradleBuildFiles(project);
    }

    /* Returns an aggregated list containing info for all Maven and Gradle build files. */
    protected final List<BuildFileInfo> getBuildFileInfoList() {
        final List<BuildFile> mavenBuildFiles;
        final List<BuildFile> gradleBuildFiles;
        try {
            mavenBuildFiles = isProjectTypeSupported(Constants.LIBERTY_MAVEN_PROJECT) ?
                    getMavenBuildFiles() : Collections.emptyList();
            gradleBuildFiles = isProjectTypeSupported(Constants.LIBERTY_GRADLE_PROJECT) ?
                    getGradleBuildFiles() : Collections.emptyList();
        }
        catch (IOException | SAXException | ParserConfigurationException e) {
            LOGGER.error("Could not find Open Liberty Maven or Gradle projects in workspace",
                    e.getMessage());
            return Collections.emptyList();
        }
        if (mavenBuildFiles.isEmpty() && gradleBuildFiles.isEmpty()) {
            return Collections.emptyList();
        }
        final List<BuildFileInfo> buildFileInfoList = new ArrayList<>();
        mavenBuildFiles.forEach(buildFile -> {
            PsiFile psiFile = buildFile.getBuildFile();
            String projectName = null;
            VirtualFile virtualFile = psiFile.getVirtualFile();
            if (virtualFile == null) {
                LOGGER.error("Could not resolve current Maven project");
            }
            else {
                try {
                    projectName = LibertyMavenUtil.getProjectNameFromPom(virtualFile);
                } catch (Exception e) {
                    LOGGER.error("Could not resolve project name from pom.xml", e.getMessage());
                }
                LOGGER.info("Liberty Maven Project: " + psiFile);
                if (projectName == null) {
                    projectName = project.getName();
                }
                buildFileInfoList.add(new BuildFileInfo(virtualFile, projectName, Constants.LIBERTY_MAVEN_PROJECT));
            }
        });
        gradleBuildFiles.forEach(buildFile -> {
            PsiFile psiFile = buildFile.getBuildFile();
            String projectName = null;
            VirtualFile virtualFile = psiFile.getVirtualFile();
            if (virtualFile == null) {
                LOGGER.error("Could not resolve current Gradle project");
            }
            else {
                try {
                    projectName = LibertyGradleUtil.getProjectName(virtualFile);
                } catch (Exception e) {
                    LOGGER.error("Could not resolve project name from settings.gradle", e.getMessage());
                }
                LOGGER.info("Liberty Gradle Project: " + psiFile);
                if (projectName == null) {
                    projectName = project.getName();
                }
                buildFileInfoList.add(new BuildFileInfo(virtualFile, projectName, Constants.LIBERTY_GRADLE_PROJECT));
            }
        });
        return buildFileInfoList;
    }

    protected final String[] toProjectNames(@NotNull List<BuildFileInfo> list) {
        final int size = list.size();
        final String[] projectNames = new String[size];
        for (int i = 0; i < size; ++i) {
            projectNames[i] = list.get(i).getProjectName();
        }
        return projectNames;
    }

    protected void setActionCmd(String actionCmd) {
        this.actionCmd = actionCmd;
    }

    protected void executeLibertyAction() {
        // must be implemented by individual actions
    }

    protected void notifyError(String errMsg) {
        Notification notif = new Notification(Constants.LIBERTY_DEV_DASHBOARD_ID
                , LibertyPluginIcons.libertyIcon
                , LocalizedResourceUtil.getMessage("liberty.action.cannot.start")
                , ""
                , errMsg
                , NotificationType.WARNING
                , NotificationListener.URL_OPENING_LISTENER);
        Notifications.Bus.notify(notif, project);
    }

    protected static final class BuildFileInfo {
        private final VirtualFile buildFile;
        private final String projectName;
        private final String projectType;

        public BuildFileInfo(VirtualFile buildFile, String projectName, String projectType) {
            this.buildFile = buildFile;
            this.projectName = projectName;
            this.projectType = projectType;
        }

        public String getProjectName() {
            return projectName;
        }

        public void writeTo(@NotNull LibertyGeneralAction action) {
            action.buildFile = buildFile;
            action.projectName = projectName;
            action.projectType = projectType;
        }
    }
}
