/*******************************************************************************
 * Copyright (c) 2020, 2023 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.java.corrections;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.serviceContainer.BaseKeyedLazyInstance;
import com.intellij.util.xmlb.annotations.Attribute;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.diagnostics.IJavaDiagnosticsParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.diagnostics.JavaDiagnosticsContext;
import org.eclipse.lsp4j.Diagnostic;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wrapper class around {@link IJavaDiagnosticsParticipant} participants.
 */
public final class JavaDiagnosticsDefinition extends BaseKeyedLazyInstance<IJavaDiagnosticsParticipant>
        implements IJavaDiagnosticsParticipant {

    public static final ExtensionPointName<JavaDiagnosticsDefinition> EP_NAME = ExtensionPointName.create("open-liberty.intellij.javaDiagnosticsParticipant");

    private static final Logger LOGGER = Logger.getLogger(JavaDiagnosticsDefinition.class.getName());
    private static final String GROUP_ATTR = "group";
    private static final String IMPLEMENTATION_CLASS_ATTR = "implementationClass";

    @Attribute(GROUP_ATTR)
    private String group;

    @Attribute(IMPLEMENTATION_CLASS_ATTR)
    public String implementationClass;

    @Override
    public boolean isAdaptedForDiagnostics(JavaDiagnosticsContext context) {
        try {
            return getInstance().isAdaptedForDiagnostics(context);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error while calling isAdaptedForDiagnostics", e);
            return false;
        }
    }

    @Override
    public void beginDiagnostics(JavaDiagnosticsContext context) {
        try {
            getInstance().beginDiagnostics(context);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error while calling beginDiagnostics", e);
        }
    }

    @Override
    public List<Diagnostic> collectDiagnostics(JavaDiagnosticsContext context) {
        try {
            List<Diagnostic> diagnostics = getInstance().collectDiagnostics(context);
            return diagnostics != null ? diagnostics : Collections.emptyList();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error while calling collectDiagnostics", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void endDiagnostics(JavaDiagnosticsContext context) {
        try {
            getInstance().endDiagnostics(context);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error while calling endDiagnostics", e);
        }
    }

    public @Nullable String getGroup() {
        return group;
    }

    @Override
    protected @Nullable String getImplementationClassName() {
        return implementationClass;
    }
}
