/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.ModifyAnnotationProposal;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.ModifyModifiersProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.util.ExceptionUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Quickfix for adding modifiers to the Nested Class.
 */
public abstract class InsertModifierToNestedClassQuickFix implements IJavaCodeActionParticipant {

    /**
     * Logger object to record events for this class.
     */
    private static final Logger LOGGER = Logger.getLogger(InsertModifierToNestedClassQuickFix.class.getName());
    /**
     * modifier to add.
     */
    private final String modifier;

    /**
     * Constructor.
     *
     * @param modifier The modifier to add.
     */
    public InsertModifierToNestedClassQuickFix(String modifier) {
        this.modifier = modifier;
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        List<CodeAction> codeActions = new ArrayList<>();
        codeActions.add(JDTUtils.createCodeAction(context, diagnostic, getLabel(modifier), getParticipantId()));
        return codeActions;
    }

    /**
     * {@inheritDoc}
     * Resolves a code action by inserting the appropriate modifier into a nested class matching
     * the annotated field or method parameter type.
     */
    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        if (node.getParent() instanceof PsiField field) {
            if (field.getType() instanceof PsiClassType classType) {
                if (insertModifier(context, classType, modifier, toResolve)) {
                    return toResolve;
                }
            }
        } else if (node.getParent() instanceof PsiMethod method) {
            for (PsiParameter param : method.getParameterList().getParameters()) {
                if (param.getType() instanceof PsiClassType classType) {
                    if (insertModifier(context, classType, modifier, toResolve)) {
                        return toResolve;
                    }
                }
            }
        }
        return toResolve;
    }

    private boolean insertModifier(
            JavaCodeActionResolveContext context,
            PsiClassType classType,
            String modifier,
            CodeAction toResolve
    ) {
        PsiClass injectedClass = classType.resolve();
        if (injectedClass == null || !needsStaticModifier(injectedClass)) {
            return false;
        }
        final String label = getLabel(modifier);
        final ChangeCorrectionProposal proposal = new ModifyModifiersProposal(
                label,
                context.getSource().getCompilationUnit(),
                context.getASTRoot(),
                injectedClass,
                0,
                injectedClass.getModifierList(),
                List.of(modifier)
        );
        ExceptionUtil.executeWithWorkspaceEditHandling(
                context, proposal, toResolve, LOGGER,
                "Unable to create workspace edit for code action " + label
        );
        return true;
    }

    /**
     * needsStaticModifier
     * It checks whether static modifier is applicable
     *
     * @param injectedClass
     * @return
     */
    private boolean needsStaticModifier(PsiClass injectedClass) {
        return injectedClass.getContainingClass() != null
                && !injectedClass.hasModifierProperty(modifier);
    }

    /**
     * Returns the label associated with the input modifier.
     *
     * @param modifier The modifier to add.
     * @return The label associated with the input modifier.
     */
    protected String getLabel(String modifier) {
        return Messages.getMessage("InsertModifierToNestedClass", modifier);
    }
}
