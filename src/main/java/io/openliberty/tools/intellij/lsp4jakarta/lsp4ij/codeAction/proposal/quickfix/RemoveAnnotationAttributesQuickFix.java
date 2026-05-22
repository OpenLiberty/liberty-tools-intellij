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
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Base class for removing annotation attributes.
 * Subclasses must implement methods to find the target annotation and its binding.
 */
public abstract class RemoveAnnotationAttributesQuickFix implements IJavaCodeActionParticipant {

    private final String[] attributes;
    private final String[] annotations;
    private static final Logger LOGGER = Logger.getLogger(RemoveAnnotationAttributesQuickFix.class.getName());

    public record AnnotationInfo(PsiAnnotation annotation, PsiModifierListOwner binding) {}

    public RemoveAnnotationAttributesQuickFix(String[] annotations, String... attributes) {
        this.annotations = annotations;
        this.attributes = attributes;
    }

    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        AnnotationInfo info = findAnnotationInfo(context.getCoveredNode());
        if (info != null) {
            String label = getLabel(info.annotation.getQualifiedName(), attributes);
            return List.of(JDTUtils.createCodeAction(context, diagnostic, label, getParticipantId()));
        }
        return List.of();
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        AnnotationInfo info = findAnnotationInfo(context.getCoveredNode());
        
        if (info != null) {
            String label = toResolve.getTitle();
            String targetAnnotation = annotations.length == 1 ? annotations[0] : null;
            ChangeCorrectionProposal proposal = new ModifyAnnotationProposal(
                label, 
                context.getSource().getCompilationUnit(),
                context.getASTRoot(), 
                info.binding, 
                info.annotation, 
                0, 
                targetAnnotation, 
                new ArrayList<>(), 
                Arrays.asList(attributes)
            );
            ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER, 
                "Unable to create workspace edit for code action " + label);
        }
        
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
    protected abstract String getLabel(String annotation, String[] attributes);

    protected String[] getAnnotations() {
        return annotations;
    }

    protected String[] getAttributes() {
        return attributes;
    }
}
