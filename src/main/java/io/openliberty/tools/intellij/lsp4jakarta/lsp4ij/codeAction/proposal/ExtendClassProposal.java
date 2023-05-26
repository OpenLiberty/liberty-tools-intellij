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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ImplementInterfaceProposal;

public class ExtendClassProposal extends ImplementInterfaceProposal {

    public ExtendClassProposal(String name, PsiFile sourceCU, PsiClass binding, PsiFile astRoot,
                               String interfaceType, int relevance) {
        super(null, binding, astRoot, interfaceType, relevance, sourceCU);
    }

}
