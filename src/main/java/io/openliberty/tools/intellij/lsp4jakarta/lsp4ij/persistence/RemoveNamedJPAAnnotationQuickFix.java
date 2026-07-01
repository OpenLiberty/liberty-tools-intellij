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
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Quick fix for removing named JPA annotations
 * (@NamedEntityGraph, @NamedEntityGraphs, @NamedQuery, @NamedQueries, @NamedNativeQuery, @NamedNativeQueries)
 * when they are applied to a class that does not meet the annotation requirements.
 */
public class RemoveNamedJPAAnnotationQuickFix extends RemoveAnnotationConflictQuickFix {

    private String[] annotations = null;

    public RemoveNamedJPAAnnotationQuickFix() {
        super();
    }

    @Override
    public String getParticipantId() {
        return RemoveNamedJPAAnnotationQuickFix.class.getName();
    }

    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        PsiElement node = context.getCoveredNode();
        PsiElement parentType = getBinding(node);

        JsonArray diagnosticData = (JsonArray) diagnostic.getData();
        annotations = StreamSupport.stream(diagnosticData.spliterator(), false)
                .map(JsonElement::getAsString).toArray(String[]::new);

        if (parentType != null) {
            List<CodeAction> codeActions = new ArrayList<>();
            removeAnnotation(diagnostic, context, codeActions, annotations);
            return codeActions;
        }
        return null;
    }

    @Override
    public String[] getAnnotations() {
        return annotations;
    }
}
