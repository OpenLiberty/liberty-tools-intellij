package io.openliberty.tools.intellij.actions;

import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.DefaultTreeExpander;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.treeStructure.Tree;

public class LibertyToolbarActionGroup extends DefaultActionGroup {

//    private CollapseAllAction collapseAction;
//    private ExpandAllAction expandAction;
    private Tree tree;

    public LibertyToolbarActionGroup(Tree tree) {
        super("LibertyActionToolBar", false);
        this.tree = tree;
        final ActionManager actionManager = ActionManager.getInstance();
        add(actionManager.getAction("io.openliberty.tools.intellij.actions.RefreshLibertyToolbar"));
        addSeparator();
        add(actionManager.getAction("io.openliberty.tools.intellij.actions.RunLibertyDevTask"));
        addSeparator();

        DefaultTreeExpander expander = new DefaultTreeExpander(tree);
        CommonActionsManager commonActionsManager = CommonActionsManager.getInstance();
        add(commonActionsManager.createCollapseAllAction(expander, tree));
        add(commonActionsManager.createExpandAllAction(expander, tree));
    }
}
