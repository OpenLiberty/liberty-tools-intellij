/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation, Reza Akhavan and others.
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
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

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
            PsiAnnotation[] annotations;
            PsiMethod[] allMethods;

            alltypes = unit.getClasses();
            for (PsiClass type : alltypes) {
                allFields = type.getFields();
                for (PsiField field : allFields) {
                    annotations = field.getAnnotations();
                    for (PsiAnnotation annotation : annotations) {
                        String matchedAnnotation = getMatchedJavaElementName(type, annotation.getQualifiedName(),
                                SET_OF_ANNOTATIONS.toArray(new String[0]));
                        if (matchedAnnotation != null) {
                            validAnnotation(field, annotation, matchedAnnotation, diagnostics);
                        }
                    }
                }
                allMethods = type.getMethods();
                for (PsiMethod method : allMethods) {
                    annotations = method.getAnnotations();
                    for (PsiAnnotation annotation : annotations) {
                        String matchedAnnotation = getMatchedJavaElementName(type, annotation.getQualifiedName(),
                                SET_OF_ANNOTATIONS.toArray(new String[0]));
                        if (matchedAnnotation != null) {
                            validAnnotation(method, annotation, matchedAnnotation, diagnostics);
                        }
                    }
                }
            }
        }

    }

    private void validAnnotation(PsiElement element, PsiAnnotation annotation, String matchedAnnotation,
                                 List<Diagnostic> diagnostics) {
        if (element != null) {
            String annotationName = annotation.getQualifiedName();
            boolean isMethod = element instanceof PsiMethod;

            if (((PsiModifierListOwner)element).hasModifierProperty(PsiModifier.STATIC)) {
                String source = isMethod ?
                        Messages.getMessage("ConstraintAnnotationsMethod") :
                        Messages.getMessage("ConstraintAnnotationsField");
                diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                        source, DIAGNOSTIC_CODE_STATIC,
                        annotationName, DiagnosticSeverity.Error));
            } else {
                PsiType type = (isMethod) ? ((PsiMethod) element).getReturnType() : ((PsiField) element).getType();
                if (type instanceof PsiClassType) {
                    PsiType t = PsiPrimitiveType.getUnboxedType(type);
                    if (t != null) {
                        type = t;
                    }
                }

                if (matchedAnnotation.equals(ASSERT_FALSE) || matchedAnnotation.equals(ASSERT_TRUE)) {
                    String source = isMethod ?
                            Messages.getMessage("AnnotationBooleanMethods", "@" + getSimpleName(annotationName)) :
                            Messages.getMessage("AnnotationBooleanFields", "@" + getSimpleName(annotationName));
                    if (!type.equals(PsiType.BOOLEAN)) {
                        diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                                source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                    }
                } else if (matchedAnnotation.equals(DECIMAL_MAX) || matchedAnnotation.equals(DECIMAL_MIN)
                        || matchedAnnotation.equals(DIGITS)) {
                    if (!type.getCanonicalText().endsWith(BIG_DECIMAL)
                            && !type.getCanonicalText().endsWith(BIG_INTEGER)
                            && !type.getCanonicalText().endsWith(CHAR_SEQUENCE)
                            && !type.equals(PsiType.BYTE)
                            && !type.equals(PsiType.SHORT)
                            && !type.equals(PsiType.INT)
                            && !type.equals(PsiType.LONG)) {
                        String source = isMethod ?
                                Messages.getMessage("AnnotationBigDecimalMethods", "@" + getSimpleName(annotationName)) :
                                Messages.getMessage("AnnotationBigDecimalFields", "@" + getSimpleName(annotationName));
                        diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(), source,
                                DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                    }
                } else if (matchedAnnotation.equals(EMAIL)) {
                    checkStringOnly(element, diagnostics, annotationName, isMethod, type);
                } else if (matchedAnnotation.equals(NOT_BLANK)) {
                    checkStringOnly(element, diagnostics, annotationName, isMethod, type);
                } else if (matchedAnnotation.equals(PATTERN)) {
                    checkStringOnly(element, diagnostics, annotationName, isMethod, type);
                } else if (matchedAnnotation.equals(FUTURE) || matchedAnnotation.equals(FUTURE_OR_PRESENT)
                        || matchedAnnotation.equals(PAST) || matchedAnnotation.equals(PAST_OR_PRESENT)) {
                    String dataType = type.getCanonicalText();
                    PsiClass containingClass = ((PsiJvmMember) element).getContainingClass(); // class containing the field or method passed in
                    String dataTypeFQName = getMatchedJavaElementName(containingClass, dataType,
                            SET_OF_DATE_TYPES.toArray(new String[0]));
                    if (dataTypeFQName == null) {
                        String source = isMethod ?
                                Messages.getMessage("AnnotationDateMethods", "@" + getSimpleName(annotationName)) :
                                Messages.getMessage("AnnotationDateFields", "@" + getSimpleName(annotationName));
                        diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                                source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                    }
                } else if (matchedAnnotation.equals(MIN) || matchedAnnotation.equals(MAX)) {
                    if (!type.getCanonicalText().endsWith(BIG_DECIMAL)
                            && !type.getCanonicalText().endsWith(BIG_INTEGER)
                            && !type.equals(PsiType.BYTE)
                            && !type.equals(PsiType.SHORT)
                            && !type.equals(PsiType.INT)
                            && !type.equals(PsiType.LONG)) {
                        String source = isMethod ?
                                Messages.getMessage("AnnotationMinMaxMethods", "@" + getSimpleName(annotationName)) :
                                Messages.getMessage("AnnotationMinMaxFields", "@" + getSimpleName(annotationName));
                        diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                                source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                    }
                } else if (matchedAnnotation.equals(NEGATIVE) || matchedAnnotation.equals(NEGATIVE_OR_ZERO)
                        || matchedAnnotation.equals(POSITIVE) || matchedAnnotation.equals(POSITIVE_OR_ZERO)) {
                    if (!type.getCanonicalText().endsWith(BIG_DECIMAL)
                            && !type.getCanonicalText().endsWith(BIG_INTEGER)
                            && !type.equals(PsiType.BYTE)
                            && !type.equals(PsiType.SHORT)
                            && !type.equals(PsiType.INT)
                            && !type.equals(PsiType.LONG)
                            && !type.equals(PsiType.FLOAT)
                            && !type.equals(PsiType.DOUBLE)) {
                        String source = isMethod ?
                                Messages.getMessage("AnnotationPositiveMethods", "@" + getSimpleName(annotationName)) :
                                Messages.getMessage("AnnotationPositiveFields", "@" + getSimpleName(annotationName));
                        diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                                source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                    }
                }

                // These ones contains check on all collection types which requires resolving
                // the String of the type somehow
                // This will also require us to check if the field type was a custom collection
                // subtype which means we
                // have to resolve it and get the super interfaces and check to see if
                // Collection, Map or Array was implemented
                // for that custom type (which could as well be a user made subtype)

//    			else if (annotation.getElementName().equals(NOT_EMPTY) || annotation.getElementName().equals(SIZE)) {
//    				
//    				System.out.println("--Field name: " + Signature.getTypeSignatureKind(fieldType));
//    				System.out.println("--Field name: " + Signature.getParameterTypes(fieldType));			
//    				if (	!fieldType.equals(getSignatureFormatOfType(CHAR_SEQUENCE)) &&
//    						!fieldType.contains("List") &&
//    						!fieldType.contains("Set") &&
//    						!fieldType.contains("Collection") &&
//    						!fieldType.contains("Array") &&
//    						!fieldType.contains("Vector") &&
//    						!fieldType.contains("Stack") &&
//    						!fieldType.contains("Queue") &&
//    						!fieldType.contains("Deque") &&
//    						!fieldType.contains("Map")) {
//    					
//    					diagnostics.add(new Diagnostic(fieldAnnotationrange,
//    							"This annotation can only be used on CharSequence, Collection, Array, "
//    							+ "Map type fields."));	
//    				}
//    			}
            }
        }
    }

    private void checkStringOnly(PsiElement element, List<Diagnostic> diagnostics, String annotationName, boolean isMethod, PsiType type) {
        if (!type.getCanonicalText().endsWith(STRING)
                && !type.getCanonicalText().endsWith(CHAR_SEQUENCE)) {
            String source = isMethod ?
                    Messages.getMessage("AnnotationStringMethods", "@" + getSimpleName(annotationName)) :
                    Messages.getMessage("AnnotationStringFields", "@" + getSimpleName(annotationName));
            diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                    source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
        }
    }
}
