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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence;

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.InsertAnnotationMissingQuickFix;

/**
 * Quick fix for inserting the @Entity annotation on a class that does not have it
 * but is annotated with @NamedEntityGraph or @NamedEntityGraphs.
 *
 * Addresses diagnostics: {@link PersistenceConstants#DIAGNOSTIC_CODE_NAMED_ENTITY_GRAPH_ON_NON_ENTITY}
 * and {@link PersistenceConstants#DIAGNOSTIC_CODE_NAMED_ENTITY_GRAPHS_ON_NON_ENTITY}
 */
public class InsertEntityAnnotationQuickFix extends InsertAnnotationMissingQuickFix {

    public InsertEntityAnnotationQuickFix() {
        super(PersistenceConstants.ENTITY);
    }

    @Override
    public String getParticipantId() {
        return InsertEntityAnnotationQuickFix.class.getName();
    }
}
