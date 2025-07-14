/*******************************************************************************
 * Copyright (c) 2022, 2025 IBM Corporation.
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
import io.openliberty.tools.intellij.runConfiguration.LibertyRunConfiguration;
import io.openliberty.tools.intellij.util.BuildFile;
import io.openliberty.tools.intellij.util.Constants;
import com.intellij.terminal.ui.TerminalWidget;

/**
 * Represents a Liberty server module
 * (one entry in the Liberty tool window tree view)
 */
public class LibertyModule {
    private Project project;
    private VirtualFile buildFile;
    private Constants.ProjectType projectType;
    private String name;
    private boolean validContainerVersion;
    private boolean debugMode;
    private TerminalWidget terminalWidget;
    private LibertyRunConfiguration customRunConfig;
    private boolean useCustom;

    public LibertyModule(Project project) {
        this.project = project;
        this.debugMode = false;
        this.terminalWidget = null;
        this.customRunConfig = null;
        this.useCustom = false;
    }

    public LibertyModule(Project project, VirtualFile buildFile, String name, Constants.ProjectType projectType, boolean validContainerVersion) {
        this(project);
        this.buildFile = buildFile;
        this.name = name;
        this.projectType = projectType;
        this.validContainerVersion = validContainerVersion;
    }

    public LibertyModule(Project project, BuildFile buildFile) {
        this(project);
        this.buildFile = buildFile.getBuildFile();
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

    public Constants.ProjectType getProjectType() {
        return projectType;
    }

    public void setProjectType(Constants.ProjectType projectType) {
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

    public LibertyRunConfiguration getCustomRunConfig() {
        return customRunConfig;
    }

    public void setCustomRunConfig(LibertyRunConfiguration newCustomRunConfig) {
        customRunConfig = newCustomRunConfig;
    }

    public String getCustomStartParams() {
        if (customRunConfig == null || customRunConfig.getParams() == null) {
            return "";
        }
        return customRunConfig.getParams();
    }

    public boolean isCustom() {
        return useCustom;
    }

    public void setUseCustom(boolean isCustom) {
        useCustom = isCustom;
    }

    public boolean runInContainer() {
        if (customRunConfig == null) {
            return false;
        }
        return customRunConfig.runInContainer();
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public TerminalWidget getTerminalWidget() {
        return terminalWidget;
    }

    public void setTerminalWidget(TerminalWidget shellWidget) {
        this.terminalWidget = shellWidget;
    }
}
