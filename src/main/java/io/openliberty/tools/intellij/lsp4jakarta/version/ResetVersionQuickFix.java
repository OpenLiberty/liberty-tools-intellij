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

package io.openliberty.tools.intellij.lsp4jakarta.version;

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Diagnostic;

import java.util.Collections;
import java.util.List;

/**
 * Quick fix for resetting the Jakarta EE version of a project.
 *
 * The code action is command based: it triggers the {@code jakarta.resetVersion} command on the Jakarta
 * language server, which clears the selected version and prompts the user to select a new one.
 */
public class ResetVersionQuickFix implements IJavaCodeActionParticipant {

    /** The command executed by the Jakarta language server to reset the version. */
    public static final String RESET_VERSION_COMMAND = "jakarta.resetVersion";

    @Override
    public String getParticipantId() {
        return ResetVersionQuickFix.class.getName();
    }

    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        // The project URI (not the file URI) identifies the project on the language server
        String projectUri = JDTUtils.getProjectURI(context.getJavaProject());

        Command command = new Command();
        command.setTitle(Messages.getMessage("ResetJakartaEEVersion"));
        command.setCommand(RESET_VERSION_COMMAND);
        command.setArguments(Collections.singletonList(projectUri));

        CodeAction codeAction = new CodeAction();
        codeAction.setTitle(Messages.getMessage("ChangeJakartaEEVersion"));
        codeAction.setKind(CodeActionKind.QuickFix);
        codeAction.setDiagnostics(Collections.singletonList(diagnostic));
        codeAction.setCommand(command);

        return Collections.singletonList(codeAction);
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        // No resolution needed for command based code actions
        return context.getUnresolved();
    }
}
