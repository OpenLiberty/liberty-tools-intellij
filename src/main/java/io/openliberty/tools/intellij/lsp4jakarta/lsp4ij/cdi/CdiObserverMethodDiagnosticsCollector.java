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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.List;

/**
 * CDI diagnostics collector that validates custom {@code ObserverMethod} implementations.
 *
 * <p>Per the CDI 3.0 specification, a class that directly implements the
 * {@code ObserverMethod} interface must override at least one of:
 * <ul>
 * <li>{@code notify(T event)}</li>
 * <li>{@code notify(EventContext&lt;T&gt; eventContext)}</li>
 * </ul>
 * If neither method is overridden, the container cannot invoke the observer logic
 * and must treat the implementation as a definition error.
 *
 * @see <a href="https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0#after_bean_discovery">
 *      CDI 3.0 spec §11.5.4</a>
 */
public class CdiObserverMethodDiagnosticsCollector extends AbstractDiagnosticsCollector {

    /**
     * {@inheritDoc}
     *
     * @return the diagnostic source identifier {@code "jakarta-cdi"}
     */
    @Override
    protected String getDiagnosticSource() {
        return ManagedBeanConstants.DIAGNOSTIC_SOURCE;
    }

    /**
     * Iterates over all top-level classes in the compilation unit and validates
     * each one for the {@code ObserverMethod} notify contract.
     *
     * @param unit        the Java source file to inspect
     * @param diagnostics the list to which any detected diagnostics are appended
     */
    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit == null) {
            return;
        }
        for (PsiClass type : unit.getClasses()) {
            validateObserverMethodImplementation(type, unit, diagnostics);
        }
    }

    /**
     * Validates that a concrete class directly implementing {@code ObserverMethod}
     * declares at least one method named {@code notify}.  If none is found, an
     * {@code InvalidObserverMethodWithoutNotify} diagnostic is appended.
     *
     * @param type        the class to validate
     * @param unit        the enclosing compilation unit
     * @param diagnostics the list to which any detected diagnostic is appended
     */
    private void validateObserverMethodImplementation(PsiClass type, PsiJavaFile unit, List<Diagnostic> diagnostics) {
        // Interfaces, annotation types, and abstract classes may legally defer
        // the notify implementation to concrete subclasses — skip them.
        if (type.isInterface() || type.isAnnotationType() || type.hasModifierProperty(PsiModifier.ABSTRACT)) {
            return;
        }

        if (!implementsObserverMethod(type)) {
            return;
        }

        // Check whether the class declares any method named "notify".
        boolean hasNotifyOverride = false;
        for (PsiMethod method : type.getMethods()) {
            if (ManagedBeanConstants.NOTIFY_METHOD_NAME.equals(method.getName()) && !method.isConstructor()) {
                hasNotifyOverride = true;
                break;
            }
        }

        if (!hasNotifyOverride) {
            String message = Messages.getMessage("InvalidObserverMethodWithoutNotify", type.getName());
            diagnostics.add(createDiagnostic(type, unit, message,
                    ManagedBeanConstants.DIAGNOSTIC_CODE_OBSERVER_METHOD_WITHOUT_NOTIFY,
                    null, DiagnosticSeverity.Error));
        }
    }

    /**
     * Returns {@code true} if the given type's {@code implements} clause contains
     * {@code jakarta.enterprise.inject.spi.ObserverMethod} (raw or parameterised).
     *
     * @param type the class to inspect
     * @return {@code true} if {@code type} directly implements {@code ObserverMethod}
     */
    private boolean implementsObserverMethod(PsiClass type) {
        for (PsiClassType ifaceType : type.getImplementsListTypes()) {
            PsiClass iface = ifaceType.resolve();
            if (iface != null && ManagedBeanConstants.OBSERVER_METHOD_FQ_NAME.equals(iface.getQualifiedName())) {
                return true;
            }
        }
        return false;
    }
}
