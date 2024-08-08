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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence;


import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.ModifyAnnotationProposal;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.InsertAnnotationMissingQuickFix;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.WorkspaceEdit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * QuickFix for fixing {@link PersistenceConstants#DIAGNOSTIC_CODE_MISSING_ATTRIBUTES} error
 * by providing several code actions* to add the missing elements to the existing annotations:
 *
 * {@link PersistenceConstants#DIAGNOSTIC_CODE_MISSING_ATTRIBUTES}
 * <ul>
 * <li> Add the `name` attribute to the `@MapKeyJoinColumn` annotation
 * <li> Add the `referencedColumnName` attribute to the `@MapKeyJoinColumn` annotation
 * </ul>
 *
 * @author Leslie Dawson (lamminade)
 *
 * * Or only one code action to fix all annotations.
 */
public class PersistenceAnnotationQuickFix extends InsertAnnotationMissingQuickFix {
    private static final Logger LOGGER = Logger.getLogger(PersistenceAnnotationQuickFix.class.getName());

    public PersistenceAnnotationQuickFix() {
        super("jakarta.persistence.MapKeyJoinColumn");
    }

    @Override
    protected void insertAnnotations(Diagnostic diagnostic, JavaCodeActionContext context,
                                     List<CodeAction> codeActions) {
        insertAndReplaceAnnotation(diagnostic, context, codeActions);
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        ArrayList<String> attributes = new ArrayList<>();
        attributes.add("name");
        attributes.add("referencedColumnName");
        String name = toResolve.getTitle();
        PsiElement node = context.getCoveredNode();
        PsiModifierListOwner binding = getBinding(node);
        List<PsiAnnotation> annotationNodes = getAnnotations(binding, this.getAnnotations());
        for (PsiAnnotation annotationNode : annotationNodes) {
            ChangeCorrectionProposal proposal = new ModifyAnnotationProposal(name, context.getSource().getCompilationUnit(),
                    context.getASTRoot(), binding, annotationNode, 0, attributes, this.getAnnotations());
            try {
                WorkspaceEdit we = context.convertToWorkspaceEdit(proposal);
                toResolve.setEdit(we);
            } catch (ProcessCanceledException e) {
                //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
                //TODO delete block when minimum required version is 2024.2
                throw e;
            } catch (IndexNotReadyException | CancellationException e) {
                throw e;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Unable to create workspace edit for code action.", e);
            }
        }

        return toResolve;
    }

    private void insertAndReplaceAnnotation(Diagnostic diagnostic, JavaCodeActionContext context,
                                            List<CodeAction> codeActions) {
        String name = Messages.getMessage("AddTheMissingAttributes");
        codeActions.add(JDTUtils.createCodeAction(context, diagnostic, name, getParticipantId()));

    }

    private static List<PsiAnnotation> getAnnotations(PsiElement e, String... names) {
        List<PsiAnnotation> result = new ArrayList<>();
        if (e instanceof PsiField) {
            PsiField field = ((PsiField) e);
            PsiAnnotation[] annotations = field.getAnnotations();
            for (String name : names) {
                List<PsiAnnotation> partial = Arrays.stream(annotations)
                        .filter(n -> n.getQualifiedName().equals(name))
                        .collect(Collectors.toList());
                result.addAll(partial);
            }
        }
        return result;
    }

    @Override
    public String getParticipantId() {
        return PersistenceAnnotationQuickFix.class.getName();
    }
}