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

import com.intellij.openapi.diagnostic.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Assisted by IBM Bob
 *
 * Extracts Liberty multi-module metadata from a Gradle project directory.
 *
 * <p>Supports both Groovy DSL ({@code build.gradle}, {@code settings.gradle}) and
 * Kotlin DSL ({@code build.gradle.kts}, {@code settings.gradle.kts}).</p>
 *
 * <p>Specifically this class determines:</p>
 * <ul>
 *   <li>The project name ({@code rootProject.name} in settings, or directory name)</li>
 *   <li>The parent project name (by inspecting the parent directory's settings file)</li>
 *   <li>The list of declared child module names ({@code include(…)} in settings)</li>
 *   <li>Whether the Liberty Gradle plugin is configured (directly or inherited)</li>
 *   <li>Whether this project is an aggregator</li>
 *   <li>Inter-project {@code project(':name')} dependencies declared in the build file</li>
 * </ul>
 */
public class GradleProjectMetadata extends AbstractProjectMetadata {

    private static final Logger LOGGER = Logger.getInstance(GradleProjectMetadata.class);

    // -------------------------------------------------------------------------
    // Patterns
    // -------------------------------------------------------------------------

    /** Matches: rootProject.name = 'value' or rootProject.name = "value" */
    private static final Pattern ROOT_NAME_PATTERN =
            Pattern.compile("rootProject\\.name\\s*=\\s*[\"']([^\"']+)[\"']");

    /**
     * Matches the start of an {@code include} statement.
     * The negative lookahead {@code (?![A-Za-z])} prevents matching
     * {@code includeBuild}, {@code includeFlat}, etc.
     */
    private static final Pattern INCLUDE_START_PATTERN =
            Pattern.compile("^\\s*include(?![A-Za-z])\\s*\\)?(.*)");

    /**
     * Matches optional projectDir remappings:
     * {@code project(':name').projectDir = new File('dir')}
     */
    private static final Pattern PROJECT_DIR_REMAP_PATTERN =
            Pattern.compile("project\\s*\\([\"']:(\\w[\\w/-]*)['\"']\\)\\.projectDir\\s*=\\s*new\\s+File\\s*\\([\"']([^\"']+)[\"']\\)");

    /**
     * Matches a {@code project(':name')} dependency inside a {@code dependencies {}} block.
     */
    private static final Pattern PROJECT_DEP_PATTERN =
            Pattern.compile("project\\s*\\(\\s*(?:path\\s*:\\s*)?[\"'](:(?:[\\w/-]+:)*[\\w/-]+)[\"']");

    /** Liberty plugin detection patterns (Groovy and Kotlin DSL, all declaration styles). */
    private static final Pattern[] LIBERTY_PLUGIN_PATTERNS = {
            Pattern.compile("id\\s*\\(?\\s*[\"']io\\.openliberty\\.tools\\.gradle\\.Liberty[\"']\\s*\\)?"),
            Pattern.compile("apply\\s+plugin\\s*:\\s*[\"']liberty[\"']"),
            Pattern.compile("apply\\s+plugin\\s*:\\s*[\"']io\\.openliberty\\.tools\\.gradle\\.Liberty[\"']"),
            Pattern.compile("classpath\\s+[\"']io\\.openliberty\\.tools:liberty-gradle-plugin"),
    };

    /** Matches the opening of an {@code allprojects} or {@code subprojects} block. */
    private static final Pattern ALL_OR_SUB_PROJECTS_BLOCK_PATTERN =
            Pattern.compile("^\\s*(allprojects|subprojects)\\s*\\{");

    // -------------------------------------------------------------------------
    // Gradle-specific field
    // -------------------------------------------------------------------------

    private final String settingsFilePath;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Parses the Gradle project at the given paths and populates all metadata fields.
     *
     * @param buildFilePath    Absolute path to the {@code build.gradle} (or {@code .kts}), or {@code null}.
     * @param settingsFilePath Absolute path to the {@code settings.gradle} (or {@code .kts}), or {@code null}.
     * @throws Exception if required files cannot be read.
     */
    public GradleProjectMetadata(String buildFilePath, String settingsFilePath) throws Exception {
        super(buildFilePath);
        this.settingsFilePath = settingsFilePath;
        extract();
    }

    /**
     * Returns the absolute path to the Gradle settings file used during extraction,
     * or {@code null} when no settings file was found.
     */
    public String getSettingsFilePath() {
        return settingsFilePath;
    }

    // -------------------------------------------------------------------------
    // Extraction
    // -------------------------------------------------------------------------

    private void extract() throws Exception {
        Path projectDir = buildFilePath != null
                ? Paths.get(buildFilePath).getParent()
                : (settingsFilePath != null ? Paths.get(settingsFilePath).getParent() : null);

        // Read the settings file once and share across all methods that need it.
        List<String> settingsLines = readSettingsLines(projectDir);

        projectName       = resolveProjectName(projectDir, settingsLines);
        subprojects.addAll(resolveSubprojects(settingsLines));
        isAggregator      = !subprojects.isEmpty();
        parentProjectName = resolveParentProjectName(projectDir);

        hasLibertyPlugin = (buildFilePath != null && isLibertyPluginInBuildFile(buildFilePath))
                || isLibertyPluginInheritedFromParent(projectDir);

        projectDependencies.addAll(buildFilePath != null
                ? resolveProjectDependencies(buildFilePath)
                : new ArrayList<>());
    }

    // -------------------------------------------------------------------------
    // Settings file reading (single I/O point)
    // -------------------------------------------------------------------------

    /**
     * Reads the settings file in {@code dir} into a list of lines.
     * Returns an empty list when no settings file exists or cannot be read.
     */
    private List<String> readSettingsLines(Path dir) {
        List<String> lines = new ArrayList<>();
        if (dir == null) return lines;
        Path settingsFile = findSettingsFile(dir);
        if (settingsFile == null) return lines;
        try (BufferedReader reader = new BufferedReader(new FileReader(settingsFile.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not read settings file: " + settingsFile, e);
        }
        return lines;
    }

    // -------------------------------------------------------------------------
    // Project name
    // -------------------------------------------------------------------------

    private String resolveProjectName(Path projectDir, List<String> settingsLines) {
        for (String line : settingsLines) {
            Matcher m = ROOT_NAME_PATTERN.matcher(line);
            if (m.find()) return m.group(1);
        }
        return (projectDir != null && projectDir.getFileName() != null)
                ? projectDir.getFileName().toString() : null;
    }

    // -------------------------------------------------------------------------
    // Sub-projects
    // -------------------------------------------------------------------------

    private List<String> resolveSubprojects(List<String> settingsLines) {
        List<String> result = new ArrayList<>();
        Map<String, String> projectDirRemappings = new HashMap<>();
        StringBuilder currentStatement = new StringBuilder();
        boolean collectingInclude = false;

        for (String line : settingsLines) {
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("#")) {
                if (collectingInclude) {
                    parseIncludeContent(currentStatement.toString(), result);
                    currentStatement.setLength(0);
                    collectingInclude = false;
                }
                continue;
            }

            Matcher remapMatcher = PROJECT_DIR_REMAP_PATTERN.matcher(trimmed);
            if (remapMatcher.find()) {
                projectDirRemappings.put(remapMatcher.group(1), remapMatcher.group(2));
            }

            if (collectingInclude) {
                currentStatement.append(" ").append(trimmed);
                if (trimmed.contains(")") || !trimmed.endsWith(",")) {
                    parseIncludeContent(currentStatement.toString(), result);
                    currentStatement.setLength(0);
                    collectingInclude = false;
                }
            } else {
                Matcher includeMatcher = INCLUDE_START_PATTERN.matcher(trimmed);
                if (includeMatcher.matches()) {
                    String rest = includeMatcher.group(1).trim();
                    currentStatement.append(rest);
                    if (rest.contains(")") || (!rest.isEmpty() && !rest.endsWith(","))) {
                        parseIncludeContent(currentStatement.toString(), result);
                        currentStatement.setLength(0);
                    } else {
                        collectingInclude = true;
                    }
                }
            }
        }

        if (collectingInclude && currentStatement.length() > 0) {
            parseIncludeContent(currentStatement.toString(), result);
        }

        // Apply projectDir remappings
        for (int i = 0; i < result.size(); i++) {
            String name = result.get(i);
            if (projectDirRemappings.containsKey(name)) {
                result.set(i, projectDirRemappings.get(name));
            }
        }
        return result;
    }

    private void parseIncludeContent(String content, List<String> result) {
        String cleaned = content.replaceAll("\\)\\s*$", "");
        String[] parts = cleaned.split(",");
        for (String part : parts) {
            String name = part.replaceAll("[\"'()\\s]", "").replaceAll("^:+", "").trim();
            if (!name.isEmpty()) {
                result.add(name);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Parent project name
    // -------------------------------------------------------------------------

    private String resolveParentProjectName(Path projectDir) {
        if (projectDir == null) return null;
        String currentDirName = projectDir.getFileName() != null ? projectDir.getFileName().toString() : null;
        if (currentDirName == null) return null;
        Path parentDir = projectDir.getParent();
        if (parentDir == null) return null;

        List<String> parentSettingsLines = readSettingsLines(parentDir);
        if (parentSettingsLines.isEmpty()) return null;

        List<String> parentSubprojects = resolveSubprojects(parentSettingsLines);
        if (!parentSubprojects.contains(currentDirName)) return null;

        return resolveProjectName(parentDir, parentSettingsLines);
    }

    // -------------------------------------------------------------------------
    // Liberty plugin detection
    // -------------------------------------------------------------------------

    private boolean isLibertyPluginInBuildFile(String buildFilePathStr) {
        try (BufferedReader reader = new BufferedReader(new FileReader(buildFilePathStr))) {
            String line;
            while ((line = reader.readLine()) != null) {
                for (Pattern p : LIBERTY_PLUGIN_PATTERNS) {
                    if (p.matcher(line).find()) return true;
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not read build file for Liberty plugin detection: " + buildFilePathStr, e);
        }
        return false;
    }

    private boolean isLibertyPluginInheritedFromParent(Path projectDir) {
        if (projectDir == null) return false;
        Path parentDir = projectDir.getParent();
        if (parentDir == null) return false;

        List<String> parentSettingsLines = readSettingsLines(parentDir);
        if (parentSettingsLines.isEmpty()) return false;

        String currentDirName = projectDir.getFileName() != null ? projectDir.getFileName().toString() : "";
        List<String> parentSubprojects = resolveSubprojects(parentSettingsLines);
        if (!parentSubprojects.contains(currentDirName)) return false;

        Path parentBuildFile = findBuildFile(parentDir);
        if (parentBuildFile == null) return false;

        return isLibertyPluginInAllOrSubprojectsBlock(parentBuildFile.toString());
    }

    private boolean isLibertyPluginInAllOrSubprojectsBlock(String buildFilePathStr) {
        try (BufferedReader reader = new BufferedReader(new FileReader(buildFilePathStr))) {
            String line;
            boolean inTargetBlock = false;
            int braceDepth = 0;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!inTargetBlock) {
                    Matcher m = ALL_OR_SUB_PROJECTS_BLOCK_PATTERN.matcher(trimmed);
                    if (m.find()) {
                        inTargetBlock = true;
                        braceDepth = countChar(trimmed, '{') - countChar(trimmed, '}');
                        if (braceDepth <= 0) inTargetBlock = false;
                    }
                } else {
                    braceDepth += countChar(trimmed, '{') - countChar(trimmed, '}');
                    if (braceDepth <= 0) {
                        inTargetBlock = false;
                        braceDepth = 0;
                        continue;
                    }
                    for (Pattern p : LIBERTY_PLUGIN_PATTERNS) {
                        if (p.matcher(trimmed).find()) return true;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not read parent build file for inherited Liberty plugin detection: " + buildFilePathStr, e);
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Project dependencies
    // -------------------------------------------------------------------------

    private List<String> resolveProjectDependencies(String buildFilePathStr) {
        List<String> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(buildFilePathStr))) {
            String line;
            boolean inDependenciesBlock = false;
            int braceDepth = 0;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!inDependenciesBlock) {
                    if (trimmed.startsWith("dependencies")) {
                        inDependenciesBlock = true;
                        braceDepth = trimmed.contains("{")
                                ? countChar(trimmed, '{') - countChar(trimmed, '}')
                                : 0;
                    }
                } else {
                    if (braceDepth == 0 && trimmed.equals("{")) { braceDepth = 1; continue; }
                    braceDepth += countChar(trimmed, '{') - countChar(trimmed, '}');
                    if (braceDepth <= 0) { inDependenciesBlock = false; braceDepth = 0; continue; }
                    Matcher m = PROJECT_DEP_PATTERN.matcher(trimmed);
                    while (m.find()) {
                        String bare = lastSegment(m.group(1));
                        if (!bare.isEmpty() && !result.contains(bare)) result.add(bare);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.warn("Could not read build file for project dependencies: " + buildFilePathStr, e);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Static file helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the settings file ({@code settings.gradle} preferred over
     * {@code settings.gradle.kts}) found in {@code dir}, or {@code null} if absent.
     */
    public static Path findSettingsFile(Path dir) {
        if (dir == null) return null;
        Path groovy = dir.resolve("settings.gradle");
        if (Files.exists(groovy)) return groovy;
        Path kotlin = dir.resolve("settings.gradle.kts");
        if (Files.exists(kotlin)) return kotlin;
        return null;
    }

    /**
     * Returns the build file ({@code build.gradle} preferred over
     * {@code build.gradle.kts}) found in {@code dir}, or {@code null} if absent.
     */
    public static Path findBuildFile(Path dir) {
        if (dir == null) return null;
        Path groovy = dir.resolve("build.gradle");
        if (Files.exists(groovy)) return groovy;
        Path kotlin = dir.resolve("build.gradle.kts");
        if (Files.exists(kotlin)) return kotlin;
        return null;
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private static String lastSegment(String gradlePath) {
        String stripped = gradlePath.replaceAll("^:+", "");
        int lastColon = stripped.lastIndexOf(':');
        return lastColon >= 0 ? stripped.substring(lastColon + 1) : stripped;
    }

    private static int countChar(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) count++;
        }
        return count;
    }

    @Override
    public String toString() {
        return "GradleProjectMetadata{name=" + projectName
                + ", parent=" + parentProjectName
                + ", subprojects=" + subprojects
                + ", aggregator=" + isAggregator
                + ", libertyPlugin=" + hasLibertyPlugin
                + ", dependencies=" + projectDependencies
                + ", buildFile=" + buildFilePath
                + ", settingsFile=" + settingsFilePath + "}";
    }
}
