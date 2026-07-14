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
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationConflictQuickFix;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.List;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb.EjbConstants.*;

/**
 * Removes the session bean annotation (@Stateless, @Stateful, or @Singleton)
 * that is actually present on a class that violates Jakarta Enterprise Beans
 * 4.0 spec section 4.1 class constraints (not public, final, abstract, or not
 * top-level).
 *
 * Only one of the three session bean annotations can be present on a class
 * at a time; this quick fix emits exactly one code action for whichever one
 * is found.
 */
public class RemoveSessionBeanAnnotationQuickFix extends RemoveAnnotationConflictQuickFix {

    public RemoveSessionBeanAnnotationQuickFix() {
        super(false, STATELESS_FQ_NAME, STATEFUL_FQ_NAME, SINGLETON_FQ_NAME);
    }

    @Override
    public String getParticipantId() {
        return RemoveSessionBeanAnnotationQuickFix.class.getName();
    }

    /**
     * Overrides the default to only emit a code action for the session bean
     * annotation that is actually present on the class.
     */
    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        List<CodeAction> codeActions = new ArrayList<>();
        PsiClass parentClass = PsiTreeUtil.getParentOfType(context.getCoveredNode(), PsiClass.class);
        if (parentClass == null) {
            return codeActions;
        }
        for (PsiAnnotation annotation : parentClass.getAnnotations()) {
            String fqn = annotation.getQualifiedName();
            if (STATELESS_FQ_NAME.equals(fqn) || STATEFUL_FQ_NAME.equals(fqn) || SINGLETON_FQ_NAME.equals(fqn)) {
                removeAnnotation(diagnostic, context, codeActions, fqn);
            }
        }
        return codeActions;
    }
}
