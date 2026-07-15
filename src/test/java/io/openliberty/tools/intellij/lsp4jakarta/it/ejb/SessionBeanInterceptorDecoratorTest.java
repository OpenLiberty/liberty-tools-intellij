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

package io.openliberty.tools.intellij.lsp4jakarta.it.ejb;

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
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;

import static io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert.*;

/**
 * Tests for session beans annotated with @Interceptor or @Decorator.
 * Each test case targets a dedicated single-class source file under
 * src/main/java/io/openliberty/sample/jakarta/ejb/interceptordecorator/.
 */
@RunWith(JUnit4.class)
public class SessionBeanInterceptorDecoratorTest extends BaseJakartaTest {

    private static final String BASE_PATH =
            "/src/main/java/io/openliberty/sample/jakarta/ejb/interceptordecorator/";

    // ---------------------------------------------------------------------------
    // Test case 1: @Stateless + @Interceptor -> InvalidStatelessWithInterceptor
    // ---------------------------------------------------------------------------
    @Test
    public void testInvalidStatelessWithInterceptor() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + BASE_PATH + "InvalidStatelessWithInterceptor.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // class decl is line 9 (1-based) = index 8 (0-based)
        // "class InvalidStatelessWithInterceptor {" -> name cols [6, 37)
        Diagnostic d = d(8, 6, 37,
                "Session beans must not be annotated with @Interceptor or @Decorator.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidSessionBeanWithInterceptorOrDecorator");

        assertJavaDiagnostics(diagnosticsParams, utils, d);

        // Quick fix: Remove @Interceptor
        JakartaJavaCodeActionParams caParams = createCodeActionParams(uri, d);

        String removeInterceptor =
                "package io.openliberty.sample.jakarta.ejb.interceptordecorator;\n\n" +
                "import jakarta.ejb.Stateless;\n" +
                "import jakarta.interceptor.Interceptor;\n\n" +
                "// Invalid: @Stateless with @Interceptor\n" +
                "@Stateless\n" +
                "class InvalidStatelessWithInterceptor {\n" +
                "    public void businessMethod() {\n" +
                "    }\n" +
                "}\n";
        TextEdit teRemoveInterceptor = te(0, 0, 12, 0, removeInterceptor);
        CodeAction caRemoveInterceptor = ca(uri, "Remove @Interceptor", d, teRemoveInterceptor);

        String removeStateless =
                "package io.openliberty.sample.jakarta.ejb.interceptordecorator;\n\n" +
                "import jakarta.ejb.Stateless;\n" +
                "import jakarta.interceptor.Interceptor;\n\n" +
                "// Invalid: @Stateless with @Interceptor\n" +
                "@Interceptor\n" +
                "class InvalidStatelessWithInterceptor {\n" +
                "    public void businessMethod() {\n" +
                "    }\n" +
                "}\n";
        TextEdit teRemoveStateless = te(0, 0, 12, 0, removeStateless);
        CodeAction caRemoveStateless = ca(uri, "Remove @Stateless", d, teRemoveStateless);

