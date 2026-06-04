/*******************************************************************************
 * Copyright (c) 2021, 2026 IBM Corporation and others.
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

/**
 * Quick fix for removing @Named annotation from producer fields
 * 
 * Producer fields must not declare a bean name using @Named annotation.
 *
 */
public class RemoveNamedAnnotationQuickFix extends RemoveAnnotationConflictQuickFix {

    public RemoveNamedAnnotationQuickFix() {
        super(false, "jakarta.inject.Named");
    }

    @Override
    public String getParticipantId() {
        return RemoveNamedAnnotationQuickFix.class.getName();
    }
}
