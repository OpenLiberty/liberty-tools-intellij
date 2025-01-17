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

import com.intellij.openapi.vfs.VirtualFile;

/**
 * Defines a BuildFile object
 */
public class BuildFile {
    public VirtualFile buildFile;
    public boolean validBuildFile;
    public boolean validContainerVersion;

    private String projectName;

    public Constants.ProjectType getProjectType() {
        return projectType;
    }

    /**
     * Liberty project type must be Gradle or Maven.
     * @param projectType
     */
    public void setProjectType(Constants.ProjectType projectType) {
        this.projectType = projectType;
    }

    private Constants.ProjectType projectType;

    public BuildFile(boolean validBuildFile, boolean validContainerVersion) {
        this.validBuildFile = validBuildFile;
        this.validContainerVersion = validContainerVersion;
        this.buildFile = null;
    }

    public VirtualFile getBuildFile() { return this.buildFile; }

    public void setBuildFile(VirtualFile buildFile) {
        this.buildFile = buildFile;
    }

    public boolean isValidBuildFile() {
        return this.validBuildFile;
    }

    public boolean isValidContainerVersion() {
        return this.validContainerVersion;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}
