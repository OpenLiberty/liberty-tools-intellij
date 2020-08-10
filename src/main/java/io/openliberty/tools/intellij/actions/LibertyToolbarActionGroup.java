package io.openliberty.tools.intellij.actions;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.ui.treeStructure.actions.CollapseAllAction;
import com.intellij.ui.treeStructure.actions.ExpandAllAction;

public class LibertyToolbarActionGroup extends DefaultActionGroup {

    private CollapseAllAction collapseAction;
    private ExpandAllAction expandAction;
    private Tree tree;

    public LibertyToolbarActionGroup(Tree tree) {
        super("LibertyActionToolBar", false);
        this.tree = tree;
        final ActionManager actionManager = ActionManager.getInstance();
        add(actionManager.getAction("io.openliberty.tools.intellij.actions.RefreshLibertyToolbar"));
        addSeparator();
        add(actionManager.getAction("io.openliberty.tools.intellij.actions.RunLibertyDevTask"));
        addSeparator();

        this.collapseAction = new CollapseAllAction(tree);
        this.expandAction = new ExpandAllAction(tree);
        add(this.collapseAction);
        add(this.expandAction);
    }
}
