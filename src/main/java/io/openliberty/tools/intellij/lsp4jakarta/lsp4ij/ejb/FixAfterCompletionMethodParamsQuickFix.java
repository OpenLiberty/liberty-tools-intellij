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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb;

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.ReplaceParametersWithSingleParamQuickFix;

/**
 * QuickFix for {@code InvalidAfterCompletionMethodParams}: fixes the parameter
 * list of an {@code @AfterCompletion} method so it declares exactly one
 * {@code boolean} parameter, as required by the EJB specification.
 *
 * <p>Extends the generic {@link ReplaceParametersWithSingleParamQuickFix},
 * supplying {@code "boolean"} as the required type and {@code "committed"}
 * as the default parameter name.
 */
public class FixAfterCompletionMethodParamsQuickFix extends ReplaceParametersWithSingleParamQuickFix {

    @Override
    public String getParticipantId() {
        return FixAfterCompletionMethodParamsQuickFix.class.getName();
    }

    @Override
    protected String getRequiredTypeName() {
        return "boolean";
    }

    /**
     * {@inheritDoc}
     * <p>Used as the inserted parameter name when the method has no parameters at all.
     */
    @Override
    protected String getDefaultParamName() {
        return "committed";
    }

    @Override
    protected String getLabel() {
        return Messages.getMessage("FixAfterCompletionMethodParam");
    }
}
