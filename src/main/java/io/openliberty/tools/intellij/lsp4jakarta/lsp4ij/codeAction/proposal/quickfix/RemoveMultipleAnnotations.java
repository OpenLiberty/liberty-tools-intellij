/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation, Adit Rada - initial API and implementation
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix;

import com.google.gson.JsonArray;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class is used to provide options to remove multiple annotations
 * at the same time. For example, "Remove @A, @B", "Remove @C, @D, @E".
 *
 * @author Adit Rada
 *
 */
public abstract class RemoveMultipleAnnotations extends RemoveAnnotationConflictQuickFix {

    public RemoveMultipleAnnotations() {
        // annotation list to be derived from the diagnostic passed to
        // `getCodeActions()`
        super();
    }

    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        PsiElement node = context.getCoveredNode();
        PsiElement parentType = getBinding(node);

        // Obtain the list of annotations from the diagnostic.
        JsonArray diagnosticData = (JsonArray) diagnostic.getData();

        List<String> annotations = IntStream.range(0, diagnosticData.size())
                .mapToObj(idx -> diagnosticData.get(idx).getAsString()).collect(Collectors.toList());

        if (parentType != null) {
            List<CodeAction> codeActions = new ArrayList<>();

            List<List<String>> annotationsListsToRemove = getMultipleRemoveAnnotations(parentType.getProject(), annotations);
            for (List<String> annotationList : annotationsListsToRemove) {

                // For each list we will create one code action in its own context
                String[] annotationsToRemove = annotationList.toArray(new String[annotationList.size()]);
                removeAnnotation(diagnostic, context, codeActions, annotationsToRemove);
            }
            return codeActions;
        }
        return null;
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        return super.resolveCodeAction(context);
    }

    /**
     * Each List in the returned List of Lists should be a set of annotations that
     * will be removed at one go. For example, to provide the user with the option to remove
     * "@A, @B" and "@C". The return should be [[A, B], [C]]
     *
     * @param project  The project is the context in which the annotation short names will be resolved to FQnames
     * @param annotations All the annotations present on the member.
     * @return A List of Lists, with each list containing the annotations that must be
     * removed at the same time.
     * @author Adit Rada
     *
     */
    protected abstract List<List<String>> getMultipleRemoveAnnotations(Project project, List<String> annotations);
}
