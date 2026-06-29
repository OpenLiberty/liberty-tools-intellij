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

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb.EjbConstants.INTERCEPTOR_FQ_NAME;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb.EjbConstants.DECORATOR_FQ_NAME;

/**
 * Quick fix for removing @Interceptor or @Decorator annotations from session beans.
 */
public class RemoveInterceptorOrDecoratorQuickFix extends RemoveAnnotationConflictQuickFix {

    public RemoveInterceptorOrDecoratorQuickFix() {
        super(false, INTERCEPTOR_FQ_NAME, DECORATOR_FQ_NAME);
    }

    @Override
    public String getParticipantId() {
        return RemoveInterceptorOrDecoratorQuickFix.class.getName();
    }
}

// Made with Bob
