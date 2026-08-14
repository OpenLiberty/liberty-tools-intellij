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
import java.util.Arrays;
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

        // Determine once whether jakarta.enterprise.event.Event is imported in this file
        boolean eventImported = isEventImported(unit);
        if (!eventImported) {
            return;
        }

        List<PsiClass> allClasses = new ArrayList<>();
        PsiUtils.collectAllClasses(unit.getClasses(), allClasses);

        for (PsiClass type : allClasses) {
            // Check @Inject fields for raw Event type
            for (PsiField field : type.getFields()) {
                if (isRawEventType(field.getType()) && AnnotationUtils.hasAnnotation(field, INJECT_FQ_NAME)) {
                    diagnostics.add(createDiagnostic(field, unit,
                            Messages.getMessage("InvalidRawEventTypeInjectionPoint"),
                            DIAGNOSTIC_CODE_RAW_EVENT, null,
                            DiagnosticSeverity.Error));
                }
            }

            for (PsiMethod method : type.getMethods()) {
                PsiParameter[] params = method.getParameterList().getParameters();
                boolean hasRawEventParam = false;
                for (PsiParameter param : params) {
                    if (isRawEventType(param.getType())) {
                        hasRawEventParam = true;
                        break;
                    }
                }
                if (hasRawEventParam && AnnotationUtils.hasAnnotation(method, INJECT_FQ_NAME)) {
                    diagnostics.add(createDiagnostic(method, unit,
                            Messages.getMessage("InvalidRawEventTypeInjectionPoint"),
                            DIAGNOSTIC_CODE_RAW_EVENT, null,
                            DiagnosticSeverity.Error));
                }
            }
        }
    }

    /**
     * Returns {@code true} if {@code jakarta.enterprise.event.Event} is imported
     * (explicitly or via on-demand) in the given file.
     *
     * @param unit the Java file to check
     * @return {@code true} if the Event type is imported
     */
    private boolean isEventImported(PsiJavaFile unit) {
        PsiImportList importList = unit.getImportList();
        if (importList == null) {
            return false;
        }
        String eventPackage = EVENT_FQ_NAME.substring(0, EVENT_FQ_NAME.lastIndexOf('.'));
        return Arrays.stream(importList.getImportStatements())
                .anyMatch(stmt -> {
                    String name = stmt.getQualifiedName();
                    if (name == null) return false;
                    return EVENT_FQ_NAME.equals(name) || (stmt.isOnDemand() && eventPackage.equals(name));
                });
    }

    /**
     * Returns {@code true} if the given {@link PsiType} is the raw (unparameterized)
     * {@code Event} type.
     *
     * <p>Called only after confirming that {@code jakarta.enterprise.event.Event} is
     * imported in the file, so a simple name + zero-parameter check is sufficient.
     *
     * @param type the PSI type to check
     * @return {@code true} if the type is a raw {@code Event}; {@code false} otherwise
     */
    private boolean isRawEventType(PsiType type) {
        if (!(type instanceof PsiClassType)) {
            return false;
        }
        PsiClassType classType = (PsiClassType) type;
        // Raw type has no type arguments
        if (classType.getParameterCount() != 0) {
            return false;
        }
        // Import confirmed — check simple name only
        return "Event".equals(classType.getClassName());
    }
}
