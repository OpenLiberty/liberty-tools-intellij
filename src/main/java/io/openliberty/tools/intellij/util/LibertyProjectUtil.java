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
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.terminal.frontend.view.TerminalView;
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTab;
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager;
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

// TerminalView and related Reworked Terminal APIs are marked @Experimental by JetBrains, but their
// use is explicitly recommended over the Classic Terminal APIs (see https://youtrack.jetbrains.com/issue/IJPL-252504).
@SuppressWarnings("UnstableApiUsage")
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
     * Creates a new terminal tab for the given module if {@code createWidget} is {@code true} and
     * no existing widget/view is present. Returns {@code true} if a usable terminal is available
     * after the call (either pre-existing or just created).
     *
     * <p>Uses the Reworked Terminal API ({@link TerminalToolWindowTabsManager}) exclusively.
     * The resulting {@link TerminalView} is stored on the module and is the sole interface used
     * for all subsequent operations. {@link TerminalWidget} is left {@code null} for Reworked tabs
     * because {@code TerminalToolWindowManager.findWidgetByContent()} always returns {@code null}
     * for Reworked Terminal tabs — they do not store {@code TERMINAL_WIDGET_KEY} on their Content.
     *
     * @return {@code true} if the module has an active terminal (existing widget, existing view,
     *         or a freshly created tab); {@code false} otherwise.
     */
    public static boolean ensureTerminalTab(Project project, LibertyModule libertyModule, boolean createWidget,
                                            TerminalWidget existingWidget) {
        // An existing Classic widget counts as a live terminal.
        if (existingWidget != null) {
            return true;
        }
        // A previously created Reworked Terminal view counts — unless it has been terminated
        // (e.g. the user closed the tab). Check the session state to detect stale views.
        TerminalView storedView = libertyModule.getTerminalView();
        if (storedView != null) {
            TerminalToolWindowTabsManager tabsManager = TerminalToolWindowTabsManager.getInstance(project);
            boolean tabStillOpen = tabsManager.getTabs().stream()
                    .anyMatch(tab -> tab.getView().equals(storedView));
            if (tabStillOpen) {
                return true;
            }
            // Tab was closed — clear the stale references so a new tab can be created.
            libertyModule.setTerminalView(null);
            libertyModule.setTerminalWidget(null);
        }
        if (!createWidget) {
            return false;
        }
        // Create a new Reworked Terminal tab.
        TerminalToolWindowTabsManager tabsManager = TerminalToolWindowTabsManager.getInstance(project);
        TerminalToolWindowTab tab = tabsManager.createTabBuilder()
                .workingDirectory(project.getBasePath())
                .tabName(libertyModule.getName())
                .requestFocus(true)
                .createTab();
        // Store the TerminalView for all subsequent operations (session state, sendText).
        // Do NOT call TerminalToolWindowManager.findWidgetByContent(tab.getContent()) —
        // Reworked Terminal tabs never set TERMINAL_WIDGET_KEY on their Content, so that
        // call always returns null and would incorrectly signal failure.
        libertyModule.setTerminalView(tab.getView());
        libertyModule.setTerminalWidget(null);
        return true;
    }

    /**
     * Brings the terminal tab associated with the given Liberty module into focus.
     *
     * <p>For Reworked Terminal tabs (where {@code existingWidget} is {@code null}),
     * matches the stored {@link TerminalView} against {@link TerminalToolWindowTabsManager#getTabs()}
     * and selects the corresponding {@link Content} directly.
     *
     * <p>For Classic Terminal tabs, falls back to matching by {@code TERMINAL_WIDGET_KEY}.
     */
    public static void setFocusToModule(Project project, LibertyModule libertyModule, TerminalWidget existingWidget) {
        TerminalToolWindowManager manager = TerminalToolWindowManager.getInstance(project);
        ToolWindow toolWindow = manager.getToolWindow();
        if (toolWindow == null) return;

        ContentManager contentManager = toolWindow.getContentManager();

        // Reworked Terminal path: match by TerminalView → Content via tabsManager.
        TerminalView view = libertyModule.getTerminalView();
        if (view != null) {
            TerminalToolWindowTabsManager tabsManager = TerminalToolWindowTabsManager.getInstance(project);
            for (TerminalToolWindowTab tab : tabsManager.getTabs()) {
                if (tab.getView().equals(view)) {
                    Content content = tab.getContent();
                    contentManager.setSelectedContent(content);
                    content.getComponent().requestFocus();
                    return;
                }
            }
            return; // tab not found — nothing to focus
        }

        // Classic Terminal path: match by TERMINAL_WIDGET_KEY.
        if (existingWidget == null) return;
        Content[] contents = contentManager.getContents();
        int index = 0;
        for (int i = 0; i < contents.length; i++) {
            if (existingWidget.equals(TerminalToolWindowManager.findWidgetByContent(contents[i]))) {
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
        if (widget == null) {
            // No Classic widget stored — this module uses a Reworked Terminal tab (or has no tab yet).
            // Do NOT clear terminalView here; its liveness is validated in ensureTerminalTab().
            return null;
        }
        // Check if the Classic widget still exists in the terminal view.
        for (TerminalWidget terminalWidget : terminalToolWindowManager.getTerminalWidgets()) {
            if (widget.equals(terminalWidget)) {
                return widget;
            }
        }
        // Classic widget is gone — clear both references so a new tab can be created.
        libertyModule.setTerminalWidget(null);
        libertyModule.setTerminalView(null);
        return null;
    }

    public static String includeEscapeToString(String path) {
        return "\"" + path + "\"";
    }
}
