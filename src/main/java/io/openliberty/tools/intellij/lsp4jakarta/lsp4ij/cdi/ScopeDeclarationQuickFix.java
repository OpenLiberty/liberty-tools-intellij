/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi;

import com.google.gson.JsonArray;
import com.intellij.psi.PsiElement;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationConflictQuickFix;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ScopeDeclarationQuickFix extends RemoveAnnotationConflictQuickFix {
    public ScopeDeclarationQuickFix() {
        // annotation list to be derived from the diagnostic passed to
        // `getCodeActions()`
        super();
    }

    @Override
    public String getParticipantId() {
        return ScopeDeclarationQuickFix.class.getName();
    }

    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        PsiElement node = context.getCoveredNode();
        PsiElement parentType = getBinding(node);

        JsonArray diagnosticData = (JsonArray) diagnostic.getData();

        List<String> annotations = IntStream.range(0, diagnosticData.size())
                .mapToObj(idx -> diagnosticData.get(idx).getAsString()).collect(Collectors.toList());

        annotations.remove(ManagedBeanConstants.PRODUCES_FQ_NAME);

        if (parentType != null) {

            List<CodeAction> codeActions = new ArrayList<>();
            /**
             * for each annotation, choose the current annotation to keep and remove the
             * rest since we can have at most one scope annotation.
             */
            for (String annotation : annotations) {
                List<String> resultingAnnotations = new ArrayList<>(annotations);
                resultingAnnotations.remove(annotation);
                removeAnnotation(diagnostic, context, codeActions,
                        resultingAnnotations.toArray(new String[] {}));
            }

            return codeActions;
        }
        return null;

    }
}
