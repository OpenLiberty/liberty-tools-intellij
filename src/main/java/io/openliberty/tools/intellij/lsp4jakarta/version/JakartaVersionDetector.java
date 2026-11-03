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

import com.intellij.openapi.module.Module;
import org.eclipse.lsp4jakarta.version.JakartaVersion;

import java.util.Arrays;
import java.util.List;

/**
 * Detects the Jakarta EE versions available to a project.
 *
 * TODO: This is a hardcoded template. Replace the body of {@link #detectVersions(Module)} with the real
 * detection logic. The JDT implementation in lsp4jakarta (org.eclipse.lsp4jakarta.version.JakartaVersionFinder)
 * uses these strategies, which need to be ported to IntelliJ module/library APIs:
 * <ul>
 *   <li>JAR manifest: Bundle-SymbolicName + Specification/Implementation/Bundle-Version of Jakarta JARs</li>
 *   <li>JAR file name: "name-version.jar" mapped to a Jakarta EE version</li>
 *   <li>Class signature: presence of version specific classes/methods on the module classpath</li>
 * </ul>
 * The result is the list of ALL Jakarta EE versions found. If it contains two or more versions the user is
 * prompted to select one. If nothing is detected, the JDT implementation falls back to
 * {@link JakartaVersion#EE_9}.
 */
public final class JakartaVersionDetector {

    private JakartaVersionDetector() {
    }

    /**
     * Returns the Jakarta EE versions detected for the given module.
     *
     * @param module the module to detect the Jakarta EE versions for
     * @return the detected Jakarta EE versions, never null or empty
     */
    public static List<JakartaVersion> detectVersions(Module module) {
        // TODO: replace with real detection. Hardcoded so that the version selection flow can be exercised.
        // Return a single version, e.g. List.of(JakartaVersion.EE_10), to skip the selection prompt.
        return Arrays.asList(JakartaVersion.EE_11, JakartaVersion.EE_10, JakartaVersion.EE_9);
    }
}
