/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.beanvalidation;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.ModifyModifiersProposal;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.RemoveAnnotationsProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.WorkspaceEdit;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quickfix for fixing {@link BeanValidationConstants#DIAGNOSTIC_CODE_STATIC} error by either action
 * 1. Removing constraint annotation on static field or method
 * 2. Removing static modifier from field or method
 *
 * @author Leslie Dawson (lamminade)
 *
 */
public class BeanValidationQuickFix implements IJavaCodeActionParticipant {

    private static final Logger LOGGER = Logger.getLogger(BeanValidationQuickFix.class.getName());

    @Override
    public String getParticipantId() {
        return BeanValidationQuickFix.class.getName();
    }

    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        List<CodeAction> codeActions = new ArrayList<>();
        getRemoveConstraintAnnotationsCodeActions(diagnostic, context.copy(), codeActions);

        if (diagnostic.getCode().getLeft().equals(BeanValidationConstants.DIAGNOSTIC_CODE_STATIC)) {
            getRemoveStaticModifierCodeActions(diagnostic, context.copy(), codeActions);
        }
        return codeActions;
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {

        final CodeAction toResolve = context.getUnresolved();
        List<Diagnostic> diagnostics = toResolve.getDiagnostics();
        final String name = Messages.getMessage("RemoveStaticModifier");

        Diagnostic diagnostic = null;
        if (diagnostics.size() > 0) {
            diagnostic = diagnostics.get(0);
        }
        final String annotationName = diagnostic.getData().toString().replace("\"", "");
//        final String name1 = Messages.getMessage("RemoveConstraintAnnotation", annotationName);
//        if(name1) {
//            resolveRemoveConstraintAnnotationsCodeAction(context, diagnostic);

//        }
        String message = toResolve.getTitle();

        if (message == Messages.getMessage("RemoveStaticModifier")) {
            resolveStaticModifierCodeAction(context, diagnostic);
            return toResolve;
        }
//        if(message == Messages.getMessage("RemoveConstraintAnnotation", annotationName)) {
        resolveRemoveConstraintAnnotationsCodeAction(context, diagnostic);
//        }
        return toResolve;
    }

    private void getRemoveConstraintAnnotationsCodeActions(Diagnostic diagnostic, JavaCodeActionContext context, List<CodeAction> codeActions) {

        final String annotationName = diagnostic.getData().toString().replace("\"", "");
        final String name = Messages.getMessage("RemoveConstraintAnnotation", annotationName);
        codeActions.add(JDTUtils.createCodeAction(context, diagnostic, name, getParticipantId()));
    }

    private void resolveRemoveConstraintAnnotationsCodeAction(JavaCodeActionResolveContext context, Diagnostic diagnostic) {

        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        final PsiClass parentType = PsiTreeUtil.getParentOfType(node, PsiClass.class);
        final String annotationName = diagnostic.getData().toString().replace("\"", "");
        final String name = Messages.getMessage("RemoveConstraintAnnotation", annotationName);
        final PsiModifierListOwner modifierListOwner = PsiTreeUtil.getParentOfType(node, PsiModifierListOwner.class);
        final PsiAnnotation[] annotations = modifierListOwner.getAnnotations();
        Optional<PsiAnnotation> annotationToRemove = null;

        if (annotations != null && annotations.length > 0) {
            annotationToRemove =
                    Arrays.stream(annotations).filter(a -> annotationName.equals(a.getQualifiedName())).findFirst();
        }

        final RemoveAnnotationsProposal proposal = new RemoveAnnotationsProposal(name, context.getSource().getCompilationUnit(),
                context.getASTRoot(), parentType, 0, Collections.singletonList(annotationToRemove.get()));

        try {
            WorkspaceEdit we = context.convertToWorkspaceEdit(proposal);
            toResolve.setEdit(we);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to create workspace edit for code action to change return type to void", e);
        }
    }

    private void resolveStaticModifierCodeAction(JavaCodeActionResolveContext context, Diagnostic diagnostic) {
        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        final PsiClass parentType = PsiTreeUtil.getParentOfType(node, PsiClass.class);
        final PsiModifierListOwner modifierListOwner = PsiTreeUtil.getParentOfType(node, PsiModifierListOwner.class);

        final ModifyModifiersProposal proposal = new ModifyModifiersProposal(Messages.getMessage(
                "RemoveStaticModifier")
                , context.getSource().getCompilationUnit(),
                context.getASTRoot(), parentType, 0, modifierListOwner.getModifierList(), Collections.emptyList(),
                Collections.singletonList("static"));

        try {
            WorkspaceEdit we = context.convertToWorkspaceEdit(proposal);
            toResolve.setEdit(we);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to create workspace edit for code action to change return type to void", e);
        }
    }

    private void getRemoveStaticModifierCodeActions(Diagnostic diagnostic, JavaCodeActionContext context,
                                                    List<CodeAction> codeActions) {

        final String name = Messages.getMessage("RemoveStaticModifier");
        codeActions.add(JDTUtils.createCodeAction(context, diagnostic, name, getParticipantId()));
    }
}