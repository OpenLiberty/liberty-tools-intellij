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
 *     IBM Corporation - initial implementation
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi;

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.ReplaceAnnotationsQuickFix;

import java.util.List;

/**
 * Quickfix for InvalidInterceptorOrDecorator diagnostic.
 * Replaces all invalid scope annotations with @Dependent.
 */
public class ReplaceInvalidScopesWithDependentQuickFix extends ReplaceAnnotationsQuickFix {

    /**
     * Constructor.
     */
    public ReplaceInvalidScopesWithDependentQuickFix() {
        super(ManagedBeanConstants.DEPENDENT_FQ_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getParticipantId() {
        return ReplaceInvalidScopesWithDependentQuickFix.class.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getCodeActionLabel(List<String> annotationsToRemove) {
        return Messages.getMessage("ReplaceAnnotationWith", formatAnnotationNames(annotationsToRemove), "@Dependent");
    }
}

