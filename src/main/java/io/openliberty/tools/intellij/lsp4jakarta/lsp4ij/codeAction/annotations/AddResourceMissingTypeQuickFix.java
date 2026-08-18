/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.annotations;

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.InsertAnnotationAttributesQuickFix;

/**
 * Quickfix for adding missing type to @Resource
 *
 *
 * @author Zijian Pei
 *
 */
public class AddResourceMissingTypeQuickFix extends InsertAnnotationAttributesQuickFix {

    public AddResourceMissingTypeQuickFix() {
        super("jakarta.annotation.Resource", false, "type");
    }

    @Override
    public String getParticipantId() {
        return AddResourceMissingTypeQuickFix.class.getName();
    }
}
