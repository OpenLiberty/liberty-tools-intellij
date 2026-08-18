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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.di;

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationBodyDeclarationsQuickFix;

/**
 * Quick fix for removing all body declarations (methods and fields) from a @Scope annotated type.
 * Extends the generic RemoveAnnotationBodyDeclarationsQuickFix base class.
 *
 * <p>Jakarta EE Dependency Injection specification states that scope annotations
 * must not declare attributes. This quick fix removes all methods and fields
 * from annotation interfaces annotated with @Scope.</p>
 */
public class RemoveScopeAttributesQuickFix extends RemoveAnnotationBodyDeclarationsQuickFix {

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getLabelKey() {
        return "RemoveScopeAttributes";
    }
}
