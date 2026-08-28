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

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.ide.wizard.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import io.openliberty.tools.intellij.LibertyPluginIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Registers "Open Liberty" in IntelliJ's new-project wizard using the modern
 * {@link GeneratorNewProjectWizard} API (introduced in IntelliJ 2022.3+).
 *
 * <p>The single wizard page is built by {@link LibertyNewProjectWizardStep} via
 * {@link NewProjectWizardChainStep}, which chains the platform's built-in
 * Name/Location step with our Liberty-specific step.</p>
 *
 * <p>Query-parameter mapping for the Starter API:</p>
 * <ul>
 *   <li>{@code g} – groupId</li>
 *   <li>{@code a} – artifactId</li>
 *   <li>{@code b} – build tool ({@code maven} | {@code gradle})</li>
 *   <li>{@code j} – Java SE version (e.g. {@code 21})</li>
 *   <li>{@code e} – Jakarta EE version (e.g. {@code 10.0})</li>
 *   <li>{@code m} – MicroProfile version (e.g. {@code 6.1})</li>
 * </ul>
 */
public class LibertyNewProjectWizard implements GeneratorNewProjectWizard {

    private static final Logger LOGGER = Logger.getInstance(LibertyNewProjectWizard.class);
    static final String STARTER_API      = "https://start.openliberty.io/api/start";
    static final String STARTER_INFO_API = "https://start.openliberty.io/api/start/info";

    // ── StarterInfo model ─────────────────────────────────────────────────────

    /**
     * Holds the defaults, option lists, and EE→MP constraints returned by
     * {@code GET /api/start/info}.
     */
    static class StarterInfo {
        final String   defaultGroup;
        final String   defaultArtifact;
        final String   defaultBuild;
        final String   defaultJava;
        final String   defaultEe;
        final String   defaultMp;
        final String[] buildOptions;
        final String[] javaOptions;
        final String[] eeOptions;
        final String[] mpOptions;
        final JSONObject eeConstraints;
        StarterInfo(String defaultGroup, String defaultArtifact,
                    String defaultBuild, String defaultJava,
                    String defaultEe,   String defaultMp,
                    String[] buildOptions, String[] javaOptions,
                    String[] eeOptions,   String[] mpOptions,
                    JSONObject eeConstraints) {
            this.defaultGroup    = defaultGroup;
            this.defaultArtifact = defaultArtifact;
            this.defaultBuild    = defaultBuild;
            this.defaultJava     = defaultJava;
            this.defaultEe       = defaultEe;
            this.defaultMp       = defaultMp;
            this.buildOptions    = buildOptions;
            this.javaOptions     = javaOptions;
            this.eeOptions       = eeOptions;
            this.mpOptions       = mpOptions;
            this.eeConstraints   = eeConstraints;
        }
    }

    /**
     * Calls {@code GET /api/start/info}, parses the JSON response with
     * {@code org.json}, and returns a {@link StarterInfo}.
     * Returns {@code null} on any error; callers fall back to hard-coded defaults.
     */
    @Nullable
    static StarterInfo fetchStarterInfo() {
        try {
            URL url = new URL(STARTER_INFO_API);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(15_000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                LOGGER.warn("Liberty Starter /info returned HTTP " + conn.getResponseCode());
                return null;
            }

            String json;
            try (InputStream in = conn.getInputStream()) {
                json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } finally {
                conn.disconnect();
            }

            return parseStarterInfo(json);
        } catch (Exception ex) {
            LOGGER.warn("Failed to fetch Liberty Starter info", ex);
            return null;
        }
    }

    /** Parses the {@code /api/start/info} JSON payload using {@code org.json}. */
    @Nullable
    static StarterInfo parseStarterInfo(String json) {
        try {
            JSONObject root = new JSONObject(json);

            String defaultGroup    = root.getJSONObject("g").getString("default");
            String defaultArtifact = root.getJSONObject("a").getString("default");
            String defaultBuild    = root.getJSONObject("b").getString("default");
            String defaultJava     = root.getJSONObject("j").getString("default");
            String defaultEe       = root.getJSONObject("e").getString("default");
            String defaultMp       = root.getJSONObject("m").getString("default");

            String[] buildOptions = toStringArray(root.getJSONObject("b").getJSONArray("options"));
            String[] javaOptions  = toStringArray(root.getJSONObject("j").getJSONArray("options"));
            String[] eeOptions    = toStringArray(root.getJSONObject("e").getJSONArray("options"));
            String[] mpOptions    = toStringArray(root.getJSONObject("m").getJSONArray("options"));

            JSONObject eeConstraints = root.getJSONObject("e").optJSONObject("constraints");

            return new StarterInfo(defaultGroup, defaultArtifact,
                    defaultBuild, defaultJava, defaultEe, defaultMp,
                    buildOptions, javaOptions, eeOptions, mpOptions, eeConstraints);
        } catch (Exception ex) {
            LOGGER.warn("Failed to parse Liberty Starter info JSON", ex);
            return null;
        }
    }

