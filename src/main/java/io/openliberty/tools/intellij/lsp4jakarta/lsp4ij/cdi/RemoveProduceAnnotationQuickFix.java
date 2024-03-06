/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation, Himanshu Chotwani - initial API and implementation
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi;

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationConflictQuickFix;

/**
 *
 * Quick fix for removing @Produces annotation
 *
 */
public class RemoveProduceAnnotationQuickFix extends RemoveAnnotationConflictQuickFix {

    public RemoveProduceAnnotationQuickFix() {
        super(false, "jakarta.enterprise.inject.Produces");
    }

    @Override
    public String getParticipantId() {
        return RemoveProduceAnnotationQuickFix.class.getName();
    }
}
