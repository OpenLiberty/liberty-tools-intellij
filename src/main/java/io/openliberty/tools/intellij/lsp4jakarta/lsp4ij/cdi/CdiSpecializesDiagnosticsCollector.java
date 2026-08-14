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

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.*;

/**
 * CDI diagnostics collector that validates specialization.
 *
 * Per CDI spec section 3.1.4: a class annotated with @Specializes must directly
 * extend a managed bean (one whose immediate superclass carries a CDI scope annotation,
 * including custom @NormalScope-annotated scopes). A scoped grandparent does NOT
 * satisfy this requirement.
 *
 * @see <a href="https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0#specializing_a_managed_bean">CDI 3.0 §3.1.4</a>
 */
public class CdiSpecializesDiagnosticsCollector extends AbstractDiagnosticsCollector {

    private static final Logger LOGGER = Logger.getLogger(CdiSpecializesDiagnosticsCollector.class.getName());

    public CdiSpecializesDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit == null) {
            return;
        }
        for (PsiClass type : unit.getClasses()) {
            if (AnnotationUtils.hasAnnotation(type, SPECIALIZES_FQ_NAME)) {
                validateSpecializes(type, unit, diagnostics);
                // https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0#direct_and_indirect_specialization
                // A specialized bean must not declare an explicit bean name using @Named.
                // The name is inherited from the bean it specializes.
                PsiAnnotation[] typeAnnotations = type.getAnnotations();
                for (PsiAnnotation annotation : typeAnnotations) {
                    if (isMatchedJavaElement(type, annotation.getQualifiedName(), NAMED_FQ_NAME)) {
                        diagnostics.add(createDiagnostic(annotation, unit,
                                Messages.getMessage("SpecializedBeanWithNamedAnnotation", type.getName()),
                                DIAGNOSTIC_CODE_SPECIALIZED_BEAN_NAMED, null,
                                DiagnosticSeverity.Error));
                        break;
                    }
                }
            }
        }
    }

    /**
     * Validates that a class annotated with @Specializes directly extends a valid bean.
     *
     * Per CDI spec 3.1.4: "the bean class of X must directly extend the bean class
     * of another managed bean Y". Only the immediate superclass is checked — a scoped
     * grandparent does NOT satisfy this requirement.
     *
     * @param type        the class to validate
     * @param unit        the containing file
     * @param diagnostics the list to add diagnostics to
     */
    private void validateSpecializes(PsiClass type, PsiJavaFile unit, List<Diagnostic> diagnostics) {
        // Per CDI spec 3.1.4, only the direct (immediate) superclass must be a bean.
        PsiClass superclass = type.getSuperClass();
        if (superclass == null) {
            diagnostics.add(createDiagnostic(type, unit,
                    Messages.getMessage("InvalidSpecializesAnnotationOnNonBeanSuperclass"),
                    DIAGNOSTIC_CODE_INVALID_SPECIALIZES, null,
                    DiagnosticSeverity.Error));
            return;
        }
        // Check built-in scope annotations first, then custom @NormalScope-annotated scopes.
        boolean directSuperclassIsBean =
                AnnotationUtils.hasAnyAnnotation(superclass, SCOPE_FQ_NAMES.toArray(String[]::new))
                || directSuperclassHasCustomScope(superclass);

        if (!directSuperclassIsBean) {
            diagnostics.add(createDiagnostic(type, unit,
                    Messages.getMessage("InvalidSpecializesAnnotationOnNonBeanSuperclass"),
                    DIAGNOSTIC_CODE_INVALID_SPECIALIZES, null,
                    DiagnosticSeverity.Error));
        }
    }

    /**
     * Returns {@code true} if {@code superclass} carries any annotation whose own
     * type is meta-annotated with {@code @NormalScope}.
     *
     * <p>This covers user-defined scopes such as {@code @MyScope} where the annotation
     * declaration is annotated with {@code @NormalScope}.</p>
     *
     * @param superclass the direct superclass to inspect
     * @return {@code true} if the superclass has a custom normal-scoped annotation
     */
    private boolean directSuperclassHasCustomScope(PsiClass superclass) {
        for (PsiAnnotation annotation : superclass.getAnnotations()) {
            String annotationFQName = annotation.getQualifiedName();
            if (annotationFQName == null) continue;
            try {
                PsiClass annotationType = JavaPsiFacade.getInstance(superclass.getProject())
                        .findClass(annotationFQName, superclass.getResolveScope());
                if (annotationType != null && AnnotationUtil.isAnnotated(annotationType, NORMAL_SCOPE_FQ_NAME, 0)) {
                    return true;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Exception during annotation type resolution for: " + annotationFQName, e);
            }
        }
        return false;
    }
}
