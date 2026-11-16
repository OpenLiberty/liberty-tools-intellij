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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.security;

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.ReplaceAnnotationsQuickFix;

import java.util.List;

/**
 * Quickfix for {@code InvalidScopeOnIdentityStoreDefinition} diagnostic.
 *
 * <p>Replaces the invalid scope annotation(s) with {@code @ApplicationScoped} on a class
 * annotated with {@code @LdapIdentityStoreDefinition} or {@code @DatabaseIdentityStoreDefinition}.</p>
 */
public class ReplaceWithApplicationScopedAnnotationQuickFix extends ReplaceAnnotationsQuickFix {

    /**
     * Constructor.
     */
    public ReplaceWithApplicationScopedAnnotationQuickFix() {
        super(Constants.APPLICATION_SCOPED_FQ_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getParticipantId() {
        return ReplaceWithApplicationScopedAnnotationQuickFix.class.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getCodeActionLabel(List<String> annotationsToRemove) {
        return Messages.getMessage("ReplaceAnnotationWith", formatAnnotationNames(annotationsToRemove), "@ApplicationScoped");
    }
}
