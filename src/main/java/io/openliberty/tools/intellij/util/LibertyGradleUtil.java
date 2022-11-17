/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.plugins.gradle.settings.DistributionType;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettings;

public class LibertyGradleUtil {
    private static Logger LOGGER = Logger.getInstance(LibertyGradleUtil.class);;
    /**
     * Given the gradle build file get the project name
     * This method looks for a settings.gradle file in the same parent dir
     * If a settings.gradle file exists, use the rootProject.name attribute
     *
     * @param file build.gradle file
     * @return project name if it exists in a settings.gradle, null if not
     */
    public static String getProjectName(VirtualFile file) {
        VirtualFile parentFolder = file.getParent();
        Path settingsPath = Paths.get(parentFolder.getCanonicalPath(), "settings.gradle");
        File settingsFile = settingsPath.toFile();
        if (settingsFile.exists()) {
            try {
                FileInputStream input = new FileInputStream(settingsFile);
                Properties prop = new Properties();
                prop.load(input);
                String name = prop.getProperty("rootProject.name");
                if (name != null) {
                    // return name without surrounding quotes
                    return name.replaceAll("^[\"']+|[\"']+$", "");
                }
            } catch (IOException e) {
                LOGGER.error("Could not read " + settingsPath, e.getMessage());
            }
        }
        return null;
    }

    /**
     * Given a path return the String content of the file
     * @param path to file
     * @return content of file
     * @throws IOException
     */
    public static String fileToString(String path) throws IOException {
        FileInputStream in = null;
        try {
            in = new FileInputStream(path);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
            return sb.toString();
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * Check if a Gradle build file is using the liberty gradle plugin
     *
     * @param file build.gradle file
     * @return BuildFile, validBuildFile true if using the liberty gradle plugin,
     * validContainerVersion true if plugin version is valid for dev mode in containers
     * @throws IOException
     */
    public static BuildFile validBuildGradle(PsiFile file) throws IOException {
            String buildFile = fileToString(file.getVirtualFile().getCanonicalPath());
            if (buildFile.isEmpty()) { return (new BuildFile(false, false)); }

            // check if "apply plugin: 'liberty'" is specified in the build.gradle
            boolean libertyPlugin = false;

            //TODO: filter out commented out lines in build.gradle
            // lookbehind for "apply plugin:", 0+ spaces, ' or ", "liberty"
            String applyPluginRegex = "(?<=apply plugin:)(\\s*)('|\")liberty";
            Pattern applyPluginPattern = Pattern.compile(applyPluginRegex);
            Matcher applyPluginMatcher = applyPluginPattern.matcher(buildFile);
            while (applyPluginMatcher.find()) {
                libertyPlugin = true;
            }
            // TODO: check if liberty is in the plugins block

            if (libertyPlugin) {
                // check if group matches io.openliberty.tools and name matches liberty-gradle-plugin
                String regex = "(?<=dependencies)(\\s*\\{)([^\\}]+)(?=\\})";
                String regex2 = "(.*\\bio\\.openliberty\\.tools\\b.*)(.*\\bliberty-gradle-plugin\\b.*)";

                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(buildFile);

                while (matcher.find()) {
                    String sub = buildFile.substring(matcher.start(), matcher.end());
                    Pattern pattern2 = Pattern.compile(regex2);
                    Matcher matcher2 = pattern2.matcher(sub);
                    while (matcher2.find()) {
                        String plugin = sub.substring(matcher2.start(), matcher2.end());
                        boolean vaildContainerVersion = containerVersion(plugin);

                        return (new BuildFile(true, vaildContainerVersion));
                    }
                }
            }
        return (new BuildFile(false, false));
    }

    /**
     * Given the plugin object as a string, use a regex to
     * get the version.
     *
     * @param plugin plugin object as a string
     * @return true if liberty-gradle-plugin is compatible for dev mode with containers
     */
    private static boolean containerVersion(String plugin) {
        // get the version from the plugin
        String versionRegex = "(?<=:liberty-gradle-plugin:).*(?=\')";
        Pattern versionPattern = Pattern.compile(versionRegex);
        Matcher versionMatcher = versionPattern.matcher(plugin);
        while (versionMatcher.find()) {
            try {
                String version = plugin.substring(versionMatcher.start(), versionMatcher.end());
                ComparableVersion pluginVersion = new ComparableVersion(version);
                ComparableVersion containerVersion = new ComparableVersion(Constants.LIBERTY_GRADLE_PLUGIN_CONTAINER_VERSION);
                if (pluginVersion.compareTo(containerVersion) >= 0) {
                    return true;
                }
                return false;
            } catch (NullPointerException | ClassCastException e) {
                return false;
            }
        }
        return false;
    }

    public static String getGradleSettingsCmd(Project project) {
        GradleProjectSettings gradleProjectSettings = GradleSettings.getInstance(project).getLinkedProjectSettings(project.getBasePath());

        if (gradleProjectSettings.getDistributionType().isWrapped()) {
            // a wrapper will be used
            String wrapperPath = getLocalGradleWrapperPath(project);
            if (wrapperPath != null) {
                return wrapperPath;
            }
        }
        else if (DistributionType.LOCAL.equals(gradleProjectSettings.getDistributionType())) {
            // local gradle to be used
            String gradleHome = gradleProjectSettings.getGradleHome(); //it is null when the path to gradle is invalid
            if (gradleHome != null) {
                String gradlePath = getCustomGradlePath(gradleHome);
                if (gradlePath != null) {
                    return gradlePath;
                }
            }
        }
        return "gradle"; // default gradle
    }

    private static String getLocalGradleWrapperPath(Project project) {
        String gradlew = SystemInfo.isWindows ? ".\\gradlew.bat" : "./gradlew";
        File file = new File(project.getBasePath(), gradlew);
        return file.exists() ? gradlew : null;
    }

    private static String getCustomGradlePath (String customGradleHome) {
        String additionalCMD = SystemInfo.isWindows ? "cmd /K " : ""; // without it, a new terminal window is opened
        File gradleHomeFile = new File(customGradleHome);
        if (gradleHomeFile != null) {
            // When a custom gradle is specified, IntelliJ settings force it to point to the root folder and consider the subfolders invalid,
            // and consequently, it will return null. For this reason, we need to use ./bin/gradle in order to execute gradle.
            File file = new File(gradleHomeFile.getAbsolutePath(), "bin"+ File.separator + "gradle");
            return file.exists() ? additionalCMD + file.getAbsolutePath() : null;
        }
        return null;
    }
}
