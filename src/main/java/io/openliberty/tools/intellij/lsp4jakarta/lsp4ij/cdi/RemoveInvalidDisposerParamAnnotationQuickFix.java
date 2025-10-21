/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveParamAnnotationQuickFix;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.INVALID_DISPOSER_FQ_PARAMS;

/**
 *
 * Removes the @Disposes annotations from the declaring element.
 *
 */
public class RemoveInvalidDisposerParamAnnotationQuickFix extends RemoveParamAnnotationQuickFix {

    public RemoveInvalidDisposerParamAnnotationQuickFix() {
        super(INVALID_DISPOSER_FQ_PARAMS);
    }

    @Override
    public String getParticipantId() {
        return RemoveInvalidDisposerParamAnnotationQuickFix.class.getName();
    }
}
