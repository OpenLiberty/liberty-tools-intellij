 /*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveParamAnnotationQuickFix;

import java.util.Arrays;

 /**
 * QuickFix for deleting any of @Disposes, @Observes and @ObservesAsync annotation for parameters
 */
public class RemoveInvalidInjectParamAnnotationQuickFix extends RemoveParamAnnotationQuickFix {

    public RemoveInvalidInjectParamAnnotationQuickFix() {
    	super(Arrays.copyOf(ManagedBeanConstants.INVALID_INJECT_PARAMS_FQ,
                ManagedBeanConstants.INVALID_INJECT_PARAMS_FQ.length));
    }
    public String getParticipantId() {
        return RemoveInvalidInjectParamAnnotationQuickFix.class.getName();
    }

}
