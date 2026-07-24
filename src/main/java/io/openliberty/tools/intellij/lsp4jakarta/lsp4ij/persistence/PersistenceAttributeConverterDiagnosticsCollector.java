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

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.List;

/** Diagnostics for Jakarta Persistence @Converter annotated classes. */
public class PersistenceAttributeConverterDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public PersistenceAttributeConverterDiagnosticsCollector() {
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
                boolean isConverterAnnotated = isMatchedAnnotation(allAnnotations,
                        PersistenceConstants.CONVERTER);

                String[] interfaces = { PersistenceConstants.ATTRIBUTE_CONVERTER };
                boolean isImplemented = doesImplementInterfaces(type, interfaces);

                if (isConverterAnnotated && !isImplemented) {
                    diagnostics.add(createDiagnostic(type, unit,
                            Messages.getMessage("ConverterMustImplementAttributeConverter"),
                            PersistenceConstants.DIAGNOSTIC_CODE_CONVERTER_MUST_IMPLEMENT, null,
                            DiagnosticSeverity.Error));
                }
            }
        }
    }
}
