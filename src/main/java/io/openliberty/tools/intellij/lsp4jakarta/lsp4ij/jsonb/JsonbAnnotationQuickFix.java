/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jsonb;

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.RemoveAnnotationConflictQuickFix;

/**
 * Quick fix for removing additional @JsonbCreator annotations when more than
 * one occur in a class
 *
 * @author Leslie Dawson
 *
 */
public class JsonbAnnotationQuickFix extends RemoveAnnotationConflictQuickFix {
    public JsonbAnnotationQuickFix() {
        super("jakarta.json.bind.annotation.JsonbCreator");
    }

    @Override
    public String getParticipantId() {
        return JsonbAnnotationQuickFix.class.getName();
    }
}
