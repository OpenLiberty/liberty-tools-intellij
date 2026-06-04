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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix;

import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.ModifyAnnotationProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.util.ExceptionUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * QuickFix for removing attributes from annotations.
 * Uses a Map structure to directly associate each annotation with its attributes to remove.
 */
public abstract class RemoveAnnotationAttributesQuickFix implements IJavaCodeActionParticipant {

    private static final Logger LOGGER = Logger.getLogger(RemoveAnnotationAttributesQuickFix.class.getName());

    /** Map of annotation names to their attributes to remove. */
    private final Map<String, List<String>> annotationAttributesMap;

    public record AnnotationInfo(PsiAnnotation annotation, PsiModifierListOwner binding) {}

    /**
     * Constructor accepting a map of annotation names to their attributes to remove.
     * 
     * @param annotationAttributesMap Map of annotation names to their attributes to remove
     */
    public RemoveAnnotationAttributesQuickFix(Map<String, List<String>> annotationAttributesMap) {
        this.annotationAttributesMap = annotationAttributesMap;
    }

    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        AnnotationInfo info = findAnnotationInfo(context.getCoveredNode());
        if (info != null) {
            String annotationName = info.annotation.getQualifiedName();
            List<String> attributes = annotationAttributesMap.get(annotationName);
            String label = getLabel(annotationName, attributes);
            return List.of(JDTUtils.createCodeAction(context, diagnostic, label, getParticipantId()));
        }
        return List.of();
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        AnnotationInfo info = findAnnotationInfo(context.getCoveredNode());
            String annotationName = info.annotation.getQualifiedName();
            List<String> attributes = annotationAttributesMap.get(annotationName);
                String label = getLabel(annotationName, attributes);
                ChangeCorrectionProposal proposal = new ModifyAnnotationProposal(
                    label,
                    context.getSource().getCompilationUnit(),
                    context.getASTRoot(),
                    info.binding,
                    info.annotation,
                    0,
                    annotationName,
                    new ArrayList<>(),
                    attributes
                );
                ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER,
                    "Unable to create workspace edit for code action " + label);
        return toResolve;
    }

    /**
     * Finds the target annotation and its binding.
     * Subclasses must implement this to locate the annotation based on their specific requirements.
     * 
     * @param node the PSI element to start searching from
     * @return AnnotationInfo containing the annotation and its binding, or null if not found
     */
    protected abstract AnnotationInfo findAnnotationInfo(PsiElement node);

    /**
     * Returns the label for the code action.
     *
     * @param annotation the fully qualified annotation name
     * @param attributes the attributes to remove
     * @return the label text
     */
    protected abstract String getLabel(String annotation, List<String> attributes);

    /**
     * Gets the map of annotations to their attributes to remove.
     * 
     * @return the annotation attributes map
     */
    protected Map<String, List<String>> getAnnotationAttributesMap() {
        return annotationAttributesMap;
    }
}
