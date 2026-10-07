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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.util.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton to save the Liberty modules in the open project
 */
public class LibertyModules {
    private final static Logger LOGGER = Logger.getInstance(LibertyModules.class);

    private static LibertyModules instance = null;

    // key is build file associated with the Liberty project
    Map<VirtualFile, LibertyModule> libertyModules;

    /**
     * Cache of the last-known {@link LibertyModule.AppState} for each build-file path.
     * Used to restore state across re-scans when the Liberty process is still running.
     * Key: absolute NIO path string of the build file.
     */
    private final Map<String, LibertyModule.AppState> stateCache = new ConcurrentHashMap<>();

    private LibertyModules() {
        libertyModules = Collections.synchronizedMap(new HashMap<>());
    }

    public synchronized static LibertyModules getInstance() {
        if (instance == null) {
            instance = new LibertyModules();
        }
        return instance;
    }

    /**
     * Remove existing data and scan the project for the modules that are Liberty apps.
     * @return this singleton, the list will be empty if there are no Liberty modules
     */
    public LibertyModules scanLibertyModules(Project project) {
        synchronized (libertyModules) {
            removeForProject(project); // remove previous data, if any
            return rescanLibertyModules(project);
        }
    }

    /**
     * Scan the project for the modules that are Liberty apps and update any existing entries.
     * @return this singleton, the list will be empty if there are no Liberty modules
     */
    public LibertyModules rescanLibertyModules(Project project) {
        synchronized (libertyModules) {
            ArrayList<BuildFile> buildFiles = new ArrayList<>();
            try {
                buildFiles.addAll(LibertyProjectUtil.getMavenBuildFiles(project));
            } catch (IOException | SAXException | ParserConfigurationException e) {
                LOGGER.error("I/O error or error parsing Liberty Maven projects in workspace", e);
            }
            try { // search for Gradle files even if Maven files experience error
                buildFiles.addAll(LibertyProjectUtil.getGradleBuildFiles(project));
            } catch (IOException | SAXException | ParserConfigurationException e) {
                LOGGER.error("I/O error or error parsing Liberty Gradle projects in workspace", e);
            }

            for (BuildFile buildFile : buildFiles) {
                // create a new Liberty Module object for this project
                VirtualFile virtualFile = buildFile.getBuildFile();
                String projectName = null;
                if (virtualFile == null) {
                    LOGGER.error(String.format("Could not resolve current project %s", virtualFile));
                    break;
                }
                try {
                    if (buildFile.getProjectType().equals(Constants.ProjectType.LIBERTY_MAVEN_PROJECT)) {
                        projectName = LibertyMavenUtil.getProjectNameFromPom(virtualFile);
                    } else {
                        projectName = LibertyGradleUtil.getProjectName(virtualFile);
                    }
                } catch (Exception e) {
                    LOGGER.warn(String.format("Could not resolve project name from build file: %s", virtualFile), e);
                }
                if (projectName == null) {
                    if (virtualFile.getParent() != null) {
                        projectName = virtualFile.getParent().getName();
                    } else {
                        projectName = project.getName();
                    }
                }

                boolean validContainerVersion = buildFile.isValidContainerVersion();
                addLibertyModule(new LibertyModule(project, virtualFile, projectName, buildFile.getProjectType(), validContainerVersion));
            }

            // After all modules are registered, parse metadata and wire relationships.
            parseBuildMetadata(project);
            buildMultiModuleRelationships(project);
            populateStatesFromCache(project);
        }
        return this;
    }

