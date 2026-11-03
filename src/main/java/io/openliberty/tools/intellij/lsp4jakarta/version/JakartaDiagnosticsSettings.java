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

import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsSettings;

import java.util.Collections;
import java.util.List;

/**
 * Jakarta diagnostics settings. Extends the LSP4MP settings, which is what the shared diagnostics
 * infrastructure ({@code JavaDiagnosticsContext}) carries, with the Jakarta EE version information that
 * the Jakarta language server sends in {@code JakartaJavaDiagnosticsSettings}.
 */
public class JakartaDiagnosticsSettings extends MicroProfileJavaDiagnosticsSettings {

    private final String selectedVersion;
    private final List<String> availableVersions;

    public JakartaDiagnosticsSettings(List<String> patterns, String selectedVersion, List<String> availableVersions) {
        super(patterns);
        this.selectedVersion = selectedVersion;
        this.availableVersions = availableVersions == null ? Collections.emptyList() : availableVersions;
    }

    /**
     * Returns the Jakarta EE version label selected for the project, or null if none is selected.
     */
    public String getSelectedVersion() {
        return selectedVersion;
    }

    /**
     * Returns the Jakarta EE version labels available for the project, never null.
     */
    public List<String> getAvailableVersions() {
        return availableVersions;
    }
}
