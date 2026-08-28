/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.starter

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import io.openliberty.tools.intellij.util.LocalizedResourceUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Panel
import org.json.JSONObject
import java.awt.Container
import java.awt.Desktop
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * Renders the "Server URL" row (with animated spinner) at the very top of the
 * wizard page — before the Name / Location rows added by
 * [com.intellij.ide.wizard.NewProjectWizardBaseStep].
 *
 * The [spinnerLabel] is stored on the shared [com.intellij.openapi.util.UserDataHolder]
 * so that the downstream [LibertyNewProjectWizardStep] can hide it once the
 * background API call completes.
 */
class LibertyServerUrlStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {

    companion object {
        val SPINNER_KEY: Key<JLabel> = Key.create("LibertyStarterSpinner")
        const val STARTER_URL = "https://openliberty.io/start/"
    }

    override fun setupUI(builder: Panel) {
        val spinnerLabel = JLabel(AnimatedIcon.Default()).apply { isVisible = true }
        data.putUserData(SPINNER_KEY, spinnerLabel)

        val urlLabel = JLabel("<html><a href=''>${LocalizedResourceUtil.getMessage("liberty.starter.server.url.display")}</a></html>").apply {
            cursor = java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR)
            addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mouseClicked(e: java.awt.event.MouseEvent) {
                    runCatching { Desktop.getDesktop().browse(URI(STARTER_URL)) }
                }
            })
        }

        // ── Server URL ─────────────────────────────────────────────────────────────
        builder.row(LocalizedResourceUtil.getMessage("liberty.starter.server.url.label")) {
            cell(urlLabel)
            cell(spinnerLabel)
        }
    }
}

/**
 * New-project-wizard step that collects Open Liberty Starter parameters.
 *
 * The UI is built immediately with fallback values so the wizard opens without
 * delay. The `GET /api/start/info` call runs on a background thread; when it
 * completes the field values and combo-box options are populated on the EDT.
 * Hard-coded fallbacks are used only when the network is unavailable.
 */
