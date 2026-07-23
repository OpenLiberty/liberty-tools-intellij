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
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import java.util.Arrays;
import java.util.List;

/**
 * Diagnostics collector that validates the use of
 * {@code @TableGenerator}, {@code @TableGenerators}, {@code @SequenceGenerator},
 * {@code @SequenceGenerators}, {@code @SecondaryTable}, and {@code @SecondaryTables}
 * annotations.
 *
 * <p>{@code @TableGenerator}, {@code @TableGenerators}, {@code @SequenceGenerator},
 * and {@code @SequenceGenerators} may appear on TYPE, METHOD, or FIELD elements.
 * {@code @SecondaryTable} and {@code @SecondaryTables} may only appear on TYPE elements.
 *
 * <p>Validates that:
 * <ul>
 *   <li>{@code @TableGenerator} must specify a non-empty {@code name} attribute</li>
 *   <li>{@code @TableGenerators} must specify at least one {@code @TableGenerator}</li>
 *   <li>{@code @SequenceGenerator} must specify a non-empty {@code name} attribute</li>
 *   <li>{@code @SequenceGenerators} must specify at least one {@code @SequenceGenerator}</li>
 *   <li>{@code @SecondaryTable} must specify a non-empty {@code name} attribute</li>
 *   <li>{@code @SecondaryTables} must specify at least one {@code @SecondaryTable}</li>
 * </ul>
 */
public class PersistenceGeneratorDiagnosticsCollector extends AbstractDiagnosticsCollector {

    /**
     * Creates the persistence generator diagnostics collector.
     */
    public PersistenceGeneratorDiagnosticsCollector() {
        super();
    }

    /**
     * Returns the diagnostic source identifier for persistence diagnostics.
     *
     * @return the persistence diagnostic source
     */
    @Override
    protected String getDiagnosticSource() {
        return PersistenceConstants.DIAGNOSTIC_SOURCE;
    }

