package org.liberty.intellij;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;
import org.liberty.intellij.util.*;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class LibertyExplorer extends SimpleToolWindowPanel {
    private Project currentProject;

    public LibertyExplorer(@NotNull Project project) {
        super(true, true);

        // create ActionToolBar
        final ActionManager actionManager = ActionManager.getInstance();
        DefaultActionGroup actionGroup = new DefaultActionGroup("DefaultActionGroup", false);
        actionGroup.add(ActionManager.getInstance().getAction("org.liberty.intellij.actions.RefreshLibertyToolbar"));
        ActionToolbar actionToolbar = actionManager.createActionToolbar(ActionPlaces.UNKNOWN, actionGroup, true);
        actionToolbar.setOrientation(SwingConstants.HORIZONTAL);
        this.setToolbar(actionToolbar.getComponent());

        // build tree
        Tree tree = buildTree(project, getBackground());
        setContent(tree);
    }

    public static Tree buildTree(Project project, Color backgroundColor) {
        String projectName = null;
        ArrayList<PsiFile> buildFiles = null;
        ArrayList<PsiFile> mavenBuildFiles = null;
        ArrayList<PsiFile> gradleBuildFiles = null;
        try {
            mavenBuildFiles = LibertyProjectUtil.getMavenBuildFiles(project);
            gradleBuildFiles = LibertyProjectUtil.getGradleBuildFiles(project);
        } catch (Exception e) {
            e.printStackTrace();
        }

        DefaultMutableTreeNode top = new DefaultMutableTreeNode("Root node");

        for (PsiFile file : mavenBuildFiles) {
            VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile == null) {
                throw new Error("Could not resolve current project");
            }
            LibertyProjectNode node;
            try {
                projectName = LibertyMavenUtil.getProjectNameFromPom(virtualFile);

            } catch (Exception e) {
                throw new Error("Could not resolve project name from pom.xml", e);
            }
            if (projectName != null) {
                node = new LibertyProjectNode(file, projectName, Constants.LIBERTY_MAVEN_PROJECT);
            } else {
                node = new LibertyProjectNode(file, project.getName(), Constants.LIBERTY_MAVEN_PROJECT);
            }
            top.add(node);
            node.add(new LibertyActionNode(Constants.LIBERTY_DEV_START));
            node.add(new LibertyActionNode(Constants.LIBERTY_DEV_CUSTOM_START));
            node.add(new LibertyActionNode(Constants.LIBERTY_DEV_STOP));
            node.add(new LibertyActionNode(Constants.LIBERTY_DEV_TESTS));
            node.add(new LibertyActionNode(Constants.VIEW_INTEGRATION_TEST_REPORT));
            node.add(new LibertyActionNode(Constants.VIEW_UNIT_TEST_REPORT));
        }

        for (PsiFile file : gradleBuildFiles) {
            VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile == null) {
                throw new Error("Could not resolve current project");
            }
            LibertyProjectNode node;
            try {
                projectName = LibertyGradleUtil.getProjectName(virtualFile);
            } catch (Exception e) {
                throw new Error("Could not resolve project name from settings.gradle", e);
            }
            if (projectName != null) {
                node = new LibertyProjectNode(file, projectName, Constants.LIBERTY_GRADLE_PROJECT);
            } else {
                node = new LibertyProjectNode(file, project.getName(), Constants.LIBERTY_GRADLE_PROJECT);
            }
            top.add(node);
            node.add(new LibertyActionNode(Constants.LIBERTY_DEV_START));

            node.add(new LibertyActionNode(Constants.LIBERTY_DEV_CUSTOM_START));
            node.add(new LibertyActionNode(Constants.LIBERTY_DEV_STOP));
            node.add(new LibertyActionNode(Constants.LIBERTY_DEV_TESTS));
            node.add(new LibertyActionNode(Constants.VIEW_TEST_REPORT));
        }

        Tree tree = new Tree(top);
        tree.setName(Constants.LIBERTY_TREE);
        tree.setRootVisible(false);

        TreeDataProvider newDataProvider = new TreeDataProvider();
        DataManager.registerDataProvider(tree, newDataProvider);
        TreeDataProvider treeDataProvider = (TreeDataProvider) DataManager.getDataProvider(tree);

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
                            AnAction viewEffectivePom = ActionManager.getInstance().getAction("org.liberty.intellij.actions.ViewEffectivePom");
                            group.add(viewEffectivePom);
                            AnAction viewIntegrationReport = ActionManager.getInstance().getAction("org.liberty.intellij.actions.ViewIntegrationTestReport");
                            group.add(viewIntegrationReport);
                            AnAction viewUnitTestReport = ActionManager.getInstance().getAction("org.liberty.intellij.actions.ViewUnitTestReport");
                            group.add(viewUnitTestReport);
                            group.addSeparator();
                        } else if (libertyNode.getProjectType().equals(Constants.LIBERTY_GRADLE_PROJECT)) {
                            AnAction viewGradleConfig = ActionManager.getInstance().getAction("org.liberty.intellij.actions.ViewGradleConfig");
                            group.add(viewGradleConfig);
                            AnAction viewTestReport = ActionManager.getInstance().getAction("org.liberty.intellij.actions.ViewTestReport");
                            group.add(viewTestReport);
                            group.addSeparator();
                        }
                        AnAction startAction = ActionManager.getInstance().getAction("org.liberty.intellij.actions.LibertyDevStartAction");
                        group.add(startAction);
                        AnAction customStartAction = ActionManager.getInstance().getAction("org.liberty.intellij.actions.LibertyDevCustomStartAction");
                        group.add(customStartAction);
                        AnAction stopAction = ActionManager.getInstance().getAction("org.liberty.intellij.actions.LibertyDevStopAction");
                        group.add(stopAction);
                        AnAction runTestsAction = ActionManager.getInstance().getAction("org.liberty.intellij.actions.LibertyDevRunTestsAction");
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
                final TreePath path = tree.getSelectionPath();
                Object node = path.getLastPathComponent();
                if (node instanceof LibertyActionNode) {
                    ActionManager am = ActionManager.getInstance();
                    String actionNodeName = ((LibertyActionNode) node).getName();
                    if (actionNodeName.equals(Constants.LIBERTY_DEV_START)) {
                        // calls action on double click
                        am.getAction("org.liberty.intellij.actions.LibertyDevStartAction").actionPerformed(new AnActionEvent(null, DataManager.getInstance().getDataContext(),
                                ActionPlaces.UNKNOWN, new Presentation(),
                                ActionManager.getInstance(), 0));
                    } else if (actionNodeName.equals(Constants.LIBERTY_DEV_CUSTOM_START)) {
                        am.getAction("org.liberty.intellij.actions.LibertyDevCustomStartAction").actionPerformed(new AnActionEvent(null, DataManager.getInstance().getDataContext(),
                                ActionPlaces.UNKNOWN, new Presentation(),
                                ActionManager.getInstance(), 0));
                    } else if (actionNodeName.equals(Constants.LIBERTY_DEV_STOP)) {
                        am.getAction("org.liberty.intellij.actions.LibertyDevStopAction").actionPerformed(new AnActionEvent(null, DataManager.getInstance().getDataContext(),
                                ActionPlaces.UNKNOWN, new Presentation(),
                                ActionManager.getInstance(), 0));
                    } else if (actionNodeName.equals(Constants.LIBERTY_DEV_TESTS)) {
                        am.getAction("org.liberty.intellij.actions.LibertyDevRunTestsAction").actionPerformed(new AnActionEvent(null, DataManager.getInstance().getDataContext(),
                                ActionPlaces.UNKNOWN, new Presentation(),
                                ActionManager.getInstance(), 0));
                    } else if (actionNodeName.equals(Constants.VIEW_INTEGRATION_TEST_REPORT)) {
                        am.getAction("org.liberty.intellij.actions.ViewIntegrationTestReport").actionPerformed(new AnActionEvent(null, DataManager.getInstance().getDataContext(),
                                ActionPlaces.UNKNOWN, new Presentation(),
                                ActionManager.getInstance(), 0));
                    } else if (actionNodeName.equals(Constants.VIEW_UNIT_TEST_REPORT)) {
                        am.getAction("org.liberty.intellij.actions.ViewUnitTestReport").actionPerformed(new AnActionEvent(null, DataManager.getInstance().getDataContext(),
                                ActionPlaces.UNKNOWN, new Presentation(),
                                ActionManager.getInstance(), 0));
                    } else if (actionNodeName.equals(Constants.VIEW_TEST_REPORT)) {
                        am.getAction("org.liberty.intellij.actions.ViewTestReport").actionPerformed(new AnActionEvent(null, DataManager.getInstance().getDataContext(),
                                ActionPlaces.UNKNOWN, new Presentation(),
                                ActionManager.getInstance(), 0));
                    }
                }
                return false;
            }
        };
        doubleClickListener.installOn(tree);

        // set tree icons and colours
        DefaultTreeCellRenderer newRenderer = new DefaultTreeCellRenderer();
        newRenderer.setLeafIcon(IconLoader.getIcon("AllIcons.General.GearPlain"));
        newRenderer.setClosedIcon(IconLoader.getIcon("/icons/OL_logo_13.svg"));
        newRenderer.setOpenIcon(IconLoader.getIcon("/icons/OL_logo_13.svg"));
        newRenderer.setBackgroundNonSelectionColor(backgroundColor);

        tree.setCellRenderer(newRenderer);
        return tree;
    }


}
