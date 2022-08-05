package io.openliberty.tools.intellij.starter;

/**
 * Models the parameters for the Open Liberty Starter API call
 */
public class LibertyStarterModel {

    private String group;
    private String artifact;
    private String buildTool;
    private String javaVersion;
    private String mpVersion;
    private String eeVersion;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getArtifact() {
        return artifact;
    }

    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    public String getBuildTool() {
        return buildTool;
    }

    public void setBuildTool(String buildTool) {
        this.buildTool = buildTool;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public String getMpVersion() {
        return mpVersion;
    }

    public void setMpVersion(String mpVersion) {
        this.mpVersion = mpVersion;
    }

    public String getEeVersion() {
        return eeVersion;
    }

    public void setEeVersion(String eeVersion) {
        this.eeVersion = eeVersion;
    }
}
