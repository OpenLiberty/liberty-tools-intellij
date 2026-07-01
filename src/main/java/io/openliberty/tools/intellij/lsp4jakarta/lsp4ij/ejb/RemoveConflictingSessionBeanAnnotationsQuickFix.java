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

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb;

import com.google.gson.JsonArray;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationConflictQuickFix;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * QuickFix for removing conflicting EJB session bean stereotype annotations.
 * For each conflicting annotation, generates a code action to keep that annotation
 * and remove all others.
 */
public class RemoveConflictingSessionBeanAnnotationsQuickFix extends RemoveAnnotationConflictQuickFix {

    @Override
    public String getParticipantId() {
        return RemoveConflictingSessionBeanAnnotationsQuickFix.class.getName();
    }

    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        List<CodeAction> codeActions = new ArrayList<>();
        if (getBinding(context.getCoveredNode()) == null) {
            return codeActions;
        }
        if (diagnostic.getData() == null) {
            return codeActions;
        }
        JsonArray diagnosticData = (JsonArray) diagnostic.getData();
        List<String> annotations = IntStream.range(0, diagnosticData.size())
                .mapToObj(idx -> diagnosticData.get(idx).getAsString())
                .collect(Collectors.toList());
        for (String annotation : annotations) {
            List<String> resultingAnnotations = new ArrayList<>(annotations);
            resultingAnnotations.remove(annotation);
            removeAnnotation(diagnostic, context, codeActions,
                    resultingAnnotations.toArray(new String[] {}));
        }
        return codeActions;
    }
}