    /**
     * Collects diagnostics for supported persistence generator annotations declared on
     * classes in the given Java file.
     *
     * <p>Validation is performed for type-level, field-level, and method-level annotations.
     *
     * @param unit the Java file to inspect
     * @param diagnostics the list to populate with discovered diagnostics
     */
    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit == null) {
            return;
        }
        for (PsiClass type : unit.getClasses()) {
            // TYPE-level: @SecondaryTable/s, @TableGenerator/s, @SequenceGenerator/s
            Arrays.stream(type.getAnnotations()).forEach(annotation -> validateAnnotation(annotation, type, unit, diagnostics));
            // FIELD-level: @TableGenerator/s, @SequenceGenerator/s
            for (PsiField field : type.getFields()) {
                Arrays.stream(field.getAnnotations()).forEach(annotation -> validateAnnotation(annotation, type, unit, diagnostics));
            }
            // METHOD-level: @TableGenerator/s, @SequenceGenerator/s
            for (PsiMethod method : type.getMethods()) {
                Arrays.stream(method.getAnnotations()).forEach(annotation -> validateAnnotation(annotation, type, unit, diagnostics));
            }
        }
    }

    /**
     * Dispatches validation for a single annotation found on a type, field, or method.
     *
     * <p>Singular annotations are validated for a non-empty {@code name} attribute.
     * Container annotations are validated for a non-empty {@code value} array and,
     * when present, each nested annotation is validated for a non-empty {@code name}
     * attribute.
     *
     * @param annotation the annotation to validate
     * @param type the enclosing type used for matching imported or qualified annotation names
     * @param unit the Java file containing the annotation
     * @param diagnostics the list to populate with discovered diagnostics
     */
    private void validateAnnotation(PsiAnnotation annotation, PsiClass type,
                                    PsiJavaFile unit, List<Diagnostic> diagnostics) {
        String qualifiedName = annotation.getQualifiedName();
        if (isMatchedJavaElement(type, qualifiedName, PersistenceConstants.TABLEGENERATOR)) {
            validateNameAttribute(annotation, unit, diagnostics,
                    PersistenceConstants.DIAGNOSTIC_CODE_TABLE_GENERATOR_INVALID_EMPTY_NAME);
        } else if (isMatchedJavaElement(type, qualifiedName, PersistenceConstants.SEQUENCEGENERATOR)) {
            validateNameAttribute(annotation, unit, diagnostics,
                    PersistenceConstants.DIAGNOSTIC_CODE_SEQUENCE_GENERATOR_INVALID_EMPTY_NAME);
        } else if (isMatchedJavaElement(type, qualifiedName, PersistenceConstants.SECONDARYTABLE)) {
            validateNameAttribute(annotation, unit, diagnostics,
                    PersistenceConstants.DIAGNOSTIC_CODE_SECONDARY_TABLE_INVALID_EMPTY_NAME);
        } else if (isMatchedJavaElement(type, qualifiedName, PersistenceConstants.TABLEGENERATORS)) {
            validateNonEmptyMappingArray(annotation, unit, diagnostics,
                    PersistenceConstants.DIAGNOSTIC_CODE_TABLE_GENERATORS_MISSING_MAPPING,
                    PersistenceConstants.DIAGNOSTIC_CODE_TABLE_GENERATOR_INVALID_EMPTY_NAME);
        } else if (isMatchedJavaElement(type, qualifiedName, PersistenceConstants.SEQUENCEGENERATORS)) {
            validateNonEmptyMappingArray(annotation, unit, diagnostics,
                    PersistenceConstants.DIAGNOSTIC_CODE_SEQUENCE_GENERATORS_MISSING_MAPPING,
                    PersistenceConstants.DIAGNOSTIC_CODE_SEQUENCE_GENERATOR_INVALID_EMPTY_NAME);
        } else if (isMatchedJavaElement(type, qualifiedName, PersistenceConstants.SECONDARYTABLES)) {
            validateNonEmptyMappingArray(annotation, unit, diagnostics,
                    PersistenceConstants.DIAGNOSTIC_CODE_SECONDARY_TABLES_MISSING_MAPPING,
                    PersistenceConstants.DIAGNOSTIC_CODE_SECONDARY_TABLE_INVALID_EMPTY_NAME);
        }
    }

    /**
     * Validates that the given annotation declares a non-empty {@code name} attribute.
     *
     * <p>A diagnostic is added when the {@code name} attribute is missing, not a string
     * literal, an empty string literal, or contains only whitespace.
     *
     * @param annotation the annotation whose {@code name} attribute is validated
     * @param unit the Java file containing the annotation
     * @param diagnostics the list to populate with discovered diagnostics
     * @param errorCode the diagnostic code to assign when validation fails; also used
     *                  as the message bundle key for the diagnostic message
     */
    private void validateNameAttribute(PsiAnnotation annotation, PsiJavaFile unit,
                                       List<Diagnostic> diagnostics,
                                       String errorCode) {
        PsiAnnotationMemberValue nameValue = annotation.findAttributeValue(PersistenceConstants.NAME);
        boolean nameIsEmpty = true;
        if (nameValue instanceof PsiLiteralExpression literal) {
            Object val = literal.getValue();
            if (val instanceof String str && !str.isBlank()) {
                nameIsEmpty = false;
            }
        }
        if (nameIsEmpty) {
            diagnostics.add(createDiagnostic(annotation, unit,
                    Messages.getMessage(errorCode),
                    errorCode, null, DiagnosticSeverity.Error));
        }
    }

    /**
     * Validates a container annotation such as {@code @TableGenerators},
     * {@code @SequenceGenerators}, or {@code @SecondaryTables}.
     *
     * <p>If the {@code value} attribute is absent or resolves to an empty array, a
     * container-level diagnostic is added and nested validation stops. Otherwise, each
     * nested annotation is validated for a non-empty {@code name} attribute.
     *
     * @param annotation the container annotation to validate
     * @param unit the Java file containing the annotation
     * @param diagnostics the list to populate with discovered diagnostics
     * @param emptyMappingCode the diagnostic code for a missing or empty container mapping;
     *                         also used as the message bundle key for the diagnostic message
     * @param emptyNameCode the diagnostic code for an invalid nested annotation name;
     *                      also used as the message bundle key for the nested diagnostic message
     */
    private void validateNonEmptyMappingArray(PsiAnnotation annotation, PsiJavaFile unit,
                                              List<Diagnostic> diagnostics,
                                              String emptyMappingCode, String emptyNameCode) {
        PsiAnnotationMemberValue valueAttr = annotation.findAttributeValue("value");
        boolean isEmpty = (valueAttr == null) || (valueAttr instanceof PsiArrayInitializerMemberValue array
                && array.getInitializers().length == 0);
        if (isEmpty) {
            diagnostics.add(createDiagnostic(annotation, unit,
                    Messages.getMessage(emptyMappingCode),
                    emptyMappingCode, null, DiagnosticSeverity.Error));
            return;
        }
        // Iterate nested annotations inside the container's value array
        PsiAnnotationMemberValue[] elements;
        if (valueAttr instanceof PsiArrayInitializerMemberValue array) {
            elements = array.getInitializers();
        } else {
            elements = new PsiAnnotationMemberValue[]{ valueAttr };
        }
        for (PsiAnnotationMemberValue element : elements) {
            if (element instanceof PsiAnnotation nested) {
                validateNameAttribute(nested, unit, diagnostics, emptyNameCode);
            }
        }
    }
}
