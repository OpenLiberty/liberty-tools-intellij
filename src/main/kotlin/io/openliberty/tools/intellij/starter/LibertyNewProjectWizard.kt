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

import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.ide.wizard.GeneratorNewProjectWizard
import com.intellij.ide.wizard.GeneratorNewProjectWizardBuilderAdapter
import com.intellij.ide.wizard.NewProjectWizardBaseStep
import com.intellij.ide.wizard.NewProjectWizardChainStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ide.wizard.RootNewProjectWizardStep
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import io.openliberty.tools.intellij.LibertyPluginIcons
import io.openliberty.tools.intellij.util.LocalizedResourceUtil
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream
import javax.swing.Icon

/**
 * Registers "Open Liberty" in IntelliJ's new-project wizard.
 *
 * The single wizard page is built by [LibertyNewProjectWizardStep] via
 * [com.intellij.ide.wizard.NewProjectWizardChainStep], which chains the
 * platform's built-in Name/Location step with our Liberty-specific step.
 *
 * Query-parameter mapping for the Starter API:
 * - `g` – groupId
 * - `a` – artifactId
 * - `b` – build tool (`maven` | `gradle`)
 * - `j` – Java SE version (e.g. `21`)
 * - `e` – Jakarta EE version (e.g. `10.0`)
 * - `m` – MicroProfile version (e.g. `6.1`)
 */
class LibertyNewProjectWizard : GeneratorNewProjectWizard {

    override val id:   String = "OpenLiberty"
    override val name: String = "Open Liberty"
    override val icon: Icon = LibertyPluginIcons.libertyIcon

    // ── Step chain ────────────────────────────────────────────────────────────

    /**
     * Builds the step chain using [NewProjectWizardChainStep.nextStep] for every
     * link so that each step's [setupUI] is called in order:
     *
     *   [RootNewProjectWizardStep]
     *   → [LibertyServerUrlStep]   (Server URL row — rendered first)
     *   → [NewProjectWizardBaseStep] (Name / Location)
     *   → [LibertyNewProjectWizardStep] (Liberty-specific fields)
     */
    override fun createStep(context: WizardContext): NewProjectWizardStep {
        val root = RootNewProjectWizardStep(context)
        return with(NewProjectWizardChainStep.Companion) {
            root.nextStep { LibertyServerUrlStep(it) }
                .nextStep { NewProjectWizardBaseStep(it) }
                .nextStep { LibertyNewProjectWizardStep(it) }
        }
    }

    /**
     * Holds the defaults, option lists, and EE→MP constraints returned by
     * `GET /api/start/info`.
     */
    class StarterInfo(
        val defaultGroup:    String,
        val defaultArtifact: String,
        val defaultBuild:    String,
        val defaultJava:     String,
        val defaultEe:       String,
        val defaultMp:       String,
        val buildOptions:    Array<String>,
        val javaOptions:     Array<String>,
        val eeOptions:       Array<String>,
        val mpOptions:       Array<String>,
        val eeConstraints:   JSONObject?
    )

    /** Adapter that surfaces [LibertyNewProjectWizard] as a legacy project builder. */
    class LibertyBuilder : GeneratorNewProjectWizardBuilderAdapter(LibertyNewProjectWizard()) {
        /** 1000 is an arbitrary offset to place Liberty first in the generator list. */
        override fun getWeight(): Int = JVM_WEIGHT + 1000
    }

    companion object {

        private val LOGGER: Logger = Logger.getInstance(LibertyNewProjectWizard::class.java)

        const val STARTER_API      = "https://start.openliberty.io/api/start"
        const val STARTER_INFO_API = "https://start.openliberty.io/api/start/info"

        /**
         * Calls `GET /api/start/info`, parses the JSON response, and returns a
         * [StarterInfo]. Returns `null` on any error; callers fall back to
         * hard-coded defaults.
         */
        @JvmStatic
        fun fetchStarterInfo(): StarterInfo? {
            return try {
                val conn = (URI(STARTER_INFO_API).toURL().openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout    = 15_000
                    requestMethod  = "GET"
                    setRequestProperty("Accept", "application/json")
                }
                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    LOGGER.warn("Liberty Starter /info returned HTTP ${conn.responseCode}")
                    return null
                }
                val json = conn.inputStream.use { it.readBytes().toString(StandardCharsets.UTF_8) }
                    .also { conn.disconnect() }
                parseStarterInfo(json)
            } catch (ex: Exception) {
                LOGGER.warn("Failed to fetch Liberty Starter info", ex)
                null
            }
        }

