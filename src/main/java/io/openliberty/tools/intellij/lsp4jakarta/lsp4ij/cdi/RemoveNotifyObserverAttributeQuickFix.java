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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Removes the 'notifyObserver' attribute from @Observes and @ObservesAsync annotations
 * when it has the value Reception.IF_EXISTS (conditional observer on @Dependent scoped beans).
 * Works on method parameters with these annotations.
 */
public class RemoveNotifyObserverAttributeQuickFix extends RemoveAnnotationAttributesQuickFix {

    public RemoveNotifyObserverAttributeQuickFix() {
        super(createAnnotationAttributesMap());
    }

    private static Map<String, List<String>> createAnnotationAttributesMap() {
        Map<String, List<String>> map = new HashMap<>();
        map.put(ManagedBeanConstants.OBSERVES_FQ_NAME, List.of("notifyObserver"));
        map.put(ManagedBeanConstants.OBSERVES_ASYNC_FQ_NAME, List.of("notifyObserver"));
        return map;
    }

    @Override
    protected AnnotationInfo findAnnotationInfo(PsiElement node) {
        // Search method parameters for @Observes/@ObservesAsync annotations
        PsiMethod method = PsiTreeUtil.getParentOfType(node, PsiMethod.class);
        if (method != null) {
            for (PsiParameter param : method.getParameterList().getParameters()) {
                for (PsiAnnotation annotation : param.getAnnotations()) {
                    String qualifiedName = annotation.getQualifiedName();
                    if (qualifiedName != null && getAnnotationAttributesMap().containsKey(qualifiedName)) {
                        return new AnnotationInfo(annotation, param);
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected String getLabel(String annotation, List<String> attributes) {
        return Messages.getMessage("RemoveNotifyObserverAttribute", JDTUtils.getSimpleName(annotation));
    }

    @Override
    public String getParticipantId() {
        return RemoveNotifyObserverAttributeQuickFix.class.getName();
    }
}
