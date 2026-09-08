/*******************************************************************************
 * Copyright (c) 2022, 2026 IBM Corporation.
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
import io.openliberty.tools.intellij.util.AbstractProjectMetadata;
import org.jetbrains.plugins.terminal.ShellTerminalWidget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a Liberty server module (one entry in the Liberty tool window tree view).
 *
 * <p>In a multi-module build a module may be either:</p>
 * <ul>
 *   <li>A <b>leaf</b> module – a concrete Liberty application with a build file that
 *       configures the Liberty Maven/Gradle plugin.</li>
 *   <li>An <b>aggregator</b> – a parent POM / Gradle root project whose child modules
 *       are the real Liberty applications.  {@link #isParentOfLibertyModule()} returns
 *       {@code true} for these, and {@link #getChildLibertyModules()} lists their
 *       children.</li>
 * </ul>
 */
public class LibertyModule {

    /**
     * Lifecycle states for a Liberty module's dev-mode process.
     * The parent module's visual state is derived dynamically from its children.
     */
    public enum AppState {
        /** Dev mode has started but Liberty has not yet reported ready. */
        STARTING,
        /** Liberty has reported the application is fully started. */
        RUNNING,
        /** A stop has been requested but Liberty has not yet confirmed shutdown. */
        STOPPING,
        /** Dev mode is not running (initial/default state). */
        STOPPED
    }

    private Project project;
    private VirtualFile buildFile;
    private Constants.ProjectType projectType;
    private String name;
    private boolean validContainerVersion;
    private boolean debugMode;
    private ShellTerminalWidget shellWidget;
    private LibertyRunConfiguration customRunConfig;
    private boolean useCustom;

    // -- Multi-module fields --

    /** The metadata extracted from this module's build file, populated during workspace scan. */
    private AbstractProjectMetadata buildMetadata;

    /** The parent aggregator module, or {@code null} for standalone / root modules. */
    private LibertyModule parentModule;

    /** The set of direct child Liberty modules (leaf modules) under this aggregator. */
    private final Set<LibertyModule> childModules = ConcurrentHashMap.newKeySet();

    /**
     * Current dev-mode lifecycle state. Volatile so that changes made on background
     * threads are immediately visible to the UI thread.
     */
    private volatile AppState appState = AppState.STOPPED;

    public LibertyModule(Project project) {
        this.project = project;
        this.debugMode = false;
        this.shellWidget = null;
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

    public ShellTerminalWidget getShellWidget() {
        return shellWidget;
    }

    public void setShellWidget(ShellTerminalWidget shellWidget) {
        this.shellWidget = shellWidget;
    }

    // -------------------------------------------------------------------------
    // Multi-module accessors
    // -------------------------------------------------------------------------

    /** Returns the metadata extracted from this module's build file. */
    public AbstractProjectMetadata getBuildMetadata() {
        return buildMetadata;
    }

    /** Stores the metadata extracted from this module's build file. */
    public void setBuildMetadata(AbstractProjectMetadata buildMetadata) {
        this.buildMetadata = buildMetadata;
    }

    /** Returns the parent aggregator module, or {@code null} when this is a root/standalone module. */
    public LibertyModule getParentModule() {
        return parentModule;
    }

    /** Sets the parent aggregator module. */
    public void setParentModule(LibertyModule parentModule) {
        this.parentModule = parentModule;
    }

    /**
     * Returns {@code true} when this module is a parent/aggregator whose child
     * modules are the real Liberty leaf applications.
     */
    public boolean isParentOfLibertyModule() {
        return !childModules.isEmpty();
    }

    /**
     * Adds a child Liberty module to this aggregator.
     * Has no effect when {@code child} is already registered.
     */
    public void addChildLibertyModule(LibertyModule child) {
        childModules.add(child);
    }

    /**
     * Returns an unmodifiable snapshot of the direct child Liberty modules
     * registered under this aggregator.
     */
    public List<LibertyModule> getChildLibertyModules() {
        return Collections.unmodifiableList(new ArrayList<>(childModules));
    }

    /** Returns the current dev-mode lifecycle state of this module. */
    public AppState getAppState() {
        return appState;
    }

    /** Updates the dev-mode lifecycle state of this module. */
    public void setAppState(AppState appState) {
        this.appState = appState;
    }
}
