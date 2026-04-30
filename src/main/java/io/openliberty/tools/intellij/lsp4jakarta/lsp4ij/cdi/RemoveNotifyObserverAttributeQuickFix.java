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

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationAttributesQuickFix;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.OBSERVES_ASYNC_FQ_NAME;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.OBSERVES_FQ_NAME;

/**
 * Removes the 'notifyObserver' attribute from @Observes and @ObservesAsync annotations.
 */
public class RemoveNotifyObserverAttributeQuickFix extends RemoveAnnotationAttributesQuickFix {

    public RemoveNotifyObserverAttributeQuickFix() {
        super(new String[] { OBSERVES_FQ_NAME, OBSERVES_ASYNC_FQ_NAME }, "notifyObserver");
    }

    @Override
    public String getParticipantId() {
        return RemoveNotifyObserverAttributeQuickFix.class.getName();
    }

    @Override
    protected String getLabel(String annotation, String[] attributes) {
        // If annotation is null, use a default name (shouldn't happen in practice)
        String annotationName = annotation != null ? JDTUtils.getSimpleName(annotation) : "Observes";
        return Messages.getMessage("RemoveNotifyObserverAttribute", annotationName);
    }
}

// Made with Bob
