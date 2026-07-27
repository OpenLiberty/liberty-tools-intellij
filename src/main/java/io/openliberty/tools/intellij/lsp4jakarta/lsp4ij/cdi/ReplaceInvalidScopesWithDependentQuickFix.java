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
 *     IBM Corporation - initial implementation
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi;

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix.ReplaceAnnotationsQuickFix;

import java.util.List;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils.getSimpleName;

/**
 * Quickfix for InvalidInterceptorOrDecorator diagnostic.
 * Replaces all invalid scope annotations with @Dependent.
 */
public class ReplaceInvalidScopesWithDependentQuickFix extends ReplaceAnnotationsQuickFix {

    /**
     * Constructor.
     */
    public ReplaceInvalidScopesWithDependentQuickFix() {
        super(ManagedBeanConstants.DEPENDENT_FQ_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getParticipantId() {
        return ReplaceInvalidScopesWithDependentQuickFix.class.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getCodeActionLabel(List<String> annotationsToRemove) {
        String formattedNames = formatAnnotationNames(annotationsToRemove);
        return Messages.getMessage("ReplaceInvalidScopesWithDependent", formattedNames);
    }

    /**
     * Formats a list of fully qualified annotation names for display.
     * Extracts simple names and joins them with commas, prefixed with @.
     *
     * @param annotationFqNames List of fully qualified annotation names
     * @return Formatted string (e.g., "@ApplicationScoped, @RequestScoped")
     */
    private String formatAnnotationNames(List<String> annotationFqNames) {
        List<String> names = annotationFqNames.stream()
                .map(fqName -> "@" + getSimpleName(fqName))
                .toList();

        if (names.isEmpty()) return "";
        if (names.size() == 1) return names.get(0);

        String allButLast = String.join(", ", names.subList(0, names.size() - 1));
        return String.join(" and ", allButLast, names.getLast());
    }
}