        /** Parses the `/api/start/info` JSON payload. */
        @JvmStatic
        fun parseStarterInfo(json: String): StarterInfo? {
            return try {
                val root = JSONObject(json)
                StarterInfo(
                    defaultGroup    = root.getJSONObject("g").getString("default"),
                    defaultArtifact = root.getJSONObject("a").getString("default"),
                    defaultBuild    = root.getJSONObject("b").getString("default"),
                    defaultJava     = root.getJSONObject("j").getString("default"),
                    defaultEe       = root.getJSONObject("e").getString("default"),
                    defaultMp       = root.getJSONObject("m").getString("default"),
                    buildOptions    = root.getJSONObject("b").getJSONArray("options").toStringArray(),
                    javaOptions     = root.getJSONObject("j").getJSONArray("options").toStringArray(),
                    eeOptions       = root.getJSONObject("e").getJSONArray("options").toStringArray(),
                    mpOptions       = root.getJSONObject("m").getJSONArray("options").toStringArray(),
                    eeConstraints   = root.getJSONObject("e").optJSONObject("constraints")
                )
            } catch (ex: Exception) {
                LOGGER.warn("Failed to parse Liberty Starter info JSON", ex)
                null
            }
        }

        /**
         * Downloads the starter ZIP and extracts it into [projectDir] on a
         * background thread, showing a modal progress dialog.
         */
        @JvmStatic
        fun downloadAndExtract(
            project:      Project,
            projectDir:   String,
            group:        String,
            artifact:     String,
            buildTool:    String,
            javaVersion:  String,
            eeVersion:    String,
            mpVersion:    String
        ) {
            val url = "$STARTER_API?g=${encode(group)}&a=${encode(artifact)}" +
                    "&b=${encode(buildTool)}&j=${encode(javaVersion)}" +
                    "&e=${encode(eeVersion)}&m=${encode(mpVersion)}"
            LOGGER.info("Liberty Starter request: $url")

            ProgressManager.getInstance().run(object : Task.Modal(
                project,
                LocalizedResourceUtil.getMessage("liberty.starter.progress.generating"),
                false
            ) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true
                    indicator.text = LocalizedResourceUtil.getMessage("liberty.starter.progress.downloading")
                    try {
                        val zipBytes = downloadZip(url, indicator)
                        indicator.text = LocalizedResourceUtil.getMessage("liberty.starter.progress.extracting")
                        unzip(zipBytes, projectDir, indicator)
                    } catch (ex: Exception) {
                        LOGGER.warn("Liberty Starter generation failed", ex)
                        ApplicationManager.getApplication().invokeLater {
                            Messages.showErrorDialog(
                                LocalizedResourceUtil.getMessage("liberty.starter.error.generation.failed", ex.message ?: ""),
                                LocalizedResourceUtil.getMessage("liberty.starter.wizard.title")
                            )
                        }
                    }
                }
            })
        }

        // Helpers
        private fun encode(s: String): String =
            URLEncoder.encode(s, StandardCharsets.UTF_8.name())

        private fun downloadZip(urlStr: String, indicator: ProgressIndicator): ByteArray {
            val conn = (URI(urlStr).toURL().openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout    = 60_000
                requestMethod  = "GET"
            }
            val status = conn.responseCode
            if (status != HttpURLConnection.HTTP_OK) {
                throw IOException("Open Liberty Starter returned HTTP $status")
            }
            return try {
                conn.inputStream.use { input ->
                    ByteArrayOutputStream().also { buf ->
                        val chunk = ByteArray(8192)
                        var read: Int
                        while (input.read(chunk).also { read = it } != -1) {
                            buf.write(chunk, 0, read)
                            indicator.checkCanceled()
                        }
                    }.toByteArray()
                }
            } finally {
                conn.disconnect()
            }
        }

        /**
         * Extracts [zipBytes] into [destDir], stripping the single top-level
         * folder the Starter wraps its output in.
         */
        private fun unzip(zipBytes: ByteArray, destDir: String, indicator: ProgressIndicator) {
            val dest = Paths.get(destDir)
            Files.createDirectories(dest)
            var stripPrefix: String? = null

            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    indicator.checkCanceled()
                    var name = entry.name

                    if (stripPrefix == null) {
                        val slash = name.indexOf('/')
                        stripPrefix = if (slash >= 0) name.substring(0, slash + 1) else ""
                    }
                    val prefix = stripPrefix
                    if (prefix.isNotEmpty() && name.startsWith(prefix)) {
                        name = name.substring(prefix.length)
                    }
                    if (name.isEmpty()) { zis.closeEntry(); entry = zis.nextEntry; continue }

                    val target = dest.resolve(name).normalize()
                    if (!target.startsWith(dest)) {
                        throw IOException("Zip-slip attempt: $name")
                    }
                    if (entry.isDirectory) {
                        Files.createDirectories(target)
                    } else {
                        Files.createDirectories(target.parent)
                        Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }

        private fun JSONArray.toStringArray(): Array<String> =
            Array(length()) { getString(it) }
    }
}
