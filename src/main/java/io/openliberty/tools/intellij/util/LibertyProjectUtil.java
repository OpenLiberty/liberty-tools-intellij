/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation.
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
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
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
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;

public class LibertyProjectUtil {
    private static Logger LOGGER = Logger.getInstance(LibertyProjectUtil.class);

    enum BuildFileFilter {
        ADDABLE {
            public boolean matches(Project project, BuildFile buildFile, VirtualFile virtualFile) {
                return !LIST.matches(project, buildFile, virtualFile);
            }
        },
        REMOVABLE {
            public boolean matches(Project project, BuildFile buildFile, VirtualFile virtualFile) {
                return isCustomLibertyProject(project, virtualFile) && !(buildFile.isValidBuildFile() || isLibertyProject(virtualFile));
            }
        },
        LIST {
            public boolean matches(Project project, BuildFile buildFile, VirtualFile virtualFile) {
                return buildFile.isValidBuildFile() || isLibertyProject(virtualFile) || isCustomLibertyProject(project, virtualFile);
            }
        };
        public abstract boolean matches(Project project, BuildFile buildFile, VirtualFile virtualFile);
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

    public static boolean isCustomLibertyProject(Project project, VirtualFile buildFile) {
        final LibertyProjectSettings state = LibertyProjectSettings.getInstance(project);
        return state.getCustomLibertyProjects().contains(buildFile.getPath());
    }

    /**
     * Returns a list of valid Gradle build files in the project
     * @param project
     * @return ArrayList of BuildFiles
     */
    public static ArrayList<BuildFile> getGradleBuildFiles(Project project) throws IOException, SAXException, ParserConfigurationException {
        return getBuildFiles(project, Constants.ProjectType.LIBERTY_GRADLE_PROJECT, BuildFileFilter.LIST);
    }

    /**
     * Returns a list of valid Maven build files in the project
     * @param project
     * @return ArrayList of BuildFiles
     */
    public static ArrayList<BuildFile> getMavenBuildFiles(Project project) throws IOException, SAXException, ParserConfigurationException {
        return getBuildFiles(project, Constants.ProjectType.LIBERTY_MAVEN_PROJECT, BuildFileFilter.LIST);
    }

    /**
     * Returns a list of Gradle build files in the project that can be added as Liberty projects
     * @param project
     * @return ArrayList of BuildFiles
     */
    public static ArrayList<BuildFile> getAddableGradleBuildFiles(Project project) throws IOException, SAXException, ParserConfigurationException {
        return getBuildFiles(project, Constants.ProjectType.LIBERTY_GRADLE_PROJECT, BuildFileFilter.ADDABLE);
    }

    /**
     * Returns a list of Maven build files in the project that can be added as Liberty projects
     * @param project
     * @return ArrayList of BuildFiles
     */
    public static ArrayList<BuildFile> getAddableMavenBuildFiles(Project project) throws IOException, SAXException, ParserConfigurationException {
        return getBuildFiles(project, Constants.ProjectType.LIBERTY_MAVEN_PROJECT, BuildFileFilter.ADDABLE);
    }

    /**
     * Returns a list of Gradle build files in the project that can be removed as Liberty projects
     * @param project
     * @return ArrayList of BuildFiles
     */
    public static ArrayList<BuildFile> getRemovableGradleBuildFiles(Project project) throws IOException, SAXException, ParserConfigurationException {
        return getBuildFiles(project, Constants.ProjectType.LIBERTY_GRADLE_PROJECT, BuildFileFilter.REMOVABLE);
    }

    /**
     * Returns a list of Maven build files in the project that can be removed as Liberty projects
     * @param project
     * @return ArrayList of BuildFiles
     */
    public static ArrayList<BuildFile> getRemovableMavenBuildFiles(Project project) throws IOException, SAXException, ParserConfigurationException {
        return getBuildFiles(project, Constants.ProjectType.LIBERTY_MAVEN_PROJECT, BuildFileFilter.REMOVABLE);
    }

    /**
     * Get the Terminal widget for corresponding Liberty module
     *
     * @param project
     * @param libertyModule
     * @param createWidget  true if a new widget should be created
     * @return TerminalWidget or null if it does not exist
     */
    public static TerminalWidget getTerminalWidget(Project project, LibertyModule libertyModule, boolean createWidget,
                                                        TerminalToolWindowManager terminalToolWindowManager, TerminalWidget widget) {
        if (widget == null && createWidget) {
            // Create terminal tab — returns TerminalWidget
            TerminalWidget terminalWidget = terminalToolWindowManager.createShellWidget(
                    project.getBasePath(), libertyModule.getName(), true, true);

            // Store it in the module
            libertyModule.setTerminalWidget(terminalWidget);

            return terminalWidget;
        }

        return widget;
    }

