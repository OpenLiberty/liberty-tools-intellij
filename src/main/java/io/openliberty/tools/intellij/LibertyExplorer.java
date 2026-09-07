/*******************************************************************************
 * Copyright (c) 2020, 2026 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Computable;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.treeStructure.Tree;
import io.openliberty.tools.intellij.actions.LibertyGeneralAction;
import io.openliberty.tools.intellij.actions.LibertyToolbarActionGroup;
import io.openliberty.tools.intellij.util.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LibertyExplorer extends SimpleToolWindowPanel {
    private final static Logger LOGGER = Logger.getInstance(LibertyExplorer.class);

    public LibertyExplorer(@NotNull Project project) {
        super(true, true);
        //NOTE: To address the "Slow operations are prohibited on EDT" Exception (https://github.com/OpenLiberty/liberty-tools-intellij/issues/674), we have implemented the workaround outlined in the document (https://plugins.jetbrains.com/docs/intellij/general-threading-rules.html).
        // We have now moved the method "buildTree(project, getBackground())" to a background thread. To pass control from a background thread to the Event Dispatch Thread (EDT), UI operations are now included within the method "ApplicationManager.getApplication().invokeLater()".
        ModalityState modalityState = getModalityState();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // build tree (Read operations need to be wrapped in a read action)
            Tree tree = ApplicationManager.getApplication().runReadAction((Computable<Tree>) () -> buildTree(project, getBackground()));

            if (tree != null) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    JBScrollPane scrollPane = new JBScrollPane(tree);
                    scrollPane.setName(Constants.LIBERTY_SCROLL_PANE);
                    this.setContent(scrollPane);
                }, modalityState);
            } else {
                ApplicationManager.getApplication().invokeLater(() -> {
                    JBTextArea jbTextArea = new JBTextArea(LocalizedResourceUtil.getMessage("no.liberty.projects.detected"));
                    jbTextArea.setEditable(false);
                    jbTextArea.setBackground(getBackground());
                    jbTextArea.setLineWrap(true);

                    this.setContent(jbTextArea);
                }, modalityState);
            }

            ApplicationManager.getApplication().invokeLater(() -> {
                ActionToolbar actionToolbar = buildActionToolbar(tree);
                this.setToolbar(actionToolbar.getComponent());
            }, modalityState);
        });
    }

    private ModalityState getModalityState() {
        return ModalityState.nonModal();
    }

    public static ActionToolbar buildActionToolbar(Tree tree) {
        // create ActionToolBar
        final ActionManager actionManager = ActionManager.getInstance();
        LibertyToolbarActionGroup libertyActionGroup = new LibertyToolbarActionGroup(tree);

        ActionToolbar actionToolbar = actionManager.createActionToolbar(ActionPlaces.TOOLBAR, libertyActionGroup, true);
        actionToolbar.setTargetComponent(tree);
        actionToolbar.setOrientation(SwingConstants.HORIZONTAL);
        actionToolbar.setShowSeparatorTitles(true);
        actionToolbar.getComponent().setName(Constants.LIBERTY_ACTION_TOOLBAR);
        return actionToolbar;
    }

    /**
     * Builds the Open Liberty Tools Dashboard tree
     *
     * @param project         current project
     * @param backgroundColor
     * @return Tree object of all valid Liberty Gradle and Liberty Maven projects
     */
    public static Tree buildTree(Project project, Color backgroundColor) {
        LibertyModules libertyModules = LibertyModules.getInstance().scanLibertyModules(project);
        // This singleton may contain entries from old projects if you close a project and open another
        if (libertyModules.getLibertyModules(project).isEmpty()) {
            return null;
        }
        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Root node");
        HashMap<String, ArrayList<Object>> projectMap = new HashMap<>();

        for (LibertyModule libertyModule : libertyModules.getLibertyModules(project)) {

            // Multi-module: child modules are rendered under their parent node, not at the
            // top level. Skip them here; they are added when the parent is processed below.
            if (libertyModule.getParentModule() != null) {
                continue;
            }

            LibertyModuleNode node = new LibertyModuleNode(libertyModule);
            top.add(node);

            ArrayList<Object> settings = new ArrayList<Object>();
            settings.add(libertyModule.getBuildFile());
            settings.add(libertyModule.getProjectType());
            projectMap.put(libertyModule.getName(), settings);

            if (libertyModule.isParentOfLibertyModule()) {
                // Aggregator: add each child Liberty module as a child tree node with
                // its own actions. The parent node itself does not get action children —
                // actions are only meaningful on the concrete (leaf) modules.
                for (LibertyModule childModule : libertyModule.getChildLibertyModules()) {
                    LibertyModuleNode childNode = new LibertyModuleNode(childModule);
                    node.add(childNode);

                    ArrayList<Object> childSettings = new ArrayList<>();
                    childSettings.add(childModule.getBuildFile());
                    childSettings.add(childModule.getProjectType());
                    projectMap.put(childModule.getName(), childSettings);

                    addActionNodes(childNode, childModule);
                }
            } else {
                // Standalone (non-aggregator) leaf module — add action nodes directly.
                addActionNodes(node, libertyModule);
            }
        }

        // If the only modules in the workspace are child modules (all have parents),
        // the loop above produced an empty tree. Return null so the "no projects" message
        // is shown — this happens transiently during re-scan before the parent is linked.
        if (top.getChildCount() == 0) {
            return null;
        }

        Tree tree = new Tree(top);
        tree.setName(Constants.LIBERTY_TREE);
        tree.setRootVisible(false);
        TreeDataProvider newDataProvider = new TreeDataProvider();
        DataManager.registerDataProvider(tree, newDataProvider);
        TreeDataProvider treeDataProvider = (TreeDataProvider) DataManager.getDataProvider(tree);

        treeDataProvider.setProjectMap(projectMap);

        tree.addTreeSelectionListener(e -> {
            Object node = e.getPath().getLastPathComponent();
            if (node instanceof LibertyModuleNode libertyNode) {
                // open build file (works for both top-level and child module nodes)
                FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, libertyNode.getFilePath()), true);
                treeDataProvider.saveData(libertyNode.getFilePath(), libertyNode.getName(), libertyNode.getProjectType());
            } else if (node instanceof LibertyActionNode) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
                // The action node's parent is always the LibertyModuleNode it belongs to,
                // regardless of whether that module node is top-level or nested under an aggregator.
                javax.swing.tree.TreeNode parentTreeNode = treeNode.getParent();
                if (parentTreeNode instanceof LibertyModuleNode parentNode) {
                    treeDataProvider.saveData(parentNode.getFilePath(), parentNode.getName(), parentNode.getProjectType());
                }
            }
        });

        tree.addMouseListener(new PopupHandler() {
            @Override
            public void invokePopup(Component comp, int x, int y) {
                final TreePath path = tree.getSelectionPath();
                if (path != null) {
                    Object node = path.getLastPathComponent();
                    if (node instanceof LibertyModuleNode libertyNode) {
                        final DefaultActionGroup group = new DefaultActionGroup();
                        if (libertyNode.getProjectType().equals(Constants.ProjectType.LIBERTY_MAVEN_PROJECT)) {
                            AnAction viewPomXml = ActionManager.getInstance().getAction(Constants.VIEW_POM_XML_ACTION_ID);
                            group.add(viewPomXml);
                            AnAction viewIntegrationReport = ActionManager.getInstance().getAction(Constants.VIEW_INTEGRATION_TEST_REPORT_ACTION_ID);
                            group.add(viewIntegrationReport);
                            AnAction viewUnitTestReport = ActionManager.getInstance().getAction(Constants.VIEW_UNIT_TEST_REPORT_ACTION_ID);
                            group.add(viewUnitTestReport);
                            group.addSeparator();
                        } else {
                            AnAction viewGradleConfig = ActionManager.getInstance().getAction(Constants.VIEW_GRADLE_CONFIG_ACTION_ID);
                            group.add(viewGradleConfig);
                            AnAction viewTestReport = ActionManager.getInstance().getAction(Constants.VIEW_GRADLE_TEST_REPORT_ACTION_ID);
                            group.add(viewTestReport);
                            group.addSeparator();
                        }
                        AnAction startAction = ActionManager.getInstance().getAction(Constants.LIBERTY_DEV_START_ACTION_ID);
                        group.add(startAction);
                        if (libertyNode.isValidContainerVersion()) {
                            AnAction customStartAction = ActionManager.getInstance().getAction(Constants.LIBERTY_DEV_START_CONTAINER_ACTION_ID);
                            group.add(customStartAction);
                        }
                        AnAction customStartAction = ActionManager.getInstance().getAction(Constants.LIBERTY_DEV_CUSTOM_START_ACTION_ID);
                        group.add(customStartAction);
                        AnAction stopAction = ActionManager.getInstance().getAction(Constants.LIBERTY_DEV_STOP_ACTION_ID);
                        group.add(stopAction);
                        AnAction runTestsAction = ActionManager.getInstance().getAction(Constants.LIBERTY_DEV_TESTS_ACTION_ID);
                        group.add(runTestsAction);

                        ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, group);

                        menu.setDataContext(() -> SimpleDataContext.builder()
                                .add(CommonDataKeys.PROJECT, libertyNode.getProject())
                                .add(Constants.LIBERTY_BUILD_FILE_DATAKEY, libertyNode.getFilePath()).build());

                        menu.getComponent().show(comp, x, y);
                    }
                }
            }
        });

        DoubleClickListener doubleClickListener = new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent event) {
                executeAction(tree);
                return false;
            }
        };
        doubleClickListener.installOn(tree);

        tree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    executeAction(tree);
                }
            }
        });

        // set tree icons, colours and state-badge renderer
        LibertyTreeRenderer libertyRenderer = new LibertyTreeRenderer(backgroundColor);
        tree.setCellRenderer(libertyRenderer);

        // Enable per-row tooltips — Swing reads setToolTipText() from the renderer component.
        javax.swing.ToolTipManager.sharedInstance().registerComponent(tree);

        return tree;
    }

    /**
     * Appends Liberty action child nodes to the given module tree node.
     * Extracted to avoid duplicating the action-wiring logic for parent and child nodes.
     */
    private static void addActionNodes(LibertyModuleNode node, LibertyModule libertyModule) {
        node.add(new LibertyActionNode(Constants.LIBERTY_DEV_START, libertyModule));
        boolean validContainerVersion = libertyModule.isValidContainerVersion();
        if (validContainerVersion) {
            node.add(new LibertyActionNode(Constants.LIBERTY_DEV_START_CONTAINER, libertyModule));
        }
        node.add(new LibertyActionNode(Constants.LIBERTY_DEV_CUSTOM_START, libertyModule));
        node.add(new LibertyActionNode(Constants.LIBERTY_DEV_STOP, libertyModule));
        node.add(new LibertyActionNode(Constants.LIBERTY_DEV_TESTS, libertyModule));
        if (libertyModule.getProjectType().equals(Constants.ProjectType.LIBERTY_MAVEN_PROJECT)) {
            node.add(new LibertyActionNode(Constants.VIEW_INTEGRATION_TEST_REPORT, libertyModule));
            node.add(new LibertyActionNode(Constants.VIEW_UNIT_TEST_REPORT, libertyModule));
        } else {
            node.add(new LibertyActionNode(Constants.VIEW_GRADLE_TEST_REPORT, libertyModule));
        }
    }

    static class LibertyTreeRenderer extends DefaultTreeCellRenderer {

        public LibertyTreeRenderer(Color backgroundColor) {
            setBackgroundNonSelectionColor(backgroundColor);
        }

        public Component getTreeCellRendererComponent(
                JTree tree,
                Object value,
                boolean sel,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus) {

            // LibertyModuleNode: badge icon (root only) + state icon + state tooltip
            if (value instanceof LibertyModuleNode moduleNode) {
                LibertyModule lm = moduleNode.getLibertyModule();

                // Resolve state icon based on effective AppState
                Icon stateIcon = resolveStateIcon(moduleNode);

                // Build the node icon.
                // Root modules: build-type badge (Maven/Gradle) + state icon side-by-side.
                // Child (sub-project) modules: state icon only.
                final Icon compositeIcon;
                if (lm.getParentModule() == null) {
                    Icon badgeIcon;
                    if (moduleNode.isGradleProjectType()) {
                        badgeIcon = LibertyPluginIcons.gradleIcon;
                    } else if (moduleNode.isMavenProjectType()) {
                        badgeIcon = LibertyPluginIcons.mavenIcon;
                    } else {
                        badgeIcon = LibertyPluginIcons.libertyIcon;
                    }
                    compositeIcon = new CompositeIcon(badgeIcon, stateIcon);
                } else {
                    compositeIcon = stateIcon;
                }

                setOpenIcon(compositeIcon);
                setClosedIcon(compositeIcon);
                setLeafIcon(compositeIcon);
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                setIcon(compositeIcon);
                // State tooltip
                setToolTipText(resolveStateTooltip(lm));
                return this;
            }

            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            // LibertyActionNode (leaf): gear icon
            if (leaf) {
                setIcon(LibertyPluginIcons.IntelliJGear);
                setToolTipText(null);
            }

            return this;
        }

        /**
         * Resolves the effective {@link LibertyModule.AppState} for display.
         * For aggregators, derives a combined state from all children.
         * Returns {@code null} when children are in a mixed state (incomplete).
         */
        private static LibertyModule.AppState resolveEffectiveState(LibertyModule module) {
            List<LibertyModule> children = module.getChildLibertyModules();
            if (children.isEmpty()) {
                return module.getAppState();
            }
            int running = 0, starting = 0, stopping = 0;
            int total = children.size();
            for (LibertyModule child : children) {
                switch (child.getAppState()) {
                    case RUNNING  -> running++;
                    case STARTING -> starting++;
                    case STOPPING -> stopping++;
                    default       -> {} // STOPPED
                }
            }
            if (running + starting + stopping == 0) return LibertyModule.AppState.STOPPED;
            if (starting > 0)                       return LibertyModule.AppState.STARTING;
            if (stopping > 0)                       return LibertyModule.AppState.STOPPING;
            if (running == total)                   return LibertyModule.AppState.RUNNING;
            return null; // mixed / incomplete
        }

        /**
         * Picks the correct state icon
         * STARTING → starting.svg, STOPPING → stopping.svg.
         */
        private static Icon resolveStateIcon(LibertyModuleNode moduleNode) {
            LibertyModule.AppState state = resolveEffectiveState(moduleNode.getLibertyModule());
            if (state == null)                         return LibertyPluginIcons.incompleteIcon();
            return switch (state) {
                case RUNNING  -> LibertyPluginIcons.runningIcon();
                case STARTING -> LibertyPluginIcons.startingIcon();
                case STOPPING -> LibertyPluginIcons.stoppingIcon();
                case STOPPED  -> LibertyPluginIcons.stoppedIcon();
            };
        }

        /**
         * Returns the tooltip text for the state icon.
         * <ul>
         *   <li>Leaf module → simple state name ("Running", "Starting...", etc.)</li>
         *   <li>Aggregator, all stopped → "Stopped"</li>
         *   <li>Aggregator, any starting → "Starting..."</li>
         *   <li>Aggregator, any stopping → "Stopping..."</li>
         *   <li>Aggregator, some/all running → "{N}/{total} running"</li>
         * </ul>
         */
        private static String resolveStateTooltip(LibertyModule module) {
            List<LibertyModule> children = module.getChildLibertyModules();
            if (children.isEmpty()) {
                // Leaf module — simple state label.
                return simpleStateTooltip(module.getAppState());
            }
            // Aggregator — compute counts across children.
            int running = 0, starting = 0, stopping = 0;
            int total = children.size();
            for (LibertyModule child : children) {
                switch (child.getAppState()) {
                    case RUNNING  -> running++;
                    case STARTING -> starting++;
                    case STOPPING -> stopping++;
                    default       -> {}
                }
            }
            if (running + starting + stopping == 0) {
                return LocalizedResourceUtil.getMessage("liberty.dashboard.tooltip.stopped");
            }
            if (starting > 0) {
                return LocalizedResourceUtil.getMessage("liberty.dashboard.tooltip.starting");
            }
            if (stopping > 0) {
                return LocalizedResourceUtil.getMessage("liberty.dashboard.tooltip.stopping");
            }
            // Always show "X/Y running" for aggregators — whether all or only some are running.
            return LocalizedResourceUtil.getMessage("liberty.dashboard.tooltip.modules.running",
                    running, total);
        }

        private static String simpleStateTooltip(LibertyModule.AppState state) {
            if (state == null) return null;
            return switch (state) {
                case RUNNING  -> LocalizedResourceUtil.getMessage("liberty.dashboard.tooltip.running");
                case STARTING -> LocalizedResourceUtil.getMessage("liberty.dashboard.tooltip.starting");
                case STOPPING -> LocalizedResourceUtil.getMessage("liberty.dashboard.tooltip.stopping");
                case STOPPED  -> LocalizedResourceUtil.getMessage("liberty.dashboard.tooltip.stopped");
            };
        }

        /**
         * A lightweight {@link Icon} that paints two icons side-by-side:
         * the build-type badge on the left and the state indicator on the right,
         * separated by a 2 px gap.
         */
        private static class CompositeIcon implements Icon {
            private final Icon left;
            private final Icon right;
            private static final int GAP = 2;

            CompositeIcon(Icon left, Icon right) {
                this.left  = left;
                this.right = right;
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                int leftH  = left.getIconHeight();
                int rightH = right.getIconHeight();
                int totalH = getIconHeight();
                // Vertically center each icon within the composite height
                int leftY  = y + (totalH - leftH)  / 2;
                int rightY = y + (totalH - rightH) / 2;
                left.paintIcon(c, g, x, leftY);
                right.paintIcon(c, g, x + left.getIconWidth() + GAP, rightY);
            }

            @Override public int getIconWidth()  { return left.getIconWidth() + GAP + right.getIconWidth(); }
            @Override public int getIconHeight() { return Math.max(left.getIconHeight(), right.getIconHeight()); }
        }
    }

    private static void executeAction(Tree tree) {
        final TreePath path = tree.getSelectionPath();
        Object node = (path != null) ? path.getLastPathComponent() : null;
        if (node instanceof LibertyActionNode) {
            ActionManager am = ActionManager.getInstance();
            String actionNodeName = ((LibertyActionNode) node).getName();
            LOGGER.debug("Selected: " + actionNodeName);

            // calls action on double click
            String actionId = Constants.FULL_ACTIONS_MAP.get(actionNodeName);
            if (actionId == null) {
                LOGGER.error("Could not find action ID for action name: " + actionNodeName);
            }
            LibertyGeneralAction action = (LibertyGeneralAction) am.getAction(actionId);
            if (action != null) {
                AnActionEvent event = new AnActionEvent(DataManager.getInstance().getDataContext(tree),
                        new Presentation(), ActionPlaces.UNKNOWN, ActionUiKind.NONE, null,
                        0, am);
                ActionUtil.performActionDumbAwareWithCallbacks(action, event);
            }
        }
    }
}