        assertJavaCodeAction(caParams, utils, caRemoveInterceptor, caRemoveStateless);
    }

    // ---------------------------------------------------------------------------
    // Test case 2: @Stateless + @Decorator -> InvalidStatelessWithDecorator
    // ---------------------------------------------------------------------------
    @Test
    public void testInvalidStatelessWithDecorator() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + BASE_PATH + "InvalidStatelessWithDecorator.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // class decl is line 11 (1-based) = index 10 (0-based)
        // "class InvalidStatelessWithDecorator {" -> name cols [6, 35)
        Diagnostic noInterceptor = d(10, 6, 35,
                "Session beans must not be annotated with @Interceptor or @Decorator.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidSessionBeanWithInterceptorOrDecorator");

        assertJavaDiagnostics(diagnosticsParams, utils, noInterceptor);

        // Quick fix: Remove @Decorator
        JakartaJavaCodeActionParams caParams = createCodeActionParams(uri, noInterceptor);

        String removeDecorator =
                "package io.openliberty.sample.jakarta.ejb.interceptordecorator;\n\n" +
                "import jakarta.ejb.Stateless;\n" +
                "import jakarta.decorator.Decorator;\n" +
                "import jakarta.decorator.Delegate;\n" +
                "import jakarta.inject.Inject;\n\n" +
                "// Invalid: @Stateless with @Decorator\n" +
                "@Stateless\n" +
                "class InvalidStatelessWithDecorator {\n" +
                "    @Inject @Delegate\n" +
                "    private Object delegate;\n\n" +
                "    public void businessMethod() {\n" +
                "    }\n" +
                "}\n";
        TextEdit teRemoveDecorator = te(0, 0, 17, 0, removeDecorator);
        CodeAction caRemoveDecorator = ca(uri, "Remove @Decorator", noInterceptor, teRemoveDecorator);

        String removeStateless =
                "package io.openliberty.sample.jakarta.ejb.interceptordecorator;\n\n" +
                "import jakarta.ejb.Stateless;\n" +
                "import jakarta.decorator.Decorator;\n" +
                "import jakarta.decorator.Delegate;\n" +
                "import jakarta.inject.Inject;\n\n" +
                "// Invalid: @Stateless with @Decorator\n" +
                "@Decorator\n" +
                "class InvalidStatelessWithDecorator {\n" +
                "    @Inject @Delegate\n" +
                "    private Object delegate;\n\n" +
                "    public void businessMethod() {\n" +
                "    }\n" +
                "}\n";
        TextEdit teRemoveStateless = te(0, 0, 17, 0, removeStateless);
        CodeAction caRemoveStateless = ca(uri, "Remove @Stateless", noInterceptor, teRemoveStateless);

        assertJavaCodeAction(caParams, utils, caRemoveDecorator, caRemoveStateless);
    }

    // ---------------------------------------------------------------------------
    // Test case 3: @Stateful + @Interceptor -> InvalidStatefulWithInterceptor
    // ---------------------------------------------------------------------------
    @Test
    public void testInvalidStatefulWithInterceptor() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + BASE_PATH + "InvalidStatefulWithInterceptor.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // class decl is line 9 (1-based) = index 8 (0-based)
        // "class InvalidStatefulWithInterceptor {" -> name cols [6, 36)
        Diagnostic noInterceptorInStateFul = d(8, 6, 36,
                "Session beans must not be annotated with @Interceptor or @Decorator.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidSessionBeanWithInterceptorOrDecorator");

        assertJavaDiagnostics(diagnosticsParams, utils, noInterceptorInStateFul);

        // Quick fix: Remove @Interceptor
        JakartaJavaCodeActionParams caParams = createCodeActionParams(uri, noInterceptorInStateFul);

        String removeInterceptor =
                "package io.openliberty.sample.jakarta.ejb.interceptordecorator;\n\n" +
                "import jakarta.ejb.Stateful;\n" +
                "import jakarta.interceptor.Interceptor;\n\n" +
                "// Invalid: @Stateful with @Interceptor\n" +
                "@Stateful\n" +
                "class InvalidStatefulWithInterceptor {\n" +
                "    public void businessMethod() {\n" +
                "    }\n" +
                "}\n";
        TextEdit teRemoveInterceptor = te(0, 0, 12, 0, removeInterceptor);
        CodeAction caRemoveInterceptor = ca(uri, "Remove @Interceptor", noInterceptorInStateFul, teRemoveInterceptor);

        String removeStateful =
                "package io.openliberty.sample.jakarta.ejb.interceptordecorator;\n\n" +
                "import jakarta.ejb.Stateful;\n" +
                "import jakarta.interceptor.Interceptor;\n\n" +
                "// Invalid: @Stateful with @Interceptor\n" +
                "@Interceptor\n" +
                "class InvalidStatefulWithInterceptor {\n" +
                "    public void businessMethod() {\n" +
                "    }\n" +
                "}\n";
        TextEdit teRemoveStateful = te(0, 0, 12, 0, removeStateful);
        CodeAction caRemoveStateful = ca(uri, "Remove @Stateful", noInterceptorInStateFul, teRemoveStateful);

        assertJavaCodeAction(caParams, utils, caRemoveInterceptor, caRemoveStateful);
    }

    // ---------------------------------------------------------------------------
    // Test case 4: @Stateful + @Decorator -> InvalidStatefulWithDecorator
    // ---------------------------------------------------------------------------
    @Test
    public void testInvalidStatefulWithDecorator() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + BASE_PATH + "InvalidStatefulWithDecorator.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // class decl is line 11 (1-based) = index 10 (0-based)
        // "class InvalidStatefulWithDecorator {" -> name cols [6, 34)
        Diagnostic noDecoratorStateFul = d(10, 6, 34,
                "Session beans must not be annotated with @Interceptor or @Decorator.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidSessionBeanWithInterceptorOrDecorator");

        assertJavaDiagnostics(diagnosticsParams, utils, noDecoratorStateFul);

        // Quick fix: Remove @Decorator
        JakartaJavaCodeActionParams caParams = createCodeActionParams(uri, noDecoratorStateFul);

        String removeDecorator =
                "package io.openliberty.sample.jakarta.ejb.interceptordecorator;\n\n" +
                "import jakarta.ejb.Stateful;\n" +
                "import jakarta.decorator.Decorator;\n" +
                "import jakarta.decorator.Delegate;\n" +
                "import jakarta.inject.Inject;\n\n" +
                "// Invalid: @Stateful with @Decorator\n" +
                "@Stateful\n" +
                "class InvalidStatefulWithDecorator {\n" +
                "    @Inject @Delegate\n" +
                "    private Object delegate;\n\n" +
                "    public void businessMethod() {\n" +
                "    }\n" +
                "}\n";
        TextEdit teRemoveDecorator = te(0, 0, 17, 0, removeDecorator);
        CodeAction caRemoveDecorator = ca(uri, "Remove @Decorator", noDecoratorStateFul, teRemoveDecorator);

        String removeStateful =
                "package io.openliberty.sample.jakarta.ejb.interceptordecorator;\n\n" +
                "import jakarta.ejb.Stateful;\n" +
                "import jakarta.decorator.Decorator;\n" +
                "import jakarta.decorator.Delegate;\n" +
                "import jakarta.inject.Inject;\n\n" +
                "// Invalid: @Stateful with @Decorator\n" +
                "@Decorator\n" +
                "class InvalidStatefulWithDecorator {\n" +
                "    @Inject @Delegate\n" +
                "    private Object delegate;\n\n" +
                "    public void businessMethod() {\n" +
                "    }\n" +
                "}\n";
        TextEdit teRemoveStateful = te(0, 0, 17, 0, removeStateful);
        CodeAction caRemoveStateful = ca(uri, "Remove @Stateful", noDecoratorStateFul, teRemoveStateful);

        assertJavaCodeAction(caParams, utils, caRemoveDecorator, caRemoveStateful);
    }

    // ---------------------------------------------------------------------------
    // Test case 5: @Singleton + @Interceptor -> InvalidSingletonWithInterceptor
    // ---------------------------------------------------------------------------
    @Test
    public void testInvalidSingletonWithInterceptor() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + BASE_PATH + "InvalidSingletonWithInterceptor.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // class decl is line 9 (1-based) = index 8 (0-based)
        // "class InvalidSingletonWithInterceptor {" -> name cols [6, 37)
        Diagnostic interceptorSingleton = d(8, 6, 37,
                "Session beans must not be annotated with @Interceptor or @Decorator.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidSessionBeanWithInterceptorOrDecorator");

        assertJavaDiagnostics(diagnosticsParams, utils, interceptorSingleton);

        // Quick fix: Remove @Interceptor
        JakartaJavaCodeActionParams caParams = createCodeActionParams(uri, interceptorSingleton);

        String removeInterceptor =
                "package io.openliberty.sample.jakarta.ejb.interceptordecorator;\n\n" +
                "import jakarta.ejb.Singleton;\n" +
                "import jakarta.interceptor.Interceptor;\n\n" +
                "// Invalid: @Singleton with @Interceptor\n" +
                "@Singleton\n" +
                "class InvalidSingletonWithInterceptor {\n" +
                "    public void businessMethod() {\n" +
                "    }\n" +
                "}\n";
        TextEdit teRemoveInterceptor = te(0, 0, 12, 0, removeInterceptor);
        CodeAction caRemoveInterceptor = ca(uri, "Remove @Interceptor", interceptorSingleton, teRemoveInterceptor);

        String removeSingleton =
                "package io.openliberty.sample.jakarta.ejb.interceptordecorator;\n\n" +
                "import jakarta.ejb.Singleton;\n" +
                "import jakarta.interceptor.Interceptor;\n\n" +
                "// Invalid: @Singleton with @Interceptor\n" +
                "@Interceptor\n" +
                "class InvalidSingletonWithInterceptor {\n" +
                "    public void businessMethod() {\n" +
                "    }\n" +
                "}\n";
        TextEdit teRemoveSingleton = te(0, 0, 12, 0, removeSingleton);
        CodeAction caRemoveSingleton = ca(uri, "Remove @Singleton", interceptorSingleton, teRemoveSingleton);

        assertJavaCodeAction(caParams, utils, caRemoveInterceptor, caRemoveSingleton);
    }

    // ---------------------------------------------------------------------------
    // Test case 6: @Singleton + @Decorator -> InvalidSingletonWithDecorator
    // ---------------------------------------------------------------------------
    @Test
    public void testInvalidSingletonWithDecorator() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + BASE_PATH + "InvalidSingletonWithDecorator.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // class decl is line 11 (1-based) = index 10 (0-based)
        // "class InvalidSingletonWithDecorator {" -> name cols [6, 35)
        Diagnostic decoratorSingleton = d(10, 6, 35,
                "Session beans must not be annotated with @Interceptor or @Decorator.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidSessionBeanWithInterceptorOrDecorator");

        assertJavaDiagnostics(diagnosticsParams, utils, decoratorSingleton);

        // Quick fix: Remove @Decorator
        JakartaJavaCodeActionParams caParams = createCodeActionParams(uri, decoratorSingleton);

        String removeDecorator =
                "package io.openliberty.sample.jakarta.ejb.interceptordecorator;\n\n" +
                "import jakarta.ejb.Singleton;\n" +
                "import jakarta.decorator.Decorator;\n" +
                "import jakarta.decorator.Delegate;\n" +
                "import jakarta.inject.Inject;\n\n" +
                "// Invalid: @Singleton with @Decorator\n" +
                "@Singleton\n" +
                "class InvalidSingletonWithDecorator {\n" +
                "    @Inject @Delegate\n" +
                "    private Object delegate;\n\n" +
                "    public void businessMethod() {\n" +
                "    }\n" +
                "}\n";
        TextEdit teRemoveDecorator = te(0, 0, 17, 0, removeDecorator);
        CodeAction caRemoveDecorator = ca(uri, "Remove @Decorator", decoratorSingleton, teRemoveDecorator);

        String removeSingleton =
                "package io.openliberty.sample.jakarta.ejb.interceptordecorator;\n\n" +
                "import jakarta.ejb.Singleton;\n" +
                "import jakarta.decorator.Decorator;\n" +
                "import jakarta.decorator.Delegate;\n" +
                "import jakarta.inject.Inject;\n\n" +
                "// Invalid: @Singleton with @Decorator\n" +
                "@Decorator\n" +
                "class InvalidSingletonWithDecorator {\n" +
                "    @Inject @Delegate\n" +
                "    private Object delegate;\n\n" +
                "    public void businessMethod() {\n" +
                "    }\n" +
                "}\n";
        TextEdit teRemoveSingleton = te(0, 0, 17, 0, removeSingleton);
        CodeAction caRemoveSingleton = ca(uri, "Remove @Singleton", decoratorSingleton, teRemoveSingleton);

        assertJavaCodeAction(caParams, utils, caRemoveDecorator, caRemoveSingleton);
    }

    // ---------------------------------------------------------------------------
    // Test case 7: Valid @Stateless only -> ValidSessionBean (no diagnostics)
    // ---------------------------------------------------------------------------
    @Test
    public void testValidSessionBean() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + BASE_PATH + "ValidSessionBean.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }
}
