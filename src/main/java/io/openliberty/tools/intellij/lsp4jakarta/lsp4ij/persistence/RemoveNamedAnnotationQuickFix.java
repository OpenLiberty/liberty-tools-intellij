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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.intellij.psi.PsiElement;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationConflictQuickFix;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Quick fix that removes annotations listed in the diagnostic data.
 * The annotation class names to remove are read from {@link Diagnostic#getData()}
 * (a {@link JsonArray} of strings) rather than being hardcoded.
 */
public class RemoveNamedAnnotationQuickFix extends RemoveAnnotationConflictQuickFix {

    public RemoveNamedAnnotationQuickFix() {
        super();
    }

    @Override
    public String getParticipantId() {
        return RemoveNamedAnnotationQuickFix.class.getName();
    }

    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        // The annotations to remove are supplied by the diagnostics collector via
        // diagnostic.getData() as a JsonArray of fully qualified annotation name strings.
        if (!(diagnostic.getData() instanceof JsonArray diagnosticData) || diagnosticData.isEmpty()) {
            return Collections.emptyList();
        }

        PsiElement node = context.getCoveredNode();
        PsiElement parentType = getBinding(node);
        if (parentType == null) {
            return Collections.emptyList();
        }

        List<CodeAction> codeActions = new ArrayList<>();
        String[] annotations = StreamSupport.stream(diagnosticData.spliterator(), false)
                .map(JsonElement::getAsString).toArray(String[]::new);
        removeAnnotation(diagnostic, context, codeActions, annotations);
        return codeActions;
    }
}
