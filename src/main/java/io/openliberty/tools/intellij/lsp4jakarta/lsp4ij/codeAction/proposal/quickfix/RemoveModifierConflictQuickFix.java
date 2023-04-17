/*******************************************************************************
* Copyright (c) 2021, 2023 IBM Corporation and others.
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

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.ModifyModifiersProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * QuickFix for removing modifiers.
 *
 * @author Himanshu Chotwani
 *
 */
public class RemoveModifierConflictQuickFix {
    
    private final String[] modifiers;

    protected final boolean generateOnlyOneCodeAction;
    
    
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
    
    /**
     * use setData() API with diagnostic to pass in ElementType in diagnostic collector class.
     *
     */
    private void removeModifier(Diagnostic diagnostic, JavaCodeActionContext context,
            List<CodeAction> codeActions, String... modifier) {
        PsiElement node = context.getCoveredNode();
        PsiClass parentType = getBinding(node);
        PsiModifierListOwner modifierListOwner = PsiTreeUtil.getParentOfType(node, PsiModifierListOwner.class);

        String type = "";
        if (modifierListOwner instanceof PsiLocalVariable) {
            type = "variable";
        } else if (modifierListOwner instanceof PsiField) {
            type = "field";
        } else if (modifierListOwner instanceof PsiMethod) {
            type = "method";
        } else if (modifierListOwner instanceof PsiClass) {
            type = "class";
        }

        String name = "Remove the \'" + modifier[0] + "\' modifier from this ";
        name = name.concat(type);
        ModifyModifiersProposal proposal = new ModifyModifiersProposal(name, context.getSource().getCompilationUnit(),
                context.getASTRoot(), parentType, 0, modifierListOwner.getModifierList(), Collections.emptyList(), Arrays.asList(modifier));
        CodeAction codeAction = context.convertToCodeAction(proposal, diagnostic);

        if (codeAction != null) {
            codeActions.add(codeAction);
        }
    }

    /**
     * Removes a set of modifiers from a given ASTNode with a given code action label
     */
    /** protected void removeModifier(Diagnostic diagnostic, JavaCodeActionContext context, IBinding parentType,
		List<CodeAction> codeActions, ASTNode coveredNode, String label, String... modifier) throws CoreException {
	
        ModifyModifiersProposal proposal = new ModifyModifiersProposal(label, context.getCompilationUnit(),
                context.getASTRoot(), parentType, 0, coveredNode, new ArrayList<>(), Arrays.asList(modifiers));
        CodeAction codeAction = context.convertToCodeAction(proposal, diagnostic);

        if (codeAction != null) {
            codeActions.add(codeAction);
        }
    } **/

    protected PsiClass getBinding(PsiElement node) {
        /** ASTNode parentNode = node.getParent();
        if (node.getParent() instanceof VariableDeclarationFragment) {
            return ((VariableDeclarationFragment) node.getParent()).resolveBinding();
        } else if (node.getParent() instanceof MethodDeclaration) {
            return ((MethodDeclaration) node.getParent()).resolveBinding();
        }
        return Bindings.getBindingOfParentType(node); **/
        return PsiTreeUtil.getParentOfType(node, PsiClass.class);
    }

}
