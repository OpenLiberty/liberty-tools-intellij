/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation.
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
    private static Logger LOGGER = Logger.getInstance(LibertyGradleUtil.class);

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
        Path settingsPath = Paths.get(parentFolder.getPath(), "settings.gradle");
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
                LOGGER.error(String.format("Could not read project name from file %s", settingsPath), e);
            }
        }
        return parentFolder.getName();
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
            String buildFile = fileToString(file.getVirtualFile().getPath());
            if (buildFile.isEmpty()) { return (new BuildFile(false, false)); }

            // instead of iterating over capture groups in a plugin{}, search directly
            // look for our plugin not defined in a line comment. if defined, grab version # from Group 2
            String negativeLineCommentLookBehind = "(?<!//\\s*)";
            String gradleLibertyPluginId = "id\\s*[\'\"]io\\.openliberty\\.tools\\.gradle\\.Liberty[\'\"]";
            String optionalVersion = "(\\s+version\\s+[\'\"](.*)[\'\"])?";
            Pattern gradleLibertyPluginIdPattern = Pattern.compile(negativeLineCommentLookBehind + gradleLibertyPluginId + optionalVersion);
            Matcher gradleLibertyPluginIdMatcher = gradleLibertyPluginIdPattern.matcher(buildFile);
            if (gradleLibertyPluginIdMatcher.find()) {
                if (gradleLibertyPluginIdMatcher.groupCount() < 2) {
                    // if version is not defined, assumes latest is pulled
                    return (new BuildFile(true, true));
                }
                ComparableVersion pluginVersion = new ComparableVersion(gradleLibertyPluginIdMatcher.group(2));
                ComparableVersion containerVersion = new ComparableVersion(Constants.LIBERTY_GRADLE_PLUGIN_CONTAINER_VERSION);
                return (new BuildFile(true, pluginVersion.compareTo(containerVersion) >= 0));
            }

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
                        return (new BuildFile(true, containerVersion(plugin)));
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

    /**
     * Get Gradle preferences from Build Tools in IntelliJ and return a command to execute or an exception
     *
     * @param project liberty project
     * @return String command to execute in the terminal or an exception to display
     * @throws LibertyException
     */
    public static String getGradleSettingsCmd(Project project, VirtualFile buildFile) throws LibertyException {
        GradleProjectSettings gradleProjectSettings = GradleSettings.getInstance(project).getLinkedProjectSettings(buildFile.getParent().getPath());
        if (gradleProjectSettings == null) {
            String translatedMessage = LocalizedResourceUtil.getMessage("gradle.settings.is.null");
            throw new LibertyException("Could not execute action because there is an error with Gradle configuration. Make sure to configure a valid path for Gradle inside IntelliJ Gradle preferences.", translatedMessage);
        }
        else if (gradleProjectSettings.getDistributionType().isWrapped()) {
            // a wrapper will be used
            return getLocalGradleWrapperPath(gradleProjectSettings.getExternalProjectPath());
        }
        else if (DistributionType.LOCAL.equals(gradleProjectSettings.getDistributionType())) {
            // local gradle to be used
            String gradleHome = gradleProjectSettings.getGradleHome(); //it is null when the path to gradle is invalid
            if (gradleHome == null) {
                String translatedMessage = LocalizedResourceUtil.getMessage("gradle.invalid.build.preference");
                throw new LibertyException("Make sure to configure a valid path for Gradle inside IntelliJ Gradle preferences.", translatedMessage);
            } else {
                return getCustomGradlePath(gradleHome);
            }
        }
        return "gradle"; // default gradle
    }

    /**
     * Get the local wrapper path for Gradle that is in the project level
     *
     * @param wrapperDir location of gradle wrapper
     * @return the Graddle wrapper path to be executed or an exception to display
     * @throws LibertyException
     */
    private static String getLocalGradleWrapperPath(String wrapperDir) throws LibertyException {
        String gradlew = SystemInfo.isWindows ? ".\\gradlew.bat" : "./gradlew";
        File file = new File(wrapperDir, gradlew);
        if (!file.exists()){
            String translatedMessage = LocalizedResourceUtil.getMessage("gradle.wrapper.does.not.exist");
            throw new LibertyException("A Gradle wrapper for the project could not be found. Make sure to configure a " +
                    "valid Gradle wrapper or change the build preferences for Gradle inside IntelliJ Gradle preferences.", translatedMessage);
        }
        if (!file.canExecute()) {
            String translatedMessage = LocalizedResourceUtil.getMessage("gradle.wrapper.cannot.execute");
            throw new LibertyException("Could not execute Gradle wrapper because the process does not have permission to " +
                    "execute it. Consider giving executable permission for the Gradle wrapper file or changing the build " +
                    "preferences for Gradle inside IntelliJ Gradle preferences.", translatedMessage);
        }
        String path;
        try {
            path = file.getCanonicalPath();
        } catch (IOException e) {
            throw new LibertyException("Could not get canonical path for gradle wrapper file");
        }
        return path;
    }

    /**
     * Get the custom Gradle path from Build Tools in IntelliJ
     *
     * @param customGradleHome the custom Gradle Home
     * @return Graddle path to be executed or an exception to display
     * @throws LibertyException
     */
    private static String getCustomGradlePath (String customGradleHome) throws LibertyException {
        File gradleHomeFile = new File(customGradleHome);
        // When a custom gradle is specified, IntelliJ settings force it to point to the root folder and consider the subfolders invalid,
        // and consequently, it will return null. For this reason, we need to use ./bin/gradle in order to execute gradle.
        String gradle = SystemInfo.isWindows ? "gradle.bat" : "gradle";
        File gradleExecutable = new File(new File(gradleHomeFile.getAbsolutePath(), "bin"), gradle);
        if (!gradleExecutable.exists()) {
            String translatedMessage = LocalizedResourceUtil.getMessage("gradle.does.not.exist", gradleExecutable.getAbsolutePath());
            throw new LibertyException(String.format("Could not execute the Gradle executable %s. Make sure a valid path is configured " +
                    "inside IntelliJ Gradle preferences.", gradleExecutable.getAbsolutePath()), translatedMessage);
        }
        if (!gradleExecutable.canExecute()) {
            String translatedMessage = LocalizedResourceUtil.getMessage("gradle.cannot.execute", gradleExecutable.getAbsolutePath());
            throw new LibertyException(String.format("Could not execute Gradle from %s because the process does not " +
                    "have permission to execute it. Consider giving executable permission for the Gradle executable or " +
                    "configure IntelliJ to use the Gradle wrapper.", gradleExecutable.getAbsolutePath()), translatedMessage);
        }
        String cmd = LibertyProjectUtil.includeEscapeToString(gradleExecutable.getAbsolutePath());
        if (SystemInfo.isWindows) {
            cmd = "cmd /K " + cmd; // without this, a new terminal window is opened
        }
        return cmd;
    }
}
