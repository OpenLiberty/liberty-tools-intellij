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

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationConflictQuickFix;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.List;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb.EjbConstants.STATELESS_FQ_NAME;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb.EjbConstants.STATEFUL_FQ_NAME;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb.EjbConstants.SINGLETON_FQ_NAME;

/**
 * Quick fix for removing session bean annotations (@Stateless, @Stateful, @Singleton)
 * when they conflict with @Interceptor or @Decorator.
 */
public class RemoveSessionBeanAnnotationQuickFix extends RemoveAnnotationConflictQuickFix {

    public RemoveSessionBeanAnnotationQuickFix() {
        super(false, STATELESS_FQ_NAME, STATEFUL_FQ_NAME, SINGLETON_FQ_NAME);
    }

    @Override
    public String getParticipantId() {
        return RemoveSessionBeanAnnotationQuickFix.class.getName();
    }

    @Override
    protected void removeAnnotations(Diagnostic diagnostic, JavaCodeActionContext context,
                                     List<CodeAction> codeActions) {
        // Only generate code actions for session bean annotations that are actually present on the class
        PsiElement node = context.getCoveredNode();
        PsiClass parentType = PsiTreeUtil.getParentOfType(node, PsiClass.class);
        
        if (parentType != null) {
            for (String annotation : getAnnotations()) {
                // Check if this annotation is present on the class
                if (hasAnnotation(parentType, annotation)) {
                    removeAnnotation(diagnostic, context, codeActions, annotation);
                }
            }
        }
    }

    /**
     * Check if the class has the specified annotation.
     */
    private boolean hasAnnotation(PsiClass psiClass, String annotationFQN) {
        for (PsiAnnotation annotation : psiClass.getAnnotations()) {
            if (annotationFQN.equals(annotation.getQualifiedName())) {
                return true;
            }
        }
        return false;
    }
}
