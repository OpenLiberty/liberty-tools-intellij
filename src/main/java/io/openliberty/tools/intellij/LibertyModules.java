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
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton that tracks all Liberty modules discovered in the open IntelliJ project.
 *
 * <p>After the initial flat scan, {@link #rescanLibertyModules(Project)} performs a second
 * pass to link parent/aggregator modules to their child Liberty leaf modules, enabling
 * multi-module build support.</p>
 */
public class LibertyModules {
    private final static Logger LOGGER = Logger.getInstance(LibertyModules.class);

    private static LibertyModules instance = null;

    // key is build file associated with the Liberty project
    Map<VirtualFile, LibertyModule> libertyModules;

    /**
     * Cache of the last known app state for each module, keyed by module name.
     * Entries are removed when the state is STOPPED (the default initial state),
     * so only non-default states are stored. This allows freshly constructed
     * LibertyModule instances to be re-stamped with the correct state after a
     * dashboard refresh that rebuilds all modules from scratch.
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
     * After updating all module entries, performs a second pass to link parent aggregator
     * modules to their Liberty leaf child modules.
     *
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

            // Attach per-module build metadata and link parent → child relationships.
            buildMultiModuleRelationships(project);

            // Re-stamp app states from the cache onto the newly created module instances
            // so that a dashboard refresh does not lose any in-progress states.
            populateStatesFromCache(project);
        }
        return this;
    }

    /**
     * Parses each module's build file to extract multi-module metadata, then links
     * aggregator (parent) modules to their Liberty leaf child modules.
     *
     * <ol>
     *   <li>Parse every module's build file and store {@link AbstractProjectMetadata}.</li>
     *   <li>For each module that declares a parent, wire the relationship; for each
     *       aggregator that declares children, wire the reverse.</li>
     * </ol>
     */
    private void buildMultiModuleRelationships(Project project) {
        // Snapshot the module list outside the synchronized block to avoid re-entrant locking.
        // getLibertyModules() acquires synchronized(libertyModules); we are called from within
        // rescanLibertyModules which already holds that lock. Take a snapshot of the values directly.
        List<LibertyModule> modules = new ArrayList<>(libertyModules.values());
        modules.removeIf(m -> !project.equals(m.getProject()));

        // Parse build metadata for every module
        // Index by BOTH the display name (set by the scan) AND the build-file artifactId/rootProject.name
        // (from metadata) to cover the case where the two differ.
        Map<String, LibertyModule> byName = new HashMap<>();
        Map<String, LibertyModule> byLocation = new HashMap<>();

        for (LibertyModule module : modules) {
            VirtualFile buildFile = module.getBuildFile();
            if (buildFile == null) continue;

            AbstractProjectMetadata metadata = parseBuildMetadata(module);
            if (metadata != null) {
                module.setBuildMetadata(metadata);
                // Index by the authoritative name from the build file.
                if (metadata.getProjectName() != null) {
                    byName.put(metadata.getProjectName(), module);
                }
            }
            // Also index by the display name used in the UI (may be a fallback directory name).
            byName.putIfAbsent(module.getName(), module);

            String location = buildFile.getParent() != null
                    ? buildFile.getParent().getPath() : null;
            if (location != null) {
                byLocation.put(location, module);
            }
        }

        // Link parent <-> child relationships
        for (LibertyModule module : modules) {
            AbstractProjectMetadata metadata = module.getBuildMetadata();
            if (metadata == null) continue;

            // Case A: child declares its parent (Maven <parent>/<artifactId>, Gradle parent detection).
            // parentProjectName is the aggregator's artifactId / root project name.
            String parentProjectName = metadata.getParentProjectName();
            if (parentProjectName != null) {
                LibertyModule parentModule = byName.get(parentProjectName);
                if (parentModule != null && module.getParentModule() == null) {
                    module.setParentModule(parentModule);
                    parentModule.addChildLibertyModule(module);
                }
                // Don't 'continue' here — also try Case B so that an aggregator that is
                // itself a child of another aggregator still processes its own subprojects.
            }

            // Case B: aggregator declares its children (Maven <modules>, Gradle include).
            if (metadata.isAggregator()) {
                String parentLocation = module.getBuildFile().getParent() != null
                        ? module.getBuildFile().getParent().getPath() : null;
                if (parentLocation == null) continue;

                for (String subprojectPath : metadata.getSubprojects()) {
                    try {
                        Path resolved = Paths.get(parentLocation, subprojectPath).normalize().toAbsolutePath();
                        String resolvedPath = resolved.toString();
                        // Also try forward-slash version in case VirtualFile.getPath() uses forward slashes
                        LibertyModule childModule = byLocation.get(resolvedPath);
                        if (childModule == null) {
                            childModule = byLocation.get(resolvedPath.replace('\\', '/'));
                        }
                        if (childModule != null && childModule.getParentModule() == null) {
                            childModule.setParentModule(module);
                            module.addChildLibertyModule(childModule);
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to resolve subproject path: " + subprojectPath
                                + " for parent: " + parentLocation, e);
                    }
                }
            }
        }

        // Handle the case where children have NO <parent> declaration and the root aggregator
        // pom.xml does NOT have the Liberty plugin (so it was never scanned into libertyModules).
        // For every Liberty module still without a parent, look one directory level up for a
        // pom.xml or settings.gradle that lists the module's directory as a submodule.
        // A synthetic aggregator LibertyModule is created on demand (one per unique root location)
        // and registered into libertyModules so it becomes a tree root node.
        Map<String, LibertyModule> syntheticAggregators = new HashMap<>();

        for (LibertyModule module : modules) {
            if (module.getParentModule() != null) continue; // already linked

            VirtualFile buildFile = module.getBuildFile();
            if (buildFile == null || buildFile.getParent() == null) continue;

            String moduleDir = buildFile.getParent().getPath();
            String moduleDirName = buildFile.getParent().getName();
            File parentDir = new File(moduleDir).getParentFile();
            if (parentDir == null) continue;

            String parentDirPath = parentDir.getPath();

            // Check for Maven aggregator
            File parentPom = new File(parentDir, "pom.xml");
            if (parentPom.exists()) {
                try {
                    MavenProjectMetadata parentMeta = new MavenProjectMetadata(parentPom.getPath());
                    if (parentMeta.isAggregator() && parentMeta.getSubprojects().contains(moduleDirName)) {
                        // Get or create the synthetic aggregator module for this root pom
                        LibertyModule aggregator = syntheticAggregators.get(parentDirPath);
                        if (aggregator == null) {
                            // Also check if it was already added via a normal scan
                            aggregator = byLocation.get(parentDirPath);
                        }
                        if (aggregator == null) {
                            // Create a synthetic aggregator — no Liberty plugin, just structural
                            String aggName = parentMeta.getProjectName() != null
                                    ? parentMeta.getProjectName()
                                    : parentDir.getName();
                            VirtualFile parentVFile = VfsUtil
                                    .findFileByIoFile(parentPom, true);
                            if (parentVFile != null) {
                                aggregator = new LibertyModule(project, parentVFile, aggName,
                                        module.getProjectType(), false);
                                aggregator.setBuildMetadata(parentMeta);
                                syntheticAggregators.put(parentDirPath, aggregator);
                                // Register into the live map so the tree builder sees it
                                libertyModules.put(parentVFile, aggregator);
                                byLocation.put(parentDirPath, aggregator);
                                byName.putIfAbsent(aggName, aggregator);
                            }
                        }
                        if (aggregator != null && module.getParentModule() == null) {
                            module.setParentModule(aggregator);
                            aggregator.addChildLibertyModule(module);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse potential parent pom at: " + parentPom.getPath(), e);
                }
                continue; // Maven project — no need to check Gradle
            }

            // Check for Gradle aggregator (settings file in parent dir)
            Path parentSettingsFile = GradleProjectMetadata.findSettingsFile(
                    Paths.get(parentDirPath));
            if (parentSettingsFile != null) {
                try {
                    GradleProjectMetadata parentMeta = new GradleProjectMetadata(
                            GradleProjectMetadata.findBuildFile(Paths.get(parentDirPath)) != null
                                    ? GradleProjectMetadata.findBuildFile(Paths.get(parentDirPath)).toString()
                                    : null,
                            parentSettingsFile.toString());
                    if (parentMeta.isAggregator() && parentMeta.getSubprojects().contains(moduleDirName)) {
                        LibertyModule aggregator = syntheticAggregators.get(parentDirPath);
                        if (aggregator == null) {
                            aggregator = byLocation.get(parentDirPath);
                        }
                        if (aggregator == null) {
                            String aggName = parentMeta.getProjectName() != null
                                    ? parentMeta.getProjectName()
                                    : parentDir.getName();
                            Path parentBuildFilePath = GradleProjectMetadata
                                    .findBuildFile(Paths.get(parentDirPath));
                            File parentBuildFile = parentBuildFilePath != null
                                    ? parentBuildFilePath.toFile()
                                    : new File(parentDir, "settings.gradle");
                            VirtualFile parentVFile = VfsUtil
                                    .findFileByIoFile(parentBuildFile, true);
                            if (parentVFile != null) {
                                aggregator = new LibertyModule(project, parentVFile, aggName,
                                        module.getProjectType(), false);
                                aggregator.setBuildMetadata(parentMeta);
                                syntheticAggregators.put(parentDirPath, aggregator);
                                libertyModules.put(parentVFile, aggregator);
                                byLocation.put(parentDirPath, aggregator);
                                byName.putIfAbsent(aggName, aggregator);
                            }
                        }
                        if (aggregator != null && module.getParentModule() == null) {
                            module.setParentModule(aggregator);
                            aggregator.addChildLibertyModule(module);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse potential parent settings at: " + parentSettingsFile, e);
                }
            }
        }
    }

    /**
     * Parses build metadata for a given Liberty module.
     * Returns {@code null} when the build file cannot be parsed.
     */
    private AbstractProjectMetadata parseBuildMetadata(LibertyModule module) {
        VirtualFile buildFile = module.getBuildFile();
        if (buildFile == null) return null;

        try {
            String buildFilePath = buildFile.getPath();
            if (module.getProjectType() == Constants.ProjectType.LIBERTY_MAVEN_PROJECT) {
                return new MavenProjectMetadata(buildFilePath);
            } else {
                // For Gradle, also look for the settings file in the same directory.
                String settingsFilePath = null;
                Path settingsFile = GradleProjectMetadata.findSettingsFile(
                        Paths.get(buildFilePath).getParent());
                if (settingsFile != null) {
                    settingsFilePath = settingsFile.toString();
                }
                return new GradleProjectMetadata(buildFilePath, settingsFilePath);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not parse build metadata for module: " + module.getName(), e);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // State cache
    // -------------------------------------------------------------------------

    /**
     * Records a module's current app state in the state cache.
     * When the state is {@code STOPPED} the entry is removed because STOPPED is the
     * default initial state of every newly constructed {@link LibertyModule}.
     *
     * @param moduleName The display name of the module whose state changed.
     * @param state      The new app state.
     */
    public void cacheState(String moduleName, LibertyModule.AppState state) {
        if (state == LibertyModule.AppState.STOPPED) {
            stateCache.remove(moduleName);
        } else {
            stateCache.put(moduleName, state);
        }
    }

    /**
     * Re-stamps the app state of every module in the given project from the state cache.
     * Called after the module list is rebuilt so that freshly created {@link LibertyModule}
     * instances start with the correct state rather than the default STOPPED.
     *
     * @param project The project whose modules should be updated.
     */
    private void populateStatesFromCache(Project project) {
        if (stateCache.isEmpty()) return;
        for (LibertyModule module : new ArrayList<>(libertyModules.values())) {
            if (!project.equals(module.getProject())) continue;
            LibertyModule.AppState cached = stateCache.get(module.getName());
            if (cached != null) {
                module.setAppState(cached);
            }
        }
    }

    // -------------------------------------------------------------------------

    /**
     * Add tracked Liberty project to workspace, update project,
     * projectType, name and validContainerVersion if already tracked.
     *
     * @param module LibertyModule
     */
    public LibertyModule addLibertyModule(LibertyModule module) {
        synchronized (libertyModules) {
            if (libertyModules.containsKey(module.getBuildFile())) {
                // Update existing Liberty project, projectType module, name and validContainerVersion
                // Do not update the build file (key), debugMode, shellWidget or customStartParams since
                // they may modify saved run configs.
                LibertyModule existing = libertyModules.get(module.getBuildFile());
                existing.setProject(module.getProject());
                existing.setProjectType(module.getProjectType());
                existing.setName(module.getName());
                existing.setValidContainerVersion(module.isValidContainerVersion());
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
                    it.remove();
                }
            }
        }
    }
}
