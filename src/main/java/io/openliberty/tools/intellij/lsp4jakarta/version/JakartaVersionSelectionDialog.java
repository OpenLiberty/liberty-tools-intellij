/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.version;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.List;

/**
 * Dialog used by the Jakarta language server to ask the user which Jakarta EE version to use for a project.
 */
public class JakartaVersionSelectionDialog extends DialogWrapper {

    private final String projectUri;
    private final ComboBox<String> versionCombo;

    /**
     * @param project    the IntelliJ project
     * @param projectUri the URI of the project the version is selected for
     * @param versions   the Jakarta EE versions to choose from, the first one is preselected
     */
    public JakartaVersionSelectionDialog(@Nullable Project project, String projectUri, List<String> versions) {
        super(project);
        this.projectUri = projectUri;
        this.versionCombo = new ComboBox<>(versions.toArray(new String[0]));
        setTitle("Select Jakarta EE Version");
        setOKButtonText("Apply");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JBLabel description = new JBLabel("<html>Select the Jakarta EE version that matches your project's "
                + "dependencies.<br>It is used for validation of the project.</html>");
        return FormBuilder.createFormBuilder()
                .addComponent(new JBLabel("Project: " + getProjectName(projectUri)))
                .addLabeledComponent("Jakarta EE version:", versionCombo)
                .addComponent(description)
                .getPanel();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return versionCombo;
    }

    /**
     * Returns the selected version, only meaningful if the dialog was closed with OK.
     */
    public String getSelectedVersion() {
        return (String) versionCombo.getSelectedItem();
    }

    private static String getProjectName(String uri) {
        if (uri == null || uri.isEmpty()) {
            return "Unknown Project";
        }
        String trimmed = uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
        String name = trimmed.substring(trimmed.lastIndexOf('/') + 1);
        return name.isEmpty() ? uri : name;
    }
}
