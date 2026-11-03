/*******************************************************************************
 * Copyright (c) 2020, 2026 IBM Corporation.
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
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.terminal.frontend.view.TerminalView;
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTab;
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.sun.istack.Nullable;
import io.openliberty.tools.intellij.LibertyModule;
import io.openliberty.tools.intellij.LibertyModules;
import io.openliberty.tools.intellij.LibertyProjectSettings;
import org.xml.sax.SAXException;

import javax.swing.JComponent;

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
     * Get the Terminal view for the corresponding Liberty module, creating a new one if requested.
     *
     * <p>A new Reworked Terminal tab is created via {@link TerminalToolWindowTabsManager}.
     * The resulting {@link TerminalView} is stored on the module.
     *
     * @param project
     * @param libertyModule
     * @param createView    true if a new view should be created
     * @param existingView  existing view, or {@code null} if none is known
     * @return TerminalView or null if it does not exist
     */
    // TerminalToolWindowTabsManager, TerminalToolWindowTab, TerminalView are @Experimental, but their usage is explicitly recommended by https://plugins.jetbrains.com/docs/intellij/embedded-terminal.html
    @SuppressWarnings("UnstableApiUsage")
    public static TerminalView getTerminalView(Project project, LibertyModule libertyModule, boolean createView,
                                               TerminalView existingView) {
        if (existingView == null && createView) {
            TerminalToolWindowTabsManager tabsManager = TerminalToolWindowTabsManager.getInstance(project);
            TerminalToolWindowTab tab = tabsManager.createTabBuilder()
                    .workingDirectory(project.getBasePath())
                    .tabName(libertyModule.getName())
                    .requestFocus(true)
                    .createTab();
            TerminalView terminalView = tab.getView();
            libertyModule.setTerminalView(terminalView);
            return terminalView;
        }
        return existingView;
    }

    // TerminalToolWindowTabsManager, TerminalToolWindowTab, TerminalView are @Experimental, but their usage is explicitly recommended by https://plugins.jetbrains.com/docs/intellij/embedded-terminal.html
    @SuppressWarnings("UnstableApiUsage")
    public static void setFocusToView(Project project, TerminalView view) {
        if (view == null) return;
        TerminalToolWindowTabsManager tabsManager = TerminalToolWindowTabsManager.getInstance(project);
        for (TerminalToolWindowTab tab : tabsManager.getTabs()) {
            if (view.equals(tab.getView())) {
                Content content = tab.getContent();
                ContentManager contentManager = content.getManager();
                if (contentManager != null) {
                    contentManager.setSelectedContent(content);
                }
                ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
                ToolWindow toolWindow = toolWindowManager.getToolWindow("Terminal");
                if (toolWindow != null && !toolWindow.isActive()) {
                    toolWindow.activate(null, true, true);
                }
                JComponent component = view.getPreferredFocusableComponent();
                component.requestFocus();
                break;
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
     * Get the Terminal view for the corresponding Liberty module. Will check if the Terminal view
     * exists in the Terminal tool window tabs.
     *
     * @param libertyModule
     * @param project
     * @return TerminalView or null if it does not exist
     */
    // TerminalToolWindowTabsManager, TerminalToolWindowTab, TerminalView are @Experimental, but their usage is explicitly recommended by https://plugins.jetbrains.com/docs/intellij/embedded-terminal.html
    @SuppressWarnings("UnstableApiUsage")
    public static TerminalView getTerminalView(LibertyModule libertyModule, Project project) {
        TerminalView view = libertyModule.getTerminalView();
        if (view != null) {
            TerminalToolWindowTabsManager tabsManager = TerminalToolWindowTabsManager.getInstance(project);
            for (TerminalToolWindowTab tab : tabsManager.getTabs()) {
                if (view.equals(tab.getView())) {
                    return view;
                }
            }
        }
        // Tab is no longer present – clear stored TerminalView reference.
        libertyModule.setTerminalView(null);
        return null;
    }

    public static String includeEscapeToString(String path) {
        return "\"" + path + "\"";
    }
}
