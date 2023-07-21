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
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.ModifyModifiersProposal;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.RemoveAnnotationsProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.*;

/**
 * Quickfix for fixing {@link BeanValidationConstants#DIAGNOSTIC_CODE_STATIC} error by either action
 * 1. Removing constraint annotation on static field or method
 * 2. Removing static modifier from field or method
 * 
 * @author Leslie Dawson (lamminade)
 *
 */
public class BeanValidationQuickFix {

    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        List<CodeAction> codeActions = new ArrayList<>();
        removeConstraintAnnotations(diagnostic, context.copy(), codeActions);

        if (diagnostic.getCode().getLeft().equals(BeanValidationConstants.DIAGNOSTIC_CODE_STATIC)) {
            removeStaticModifier(diagnostic, context.copy(), codeActions);
        }
        return codeActions;
    }

    private void removeConstraintAnnotations(Diagnostic diagnostic, JavaCodeActionContext context, List<CodeAction> codeActions) {
        final PsiElement node = context.getCoveredNode();
        final PsiClass parentType = PsiTreeUtil.getParentOfType(node, PsiClass.class);
        final PsiModifierListOwner modifierListOwner = PsiTreeUtil.getParentOfType(node, PsiModifierListOwner.class);

        final String annotationName = diagnostic.getData().toString().replace("\"", "");
        final PsiAnnotation[] annotations = modifierListOwner.getAnnotations();
        if (annotations != null && annotations.length > 0) {
            final Optional<PsiAnnotation> annotationToRemove =
                    Arrays.stream(annotations).filter(a -> annotationName.equals(a.getQualifiedName())).findFirst();
            if (annotationToRemove.isPresent()) {
                final String name = Messages.getMessage("RemoveConstraintAnnotation", annotationName);
                final RemoveAnnotationsProposal proposal = new RemoveAnnotationsProposal(name, context.getSource().getCompilationUnit(),
                        context.getASTRoot(), parentType, 0, Collections.singletonList(annotationToRemove.get()));
                final CodeAction codeAction = context.convertToCodeAction(proposal, diagnostic);
                if (codeAction != null) {
                    codeActions.add(codeAction);
                }
            }
        }
    }

    private void removeStaticModifier(Diagnostic diagnostic, JavaCodeActionContext context, List<CodeAction> codeActions) {
        final PsiElement node = context.getCoveredNode();
        final PsiClass parentType = PsiTreeUtil.getParentOfType(node, PsiClass.class);
        final PsiModifierListOwner modifierListOwner = PsiTreeUtil.getParentOfType(node, PsiModifierListOwner.class);

        final String name = Messages.getMessage("RemoveStaticModifier");
        final ModifyModifiersProposal proposal = new ModifyModifiersProposal(name, context.getSource().getCompilationUnit(),
                context.getASTRoot(), parentType, 0, modifierListOwner.getModifierList(), Collections.emptyList(),
                Collections.singletonList("static"));
        final CodeAction codeAction = context.convertToCodeAction(proposal, diagnostic);
        if (codeAction != null) {
            codeActions.add(codeAction);
        }
    }
}