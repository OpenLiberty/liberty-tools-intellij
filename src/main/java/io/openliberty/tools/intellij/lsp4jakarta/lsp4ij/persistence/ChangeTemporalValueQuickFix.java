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
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.ModifyAnnotationAttributeValueQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.util.AnnotationValueExpressionUtil;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import org.eclipse.lsp4j.Diagnostic;

import java.util.HashMap;
import java.util.Map;

/**
 * Quick fix for changing the value of @Temporal annotation to TemporalType.DATE
 * for fields/properties annotated with @Id and of type java.util.Date.
 * 
 * Addresses diagnostic: {@link PersistenceConstants#DIAGNOSTIC_CODE_TEMPORAL_INVALID_VALUE}
 */
public class ChangeTemporalValueQuickFix extends ModifyAnnotationAttributeValueQuickFix {

    @Override
    protected String getAnnotationName() {
        return PersistenceConstants.TEMPORAL;
    }

    @Override
    protected Map<String, PsiAnnotationMemberValue> getNewAttributeValues(JavaCodeActionResolveContext context,
                                                                           PsiAnnotation annotation) {
        Map<String, PsiAnnotationMemberValue> attributes = new HashMap<>();
        
        // Create the TemporalType.DATE enum value
        PsiAnnotationMemberValue dateValue = AnnotationValueExpressionUtil.createEnumValueExpression(
                annotation,
                "jakarta.persistence.TemporalType",
                "DATE"
        );
        attributes.put("value", dateValue);
        
        return attributes;
    }

    @Override
    protected String getLabel(JavaCodeActionContext context, Diagnostic diagnostic) {
        return Messages.getMessage("ChangeTemporalValueToDate");
    }

    @Override
    public String getParticipantId() {
        return ChangeTemporalValueQuickFix.class.getName();
    }
}

