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
     * @return true if the liberty gradle plugin is detected in the build.gradle
     * @throws IOException
     */
    public static boolean validBuildGradle(PsiFile file) throws IOException {
            String buildFile = fileToString(file.getVirtualFile().getCanonicalPath());
            if (buildFile.isEmpty()) { return false; }

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
                        return true;
                    }
                }
            }
        return false;
    }

}