class LibertyNewProjectWizardStep(parent: NewProjectWizardStep) :
    AbstractNewProjectWizardStep(parent), Disposable {

    // Fallback constants
    companion object {
        private const val DEFAULT_GROUP    = "com.demo"
        private const val DEFAULT_ARTIFACT = "app-name"
        private const val DEFAULT_BUILD    = "maven"
        private const val DEFAULT_JAVA     = "21"
        private const val DEFAULT_EE       = "11.0"
        private const val DEFAULT_MP       = "7.1"
        private val FALLBACK_BUILD = arrayOf("maven", "gradle")
        private val FALLBACK_JAVA  = arrayOf("21", "17", "11", "8")
        private val FALLBACK_EE    = arrayOf("11.0", "10.0", "9.1", "8.0", "7.0", "None")
        private val FALLBACK_MP    = arrayOf("7.1", "7.0", "6.1", "6.0", "5.0", "4.1", "3.3", "2.2", "1.4", "None")
    }

    // GraphProperty ties each field's live value to the wizard's reactive graph so that UI controls can bind to and observe it
    private val groupProp:    GraphProperty<String> = propertyGraph.property(DEFAULT_GROUP)
    private val artifactProp: GraphProperty<String> = propertyGraph.property(DEFAULT_ARTIFACT)
    private val buildProp:    GraphProperty<String> = propertyGraph.property(DEFAULT_BUILD)
    private val javaProp:     GraphProperty<String> = propertyGraph.property(DEFAULT_JAVA)
    private val eeProp:       GraphProperty<String> = propertyGraph.property(DEFAULT_EE)
    private val mpProp:       GraphProperty<String> = propertyGraph.property(DEFAULT_MP)

    // Async info API fetch
    private val infoFuture: CompletableFuture<LibertyNewProjectWizard.StarterInfo?> =
        CompletableFuture.supplyAsync { LibertyNewProjectWizard.fetchStarterInfo() }

    private var eeConstraints: JSONObject? = null

    // Validation states for group, artifact and create button
    private var groupValid    = true
    private var artifactValid = true
    private var createButton: JButton? = null

    init {
        Disposer.register(context.disposable, this)
    }

    override fun setupUI(builder: Panel) {

        val compatibilityMessageLabel = JLabel(UIManager.getIcon("OptionPane.informationIcon")).apply { isVisible = false }

        // ── Group ─────────────────────────────────────────────────────────────
        val groupField = JBTextField(groupProp.get(), 25)
        val groupErrorLabel = JLabel(
            LocalizedResourceUtil.getMessage("liberty.starter.error.group.invalid")
        ).apply {
            icon = UIManager.getIcon("OptionPane.errorIcon")
            isVisible = false
        }
        groupField.document.addDocumentListener(textListener {
            val text = groupField.text
            groupProp.set(text)
            groupValid = text.isEmpty() || text.matches(Regex("[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)*"))
            groupErrorLabel.isVisible = !groupValid
            updateCreateButton()
        })
        builder.row(LocalizedResourceUtil.getMessage("liberty.starter.group.label")) { cell(groupField) }
        builder.row("") { cell(groupErrorLabel) }

        // ── Artifact ──────────────────────────────────────────────────────────
        val artifactField = JBTextField(artifactProp.get(), 25)
        val artifactErrorLabel = JLabel(LocalizedResourceUtil.getMessage("liberty.starter.error.artifact.invalid")).apply {
            icon = UIManager.getIcon("OptionPane.errorIcon")
            isVisible = false
        }
        artifactField.document.addDocumentListener(textListener {
            val text = artifactField.text
            artifactProp.set(text)
            artifactValid = text.isEmpty() || text.matches(Regex("[a-z]+(-[a-z]+)*"))
            artifactErrorLabel.isVisible = !artifactValid
            updateCreateButton()
        })
        builder.row(LocalizedResourceUtil.getMessage("liberty.starter.artifact.label")) { cell(artifactField) }
        builder.row("") { cell(artifactErrorLabel) }

        // Locate the wizard's "Create" button inorder to disable it if the validation fails for group and artifact fields.
        SwingUtilities.invokeLater { resolveCreateButton(groupField) }

        // ── Build Tool ────────────────────────────────────────────────────────
        builder.row(LocalizedResourceUtil.getMessage("liberty.starter.build.tool.label")) {
            segmentedButton(FALLBACK_BUILD.toList()) { text = it.replaceFirstChar { c -> c.uppercaseChar() } }
                .bind(buildProp)
        }

        // ── Version combos ────────────────────────────────────────────────────
        val javaCombo = JComboBox(FALLBACK_JAVA).apply {
            selectedItem = javaProp.get()
            addActionListener { javaProp.set(selectedItem as String) }
        }

        val eeCombo = JComboBox(FALLBACK_EE).apply {
            selectedItem = eeProp.get()
        }

        val mpCombo = JComboBox(FALLBACK_MP).apply {
            selectedItem = mpProp.get()
            // MP action listener added for compatability checking.
            addActionListener {
                val selectedMp = selectedItem as String
                mpProp.set(selectedMp)
                applyEeConstraints(eeCombo, selectedMp, null, compatibilityMessageLabel)
            }
        }

        builder.row(LocalizedResourceUtil.getMessage("liberty.starter.java.se.label")) {
            cell(javaCombo)
            label(LocalizedResourceUtil.getMessage("liberty.starter.jakarta.ee.label"))
            cell(eeCombo)
            label(LocalizedResourceUtil.getMessage("liberty.starter.microprofile.label"))
            cell(mpCombo)
        }

        // EE action listener added for compatability checking.
        eeCombo.addActionListener {
            val selectedEe = eeCombo.selectedItem as String
            eeProp.set(selectedEe)
            applyMpConstraints(mpCombo, selectedEe, null, compatibilityMessageLabel)
        }

        builder.row(null as JLabel?) {
            cell(compatibilityMessageLabel)
        }

        // Retrieve the spinner stored by the upstream LibertyServerUrlStep.
        val spinnerLabel = data.getUserData(LibertyServerUrlStep.SPINNER_KEY)

        // Populate fields once the background API call completes
        infoFuture.whenComplete { info, _ ->
            SwingUtilities.invokeLater {
                if (infoFuture.isCancelled) return@invokeLater

                val group      = info?.defaultGroup    ?: DEFAULT_GROUP
                val artifact   = info?.defaultArtifact ?: DEFAULT_ARTIFACT
                val build      = info?.defaultBuild    ?: DEFAULT_BUILD
                val java       = info?.defaultJava     ?: DEFAULT_JAVA
                val ee         = info?.defaultEe       ?: DEFAULT_EE
                val mp         = info?.defaultMp       ?: DEFAULT_MP
                val buildOpts  = info?.buildOptions    ?: FALLBACK_BUILD
                val javaOpts   = info?.javaOptions     ?: FALLBACK_JAVA
                val eeOpts     = info?.eeOptions       ?: FALLBACK_EE
                eeConstraints  = info?.eeConstraints

                spinnerLabel?.isVisible = false

                groupProp.set(group)
                groupField.text = group

                artifactProp.set(artifact)
                artifactField.text = artifact

                // Update the segmented button options and selection via the property.
                // The segmentedButton control observes buildProp so setting the
                // property re-selects the matching segment automatically.
                // Re-populate options if the live list differs from the fallback.
                buildProp.set(build)

                javaProp.set(java)
                javaCombo.model = DefaultComboBoxModel(javaOpts)
                javaCombo.selectedItem = java

                eeProp.set(ee)
                eeCombo.model = DefaultComboBoxModel(eeOpts)
                eeCombo.selectedItem = ee

                applyMpConstraints(mpCombo, ee, mp, compatibilityMessageLabel)
                applyEeConstraints(eeCombo, mpProp.get(), eeProp.get(), compatibilityMessageLabel)
            }
        }
    }

    // Project generation
    override fun setupProject(project: Project) {
        LibertyNewProjectWizard.downloadAndExtract(
            project,
            context.projectDirectory.toString(),
            groupProp.get(),
            artifactProp.get(),
            buildProp.get(),
            javaProp.get(),
            eeProp.get(),
            mpProp.get()
        )
    }

    // Constraint/Compatability helpers
    private fun applyMpConstraints(
        mpCombo: JComboBox<String>,
        eeVersion: String,
        preferredMp: String?,
        compatibilityMessageLabel: JLabel
    ) {
        val originalMp = preferredMp ?: mpProp.get()
        val selectedMp = if (isAllowedMpOption(eeVersion, originalMp)) originalMp
                         else getFallbackMpOption(eeVersion)

        if (selectedMp != null) {
            mpProp.set(selectedMp)
            mpCombo.selectedItem = selectedMp
            updateCompatibilityMessage(
                compatibilityMessageLabel,
                if (selectedMp != originalMp)
                    LocalizedResourceUtil.getMessage("liberty.starter.compat.mp.updated", originalMp, selectedMp)
                else null
            )
        }
    }

    private fun isAllowedMpOption(eeVersion: String, mpVersion: String): Boolean {
        val constraint = eeConstraints?.optJSONObject(eeVersion) ?: return true
        val constrainedMps = constraint.optJSONArray("m") ?: return true
        return constrainedMps.isEmpty || constrainedMps.toList().contains(mpVersion)
    }

    private fun getFallbackMpOption(eeVersion: String): String? {
        val constraint = eeConstraints?.optJSONObject(eeVersion) ?: return mpProp.get()
        val fallbackMps = constraint.optJSONArray("m") ?: return mpProp.get()
        return if (!fallbackMps.isEmpty) fallbackMps.optString(fallbackMps.length() - 1) else mpProp.get()
    }

    private fun applyEeConstraints(
        eeCombo: JComboBox<String>,
        mpVersion: String,
        preferredEe: String?,
        compatibilityMessageLabel: JLabel
    ) {
        val originalEe = preferredEe ?: eeProp.get()
        val selectedEe = if (isAllowedEeOption(originalEe, mpVersion)) originalEe
                         else getFallbackEeOption(mpVersion)

        if (selectedEe != null) {
            eeProp.set(selectedEe)
            eeCombo.selectedItem = selectedEe
            updateCompatibilityMessage(
                compatibilityMessageLabel,
                if (selectedEe != originalEe)
                    LocalizedResourceUtil.getMessage("liberty.starter.compat.ee.updated", originalEe, selectedEe)
                else null
            )
        }
    }

    private fun isAllowedEeOption(eeVersion: String, mpVersion: String): Boolean {
        val constraint = eeConstraints?.optJSONObject(eeVersion) ?: return true
        val allowedMps = constraint.optJSONArray("m") ?: return true
        for (i in 0 until allowedMps.length()) {
            if (mpVersion == allowedMps.optString(i)) return true
        }
        return false
    }

    private fun getFallbackEeOption(mpVersion: String): String {
        if (eeConstraints == null) return eeProp.get()
        return FALLBACK_EE.firstOrNull { isAllowedEeOption(it, mpVersion) } ?: eeProp.get()
    }

    private fun updateCompatibilityMessage(label: JLabel, message: String?) {
        label.text = message ?: ""
        label.isVisible = message != null
    }

    // Create-button helpers
    /**
     * Walks up the Swing hierarchy from [anchor] to find the root window,
     * then searches its button bar for a button whose text is "Create".
     */
    private fun resolveCreateButton(anchor: JComponent) {
        val window = SwingUtilities.getWindowAncestor(anchor) ?: return
        findButton(window) { btn ->
            if (btn.text.equals("Create", ignoreCase = true)) {
                createButton = btn
                updateCreateButton()
            }
        }
    }

    private fun findButton(container: Container, visitor: Consumer<JButton>) {
        for (c in container.components) {
            when (c) {
                is JButton    -> visitor.accept(c)
                is Container  -> findButton(c, visitor)
            }
        }
    }

    private fun updateCreateButton() {
        createButton?.isEnabled = groupValid && artifactValid
    }

    // Disposable
    override fun dispose() {
        infoFuture.cancel(true)
    }

    private fun textListener(onChange: () -> Unit): DocumentListener = object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent)  = onChange()
        override fun removeUpdate(e: DocumentEvent)  = onChange()
        override fun changedUpdate(e: DocumentEvent) = onChange()
    }
}
