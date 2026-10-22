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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.security;

import com.google.gson.Gson;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Jakarta Security diagnostics collector that validates identity store definition beans.
 *
 * Ensures that beans annotated with @LdapIdentityStoreDefinition or @DatabaseIdentityStoreDefinition
 * comply with the Jakarta Security specification by having the required @ApplicationScoped scope.
 *
 * @see <a href="https://jakarta.ee/specifications/security/2.0/jakarta-security-spec-2.0#annotations-and-built-in-identitystore-beans">
 *      Jakarta Security Specification - Identity Store Annotations</a>
 */
public class SecurityIdentityStoreDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public SecurityIdentityStoreDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return Constants.DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit == null) {
            return;
        }

        // Identity store definition annotations to check
        List<String> identityStoreAnnotations = Arrays.asList(
            Constants.LDAP_IDENTITY_STORE_DEFINITION_FQ_NAME,
            Constants.DATABASE_IDENTITY_STORE_DEFINITION_FQ_NAME
        );

        for (PsiClass type : unit.getClasses()) {
            // Get all annotations on the type
            PsiAnnotation[] typeAnnotations = type.getAnnotations();
            List<String> annotationNames = Stream.of(typeAnnotations)
                    .map(PsiAnnotation::getQualifiedName)
                    .collect(Collectors.toList());

            // Check if type has any identity store definition annotation
            List<String> identityStoreDefAnnotations = getMatchedJavaElementNames(
                    type, annotationNames, identityStoreAnnotations);

            if (!identityStoreDefAnnotations.isEmpty()) {
                // Type has an identity store definition annotation
                String identityStoreAnnotationName = JDTUtils.getSimpleName(identityStoreDefAnnotations.get(0));
                
                // Now check if it has @ApplicationScoped
                boolean hasApplicationScoped = !getMatchedJavaElementNames(
                        type, annotationNames, Collections.singletonList(Constants.APPLICATION_SCOPED_FQ_NAME)).isEmpty();

                if (!hasApplicationScoped) {
                    // Check if it has any other scope annotation
                    List<String> foundScopes = getMatchedJavaElementNames(
                            type, annotationNames, Constants.SCOPE_FQ_NAMES);
                    
                    String wrongScope = foundScopes.isEmpty() ? null : JDTUtils.getSimpleName(foundScopes.get(0));

                    if (wrongScope == null) {
                        // Missing @ApplicationScoped annotation
                        diagnostics.add(createDiagnostic(
                                type,
                                unit,
                                Messages.getMessage("MissingApplicationScopedOnIdentityStoreDefinition",
                                        identityStoreAnnotationName),
                                Constants.DIAGNOSTIC_CODE_MISSING_APPLICATION_SCOPED,
                                null,
                                DiagnosticSeverity.Error));
                    } else {
                        // Has wrong scope annotation — pass the FQ names as data for the replace quickfix
                        diagnostics.add(createDiagnostic(
                                type,
                                unit,
                                Messages.getMessage("InvalidScopeOnIdentityStoreDefinition",
                                        identityStoreAnnotationName,
                                        wrongScope),
                                Constants.DIAGNOSTIC_CODE_INVALID_SCOPE,
                                new Gson().toJsonTree(foundScopes),
                                DiagnosticSeverity.Error));
                    }
                }
            }
        }
    }
}