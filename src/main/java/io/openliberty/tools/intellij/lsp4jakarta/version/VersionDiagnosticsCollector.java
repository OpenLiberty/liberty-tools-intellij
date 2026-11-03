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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiFile;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.diagnostics.IJavaDiagnosticsParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.diagnostics.JavaDiagnosticsContext;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;

import java.util.Collections;
import java.util.List;

/**
 * Jakarta version diagnostics participant.
 *
 * Adds a persistent warning on the first line of every Java file of a project that has more than one
 * Jakarta EE version available, so that the user can change the Jakarta EE version of the project.
 */
public class VersionDiagnosticsCollector implements IJavaDiagnosticsParticipant {

    public static final String DIAGNOSTIC_SOURCE = "jakarta-version";

    @Override
    public List<Diagnostic> collectDiagnostics(JavaDiagnosticsContext context) {
        if (!(context.getSettings() instanceof JakartaDiagnosticsSettings settings)) {
            // TODO remove: temporary check that the version reaches the participant
            System.out.println("[Jakarta version] settings are not JakartaDiagnosticsSettings: " + context.getSettings());
            return Collections.emptyList();
        }

        // TODO remove: temporary check that the version reaches the participant
        System.out.println("[Jakarta version] selected=" + settings.getSelectedVersion()
                + ", available=" + settings.getAvailableVersions() + ", file=" + context.getUri());

        // Only show the diagnostic if there are 2 or more available versions
        if (settings.getAvailableVersions().size() <= 1) {
            return Collections.emptyList();
        }

        PsiFile unit = context.getTypeRoot();
        if (unit == null) {
            return Collections.emptyList();
        }

        String selectedVersion = settings.getSelectedVersion();
        String message = selectedVersion != null
                ? Messages.getMessage("MultipleJakartaEEVersionsSelected", selectedVersion)
                : Messages.getMessage("MultipleJakartaEEVersions");

        Range range = ApplicationManager.getApplication().runReadAction((Computable<Range>) () -> getFirstLineRange(context, unit));
        if (range == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(context.createDiagnostic(context.getUri(), message, range,
                DIAGNOSTIC_SOURCE, JakartaVersionErrorCode.MultipleJakartaEEVersions, DiagnosticSeverity.Warning));
    }

    /**
     * Returns the range of the first line of the file. The length is at least 1 so that the range is valid
     * even if the first line is empty.
     */
    private static Range getFirstLineRange(JavaDiagnosticsContext context, PsiFile unit) {
        String source = unit.getText();
        int firstLineLength;
        if (source == null || source.isEmpty()) {
            firstLineLength = 1;
        } else {
            int newlineIndex = source.indexOf('\n');
            firstLineLength = Math.max(1, newlineIndex == -1 ? source.length() : newlineIndex);
        }
        return context.getUtils().toRange(unit, 0, firstLineLength);
    }
}
