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

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb;

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationConflictQuickFix;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb.EjbConstants.STATELESS_FQ_NAME;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb.EjbConstants.STATEFUL_FQ_NAME;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb.EjbConstants.SINGLETON_FQ_NAME;

/**
 * Quick fix for removing session bean annotations (@Stateless, @Stateful, @Singleton)
 * when they conflict with @Interceptor or @Decorator.
 */
public class RemoveSessionBeanAnnotationQuickFix extends RemoveAnnotationConflictQuickFix {

    public RemoveSessionBeanAnnotationQuickFix() {
        super(false, STATELESS_FQ_NAME, STATEFUL_FQ_NAME, SINGLETON_FQ_NAME);
    }

    @Override
    public String getParticipantId() {
        return RemoveSessionBeanAnnotationQuickFix.class.getName();
    }
}

// Made with Bob
