/* Copyright (c) 2021 IBM Corporation.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Hani Damlaj
*******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi;

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.InsertAnnotationMissingQuickFix;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.servlet.HttpServletQuickFix;

public class ManagedBeanConstructorQuickFix extends InsertAnnotationMissingQuickFix {
    public ManagedBeanConstructorQuickFix() {
        super("jakarta.inject.Inject");
    }
    @Override
    public String getParticipantId() {
        return ManagedBeanConstructorQuickFix.class.getName();
    }
}
