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

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.SystemInfo;
import io.openliberty.tools.intellij.util.*;
import org.jetbrains.idea.maven.config.MavenConfig;
import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.idea.maven.project.MavenGeneralSettings;
import org.jetbrains.idea.maven.project.MavenGeneralSettingsWatcher;
import org.jetbrains.idea.maven.project.MavenWorkspaceSettings;
import org.jetbrains.idea.maven.project.MavenWorkspaceSettingsComponent;
import org.jetbrains.plugins.gradle.service.settings.GradleSystemSettingsControl;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import com.intellij.openapi.project.Project;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.jetbrains.idea.maven.server.MavenServerManager;

//import org.jetbrains.plugins.maven;
//import org.jetbrains.idea.maven.server.*;
//
public class LibertyDevStartAction extends LibertyGeneralAction {

    public LibertyDevStartAction() {
        setActionCmd(LocalizedResourceUtil.getMessage("start.liberty.dev"));
    }

    @Override
    protected void executeLibertyAction() {
        ShellTerminalWidget widget = LibertyProjectUtil.getTerminalWidget(project, projectName, true);
        String startCmd = null;

        if (projectType.equals(Constants.LIBERTY_MAVEN_PROJECT)) {
            startCmd = getMavenConfigPreference(project) + " io.openliberty.tools:liberty-maven-plugin:dev";
        } else if (projectType.equals(Constants.LIBERTY_GRADLE_PROJECT)) {
            startCmd = getGradleConfigPreference(project) + " libertyDev";
        }
        if (widget == null) {
            LOGGER.debug("Unable to start Liberty dev mode, could not get or create terminal widget for " + projectName);
            return;
        }
        String cdToProjectCmd = "cd \"" + buildFile.getParent().getCanonicalPath() + "\"";
        LibertyActionUtil.executeCommand(widget, cdToProjectCmd);
        LibertyActionUtil.executeCommand(widget, startCmd);
    }

    private String getGradleConfigPreference(Project project) {
//        GradleSystemSettingsControl gradleSettings = GradleSystemSettingsControl;
        GradleSettings gradleSettings = GradleSettings.getInstance(project);
//        gradleSettings.myLinkedProjectsSettings
        GradleProjectSettings gradleProjectSettings = gradleSettings.getLinkedProjectSettings(project.getBasePath());

        return "gradle";
    }

    private String getMavenConfigPreference(Project project) {
        MavenServerManager mavenManager = MavenServerManager.getInstance();
        MavenGeneralSettings mavenSettings = MavenWorkspaceSettingsComponent.getInstance(project).getSettings().getGeneralSettings();
        String mavenHome = mavenSettings.getMavenHome();

        if (mavenManager.WRAPPED_MAVEN.equals(mavenHome)) {
            // it is set to use the wrapper
            String mvnwPath = getLocalMavenWrapper(project);
            if (mvnwPath != null) {
                return mvnwPath;
            }
        } else {
            // try to use maven home path defined in the settings
            File mavenHomeFile = getCustomMavenPath(mavenHome);
            if (mavenHomeFile != null) {
                return mavenHomeFile.getAbsolutePath();
            }
        }
        // default maven
        return "mvn";
    }

    private String getLocalMavenWrapper(Project project) {
        String mvnw = SystemInfo.isWindows ? "mvnw.cmd" : "./mvnw";
        File file = new File(project.getBasePath() + File.separator + mvnw);
        return file.exists() ? mvnw : null;
    }

    private File getCustomMavenPath(String customMavenHome) {
        File mavenHomeFile = MavenServerManager.getMavenHomeFile(customMavenHome); // when customMavenHome path is invalid it returns null
        if (mavenHomeFile != null) {
            File file = new File(mavenHomeFile.getAbsolutePath() + File.separator + "bin" + File.separator + "mvn");
            return file.exists() ? file : null;
        }
        return null;
    }
}