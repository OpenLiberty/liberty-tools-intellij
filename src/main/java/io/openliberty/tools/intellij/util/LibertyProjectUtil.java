/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.util;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.terminal.JBTerminalWidget;
import com.intellij.ui.content.Content;
import com.sun.istack.Nullable;
import io.openliberty.tools.intellij.LibertyModule;
import io.openliberty.tools.intellij.LibertyModules;
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalTabState;
import org.jetbrains.plugins.terminal.TerminalView;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class LibertyProjectUtil {
    private static Logger LOGGER = Logger.getInstance(LibertyProjectUtil.class);;

    enum BuildFileFilter {
        ADDABLE {
            public boolean matches(BuildFile buildFile, PsiFile psiFile) {
                return !LIST.matches(buildFile, psiFile);
            }
        },
        REMOVABLE {
            public boolean matches(BuildFile buildFile, PsiFile psiFile) {
                return isCustomLibertyProject(psiFile) && !(buildFile.isValidBuildFile() || isLibertyProject(psiFile));
            }
        },
        LIST {
            public boolean matches(BuildFile buildFile, PsiFile psiFile) {
                return buildFile.isValidBuildFile() || isLibertyProject(psiFile) || isCustomLibertyProject(psiFile);
            }
        };
        public abstract boolean matches(BuildFile buildFile, PsiFile _buildFile);
    }

    /** REVISIT: In memory collection of Liberty projects but need to persist. **/
    private static final Set<String> customLibertyProjects = Collections.synchronizedSet(new HashSet<>());

    @Nullable
    public static Project getProject(DataContext context) {
        return CommonDataKeys.PROJECT.getData(context);
    }

    public static void addCustomLibertyProject(LibertyModule libertyModule) {
        final String path = libertyModule.getBuildFile().getCanonicalPath();
        if (path != null) {
            customLibertyProjects.add(path);
            LibertyModules.getInstance().addLibertyModule(libertyModule);
        }
    }

    public static void removeCustomLibertyProject(LibertyModule libertyModule) {
        customLibertyProjects.remove(libertyModule.getBuildFile().getCanonicalPath());
        LibertyModules.getInstance().removeLibertyModule(libertyModule);
    }

    public static boolean isCustomLibertyProject(PsiFile buildFile) {
        return customLibertyProjects.contains(buildFile.getVirtualFile().getCanonicalPath());
    }

    /**
     * Returns a list of valid Gradle build files in the project
     * @param project
     * @return ArrayList of BuildFiles
     */
    public static ArrayList<BuildFile> getGradleBuildFiles(Project project) throws IOException, SAXException, ParserConfigurationException {
        return getBuildFiles(project, Constants.LIBERTY_GRADLE_PROJECT, BuildFileFilter.LIST);
    }

    /**
     * Returns a list of valid Maven build files in the project
     * @param project
     * @return ArrayList of BuildFiles
     */
    public static ArrayList<BuildFile> getMavenBuildFiles(Project project) throws IOException, SAXException, ParserConfigurationException {
        return getBuildFiles(project, Constants.LIBERTY_MAVEN_PROJECT, BuildFileFilter.LIST);
    }

    /**
     * Returns a list of Gradle build files in the project that can be added as Liberty projects
     * @param project
     * @return ArrayList of BuildFiles
     */
    public static ArrayList<BuildFile> getAddableGradleBuildFiles(Project project) throws IOException, SAXException, ParserConfigurationException {
        return getBuildFiles(project, Constants.LIBERTY_GRADLE_PROJECT, BuildFileFilter.ADDABLE);
    }

    /**
     * Returns a list of Maven build files in the project that can be added as Liberty projects
     * @param project
     * @return ArrayList of BuildFiles
     */
    public static ArrayList<BuildFile> getAddableMavenBuildFiles(Project project) throws IOException, SAXException, ParserConfigurationException {
        return getBuildFiles(project, Constants.LIBERTY_MAVEN_PROJECT, BuildFileFilter.ADDABLE);
    }

    /**
     * Returns a list of Gradle build files in the project that can be removed as Liberty projects
     * @param project
     * @return ArrayList of BuildFiles
     */
    public static ArrayList<BuildFile> getRemovableGradleBuildFiles(Project project) throws IOException, SAXException, ParserConfigurationException {
        return getBuildFiles(project, Constants.LIBERTY_GRADLE_PROJECT, BuildFileFilter.REMOVABLE);
    }

    /**
     * Returns a list of Maven build files in the project that can be removed as Liberty projects
     * @param project
     * @return ArrayList of BuildFiles
     */
    public static ArrayList<BuildFile> getRemovableMavenBuildFiles(Project project) throws IOException, SAXException, ParserConfigurationException {
        return getBuildFiles(project, Constants.LIBERTY_MAVEN_PROJECT, BuildFileFilter.REMOVABLE);
    }

    /**
     * Get the terminal widget for the current project
     * @param project
     * @param projectName
     * @param createWidget true if a new widget should be created
     * @return ShellTerminalWidget object
     */
    public static ShellTerminalWidget getTerminalWidget(Project project, String projectName, boolean createWidget) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow terminalWindow = toolWindowManager.getToolWindow("Terminal");

        // look for existing terminal tab
        ShellTerminalWidget widget = getTerminalWidget(terminalWindow, projectName);
        if (widget != null) {
            return widget;
        } else if (createWidget) {
            // create a new terminal tab
            TerminalView terminalView = TerminalView.getInstance(project);
            LocalTerminalDirectRunner terminalRun = new LocalTerminalDirectRunner(project);
            TerminalTabState tabState = new TerminalTabState();
            tabState.myTabName = projectName;
            tabState.myWorkingDirectory = project.getBasePath();
            terminalView.createNewSession(terminalRun, tabState);
            return getTerminalWidget(terminalWindow, projectName);
        }
        return null;
    }

    // returns valid build files for the current project
    private static ArrayList<BuildFile> getBuildFiles(Project project, String buildFileType, BuildFileFilter filter) throws ParserConfigurationException, SAXException, IOException {
        ArrayList<BuildFile> buildFiles = new ArrayList<BuildFile>();

        if (buildFileType.equals(Constants.LIBERTY_MAVEN_PROJECT)) {
            PsiFile[] mavenFiles = FilenameIndex.getFilesByName(project, "pom.xml", GlobalSearchScope.projectScope(project));
            for (PsiFile mavenFile : mavenFiles) {
                BuildFile buildFile = LibertyMavenUtil.validPom(mavenFile);
                // check if valid pom.xml, or if part of Liberty project
                if (filter.matches(buildFile, mavenFile)) {
                    buildFile.setBuildFile(mavenFile);
                    buildFiles.add(buildFile);
                }
            }
        } else if (buildFileType.equals(Constants.LIBERTY_GRADLE_PROJECT)) {
            PsiFile[] gradleFiles = FilenameIndex.getFilesByName(project, "build.gradle", GlobalSearchScope.projectScope(project));
            for (PsiFile gradleFile : gradleFiles) {
                try {
                    BuildFile buildFile = LibertyGradleUtil.validBuildGradle(gradleFile);
                    // check if valid build.gradle, or if part of Liberty project
                    if (filter.matches(buildFile, gradleFile)) {
                        buildFile.setBuildFile(gradleFile);
                        buildFiles.add(buildFile);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error parsing build.gradle", e.getMessage());
                }
            }
        }
        return buildFiles;
    }

    /**
     * 
     * @param buildFile maven or gradle build file in the form of PsiFile
     * @return <code>true</code> if the project contains src/main/liberty/config/server.xml relative to the build file; <code>false</code> otherwise
     */
    private static boolean isLibertyProject(PsiFile buildFile) {
        String rootDir = buildFile.getVirtualFile().getParent().getCanonicalPath();
        return new File(rootDir, "src/main/liberty/config/server.xml").exists();
    }

    private static ShellTerminalWidget getTerminalWidget(ToolWindow terminalWindow, String projectName) {
        Content[] terminalContents = terminalWindow.getContentManager().getContents();
        for (int i = 0; i < terminalContents.length; i++) {
            // TODO use LibertyModule rather than projectName see https://github.com/OpenLiberty/liberty-tools-intellij/issues/143
            if (terminalContents[i].getTabName().equals(projectName)) {
                JBTerminalWidget widget = TerminalView.getWidgetByContent(terminalContents[i]);
                ShellTerminalWidget shellWidget = (ShellTerminalWidget) Objects.requireNonNull(widget);
                return shellWidget;
            }
        }
        return null;
    }



}
