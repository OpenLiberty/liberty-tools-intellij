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
import java.util.Arrays;
import java.util.List;
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
            boolean isCdiScoped = hasCdiScopeAnnotation(type);
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
                    List<PsiAnnotation> qualifiers = getQualifiers(field.getAnnotations(), unit, type);
                    if (qualifiers.size() > 1 && !isCdiScoped) {
                        diagnostics.add(createDiagnostic(field, unit , Messages.getMessage("InjectInvalidQualifiersOnField"),
                                DIAGNOSTIC_CODE_INJECT_INVALID_QUALIFIER, field.getType().getInternalCanonicalText(),
                                DiagnosticSeverity.Error));
                    }
                }
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
                        List<PsiAnnotation> qualifiers = getQualifiers(param.getAnnotations(), unit, type);
                        if (qualifiers.size() > 1 && !isCdiScoped) {
                            if (isConstructorMethod(method))
                                diagnostics.add(createDiagnostic(method, unit , Messages.getMessage("InjectInvalidQualifiersOnField"),
                                        DIAGNOSTIC_CODE_INJECT_INVALID_QUALIFIER, null,
                                        DiagnosticSeverity.Error));
                            else
                                diagnostics.add(createDiagnostic(method, unit , Messages.getMessage("InjectInvalidQualifiersOnField"),
                                        DIAGNOSTIC_CODE_INJECT_INVALID_QUALIFIER, method.getReturnType().getInternalCanonicalText(),
                                        DiagnosticSeverity.Error));
                        }
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
            for(PsiClass innerClass: type.getInnerClasses()){
                for(PsiField innerField: innerClass.getFields()){
                    if (containsAnnotation(type, innerField.getAnnotations(), INJECT_FQ_NAME)) {
                        List<PsiAnnotation> qualifiers = getQualifiers(innerField.getAnnotations(), unit, type);
                        if (qualifiers.size() > 1 && !isCdiScoped) {
                            diagnostics.add(createDiagnostic(innerField, unit , Messages.getMessage("InjectInvalidQualifiersOnField"),
                                    DIAGNOSTIC_CODE_INJECT_INVALID_QUALIFIER, innerField.getType().getInternalCanonicalText(),
                                    DiagnosticSeverity.Error));
                        }
                    }
                }
                for(PsiMethod innerMethod: innerClass.getMethods()){
                    if (containsAnnotation(type, innerMethod.getAnnotations(), INJECT_FQ_NAME)) {
                        for (PsiParameter param : innerMethod.getParameterList().getParameters()) {
                            List<PsiAnnotation> qualifiers = getQualifiers(innerMethod.getAnnotations(), unit, type);
                            if (qualifiers.size() > 1 && !isCdiScoped) {
                                if (isConstructorMethod(innerMethod))
                                    diagnostics.add(createDiagnostic(innerMethod, unit, Messages.getMessage("InjectInvalidQualifiersOnField"),
                                            DIAGNOSTIC_CODE_INJECT_INVALID_QUALIFIER, null,
                                            DiagnosticSeverity.Error));
                                else
                                    diagnostics.add(createDiagnostic(innerMethod, unit , Messages.getMessage("InjectInvalidQualifiersOnField"),
                                            DIAGNOSTIC_CODE_INJECT_INVALID_QUALIFIER, innerMethod.getReturnType().getInternalCanonicalText(),
                                            DiagnosticSeverity.Error));
                            }
                        }
                    }
                }
            }
        }
    }

    private List<PsiAnnotation> getQualifiers(PsiAnnotation [] annotations, PsiJavaFile unit, PsiClass type) {
        return annotations == null ? List.of() : Arrays.stream(annotations).filter(Objects::nonNull).filter(annotation -> DIUtils.isQualifier(annotation, unit, type)
        ).collect(Collectors.toList());
    }

    private Boolean hasCdiScopeAnnotation(PsiClass type) {
        return Arrays.stream(type.getAnnotations()).filter(Objects::nonNull).anyMatch(annotation -> isCdiAnnotation(annotation.getQualifiedName(), type));
    }

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