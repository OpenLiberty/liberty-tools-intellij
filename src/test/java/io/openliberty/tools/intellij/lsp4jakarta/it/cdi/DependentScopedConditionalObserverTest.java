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
 *     IBM Corporation - initial implementation
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

@RunWith(JUnit4.class)
public class DependentScopedConditionalObserverTest extends BaseJakartaTest {

    @Test
    public void testDependentScopedConditionalObserver() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/DependentScopedConditionalObserver.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test case 1: @Dependent with conditional @Observes (should trigger diagnostic)
        Diagnostic dependentWithConditionalObserves = d(12, 16, 30,
                "Beans with scope @Dependent may not have conditional observer methods. Observer method 'observerMethod' has notifyObserver set to Reception.IF_EXISTS, which is not allowed on a @Dependent scoped bean.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidDependentScopeWithConditionalObserver");

        // Test case 2: @Dependent with conditional @ObservesAsync (should trigger diagnostic)
        Diagnostic dependentWithConditionalObservesAsync = d(21, 16, 30,
                "Beans with scope @Dependent may not have conditional observer methods. Observer method 'observerMethod' has notifyObserver set to Reception.IF_EXISTS, which is not allowed on a @Dependent scoped bean.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidDependentScopeWithConditionalObserver");

        assertJavaDiagnostics(diagnosticsParams, utils, dependentWithConditionalObserves, dependentWithConditionalObservesAsync);
    }
}
