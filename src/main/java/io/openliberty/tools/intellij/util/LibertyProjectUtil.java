/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation.
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
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.terminal.JBTerminalWidget;
import com.intellij.terminal.ui.TerminalWidget;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.sun.istack.Nullable;
import io.openliberty.tools.intellij.LibertyModule;
import io.openliberty.tools.intellij.LibertyModules;
import io.openliberty.tools.intellij.LibertyProjectSettings;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;
import org.jetbrains.plugins.terminal.TerminalView;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.Collections;
import java.util.HashSet;

public class LibertyProjectUtil {
    private static Logger LOGGER = Logger.getInstance(LibertyProjectUtil.class);

    enum BuildFileFilter {
        ADDABLE {
            public boolean matches(Project project, BuildFile buildFile, PsiFile psiFile) {
                return !LIST.matches(project, buildFile, psiFile);
            }
        },
        REMOVABLE {
            public boolean matches(Project project, BuildFile buildFile, PsiFile psiFile) {
                return isCustomLibertyProject(project, psiFile) && !(buildFile.isValidBuildFile() || isLibertyProject(psiFile));
            }
        },
        LIST {
            public boolean matches(Project project, BuildFile buildFile, PsiFile psiFile) {
                return buildFile.isValidBuildFile() || isLibertyProject(psiFile) || isCustomLibertyProject(project, psiFile);
            }
        };
        public abstract boolean matches(Project project, BuildFile buildFile, PsiFile psiFile);
    }

    /** REVISIT: In memory collection of Liberty projects but need to persist. **/
    private static final Set<String> customLibertyProjects = Collections.synchronizedSet(new HashSet<>());

    @Nullable
    public static Project getProject(DataContext context) {
        return CommonDataKeys.PROJECT.getData(context);
    }

    public static void addCustomLibertyProject(LibertyModule libertyModule) {
        final String path = libertyModule.getBuildFile().getPath();
        if (path != null) {
            final LibertyProjectSettings state = LibertyProjectSettings.getInstance(libertyModule.getProject());
            state.getCustomLibertyProjects().add(path);
            LibertyModules.getInstance().addLibertyModule(libertyModule);
        }
    }

    public static void removeCustomLibertyProject(LibertyModule libertyModule) {
        final LibertyProjectSettings state = LibertyProjectSettings.getInstance(libertyModule.getProject());
        state.getCustomLibertyProjects().remove(libertyModule.getBuildFile().getPath());
        LibertyModules.getInstance().removeLibertyModule(libertyModule);
    }

    public static boolean isCustomLibertyProject(Project project, PsiFile buildFile) {
        final LibertyProjectSettings state = LibertyProjectSettings.getInstance(project);
        return state.getCustomLibertyProjects().contains(buildFile.getVirtualFile().getPath());
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
     * Get the Terminal widget for corresponding Liberty module
     *
     * @param project
     * @param libertyModule
     * @param createWidget  true if a new widget should be created
     * @return ShellTerminalWidget or null if it does not exist
     */
    public static ShellTerminalWidget getTerminalWidget(Project project, LibertyModule libertyModule, boolean createWidget,
                                                        TerminalToolWindowManager terminalToolWindowManager, ShellTerminalWidget widget) {
        if (widget == null && createWidget) {
            // create a new terminal tab
            ShellTerminalWidget newTerminal = ShellTerminalWidget.toShellJediTermWidgetOrThrow(
                    terminalToolWindowManager.createShellWidget(project.getBasePath(), libertyModule.getName(),
                            true, true));
            libertyModule.setShellWidget(newTerminal);
            return newTerminal;
        }
        return widget;
    }

    public static void setFocusToWidget(Project project, ShellTerminalWidget widget) {
        TerminalToolWindowManager manager = TerminalToolWindowManager.getInstance(project);
        ToolWindow toolWindow = manager.getToolWindow();

        if (toolWindow != null && widget != null) {
            ContentManager contentManager = toolWindow.getContentManager();
            Content[] contents = contentManager.getContents();

            int index = 0;
            for (int i = 0; i < contents.length; i++) {
                if (contents[i].getPreferredFocusableComponent().equals(widget)) {
                    index = i;
                    break;
                }
            }
            if (contents.length > 0) {
                Content terminalContent = contents[index];
                contentManager.setSelectedContent(terminalContent);
                terminalContent.getComponent().requestFocus();
            }
        }
    }

    // returns valid build files for the current project
    private static ArrayList<BuildFile> getBuildFiles(Project project, String buildFileType, BuildFileFilter filter) throws ParserConfigurationException, SAXException, IOException {
        ArrayList<BuildFile> buildFiles = new ArrayList<BuildFile>();

        if (buildFileType.equals(Constants.LIBERTY_MAVEN_PROJECT)) {
            PsiFile[] mavenFiles = FilenameIndex.getFilesByName(project, "pom.xml", GlobalSearchScope.projectScope(project));
            for (PsiFile mavenFile : mavenFiles) {
                BuildFile buildFile = LibertyMavenUtil.validPom(mavenFile);
                // check if valid pom.xml, or if part of Liberty project
                if (filter.matches(project, buildFile, mavenFile)) {
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
                    if (filter.matches(project, buildFile, gradleFile)) {
                        buildFile.setBuildFile(gradleFile);
                        buildFiles.add(buildFile);
                    }
                } catch (Exception e) {
                    LOGGER.error(String.format("Error parsing build.gradle %s", gradleFile), e.getMessage());
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
        String rootDir = buildFile.getVirtualFile().getParent().getPath();
        return new File(rootDir, "src/main/liberty/config/server.xml").exists();
    }

    /**
     * Get the Terminal widget for the corresponding Liberty module. Will check if the Terminal widget
     * exists in the Terminal view.
     *
     * @param libertyModule
     * @param terminalToolWindowManager
     * @return ShellTerminalWidget or null if it does not exist
     */
    public static ShellTerminalWidget getTerminalWidget(LibertyModule libertyModule, TerminalToolWindowManager terminalToolWindowManager) {
        ShellTerminalWidget widget = libertyModule.getShellWidget();
        // check if widget exists in terminal view
        if (widget != null) {
            for (TerminalWidget terminalWidget : terminalToolWindowManager.getTerminalWidgets()) {
                JBTerminalWidget jbTerminalWidget = JBTerminalWidget.asJediTermWidget(terminalWidget);
                if (widget.equals(jbTerminalWidget)) {
                    return widget;
                }
            }
        }
        libertyModule.setShellWidget(null);
        return null;
    }

    public static String includeEscapeToString(String path) {
        return "\"" + path + "\"";
    }
}
