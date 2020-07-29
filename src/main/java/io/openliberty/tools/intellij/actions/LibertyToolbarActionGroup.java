package io.openliberty.tools.intellij.actions;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.ui.treeStructure.actions.CollapseAllAction;
import com.intellij.ui.treeStructure.actions.ExpandAllAction;
import io.openliberty.tools.intellij.util.Constants;

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
        add(actionManager.getAction("io.openliberty.tools.intellij.actions.ExecuteLibertyDevTask"));
        addSeparator();

        this.collapseAction = new CollapseAllAction(tree);
        this.expandAction = new ExpandAllAction(tree);
        add(this.collapseAction);
        add(this.expandAction);
    }

    @Override
    public void update(AnActionEvent event) {
        // Enable/disable depending on whether user is editing...
        Tree updatedTree = (Tree) event.getDataContext().getData(Constants.LIBERTY_DASHBOARD_TREE);

        if (updatedTree != null && updatedTree != this.tree) {
            remove(this.collapseAction);
            remove(this.expandAction);
            CollapseAllAction newCollapseAction = new CollapseAllAction(updatedTree);
            ExpandAllAction newExpandAction = new ExpandAllAction(updatedTree);
            add(newCollapseAction);
            add(newExpandAction);
            this.collapseAction = newCollapseAction;
            this.expandAction = newExpandAction;
            this.tree = updatedTree;
        }
    }

}
