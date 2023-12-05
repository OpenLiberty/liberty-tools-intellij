/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     IBM Corporation, Jianing Xu - initial API and implementation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi;

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationConflictQuickFix;

public class ConflictProducesInjectQuickFix extends RemoveAnnotationConflictQuickFix {

    public ConflictProducesInjectQuickFix() {
        super(false, "jakarta.enterprise.inject.Produces", "jakarta.inject.Inject");
    }

    @Override
    public String getParticipantId() {
        return ConflictProducesInjectQuickFix.class.getName();
    }
}
