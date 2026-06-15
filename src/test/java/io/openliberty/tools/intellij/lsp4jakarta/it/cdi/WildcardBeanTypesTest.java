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

package io.openliberty.tools.intellij.lsp4jakarta.it.cdi;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.lsp4jakarta.it.core.BaseJakartaTest;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;

import static io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert.*;

/**
 * Test class for wildcard type validation in CDI bean types.
 * 
 * Tests that wildcard types (?, ? extends, ? super) are properly detected
 * and reported as errors in:
 * - @Inject fields
 * - @Produces fields
 * - @Produces methods
 */
@RunWith(JUnit4.class)
public class WildcardBeanTypesTest extends BaseJakartaTest {

    @Test
    public void wildcardBeanTypes() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/WildcardBeanTypes.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test expected diagnostics for @Inject fields with wildcard types
        Diagnostic injectWildcard = d(29, 20, 32,
                "Wildcard types are not legal bean types. Injection points must use concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInInjectField");

        Diagnostic injectExtendsWildcard = d(32, 35, 54,
                "Wildcard types are not legal bean types. Injection points must use concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInInjectField");

        Diagnostic injectSuperWildcard = d(35, 34, 51,
                "Wildcard types are not legal bean types. Injection points must use concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInInjectField");

        Diagnostic injectMapWildcard = d(38, 27, 38,
                "Wildcard types are not legal bean types. Injection points must use concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInInjectField");

        // Test expected diagnostics for @Produces fields with wildcard types
        Diagnostic producerFieldWildcard = d(42, 20, 40,
                "Wildcard types are not legal bean types. Producer fields must use concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInProducerField");

        Diagnostic producerFieldExtendsWildcard = d(45, 35, 62,
                "Wildcard types are not legal bean types. Producer fields must use concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInProducerField");

        Diagnostic producerFieldSuperWildcard = d(48, 34, 59,
                "Wildcard types are not legal bean types. Producer fields must use concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInProducerField");

        // Test expected diagnostics for @Produces methods with wildcard return types
        Diagnostic producerMethodWildcard = d(52, 19, 38,
                "Wildcard types are not legal bean types. Producer methods must return concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInProducerMethod");

        Diagnostic producerMethodExtendsWildcard = d(57, 34, 60,
                "Wildcard types are not legal bean types. Producer methods must return concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInProducerMethod");

        Diagnostic producerMethodSuperWildcard = d(62, 33, 57,
                "Wildcard types are not legal bean types. Producer methods must return concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInProducerMethod");

        assertJavaDiagnostics(diagnosticsParams, utils,
                injectWildcard, injectExtendsWildcard, injectSuperWildcard, injectMapWildcard,
                producerFieldWildcard, producerFieldExtendsWildcard, producerFieldSuperWildcard,
                producerMethodWildcard, producerMethodExtendsWildcard, producerMethodSuperWildcard);
    }
}
