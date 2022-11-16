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

    public LibertyModule(Project project, VirtualFile buildFile, String name, String projectType, boolean validContainerVersion) {
        this.project = project;
        this.buildFile = buildFile;
        this.name = name;
        this.projectType = projectType;
        this.validContainerVersion = validContainerVersion;
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
}
