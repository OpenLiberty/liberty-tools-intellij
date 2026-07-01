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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiClassImplUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.DiagnosticsUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

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
                PsiAnnotation MappedSuperclassAnnotation = null;
                PsiAnnotation NamedEntityGraphAnnotation = null;
                PsiAnnotation NamedEntityGraphsAnnotation = null;
                PsiAnnotation NamedQueryAnnotation = null;
                PsiAnnotation NamedQueriesAnnotation = null;
                PsiAnnotation NamedNativeQueryAnnotation = null;
                PsiAnnotation NamedNativeQueriesAnnotation = null;

                for (PsiAnnotation annotation : allAnnotations) {
                    String qualifiedName = annotation.getQualifiedName();
                    if (isMatchedJavaElement(type, qualifiedName, PersistenceConstants.ENTITY)) {
                        EntityAnnotation = annotation;
                    } else if (isMatchedJavaElement(type, qualifiedName, PersistenceConstants.MAPPEDSUPERCLASS)) {
                        MappedSuperclassAnnotation = annotation;
                    } else if (isMatchedJavaElement(type, qualifiedName, PersistenceConstants.NAMEDENTITYGRAPH)) {
                        NamedEntityGraphAnnotation = annotation;
                    } else if (isMatchedJavaElement(type, qualifiedName, PersistenceConstants.NAMEDENTITYGRAPHS)) {
                        NamedEntityGraphsAnnotation = annotation;
                    } else if (isMatchedJavaElement(type, qualifiedName, PersistenceConstants.NAMEDQUERY)) {
                        NamedQueryAnnotation = annotation;
                    } else if (isMatchedJavaElement(type, qualifiedName, PersistenceConstants.NAMEDQUERIES)) {
                        NamedQueriesAnnotation = annotation;
                    } else if (isMatchedJavaElement(type, qualifiedName, PersistenceConstants.NAMEDNATIVEQUERY)) {
                        NamedNativeQueryAnnotation = annotation;
                    } else if (isMatchedJavaElement(type, qualifiedName, PersistenceConstants.NAMEDNATIVEQUERIES)) {
                        NamedNativeQueriesAnnotation = annotation;
                    }
                }

                boolean hasEntity = EntityAnnotation != null;
                boolean hasMappedSuperclass = MappedSuperclassAnnotation != null;

                // Validate named JPA annotations are on correct class types
                addNamedJPAAnnotationDiagnosticIfNeeded(NamedEntityGraphAnnotation, !hasEntity,
                        PersistenceConstants.NAMEDENTITYGRAPH, "NamedEntityGraphOnNonEntityClass",
                        PersistenceConstants.DIAGNOSTIC_CODE_NAMED_ENTITY_GRAPH_ON_NON_ENTITY, unit, diagnostics);

                addNamedJPAAnnotationDiagnosticIfNeeded(NamedEntityGraphsAnnotation, !hasEntity,
                        PersistenceConstants.NAMEDENTITYGRAPHS, "NamedEntityGraphsOnNonEntityClass",
                        PersistenceConstants.DIAGNOSTIC_CODE_NAMED_ENTITY_GRAPHS_ON_NON_ENTITY, unit, diagnostics);

                addNamedJPAAnnotationDiagnosticIfNeeded(NamedQueryAnnotation, !hasEntity && !hasMappedSuperclass,
                        PersistenceConstants.NAMEDQUERY, "NamedQueryOnInvalidClass",
                        PersistenceConstants.DIAGNOSTIC_CODE_NAMED_QUERY_ON_INVALID_CLASS, unit, diagnostics);

                addNamedJPAAnnotationDiagnosticIfNeeded(NamedQueriesAnnotation, !hasEntity && !hasMappedSuperclass,
                        PersistenceConstants.NAMEDQUERIES, "NamedQueriesOnInvalidClass",
                        PersistenceConstants.DIAGNOSTIC_CODE_NAMED_QUERIES_ON_INVALID_CLASS, unit, diagnostics);

                addNamedJPAAnnotationDiagnosticIfNeeded(NamedNativeQueryAnnotation, !hasEntity && !hasMappedSuperclass,
                        PersistenceConstants.NAMEDNATIVEQUERY, "NamedNativeQueryOnInvalidClass",
                        PersistenceConstants.DIAGNOSTIC_CODE_NAMED_NATIVE_QUERY_ON_INVALID_CLASS, unit, diagnostics);

                addNamedJPAAnnotationDiagnosticIfNeeded(NamedNativeQueriesAnnotation, !hasEntity && !hasMappedSuperclass,
                        PersistenceConstants.NAMEDNATIVEQUERIES, "NamedNativeQueriesOnInvalidClass",
                        PersistenceConstants.DIAGNOSTIC_CODE_NAMED_NATIVE_QUERIES_ON_INVALID_CLASS, unit, diagnostics);

                if (EntityAnnotation != null) {
                    // Define boolean requirements for the diagnostics
                    boolean hasPublicOrProtectedNoArgConstructor = false;
                    boolean hasArgConstructor = false;
                    boolean isEntityClassFinal = false;
                    boolean hasPrimaryKey = false;
                    List<PsiJvmModifiersOwner> versionAnnotatedElements = new ArrayList<>();

                    // Get the Methods of the annotated Class
                    for (PsiMethod method : type.getMethods()) {
                        // find @Version annotation usage on methods
                        if (isMatchedAnnotation(method.getAnnotations(), PersistenceConstants.VERSION)) {
                            versionAnnotatedElements.add(method);
                            // Validate @Version method return type
                            validateVersionType(method, unit, diagnostics);
                        }

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
                        // find @Version annotation usage on fields
                        if (isMatchedAnnotation(field.getAnnotations(), PersistenceConstants.VERSION)) {
                            versionAnnotatedElements.add(field);
                            // Validate @Version field type
                            validateVersionType(field, unit, diagnostics);
                        }

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

                    // Validate @Version annotation usage
                    if(!versionAnnotatedElements.isEmpty()){
                        validateVersionAnnotation(versionAnnotatedElements,type, unit, diagnostics);
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
     * Adds a diagnostic for a named JPA annotation if the given condition is true.
     *
     * @param annotation     the named JPA annotation (e.g. @NamedEntityGraph), or null if not present
     * @param condition      true if the diagnostic should be added (i.e. class does not meet requirements)
     * @param annotationFQN  the fully qualified annotation name, used as diagnostic data
     * @param messageKey     the message key for the diagnostic message
     * @param diagnosticCode the diagnostic code
     * @param unit           the compilation unit
     * @param diagnostics    the list to add the diagnostic to
     */
    private void addNamedJPAAnnotationDiagnosticIfNeeded(PsiAnnotation annotation, boolean condition,
                                                         String annotationFQN, String messageKey,
                                                         String diagnosticCode, PsiJavaFile unit,
                                                         List<Diagnostic> diagnostics) {
        if (annotation != null && condition) {
            JsonArray diagnosticsData = new JsonArray();
            diagnosticsData.add(annotationFQN);
            diagnostics.add(createDiagnostic(annotation, unit,
                    Messages.getMessage(messageKey),
                    diagnosticCode, diagnosticsData, DiagnosticSeverity.Error));
        }
    }

    /**
     * Validates @Version annotation usage in entity classes.
     * Checks for:
     * 1. Multiple @Version annotations within the same entity class
     * 2. @Version annotations in both parent and child entity classes
     *
     * @param versionAnnotatedElements list of elements with @version annotation
     * @param type        the entity class to validate
     * @param unit        compilation unit of Java class
     * @param diagnostics list to add diagnostics to
     */
    private void validateVersionAnnotation(List<PsiJvmModifiersOwner> versionAnnotatedElements,PsiClass type, PsiJavaFile unit, List<Diagnostic> diagnostics) {

        // Check for duplicate @Version annotations within the same class
        if (versionAnnotatedElements.size() > 1) {
            createVersionAnnotationDiagnostics(unit, versionAnnotatedElements, diagnostics, "DuplicateVersionAnnotation",
                    PersistenceConstants.DIAGNOSTIC_CODE_DUPLICATE_VERSION);
        }

        // Check for @Version annotations in the inheritance hierarchy
        if (!versionAnnotatedElements.isEmpty() && hasVersionInParentEntity(type)) {
            createVersionAnnotationDiagnostics(unit, versionAnnotatedElements, diagnostics, "VersionAnnotationInHierarchy",
                    PersistenceConstants.DIAGNOSTIC_CODE_VERSION_IN_HIERARCHY);

        }
    }

    /**
     * Create diagnostics for @Version annotation validation
     *
     * @param unit
     * @param versionAnnotatedElements
     * @param diagnostics
     * @param messageKey
     * @param errorCode
     */
    private void createVersionAnnotationDiagnostics(PsiJavaFile unit, List<PsiJvmModifiersOwner> versionAnnotatedElements,
                                                    List<Diagnostic> diagnostics, String messageKey, String errorCode) {

        for (PsiJvmModifiersOwner element : versionAnnotatedElements) {
            diagnostics.add(createDiagnostic(element, unit,
                    Messages.getMessage(messageKey),
                    errorCode, null,
                    DiagnosticSeverity.Error));
        }
    }

    /**
     * Check @Version annotation exist in super classes
     *
     * @param type
     * @return
     */
    private boolean hasVersionInParentEntity(PsiClass type) {
        // Get all superclasses recursively
        Set<PsiClass> hierarchy = new LinkedHashSet<>(PsiClassImplUtil.getAllSuperClassesRecursively(type));
        boolean versionInParent = false;
        for (PsiClass superClass : hierarchy) {
            // Skip Object class or same class
            if (superClass.getQualifiedName() != null &&
                    superClass.getQualifiedName().equals(PersistenceConstants.OBJECT) || type.equals(superClass)) {
                continue;
            }

            // Check if it's an entity class
            boolean isSuperEntity = isMatchedAnnotation(superClass.getAnnotations(), PersistenceConstants.MAPPEDSUPERCLASS);
            if (!isSuperEntity) {
                continue;
            }


            // Check for @Version in superclass fields
            for (PsiField field : superClass.getFields()) {
                if (isMatchedAnnotation(field.getAnnotations(), PersistenceConstants.VERSION)) {
                    return true;
                }
            }
            // Check for @Version in superclass methods
            for (PsiMethod method : superClass.getMethods()) {
                if (isMatchedAnnotation(method.getAnnotations(), PersistenceConstants.VERSION)) {
                    return true;
                }
            }
        }
        return false;
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

    /**
     * Validates that a field or method annotated with @Version has a supported type.
     * For fields: validates the field type
     * For methods: validates the return type
     * Supported types: int, Integer, short, Short, long, Long, java.sql.Timestamp
     *
     * @param element     the field or method annotated with @Version
     * @param unit        compilation unit of Java class
     * @param diagnostics list to add diagnostics to
     */
    private void validateVersionType(PsiJvmModifiersOwner element, PsiJavaFile unit, List<Diagnostic> diagnostics) {
        PsiType elementType = null;
        
        // Get the type based on whether it's a field or method
        if (element instanceof PsiField) {
            elementType = ((PsiField) element).getType();
        } else if (element instanceof PsiMethod) {
            elementType = ((PsiMethod) element).getReturnType();
        }
        
        // If we couldn't determine the type, skip validation
        if (elementType == null) {
            return;
        }
        
        String typeName = elementType.getCanonicalText();

        // Check if type is in the list of supported types
        boolean isValidType = PersistenceConstants.SET_OF_VALID_VERSION_TYPES.contains(typeName);

        if (!isValidType) {
            diagnostics.add(createDiagnostic(element, unit,
                    Messages.getMessage("InvalidVersionFieldOrPropertyType"),
                    PersistenceConstants.DIAGNOSTIC_CODE_INVALID_VERSION_TYPE, null,
                    DiagnosticSeverity.Error));
        }
    }
}
