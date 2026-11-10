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

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.faces;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.List;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.faces.FacesConstants.*;

/**
 * {@code @FacesValidator} diagnostic collector.
 */
public class FacesDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public FacesDiagnosticsCollector() {
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

        for (PsiClass type : unit.getClasses()) {
            boolean isFacesValidatorAnnotated = false;
            for (PsiAnnotation annotation : type.getAnnotations()) {
                if (isMatchedJavaElement(type, annotation.getQualifiedName(), FACES_VALIDATOR_FQ_NAME)) {
                    isFacesValidatorAnnotated = true;
                    break;
                }
            }

            if (isFacesValidatorAnnotated) {
                boolean isValidatorImplemented = doesImplementInterfaces(type,
                        new String[] { VALIDATOR_FQ_NAME });
                if (!isValidatorImplemented) {
                    diagnostics.add(createDiagnostic(type, unit,
                            Messages.getMessage("FacesValidatorMustImplement"),
                            DIAGNOSTIC_CODE_FACES_VALIDATOR, null, DiagnosticSeverity.Error));
                }
            }
        }
    }
}
