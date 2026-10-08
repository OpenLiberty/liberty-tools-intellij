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

import com.intellij.ide.BrowserUtil;
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
import com.intellij.ui.treeStructure.Tree;
import io.openliberty.tools.intellij.actions.LibertyGeneralAction;
import io.openliberty.tools.intellij.actions.LibertyToolbarActionGroup;
import io.openliberty.tools.intellij.starter.LibertyNewProjectWizard;
import io.openliberty.tools.intellij.util.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
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
        buildContent(project);
    }

    /**
     * Refreshes the Liberty tool window content by rebuilding the tree and toolbar.
     * Safe to call from any thread; UI work is dispatched onto the EDT.
     *
     * @param project current project
     */
    public void refresh(@NotNull Project project) {
        buildContent(project);
    }

    /**
     * Builds (or rebuilds) the tree and toolbar asynchronously.
     * Read-actions run on a pooled thread; UI updates run on the EDT.
     */
    private void buildContent(@NotNull Project project) {
        //NOTE: To address the "Slow operations are prohibited on EDT" Exception (https://github.com/OpenLiberty/liberty-tools-intellij/issues/674), we have implemented the workaround outlined in the document (https://plugins.jetbrains.com/docs/intellij/general-threading-rules.html).
        // We have now moved the method "buildTree(project, getBackground())" to a background thread. To pass control from a background thread to the Event Dispatch Thread (EDT), UI operations are now included within the method "ApplicationManager.getApplication().invokeLater()".
        ModalityState modalityState = getModalityState();
        // Capture background colour on the EDT before handing off to the pooled thread.
        Color backgroundColor = getBackground();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // build tree (Read operations need to be wrapped in a read action)
            Tree tree = ApplicationManager.getApplication().runReadAction((Computable<Tree>) () -> buildTree(project, backgroundColor));

            if (tree != null) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    JBScrollPane scrollPane = new JBScrollPane(tree);
                    scrollPane.setName(Constants.LIBERTY_SCROLL_PANE);
                    this.setContent(scrollPane);
                }, modalityState);
            } else {
                ApplicationManager.getApplication().invokeLater(() -> {
                    this.setContent(buildEmptyStatePanel(project, getBackground()));
                }, modalityState);
            }

            ApplicationManager.getApplication().invokeLater(() -> {
                ActionToolbar actionToolbar = buildActionToolbar(tree);
                this.setToolbar(actionToolbar.getComponent());
                this.revalidate();
                this.repaint();
            }, modalityState);
        });
    }

    /** URL for the Liberty Maven plugin CI configuration docs. */
    private static final String MAVEN_PLUGIN_URL = "https://github.com/OpenLiberty/ci.maven/#configuration";

    /** URL for the Liberty Gradle plugin docs. */
    private static final String GRADLE_PLUGIN_URL = "https://github.com/OpenLiberty/ci.gradle#adding-the-plugin-to-the-build-script";

    /** URL for the Liberty server.xml configuration overview. */
    private static final String SERVER_XML_URL = "https://openliberty.io/docs/latest/reference/config/server-configuration-overview.html#server-xml";

    private static JPanel buildEmptyStatePanel(Project project, Color background) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(background);

        JEditorPane introText = createLinkLabel(
                LocalizedResourceUtil.getMessage("no.liberty.projects.detected.intro"));
        adjustHeightForEditorPane(panel, introText);

        JButton openButton = new JButton(LocalizedResourceUtil.getMessage("no.liberty.open.button"));
        openButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        openButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, openButton.getPreferredSize().height));
        openButton.addActionListener(e -> {
            AnAction openAction = ActionManager.getInstance().getAction("OpenFile");
            if (openAction != null) {
                AnActionEvent event = AnActionEvent.createEvent(
                        openAction,
                        DataManager.getInstance().getDataContext(openButton),
                        null, ActionPlaces.UNKNOWN, ActionUiKind.NONE, null);
                ActionUtil.performAction(openAction, event);
            }
        });
        panel.add(openButton);

        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        JButton createButton = new JButton(LocalizedResourceUtil.getMessage("no.liberty.create.button"));
        createButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        createButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, createButton.getPreferredSize().height));
        createButton.addActionListener(e -> LibertyNewProjectWizard.show(project));
        panel.add(createButton);

        panel.add(Box.createRigidArea(new Dimension(0, 8)));

        JEditorPane restText = createLinkLabel(
                LocalizedResourceUtil.getMessage("no.liberty.projects.detected.rest",
                        MAVEN_PLUGIN_URL, GRADLE_PLUGIN_URL, SERVER_XML_URL));
        adjustHeightForEditorPane(panel, restText);

        JButton addProjectButton = new JButton(LocalizedResourceUtil.getMessage("no.liberty.add.project.button"));
        addProjectButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        addProjectButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, addProjectButton.getPreferredSize().height));
        addProjectButton.addActionListener(e -> {
            AnAction addAction = ActionManager.getInstance().getAction(
                    "io.openliberty.tools.intellij.actions.AddLibertyProjectAction");
            if (addAction != null) {
                AnActionEvent event = AnActionEvent.createEvent(
                        addAction,
                        DataManager.getInstance().getDataContext(addProjectButton),
                        null, ActionPlaces.UNKNOWN, ActionUiKind.NONE, null);
                ActionUtil.performAction(addAction, event);
            }
        });
        panel.add(addProjectButton);

        return panel;
    }

    /**
     * Creates a non-editable HTML editor pane with a hyperlink listener that opens
     * links in the system browser. Mirrors the pattern used in AccessKeyConfigurable.
     *
     * @param htmlText the HTML content (without outer {@code <html>} wrapper)
     * @return a configured {@link JEditorPane}
     */
    private static JEditorPane createLinkLabel(String htmlText) {
        String html = "<html><body>" + htmlText + "</body></html>";
        JEditorPane editorPane = new JEditorPane("text/html", html);
        editorPane.setEditable(false);
        editorPane.setOpaque(false);
        editorPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        editorPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                BrowserUtil.browse(e.getURL().toString());
            }
        });
        return editorPane;
    }

    private static void adjustHeightForEditorPane(JPanel panel, JEditorPane pane) {
        // Override getMaximumSize() so BoxLayout always uses the actual preferred
        // height after the component has been given its real width by the layout pass.
        JEditorPane sized = new JEditorPane(pane.getContentType(), pane.getText()) {
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        sized.setEditable(false);
        sized.setOpaque(false);
        sized.setAlignmentX(Component.LEFT_ALIGNMENT);
        sized.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        for (javax.swing.event.HyperlinkListener l : pane.getHyperlinkListeners()) {
            sized.addHyperlinkListener(l);
        }
        panel.add(sized);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
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
     * Builds the Liberty Tools Dashboard tree
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
        TreeDataProvider treeDataProvider = new TreeDataProvider();
        UiDataProvider.wrapComponent(tree, treeDataProvider);
        tree.putClientProperty(Constants.LIBERTY_TREE_DATA_PROVIDER_KEY, treeDataProvider);

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
                DataContext dataContext = buildDataContext(tree);
                AnActionEvent event = new AnActionEvent(dataContext,
                        new Presentation(), ActionPlaces.UNKNOWN, ActionUiKind.NONE, null,
                        0, am);
                ActionUtil.performActionDumbAwareWithCallbacks(action, event);
            }
        }
    }

    /**
     * Builds a DataContext for the given tree that includes data from the TreeDataProvider
     * stored as a client property. This is necessary because UiDataProvider.wrapComponent
     * is not resolved by DataManager.getDataContext(component).
     */
    public static DataContext buildDataContext(Tree tree) {
        TreeDataProvider provider = (TreeDataProvider) tree.getClientProperty(Constants.LIBERTY_TREE_DATA_PROVIDER_KEY);
        if (provider == null) {
            return DataManager.getInstance().getDataContext(tree);
        }
        return SimpleDataContext.builder()
                .setParent(DataManager.getInstance().getDataContext(tree))
                .add(Constants.LIBERTY_BUILD_FILE_DATAKEY, provider.currentFile)
                .add(Constants.LIBERTY_PROJECT_NAME, provider.projectName)
                .add(Constants.LIBERTY_PROJECT_TYPE, provider.projectType)
                .add(Constants.LIBERTY_PROJECT_MAP, provider.map)
                .build();
    }
}
