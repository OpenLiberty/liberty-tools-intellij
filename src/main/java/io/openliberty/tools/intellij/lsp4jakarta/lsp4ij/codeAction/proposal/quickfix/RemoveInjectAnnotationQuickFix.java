/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation, Himanshu Chotwani - initial API and implementation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix;

/**
 *
 * Quick fix for removing @Inject when it is used for final field
 *
 * @author Himanshu Chotwani
 *
 */

public class RemoveInjectAnnotationQuickFix extends RemoveAnnotationConflictQuickFix {

    public RemoveInjectAnnotationQuickFix() {
        super(false, "jakarta.inject.Inject");
    }

    @Override
    public String getParticipantId() {
        return RemoveInjectAnnotationQuickFix.class.getName();
    }
}
