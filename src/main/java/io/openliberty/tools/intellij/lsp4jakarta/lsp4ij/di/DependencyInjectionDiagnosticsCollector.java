/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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
import java.util.List;
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
            for (PsiField field : allFields) {
                if (field.hasModifierProperty(PsiModifier.FINAL)
                        && containsAnnotation(type, field.getAnnotations(), INJECT_FQ_NAME)) {
                    String msg = Messages.getMessage("InjectNoFinalField");
                    diagnostics.add(createDiagnostic(field, unit, msg,
                            DIAGNOSTIC_CODE_INJECT_FINAL, field.getType().getInternalCanonicalText(),
                            DiagnosticSeverity.Error));
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
        }
    }

    private boolean containsAnnotation(PsiClass type, PsiAnnotation[] annotations, String annotationFQName) {
        return Stream.of(annotations).anyMatch(annotation -> {
                return isMatchedJavaElement(type, annotation.getQualifiedName(), annotationFQName);
        });
    }
}
