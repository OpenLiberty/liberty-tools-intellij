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
 * Tests for CdiDecoratorDiagnosticsCollector that validates decorator delegate injection points.
 *
 * Tests cover two main scenarios:
 * 1. Decorator with no @Delegate injection point (should produce error)
 * 2. Decorator with multiple @Delegate injection points (should produce error on each delegate)
 */
@RunWith(JUnit4.class)
public class DecoratorDiagnosticsTest extends BaseJakartaTest {

    /**
     * Test that a decorator with multiple @Delegate injection points produces diagnostics.
     *
     * According to CDI specification, a decorator must declare exactly one injection point
     * annotated with @Delegate. This test verifies that when multiple @Delegate annotations
     * are present (on a field, constructor parameter, and method parameter), appropriate
     * diagnostics are generated for each delegate injection point.
     *
     * This test covers all three types of injection points:
     * 1. Field injection (@Inject @Delegate on a field)
     * 2. Constructor parameter injection (@Delegate on constructor parameter)
     * 3. Method parameter injection (@Delegate on method parameter)
     */
    @Test
    public void decoratorWithMultipleDelegates() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/DecoratorWithMultipleDelegates.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Expected diagnostics for multiple @Delegate injection points
        // Diagnostic on the field delegate (line 17, "fieldDelegate")
        Diagnostic fieldDelegateDiagnostic = d(16, 28, 41,
                "A decorator must declare exactly one injection point annotated with @Delegate, but found 3.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidDecoratorDelegateInjectionPoints");

        // Diagnostic on the constructor parameter delegate (line 21, "constructorDelegate")
        Diagnostic constructorDelegateDiagnostic = d(20, 68, 87,
                "A decorator must declare exactly one injection point annotated with @Delegate, but found 3.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidDecoratorDelegateInjectionPoints");

        // Diagnostic on the method parameter delegate (line 26, "methodDelegate")
        Diagnostic methodDelegateDiagnostic = d(25, 60, 74,
                "A decorator must declare exactly one injection point annotated with @Delegate, but found 3.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidDecoratorDelegateInjectionPoints");

        assertJavaDiagnostics(diagnosticsParams, utils, fieldDelegateDiagnostic, constructorDelegateDiagnostic, methodDelegateDiagnostic);
    }

    /**
     * Test that a decorator with no @Delegate injection point produces a diagnostic.
     *
     * According to CDI specification, a decorator must declare exactly one injection point
     * annotated with @Delegate. This test verifies that when no @Delegate annotation is
     * present in a decorator class, an appropriate diagnostic is generated on the class
     * declaration itself.
     *
     * Note: This test file also contains @Observes and @ObservesAsync parameters which
     * generate additional diagnostics from a different collector, so we expect multiple
     * diagnostics in total.
     */
    @Test
    public void decoratorWithNoDelegates() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/DecoratorWithObserverMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Expected diagnostic for missing @Delegate injection point (from CdiDecoratorDiagnosticsCollector)
        // Diagnostic on the class declaration (line 8, "DecoratorWithObserverMethod")
        Diagnostic missingDelegateDiagnostic = d(7, 13, 40,
                "A decorator must declare exactly one injection point annotated with @Delegate.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidDecoratorDelegateInjectionPoints");

        // Additional diagnostics from other collectors for @Observes and @ObservesAsync
        // Diagnostic on @Observes parameter (line 14, "observerMethod")
        Diagnostic observesMethodDiagnostic = d(13, 16, 30,
                "Interceptors and Decorators cannot have methods with parameters annotated with @Observes or @ObservesAsync.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecoratorWithObserverMethod");

        // Diagnostic on @ObservesAsync parameter (line 19, "observerAsyncMethod")
        Diagnostic observesAsyncMethodDiagnostic = d(18, 16, 35,
                "Interceptors and Decorators cannot have methods with parameters annotated with @Observes or @ObservesAsync.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecoratorWithObserverMethod");

        assertJavaDiagnostics(diagnosticsParams, utils, missingDelegateDiagnostic, observesMethodDiagnostic, observesAsyncMethodDiagnostic);
    }
}
