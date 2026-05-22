/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveParamAnnotationQuickFix;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.OBSERVES_ASYNC_FQ_NAME;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.OBSERVES_FQ_NAME;

/**
 * Quick fix for removing @Observes or @ObservesAsync annotation from method parameters
 */
public class RemoveObserverAnnotationQuickFix extends RemoveParamAnnotationQuickFix {

    public RemoveObserverAnnotationQuickFix() {
        super(OBSERVES_FQ_NAME, OBSERVES_ASYNC_FQ_NAME);
    }

    @Override
    public String getParticipantId() {
        return RemoveObserverAnnotationQuickFix.class.getName();
    }
}
