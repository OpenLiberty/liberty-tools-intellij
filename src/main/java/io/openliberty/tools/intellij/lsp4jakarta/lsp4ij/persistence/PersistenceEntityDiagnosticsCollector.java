/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation, Ankush Sharma and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation, Ankush Sharma - initial API and implementation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence;

import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.DiagnosticsUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.List;

/**
 * @author ankushsharma
 * @brief Diagnostics implementation for Jakarta Persistence 3.0
 */

public class PersistenceEntityDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public PersistenceEntityDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return PersistenceConstants.DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit != null) {
            PsiClass[] alltypes;
            PsiAnnotation[] allAnnotations;

            alltypes = unit.getClasses();
            for (PsiClass type : alltypes) {
                allAnnotations = type.getAnnotations();

                /* ============ Entity Annotation Diagnostics =========== */
                PsiAnnotation EntityAnnotation = null;
                for (PsiAnnotation annotation : allAnnotations) {
                    if (isMatchedJavaElement(type, annotation.getQualifiedName(), PersistenceConstants.ENTITY)) {
                        EntityAnnotation = annotation;
                    }
                }

                if (EntityAnnotation != null) {
                    // Define boolean requirements for the diagnostics
                    boolean hasPublicOrProtectedNoArgConstructor = false;
                    boolean hasArgConstructor = false;
                    boolean isEntityClassFinal = false;
                    boolean hasPrimaryKey = false;

                    // Get the Methods of the annotated Class
                    for (PsiMethod method : type.getMethods()) {
                        if (isConstructorMethod(method)) {
                            // We have found a method that is a constructor
                            if (method.getParameterList().getParametersCount() > 0) {
                                hasArgConstructor = true;
                                continue;
                            }
                            // Don't need to perform subtractions to check flags because eclipse notifies on
                            // illegal constructor modifiers
                            if (!method.hasModifierProperty(PsiModifier.PUBLIC) && !method.hasModifierProperty(PsiModifier.PROTECTED))
                                continue;
                            hasPublicOrProtectedNoArgConstructor = true;
                        }
                        // All Methods of this class should not be final
                        if (method.hasModifierProperty(PsiModifier.FINAL)) {
                            diagnostics.add(createDiagnostic(method, unit,
                                    Messages.getMessage("EntityNoFinalMethods"),
                                    PersistenceConstants.DIAGNOSTIC_CODE_FINAL_METHODS, method.getReturnType().getInternalCanonicalText(),
                                    DiagnosticSeverity.Error));
                        }

                        // Check if any method has @Id or @EmbeddedId annotation
                        if (!hasPrimaryKey && hasPrimaryKeyAnnotation(type, method.getAnnotations())) {
                            hasPrimaryKey = true;
                        }

                    }

                    // Go through the instance variables and make sure no instance vars are final
                    for (PsiField field : type.getFields()) {
                        // If a field is static, we do not care about it, we care about all other field
                        if (field.hasModifierProperty(PsiModifier.STATIC)) {
                            continue;
                        }
                        // If we find a non-static variable that is final, this is a problem
                        if (field.hasModifierProperty(PsiModifier.FINAL)) {
                            diagnostics.add(createDiagnostic(field, unit,
                                    Messages.getMessage("EntityNoFinalVariables"),
                                    PersistenceConstants.DIAGNOSTIC_CODE_FINAL_VARIABLES, field.getType().getInternalCanonicalText(),
                                    DiagnosticSeverity.Error));
                        }

                        // Check if any field has @Id or @EmbeddedId annotation
                        if (!hasPrimaryKey && hasPrimaryKeyAnnotation(type, field.getAnnotations())) {
                            hasPrimaryKey = true;
                        }
                    }

                    // Check superclass hierarchy for primary key in @MappedSuperclass
                    if (!hasPrimaryKey) {
                        hasPrimaryKey = hasPrimaryKeyInSuperclass(type);
                    }

                    // Ensure that the Entity class is not given a final modifier
                    if (type.hasModifierProperty(PsiModifier.FINAL))
                        isEntityClassFinal = true;

                    // Create Diagnostics if needed
                    if (!hasPublicOrProtectedNoArgConstructor && hasArgConstructor) {
                        diagnostics.add(createDiagnostic(type, unit,
                                Messages.getMessage("EntityNoArgConstructor"),
                                PersistenceConstants.DIAGNOSTIC_CODE_MISSING_EMPTY_CONSTRUCTOR, null,
                                DiagnosticSeverity.Error));
                    }

                    if (isEntityClassFinal) {
                        diagnostics.add(createDiagnostic(type, unit,
                                Messages.getMessage("EntityNoFinalClass"),
                                PersistenceConstants.DIAGNOSTIC_CODE_FINAL_CLASS, type.getQualifiedName(),
                                DiagnosticSeverity.Error));
                    }

                    if (!hasPrimaryKey) {
                        diagnostics.add(createDiagnostic(type, unit,
                                Messages.getMessage("EntityMissingPrimaryKey"),
                                PersistenceConstants.DIAGNOSTIC_CODE_MISSING_PRIMARY_KEY, null,
                                DiagnosticSeverity.Error));
                    }
                }
            }
        }
        // We do not do anything if the found unit is null
    }

    /**
     * Check if the given annotations contain @Id or @EmbeddedId
     *
     * @param type the type context for resolving annotations
     * @param annotations the annotations to check
     * @return true if a primary key annotation is found
     */
    private boolean hasPrimaryKeyAnnotation(PsiClass type, PsiAnnotation[] annotations) {
        for (PsiAnnotation annotation : annotations) {
            if (isMatchedJavaElement(type, annotation.getQualifiedName(), PersistenceConstants.ID) || isMatchedJavaElement(type, annotation.getQualifiedName(), PersistenceConstants.EMBEDDEDID)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the type or its superclass hierarchy (annotated with @MappedSuperclass)
     * contains a primary key (@Id or @EmbeddedId)
     *
     * @param type the type to check
     * @return true if a primary key is found in the hierarchy
     */
    private boolean hasPrimaryKeyInSuperclass(PsiClass type) {
        List<PsiClass> hierarchySuperClasses = DiagnosticsUtils.collectSuperClasses(type);

        for (PsiClass superClass : hierarchySuperClasses) {
            if (superClass.equals(type)) {
                continue;
            }
            // Check if superclass is annotated with @MappedSuperclass
            boolean isMappedSuperclass = false;
            for (PsiAnnotation annotation : superClass.getAnnotations()) {
                if (isMatchedJavaElement(type, annotation.getQualifiedName(), PersistenceConstants.MAPPEDSUPERCLASS)) {
                    isMappedSuperclass = true;
                    break;
                }
            }

            // Only check for primary key if it's a @MappedSuperclass
            if (isMappedSuperclass) {
                // Check fields in superclass
                for (PsiField field : superClass.getFields()) {
                    if (hasPrimaryKeyAnnotation(superClass, field.getAnnotations())) {
                        return true;
                    }
                }

                // Check methods in superclass
                for (PsiMethod method : superClass.getMethods()) {
                    if (hasPrimaryKeyAnnotation(superClass, method.getAnnotations())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
