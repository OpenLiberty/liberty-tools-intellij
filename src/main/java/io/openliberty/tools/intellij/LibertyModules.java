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
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.util.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
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
            buildMultiModuleModel(project);
            populateStatesFromCache(project);
        }
        return this;
    }

    /**
     * A {@code pom.xml} taking part in the multi-module graph: either a registered Liberty
     * module or an aggregator candidate that does not configure the Liberty plugin.
     */
    private static class PomNode {
        final File dir;
        final VirtualFile pomFile;
        final MavenProjectMetadata meta;
        /** Canonical directories of the modules listed in {@code <modules>}. */
        final List<String> childDirs = new ArrayList<>();

        PomNode(File dir, VirtualFile pomFile, MavenProjectMetadata meta) {
            this.dir = dir;
            this.pomFile = pomFile;
            this.meta = meta;
            for (String subproject : meta.getSubprojects()) {
                File child = new File(dir, subproject);
                // <module> may point at a pom file rather than a directory
                childDirs.add(canonicalPath(child.isFile() ? child.getParentFile() : child));
            }
        }
    }

    /**
     * Builds the multi-module model in a single pass:
     * <ol>
     *   <li>Parse the metadata of every registered Maven Liberty module.</li>
     *   <li>Collect every other {@code pom.xml} (project POMs plus the POMs found in the
     *       ancestor directories of each Liberty module, which may lie outside the project)
     *       and parse each aggregator once.</li>
     *   <li>Register every aggregator that reaches a Liberty module, directly or through other
     *       aggregators, so that it does not matter in which order the POMs are visited.</li>
     *   <li>Link each child to the module named in its {@code <parent>} (by {@code groupId:artifactId}),
     *       then link each aggregator to the modules in its {@code <modules>} list, resolved by
     *       canonical directory, for children that are still unlinked.</li>
     * </ol>
     * What counts as an aggregator is defined in one place, {@link MavenProjectMetadata}.
     *
     * @param project the IntelliJ project
     */
    private void buildMultiModuleModel(Project project) {
        // 1. Registered modules, keyed by canonical directory
        Map<String, LibertyModule> byDir = new HashMap<>();
        Map<String, PomNode> nodes = new TreeMap<>(); // sorted so that linking is deterministic
        for (LibertyModule module : getLibertyModules(project)) {
            VirtualFile buildFile = module.getBuildFile();
            if (buildFile == null || buildFile.getParent() == null) continue;
            File dir = new File(buildFile.getParent().getPath());
            String key = canonicalPath(dir);
            byDir.put(key, module);
            if (module.getProjectType().equals(Constants.ProjectType.LIBERTY_MAVEN_PROJECT)) {
                MavenProjectMetadata meta = new MavenProjectMetadata(dir, project);
                module.setBuildMetadata(meta);
                if (meta.isAggregator()) {
                    nodes.put(key, new PomNode(dir, buildFile, meta));
                }
            }
        }
        // Clear stale relationships from any previous scan before re-wiring.
        byDir.values().forEach(LibertyModule::clearMultiModuleRelationships);

        // 2. Every other POM that is an aggregator
        Map<String, VirtualFile> otherPoms = new TreeMap<>();
        for (VirtualFile pom : LibertyProjectUtil.getAllPomFiles(project)) {
            if (pom.getParent() != null) {
                otherPoms.put(canonicalPath(new File(pom.getParent().getPath())), pom);
            }
        }
        for (LibertyModule module : new ArrayList<>(byDir.values())) {
            if (!module.getProjectType().equals(Constants.ProjectType.LIBERTY_MAVEN_PROJECT)) continue;
            VirtualFile buildFile = module.getBuildFile();
            for (File dir = new File(buildFile.getParent().getPath()).getParentFile(); dir != null; dir = dir.getParentFile()) {
                File pom = new File(dir, "pom.xml");
                if (!pom.isFile()) break;
                String key = canonicalPath(dir);
                if (!otherPoms.containsKey(key)) {
                    VirtualFile pomVFile = LocalFileSystem.getInstance().findFileByPath(pom.getAbsolutePath());
                    if (pomVFile != null) otherPoms.put(key, pomVFile);
                }
            }
        }
        Map<String, PomNode> candidates = new TreeMap<>();
        for (Map.Entry<String, VirtualFile> entry : otherPoms.entrySet()) {
            if (byDir.containsKey(entry.getKey())) continue;
            File pomFile = new File(entry.getValue().getPath());
            if (!MavenProjectMetadata.hasModulesElement(pomFile)) continue;
            MavenProjectMetadata meta = new MavenProjectMetadata(pomFile.getParentFile(), project);
            if (meta.isAggregator()) {
                candidates.put(entry.getKey(), new PomNode(pomFile.getParentFile(), entry.getValue(), meta));
            }
        }

        // 3. Register the aggregators that reach a Liberty module
        Map<String, Boolean> reaches = new HashMap<>();
        for (Map.Entry<String, PomNode> entry : candidates.entrySet()) {
            if (!reachesLibertyModule(entry.getKey(), byDir.keySet(), candidates, reaches, new HashSet<>())) continue;
            PomNode node = entry.getValue();
            String name = node.meta.getProjectName() != null && !node.meta.getProjectName().isEmpty()
                    ? node.meta.getProjectName() : node.dir.getName();
            LibertyModule aggregator = new LibertyModule(
                    project, node.pomFile, name, Constants.ProjectType.LIBERTY_MAVEN_PROJECT, false);
            aggregator.setBuildMetadata(node.meta);
            byDir.put(entry.getKey(), addLibertyModule(aggregator));
            nodes.put(entry.getKey(), node);
            LOGGER.debug("Registered aggregator module: " + name + " at " + entry.getKey());
        }

        // 4a. Link each child to the module named in its <parent>. Coordinates are groupId:artifactId,
        // so two modules with the same artifactId and a different groupId cannot overwrite each other.
        Map<String, LibertyModule> byCoordinates = new HashMap<>();
        for (LibertyModule module : byDir.values()) {
            if (module.getBuildMetadata() instanceof MavenProjectMetadata) {
                String coordinates = ((MavenProjectMetadata) module.getBuildMetadata()).getCoordinates();
                if (coordinates != null) byCoordinates.putIfAbsent(coordinates, module);
            }
        }
        for (LibertyModule child : byDir.values()) {
            if (!(child.getBuildMetadata() instanceof MavenProjectMetadata)) continue;
            String parentCoordinates = ((MavenProjectMetadata) child.getBuildMetadata()).getParentCoordinates();
            LibertyModule parent = parentCoordinates == null ? null : byCoordinates.get(parentCoordinates);
            if (parent != null && parent != child && !isAncestorOrSelf(child, parent)) {
                child.setParentModule(parent);
                parent.addChildLibertyModule(child);
            }
        }

        // 4b. Link each aggregator to its modules, unless already linked by <parent>
        for (Map.Entry<String, PomNode> entry : nodes.entrySet()) {
            LibertyModule aggregator = byDir.get(entry.getKey());
            for (String childDir : entry.getValue().childDirs) {
                LibertyModule child = byDir.get(childDir);
                if (child != null && child != aggregator && child.getParentModule() == null
                        && !isAncestorOrSelf(child, aggregator)) {
                    child.setParentModule(aggregator);
                    aggregator.addChildLibertyModule(child);
                }
            }
        }
    }

    /**
     * Returns whether the aggregator in {@code dir} lists, directly or through other aggregator
     * candidates, a directory that is a registered Liberty module. Cycles are tolerated.
     */
    private static boolean reachesLibertyModule(String dir, Set<String> libertyDirs, Map<String, PomNode> candidates,
                                                Map<String, Boolean> memo, Set<String> visiting) {
        if (libertyDirs.contains(dir)) return true;
        Boolean known = memo.get(dir);
        if (known != null) return known;
        PomNode node = candidates.get(dir);
        if (node == null || !visiting.add(dir)) return false;
        boolean result = false;
        for (String childDir : node.childDirs) {
            if (reachesLibertyModule(childDir, libertyDirs, candidates, memo, visiting)) {
                result = true;
                break;
            }
        }
        visiting.remove(dir);
        memo.put(dir, result);
        return result;
    }

    /** Returns whether {@code candidate} is {@code module} or one of its ancestors. */
    private static boolean isAncestorOrSelf(LibertyModule candidate, LibertyModule module) {
        for (LibertyModule m = module; m != null; m = m.getParentModule()) {
            if (m == candidate) return true;
        }
        return false;
    }

    private static String canonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.getAbsolutePath();
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
                // Clear multi-module relationships so buildMultiModuleModel() re-wires them cleanly.
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
