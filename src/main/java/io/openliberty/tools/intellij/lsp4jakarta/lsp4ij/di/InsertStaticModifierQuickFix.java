/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.di;

import com.intellij.psi.PsiModifier;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.InsertModifierToNestedClassQuickFix;

public class InsertStaticModifierQuickFix extends InsertModifierToNestedClassQuickFix {

    public InsertStaticModifierQuickFix() {
        super(PsiModifier.STATIC);
    }

    @Override
    public String getParticipantId() {
        return InsertStaticModifierQuickFix.class.getName();
    }
}
