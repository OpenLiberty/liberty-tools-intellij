/*******************************************************************************
 * Copyright (c) 2020, 2026 IBM Corporation, Reza Akhavan and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation, Reza Akhavan - initial API and implementation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.beanvalidation;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.DiagnosticsUtils.inheritsFrom;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils.getSimpleName;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.beanvalidation.BeanValidationConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BeanValidationDiagnosticsCollector extends AbstractDiagnosticsCollector {

    private static final Logger LOGGER = Logger.getLogger(BeanValidationDiagnosticsCollector.class.getName());
    public BeanValidationDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return DIAGNOSTIC_SOURCE;
    }

    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit != null) {
            PsiClass[] alltypes;
            PsiField[] allFields;
            PsiMethod[] allMethods;

            alltypes = unit.getClasses();
            for (PsiClass type : alltypes) {
                allFields = type.getFields();
                for (PsiField field : allFields) {
                    processAnnotations(field, type, diagnostics);
                }
                allMethods = type.getMethods();
                for (PsiMethod method : allMethods) {
                    processAnnotations(method, type, diagnostics);
                    PsiParameter[] parameters = method.getParameterList().getParameters();
                    for (PsiParameter parameter : parameters) {
                        processAnnotations(parameter, type, diagnostics);
                    }
                }
            }
        }
    }

    private void processAnnotations(PsiJvmModifiersOwner psiModifierOwner, PsiClass type, List<Diagnostic> diagnostics) {
        PsiAnnotation[] annotations = psiModifierOwner.getAnnotations();
        
        // Check for conflicting constraints
        checkConflictingConstraints(psiModifierOwner, type, annotations, diagnostics);
        
        for (PsiAnnotation annotation : annotations) {
            String matchedAnnotation = getMatchedJavaElementName(type, annotation.getQualifiedName(),
                    SET_OF_ANNOTATIONS.toArray(new String[0]));
            if (matchedAnnotation != null) {
                validAnnotation(psiModifierOwner, annotation, matchedAnnotation, diagnostics, type);
            }
        }
    }

    private void validAnnotation(PsiElement element, PsiAnnotation annotation, String matchedAnnotation,
                                 List<Diagnostic> diagnostics, PsiClass classType) {
        if (element != null) {
            String annotationName = annotation.getQualifiedName();
            boolean isMethod = element instanceof PsiMethod;
            boolean isField = element instanceof PsiField;

            if (((PsiModifierListOwner)element).hasModifierProperty(PsiModifier.STATIC)) {
                String source = isMethod ?
                        Messages.getMessage("ConstraintAnnotationsMethod") :
                        Messages.getMessage("ConstraintAnnotationsField");
                diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                        source, DIAGNOSTIC_CODE_STATIC,
                        annotationName, DiagnosticSeverity.Error));
            }
            PsiType type = (isMethod) ? ((PsiMethod) element).getReturnType() : (isField) ?
                    ((PsiField) element).getType() : ((PsiParameter) element).getType();
            if (type instanceof PsiClassType) {
                PsiType t = PsiPrimitiveType.getUnboxedType(type);
                if (t != null) {
                    type = t;
                }
            }
            //The below block throws diagnostics if invalid element type is used with constraint annotations
            switch (matchedAnnotation) {
                case ASSERT_FALSE, ASSERT_TRUE -> {
                    String source = getSource(isMethod, isField, annotationName, "AnnotationBoolean");
                    if (!type.equals(PsiTypes.booleanType())) {
                        diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                                source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                    }
                }
                case DECIMAL_MAX, DECIMAL_MIN, DIGITS -> {
                    if (!type.getCanonicalText().equals(BIG_DECIMAL)
                            && !type.getCanonicalText().equals(BIG_INTEGER)
                            && !type.getCanonicalText().equals(CHAR_SEQUENCE)
                            && !type.equals(PsiTypes.byteType())
                            && !type.equals(PsiTypes.shortType())
                            && !type.equals(PsiTypes.intType())
                            && !type.equals(PsiTypes.longType())) {
                        String source = getSource(isMethod, isField, annotationName, "AnnotationBigDecimal");
                        diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(), source,
                                DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                    }
                }
                case EMAIL, PATTERN, NOT_BLANK -> checkStringOnly(element, diagnostics, annotationName, isMethod, type, isField);
                case FUTURE, FUTURE_OR_PRESENT, PAST, PAST_OR_PRESENT -> {
                    String dataType = type.getCanonicalText();
                    String dataTypeFQName = getMatchedJavaElementName(classType, dataType,
                            SET_OF_DATE_TYPES.toArray(new String[0]));
                    if (dataTypeFQName == null) {
                        String source = getSource(isMethod, isField, annotationName, "AnnotationDate");
                        diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                                source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                    }
                }
                case MIN, MAX -> {
                    if (!type.getCanonicalText().equals(BIG_DECIMAL)
                            && !type.getCanonicalText().equals(BIG_INTEGER)
                            && !type.equals(PsiTypes.byteType())
                            && !type.equals(PsiTypes.shortType())
                            && !type.equals(PsiTypes.intType())
                            && !type.equals(PsiTypes.longType())) {
                        String source = getSource(isMethod, isField, annotationName, "AnnotationMinMax");
                        diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                                source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                    }
                }
                case NEGATIVE, NEGATIVE_OR_ZERO, POSITIVE, POSITIVE_OR_ZERO -> {
                    if (!type.getCanonicalText().equals(BIG_DECIMAL)
                            && !type.getCanonicalText().equals(BIG_INTEGER)
                            && !type.equals(PsiTypes.byteType())
                            && !type.equals(PsiTypes.shortType())
                            && !type.equals(PsiTypes.intType())
                            && !type.equals(PsiTypes.longType())
                            && !type.equals(PsiTypes.floatType())
                            && !type.equals(PsiTypes.doubleType())) {
                        String source = getSource(isMethod, isField, annotationName, "AnnotationPositive");
                        diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                                source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                    }
                }
                case NOT_EMPTY, SIZE -> {
                    if (!(isSizeOrNonEmptyAllowed(type))) {
                        String source = getSource(isMethod, isField, annotationName, "SizeOrNonEmptyAnnotations");
                        diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                                source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                    }
                }
                default -> LOGGER.log(Level.SEVERE, "Unexpected value for annotation");
            }
        }
    }

    /**
     * getSource message
     * @param isMethod
     * @param isField
     * @param annotationName
     * @param messageKey
     * @return
     */
    private static String getSource(boolean isMethod, boolean isField, String annotationName, String messageKey) {
        return isMethod ?
                Messages.getMessage(messageKey + "Methods", "@" + getSimpleName(annotationName)) : isField ?
                Messages.getMessage(messageKey + "Fields", "@" + getSimpleName(annotationName)) :
                Messages.getMessage(messageKey + "Params", "@" + getSimpleName(annotationName));

    }

    /**
     * isSizeOrNonEmptyAllowed
     * This method checks whether the supported types for the Size and NotEmpty annotations are CharSequence, Collection, Map, or array.
     *
     * @param childType
     * @return
     */
    public static boolean isSizeOrNonEmptyAllowed(PsiType childType) {

        if (childType instanceof PsiArrayType) {
            return true;
        }
        if (childType instanceof PsiPrimitiveType) {
            return false;
        }
        PsiClass resolvedClass = PsiUtil.resolveClassInClassTypeOnly(childType);
        return resolvedClass != null && (inheritsFrom(resolvedClass, CHAR_SEQUENCE)
                || inheritsFrom(resolvedClass, COLLECTION_FQ)
                || inheritsFrom(resolvedClass, MAP_FQ));
    }

    private void checkStringOnly(PsiElement element, List<Diagnostic> diagnostics, String annotationName,
                                 boolean isMethod, PsiType type, boolean isField) {
        if (!type.getCanonicalText().equals(STRING)
                && !type.getCanonicalText().equals(CHAR_SEQUENCE)) {
            String source = getSource(isMethod, isField, annotationName, "AnnotationString");
            diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                    source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
        }
    }

    /**
     * checkConflictingConstraints
     * Check for conflicting constraint annotations (e.g., @Min > @Max, @DecimalMin > @DecimalMax, @Size min > max).
     *
     * @param element     the PSI element (field, method, or parameter)
     * @param type        the declaring class
     * @param annotations the annotations on the element
     * @param diagnostics the list to add diagnostics to
     */
    private void checkConflictingConstraints(PsiJvmModifiersOwner element,
                                             PsiClass type,
                                             PsiAnnotation[] annotations,
                                             List<Diagnostic> diagnostics) {

        PsiAnnotation minAnnotation = null, maxAnnotation = null,
                decMinAnnotation = null, decMaxAnnotation = null,
                sizeAnnotation = null;

        for (PsiAnnotation annotation : annotations) {
            String matched = getMatchedJavaElementName(type, annotation.getQualifiedName(),
                    new String[]{MIN, MAX, DECIMAL_MIN, DECIMAL_MAX, SIZE});
            if (matched != null) {
                switch (matched) {
                    case MIN -> minAnnotation = annotation;
                    case MAX -> maxAnnotation = annotation;
                    case DECIMAL_MIN -> decMinAnnotation = annotation;
                    case DECIMAL_MAX -> decMaxAnnotation = annotation;
                    case SIZE -> sizeAnnotation = annotation;
                }
            }
        }

        // Build constraint checks
        List<ConstraintCheck> checks = new ArrayList<>();
        if (minAnnotation != null && maxAnnotation != null) {
            checks.add(new ConstraintCheck(minAnnotation, maxAnnotation,
                    "value", "value", Long::parseLong,
                    "ConflictingConstraintAnnotationsMinMax"));
        }
        if (decMinAnnotation != null && decMaxAnnotation != null) {
            checks.add(new ConstraintCheck(decMinAnnotation, decMaxAnnotation,
                    "value", "value", Double::parseDouble,
                    "ConflictingConstraintAnnotationsDecimalMinMax"));
        }
        if (sizeAnnotation != null) {
            checks.add(new ConstraintCheck(sizeAnnotation, sizeAnnotation,
                    "min", "max", Integer::parseInt,
                    "ConflictingConstraintAnnotationsSize"));
        }

        // Run all checks
        for (ConstraintCheck check : checks) {
            checkConflict(element, check, diagnostics);
        }
    }

    private void checkConflict(PsiJvmModifiersOwner element,
                               ConstraintCheck check,
                               List<Diagnostic> diagnostics) {

        var minStr = AnnotationUtils.getAnnotationMemberValue(check.minAnnotation(), check.minKey());
        var maxStr = AnnotationUtils.getAnnotationMemberValue(check.maxAnnotation(), check.maxKey());

        if (minStr != null && maxStr != null) {
            try {
                Number min = check.parser().apply(minStr);
                Number max = check.parser().apply(maxStr);
                if (min.doubleValue() > max.doubleValue()) {
                    diagnostics.add(createDiagnostic(
                            element,
                            (PsiJavaFile) element.getContainingFile(),
                            Messages.getMessage(check.messageKey(), minStr, maxStr),
                            DIAGNOSTIC_CODE_CONFLICTING_CONSTRAINTS,
                            null,
                            DiagnosticSeverity.Warning));
                }
            } catch (NumberFormatException e) {
                LOGGER.log(Level.INFO, () -> "Ignore invalid number format for " + check.messageKey());
            }
        }
    }

    private record ConstraintCheck(
            PsiAnnotation minAnnotation,
            PsiAnnotation maxAnnotation,
            String minKey,
            String maxKey,
            Function<String, Number> parser,
            String messageKey) {}


}
