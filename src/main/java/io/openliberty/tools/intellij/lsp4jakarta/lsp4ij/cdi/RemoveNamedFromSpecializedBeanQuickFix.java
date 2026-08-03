/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationConflictQuickFix;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.NAMED_FQ_NAME;

/**
 * Removes the @Named annotation from a bean annotated with @Specializes.
 *
 * <p>Per CDI 3.0 §4.3, a specialized bean inherits its bean name from the bean it
 * specializes. Declaring @Named on a specialized bean is therefore invalid.</p>
 *
 * @see <a href="https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0#direct_and_indirect_specialization">CDI 3.0 §4.3</a>
 */
public class RemoveNamedFromSpecializedBeanQuickFix extends RemoveAnnotationConflictQuickFix {

    public RemoveNamedFromSpecializedBeanQuickFix() {
        super(false, NAMED_FQ_NAME);
    }

    @Override
    public String getParticipantId() {
        return RemoveNamedFromSpecializedBeanQuickFix.class.getName();
    }
}
