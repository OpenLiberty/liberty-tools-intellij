package io.openliberty.tools.intellij.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.artifact.versioning.ComparableVersion;

public class LibertyGradleUtil {
    private static Logger log = Logger.getInstance(LibertyGradleUtil.class);;
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
                log.error("Could not read " + settingsPath, e.getMessage());
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

}
