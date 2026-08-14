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
 * Inserts either @Entity or @MappedSuperclass on a class that has @NamedQuery,
 * @NamedQueries, @NamedNativeQuery, or @NamedNativeQueries but is missing both
 * required annotations. Two separate code actions are offered, one per annotation.
 *
 * Addresses diagnostics: {@link PersistenceConstants#DIAGNOSTIC_CODE_NAMED_QUERY_ON_INVALID_CLASS},
 * {@link PersistenceConstants#DIAGNOSTIC_CODE_NAMED_QUERIES_ON_INVALID_CLASS},
 * {@link PersistenceConstants#DIAGNOSTIC_CODE_NAMED_NATIVE_QUERY_ON_INVALID_CLASS},
 * and {@link PersistenceConstants#DIAGNOSTIC_CODE_NAMED_NATIVE_QUERIES_ON_INVALID_CLASS}
 */
public class InsertEntityOrMappedSuperclassAnnotationQuickFix extends InsertAnnotationMissingQuickFix {

    public InsertEntityOrMappedSuperclassAnnotationQuickFix() {
        super(PersistenceConstants.ENTITY, PersistenceConstants.MAPPEDSUPERCLASS);
    }

    @Override
    public String getParticipantId() {
        return InsertEntityOrMappedSuperclassAnnotationQuickFix.class.getName();
    }
}
