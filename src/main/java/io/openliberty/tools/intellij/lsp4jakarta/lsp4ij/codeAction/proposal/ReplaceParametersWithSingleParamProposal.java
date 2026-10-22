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
import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.Change;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import org.eclipse.lsp4j.CodeActionKind;

import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Generic PSI proposal that replaces a method's parameter list so it contains
 * exactly one parameter of a caller-specified primitive type.
 *
 * <p>The four cases handled automatically:
 * <ol>
 *   <li><b>No params</b> &rarr; inserts one parameter of the required type
 *       with the caller-supplied default name</li>
 *   <li><b>Single param, wrong type</b> &rarr; changes its type to the
 *       required type (name kept as-is)</li>
 *   <li><b>Multiple params, one already the required type</b> &rarr; removes
 *       all others; keeps the matching one</li>
 *   <li><b>Multiple params, none the required type</b> &rarr; keeps the first
 *       param but changes its type; removes all others</li>
 * </ol>
 *
 * <p>This proposal is annotation-agnostic. Callers supply the required
 * primitive type name (e.g. {@code "boolean"}) and default parameter name.
 */
public class ReplaceParametersWithSingleParamProposal extends ChangeCorrectionProposal {

    private final PsiFile sourceCU;
    private final PsiFile invocationNode;
    private final PsiMethod method;

    /** Java keyword for the required parameter type (e.g. {@code "boolean"}). */
    private final String requiredTypeName;

    /** Name used when inserting a brand-new parameter. */
    private final String defaultParamName;

    /**
     * Constructor.
     *
     * @param label            the code action label shown to the user
     * @param sourceCU         the source compilation unit
     * @param invocationNode   the PSI file root
     * @param method           the method whose parameters are to be fixed
     * @param relevance        the proposal relevance
     * @param requiredTypeName Java keyword for the required type (e.g. {@code "boolean"})
     * @param defaultParamName name used when inserting a brand-new parameter
     */
    public ReplaceParametersWithSingleParamProposal(String label, PsiFile sourceCU,
                                                    PsiFile invocationNode, PsiMethod method,
                                                    int relevance,
                                                    String requiredTypeName,
                                                    String defaultParamName) {
        super(label, CodeActionKind.QuickFix, relevance);
        this.sourceCU = sourceCU;
        this.invocationNode = invocationNode;
        this.method = method;
        this.requiredTypeName = requiredTypeName;
        this.defaultParamName = defaultParamName;
    }

    @Override
    public Change getChange() {
        PsiElementFactory factory = JavaPsiFacade.getInstance(method.getProject()).getElementFactory();
        PsiParameterList paramList = method.getParameterList();
        List<PsiParameter> params = Arrays.asList(paramList.getParameters());

        if (params.isEmpty()) {
            // Case 1: no params → insert "boolean committed" (or specified type/name)
            PsiParameter newParam = factory.createParameterFromText(
                    requiredTypeName + " " + defaultParamName, method);
            paramList.add(newParam);

        } else if (params.size() == 1) {
            // Case 2: single param with wrong type → change its type (name kept)
            PsiTypeElement oldType = params.get(0).getTypeElement();
            if (oldType != null) {
                PsiTypeElement newType = factory.createTypeElementFromText(requiredTypeName, method);
                oldType.replace(newType);
            }

        } else {
            // Multiple params
            OptionalInt matchIndex = findMatchingParamIndex(params);

            if (matchIndex.isPresent()) {
                // Case 3: one already has the required type → remove all others
                int keepIdx = matchIndex.getAsInt();
                IntStream.range(0, params.size())
                         .filter(i -> i != keepIdx)
                         .mapToObj(params::get)
                         .collect(Collectors.toList()) // collect first to avoid ConcurrentModificationException
                         .forEach(PsiElement::delete);
            } else {
                // Case 4: none has the required type → keep first, change its type; remove rest
                PsiTypeElement oldType = params.get(0).getTypeElement();
                if (oldType != null) {
                    PsiTypeElement newType = factory.createTypeElementFromText(requiredTypeName, method);
                    oldType.replace(newType);
                }
                params.stream().skip(1)
                      .collect(Collectors.toList())
                      .forEach(PsiElement::delete);
            }
        }

        final Document changed = invocationNode.getViewProvider().getDocument();
        return new Change(sourceCU.getViewProvider().getDocument(), changed);
    }

    /**
     * Returns the index of the first parameter whose type already matches the
     * required type name, or an empty {@link OptionalInt} if none matches.
     *
     * @param params the method's parameter list
     * @return an {@link OptionalInt} with the zero-based index of the first
     *         matching parameter, or empty if none is found
     */
    private OptionalInt findMatchingParamIndex(List<PsiParameter> params) {
        return IntStream.range(0, params.size())
                        .filter(i -> requiredTypeName.equals(params.get(i).getType().getCanonicalText()))
                        .findFirst();
    }
}
