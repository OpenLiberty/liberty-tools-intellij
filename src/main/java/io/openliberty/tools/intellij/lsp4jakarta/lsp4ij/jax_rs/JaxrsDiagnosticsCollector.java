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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jax_rs;

import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AnnotationUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.DiagnosticsUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.Arrays;
import java.util.List;

public class JaxrsDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public JaxrsDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return Jax_RSConstants.DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit != null) {
            PsiClass[] alltypes;

            alltypes = unit.getClasses();
            for (PsiClass type : alltypes) {
                if (!DiagnosticsUtils.isClass(type)) {
                    continue;
                }
                boolean isJaxrsClass = false;
                PsiAnnotation[] annotationList = type.getAnnotations();
                for (PsiAnnotation annotation : annotationList) {
                    isJaxrsClass = isMatchedJavaElement(type, annotation.getQualifiedName(), Jax_RSConstants.PATH_ANNOTATION);
                }

                if (isJaxrsClass) {
                    checkInvalidConstraintTarget(type, diagnostics, unit);
                }
            }
        }
    }

    private boolean validateSetterMethod(PsiMethod method) {
        return method.getName().startsWith("set") && PsiTypes.voidType().equals(method.getReturnType())
                && method.getParameterList().getParametersCount() == 1
                && method.getModifierList().hasModifierProperty(PsiModifier.PUBLIC);
    }

    private void checkInvalidConstraintTarget(PsiClass type, List<Diagnostic> diagnostics, PsiJavaFile unit){
        Arrays.stream(type.getMethods())
                .filter(method -> isConstructorMethod(method) || validateSetterMethod(method))
                .flatMap(method -> Arrays.stream(method.getParameterList().getParameters()))
                .flatMap(param -> Arrays.stream(param.getAnnotations()))
                .filter(paramAnnotation -> AnnotationUtil.hasMetaAnnotation(paramAnnotation, type, Jax_RSConstants.CONSTRAINT_ANNOTATION))
                .forEach(paramAnnotation ->
                        diagnostics.add(createDiagnostic(paramAnnotation, unit,
                                Messages.getMessage("InvalidConstraintTarget"),
                                Jax_RSConstants.DIAGNOSTIC_CODE_INVALID_CONSTRAINT_TARGET,
                                null, DiagnosticSeverity.Error))
                );
    }
}
