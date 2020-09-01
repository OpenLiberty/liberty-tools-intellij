package io.openliberty.tools.intellij.util;

import com.intellij.psi.PsiFile;

/**
 * Defines a BuildFile object
 */
public class BuildFile {
    public PsiFile buildFile;
    public boolean validBuildFile;
    public boolean validContainerVersion;

    public BuildFile(boolean validBuildFile, boolean validContainerVersion) {
        this.validBuildFile = validBuildFile;
        this.validContainerVersion = validContainerVersion;
        this.buildFile = null;
    }

    public PsiFile getBuildFile() { return this.buildFile; }

    public void setBuildFile(PsiFile buildFile) {
        this.buildFile = buildFile;
    }

    public boolean isValidBuildFile() {
        return this.validBuildFile;
    }

    public boolean isValidContainerVersion() {
        return this.validContainerVersion;
    }

}
