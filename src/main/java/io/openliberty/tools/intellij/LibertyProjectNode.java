package io.openliberty.tools.intellij;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

public class LibertyProjectNode extends DefaultMutableTreeNode {
    public VirtualFile path;
    public String name;
    public String projectType;

    public LibertyProjectNode(PsiFile file, String name, String projectType) {
        super(name);
        this.name = name;
        this.projectType = projectType;
        this.path = file.getVirtualFile();
    }

    public String getName() {
        return this.name;
    }

    public VirtualFile getFilePath() {
        return this.path;
    }

    public String getProjectType() { return this.projectType; }
}
