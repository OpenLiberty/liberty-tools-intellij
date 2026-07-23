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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationConflictQuickFix;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.List;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb.EjbConstants.INTERCEPTOR_FQ_NAME;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb.EjbConstants.DECORATOR_FQ_NAME;

/**
 * Quick fix for removing @Interceptor or @Decorator annotations from session beans.
 */
public class RemoveInterceptorOrDecoratorQuickFix extends RemoveAnnotationConflictQuickFix {

    public RemoveInterceptorOrDecoratorQuickFix() {
        super(false, INTERCEPTOR_FQ_NAME, DECORATOR_FQ_NAME);
    }

    @Override
    public String getParticipantId() {
        return RemoveInterceptorOrDecoratorQuickFix.class.getName();
    }

    @Override
    protected void removeAnnotations(Diagnostic diagnostic, JavaCodeActionContext context,
                                     List<CodeAction> codeActions) {
        // Only generate code actions for @Interceptor or @Decorator annotations that are actually present on the class
        PsiElement node = context.getCoveredNode();
        PsiClass parentType = PsiTreeUtil.getParentOfType(node, PsiClass.class);
        
        if (parentType != null) {
            for (String annotation : getAnnotations()) {
                // Check if this annotation is present on the class
                if (AnnotationUtils.hasAnnotation(parentType, annotation)) {
                    removeAnnotation(diagnostic, context, codeActions, annotation);
                }
            }
        }
    }
}
