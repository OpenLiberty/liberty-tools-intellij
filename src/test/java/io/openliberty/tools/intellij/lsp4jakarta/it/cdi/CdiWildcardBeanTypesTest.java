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
 * - @Inject method parameters
 * - @Produces fields
 * - @Produces methods
 */
@RunWith(JUnit4.class)
public class CdiWildcardBeanTypesTest extends BaseJakartaTest {

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

        // Test expected diagnostics for nested wildcard types
        Diagnostic nestedMapListWildcard = d(85, 33, 54,
                "Wildcard types are not legal bean types. Injection points must use concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInInjectField");

        Diagnostic nestedMapMapWildcard = d(88, 41, 61,
                "Wildcard types are not legal bean types. Injection points must use concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInInjectField");

        Diagnostic arrayWildcard = d(91, 22, 35,
                "Wildcard types are not legal bean types. Injection points must use concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInInjectField");

        Diagnostic producerNestedMapListWildcard = d(95, 33, 62,
                "Wildcard types are not legal bean types. Producer fields must use concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInProducerField");

        Diagnostic producerNestedMapMapWildcard = d(98, 41, 69,
                "Wildcard types are not legal bean types. Producer fields must use concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInProducerField");

        Diagnostic producerArrayWildcard = d(101, 22, 43,
                "Wildcard types are not legal bean types. Producer fields must use concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInProducerField");

        Diagnostic producerMethodNestedMapListWildcard = d(105, 32, 60,
                "Wildcard types are not legal bean types. Producer methods must return concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInProducerMethod");

        Diagnostic producerMethodNestedMapMapWildcard = d(110, 40, 67,
                "Wildcard types are not legal bean types. Producer methods must return concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInProducerMethod");

        // Test expected diagnostics for @Inject methods with wildcard parameter types
        Diagnostic injectMethodWildcard = d(116, 40, 44,
                "Wildcard types are not legal bean types. Injection method parameters must use concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInInjectField");

        Diagnostic injectMethodExtendsWildcard = d(120, 62, 66,
                "Wildcard types are not legal bean types. Injection method parameters must use concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInInjectField");

        Diagnostic injectMethodSuperWildcard = d(124, 59, 63,
                "Wildcard types are not legal bean types. Injection method parameters must use concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInInjectField");

        Diagnostic injectMethodMapWildcard = d(128, 46, 49,
                "Wildcard types are not legal bean types. Injection method parameters must use concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInInjectField");

        Diagnostic injectMethodNestedWildcard = d(132, 55, 58,
                "Wildcard types are not legal bean types. Injection method parameters must use concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInInjectField");

        Diagnostic injectMethodArrayWildcard = d(136, 43, 48,
                "Wildcard types are not legal bean types. Injection method parameters must use concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInInjectField");

        Diagnostic injectMethodMultiDimArrayWildcard = d(140, 61, 66,
                "Wildcard types are not legal bean types. Injection method parameters must use concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInInjectField");

        // Test for mixed parameters - only wildcard parameter should be flagged
        Diagnostic injectMethodMixedParams = d(154, 67, 79,
                "Wildcard types are not legal bean types. Injection method parameters must use concrete parameterized types without wildcards (?, ? extends, ? super).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidWildcardTypeInInjectField");

        assertJavaDiagnostics(diagnosticsParams, utils,
                injectWildcard, injectExtendsWildcard, injectSuperWildcard, injectMapWildcard,
                producerFieldWildcard, producerFieldExtendsWildcard, producerFieldSuperWildcard,
                producerMethodWildcard, producerMethodExtendsWildcard, producerMethodSuperWildcard,
                nestedMapListWildcard, nestedMapMapWildcard, arrayWildcard,
                producerNestedMapListWildcard, producerNestedMapMapWildcard, producerArrayWildcard,
                producerMethodNestedMapListWildcard, producerMethodNestedMapMapWildcard,
                injectMethodWildcard, injectMethodExtendsWildcard, injectMethodSuperWildcard,
                injectMethodMapWildcard, injectMethodNestedWildcard, injectMethodArrayWildcard,
                injectMethodMultiDimArrayWildcard, injectMethodMixedParams);
    }
}