    /**
     * Parses the build file of each Liberty module for the given project and stores
     * the result in {@link LibertyModule#setBuildMetadata(AbstractProjectMetadata)}.
     * Errors are logged but do not abort processing of remaining modules.
     *
     * @param project the IntelliJ project whose modules should be parsed
     */
    private void parseBuildMetadata(Project project) {
        for (LibertyModule module : getLibertyModules(project)) {
            if (!module.getProjectType().equals(Constants.ProjectType.LIBERTY_MAVEN_PROJECT)) {
                continue;
            }
            VirtualFile buildFile = module.getBuildFile();
            if (buildFile == null) continue;
            String buildFilePath = buildFile.toNioPath().toString();
            try {
                module.setBuildMetadata(new MavenProjectMetadata(buildFilePath));
            } catch (Exception e) {
                LOGGER.warn(String.format("Could not parse build metadata for: %s", buildFilePath), e);
            }
        }
    }

    /**
     * Wires parent/child relationships between Liberty modules for the given project
     * based on the {@link AbstractProjectMetadata#getParentProjectName()} declared in
     * each module's build metadata.
     *
     * <p>For each module whose metadata reports a parent project name, the method
     * searches for a module in the same project whose {@code projectName} matches.
     * When a match is found:
     * <ul>
     *   <li>the child's {@link LibertyModule#setParentModule(LibertyModule)} is set, and</li>
     *   <li>the parent's {@link LibertyModule#addChildLibertyModule(LibertyModule)} is called.</li>
     * </ul>
     *
     * @param project the IntelliJ project whose modules should be linked
     */
    private void buildMultiModuleRelationships(Project project) {
        List<LibertyModule> modules = getLibertyModules(project);

        // Build a lookup: projectName → LibertyModule (skip null names)
        Map<String, LibertyModule> byName = new HashMap<>();
        for (LibertyModule module : modules) {
            AbstractProjectMetadata meta = module.getBuildMetadata();
            if (meta != null && meta.getProjectName() != null) {
                byName.put(meta.getProjectName(), module);
            }
        }

        // Wire relationships
        for (LibertyModule child : modules) {
            AbstractProjectMetadata meta = child.getBuildMetadata();
            if (meta == null || meta.getParentProjectName() == null) continue;
            LibertyModule parent = byName.get(meta.getParentProjectName());
            if (parent == null || parent == child) continue;
            child.setParentModule(parent);
            parent.addChildLibertyModule(child);
        }
    }

    /**
     * Saves the current {@link LibertyModule.AppState} of every module in the
     * given project to the in-memory state cache.
     *
     * @param project the IntelliJ project whose module states should be cached
     */
    public void cacheState(Project project) {
        for (LibertyModule module : getLibertyModules(project)) {
            VirtualFile buildFile = module.getBuildFile();
            if (buildFile != null) {
                stateCache.put(buildFile.toNioPath().toString(), module.getAppState());
            }
        }
    }

    /**
     * Restores the {@link LibertyModule.AppState} of each module in the given
     * project from the in-memory state cache (populated by {@link #cacheState(Project)}).
     * Only states that differ from the default {@link LibertyModule.AppState#STOPPED} are
     * restored so that newly added modules start with the correct default.
     *
     * @param project the IntelliJ project whose module states should be restored
     */
    private void populateStatesFromCache(Project project) {
        for (LibertyModule module : getLibertyModules(project)) {
            VirtualFile buildFile = module.getBuildFile();
            if (buildFile == null) continue;
            LibertyModule.AppState cached = stateCache.get(buildFile.toNioPath().toString());
            if (cached != null && cached != LibertyModule.AppState.STOPPED) {
                module.setAppState(cached);
            }
        }
    }

    /**
     * Add tracked Liberty project to workspace, update project,
     * projectType, name and validContainerVersion if already tracked.
     *
     * @param module LibertyModule
     */
    public LibertyModule addLibertyModule(LibertyModule module) {
        synchronized (libertyModules) {
            if (libertyModules.containsKey(module.getBuildFile())) {
                // Update existing Liberty project, projectType module, name and validContainerVersion.
                // Do not update the build file (key), debugMode, shellWidget or customStartParams since
                // they may modify saved run configs.
                // Clear multi-module relationships so buildMultiModuleRelationships() re-wires them cleanly.
                LibertyModule existing = libertyModules.get(module.getBuildFile());
                existing.setProject(module.getProject());
                existing.setProjectType(module.getProjectType());
                existing.setName(module.getName());
                existing.setValidContainerVersion(module.isValidContainerVersion());
                existing.clearMultiModuleRelationships();
            } else {
                libertyModules.put(module.getBuildFile(), module);
            }
            return libertyModules.get(module.getBuildFile());
        }
    }

