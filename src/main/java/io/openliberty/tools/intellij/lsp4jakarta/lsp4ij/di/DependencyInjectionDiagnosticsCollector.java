/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation, Himanshu Chotwani - initial API and implementation
 *     Ananya Rao - Diagnostic Collection for multiple constructors annotated with inject
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.di;

import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.di.DependencyInjectionConstants.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * jararta.annotation Diagnostics
 *
 * <li>Diagnostic 1: @Inject fields cannot be final.</li>
 * <li>Diagnostic 2: @Inject methods cannot be final.</li>
 * <li>Diagnostic 3: @Inject methods cannot be abstract.</li>
 * <li>Diagnostic 4: @Inject methods cannot be static.</li>
 * <li>Diagnostic 5: @Inject methods cannot be generic.</li>
 *
 * @see <a href="https://jakarta.ee/specifications/dependency-injection/2.0/jakarta-injection-spec-2.0.html">...</a>
 *
 */

public class DependencyInjectionDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public DependencyInjectionDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit == null)
            return;

        PsiClass[] alltypes;
        alltypes = unit.getClasses();
        for (PsiClass type : alltypes) {
            PsiField[] allFields = type.getFields();
            //Added for checking if the class is CDI scoped and used for both method and field conditions.
            boolean isCdiScoped = hasCdiScopeAnnotation(type);
            String[] implicitQualifiers = IMPLICIT_QUALIFIERS.toArray(String[]::new);
            for (PsiField field : allFields) {
                if (containsAnnotation(type, field.getAnnotations(), INJECT_FQ_NAME)) {
                    if (field.hasModifierProperty(PsiModifier.FINAL)) {
                        String msg = Messages.getMessage("InjectNoFinalField");
                        diagnostics.add(createDiagnostic(field, unit, msg,
                                DIAGNOSTIC_CODE_INJECT_FINAL, field.getType().getInternalCanonicalText(),
                                DiagnosticSeverity.Error));
                    }
                    if (isNonStaticInnerClass(type, field.getType())) {
                        String msg = Messages.getMessage("InjectNonStaticInnerClass");
                        diagnostics.add(createDiagnostic(field, unit, msg,
                                DIAGNOSTIC_CODE_INJECT_INNER_CLASS, field.getType().getInternalCanonicalText(),
                                DiagnosticSeverity.Error));
                    }
                }
                boolean isInject = false;
                Set<String> fqNames = new HashSet<>();
                for(PsiAnnotation annotation: field.getAnnotations()){
                    if(isMatchedJavaElement(type, annotation.getQualifiedName(), INJECT_FQ_NAME)) {
                        isInject = true;
                    }
                    else {
                        getMatchedAnnotationFQ(type, annotation, implicitQualifiers, fqNames);
                    }
                }
                checkInvalidQualifiersForField(unit, diagnostics, type, field, fqNames, isInject, isCdiScoped);
            }

            List<PsiMethod> injectedConstructors = new ArrayList<PsiMethod>();
            PsiMethod[] allMethods = type.getMethods();
            for (PsiMethod method : allMethods) {
                boolean isFinal = method.hasModifierProperty(PsiModifier.FINAL);
                boolean isAbstract = method.hasModifierProperty(PsiModifier.ABSTRACT);
                boolean isStatic = method.hasModifierProperty(PsiModifier.STATIC);
                boolean isGeneric = method.hasTypeParameters();

                if (containsAnnotation(type, method.getAnnotations(), INJECT_FQ_NAME)) {
                    if (isConstructorMethod(method))
                        injectedConstructors.add(method);
                    if (isFinal) {
                        String msg = Messages.getMessage("InjectNoFinalMethod");
                        diagnostics.add(createDiagnostic(method, unit, msg,
                                DIAGNOSTIC_CODE_INJECT_FINAL, method.getReturnType().getInternalCanonicalText(),
                                DiagnosticSeverity.Error));
                    }
                    if (isAbstract) {
                        String msg = Messages.getMessage("InjectNoAbstractMethod");
                        diagnostics.add(createDiagnostic(method, unit, msg,
                                DIAGNOSTIC_CODE_INJECT_ABSTRACT, method.getReturnType().getInternalCanonicalText(),
                                DiagnosticSeverity.Error));
                    }
                    if (isStatic) {
                        String msg = Messages.getMessage("InjectNoStaticMethod");
                        diagnostics.add(createDiagnostic(method, unit, msg,
                                DIAGNOSTIC_CODE_INJECT_STATIC, method.getReturnType().getInternalCanonicalText(),
                                DiagnosticSeverity.Error));
                    }

                    if (isGeneric) {
                        String msg = Messages.getMessage("InjectNoGenericMethod");
                        diagnostics.add(createDiagnostic(method, unit, msg,
                                DIAGNOSTIC_CODE_INJECT_GENERIC, method.getReturnType().getInternalCanonicalText(),
                                DiagnosticSeverity.Error));
                    }
                    for (PsiParameter param : method.getParameterList().getParameters()) {
                        if (isNonStaticInnerClass(type, param.getType())) {
                            String msg = Messages.getMessage("InjectNonStaticInnerClass");
                            diagnostics.add(createDiagnostic(method, unit, msg,
                                    DIAGNOSTIC_CODE_INJECT_INNER_CLASS, method.getReturnType().getInternalCanonicalText(),
                                    DiagnosticSeverity.Error));
                        }
                        checkInvalidQualifierMethodDiagnostics(unit, diagnostics, type, method, param, implicitQualifiers, isCdiScoped, true);
                    }
                }
            }

            // if more than one 'inject' constructor, add diagnostic to all constructors
            if (injectedConstructors.size() > 1) {
                String msg = Messages.getMessage("InjectMoreThanOneConstructor");
                for (PsiMethod m : injectedConstructors) {
                    diagnostics.add(createDiagnostic(m, unit, msg,
                            DIAGNOSTIC_CODE_INJECT_CONSTRUCTOR, null, DiagnosticSeverity.Error));
                }
            }
            //Checking if inner classes have more than one qualifier in fields or method params
            for(PsiClass innerClass: type.getInnerClasses()) {
                for(PsiField innerField: innerClass.getFields()) {
                    boolean isInjectInner = false;
                    Set<String> fqNamesInner = new HashSet<>();
                    for(PsiAnnotation annotation: innerField.getAnnotations()) {
                        if(isMatchedJavaElement(type, annotation.getQualifiedName(), INJECT_FQ_NAME)) {
                            isInjectInner = true;
                            }
                        else {
                                getMatchedAnnotationFQ(type, annotation, implicitQualifiers, fqNamesInner);
                            }
                        }
                    checkInvalidQualifiersForField(unit, diagnostics, type, innerField, fqNamesInner, isInjectInner, isCdiScoped);
                }
                for(PsiMethod innerMethod: innerClass.getMethods()) {
                    if (containsAnnotation(type, innerMethod.getAnnotations(), INJECT_FQ_NAME)) {
                        for (PsiParameter param : innerMethod.getParameterList().getParameters()) {
                            checkInvalidQualifierMethodDiagnostics(unit, diagnostics, type, innerMethod, param, implicitQualifiers, isCdiScoped, false);
                        }
                    }
                }
            }
        }
    }

    /**
     * checkInvalidQualifiersForField
     * Method to generate field level diagnostics for invalid qualifiers
     *
     * @param unit
     * @param diagnostics
     * @param type
     * @param field
     * @param fqNames
     * @param isInject
     * @param isCdiScoped
     */
    private void checkInvalidQualifiersForField(PsiJavaFile unit, List<Diagnostic> diagnostics, PsiClass type, PsiField field, Set<String> fqNames, boolean isInject, boolean isCdiScoped) {
        if (isInject && !isCdiScoped) {
            if (fqNames.equals(IMPLICIT_QUALIFIERS)) {
                return;
            }
            else {
                //Finding and generating invalid inject qualifier diagnostics for fields in parent class
                List<PsiAnnotation> qualifiers = getQualifiers(field.getAnnotations(), unit, type);
                if (qualifiers.size() > 1) {
                    createInvalidInjectQualifierFieldDiagnostics(unit, diagnostics, field);
                }
            }
        }
    }

    /**
     * getMatchedAnnotationFQ
     * Gets Fully qualified annotations matching with implicit qualifiers
     *
     * @param type
     * @param annotation
     * @param implicitQualifiers
     * @param fqNamesInner
     */
    private static void getMatchedAnnotationFQ(PsiClass type, PsiAnnotation annotation, String[] implicitQualifiers, Set<String> fqNamesInner) {
        String matchedAnnotation = getMatchedJavaElementName(type, annotation.getQualifiedName(), implicitQualifiers);
        if(matchedAnnotation !=null) {
            fqNamesInner.add(matchedAnnotation);
        }
    }

    /**
     * checkInvalidQualifierMethodDiagnostics
     * Method checks if invalid qualifier combination is used and throws appropriate diagnostics
     *
     * @param unit
     * @param diagnostics
     * @param type
     * @param method
     * @param param
     * @param implicitQualifiers
     * @param isCdiScoped
     */
    private void checkInvalidQualifierMethodDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics, PsiClass type, PsiMethod method, PsiParameter param, String[] implicitQualifiers, boolean isCdiScoped, boolean isParent) {
        if(!isCdiScoped) {
            String[] paramAnnotations= Arrays.stream(param.getAnnotations())
                    .map(PsiAnnotation::getQualifiedName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet()).toArray(String[]::new);
            Set paramAnnotationsFQ = new HashSet<>(getMatchedJavaElementNames(type, paramAnnotations, implicitQualifiers));

            if (paramAnnotationsFQ.equals(IMPLICIT_QUALIFIERS)) {
                return;
            } else {
                //Finding and generating invalid inject qualifier diagnostics for method parameters
                List<PsiAnnotation> qualifiers;

                if (isParent) {
                    qualifiers = getQualifiers(param.getAnnotations(), unit, type);
                } else {
                    qualifiers = getQualifiers(method.getAnnotations(), unit, type);
                }

                if (qualifiers.size() > 1) {
                    createInvalidInjectQualifierMethodDiagnostics(unit, diagnostics, method);
                }
            }
        }
    }

    /**
     * createInvalidInjectQualifierFieldDiagnostics
     * @description Method to create diagnostics invalid inject qualifiers for fields.
     * @param unit
     * @param diagnostics
     * @param field
     */
    private void createInvalidInjectQualifierFieldDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics, PsiField field) {
        diagnostics.add(createDiagnostic(field, unit, Messages.getMessage("InjectInvalidQualifiersOnField"),
                DIAGNOSTIC_CODE_INJECT_INVALID_QUALIFIER, field.getType().getInternalCanonicalText(),
                DiagnosticSeverity.Error));
    }

    /**
     * createInvalidInjectQualifierMethodDiagnostics
     * @description Method to create diagnostics invalid inject qualifiers for methods.
     * @param unit
     * @param diagnostics
     * @param method
     */
    private void createInvalidInjectQualifierMethodDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics, PsiMethod method) {
        if (isConstructorMethod(method)) {
            diagnostics.add(createDiagnostic(method, unit, Messages.getMessage("InjectInvalidQualifiersOnField"),
                    DIAGNOSTIC_CODE_INJECT_INVALID_QUALIFIER, null,
                    DiagnosticSeverity.Error));
        }
        else {
            diagnostics.add(createDiagnostic(method, unit, Messages.getMessage("InjectInvalidQualifiersOnField"),
                    DIAGNOSTIC_CODE_INJECT_INVALID_QUALIFIER, method.getReturnType().getInternalCanonicalText(),
                    DiagnosticSeverity.Error));

        }
    }
    
    /**
     * getQualifiers
     * @description Method to check if annotations are qualifier annotations or not
     * @param annotations
     * @param unit
     * @param type
     * @return List PsiAnnotation
     */
    private List<PsiAnnotation> getQualifiers(PsiAnnotation [] annotations, PsiJavaFile unit, PsiClass type) {
        return annotations == null ? List.of() : Arrays.stream(annotations).filter(Objects::nonNull).filter(annotation -> DIUtils.isQualifier(annotation, unit, type)
        ).collect(Collectors.toList());
    }

    /**
     * hasCdiScopeAnnotation
     * @description Method to check if annotations of class are CDI scoped or not.
     * @param type
     * @return boolean
     */
    private Boolean hasCdiScopeAnnotation(PsiClass type) {
        return Arrays.stream(type.getAnnotations()).filter(Objects::nonNull).anyMatch(annotation -> isCdiAnnotation(annotation.getQualifiedName(), type));
    }

    /**
     * isCdiAnnotation
     * @description Method to check if annotations of class are CDI scoped or not.
     * @param annotationName
     * @param type
     * @return boolean
     */
    private boolean isCdiAnnotation(String annotationName, PsiClass type) {
        return CDI_ANNOTATIONS_FQ.stream().anyMatch(annotation -> {
            return isMatchedJavaElement(type, annotationName, annotation);
        });
    }

    /**
     * isNonStaticInnerClass
     * This will check whether the parent class contains any nested class that has no static field matching the field’s type.
     *
     * @param outerClass
     * @param injectedType
     * @return
     */
    private boolean isNonStaticInnerClass(PsiClass outerClass, PsiType injectedType) {
        PsiClass injectedClass = injectedType instanceof PsiClassType
                ? ((PsiClassType) injectedType).resolve()
                : null;

        PsiClass parentClass = injectedClass != null
                ? injectedClass.getContainingClass()
                : null;

        return injectedClass != null
                && outerClass.equals(parentClass)
                && !injectedClass.hasModifierProperty(PsiModifier.STATIC);
    }


    private boolean containsAnnotation(PsiClass type, PsiAnnotation[] annotations, String annotationFQName) {
        return Stream.of(annotations).anyMatch(annotation -> {
            return isMatchedJavaElement(type, annotation.getQualifiedName(), annotationFQName);
        });
    }
}