    public static void setFocusToWidget(Project project, TerminalWidget widget) {
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

    // Search the filename index to find valid build files (Maven and Gradle) for the current project
    private static ArrayList<BuildFile> getBuildFiles(Project project, Constants.ProjectType buildFileType, BuildFileFilter filter) {
        ArrayList<BuildFile> collectedBuildFiles = new ArrayList<BuildFile>();
        Collection<VirtualFile> indexedVFiles;
        if (buildFileType.equals(Constants.ProjectType.LIBERTY_MAVEN_PROJECT)) {
            indexedVFiles = readIndex(project, "pom.xml");
        } else {
            indexedVFiles = readIndex(project, "build.gradle");
        }
        if (indexedVFiles != null) {
            for (VirtualFile vFile : indexedVFiles) {
                try {
                    BuildFile buildFile;
                    if (buildFileType.equals(Constants.ProjectType.LIBERTY_MAVEN_PROJECT)) {
                        buildFile = LibertyMavenUtil.validPom(vFile);
                    } else {
                        buildFile = LibertyGradleUtil.validBuildGradle(vFile);
                    }
                    // check if valid pom.xml or build.gradle, or if part of Liberty project
                    if (filter.matches(project, buildFile, vFile)) {
                        buildFile.setBuildFile(vFile);
                        buildFile.setProjectType(buildFileType);
                        collectedBuildFiles.add(buildFile);
                    }
                } catch (Exception e) {
                    LOGGER.error(String.format("Error parsing build file %s", vFile), e.getMessage());
                }
            }
        }
        return collectedBuildFiles;
    }

    // Wrap the search for files in a executeOnPooledThread() method to handle the slow operations on EDT issue
    // and in a runReadAction() to handle the read action required problem.
    private static Collection<VirtualFile> readIndex(Project project, String name) {
        try {
            Computable<Collection<VirtualFile>> virtualFilesComputation = () -> FilenameIndex.getVirtualFilesByName(name, GlobalSearchScope.projectScope(project));
            Callable<Collection<VirtualFile>> readAction = () -> ApplicationManager.getApplication().runReadAction(virtualFilesComputation);
            Future<Collection<VirtualFile>> filesFuture = ApplicationManager.getApplication().executeOnPooledThread(readAction);
            return filesFuture.get();
        } catch (ExecutionException | InterruptedException e) {
            return null;
        }
    }

    /**
     * 
     * @param buildFile maven or gradle build file in the form of PsiFile
     * @return <code>true</code> if the project contains src/main/liberty/config/server.xml relative to the build file; <code>false</code> otherwise
     */
    private static boolean isLibertyProject(VirtualFile buildFile) {
        String rootDir = buildFile.getParent().getPath();
        return new File(rootDir, "src/main/liberty/config/server.xml").exists();
    }

    /**
     * Get the Terminal widget for the corresponding Liberty module. Will check if the Terminal widget
     * exists in the Terminal view.
     *
     * @param libertyModule
     * @param terminalToolWindowManager
     * @return TerminalWidget or null if it does not exist
     */
    public static TerminalWidget getTerminalWidget(LibertyModule libertyModule, TerminalToolWindowManager terminalToolWindowManager) {
        TerminalWidget widget = libertyModule.getTerminalWidget();
        // check if widget exists in terminal view
        if (widget != null) {
            for (TerminalWidget terminalWidget : terminalToolWindowManager.getTerminalWidgets()) {
                JBTerminalWidget jbTerminalWidget = JBTerminalWidget.asJediTermWidget(terminalWidget);
                if (widget.equals(jbTerminalWidget)) {
                    return widget;
                }
            }
        }
        libertyModule.setTerminalWidget(null);
        return null;
    }

    public static String includeEscapeToString(String path) {
        return "\"" + path + "\"";
    }

    /**
     * Checks whether a command is currently running in the given terminal widget.
     * This method ensures compatibility across IntelliJ versions:
     * - In IntelliJ IDEA 2025.1 and later, it attempts to call {@code isCommandRunning()}.
     * - In IntelliJ IDEA 2024.x and earlier, it falls back to {@code hasRunningCommands()}.
     * If neither method is available or an error occurs, it returns {@code false}.
     *
     * @param terminalWidget the terminal widget instance to check. Typically an instance of TerminalWidget, JBTerminalWidget, or ShellTerminalWidget.
     * @return true if a command is running in the terminal widget; false otherwise.
     */
    public static boolean isCommandRunningSafe(Object terminalWidget) {
        if (terminalWidget == null) return false;
        try {
            // Try to call isCommandRunning() (2025.1+)
            return (boolean) terminalWidget.getClass()
                    .getMethod("isCommandRunning")
                    .invoke(terminalWidget);
        } catch (NoSuchMethodException e1) {
            try {
                // Try fallback to hasRunningCommands() (2024.x and before)
                return (boolean) terminalWidget.getClass()
                        .getMethod("hasRunningCommands")
                        .invoke(terminalWidget);
            } catch (Exception e2) {
                // Both methods missing or failed - treat as not running
            }
        } catch (Exception e) {
            // Invocation target error or other issues
        }
        return false;
    }
}
