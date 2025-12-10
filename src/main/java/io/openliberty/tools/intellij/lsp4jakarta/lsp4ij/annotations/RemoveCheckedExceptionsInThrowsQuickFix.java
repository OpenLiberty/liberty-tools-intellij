/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.annotations;

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveExceptionsInThrowsQuickFix;

/**
 * Quickfix for removing checked exceptions from throws clause
 *
 */
public class RemoveCheckedExceptionsInThrowsQuickFix extends RemoveExceptionsInThrowsQuickFix {

    public RemoveCheckedExceptionsInThrowsQuickFix() {
        super("RemoveCheckedExceptions");
    }

    @Override
    public String getParticipantId() {
        return RemoveCheckedExceptionsInThrowsQuickFix.class.getName();
    }
}
