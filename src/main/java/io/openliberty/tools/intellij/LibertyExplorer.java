/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation.
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
import java.util.ArrayList;
import java.util.HashMap;

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
            LibertyModuleNode node = new LibertyModuleNode(libertyModule);

            top.add(node);
            ArrayList<Object> settings = new ArrayList<Object>();
            settings.add(libertyModule.getBuildFile());
            settings.add(libertyModule.getProjectType());
            projectMap.put(libertyModule.getName(), settings);

            // ordered to align with IntelliJ's right-click menu
            node.add(new LibertyActionNode(Constants.LIBERTY_DEV_START, libertyModule));
            // check if Liberty Maven Plugin is 3.3-M1+ or Liberty Gradle Plugin is 3.1-M1+
            // if version is not specified in pom, assume latest version as downloaded from maven central
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
                // open build file
                FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, libertyNode.getFilePath()), true);
                treeDataProvider.saveData(libertyNode.getFilePath(), libertyNode.getName(), libertyNode.getProjectType());
            } else if (node instanceof LibertyActionNode) {
                DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
                LibertyModuleNode parentNode = (LibertyModuleNode) treeNode.getParent();
                treeDataProvider.saveData(parentNode.getFilePath(), parentNode.getName(), parentNode.getProjectType());
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

        // set tree icons and colours
        LibertyTreeRenderer libertyRenderer = new LibertyTreeRenderer(backgroundColor);
        tree.setCellRenderer(libertyRenderer);
        return tree;
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
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            // assign gear icon to action nodes
            if (leaf) {
                setIcon(LibertyPluginIcons.IntelliJGear);
                return this;
            }

            // select icon for node based on project type
            if (value instanceof LibertyModuleNode) {
                LibertyModuleNode moduleNode = (LibertyModuleNode) value;
                if (moduleNode.isGradleProjectType()) {
                    setIcon(LibertyPluginIcons.gradleIcon);
                } else if (moduleNode.isMavenProjectType()) {
                    setIcon(LibertyPluginIcons.mavenIcon);
                } else {
                    setIcon(LibertyPluginIcons.libertyIcon);
                }
            }

            return this;
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
                action.actionPerformed(new AnActionEvent(null,
                        DataManager.getInstance().getDataContext(tree),
                        ActionPlaces.UNKNOWN, new Presentation(),
                        am, 0));
            }
        }
    }
}
