/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation
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

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.PsiUtils;
import org.eclipse.lsp4j.Range;

public class PositionUtils {
    /**
     * Returns the LSP range for the given element.
     * PsiNameIdentifierOwners are class, method, variable
     * Variable includes enum constant, field, local variable
     *
     * @param element the java field.
     * @return the LSP range for the given field name.
     */
    public static Range toNameRange(PsiElement element) {
        PsiFile openable = element.getContainingFile();
        TextRange sourceRange;
        // TODO : why does lsp4mp use psifield.getNameIdentifier().getTextRange() instead of psielement.getTextRange()??
        if (element instanceof PsiNameIdentifierOwner) {
            sourceRange = ((PsiNameIdentifierOwner) element).getNameIdentifier().getTextRange();
        } else if (element instanceof PsiAnnotation) {
            sourceRange = ((PsiAnnotation) element).getTextRange();
        } else {
            sourceRange = element.getTextRange();
        }
        return PsiUtils.toRange(openable, sourceRange.getStartOffset(), sourceRange.getLength());
    }

    /**
     * Given an element in a Java file, reformat the code in the style currently active.
     * @param e an element in a Java compilation unit
     */
    public static void formatDocument(PsiElement e) {
        Document document = e.getContainingFile().getViewProvider().getDocument();
        CodeStyleManager.getInstance(e.getProject()).reformatText(e.getContainingFile(),
                0, document.getTextLength());
    }
}
