/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for Maven and Gradle project metadata.
 *
 * <p>Holds the common fields shared by {@link MavenProjectMetadata} and
 * {@link GradleProjectMetadata} and provides the identical getter implementations
 * so subclasses only need to populate the fields during their own parsing phase.</p>
 */
public abstract class AbstractProjectMetadata {

    protected String projectName;
    protected String parentProjectName;
    protected List<String> subprojects;
    protected List<String> projectDependencies;
    protected boolean hasLibertyPlugin;
    protected boolean isModuleDisabled;
    protected boolean isAggregator;
    protected final String buildFilePath;

    protected AbstractProjectMetadata(String buildFilePath) {
        this.buildFilePath = buildFilePath;
    }

    // -------------------------------------------------------------------------
    // Common getters
    // -------------------------------------------------------------------------

    public String getProjectName() {
        return projectName;
    }

    public String getParentProjectName() {
        return parentProjectName;
    }

    public List<String> getSubprojects() {
        return subprojects != null ? subprojects : new ArrayList<>();
    }

    public boolean isLibertyPluginConfigured() {
        return hasLibertyPlugin;
    }

    public boolean isAggregator() {
        return isAggregator;
    }

    public String getBuildFilePath() {
        return buildFilePath;
    }

    public boolean isModuleDisabled() {
        return isModuleDisabled;
    }

    public List<String> getProjectDependencies() {
        return projectDependencies != null ? projectDependencies : new ArrayList<>();
    }
}
