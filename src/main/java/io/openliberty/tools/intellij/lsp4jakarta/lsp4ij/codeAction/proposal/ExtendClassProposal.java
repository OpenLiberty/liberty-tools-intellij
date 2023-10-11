/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copied from /org.eclipse.jdt.ui/src/org/eclipse/jdt/internal/ui/text/correction/proposals/ImplementInterfaceProposal.java
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ASTRewriteCorrectionProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ImplementInterfaceProposal;
import org.eclipse.lsp4j.CodeActionKind;

public class ExtendClassProposal extends ASTRewriteCorrectionProposal {

    private final PsiClass fBinding;
    private final String parentClassType;

    public ExtendClassProposal(String name, PsiFile targetCU, PsiFile sourceCU,
                               PsiClass binding, String parentClassType, int relevance) {
        super(name, CodeActionKind.QuickFix, targetCU, relevance, sourceCU);
        fBinding = binding;
        this.parentClassType = parentClassType;
    }

    @Override
    public void performUpdate() {
        final Project project = fBinding.getProject();
        final PsiClass parentClass = JavaPsiFacade.getInstance(project).
                findClass(parentClassType, GlobalSearchScope.allScope(project));
        if (parentClass != null) {
            final PsiReferenceList extendsList = fBinding.getExtendsList();
            if (extendsList != null) {
                extendsList.add(PsiElementFactory.getInstance(project).
                        createClassReferenceElement(parentClass));
            }
        }
    }
}
