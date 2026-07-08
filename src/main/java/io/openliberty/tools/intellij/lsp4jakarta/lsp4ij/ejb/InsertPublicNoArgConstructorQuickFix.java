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

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb;

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.common.InsertDefaultPubOrProConstructorQuickFix;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.List;

/**
 * QuickFix for adding a public no-arg constructor to session beans.
 */
public class InsertPublicNoArgConstructorQuickFix extends InsertDefaultPubOrProConstructorQuickFix {

    @Override
    public String getParticipantId() {
        return InsertPublicNoArgConstructorQuickFix.class.getName();
    }

    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        List<CodeAction> codeActions = new ArrayList<>();
        if (getBinding(context.getCoveredNode()) != null) {
            String constructorName = Messages.getMessage("AddPublicNoArgConstructor");
            codeActions.add(JDTUtils.createCodeAction(context, diagnostic, constructorName, getParticipantId()));
        }
        return codeActions;
    }
}