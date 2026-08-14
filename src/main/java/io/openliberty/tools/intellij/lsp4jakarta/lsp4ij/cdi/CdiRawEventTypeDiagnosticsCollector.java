/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation.
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

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi;

import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.util.PsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.List;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.*;

/**
 * Diagnostics collector for detecting raw {@code Event} injection points in CDI.
 *
 * <p>According to CDI 3.0 specification section 10.2.4 (The built-in Event):
 * "If an injection point of raw type Event is defined, the container automatically
 * detects the problem and treats it as a definition error."
 *
 * <p>This collector checks:
 * <ul>
 *   <li>{@code @Inject} fields of raw type {@code Event} (no type parameter)</li>
 *   <li>Parameters of {@code @Inject} methods of raw type {@code Event}</li>
 * </ul>
 *
 * <p>Valid examples: {@code Event<String>}, {@code Event<OrderCreated>}
 * <p>Invalid example: {@code Event} (raw — no type parameter)
 */
public class CdiRawEventTypeDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public CdiRawEventTypeDiagnosticsCollector() {
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

        // Early exit: if jakarta.enterprise.event.Event is not imported, no raw-Event
        // injection point can exist in this file.
        if (!PsiUtils.isImportedJavaElement(unit, EVENT_FQ_NAME)) {
            return;
        }

        List<PsiClass> allClasses = new ArrayList<>();
        PsiUtils.collectAllClasses(unit.getClasses(), allClasses);

        for (PsiClass type : allClasses) {
            // Check @Inject fields for raw Event type
            for (PsiField field : type.getFields()) {
                if (PsiUtils.isRawEventType(field.getType()) && AnnotationUtils.hasAnnotation(field, INJECT_FQ_NAME)) {
                    diagnostics.add(createDiagnostic(field, unit,
                            Messages.getMessage(DIAGNOSTIC_CODE_RAW_EVENT),
                            DIAGNOSTIC_CODE_RAW_EVENT, null,
                            DiagnosticSeverity.Error));
                }
            }

            // Check parameters of @Inject methods for raw Event type.
            for (PsiMethod method : type.getMethods()) {
                for (PsiParameter param : method.getParameterList().getParameters()) {
                    if (PsiUtils.isRawEventType(param.getType()) && AnnotationUtils.hasAnnotation(method, INJECT_FQ_NAME)) {
                        diagnostics.add(createDiagnostic(method, unit,
                                Messages.getMessage(DIAGNOSTIC_CODE_RAW_EVENT),
                                DIAGNOSTIC_CODE_RAW_EVENT, null,
                                DiagnosticSeverity.Error));
                        // One diagnostic per method is sufficient — the whole @Inject must be removed
                        break;
                    }
                }
            }
        }
    }

}
