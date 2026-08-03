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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi;

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationConflictQuickFix;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.INJECT_FQ_NAME;

/**
 * Removes the {@code @Inject} annotation from an injection point that uses a raw
 * {@code Event} type (i.e. {@code Event} without a type parameter).
 *
 * <p>This quickfix responds to the
 * {@link ManagedBeanConstants#DIAGNOSTIC_CODE_RAW_EVENT} diagnostic produced by
 * {@link CdiRawEventTypeDiagnosticsCollector}.
 */
public class RemoveInjectAnnotationFromRawEventQuickFix extends RemoveAnnotationConflictQuickFix {

    public RemoveInjectAnnotationFromRawEventQuickFix() {
        super(false, INJECT_FQ_NAME);
    }

    @Override
    public String getParticipantId() {
        return RemoveInjectAnnotationFromRawEventQuickFix.class.getName();
    }
}
