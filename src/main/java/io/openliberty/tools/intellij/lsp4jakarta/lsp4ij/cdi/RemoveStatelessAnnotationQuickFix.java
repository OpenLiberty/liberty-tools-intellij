/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi;

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationConflictQuickFix;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.STATELESS_FQ_NAME;

/**
 *
 * Quick fix for removing @Stateless annotation
 *
 */
public class RemoveStatelessAnnotationQuickFix extends RemoveAnnotationConflictQuickFix {

    public RemoveStatelessAnnotationQuickFix() {
        super(false, STATELESS_FQ_NAME);
    }

    @Override
    public String getParticipantId() {
        return RemoveStatelessAnnotationQuickFix.class.getName();
    }
}
