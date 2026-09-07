/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import io.openliberty.tools.intellij.LibertyModule;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Assisted by IBM Bob
 *
 * A modal dialog that lets the user choose a single child Liberty module from a
 * multi-module aggregator project.
 *
 * <p>Use {@link #show()} to display the dialog and {@link #getSelectedModule()} to
 * retrieve the user's choice. {@link #getSelectedModule()} returns {@code null} when
 * the user closes the dialog without making a selection (Cancel / ESC).</p>
 */
public class LibertyModuleSelectionDialog extends DialogWrapper {

    private final List<LibertyModule> modules;
    private final String message;
    private JBList<String> moduleList;

    /**
     * Creates the dialog.
     *
     * @param project  The current IntelliJ project (used to anchor the dialog).
     * @param title    The dialog window title.
     * @param message  A short description displayed above the list.
     * @param modules  The child modules to display.
     */
    public LibertyModuleSelectionDialog(@NotNull Project project,
                                        @NotNull String title,
                                        @NotNull String message,
                                        @NotNull List<LibertyModule> modules) {
        super(project, true);
        this.modules = modules;
        this.message = message;
        setTitle(title);
        setOKButtonText(LocalizedResourceUtil.getMessage("liberty.multimodule.selection.dialog.ok.button"));
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        // Build a string list model from the module names.
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (LibertyModule module : modules) {
            listModel.addElement(buildDisplayName(module));
        }

        moduleList = new JBList<>(listModel);
        moduleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        moduleList.setSelectedIndex(0);
        // Enable OK when selection is non-empty; double-click confirms.
        moduleList.addListSelectionListener(e -> setOKActionEnabled(!moduleList.isSelectionEmpty()));
        moduleList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && !moduleList.isSelectionEmpty()) {
                    doOKAction();
                }
            }
        });

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setPreferredSize(new Dimension(400, 220));

        JLabel label = new JLabel(message);
        panel.add(label, BorderLayout.NORTH);
        panel.add(new JBScrollPane(moduleList), BorderLayout.CENTER);

        return panel;
    }

    /**
     * Returns the Liberty module selected by the user, or {@code null} when the
     * user cancelled (pressed Cancel or closed the dialog without confirming).
     */
    @Nullable
    public LibertyModule getSelectedModule() {
        if (getExitCode() != OK_EXIT_CODE) {
            return null;
        }
        int index = moduleList.getSelectedIndex();
        if (index < 0 || index >= modules.size()) {
            return null;
        }
        return modules.get(index);
    }

    /**
     * Builds a display string for a module.
     * Appends the parent-relative directory name when it differs from the module name
     * so users can distinguish identically-named modules in different subdirectories.
     */
    private String buildDisplayName(LibertyModule module) {
        String name = module.getName();
        if (module.getBuildFile() != null && module.getBuildFile().getParent() != null) {
            String dir = module.getBuildFile().getParent().getName();
            if (!dir.equals(name)) {
                return name + "  (" + dir + ")";
            }
        }
        return name;
    }
}
