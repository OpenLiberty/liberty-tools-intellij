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
*     IBM Corporation - initial implementation
*******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.annotations;


import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationAttributesQuickFix;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Removes the @Resource annotation attribute.
 * Works on @Resource annotations at field/variable/method/class level.
 */
public class RemoveResourceAnnotationAttributeQuickFix extends RemoveAnnotationAttributesQuickFix {

    public RemoveResourceAnnotationAttributeQuickFix() {
        super(Collections.singletonMap(AnnotationConstants.RESOURCE_FQ_NAME, List.of("type")));
    }

    @Override
    protected AnnotationInfo findAnnotationInfo(PsiElement node) {
        // Find the @Resource annotation directly (it's at the annotation level)
        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(node, PsiAnnotation.class);
            PsiModifierListOwner binding = getBinding(node);
            if (binding != null) {
                return new AnnotationInfo(annotation, binding);
            }
        return null;
    }

    /**
     * Finds the nearest PsiModifierListOwner parent (field, method, or class).
     */
    private static PsiModifierListOwner getBinding(PsiElement node) {
        return Stream.<Class<? extends PsiModifierListOwner>>of(
                PsiVariable.class,
                PsiMethod.class,
                PsiClass.class
        )
                .map(clazz -> PsiTreeUtil.getParentOfType(node, clazz))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    protected String getLabel(String annotation, List<String> attributes) {
        return Messages.getMessage("RemoveRedundantAttribute", "type", "@Resource");
    }

    @Override
    public String getParticipantId() {
        return RemoveResourceAnnotationAttributeQuickFix.class.getName();
    }
}
