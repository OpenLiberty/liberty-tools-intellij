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

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveParamAnnotationQuickFix;

/**
 * Quick fix that removes the {@code @Disposes} annotation from a disposer method
 * parameter whose type has no matching {@code @Produces} producer in the same class
 * (orphan disposer).
 */
public class RemoveOrphanDisposesAnnotationQuickFix extends RemoveParamAnnotationQuickFix {

    public RemoveOrphanDisposesAnnotationQuickFix() {
        super(ManagedBeanConstants.DISPOSES_FQ_NAME);
    }

    @Override
    public String getParticipantId() {
        return RemoveOrphanDisposesAnnotationQuickFix.class.getName();
    }
}
