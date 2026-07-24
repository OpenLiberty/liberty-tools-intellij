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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence;

import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.DiagnosticsUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.ejb.EjbConstants;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.servlet.ServletConstants;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.List;

/**
 * Diagnostics collector for {@code @PersistenceContext} injection rules.
 *
 * <p>Two rules are enforced, both grounded in the Jakarta Persistence 3.0 specification:
 *
 * <ol>
 *   <li>{@link PersistenceConstants#DIAGNOSTIC_CODE_PERSISTENCE_CONTEXT_NOT_IN_MANAGED_COMPONENT}:
 *       {@code @PersistenceContext} is used in a class that is not a container-managed component.
 *       Container-managed injection is only available in CDI beans, EJB session beans, and
 *       Jakarta Servlet components.
 *       Spec §7.6: <a href="https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0#a11791">§a11791</a>
 *   </li>
 *   <li>{@link PersistenceConstants#DIAGNOSTIC_CODE_EXTENDED_CONTEXT_IN_NON_STATEFUL}:
 *       {@code PersistenceContextType.EXTENDED} is used outside a {@code @Stateful} EJB.
 *       An extended persistence context can only be initiated within a stateful session bean.
 *       Spec §7.6.3: <a href="https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0#a11810">§a11810</a>
 *   </li>
 * </ol>
 */
public class PersistenceContextDiagnosticsCollector extends AbstractDiagnosticsCollector {

    /** FQN of HttpServlet, used with {@link DiagnosticsUtils#inheritsFrom}. */
    private static final String HTTP_SERVLET_FQ_NAME = "jakarta.servlet.http.HttpServlet";

    public PersistenceContextDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return PersistenceConstants.DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit == null) {
            return;
        }

        for (PsiClass type : unit.getClasses()) {
            // Check @PersistenceContext on the type itself
            PsiAnnotation typeAnnotation = type.getAnnotation(PersistenceConstants.PERSISTENCE_CONTEXT);
            if (typeAnnotation != null) {
                checkAnnotation(type, unit, type, diagnostics);
            }

            // Check @PersistenceContext on fields
            for (PsiField field : type.getFields()) {
                PsiAnnotation fieldAnnotation = field.getAnnotation(PersistenceConstants.PERSISTENCE_CONTEXT);
                if (fieldAnnotation != null) {
                    checkAnnotation(field, unit, type, diagnostics);
                }
            }

            // Check @PersistenceContext on methods
            for (PsiMethod method : type.getMethods()) {
                PsiAnnotation methodAnnotation = method.getAnnotation(PersistenceConstants.PERSISTENCE_CONTEXT);
                if (methodAnnotation != null) {
                    checkAnnotation(method, unit, type, diagnostics);
                }
            }
        }
    }

    /**
     * Applies both diagnostic rules for a single {@code @PersistenceContext} occurrence
     * on the given PSI element (type, field, or method).
     */
    private void checkAnnotation(PsiModifierListOwner element, PsiJavaFile unit, PsiClass type,
                                 List<Diagnostic> diagnostics) {
        // Rule 1: the enclosing class must be a container-managed component.
        if (!isManagedComponent(type)) {
            diagnostics.add(createDiagnostic(element, unit,
                    Messages.getMessage("PersistenceContextNotInManagedComponent"),
                    PersistenceConstants.DIAGNOSTIC_CODE_PERSISTENCE_CONTEXT_NOT_IN_MANAGED_COMPONENT,
                    null, DiagnosticSeverity.Error));
            return; // Rule 2 is irrelevant when Rule 1 fires.
        }

        // Rule 2: PersistenceContextType.EXTENDED is only valid in a @Stateful EJB.
        PsiAnnotation pcAnnotation = element.getAnnotation(PersistenceConstants.PERSISTENCE_CONTEXT);
        if (pcAnnotation != null
                && isExtendedContext(pcAnnotation)
                && !isMatchedAnnotation(type.getAnnotations(), EjbConstants.STATEFUL_FQ_NAME)) {
            diagnostics.add(createDiagnostic(element, unit,
                    Messages.getMessage("ExtendedPersistenceContextInNonStatefulBean"),
                    PersistenceConstants.DIAGNOSTIC_CODE_EXTENDED_CONTEXT_IN_NON_STATEFUL,
                    null, DiagnosticSeverity.Error));
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers — using existing shared infrastructure wherever possible
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} when the given class is a container-managed component:
     * a CDI bean (any CDI scope from {@link ManagedBeanConstants#SCOPE_FQ_NAMES}),
     * an EJB session bean ({@link EjbConstants#SESSION_BEAN_ANNOTATIONS}),
     * a servlet component ({@code @WebServlet}/{@code @WebFilter}/{@code @WebListener}),
     * or a class that inherits from {@code HttpServlet}
     * (checked via {@link DiagnosticsUtils#inheritsFrom}).
     */
    private boolean isManagedComponent(PsiClass type) {
        PsiAnnotation[] typeAnnotations = type.getAnnotations();

        // CDI scopes
        for (String scopeFqn : ManagedBeanConstants.SCOPE_FQ_NAMES) {
            if (isMatchedAnnotation(typeAnnotations, scopeFqn)) {
                return true;
            }
        }

        // EJB session beans
        for (String ejbFqn : EjbConstants.SESSION_BEAN_ANNOTATIONS) {
            if (isMatchedAnnotation(typeAnnotations, ejbFqn)) {
                return true;
            }
        }

        // Servlet component annotations
        if (isMatchedAnnotation(typeAnnotations, ServletConstants.WEB_SERVLET_FQ_NAME)
                || isMatchedAnnotation(typeAnnotations, ServletConstants.WEBFILTER_FQ_NAME)
                || isMatchedAnnotation(typeAnnotations, ServletConstants.WEB_LISTENER_FQ_NAME)) {
            return true;
        }

        // Subclass of HttpServlet
        return DiagnosticsUtils.inheritsFrom(type, HTTP_SERVLET_FQ_NAME);
    }

    /**
     * Returns {@code true} if the {@code @PersistenceContext} annotation explicitly
     * sets {@code type = PersistenceContextType.EXTENDED}.
     */
    private static boolean isExtendedContext(PsiAnnotation pcAnnotation) {
        PsiAnnotationMemberValue typeValue = pcAnnotation.findAttributeValue("type");
        if (typeValue == null) {
            return false;
        }
        String text = typeValue.getText();
        return text != null && text.endsWith(PersistenceConstants.PERSISTENCE_CONTEXT_TYPE_EXTENDED);
    }
}
