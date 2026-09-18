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
 * Common base class for build-tool metadata extracted from a project's build file.
 *
 * <p>This class is intentionally kept free of any build-tool specifics so that
 * the multi-module relationship model in
 * {@link io.openliberty.tools.intellij.LibertyModules} can treat Maven and
 * Gradle projects uniformly.</p>
 */
public abstract class AbstractProjectMetadata {

    /** Absolute path to the build file that was parsed. */
    protected final String buildFilePath;

    /**
     * The name that identifies this project.
     */
    protected String projectName;

    /**
     * The name of the parent/aggregator project, as declared in this module's
     * own build file.
     */
    protected String parentProjectName;

    /**
     * Relative paths (or names) of the direct child modules declared by this
     * project (Maven {@code <modules>}, Gradle {@code include(...)}).
     * Empty list for leaf/standalone projects.
     */
    protected List<String> subprojects = new ArrayList<>();

    /**
     * Artifact IDs / project-path segments of inter-project dependencies
     * declared in this module's build file.
     */
    protected List<String> projectDependencies = new ArrayList<>();

    /** {@code true} when the Liberty Maven/Gradle plugin is configured for this project. */
    protected boolean hasLibertyPlugin;

    /**
     * {@code true} when this project has been explicitly configured to skip
     * Liberty dev mode (e.g. Maven {@code <skip>true</skip>} in the Liberty
     * plugin configuration).
     */
    protected boolean isModuleDisabled;

    /**
     * {@code true} when this project is an aggregator that declares child modules
     * (Maven: {@code packaging=pom} + at least one {@code <module>};
     * Gradle: at least one {@code include(...)}).
     */
    protected boolean isAggregator;

    protected AbstractProjectMetadata(String buildFilePath) {
        this.buildFilePath = buildFilePath;
    }

    /** Returns the absolute path to the build file that was parsed. */
    public String getBuildFilePath() {
        return buildFilePath;
    }

    /**
     * Returns the project name ({@code artifactId} / {@code rootProject.name}),
     * or {@code null} when it could not be determined.
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Returns the name of the parent/aggregator project as declared in this
     * module's build file, or {@code null} for standalone/root modules.
     */
    public String getParentProjectName() {
        return parentProjectName;
    }

    /**
     * Returns the relative paths (or names) of direct child modules declared
     * by this project.  Never {@code null}; returns an empty list for leaf projects.
     */
    public List<String> getSubprojects() {
        return subprojects;
    }

    /**
     * Returns artifact IDs / project-path segments of inter-project dependencies
     * declared in this module's build file.  Never {@code null}.
     */
    public List<String> getProjectDependencies() {
        return projectDependencies != null ? projectDependencies : new ArrayList<>();
    }

    /** Returns {@code true} when the Liberty Maven/Gradle plugin is configured. */
    public boolean isLibertyPluginConfigured() {
        return hasLibertyPlugin;
    }

    /**
     * Returns {@code true} when Liberty dev mode has been explicitly disabled
     * for this module (e.g. {@code <skip>true</skip>}).
     */
    public boolean isModuleDisabled() {
        return isModuleDisabled;
    }

    /**
     * Returns {@code true} when this project is a multi-module aggregator that
     * declares child modules.
     */
    public boolean isAggregator() {
        return isAggregator;
    }
}
