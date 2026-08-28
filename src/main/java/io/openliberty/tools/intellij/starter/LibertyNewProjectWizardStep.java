/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation.
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
import com.intellij.openapi.Disposable;
import com.intellij.openapi.observable.properties.GraphProperty;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.AnimatedIcon;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.dsl.builder.Panel;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * New-project-wizard step that collects Open Liberty Starter parameters.
 *
 * <p>The UI is built immediately with empty fields so the wizard opens
 * without delay. The {@code GET /api/start/info} call runs on a background
 * thread; when it completes the field values and combo-box options are
 * populated in place on the EDT. Hard-coded fallbacks are used only when
 * the network is unavailable.</p>
 */
public class LibertyNewProjectWizardStep extends AbstractNewProjectWizardStep implements Disposable {

    // ── Fallback constants (used until /info response arrives) ────────────────

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

    // ── Async info fetch ──────────────────────────────────────────────────────

    private final CompletableFuture<LibertyNewProjectWizard.StarterInfo> infoFuture;
    private JSONObject eeConstraints;

    // ── Validation state ──────────────────────────────────────────────────────

    private boolean groupValid    = true;
    private boolean artifactValid = true;
    /** The wizard's "Create" button — resolved once after the UI is shown. */
    private JButton createButton  = null;

