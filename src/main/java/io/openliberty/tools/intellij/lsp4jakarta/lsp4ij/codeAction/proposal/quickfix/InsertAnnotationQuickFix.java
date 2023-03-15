/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lidia Ataupillco Ramos
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix;

import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;

import java.util.ArrayList;
import java.util.List;

/**
 * Quickfix for adding new annotations with or without attributes
 *
 * @author Zijian Pei
 * @author Lidia Ataupillco Ramos
 *
 */
public class InsertAnnotationQuickFix {

    private final String[] attributes;

    private final String annotation;

    protected final boolean generateOnlyOneCodeAction;

    public InsertAnnotationQuickFix(String annotation, String... attributes) {
        this(annotation, false, attributes);
    }

    /**
     * Constructor for add missing attributes quick fix.
     *
     * @param generateOnlyOneCodeAction true if the participant must generate a
     *                                  CodeAction which add the list of attributes
     *                                  and false otherwise.
     * @param attributes                list of attributes to add.
     */
    public InsertAnnotationQuickFix(String annotation, boolean generateOnlyOneCodeAction,
                                    String... attributes) {
        this.annotation = annotation;
        this.generateOnlyOneCodeAction = generateOnlyOneCodeAction;
        this.attributes = attributes;
    }

    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
//        ASTNode node = context.getCoveredNode();
//        IBinding parentType = getBinding(node);

        List<CodeAction> codeActions = new ArrayList<>();
//        addAttributes(diagnostic, context, parentType, codeActions, annotation);

        return codeActions;
    }
}
