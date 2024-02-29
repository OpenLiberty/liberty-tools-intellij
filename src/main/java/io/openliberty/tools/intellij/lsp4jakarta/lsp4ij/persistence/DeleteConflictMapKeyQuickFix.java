/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence;

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationConflictQuickFix;

/**
 *
 * Quick fix for removing @MapKey/@MapKeyClass when they are used for the same field
 * or property
 *
 * @author Jianing Xu
 *
 */
public class DeleteConflictMapKeyQuickFix extends RemoveAnnotationConflictQuickFix {

    public DeleteConflictMapKeyQuickFix() {
        super(false, "jakarta.persistence.MapKeyClass", "jakarta.persistence.MapKey");
    }

    @Override
    public String getParticipantId() {
        return DeleteConflictMapKeyQuickFix.class.getName();
    }
}
