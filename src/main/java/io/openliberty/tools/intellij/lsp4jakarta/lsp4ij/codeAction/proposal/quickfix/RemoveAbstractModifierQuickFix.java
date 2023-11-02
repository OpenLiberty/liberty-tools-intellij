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
 * Quick fix for removing abstract when it is used for method with @Inject
 * 
 * @author Himanshu Chotwani
 *
 */
public class RemoveAbstractModifierQuickFix extends RemoveModifierConflictQuickFix{
    public RemoveAbstractModifierQuickFix() {
        super (false, "abstract");
    }

    @Override
    public String getParticipantId() {
        return RemoveAbstractModifierQuickFix.class.getName();
    }
}
