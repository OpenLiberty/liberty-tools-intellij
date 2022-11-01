/*******************************************************************************
 * Copyright (c) 2020,2022 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 * IBM Corporation - handle Jakarta
 ******************************************************************************/

package com.langserver.devtools.intellij.lsp4jakarta.lsp4ij;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.psi.*;
import com.langserver.devtools.intellij.lsp4jakarta.lsp4ij.annotations.AnnotationDiagnosticsCollector;
import com.langserver.devtools.intellij.lsp4jakarta.lsp4ij.beanvalidation.BeanValidationDiagnosticsCollector;
import com.langserver.devtools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanDiagnosticsCollector;
import com.langserver.devtools.intellij.lsp4jakarta.lsp4ij.di.DependencyInjectionDiagnosticsCollector;
import com.langserver.devtools.intellij.lsp4jakarta.lsp4ij.jax_rs.Jax_RSClassDiagnosticsCollector;
import com.langserver.devtools.intellij.lsp4jakarta.lsp4ij.jax_rs.ResourceMethodDiagnosticsCollector;
import com.langserver.devtools.intellij.lsp4jakarta.lsp4ij.jsonb.JsonbDiagnosticsCollector;
import com.langserver.devtools.intellij.lsp4jakarta.lsp4ij.jsonp.JsonpDiagnosticCollector;
import com.langserver.devtools.intellij.lsp4jakarta.lsp4ij.persistence.PersistenceEntityDiagnosticsCollector;
import com.langserver.devtools.intellij.lsp4jakarta.lsp4ij.persistence.PersistenceMapKeyDiagnosticsCollector;
import com.langserver.devtools.intellij.lsp4jakarta.lsp4ij.servlet.FilterDiagnosticsCollector;
import com.langserver.devtools.intellij.lsp4jakarta.lsp4ij.servlet.ListenerDiagnosticsCollector;
import com.langserver.devtools.intellij.lsp4jakarta.lsp4ij.servlet.ServletDiagnosticsCollector;
import com.langserver.devtools.intellij.lsp4jakarta.lsp4ij.websocket.WebSocketDiagnosticsCollector;
import com.langserver.devtools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4mp.commons.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.lsp4jakarta.commons.JakartaDiagnosticsParams;

public class PropertiesManagerForJakarta {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesManagerForJakarta.class);

    private static final PropertiesManagerForJakarta INSTANCE = new PropertiesManagerForJakarta();

    public static PropertiesManagerForJakarta getInstance() {
        return INSTANCE;
    }

    private List<DiagnosticsCollector> diagnosticsCollectors = new ArrayList<>();

    private PropertiesManagerForJakarta() {
        diagnosticsCollectors.add(new ServletDiagnosticsCollector());
        diagnosticsCollectors.add(new AnnotationDiagnosticsCollector());
        diagnosticsCollectors.add(new FilterDiagnosticsCollector());
        diagnosticsCollectors.add(new ListenerDiagnosticsCollector());
        diagnosticsCollectors.add(new BeanValidationDiagnosticsCollector());
        diagnosticsCollectors.add(new PersistenceEntityDiagnosticsCollector());
        diagnosticsCollectors.add(new PersistenceMapKeyDiagnosticsCollector());
        diagnosticsCollectors.add(new ResourceMethodDiagnosticsCollector());
        diagnosticsCollectors.add(new Jax_RSClassDiagnosticsCollector());
        diagnosticsCollectors.add(new JsonbDiagnosticsCollector());
        diagnosticsCollectors.add(new ManagedBeanDiagnosticsCollector());
        diagnosticsCollectors.add(new DependencyInjectionDiagnosticsCollector());
        diagnosticsCollectors.add(new JsonpDiagnosticCollector());
        diagnosticsCollectors.add(new WebSocketDiagnosticsCollector());
    }

    /**
     * Returns diagnostics for the given uris list.
     *
     * @param params the diagnostics parameters
     * @param utils  the utilities class
     * @return diagnostics for the given uris list.
     */
    public List<PublishDiagnosticsParams> diagnostics(JakartaDiagnosticsParams params, IPsiUtils utils) {
        List<String> uris = params.getUris();
        if (uris == null) {
            return Collections.emptyList();
        }
        DocumentFormat documentFormat = params.getDocumentFormat();
        List<PublishDiagnosticsParams> publishDiagnostics = new ArrayList<PublishDiagnosticsParams>();
        for (String uri : uris) {
            List<Diagnostic> diagnostics = new ArrayList<>();
            PublishDiagnosticsParams publishDiagnostic = new PublishDiagnosticsParams(uri, diagnostics);
            publishDiagnostics.add(publishDiagnostic);
            collectDiagnostics(uri, utils, documentFormat, diagnostics);
        }
        return publishDiagnostics;
    }

    private void collectDiagnostics(String uri, IPsiUtils utils, DocumentFormat documentFormat, List<Diagnostic> diagnostics) {
        PsiFile typeRoot = ApplicationManager.getApplication().runReadAction((Computable<PsiFile>) () -> resolveTypeRoot(uri, utils));
        if (typeRoot == null) {
            return;
        }

        try {
            if (typeRoot instanceof PsiJavaFile) {
                Module module = ApplicationManager.getApplication().runReadAction((ThrowableComputable<Module, IOException>) () -> utils.getModule(uri));
                DumbService.getInstance(module.getProject()).runReadActionInSmartMode(() -> {
                    PsiJavaFile unit = (PsiJavaFile) typeRoot;
                    for (DiagnosticsCollector collector : diagnosticsCollectors) {
                        collector.collectDiagnostics(unit, diagnostics);
                    }
                });
            }
        } catch (IOException e) {
            LOGGER.warn(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Given the uri returns a {@link PsiFile}. May return null if it can not
     * associate the uri with a Java file ot class file.
     *
     * @param uri
     * @param utils   JDT LS utilities
     * @return compilation unit
     */
    private static PsiFile resolveTypeRoot(String uri, IPsiUtils utils) {
        return utils.resolveCompilationUnit(uri);
    }

}
