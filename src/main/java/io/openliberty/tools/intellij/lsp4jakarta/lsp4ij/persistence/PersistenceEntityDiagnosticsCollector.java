/*******************************************************************************
 * Copyright (c) 2020, 2026 IBM Corporation, Ankush Sharma and others.
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

import java.util.Arrays;
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


                        //Validate @Id and @Temporal annotations
                        validatePKDateTemporal(method,type,diagnostics,unit);

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

                        //Validate @Id and @Temporal annotations
                        validatePKDateTemporal(field,type,diagnostics,unit);
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
     * Check the annotation value is TemporalType.DATE Enum
     *
     * @param value
     * @return true if the value is a reference to a TemporalType.DATE else return false
     */
    private boolean isValidTemporalDateValue(PsiAnnotationMemberValue value) {
        return value instanceof PsiReferenceExpression ref
                && ref.resolve() instanceof PsiEnumConstant
                && PersistenceConstants.TEMPORAL_TYPE_DATE.equals(value.getText());
    }

    /**
     * Check @Temporal annotation exist for primary key field/property with @Id annotation
     * Specification: https://jakarta.ee/specifications/persistence/3.2/jakarta-persistence-spec-3.2#a132
     *
     * @param fieldOrProperty
     * @param type
     * @param diagnostics
     * @param unit
     */
    private void validatePKDateTemporal(PsiJvmModifiersOwner fieldOrProperty, PsiClass type, List<Diagnostic> diagnostics, PsiJavaFile unit) {

        PsiAnnotation[] annotations = null;
        PsiAnnotation id = null, temporal = null;
        String typeFQ = null;

        if (fieldOrProperty instanceof PsiMethod method) {
            annotations = method.getAnnotations();
            if (method.getReturnType() instanceof PsiClassType classType) {
                PsiClass psiClass = classType.resolve();
                typeFQ = psiClass != null ? psiClass.getQualifiedName() : "";
            }
        } else if (fieldOrProperty instanceof PsiField field) {
            annotations = field.getAnnotations();
            if (field.getType() instanceof PsiClassType classType) {
                PsiClass psiClass = classType.resolve();
                typeFQ = psiClass != null ? psiClass.getQualifiedName() : "";
            }
        }

        if (annotations != null) {
            for (PsiAnnotation annotation : annotations) {
                String matchedAnnotation = getMatchedJavaElementName(type, annotation.getQualifiedName(),
                        PersistenceConstants.SET_OF_PRIMARY_KEY_DATE_ANNOTATIONS);

                if (matchedAnnotation != null) {
                    if (matchedAnnotation.equals(PersistenceConstants.ID)) {
                        id = annotation;
                    } else if (matchedAnnotation.equals(PersistenceConstants.TEMPORAL)) {
                        temporal = annotation;
                    }
                }
            }
        }

        if (id != null) {

            if (typeFQ != null && typeFQ.equals(PersistenceConstants.UTIL_DATE)) {
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
                    diagnostics.add(createDiagnostic(fieldOrProperty, unit,
                            Messages.getMessage("MissingTemporalAnnotation"),
                            PersistenceConstants.DIAGNOSTIC_CODE_MISSING_TEMPORAL, null,
                            DiagnosticSeverity.Error));
                }
            }
        }
    }

    /**
     * Check if the given annotations contain @Id or @EmbeddedId
     *
     * @param type the type context for resolving annotations
     * @param annotations the annotations to check
     * @return true if a primary key annotation is found
     */
    private boolean hasPrimaryKeyAnnotation(PsiClass type, PsiAnnotation[] annotations) {
        return Arrays.stream(annotations).anyMatch(annotation ->
                getMatchedJavaElementName(type, annotation.getQualifiedName(),new String[] {PersistenceConstants.ID,PersistenceConstants.EMBEDDEDID}) != null);
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
