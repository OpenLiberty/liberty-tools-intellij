/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 *  SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package io.openliberty.tools.intellij;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.util.BuildFile;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

/**
 * Represents a Liberty server module
 * (one entry in the Liberty tool window tree view)
 */
public class LibertyModule {
    private Project project;
    private VirtualFile buildFile;
    private String projectType;
    private String name;
    private boolean validContainerVersion;

    private String customStartParams;
    // FIXME not currently being used, need to enable runInContainer checkbox in LibertyRunConfiguration see https://github.com/OpenLiberty/liberty-tools-intellij/issues/160
    private boolean runInContainer;

    private boolean debugMode;
    private ShellTerminalWidget shellWidget;

    public LibertyModule(Project project) {
        this.project = project;
        this.customStartParams = "";
        this.runInContainer = false;
        this.debugMode = false;
        this.shellWidget = null;
    }

    public LibertyModule(Project project, VirtualFile buildFile, String name, String projectType, boolean validContainerVersion) {
        this(project);
        this.buildFile = buildFile;
        this.name = name;
        this.projectType = projectType;
        this.validContainerVersion = validContainerVersion;
    }

    public LibertyModule(Project project, BuildFile buildFile) {
        this(project);
        this.buildFile = buildFile.getBuildFile().getVirtualFile();
        this.name = buildFile.getProjectName();
        this.projectType = buildFile.getProjectType();
        this.validContainerVersion = buildFile.isValidContainerVersion();
    }

    public VirtualFile getBuildFile() {
        return buildFile;
    }

    public void setBuildFile(VirtualFile buildFile) {
        this.buildFile = buildFile;
    }

    public String getProjectType() {
        return projectType;
    }

    public void setProjectType(String projectType) {
        this.projectType = projectType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isValidContainerVersion() {
        return validContainerVersion;
    }

    public void setValidContainerVersion(boolean validContainerVersion) {
        this.validContainerVersion = validContainerVersion;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getCustomStartParams() {
        return customStartParams;
    }

    public void setCustomStartParams(String customStartParams) {
        if (customStartParams == null) {
            customStartParams = "";
        }
        this.customStartParams = customStartParams;
    }

    public boolean runInContainer() {
        return runInContainer;
    }

    public void setRunInContainer(boolean runInContainer) {
        this.runInContainer = runInContainer;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public ShellTerminalWidget getShellWidget() {
        return shellWidget;
    }

    public void setShellWidget(ShellTerminalWidget shellWidget) {
        this.shellWidget = shellWidget;
    }
}