    public LibertyNewProjectWizardStep(@NotNull NewProjectWizardStep parent) {
        super(parent);
        Disposer.register(getContext().getDisposable(), this);

        // Kick off the API call on a background thread — do NOT block the EDT.
        infoFuture = CompletableFuture.supplyAsync(LibertyNewProjectWizard::fetchStarterInfo);

        // Initialise with fallback defaults; overwritten once the API responds.
        groupProp    = getPropertyGraph().property(DEFAULT_GROUP);
        artifactProp = getPropertyGraph().property(DEFAULT_ARTIFACT);
        buildProp    = getPropertyGraph().property(DEFAULT_BUILD);
        javaProp     = getPropertyGraph().property(DEFAULT_JAVA);
        eeProp       = getPropertyGraph().property(DEFAULT_EE);
        mpProp       = getPropertyGraph().property(DEFAULT_MP);
        eeConstraints = null;
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    @Override
    public void setupUI(@NotNull Panel builder) {

        // ── Server URL row ────────────────────────────────────────────────────
        JLabel urlLabel = new JLabel("<html><a href=''>openliberty.io/start/</a></html>");
        urlLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        urlLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new java.net.URI("https://openliberty.io/start/"));
                } catch (Exception ignored) {}
            }
        });

        JLabel spinnerLabel = new JLabel(new AnimatedIcon.Default());
        spinnerLabel.setVisible(true); // visible while API call is in flight
        JLabel compatibilityMessageLabel = new JLabel(UIManager.getIcon("OptionPane.informationIcon"));
        compatibilityMessageLabel.setVisible(false);

        builder.row("Server URL:", row -> {
            row.cell(urlLabel);
            row.cell(spinnerLabel);
            return null;
        });

        // ── Group ─────────────────────────────────────────────────────────────
        JBTextField groupField = new JBTextField(groupProp.get(), 25);
        JLabel groupErrorLabel = new JLabel(
                "Valid characters for package names include a-z, A-Z, '_' and 0-9. Packages must be separated by '.'");
        groupErrorLabel.setIcon(UIManager.getIcon("OptionPane.errorIcon"));
        groupErrorLabel.setVisible(false);
        groupField.getDocument().addDocumentListener(textListener(() -> {
            String text = groupField.getText();
            groupProp.set(text);
            groupValid = text.isEmpty() || text.matches("[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)*");
            groupErrorLabel.setVisible(!groupValid);
            updateCreateButton();
        }));
        builder.row("Group:", row -> {
            row.cell(groupField);
            return null;
        });
        builder.row("", row -> {
            row.cell(groupErrorLabel);
            return null;
        });

        // ── Artifact ──────────────────────────────────────────────────────────
        JBTextField artifactField = new JBTextField(artifactProp.get(), 25);
        JLabel artifactErrorLabel = new JLabel("Valid characters include a-z separated by '-'");
        artifactErrorLabel.setIcon(UIManager.getIcon("OptionPane.errorIcon"));
        artifactErrorLabel.setVisible(false);
        artifactField.getDocument().addDocumentListener(textListener(() -> {
            String text = artifactField.getText();
            artifactProp.set(text);
            artifactValid = text.isEmpty() || text.matches("[a-z]+(-[a-z]+)*");
            artifactErrorLabel.setVisible(!artifactValid);
            updateCreateButton();
        }));
        builder.row("Artifact:", row -> {
            row.cell(artifactField);
            return null;
        });
        builder.row("", row -> {
            row.cell(artifactErrorLabel);
            return null;
        });

        // Locate the wizard's "Create" button once the component hierarchy is fully realised.
        SwingUtilities.invokeLater(() -> resolveCreateButton(groupField));

        // ── Build Tool ────────────────────────────────────────────────────────
        JPanel buildPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buildPanel.setOpaque(false);
        final List<JRadioButton> buildRadios = new ArrayList<>();
        final ButtonGroup buildGroup = new ButtonGroup();
        populateBuildRadios(buildPanel, buildRadios, buildGroup, FALLBACK_BUILD, buildProp.get());
        builder.row("Build Tool:", row -> {
            row.cell(buildPanel);
            return null;
        });

        // ── Version combos (start empty; populated by API response) ──────────
        JComboBox<String> javaCombo = new JComboBox<>(FALLBACK_JAVA);
        javaCombo.setSelectedItem(javaProp.get());
        javaCombo.addActionListener(e -> javaProp.set((String) javaCombo.getSelectedItem()));

        JComboBox<String> eeCombo = new JComboBox<>(FALLBACK_EE);
        eeCombo.setSelectedItem(eeProp.get());
        eeCombo.addActionListener(e -> eeProp.set((String) eeCombo.getSelectedItem()));

        JComboBox<String> mpCombo = new JComboBox<>(FALLBACK_MP);
        mpCombo.setSelectedItem(mpProp.get());
        mpCombo.addActionListener(e -> {
            String selectedMp = (String) mpCombo.getSelectedItem();
            mpProp.set(selectedMp);
            applyEeConstraints(eeCombo, selectedMp, null, compatibilityMessageLabel);
        });

        builder.row("Java SE Version:", row -> {
            row.cell(javaCombo);
            row.label("Java EE/Jakarta EE Version:");
            row.cell(eeCombo);
            row.label("MicroProfile Version:");
            row.cell(mpCombo);
            return null;
        });

        eeCombo.addActionListener(e -> {
            String selectedEe = (String) eeCombo.getSelectedItem();
            eeProp.set(selectedEe);
            applyMpConstraints(mpCombo, selectedEe, null, compatibilityMessageLabel);
        });

        builder.row((JLabel) null, row -> {
            row.cell(compatibilityMessageLabel);
            return null;
        });

        // ── Populate fields once the background API call completes ────────────
        // Use whenComplete so the callback fires whether the future is already
        // done or not, then unconditionally jump to the EDT via invokeLater.
        // Guard against cancellation: if the wizard was closed before the fetch
        // completed the future is cancelled and we must not touch disposed UI.
        infoFuture.whenComplete((info, ex) -> SwingUtilities.invokeLater(() -> {
            if (infoFuture.isCancelled()) return;
            // Use live values if available, fall back to hard-coded defaults.
            String group    = info != null ? info.defaultGroup    : DEFAULT_GROUP;
            String artifact = info != null ? info.defaultArtifact : DEFAULT_ARTIFACT;
            String build    = info != null ? info.defaultBuild    : DEFAULT_BUILD;
            String java     = info != null ? info.defaultJava     : DEFAULT_JAVA;
            String ee       = info != null ? info.defaultEe       : DEFAULT_EE;
            String mp       = info != null ? info.defaultMp       : DEFAULT_MP;
            String[] buildOpts = info != null ? info.buildOptions : FALLBACK_BUILD;
            String[] javaOpts  = info != null ? info.javaOptions  : FALLBACK_JAVA;
            String[] eeOpts    = info != null ? info.eeOptions    : FALLBACK_EE;
            eeConstraints      = info != null ? info.eeConstraints : null;

            // Hide the spinner now that values are populated.
            spinnerLabel.setVisible(false);

            groupProp.set(group);
            groupField.setText(group);

            artifactProp.set(artifact);
            artifactField.setText(artifact);

            buildProp.set(build);
            populateBuildRadios(buildPanel, buildRadios, buildGroup, buildOpts, build);
            buildPanel.revalidate();
            buildPanel.repaint();

            javaProp.set(java);
            javaCombo.setModel(new DefaultComboBoxModel<>(javaOpts));
            javaCombo.setSelectedItem(java);

            eeProp.set(ee);
            eeCombo.setModel(new DefaultComboBoxModel<>(eeOpts));
            eeCombo.setSelectedItem(ee);

            applyMpConstraints(mpCombo, ee, mp, compatibilityMessageLabel);
            applyEeConstraints(eeCombo, mpProp.get(), eeProp.get(), compatibilityMessageLabel);
        }));
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
     * Clears and repopulates {@code panel} with one {@link JRadioButton} per build option.
     * Existing buttons are removed first so this can be called again when the API response
     * arrives with the live option list.
     */
    private void populateBuildRadios(JPanel panel, List<JRadioButton> radios,
                                     ButtonGroup group, String[] options, String selected) {
        for (JRadioButton r : radios) group.remove(r);
        radios.clear();
        panel.removeAll();

        for (final String value : options) {
            JRadioButton radio = new JRadioButton(capitalize(value));
            radio.setSelected(value.equalsIgnoreCase(selected));
            radio.setOpaque(false);
            radio.addActionListener(e -> buildProp.set(value));
            group.add(radio);
            panel.add(radio);
            radios.add(radio);
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void applyMpConstraints(JComboBox<String> mpCombo, String eeVersion, String preferredMp,
                                    JLabel compatibilityMessageLabel) {
        String originalMp = preferredMp != null ? preferredMp : mpProp.get();
        String selectedMp = originalMp;
        if (!isAllowedMpOption(eeVersion, selectedMp)) {
            selectedMp = getFallbackMpOption(eeVersion);
        }

        if (selectedMp != null) {
            mpProp.set(selectedMp);
            mpCombo.setSelectedItem(selectedMp);
            updateCompatibilityMessage(
                    compatibilityMessageLabel,
                    !selectedMp.equals(originalMp)
                            ? "MicroProfile Version has been automatically updated from " + originalMp
                            + " to " + selectedMp
                            + " for compatibility with Java EE / Jakarta EE Version."
                            : null);
        }
    }

    private boolean isAllowedMpOption(String eeVersion, String mpVersion) {
        if (eeConstraints == null) {
            return true;
        }
        JSONObject constraint = eeConstraints.optJSONObject(eeVersion);
        if (constraint == null) {
            return true;
        }
        JSONArray constrainedMps = constraint.optJSONArray("m");
        return constrainedMps.isEmpty() || constrainedMps.toList().contains(mpVersion);
    }

    private String getFallbackMpOption(String eeVersion) {
        if (eeConstraints == null) {
            return mpProp.get();
        }
        JSONObject constraint = eeConstraints.optJSONObject(eeVersion);
        if (constraint == null) {
            return mpProp.get();
        }
        JSONArray fallbackMps = constraint.optJSONArray("m");
        return !fallbackMps.isEmpty() ? fallbackMps.optString(fallbackMps.length() - 1) : mpProp.get();
    }

    private void applyEeConstraints(JComboBox<String> eeCombo, String mpVersion, String preferredEe,
                                    JLabel compatibilityMessageLabel) {
        String originalEe = preferredEe != null ? preferredEe : eeProp.get();
        String selectedEe = originalEe;
        if (!isAllowedEeOption(selectedEe, mpVersion)) {
            selectedEe = getFallbackEeOption(mpVersion);
        }

        if (selectedEe != null) {
            eeProp.set(selectedEe);
            eeCombo.setSelectedItem(selectedEe);
            updateCompatibilityMessage(
                    compatibilityMessageLabel,
                    !selectedEe.equals(originalEe)
                            ? "Java EE / Jakarta EE Version has been automatically updated from " + originalEe
                            + " to " + selectedEe
                            + " for compatibility with MicroProfile Version."
                            : null);
        }
    }

    private boolean isAllowedEeOption(String eeVersion, String mpVersion) {
        if (eeConstraints == null) {
            return true;
        }
        JSONObject constraint = eeConstraints.optJSONObject(eeVersion);
        if (constraint == null) {
            return true;
        }
        JSONArray allowedMps = constraint.optJSONArray("m");
        if (allowedMps == null) {
            return true;
        }
        for (int i = 0; i < allowedMps.length(); i++) {
            if (mpVersion.equals(allowedMps.optString(i))) {
                return true;
            }
        }
        return false;
    }

    private String getFallbackEeOption(String mpVersion) {
        if (eeConstraints == null) {
            return eeProp.get();
        }
        JComboBox<String> unused = null;
        for (String eeOption : FALLBACK_EE) {
            if (isAllowedEeOption(eeOption, mpVersion)) {
                return eeOption;
            }
        }
        return eeProp.get();
    }

    private void updateCompatibilityMessage(JLabel compatibilityMessageLabel, String message) {
        compatibilityMessageLabel.setText(message == null ? "" : message);
        compatibilityMessageLabel.setVisible(message != null);
    }

    private static DocumentListener textListener(Runnable onChange) {
        return new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e)  { onChange.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e)  { onChange.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
        };
    }

    /**
     * Walks up the Swing hierarchy from {@code anchor} to find the root window,
     * then searches its button bar for a button whose text is "Create" or "Finish".
     * Stores the reference in {@link #createButton} for later enable/disable calls.
     */
    private void resolveCreateButton(JComponent anchor) {
        Window window = SwingUtilities.getWindowAncestor(anchor);
        if (window == null) return;
        findButton(window, btn -> {
            String text = btn.getText();
            if ("Create".equalsIgnoreCase(text) || "Finish".equalsIgnoreCase(text)) {
                createButton = btn;
                updateCreateButton();
            }
        });
    }

    /** Recursively walks a container looking for {@link JButton} instances. */
    private static void findButton(Container container, Consumer<JButton> visitor) {
        for (Component c : container.getComponents()) {
            if (c instanceof JButton) {
                visitor.accept((JButton) c);
            } else if (c instanceof Container) {
                findButton((Container) c, visitor);
            }
        }
    }

    /** Enables or disables the "Create" button based on current validation state. */
    private void updateCreateButton() {
        if (createButton != null) {
            createButton.setEnabled(groupValid && artifactValid);
        }
    }

    // ── Disposable ────────────────────────────────────────────────────────────

    /**
     * Cancels the background {@code /api/start/info} fetch so that its
     * {@code whenComplete} callback does not attempt to update Swing components
     * that may have already been disposed when the wizard is closed or cancelled.
     */
    @Override
    public void dispose() {
        infoFuture.cancel(true);
    }
}