    /**
     * Get a Liberty module associated with the corresponding build file
     *
     * @param buildFile build file
     * @return LibertyModule
     */
    public LibertyModule getLibertyModule(VirtualFile buildFile) {
        return libertyModules.get(buildFile);
    }

    /**
     * Returns the Liberty project associated with a build file path string
     *
     * @param buildFile String, path to build file
     * @return LibertyModule
     */
    public LibertyModule getLibertyProjectFromString(String buildFile) {
        VirtualFile vBuildFile = VfsUtil.findFile(Paths.get(buildFile), true);
        return libertyModules.get(vBuildFile);
    }

    /**
     * Returns all build files as a list of strings associated with the Liberty project.
     * Used for Liberty run configuration
     *
     * @param project
     * @return List<String> Liberty project build files as strings
     */
    public List<String> getLibertyBuildFilesAsString(Project project) {
        List<String> sBuildFiles = new ArrayList<>();
        synchronized (libertyModules) {
            libertyModules.values().forEach(libertyModule -> {
                if (project.equals(libertyModule.getProject())) {
                    // need to convert to NioPath for OS specific paths
                    sBuildFiles.add(libertyModule.getBuildFile().toNioPath().toString());
                }
            });
        }
        return sBuildFiles;
    }

    /**
     * Returns all Liberty modules for the given project
     *
     * @param project
     * @return Liberty modules for the given project
     */
    public List<LibertyModule> getLibertyModules(Project project) {
        ArrayList<LibertyModule> supportedLibertyModules = new ArrayList<>();
        synchronized (libertyModules) {
            libertyModules.values().forEach(libertyModule -> {
                if (project.equals(libertyModule.getProject())) {
                    supportedLibertyModules.add(libertyModule);
                }
            });
        }
        return supportedLibertyModules;
    }

    /**
     * Returns all Liberty modules with the supported project type(s) for the given project
     * ex. all Liberty Maven projects
     *
     * @param project
     * @param projectTypes
     * @return Liberty modules with the given project type(s)
     */
    public List<LibertyModule> getLibertyModules(Project project, List<Constants.ProjectType> projectTypes) {
        ArrayList<LibertyModule> supportedLibertyModules = new ArrayList<>();
        synchronized (libertyModules) {
            libertyModules.values().forEach(libertyModule -> {
                if (project.equals(libertyModule.getProject()) && projectTypes.contains(libertyModule.getProjectType())) {
                    supportedLibertyModules.add(libertyModule);
                }
            });
        }
        return supportedLibertyModules;
    }

    /**
     * Remove the given Liberty module
     *
     * @param libertyModule
     */
    public void removeLibertyModule(LibertyModule libertyModule) {
        libertyModules.remove(libertyModule.getBuildFile());
    }

    /**
     * Remove all stored Liberty modules for the given project that
     * do not have active terminal widgets (running commands)
     *
     * @param project
     */
    public void removeForProject(Project project) {
        synchronized(libertyModules) {
            Iterator it = libertyModules.values().iterator();
            while (it.hasNext()) {
                LibertyModule libertyModule = (LibertyModule) it.next();
                // do not remove from list if the corresponding terminal widget has running commands
                if (project.equals(libertyModule.getProject()) && !(libertyModule.getShellWidget() != null && libertyModule.getShellWidget().hasRunningCommands())) {
                    // Evict the state cache entry so it does not leak
                    VirtualFile buildFile = libertyModule.getBuildFile();
                    if (buildFile != null) {
                        stateCache.remove(buildFile.toNioPath().toString());
                    }
                    it.remove();
                }
            }
        }
    }
}
