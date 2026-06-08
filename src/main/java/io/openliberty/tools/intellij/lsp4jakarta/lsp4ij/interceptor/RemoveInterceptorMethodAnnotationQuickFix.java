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
 *     IBM Corporation, Archana Iyer - initial API and implementation
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.interceptor;

import com.intellij.openapi.project.Project;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveMultipleAnnotations;
import java.util.ArrayList;
import java.util.List;

/**
 * Quick fix for removing Interceptror method annotations when more than
 * one occur in a class.
 * The getCodeActions method is overridden in order to make sure that
 * we return our custom quick fixes. There will be two quick fixes given
 * to the user: (1) remove @AroundInvoke (2) remove @AroundTimeout (3) remove @AroundConstruct (4) remove @PreDestroy (5) remove @PostConstruct
 *
 * @author Archana Iyer
 *
 */
public class RemoveInterceptorMethodAnnotationQuickFix extends RemoveMultipleAnnotations {
    @Override
    protected List<List<String>> getMultipleRemoveAnnotations(Project project, List<String> annotations) {
        List<List<String>> annotationsListsToRemove = new ArrayList<List<String>>();
        if (annotations.size() > 0 && annotations.stream().anyMatch(Constants.INTERCEPTOR_METHODS::contains)) {
            annotationsListsToRemove.add(annotations);
        }
        return annotationsListsToRemove;
    }

    @Override
    public String getParticipantId() {
        return RemoveInterceptorMethodAnnotationQuickFix.class.getName();
    }
}
