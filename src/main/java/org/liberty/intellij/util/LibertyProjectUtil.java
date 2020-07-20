package org.liberty.intellij.util;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.terminal.JBTerminalWidget;
import com.intellij.ui.content.Content;
import com.sun.istack.Nullable;
import org.jetbrains.plugins.terminal.AbstractTerminalRunner;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;
import org.jetbrains.plugins.terminal.TerminalTabState;
import org.jetbrains.plugins.terminal.TerminalView;

import java.util.ArrayList;
import java.util.Objects;

public class LibertyProjectUtil {

    @Nullable
    public static Project getProject(DataContext context) {
        return CommonDataKeys.PROJECT.getData(context);
    }

    /**
     * Returns a list of valid Gradle build files in the project
     * @param project
     * @return ArrayList of PsiFiles
     */
    public static ArrayList<PsiFile> getGradleBuildFiles(Project project) {
        return getBuildFiles(project, Constants.LIBERTY_GRADLE_PROJECT);
    }

    /**
     * Returns a list of valid Maven build files in the project
     * @param project
     * @return ArrayList of PsiFiles
     */
    public static ArrayList<PsiFile> getMavenBuildFiles(Project project) {
        return getBuildFiles(project, Constants.LIBERTY_MAVEN_PROJECT);
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
            AbstractTerminalRunner terminalRunner = terminalView.getTerminalRunner();
            TerminalTabState tabState = new TerminalTabState();
            tabState.myTabName = projectName;
            tabState.myWorkingDirectory = project.getBasePath();
            terminalView.createNewSession(terminalRunner, tabState);
            return getTerminalWidget(terminalWindow, projectName);
        }
        return null;
    }

    // returns valid build files for the current project
    private static ArrayList<PsiFile> getBuildFiles(Project project, String buildFileType) {
        ArrayList<PsiFile> buildFiles = new ArrayList<PsiFile>();

        if (buildFileType.equals(Constants.LIBERTY_MAVEN_PROJECT)) {
            PsiFile[] mavenFiles = FilenameIndex.getFilesByName(project, "pom.xml", GlobalSearchScope.projectScope(project));
            for (int i = 0; i < mavenFiles.length; i++) {
                System.out.println(mavenFiles[i].getVirtualFile().getPath());
                try {
                    if (LibertyMavenUtil.validPom(mavenFiles[i])) {
                        buildFiles.add(mavenFiles[i]);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error parsing pom.xml");
                }
            }
        } else if (buildFileType.equals(Constants.LIBERTY_GRADLE_PROJECT)) {
            PsiFile[] gradleFiles = FilenameIndex.getFilesByName(project, "build.gradle", GlobalSearchScope.projectScope(project));
            for (int i = 0; i < gradleFiles.length; i++) {
                System.out.println(gradleFiles[i].getVirtualFile().getPath());
                try {
                    if (LibertyGradleUtil.validBuildGradle(gradleFiles[i])) {
                        buildFiles.add(gradleFiles[i]);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error parsing build.gradle");
                }
            }
        }
        return buildFiles;
    }

    private static ShellTerminalWidget getTerminalWidget(ToolWindow terminalWindow, String projectName) {
        Content[] terminalContents = terminalWindow.getContentManager().getContents();
        for (int i = 0; i < terminalContents.length; i++) {
            if (terminalContents[i].getTabName().equals(projectName)) {
                JBTerminalWidget widget = TerminalView.getWidgetByContent(terminalContents[i]);
                ShellTerminalWidget shellWidget = (ShellTerminalWidget) Objects.requireNonNull(widget);
                return shellWidget;
            }
        }
        return null;
    }

}
