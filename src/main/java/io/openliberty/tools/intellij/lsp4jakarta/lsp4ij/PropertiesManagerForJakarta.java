/*******************************************************************************
 * Copyright (c) 2020, 2025 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 * IBM Corporation - handle Jakarta
 ******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.completion.CompletionHandler;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.diagnostics.DiagnosticsHandler;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.java.codeaction.CodeActionHandler;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4jakarta.commons.*;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeActionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCompletionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsSettings;

import java.util.Collections;
import java.util.List;

public final class PropertiesManagerForJakarta {

    private static final String GROUP_NAME = "jakarta";

    private static final PropertiesManagerForJakarta INSTANCE = new PropertiesManagerForJakarta();

    public static PropertiesManagerForJakarta getInstance() {
        return INSTANCE;
    }

    private final CompletionHandler completionHandler;

    private final CodeActionHandler codeActionHandler;

    private final DiagnosticsHandler diagnosticsHandler;

    private PropertiesManagerForJakarta() {
        completionHandler = new CompletionHandler(GROUP_NAME);
        codeActionHandler = new CodeActionHandler(GROUP_NAME);
        diagnosticsHandler = new DiagnosticsHandler(GROUP_NAME);
    }

    /**
     * Returns diagnostics for the given uris list.
     *
     * @param params the diagnostics parameters
     * @param utils  the IPsiUtils
     * @return diagnostics for the given uris list.
     */
    public List<PublishDiagnosticsParams> diagnostics(JakartaJavaDiagnosticsParams params, IPsiUtils utils) {
        return diagnosticsHandler.collectDiagnostics(adapt(params), utils);
    }

    /**
     * Returns the CompletionItems given the completion item params
     *
     * @param params  the completion item params
     * @param utils   the IPsiUtils
     * @return the CompletionItems for the given the completion item params
     */
    public CompletionList completion(JakartaJavaCompletionParams params, IPsiUtils utils) {
        return completionHandler.completion(adapt(params), utils);
    }

    /**
     * Returns the cursor context for the given file and cursor position.
     *
     * @param params  the completion params that provide the file and cursor
     *                position to get the context for
     * @param utils   the IPsiUtils
     * @return the cursor context for the given file and cursor position
     */
    public JavaCursorContextResult javaCursorContext(JakartaJavaCompletionParams params, IPsiUtils utils) {
        return adapt(completionHandler.javaCursorContext(adapt(params), utils));
    }

    /**
     * Returns the list of code actions for the given diagnostics. The code
     * actions in this list may have already been resolved, or they may be
     * resolved later.
     *
     * @param params  the code action parameters
     * @param utils   the IPsiUtils
     * @return the list of code actions for the given diagnostics
     */
    public List<? extends CodeAction> getCodeAction(JakartaJavaCodeActionParams params, IPsiUtils utils) {
        return ApplicationManager.getApplication().runReadAction((Computable<List<? extends CodeAction>>) () ->
                codeActionHandler.codeAction(adapt(params), utils));
    }

    /**
     * Resolves and returns the given code action.
     *
     * @param unresolved the unresolved code action
     * @param utils      the IPsiUtils
     * @return the resolved code action
     */
    public CodeAction resolveCodeAction(CodeAction unresolved, IPsiUtils utils) {
        return ApplicationManager.getApplication().runReadAction((Computable<CodeAction>) () ->
                codeActionHandler.resolveCodeAction(unresolved, utils));
    }

    // REVISIT: The "adapt" methods in this class are being used to convert between data structures
    // from LSP4MP and LSPJakarta that are otherwise identical except for their class names. Once
    // LSP4MP and LSP4Jakarta have a common/unified client API, the "adapt" methods can be removed.

    private MicroProfileJavaCompletionParams adapt(JakartaJavaCompletionParams params) {
        return new MicroProfileJavaCompletionParams(params.getUri(), params.getPosition());
    }

    private JavaCursorContextResult adapt(org.eclipse.lsp4mp.commons.JavaCursorContextResult contextResult) {
        if (contextResult != null) {
            var kind = contextResult.getKind();
            if(kind != null)
                return new JavaCursorContextResult(JavaCursorContextKind.forValue(kind.getValue()), contextResult.getPrefix());
        }
        return null;
    }

    private MicroProfileJavaDiagnosticsParams adapt(JakartaJavaDiagnosticsParams params) {
        JakartaJavaDiagnosticsSettings settings = params.getSettings();
        MicroProfileJavaDiagnosticsParams mpParams = new MicroProfileJavaDiagnosticsParams(params.getUris(),
                new MicroProfileJavaDiagnosticsSettings(settings != null ? settings.getPatterns() : Collections.emptyList()));
        DocumentFormat df = params.getDocumentFormat();
        mpParams.setDocumentFormat(df != null ? org.eclipse.lsp4mp.commons.DocumentFormat.forValue(df.getValue()) : null);
        return mpParams;
    }

    private MicroProfileJavaCodeActionParams adapt(JakartaJavaCodeActionParams params) {
        MicroProfileJavaCodeActionParams mpParams = new MicroProfileJavaCodeActionParams(params.getTextDocument(), params.getRange(), params.getContext());
        mpParams.setResourceOperationSupported(params.isResourceOperationSupported());
        mpParams.setResolveSupported(params.isResolveSupported());
        return mpParams;
    }
}
