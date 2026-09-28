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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.websocket;

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

public class RemoveWebsocketAnnotationsQuickFix extends RemoveAnnotationConflictQuickFix {

    private String[] annotations = null;


    /**
     * Returns the unique identifier of this code action participant.
     *
     * @return the unique identifier of this code action participant
     */
    @Override
    public String getParticipantId() {
        return RemoveWebsocketAnnotationsQuickFix.class.getName();
    }

    /**
     *
     * @param context    the java code action context.
     * @param diagnostic the diagnostic which must be fixed and null otherwise.
     * @return list of CodeActions
     */
    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {

        PsiElement node = context.getCoveredNode();
        PsiElement parentType = getBinding(node);

        // Obtain the list of annotations from the diagnostic.
        JsonArray diagnosticData = (JsonArray) diagnostic.getData();
        annotations = StreamSupport.stream(diagnosticData.spliterator(), false).map(JsonElement::getAsString).toArray(String[]::new);

        if (parentType != null) {
            List<CodeAction> codeActions = new ArrayList<>();
            removeAnnotation(diagnostic, context, codeActions, annotations);
            return codeActions;
        }

        return null;
    }

    /**
     *
     * @return list of annotations to remove
     */
    @Override
    public String[] getAnnotations() {
        return annotations;
    }
}
