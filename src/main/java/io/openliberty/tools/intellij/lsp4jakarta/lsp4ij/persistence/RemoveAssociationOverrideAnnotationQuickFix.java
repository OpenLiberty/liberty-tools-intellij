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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationConflictQuickFix;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.List;

/**
 * Removes {@code @AssociationOverride} or {@code @AssociationOverrides} from a
 * class that is not annotated with {@code @Entity}, {@code @MappedSuperclass},
 * or {@code @Embeddable}.
 *
 * <p>One code action is produced per annotation that is <em>actually present</em>
 * on the offending type — so the user sees "Remove @AssociationOverride" when only
 * the single form is present, or "Remove @AssociationOverrides" when only the
 * container form is present.
 *
 * @see PersistenceConstants#DIAGNOSTIC_CODE_ASSOCIATION_OVERRIDE_INVALID_TARGET
 */
public class RemoveAssociationOverrideAnnotationQuickFix extends RemoveAnnotationConflictQuickFix {

    public RemoveAssociationOverrideAnnotationQuickFix() {
        super(false, PersistenceConstants.ASSOCIATION_OVERRIDE, PersistenceConstants.ASSOCIATION_OVERRIDES);
    }

    /**
     * Overrides the default behaviour so that only a code action for an annotation
     * that is <em>actually present</em> on the covered type is produced.
     */
    @Override
    protected void removeAnnotations(Diagnostic diagnostic, JavaCodeActionContext context,
                                     List<CodeAction> codeActions) {
        PsiElement coveredNode = context.getCoveredNode();
        PsiClass declaringClass = PsiTreeUtil.getParentOfType(coveredNode, PsiClass.class, false);
        PsiModifierListOwner owner = declaringClass != null ? declaringClass
                : (PsiModifierListOwner) getBinding(coveredNode);
        if (owner != null) {
            for (String annotationFqn : getAnnotations()) {
                if (owner.hasAnnotation(annotationFqn)) {
                    removeAnnotation(diagnostic, context, codeActions, annotationFqn);
                }
            }
        } else {
            super.removeAnnotations(diagnostic, context, codeActions);
        }
    }

    @Override
    public String getParticipantId() {
        return RemoveAssociationOverrideAnnotationQuickFix.class.getName();
    }
}
