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
 *     IBM Corporation - initial implementation
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix;

import com.google.gson.JsonArray;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ReplaceAnnotationProposal;
import io.openliberty.tools.intellij.util.ExceptionUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils.getSimpleName;

/**
 * Abstract base class for quickfixes that replace annotations.
 * Provides common functionality for extracting annotation data from diagnostics
 * and creating code actions that replace multiple annotations with a single annotation.
 */
public abstract class ReplaceAnnotationsQuickFix extends InsertAnnotationMissingQuickFix {

    /** Logger object to record events for this class. */
    private static final Logger LOGGER = Logger.getLogger(ReplaceAnnotationsQuickFix.class.getName());

    /**
     * Formats a list of fully qualified annotation names for display.
     * Extracts simple names and joins them with commas and "and" before the last one, prefixed with @.
     *
     * @param annotationFqNames List of fully qualified annotation names
     * @return Formatted string (e.g., "@RequestScoped" or "@RequestScoped and @SessionScoped")
     */
    protected String formatAnnotationNames(List<String> annotationFqNames) {
        List<String> names = annotationFqNames.stream()
                .map(fqName -> "@" + getSimpleName(fqName))
                .toList();

        if (names.isEmpty()) return "";
        if (names.size() == 1) return names.get(0);

        String allButLast = String.join(", ", names.subList(0, names.size() - 1));
        return String.join(" and ", allButLast, names.getLast());
    }

    /**
     * Constructor.
     *
     * @param annotation The fully qualified name of the annotation to insert
     */
    public ReplaceAnnotationsQuickFix(String annotation) {
        super(annotation);
    }

    /**
     * Extracts the list of annotation fully qualified names from diagnostic data.
     *
     * @param diagnostic The diagnostic containing annotation data
     * @return List of fully qualified annotation names, or null if data is invalid
     */
    private List<String> extractAnnotationsFromDiagnostic(Diagnostic diagnostic) {
        JsonArray diagnosticData = (JsonArray) diagnostic.getData();
        if (diagnosticData == null || diagnosticData.isEmpty()) {
            return null;
        }

        return IntStream.range(0, diagnosticData.size())
                .mapToObj(idx -> diagnosticData.get(idx).getAsString())
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void insertAnnotations(Diagnostic diagnostic, JavaCodeActionContext context,
                                     List<CodeAction> codeActions) {
        List<String> annotationsToRemove = extractAnnotationsFromDiagnostic(diagnostic);
        if (annotationsToRemove == null) {
            return;
        }
        // Create code action
        codeActions.add(JDTUtils.createCodeAction(context, diagnostic, getCodeActionLabel(annotationsToRemove), getParticipantId()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        // Get the diagnostic to extract annotations to remove
        Diagnostic diagnostic = toResolve.getDiagnostics().get(0);
        List<String> annotationsToRemove = extractAnnotationsFromDiagnostic(diagnostic);
        if (annotationsToRemove == null) {
            return toResolve;
        }
        // Convert to array of fully qualified names for ReplaceAnnotationProposal
        String[] fqNames = annotationsToRemove.toArray(new String[0]);
        PsiModifierListOwner parentType = PsiTreeUtil.getParentOfType(context.getCoveringNode(), PsiClass.class);
        // Create a proposal that replaces all annotations
        ChangeCorrectionProposal proposal = new ReplaceAnnotationProposal(toResolve.getTitle(), context.getCompilationUnit(),
                context.getASTRoot(), parentType, 0, getAnnotations()[0],
                context.getSource().getCompilationUnit(), fqNames);
        ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER,
                "Unable to create workspace edit for code action to replace annotations");
        return toResolve;
    }

    /**
     * Returns the code action label for the given list of annotation fully qualified names.
     * Subclasses should override this to provide custom labels based on the annotations to remove.
     *
     * @param annotationsToRemove List of fully qualified annotation names to be removed
     * @return The code action label
     */
    protected abstract String getCodeActionLabel(List<String> annotationsToRemove);
}
