/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation, Reza Akhavan and others.
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
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.DiagnosticsUtils.inheritsFrom;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils.getSimpleName;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.beanvalidation.BeanValidationConstants.*;

import java.util.List;

public class BeanValidationDiagnosticsCollector extends AbstractDiagnosticsCollector {

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
            } else {
                PsiType type = (isMethod) ? ((PsiMethod) element).getReturnType() : (isField) ?
                        ((PsiField) element).getType() : ((PsiParameter) element).getType();
                if (type instanceof PsiClassType) {
                    PsiType t = PsiPrimitiveType.getUnboxedType(type);
                    if (t != null) {
                        type = t;
                    }
                }

                if (matchedAnnotation.equals(ASSERT_FALSE) || matchedAnnotation.equals(ASSERT_TRUE)) {
                    String source = getSource(isMethod, isField, annotationName, "AnnotationBoolean");
                    if (!type.equals(PsiTypes.booleanType())) {
                        diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                                source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                    }
                } else if (matchedAnnotation.equals(DECIMAL_MAX) || matchedAnnotation.equals(DECIMAL_MIN)
                        || matchedAnnotation.equals(DIGITS)) {
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
                } else if (matchedAnnotation.equals(EMAIL)) {
                    checkStringOnly(element, diagnostics, annotationName, isMethod, type, isField);
                } else if (matchedAnnotation.equals(NOT_BLANK)) {
                    checkStringOnly(element, diagnostics, annotationName, isMethod, type, isField);
                } else if (matchedAnnotation.equals(PATTERN)) {
                    checkStringOnly(element, diagnostics, annotationName, isMethod, type, isField);
                } else if (matchedAnnotation.equals(FUTURE) || matchedAnnotation.equals(FUTURE_OR_PRESENT)
                        || matchedAnnotation.equals(PAST) || matchedAnnotation.equals(PAST_OR_PRESENT)) {
                    String dataType = type.getCanonicalText();
                    String dataTypeFQName = getMatchedJavaElementName(classType, dataType,
                            SET_OF_DATE_TYPES.toArray(new String[0]));
                    if (dataTypeFQName == null) {
                        String source = getSource(isMethod, isField, annotationName, "AnnotationDate");
                        diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                                source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                    }
                } else if (matchedAnnotation.equals(MIN) || matchedAnnotation.equals(MAX)) {
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
                } else if (matchedAnnotation.equals(NEGATIVE) || matchedAnnotation.equals(NEGATIVE_OR_ZERO)
                        || matchedAnnotation.equals(POSITIVE) || matchedAnnotation.equals(POSITIVE_OR_ZERO)) {
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
                } else if (matchedAnnotation.equals(NOT_EMPTY) || matchedAnnotation.equals(SIZE)) {
                    if (!(isSizeOrNonEmptyAllowed(type))) {
                        String source = getSource(isMethod, isField, annotationName, "SizeOrNonEmptyAnnotations");
                        diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                                source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                    }
                }
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
}
