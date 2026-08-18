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

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.JavaPsiFacade;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.Change;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeActionKind;

import java.util.List;

/**
 * Code action proposal for adding/removing thrown exceptions in a method signature.
 */
public class ModifyThrownExceptionsProposal extends ChangeCorrectionProposal {

    private final PsiFile sourceCU;
    private final PsiFile invocationNode;
    private final PsiMethod method;

    // list of exceptions to add (fully qualified names)
    private final List<String> exceptionsToAdd;

    // list of exceptions to remove (fully qualified names)
    private final List<String> exceptionsToRemove;

    public ModifyThrownExceptionsProposal(String label,
                                          PsiFile sourceCU,
                                          PsiFile invocationNode,
                                          PsiMethod method,
                                          int relevance,
                                          List<String> exceptionsToAdd,
                                          List<String> exceptionsToRemove) {
        super(label, CodeActionKind.QuickFix, relevance);
        this.sourceCU = sourceCU;
        this.invocationNode = invocationNode;
        this.method = method;
        this.exceptionsToAdd = exceptionsToAdd;
        this.exceptionsToRemove = exceptionsToRemove;
    }

    @Override
    public Change getChange() {
        PsiReferenceList throwsList = method.getThrowsList();
        PsiElementFactory factory = JavaPsiFacade.getElementFactory(method.getProject());
        // Remove exceptions
        exceptionsToRemove.forEach(exceptionFQN -> {
            for (PsiJavaCodeReferenceElement ref : throwsList.getReferenceElements()) {
                if (ref.getQualifiedName() != null && ref.getQualifiedName().equals(exceptionFQN)) {
                    ref.delete();
                }
            }
        });
        // Add exceptions
        exceptionsToAdd.forEach(exceptionFQN -> {
                PsiClassType exceptionType = factory.createTypeByFQClassName(exceptionFQN, method.getResolveScope());
                throwsList.add(factory.createReferenceElementByType(exceptionType));
        });
        final Document document = invocationNode.getViewProvider().getDocument();
        return new Change(sourceCU.getViewProvider().getDocument(), document);
    }
}
