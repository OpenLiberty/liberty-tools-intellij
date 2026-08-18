/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ASTRewriteCorrectionProposal;
import org.eclipse.lsp4j.CodeActionKind;

public class PrefixSlashAnnotationProposal extends ASTRewriteCorrectionProposal {
    private final PsiAnnotation fAnnotation;

    public PrefixSlashAnnotationProposal(String name, int relevance, PsiFile sourceCU, PsiElement declaringNode, PsiAnnotation annotationNode) {
        super(name, CodeActionKind.QuickFix, declaringNode, relevance, sourceCU);
        this.fAnnotation = annotationNode;
    }

    @Override
    public void performUpdate() {
        final String FORWARD_SLASH = "/";
        final String ESCAPE_QUOTE = "\"";
        if (fAnnotation != null) {
            PsiElementFactory factory = JavaPsiFacade.getElementFactory(fAnnotation.getProject());
            for (PsiNameValuePair pair : fAnnotation.getParameterList().getAttributes()) {
                if (pair.getValue() instanceof PsiLiteralExpression literal && literal.getValue() instanceof String valueText) {
                    if (!valueText.startsWith(FORWARD_SLASH)) {
                        fAnnotation.setDeclaredAttributeValue(PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME, factory.createExpressionFromText(
                                String.format("%s%s%s%s", ESCAPE_QUOTE, FORWARD_SLASH, valueText, ESCAPE_QUOTE),
                                fAnnotation
                        ));
                    }
                }
            }
        }
    }
}
