/*******************************************************************************
* Copyright (c) 2021 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     IBM Corporation, Himanshu Chotwani - initial API and implementation
*******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix;


/**
 * 
 * Quick fix for removing final when it is used for @Inject field
 * 
 * @author Himanshu Chotwani
 *
 */
public class RemoveFinalModifierQuickFix extends RemoveModifierConflictQuickFix { 

    public RemoveFinalModifierQuickFix() {
        super(false, "final");
    }

    @Override
    public String getParticipantId() {
        return RemoveFinalModifierQuickFix.class.getName();
    }
}
