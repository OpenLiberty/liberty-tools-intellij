/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation.
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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.treeStructure.Tree;
import io.openliberty.tools.intellij.actions.LibertyToolbarActionGroup;
import io.openliberty.tools.intellij.util.*;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class LibertyExplorer extends SimpleToolWindowPanel {
    private static Logger log = Logger.getInstance(LibertyExplorer.class);

    public LibertyExplorer(@NotNull Project project) {
        super(true, true);
        // build tree
        Tree tree = buildTree(project, getBackground());

        if (tree != null) {
            this.setContent(tree);
        } else {
            JBTextArea jbTextArea = new JBTextArea(LocalizedResourceUtil.getMessage("no.liberty.projects.detected"));
            jbTextArea.setEditable(false);
            jbTextArea.setBackground(getBackground());
            jbTextArea.setLineWrap(true);

            this.setContent(jbTextArea);
        }
        ActionToolbar actionToolbar = buildActionToolbar(tree);
        this.setToolbar(actionToolbar.getComponent());
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
     * @param project current project
     * @param backgroundColor
     * @return Tree object of all valid Liberty Gradle and Liberty Maven projects
     */
    public static Tree buildTree(Project project, Color backgroundColor) {
        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Root node");

        ArrayList<BuildFile> mavenBuildFiles;
        ArrayList<BuildFile> gradleBuildFiles;
        ArrayList<String> projectNames = new ArrayList<String>();
        HashMap<String, ArrayList<Object>> map = new HashMap<String, ArrayList<Object>>();
        try {
            mavenBuildFiles = LibertyProjectUtil.getMavenBuildFiles(project);
            gradleBuildFiles = LibertyProjectUtil.getGradleBuildFiles(project);
        } catch (IOException | SAXException | ParserConfigurationException e) {
            log.error("Could not find Open Liberty Maven or Gradle projects in workspace",
                    e.getMessage());
            return null;
        }

        if (mavenBuildFiles.isEmpty() && gradleBuildFiles.isEmpty()) {
            return null;
        }

        for (BuildFile buildFile : mavenBuildFiles) {
            PsiFile psiFile = buildFile.getBuildFile();
            String projectName = null;
            VirtualFile virtualFile = psiFile.getVirtualFile();
            if (virtualFile == null) {
                log.error("Could not resolve current Maven project");
            }
            LibertyProjectNode node;
            try {
                projectName = LibertyMavenUtil.getProjectNameFromPom(virtualFile);
            } catch (Exception e) {
                log.error("Could not resolve project name from pom.xml", e.getMessage());
            }
            log.info("Liberty Maven Project: " + psiFile);
            if (projectName == null) {
                projectName = project.getName();
            }
            boolean validContainerVersion = buildFile.isValidContainerVersion();
            node = new LibertyProjectNode(psiFile, projectName, Constants.LIBERTY_MAVEN_PROJECT, validContainerVersion);

            top.add(node);
            projectNames.add(projectName);
            ArrayList<Object> settings = new ArrayList<Object>();
            settings.add(virtualFile);
            settings.add(Constants.LIBERTY_MAVEN_PROJECT);
            map.put(projectName, settings);
            node.add(new LibertyActionNode(Constants.LIBERTY_DEV_START));
            node.add(new LibertyActionNode(Constants.LIBERTY_DEV_CUSTOM_START));

            // check if Liberty Maven Plugin is 3.3-M1+
            // if version is not specified in pom, assume latest version as downloaded from maven central
            if (validContainerVersion){
                node.add(new LibertyActionNode(Constants.LIBERTY_DEV_START_CONTAINER));
            }

            node.add(new LibertyActionNode(Constants.LIBERTY_DEV_STOP));
            node.add(new LibertyActionNode(Constants.LIBERTY_DEV_TESTS));
            node.add(new LibertyActionNode(Constants.VIEW_INTEGRATION_TEST_REPORT));
            node.add(new LibertyActionNode(Constants.VIEW_UNIT_TEST_REPORT));
        }

        for (BuildFile buildFile : gradleBuildFiles) {
            PsiFile psiFile = buildFile.getBuildFile();
            String projectName = null;
            VirtualFile virtualFile = psiFile.getVirtualFile();
            if (virtualFile == null) {
                log.error("Could not resolve current Gradle project");
            }
            LibertyProjectNode node;
            try {
                projectName = LibertyGradleUtil.getProjectName(virtualFile);
            } catch (Exception e) {
                log.error("Could not resolve project name from settings.gradle", e.getMessage());
            }
            log.info("Liberty Gradle Project: " + psiFile);
            if (projectName == null) {
                projectName = project.getName();
            }
            node = new LibertyProjectNode(psiFile, project.getName(), Constants.LIBERTY_GRADLE_PROJECT, buildFile.isValidContainerVersion());

            top.add(node);
            projectNames.add(projectName);
            ArrayList<Object> settings = new ArrayList<Object>();
            settings.add(virtualFile);
            settings.add(Constants.LIBERTY_GRADLE_PROJECT);
            map.put(projectName, settings);
            node.add(new LibertyActionNode(Constants.LIBERTY_DEV_START));
            node.add(new LibertyActionNode(Constants.LIBERTY_DEV_CUSTOM_START));

            // check if Liberty Gradle Plugin is 3.1-M1+
            // TODO: handle version specified in a gradle.settings file
            if (buildFile.isValidContainerVersion()) {
                node.add(new LibertyActionNode(Constants.LIBERTY_DEV_START_CONTAINER));
            }

            node.add(new LibertyActionNode(Constants.LIBERTY_DEV_STOP));
            node.add(new LibertyActionNode(Constants.LIBERTY_DEV_TESTS));
            node.add(new LibertyActionNode(Constants.VIEW_GRADLE_TEST_REPORT));
        }

        Tree tree = new Tree(top);
        tree.setName(Constants.LIBERTY_TREE);
        tree.setRootVisible(false);

        TreeDataProvider newDataProvider = new TreeDataProvider();
        DataManager.registerDataProvider(tree, newDataProvider);
        TreeDataProvider treeDataProvider = (TreeDataProvider) DataManager.getDataProvider(tree);

        treeDataProvider.setProjectMap(map);

        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                Object node = e.getPath().getLastPathComponent();
                if (node instanceof LibertyProjectNode) {
                    LibertyProjectNode libertyNode = (LibertyProjectNode) node;
                    // open build file
                    FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, libertyNode.getFilePath()), true);
                    treeDataProvider.saveData(libertyNode.getFilePath(), libertyNode.getName(), libertyNode.getProjectType());
                } else if (node instanceof LibertyActionNode) {
                    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) node;
                    LibertyProjectNode parentNode = (LibertyProjectNode) treeNode.getParent();
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
                    if (node instanceof LibertyProjectNode) {
                        LibertyProjectNode libertyNode = ((LibertyProjectNode) node);
                        final DefaultActionGroup group = new DefaultActionGroup();
                        if (libertyNode.getProjectType().equals(Constants.LIBERTY_MAVEN_PROJECT)) {
                            AnAction viewEffectivePom = ActionManager.getInstance().getAction(Constants.VIEW_EFFECTIVE_POM_ACTION_ID);
                            group.add(viewEffectivePom);
                            AnAction viewIntegrationReport = ActionManager.getInstance().getAction(Constants.VIEW_INTEGRATION_TEST_REPORT_ACTION_ID);
                            group.add(viewIntegrationReport);
                            AnAction viewUnitTestReport = ActionManager.getInstance().getAction(Constants.VIEW_UNIT_TEST_REPORT_ACTION_ID);
                            group.add(viewUnitTestReport);
                            group.addSeparator();
                        } else if (libertyNode.getProjectType().equals(Constants.LIBERTY_GRADLE_PROJECT)) {
                            AnAction viewGradleConfig = ActionManager.getInstance().getAction(Constants.VIEW_GRADLE_CONFIG_ACTION_ID);
                            group.add(viewGradleConfig);
                            AnAction viewTestReport = ActionManager.getInstance().getAction(Constants.VIEW_GRADLE_TEST_REPORT_ACTION_ID);
                            group.add(viewTestReport);
                            group.addSeparator();
                        }
                        AnAction startAction = ActionManager.getInstance().getAction(Constants.LIBERTY_DEV_START_ACTION_ID);
                        group.add(startAction);
                        if (libertyNode.isValidContainerVersion()){
                            AnAction customStartAction = ActionManager.getInstance().getAction(Constants.LIBERTY_DEV_START_CONTAINER_ACTION_ID);
                            group.add(customStartAction);
                        }
                        AnAction customStartAction = ActionManager.getInstance().getAction(Constants.LIBERTY_DEV_CUSTOM_START_ACTION_ID);
                        group.add(customStartAction);
                        AnAction stopAction = ActionManager.getInstance().getAction(Constants.LIBERTY_DEV_STOP_ACTION_ID);
                        group.add(stopAction);
                        AnAction runTestsAction = ActionManager.getInstance().getAction(Constants.LIBERTY_DEV_TESTS_ACTION_ID);
                        group.add(runTestsAction);

                        ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN, group);
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
                if (e.getKeyCode()==KeyEvent.VK_ENTER) {
                    executeAction(tree);
                }
            }
        });

        // set tree icons and colours
        DefaultTreeCellRenderer newRenderer = new DefaultTreeCellRenderer();

        newRenderer.setLeafIcon(LibertyPluginIcons.IntelliJGear);
        newRenderer.setClosedIcon(LibertyPluginIcons.libertyIcon);
        newRenderer.setOpenIcon(LibertyPluginIcons.libertyIcon);
        newRenderer.setBackgroundNonSelectionColor(backgroundColor);

        tree.setCellRenderer(newRenderer);

        return tree;
    }

    private static void executeAction(Tree tree) {
        final TreePath path = tree.getSelectionPath();
        Object node = path.getLastPathComponent();
        if (node instanceof LibertyActionNode) {
            ActionManager am = ActionManager.getInstance();
            String actionNodeName = ((LibertyActionNode) node).getName();
            log.debug("Selected: " + actionNodeName);
            if (actionNodeName.equals(Constants.LIBERTY_DEV_START)) {
                // calls action on double click
                am.getAction(Constants.LIBERTY_DEV_START_ACTION_ID).actionPerformed(new AnActionEvent(null, DataManager.getInstance().getDataContext(),
                        ActionPlaces.UNKNOWN, new Presentation(),
                        ActionManager.getInstance(), 0));
            } else if (actionNodeName.equals(Constants.LIBERTY_DEV_START_CONTAINER)) {
                am.getAction(Constants.LIBERTY_DEV_START_CONTAINER_ACTION_ID).actionPerformed(new AnActionEvent(null, DataManager.getInstance().getDataContext(),
                        ActionPlaces.UNKNOWN, new Presentation(),
                        ActionManager.getInstance(), 0));
            } else if (actionNodeName.equals(Constants.LIBERTY_DEV_CUSTOM_START)) {
                am.getAction(Constants.LIBERTY_DEV_CUSTOM_START_ACTION_ID).actionPerformed(new AnActionEvent(null, DataManager.getInstance().getDataContext(),
                        ActionPlaces.UNKNOWN, new Presentation(),
                        ActionManager.getInstance(), 0));
            } else if (actionNodeName.equals(Constants.LIBERTY_DEV_STOP)) {
                am.getAction(Constants.LIBERTY_DEV_STOP_ACTION_ID).actionPerformed(new AnActionEvent(null, DataManager.getInstance().getDataContext(),
                        ActionPlaces.UNKNOWN, new Presentation(),
                        ActionManager.getInstance(), 0));
            } else if (actionNodeName.equals(Constants.LIBERTY_DEV_TESTS)) {
                am.getAction(Constants.LIBERTY_DEV_TESTS_ACTION_ID).actionPerformed(new AnActionEvent(null, DataManager.getInstance().getDataContext(),
                        ActionPlaces.UNKNOWN, new Presentation(),
                        ActionManager.getInstance(), 0));
            } else if (actionNodeName.equals(Constants.VIEW_INTEGRATION_TEST_REPORT)) {
                am.getAction(Constants.VIEW_INTEGRATION_TEST_REPORT_ACTION_ID).actionPerformed(new AnActionEvent(null, DataManager.getInstance().getDataContext(),
                        ActionPlaces.UNKNOWN, new Presentation(),
                        ActionManager.getInstance(), 0));
            } else if (actionNodeName.equals(Constants.VIEW_UNIT_TEST_REPORT)) {
                am.getAction(Constants.VIEW_UNIT_TEST_REPORT_ACTION_ID).actionPerformed(new AnActionEvent(null, DataManager.getInstance().getDataContext(),
                        ActionPlaces.UNKNOWN, new Presentation(),
                        ActionManager.getInstance(), 0));
            } else if (actionNodeName.equals(Constants.VIEW_GRADLE_TEST_REPORT)) {
                am.getAction(Constants.VIEW_GRADLE_TEST_REPORT_ACTION_ID).actionPerformed(new AnActionEvent(null, DataManager.getInstance().getDataContext(),
                        ActionPlaces.UNKNOWN, new Presentation(),
                        ActionManager.getInstance(), 0));
            }
        }
    }

}
