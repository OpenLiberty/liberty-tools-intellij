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
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiModifierListOwner;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.InsertAnnotationWithAttributesQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.util.PsiUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.util.AnnotationValueExpressionUtil;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import org.eclipse.lsp4j.Diagnostic;

import java.util.HashMap;
import java.util.Map;

/**
 * Quick fix for inserting @Temporal annotation with TemporalType.DATE value
 * for fields/properties annotated with @Id and of type java.util.Date.
 * 
 * Addresses diagnostic: {@link PersistenceConstants#DIAGNOSTIC_CODE_MISSING_TEMPORAL}
 */
public class InsertTemporalAnnotationQuickFix extends InsertAnnotationWithAttributesQuickFix {

    @Override
    protected String getAnnotation() {
        return PersistenceConstants.TEMPORAL;
    }

    @Override
    protected Map<String, PsiAnnotationMemberValue> getAttributeValues(JavaCodeActionResolveContext context) {
        Map<String, PsiAnnotationMemberValue> attributes = new HashMap<>();
        
        // Get the binding (field or method) where the annotation will be added
        PsiModifierListOwner binding = PsiUtils.getBinding(context.getCoveringNode());
        
        if (binding != null && binding.getModifierList() != null) {
            // Create a temporary annotation to use for creating the value expression
            PsiAnnotation tempAnnotation = binding.getModifierList().addAnnotation(PersistenceConstants.TEMPORAL);
            
            // Create the TemporalType.DATE enum value
            PsiAnnotationMemberValue dateValue = AnnotationValueExpressionUtil.createEnumValueExpression(
                    tempAnnotation,
                    "jakarta.persistence.TemporalType",
                    "DATE"
            );
            attributes.put("value", dateValue);
            
            // Remove the temporary annotation (it will be re-created properly by the proposal)
            tempAnnotation.delete();
        }
        
        return attributes;
    }

    @Override
    protected String getLabel(JavaCodeActionContext context, Diagnostic diagnostic) {
        return Messages.getMessage("InsertItem", "@Temporal(TemporalType.DATE)");
    }

    @Override
    public String getParticipantId() {
        return InsertTemporalAnnotationQuickFix.class.getName();
    }
}

