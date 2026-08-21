/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.starter;

import com.intellij.ide.wizard.AbstractNewProjectWizardStep;
import com.intellij.ide.wizard.NewProjectWizardStep;
import com.intellij.openapi.observable.properties.GraphProperty;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.dsl.builder.Panel;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentListener;
import java.awt.*;

/**
 * New-project-wizard step that collects Open Liberty Starter parameters.
 *
 * <p>Field defaults and combo-box options are initialised from the live
 * {@code GET /api/start/info} response. Hard-coded fallbacks are used when
 * the network is unavailable.</p>
 */
public class LibertyNewProjectWizardStep extends AbstractNewProjectWizardStep {

    // ── Fallback constants (used when /info is unreachable) ───────────────────

    private static final String   DEFAULT_GROUP    = "com.demo";
    private static final String   DEFAULT_ARTIFACT = "app-name";
    private static final String   DEFAULT_BUILD    = "maven";
    private static final String   DEFAULT_JAVA     = "21";
    private static final String   DEFAULT_EE       = "11.0";
    private static final String   DEFAULT_MP       = "7.1";
    private static final String[] FALLBACK_BUILD   = {"maven", "gradle"};
    private static final String[] FALLBACK_JAVA    = {"21", "17", "11", "8"};
    private static final String[] FALLBACK_EE      = {"11.0", "10.0", "9.1", "8.0", "7.0", "None"};
    private static final String[] FALLBACK_MP      = {"7.1", "7.0", "6.1", "6.0", "5.0", "4.1", "3.3", "2.2", "1.4", "None"};

    // ── Observable String properties ──────────────────────────────────────────

    private final GraphProperty<String> groupProp;
    private final GraphProperty<String> artifactProp;
    private final GraphProperty<String> buildProp;
    private final GraphProperty<String> javaProp;
    private final GraphProperty<String> eeProp;
    private final GraphProperty<String> mpProp;

    // ── Data from the API ─────────────────────────────────────────────────────

    private final LibertyNewProjectWizard.StarterInfo starterInfo;

    public LibertyNewProjectWizardStep(@NotNull NewProjectWizardStep parent) {
        super(parent);

        // Fetch live info from the Starter API; falls back to null on failure.
        starterInfo = LibertyNewProjectWizard.fetchStarterInfo();

        groupProp    = getPropertyGraph().property(info() != null ? info().defaultGroup    : DEFAULT_GROUP);
        artifactProp = getPropertyGraph().property(info() != null ? info().defaultArtifact : DEFAULT_ARTIFACT);
        buildProp    = getPropertyGraph().property(info() != null ? info().defaultBuild    : DEFAULT_BUILD);
        javaProp     = getPropertyGraph().property(info() != null ? info().defaultJava     : DEFAULT_JAVA);
        eeProp       = getPropertyGraph().property(info() != null ? info().defaultEe       : DEFAULT_EE);
        mpProp       = getPropertyGraph().property(info() != null ? info().defaultMp       : DEFAULT_MP);
    }

    private LibertyNewProjectWizard.StarterInfo info() {
        return starterInfo;
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    @Override
    public void setupUI(@NotNull Panel builder) {
        builder.row("Group:", row -> {
            JBTextField groupField = new JBTextField(groupProp.get(), 25);
            groupField.getDocument().addDocumentListener(textListener(() -> groupProp.set(groupField.getText())));
            row.cell(groupField);
            return null;
        });

        builder.row("Artifact:", row -> {
            JBTextField artifactField = new JBTextField(artifactProp.get(), 25);
            artifactField.getDocument().addDocumentListener(textListener(() -> artifactProp.set(artifactField.getText())));
            row.cell(artifactField);
            return null;
        });

        builder.row("Build Tool:", row -> {
            String[] opts = info() != null ? info().buildOptions : FALLBACK_BUILD;
            row.cell(buildToggleGroup(opts, buildProp.get(), buildProp::set));
            return null;
        });

        builder.row("Java SE Version:", row -> {
            String[] javaOpts = info() != null ? info().javaOptions : FALLBACK_JAVA;
            JComboBox<String> javaCombo = new JComboBox<>(javaOpts);
            javaCombo.setSelectedItem(javaProp.get());
            javaCombo.addActionListener(e -> javaProp.set((String) javaCombo.getSelectedItem()));
            row.cell(javaCombo);

            row.label("Jakarta EE Version:");
            String[] eeOpts = info() != null ? info().eeOptions : FALLBACK_EE;
            JComboBox<String> eeCombo = new JComboBox<>(eeOpts);
            eeCombo.setSelectedItem(eeProp.get());
            eeCombo.addActionListener(e -> eeProp.set((String) eeCombo.getSelectedItem()));
            row.cell(eeCombo);

            row.label("MicroProfile Version:");
            String[] mpOpts = info() != null ? info().mpOptions : FALLBACK_MP;
            JComboBox<String> mpCombo = new JComboBox<>(mpOpts);
            mpCombo.setSelectedItem(mpProp.get());
            mpCombo.addActionListener(e -> mpProp.set((String) mpCombo.getSelectedItem()));
            row.cell(mpCombo);
            return null;
        });
    }

    // ── Project generation ────────────────────────────────────────────────────

    @Override
    public void setupProject(@NotNull Project project) {
        LibertyNewProjectWizard.downloadAndExtract(
                project,
                getContext().getProjectDirectory().toString(),
                groupProp.get(),
                artifactProp.get(),
                buildProp.get(),
                javaProp.get(),
                eeProp.get(),
                mpProp.get()
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Builds a segmented-button-style toggle group from plain Swing components.
     * Visually matches the IntelliJ "Build system" selector (Maven / Gradle).
     */
    private static JPanel buildToggleGroup(String[] options, String selected,
                                           java.util.function.Consumer<String> onSelect) {
        JPanel panel = new JPanel(new GridLayout(1, options.length, 0, 0));
        panel.setOpaque(false);
        ButtonGroup group = new ButtonGroup();

        for (final String value : options) {
            JToggleButton btn = new JToggleButton(capitalize(value));
            btn.setSelected(value.equals(selected));
            btn.setFocusPainted(false);

            btn.addActionListener(e -> onSelect.accept(value));
            group.add(btn);
            panel.add(btn);
        }
        return panel;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static DocumentListener textListener(Runnable onChange) {
        return new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e)  { onChange.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e)  { onChange.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
        };
    }
}
