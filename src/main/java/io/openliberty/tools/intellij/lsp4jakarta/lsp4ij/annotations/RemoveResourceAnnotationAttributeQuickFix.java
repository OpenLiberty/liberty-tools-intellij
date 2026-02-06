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

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.annotations;


import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationAttributesQuickFix;

/**
 * Removes the @Resource annotation attribute
 */
public class RemoveResourceAnnotationAttributeQuickFix extends RemoveAnnotationAttributesQuickFix {

    public RemoveResourceAnnotationAttributeQuickFix() {
        super("jakarta.annotation.Resource","type");
    }

    @Override
    public String getLabel() {
        return Messages.getMessage("RemoveAttribute", "type", "@Resource");
    }

    @Override
    public String getParticipantId() {
        return RemoveResourceAnnotationAttributeQuickFix.class.getName();
    }
}
