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

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationConflictQuickFix;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.List;

/**
 * Removes {@code @AttributeOverride} or {@code @AttributeOverrides} from a
 * field or property that is not annotated with {@code @Embedded},
 * {@code @EmbeddedId}, or {@code @ElementCollection}.
 *
 * <p>One code action is produced per annotation that is <em>actually present</em>
 * on the offending member — so the user sees "Remove @AttributeOverride" when only
 * the single form is present, or "Remove @AttributeOverrides" when only the
 * container form is present.
 */
public class RemoveAttributeOverrideAnnotationQuickFix extends RemoveAnnotationConflictQuickFix {

    public RemoveAttributeOverrideAnnotationQuickFix() {
        super(false, PersistenceConstants.ATTRIBUTE_OVERRIDE, PersistenceConstants.ATTRIBUTE_OVERRIDES);
    }

    /**
     * Overrides the default behaviour so that only a code action for an annotation
     * that is <em>actually present</em> on the covered member is produced. The base
     * class would otherwise generate one action for every registered annotation
     * regardless of whether it appears on the element.
     */
    @Override
    protected void removeAnnotations(Diagnostic diagnostic, JavaCodeActionContext context,
                                     List<CodeAction> codeActions) {
        PsiElement binding = getBinding(context.getCoveredNode());
        if (!(binding instanceof PsiModifierListOwner)) {
            super.removeAnnotations(diagnostic, context, codeActions);
            return;
        }
        PsiModifierListOwner owner = (PsiModifierListOwner) binding;
        for (String annotationFqn : getAnnotations()) {
            if (owner.hasAnnotation(annotationFqn)) {
                removeAnnotation(diagnostic, context, codeActions, annotationFqn);
            }
        }
    }

    @Override
    public String getParticipantId() {
        return RemoveAttributeOverrideAnnotationQuickFix.class.getName();
    }
}
