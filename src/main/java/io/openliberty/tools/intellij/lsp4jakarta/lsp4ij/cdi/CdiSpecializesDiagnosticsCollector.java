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
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.util.PsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.*;

/**
 * CDI diagnostics collector that validates specialization.
 * <p>
 * Per CDI spec section 3.1.4: a class annotated with @Specializes must directly
 * extend a managed bean (one whose immediate superclass carries a CDI scope annotation,
 * including custom @NormalScope-annotated scopes). A scoped grandparent does NOT
 * satisfy this requirement.
 * <p>
 * Per CDI spec section 4.3: a specialized bean must not declare an explicit bean
 * name using @Named.
 * <p>
 * Per CDI spec section 5.1.3: detects inconsistent specialization where more than one
 * bean annotated with @Specializes directly extends the same superclass bean.
 *
 * @see <a href="https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0#specializing_a_managed_bean">CDI 3.0 §3.1.4</a>
 * @see <a href="https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0#direct_and_indirect_specialization">CDI 3.0 §4.3</a>
 * @see <a href="https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0#inconsistent_specialization">CDI 3.0 §5.1.3</a>
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

        // Get all classes including nested classes
        List<PsiClass> allTypes = new ArrayList<>();
        PsiUtils.collectAllClasses(unit.getClasses(), allTypes);

        for (PsiClass type : allTypes) {
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

                // Rule 3: inconsistent specialization — more than one direct subclass of the
                // immediate superclass carries @Specializes (CDI spec §5.1.3).
                PsiClass directSuperClass = type.getSuperClass();
                if (directSuperClass != null) {
                    String superFqName = directSuperClass.getQualifiedName();
                    if (superFqName != null && !JAVA_LANG_OBJECT.equals(superFqName)) {
                        if (hasInconsistentSpecialization(directSuperClass, type)) {
                            diagnostics.add(createDiagnostic(type, unit,
                                    Messages.getMessage("InconsistentSpecialization",
                                            type.getName(), superFqName),
                                    DIAGNOSTIC_CODE_INCONSISTENT_SPECIALIZATION, null,
                                    DiagnosticSeverity.Error));
                        }
                    }
                }
            }
        }
    }

    /**
     * Validates that a class annotated with @Specializes directly extends a valid bean.
     * <p>
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

    /**
     * Returns {@code true} if more than one direct subclass of {@code superClass}
     * carries {@code @Specializes}, indicating inconsistent specialization.
     *
     * <p>Uses {@link ClassInheritorsSearch} with {@code deep=false} to restrict the
     * search to immediate subclasses only, so valid linear transitive chains are not flagged.
     *
     * @param superClass   the direct superclass to inspect
     * @param currentType  the type being validated (used to resolve the project scope)
     * @return {@code true} if the specialization is inconsistent
     */
    private boolean hasInconsistentSpecialization(PsiClass superClass, PsiClass currentType) {
        GlobalSearchScope scope = GlobalSearchScope.projectScope(currentType.getProject());
        int specializerCount = 0;
        for (PsiClass subclass : ClassInheritorsSearch.search(superClass, scope, false)) {
            if (isMatchedAnnotation(subclass.getAnnotations(), SPECIALIZES_FQ_NAME)) {
                specializerCount++;
                if (specializerCount > 1) {
                    return true;
                }
            }
        }
        return false;
    }
}
