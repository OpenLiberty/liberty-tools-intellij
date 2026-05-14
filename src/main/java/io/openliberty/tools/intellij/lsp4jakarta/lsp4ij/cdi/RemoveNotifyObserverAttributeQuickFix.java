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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationAttributesQuickFix;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;

import java.util.Arrays;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.OBSERVES_ASYNC_FQ_NAME;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.OBSERVES_FQ_NAME;

/**
 * Removes the 'notifyObserver' attribute from @Observes and @ObservesAsync annotations
 * when they are conditional observers (notifyObserver=IF_EXISTS).
 * Works on method parameters with these annotations.
 */
public class RemoveNotifyObserverAttributeQuickFix extends RemoveAnnotationAttributesQuickFix {

    public RemoveNotifyObserverAttributeQuickFix() {
        super(new String[] { OBSERVES_FQ_NAME, OBSERVES_ASYNC_FQ_NAME }, "notifyObserver");
    }

    @Override
    protected AnnotationInfo findAnnotationInfo(PsiElement node) {
        // Search method parameters for @Observes/@ObservesAsync annotations
        PsiMethod method = PsiTreeUtil.getParentOfType(node, PsiMethod.class);
        if (method != null) {
            for (PsiParameter param : method.getParameterList().getParameters()) {
                for (PsiAnnotation annotation : param.getAnnotations()) {
                    if (isTargetAnnotation(annotation)) {
                        return new AnnotationInfo(annotation, param);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Checks if the annotation is @Observes or @ObservesAsync with notifyObserver=IF_EXISTS.
     */
    private boolean isTargetAnnotation(PsiAnnotation annotation) {
        String qualifiedName = annotation.getQualifiedName();
        if (qualifiedName == null || !Arrays.asList(getAnnotations()).contains(qualifiedName)) {
            return false;
        }
        
        // Check if the annotation has the notifyObserver attribute with IF_EXISTS value (conditional observer)
        for (String attribute : getAttributes()) {
            String value = AnnotationUtils.getAnnotationMemberValue(annotation, attribute);
            if (value != null && value.endsWith("IF_EXISTS")) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected String getLabel(String annotation, String[] attributes) {
        return Messages.getMessage("RemoveNotifyObserverAttribute", JDTUtils.getSimpleName(annotation));
    }

    @Override
    public String getParticipantId() {
        return RemoveNotifyObserverAttributeQuickFix.class.getName();
    }
}

// Made with Bob
