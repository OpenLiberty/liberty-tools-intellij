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

/**
 * Quick fix for removing the {@code @Typed} annotation.
 *
 * <p>Offered when the {@code @Typed} annotation's {@code value} member lists a class
 * that is not in the unrestricted set of bean types of the bean (CDI 3.0 §2.2.2).</p>
 */
public class RemoveTypedAnnotationQuickFix extends RemoveAnnotationConflictQuickFix {

    public RemoveTypedAnnotationQuickFix() {
        super(false, ManagedBeanConstants.TYPED_FQ_NAME);
    }

    @Override
    public String getParticipantId() {
        return RemoveTypedAnnotationQuickFix.class.getName();
    }
}