    private static String[] toStringArray(JSONArray arr) {
        String[] result = new String[arr.length()];
        for (int i = 0; i < arr.length(); i++) {
            result[i] = arr.getString(i);
        }
        return result;
    }

    // ── GeneratorNewProjectWizard identity ────────────────────────────────────

    @NotNull
    @Override
    public String getId() { return "OpenLiberty"; }

    @NotNull
    @Override
    public String getName() { return "Open Liberty"; }

    @NotNull
    @Override
    public Icon getIcon() { return LibertyPluginIcons.libertyIcon; }

    // ── Step chain ────────────────────────────────────────────────────────────

    /**
     * Builds the step chain:
     * {@link RootNewProjectWizardStep} (propertyGraph + data)
     * → {@link NewProjectWizardBaseStep} (Name / Location)
     * → {@link LibertyNewProjectWizardStep} (our fields).
     */
    @NotNull
    @Override
    public NewProjectWizardStep createStep(@NotNull WizardContext context) {
        RootNewProjectWizardStep root = new RootNewProjectWizardStep(context);
        NewProjectWizardBaseStep base = new NewProjectWizardBaseStep(root);
        return NewProjectWizardChainStep.Companion.nextStep(base, LibertyNewProjectWizardStep::new);
    }

    // ── Static helper called from LibertyNewProjectWizardStep.setupProject ────

    /**
     * Downloads the starter ZIP and extracts it into {@code projectDir} on a
     * background thread, showing a modal progress dialog.
     */
    public static void downloadAndExtract(
            @NotNull Project project,
            @NotNull String  projectDir,
            @NotNull String  group,
            @NotNull String  artifact,
            @NotNull String  buildTool,
            @NotNull String  javaVersion,
            @NotNull String  eeVersion,
            @NotNull String  mpVersion) {

        String url = STARTER_API
                + "?g=" + encode(group)
                + "&a=" + encode(artifact)
                + "&b=" + encode(buildTool)
                + "&j=" + encode(javaVersion)
                + "&e=" + encode(eeVersion)
                + "&m=" + encode(mpVersion);

        LOGGER.info("Liberty Starter request: " + url);

        final String finalUrl = url;
        ProgressManager.getInstance().run(
                new Task.Modal(project, "Generating Liberty Project\u2026", false) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        indicator.setIndeterminate(true);
                        indicator.setText("Downloading from Open Liberty Starter\u2026");
                        try {
                            byte[] zipBytes = downloadZip(finalUrl, indicator);
                            indicator.setText("Extracting project\u2026");
                            unzip(zipBytes, projectDir, indicator);
                        } catch (Exception ex) {
                            LOGGER.warn("Liberty Starter generation failed", ex);
                            ApplicationManager.getApplication().invokeLater(() ->
                                    Messages.showErrorDialog(
                                            "Failed to generate Liberty project:\n" + ex.getMessage(),
                                            "Open Liberty Starter"));
                        }
                    }
                });
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static String encode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }

    private static byte[] downloadZip(String urlStr,
                                      ProgressIndicator indicator) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(60_000);
        conn.setRequestMethod("GET");

        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            throw new IOException("Open Liberty Starter returned HTTP " + status);
        }

        try (InputStream in = conn.getInputStream();
             ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buf.write(chunk, 0, read);
                indicator.checkCanceled();
            }
            return buf.toByteArray();
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Extracts {@code zipBytes} into {@code destDir}, stripping the single
     * top-level folder the Starter wraps its output in.
     */
    private static void unzip(byte[] zipBytes,
                               String destDir,
                               ProgressIndicator indicator) throws IOException {
        Path dest = Paths.get(destDir);
        Files.createDirectories(dest);
        String stripPrefix = null;

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                indicator.checkCanceled();
                String name = entry.getName();

                if (stripPrefix == null) {
                    int slash = name.indexOf('/');
                    stripPrefix = (slash >= 0) ? name.substring(0, slash + 1) : "";
                }
                if (!stripPrefix.isEmpty() && name.startsWith(stripPrefix)) {
                    name = name.substring(stripPrefix.length());
                }
                if (name.isEmpty()) { zis.closeEntry(); continue; }

                Path target = dest.resolve(name).normalize();
                if (!target.startsWith(dest)) {
                    throw new IOException("Zip-slip attempt: " + name);
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zis, target, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }

    public static class LibertyBuilder extends GeneratorNewProjectWizardBuilderAdapter {

        public LibertyBuilder() {
            super(new LibertyNewProjectWizard());
        }

        @Override
        public int getWeight() {
            // 1000 is an arbitrary number given for making the liberty generator come first in the generator list
            return JVM_WEIGHT + 1000;
        }
    }
}
