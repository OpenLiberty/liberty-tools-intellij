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

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationConflictQuickFix;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Quick fix for removing conflicting {@code @EmbeddedId} or {@code @Id} annotations
 * from the declaring element.
 *
 * <p>Only offers removal of annotations that are actually present on the covered node,
 * preventing spurious "Remove @X" actions for annotations that do not exist on the member.
 *
 * <p>Applicable to:
 * <ul>
 *   <li>Multiple {@code @EmbeddedId} declarations on the same entity
 *       ({@code MultipleEmbeddedIdAnnotations})</li>
 *   <li>Mixed {@code @Id} and {@code @EmbeddedId} usage on the same entity
 *       ({@code MixedIdentifierAnnotations})</li>
 * </ul>
 *
 * @see PersistenceConstants#DIAGNOSTIC_CODE_MULTIPLE_EMBEDDED_ID
 * @see PersistenceConstants#DIAGNOSTIC_CODE_MIXED_IDENTIFIER
 */
public class RemoveAnnotationConflictForIdentifiersQuickFix extends RemoveAnnotationConflictQuickFix {

    public RemoveAnnotationConflictForIdentifiersQuickFix() {
        super(false, PersistenceConstants.EMBEDDEDID, PersistenceConstants.ID);
    }

    /**
     * Overrides the base implementation to filter offered code actions down to only
     * the annotations actually present on the member covered by the diagnostic.
     *
     * <p>Without this, a diagnostic on an {@code @Id} field would incorrectly also
     * offer "Remove @EmbeddedId", even though {@code @EmbeddedId} is on a different
     * field entirely.
     */
    @Override
    protected void removeAnnotations(Diagnostic diagnostic, JavaCodeActionContext context,
                                     List<CodeAction> codeActions) {
        PsiElement coveredNode = context.getCoveredNode();
        PsiModifierListOwner declaringMember =
                PsiTreeUtil.getParentOfType(coveredNode, PsiModifierListOwner.class, false);

        if (declaringMember != null) {
            // Collect the simple names of annotations present on this specific member
            Set<String> presentSimpleNames = Arrays.stream(declaringMember.getAnnotations())
                    .map(PsiAnnotation::getQualifiedName)
                    .filter(fqn -> fqn != null)
                    .map(fqn -> fqn.substring(fqn.lastIndexOf('.') + 1))
                    .collect(Collectors.toSet());

            // Only offer removal for annotations that are actually on this member
            for (String candidateFqn : getAnnotations()) {
                String simpleName = candidateFqn.substring(candidateFqn.lastIndexOf('.') + 1);
                if (presentSimpleNames.contains(simpleName)) {
                    removeAnnotation(diagnostic, context, codeActions, candidateFqn);
                }
            }
        } else {
            // Fallback: offer all candidate annotations
            super.removeAnnotations(diagnostic, context, codeActions);
        }
    }

    @Override
    public String getParticipantId() {
        return RemoveAnnotationConflictForIdentifiersQuickFix.class.getName();
    }
}
