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
                        //Validate @Id and @Temporal annotations
                        //Spec: https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2#a132
                        PsiAnnotation[] fieldAnnotations = field.getAnnotations();
                        PsiAnnotation id = null, temporal = null;
                        for (PsiAnnotation fieldAnnotation : fieldAnnotations) {
                            String matchedAnnotation = getMatchedJavaElementName(type, fieldAnnotation.getQualifiedName(),
                                    PersistenceConstants.SET_OF_PRIMARY_KEY_DATE_ANNOTATIONS);

                            if (matchedAnnotation != null) {
                                if (matchedAnnotation.equals(PersistenceConstants.ID)) {
                                    id = fieldAnnotation;
                                } else if (matchedAnnotation.equals(PersistenceConstants.TEMPORAL)) {
                                    temporal = fieldAnnotation;
                                }
                            }
                        }

                        if (id != null) {
                            String fieldTypeFQ = null;
                            if (field.getType() instanceof PsiClassType classType) {
                                PsiClass psiClass = classType.resolve();
                                fieldTypeFQ = psiClass.getQualifiedName();
                            }
                            if (fieldTypeFQ != null && fieldTypeFQ.equals(PersistenceConstants.UTIL_DATE)) {
                                if (temporal != null) {
                                    if (!isValidTemporalDateValue(temporal.findAttributeValue("value"))) {
                                        // Add diagnostics for invalid type
                                        diagnostics.add(createDiagnostic(temporal, unit,
                                                Messages.getMessage("InvalidValueInTemporalAnnotation"),
                                                PersistenceConstants.DIAGNOSTIC_CODE_TEMPORAL_INVALID_VALUE, null,
                                                DiagnosticSeverity.Error));
                                    }
                                } else {
                                    // Add diagnostics for missing annotation
                                    diagnostics.add(createDiagnostic(field, unit,
                                            Messages.getMessage("MissingTemporalAnnotation"),
                                            PersistenceConstants.DIAGNOSTIC_CODE_MISSING_TEMPORAL, null,
                                            DiagnosticSeverity.Error));
                                }
                            }
                        }
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
                }
            }
        }
        // We do not do anything if the found unit is null
    }

    private boolean isValidTemporalDateValue(PsiAnnotationMemberValue value) {
        if(value == null) return false;
        if (!(value instanceof PsiReferenceExpression ref)) return false;
        if (!(ref.resolve() instanceof PsiEnumConstant)) return false;
        return PersistenceConstants.TEMPORAL_TYPE_DATE.equals(value.getText());
    }
}
