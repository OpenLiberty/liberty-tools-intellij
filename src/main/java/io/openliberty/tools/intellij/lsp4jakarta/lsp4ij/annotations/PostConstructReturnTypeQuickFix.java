/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Yijia Jing
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.annotations;

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.ChangeReturnTypeToVoidQuickFix;

/**
 * Quick fix for AnnotationDiagnosticsCollector that changes the return type of a method to void.
 *
 * @author Yijia Jing
 */
public class PostConstructReturnTypeQuickFix extends ChangeReturnTypeToVoidQuickFix {

    @Override
    public String getParticipantId() {
        return PostConstructReturnTypeQuickFix.class.getName();
    }
}
