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
*    Himanshu Chotwani - initial API and implementation
*******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.ModifyModifiersProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.WorkspaceEdit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * QuickFix for removing modifiers.
 *
 * @author Himanshu Chotwani
 *
 */
public abstract class RemoveModifierConflictQuickFix implements IJavaCodeActionParticipant {
    
    private final String[] modifiers;

    protected final boolean generateOnlyOneCodeAction;

    private static final Logger LOGGER = Logger.getLogger(RemoveModifierConflictQuickFix.class.getName());
    
    
    /**
     * Constructor for remove modifier quick fix.
     *
     * <p>
     * The participant will generate a CodeAction per modifier.
     * </p>
     *
     * @param modifiers list of modifiers to remove.
     */
    public RemoveModifierConflictQuickFix(String... modifiers) {
        this(false, modifiers);
    }
  
    /**
     * Constructor for remove modifiers quick fix.
     *
     * @param generateOnlyOneCodeAction true if the participant must generate a
     *                                  CodeAction which remove the list of
     *                                  modifiers and false otherwise.
     * @param modifiers               list of modifiers to remove.
     */
    public RemoveModifierConflictQuickFix(boolean generateOnlyOneCodeAction, String... modifiers) {
        this.generateOnlyOneCodeAction = generateOnlyOneCodeAction;
        this.modifiers = modifiers;
    }

    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        List<CodeAction> codeActions = new ArrayList<>();
        removeModifiers(diagnostic, context, codeActions);
        return codeActions;
    }


    protected void removeModifiers(Diagnostic diagnostic, JavaCodeActionContext context,
                                   List<CodeAction> codeActions) {
        if (generateOnlyOneCodeAction || modifiers.length == 1) {
            removeModifier(diagnostic, context, codeActions, modifiers);
        } else {
            // Clone the psi.FileViewProvider for each CodeAction.
            for (String modifier : modifiers) {
                removeModifier(diagnostic, context.copy(), codeActions, modifier);
            }
        }
    }

        @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        final PsiClass parentType = getBinding(node);
        final PsiModifierListOwner modifierListOwner = PsiTreeUtil.getParentOfType(node, PsiModifierListOwner.class);

        String label = getLabel(modifierListOwner, modifiers);

        assert parentType != null;
        ModifyModifiersProposal proposal = new ModifyModifiersProposal(label, context.getSource().getCompilationUnit(),
            context.getASTRoot(), parentType, 0, modifierListOwner.getModifierList(), Collections.emptyList(), Arrays.asList(modifiers), false);

        try {
            WorkspaceEdit we = context.convertToWorkspaceEdit(proposal);
            toResolve.setEdit(we);
        } catch (ProcessCanceledException e) {
            //Since 2024.2 ProcessCanceledException extends CancellationException so we can't use multicatch to keep backward compatibility
            //TODO delete block when minimum required version is 2024.2
            throw e;
        } catch (IndexNotReadyException | CancellationException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unable to create workspace edit for code action " + label, e);
        }
        return toResolve;
    }

    private void removeModifier(Diagnostic diagnostic, JavaCodeActionContext context,
                                List<CodeAction> codeActions, String... modifier) {
        PsiElement node = context.getCoveredNode();
        PsiModifierListOwner modifierListOwner = PsiTreeUtil.getParentOfType(node, PsiModifierListOwner.class);
        String label = getLabel(modifierListOwner, modifier);
        codeActions.add(JDTUtils.createCodeAction(context, diagnostic, label, getParticipantId()));
    }

    private String getLabel(PsiModifierListOwner modifierListOwner, String... modifier) {
        String label;
        if (modifierListOwner instanceof PsiLocalVariable){
            label = Messages.getMessage("RemoveTheModifierFromThisVariable", modifier[0]);
        } else if (modifierListOwner instanceof PsiField) {
            label = Messages.getMessage("RemoveTheModifierFromThisField", modifier[0]);
        } else if (modifierListOwner instanceof PsiMethod) {
            label = Messages.getMessage("RemoveTheModifierFromThisMethod", modifier[0]);
        } else if (modifierListOwner instanceof PsiClass) {
            label = Messages.getMessage("RemoveTheModifierFromThisClass", modifier[0]);
        } else {
            label = Messages.getMessage("RemoveTheModifierFromThis", modifier[0], "");
        }
        return label;
    }

    protected PsiClass getBinding(PsiElement node) {
        return PsiTreeUtil.getParentOfType(node, PsiClass.class);
    }
}
