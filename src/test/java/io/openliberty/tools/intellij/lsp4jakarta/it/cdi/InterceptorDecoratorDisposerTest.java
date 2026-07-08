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
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;

import static io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert.*;

@RunWith(JUnit4.class)
public class InterceptorDecoratorDisposerTest extends BaseJakartaTest {

    @Test
    public void interceptorWithDisposerMethodTest() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/InterceptorWithDisposerMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test expected diagnostic for interceptor with disposer method
        // Diagnostic for method with @Disposes parameter
        Diagnostic disposesDiagnostic = d(9, 16, 30,
                "Interceptors and Decorators cannot have methods with parameter(s) 'resource' annotated with @Disposes.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecoratorWithDisposerMethod");

        assertJavaDiagnostics(diagnosticsParams, utils, disposesDiagnostic);

        // Test code actions
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, disposesDiagnostic);
        
        String newText = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.enterprise.inject.Disposes;\n\n" +
                "@Interceptor\n" +
                "public class InterceptorWithDisposerMethod {\n\n" +
                "    // This method should trigger a diagnostic - interceptor with @Disposes\n" +
                "    public void disposerMethod(String resource) {\n\n" +
                "    }\n\n" +
                "    // This method should not trigger a diagnostic - no disposer annotation\n" +
                "    public void normalMethod(String param) {\n\n" +
                "    }\n}\n";
        
        TextEdit editRemoveDisposes = te(0, 0, 18, 0, newText);
        CodeAction actionRemoveDisposes = ca(uri, "Remove the @Disposes modifier from parameter resource", disposesDiagnostic, editRemoveDisposes);
        
        assertJavaCodeAction(codeActionParams, utils, actionRemoveDisposes);
    }

    @Test
    public void decoratorWithDisposerMethodTest() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/DecoratorWithDisposerMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test expected diagnostic for decorator with disposer method
        // Diagnostic for method with @Disposes parameter
        Diagnostic disposesDiagnostic = d(9, 16, 30,
                "Interceptors and Decorators cannot have methods with parameter(s) 'resource' annotated with @Disposes.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecoratorWithDisposerMethod");

        assertJavaDiagnostics(diagnosticsParams, utils, disposesDiagnostic);

        // Test code actions
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, disposesDiagnostic);
        
        String newText = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.decorator.Decorator;\n" +
                "import jakarta.enterprise.inject.Disposes;\n\n" +
                "@Decorator\n" +
                "public class DecoratorWithDisposerMethod {\n\n" +
                "    // This method should trigger a diagnostic - decorator with @Disposes\n" +
                "    public void disposerMethod(String resource) {\n\n" +
                "    }\n\n" +
                "    // This method should not trigger a diagnostic - no disposer annotation\n" +
                "    public void normalMethod(String param) {\n\n" +
                "    }\n}\n";
        
        TextEdit editRemoveDisposes = te(0, 0, 18, 0, newText);
        CodeAction actionRemoveDisposes = ca(uri, "Remove the @Disposes modifier from parameter resource", disposesDiagnostic, editRemoveDisposes);
        
        assertJavaCodeAction(codeActionParams, utils, actionRemoveDisposes);
    }
}
