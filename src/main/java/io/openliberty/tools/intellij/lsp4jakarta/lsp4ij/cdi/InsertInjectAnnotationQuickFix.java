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

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.InsertAnnotationMissingQuickFix;

/**
 * QuickFix for inserting @Inject annotation.
 */
public class InsertInjectAnnotationQuickFix extends InsertAnnotationMissingQuickFix {
    
    public InsertInjectAnnotationQuickFix() {
        super(ManagedBeanConstants.INJECT_FQ_NAME);
    }

    @Override
    public String getParticipantId() {
        return InsertInjectAnnotationQuickFix.class.getName();
    }
}
