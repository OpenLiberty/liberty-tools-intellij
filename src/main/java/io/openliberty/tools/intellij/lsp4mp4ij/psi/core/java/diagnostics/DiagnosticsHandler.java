/*******************************************************************************
 * Copyright (c) 2020, 2025 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.diagnostics;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.psi.PsiFile;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.java.corrections.JavaDiagnosticsDefinition;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4mp.commons.DocumentFormat;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DiagnosticsHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsHandler.class);

    private final String group;

    public DiagnosticsHandler(String group) {
        this.group = group;
    }

    public List<PublishDiagnosticsParams> collectDiagnostics(MicroProfileJavaDiagnosticsParams params, IPsiUtils utils) {
        List<String> uris = params.getUris();
        if (uris == null) {
            return Collections.emptyList();
        }
        DocumentFormat documentFormat = params.getDocumentFormat();
        List<PublishDiagnosticsParams> publishDiagnostics = new ArrayList<>();
        for (String uri : uris) {
            List<Diagnostic> diagnostics = new ArrayList<>();
            PublishDiagnosticsParams publishDiagnostic = new PublishDiagnosticsParams(uri, diagnostics);
            publishDiagnostics.add(publishDiagnostic);
            collectDiagnostics(uri, utils, documentFormat, params.getSettings(), diagnostics);
        }
        return publishDiagnostics;
    }

    private void collectDiagnostics(String uri, IPsiUtils utils, DocumentFormat documentFormat,
                                    MicroProfileJavaDiagnosticsSettings settings, List<Diagnostic> diagnostics) {
        PsiFile typeRoot = ApplicationManager.getApplication().runReadAction((Computable<PsiFile>) () -> resolveTypeRoot(uri, utils));
        if (typeRoot == null) {
            return;
        }

        try {
            Module module = ApplicationManager.getApplication().runReadAction((ThrowableComputable<Module, IOException>) () -> utils.getModule(uri));
            // Collect all adapted diagnostic definitions
            JavaDiagnosticsContext context = new JavaDiagnosticsContext(uri, typeRoot, utils, module, documentFormat, settings);
            List<JavaDiagnosticsDefinition> definitions = JavaDiagnosticsDefinition.EP_NAME.getExtensionList()
                    .stream()
                    .filter(definition -> group.equals(definition.getGroup()))
                    .filter(definition -> definition.isAdaptedForDiagnostics(context))
                    .toList();

            // Begin, collect, end participants
            definitions.forEach(definition -> definition.beginDiagnostics(context));
            definitions.forEach(definition -> {
                List<Diagnostic> collectedDiagnostics = definition.collectDiagnostics(context);
                if (collectedDiagnostics != null && !collectedDiagnostics.isEmpty()) {
                    diagnostics.addAll(collectedDiagnostics);
                }
            });
            definitions.forEach(definition -> definition.endDiagnostics(context));
        } catch (IOException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }

    // REVISIT: Make this a public method on a common utility class?
    private static PsiFile resolveTypeRoot(String uri, IPsiUtils utils) {
        return utils.resolveCompilationUnit(uri);
    }
}
