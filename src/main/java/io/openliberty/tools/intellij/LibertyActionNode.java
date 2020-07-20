package io.openliberty.tools.intellij;

import javax.swing.tree.DefaultMutableTreeNode;

public class LibertyActionNode extends DefaultMutableTreeNode {
    public String name;

    public LibertyActionNode(String name) {
        super(name);
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

}