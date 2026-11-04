package io.openliberty.tools.intellij.lsp4jakarta.version;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

public class JakartaVersionManager {

    private static volatile JakartaVersionManager instance;

    // HashMap to store project versions (key: project name, value: list of Jakarta versions)
    private final Map<String, List<JakartaVersion>> projectVersionMap;

    private JakartaVersionManager() {
        projectVersionMap = new HashMap<>();
    }

    // Public method to get the singleton instance (thread-safe)
    public static JakartaVersionManager getInstance() {
        if (instance == null) {
            synchronized (JakartaVersionManager.class) {
                if (instance == null) {
                    instance = new JakartaVersionManager();
                }
            }
        }
        return instance;
    }

    public void setVersions(String projectName, List<JakartaVersion> versions) {
        projectVersionMap.put(projectName, versions);
    }

    public List<JakartaVersion> getVersion(String projectName, Project project, VirtualFile[] entries) {
        List<JakartaVersion> detectedVersions = JakartaVersionFinder.analyzeClasspath(entries, project);
        this.setVersions(projectName, detectedVersions);
        return detectedVersions;
    }

    public List<JakartaVersion> getVersion(String projectName) {
        return projectVersionMap.getOrDefault(projectName, Collections.emptyList());
    }

    public boolean hasVersion(String projectName) {
        return projectVersionMap.containsKey(projectName);
    }

    public void removeVersion(String projectName) {
        projectVersionMap.remove(projectName);
    }

    public Map<String, List<JakartaVersion>> getAllVersions() {
        return new HashMap<>(projectVersionMap);
    }

    public void clearVersions() {
        projectVersionMap.clear();
    }

    public int getVersionCount() {
        return projectVersionMap.size();
    }
}
