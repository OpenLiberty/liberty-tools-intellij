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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence;

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationConflictQuickFix;

/**
 * Quick fix for removing @MapKeyTemporal annotation when it is applied to a field
 * or property where the map key type is not java.util.Date or java.util.Calendar.
 */
public class RemoveMapKeyTemporalAnnotationQuickFix extends RemoveAnnotationConflictQuickFix {

    public RemoveMapKeyTemporalAnnotationQuickFix() {
        super(false, PersistenceConstants.MAPKEYTEMPORAL);
    }

    @Override
    public String getParticipantId() {
        return RemoveMapKeyTemporalAnnotationQuickFix.class.getName();
    }
}

