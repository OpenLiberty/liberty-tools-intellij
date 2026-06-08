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

import com.google.gson.JsonArray;
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
 * Tests for CDI Interceptor and Decorator scope validation.
 */
@RunWith(JUnit4.class)
public class InterceptorDecoratorScopesTest extends BaseJakartaTest {

    @Test
    public void interceptorDecoratorScopes() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/InterceptorDecoratorScopes.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Invalid: @Interceptor with @ApplicationScoped
        Diagnostic interceptorWithAppScoped = d(12, 13, 39,
                "Interceptors and decorators must be annotated with the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecorator");

        // Invalid: @Decorator with @RequestScoped
        Diagnostic decoratorWithReqScoped = d(18, 6, 39,
                "Interceptors and decorators must be annotated with the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecorator");

        // Invalid: @Interceptor with @Dependent and @SessionScoped (has multiple scopes)
        JsonArray data = new JsonArray();
        data.add("jakarta.enterprise.context.Dependent");
        data.add("jakarta.enterprise.context.SessionScoped");
        Diagnostic interceptorMultipleScopesDecl = d(37, 6, 42,
                "Scope type annotations must be specified by a managed bean class at most once.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidScopeDecl", data);

        // Invalid: @Interceptor with @Dependent and @SessionScoped (has invalid scope)
        Diagnostic interceptorWithMultipleScopes = d(37, 6, 42,
                "Interceptors and decorators must be annotated with the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecorator");

        assertJavaDiagnostics(diagnosticsParams, utils, interceptorMultipleScopesDecl, interceptorWithMultipleScopes, decoratorWithReqScoped, interceptorWithAppScoped);

        // Test quick fix for @Interceptor with @ApplicationScoped - replace with @Dependent
        JakartaJavaCodeActionParams interceptorAppScopedParams = createCodeActionParams(uri, interceptorWithAppScoped);
        String newText1 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.interceptor.Interceptor;\n" +
                "import jakarta.decorator.Decorator;\nimport jakarta.enterprise.context.RequestScoped;\n" +
                "import jakarta.enterprise.context.SessionScoped;\nimport jakarta.enterprise.context.Dependent;\n\n" +
                "// Invalid: @Interceptor with @ApplicationScoped\n@Dependent\n@Interceptor\npublic class InterceptorDecoratorScopes {\n}\n\n" +
                "// Invalid: @Decorator with @RequestScoped\n@Decorator\n@RequestScoped\nclass InvalidDecoratorWithRequestScoped {\n}\n\n" +
                "// Valid: @Interceptor with @Dependent\n@Interceptor\n@Dependent\nclass ValidInterceptorWithDependent {\n}\n\n" +
                "// Valid: @Decorator with @Dependent\n@Decorator\n@Dependent\nclass ValidDecoratorWithDependent {\n}\n\n" +
                "// Invalid: @Interceptor with @Dependent and @SessionScoped (has invalid scope)\n@Interceptor\n@Dependent\n@SessionScoped\n" +
                "class InvalidInterceptorWithMultipleScopes {\n}\n\n// Valid: @Decorator with no scope annotation (defaults to @Dependent)\n" +
                "@Decorator\nclass ValidDecoratorWithNoScope {\n}\n\n// Valid: @Interceptor with no scope annotation (defaults to @Dependent)\n" +
                "@Interceptor\nclass ValidInterceptorWithNoScope {\n}\n";
        TextEdit interceptorAppScopedEdit = te(0, 0, 49, 0, newText1);
        CodeAction interceptorAppScopedAction = ca(uri, "Replace current scope with @Dependent",
                interceptorWithAppScoped, interceptorAppScopedEdit);
        assertJavaCodeAction(interceptorAppScopedParams, utils, interceptorAppScopedAction);

        // Test quick fix for @Decorator with @RequestScoped - replace with @Dependent
        JakartaJavaCodeActionParams decoratorReqScopedParams = createCodeActionParams(uri, decoratorWithReqScoped);
        String newText2 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.interceptor.Interceptor;\n" +
                "import jakarta.decorator.Decorator;\nimport jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.enterprise.context.SessionScoped;\nimport jakarta.enterprise.context.Dependent;\n\n" +
                "// Invalid: @Interceptor with @ApplicationScoped\n@Interceptor\n@ApplicationScoped\npublic class InterceptorDecoratorScopes {\n}\n\n" +
                "// Invalid: @Decorator with @RequestScoped\n@Dependent\n@Decorator\nclass InvalidDecoratorWithRequestScoped {\n}\n\n" +
                "// Valid: @Interceptor with @Dependent\n@Interceptor\n@Dependent\nclass ValidInterceptorWithDependent {\n}\n\n" +
                "// Valid: @Decorator with @Dependent\n@Decorator\n@Dependent\nclass ValidDecoratorWithDependent {\n}\n\n" +
                "// Invalid: @Interceptor with @Dependent and @SessionScoped (has invalid scope)\n@Interceptor\n@Dependent\n@SessionScoped\n" +
                "class InvalidInterceptorWithMultipleScopes {\n}\n\n// Valid: @Decorator with no scope annotation (defaults to @Dependent)\n" +
                "@Decorator\nclass ValidDecoratorWithNoScope {\n}\n\n// Valid: @Interceptor with no scope annotation (defaults to @Dependent)\n" +
                "@Interceptor\nclass ValidInterceptorWithNoScope {\n}\n";
        TextEdit decoratorReqScopedEdit = te(0, 0, 49, 0, newText2);
        CodeAction decoratorReqScopedAction = ca(uri, "Replace current scope with @Dependent", decoratorWithReqScoped,
                decoratorReqScopedEdit);
        assertJavaCodeAction(decoratorReqScopedParams, utils, decoratorReqScopedAction);

        // Test quick fix for @Interceptor with multiple scopes - replace with @Dependent
        JakartaJavaCodeActionParams interceptorMultiScopesParams = createCodeActionParams(uri,
                interceptorWithMultipleScopes);
        String newText3 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.Dependent;\n" +
                "import jakarta.interceptor.Interceptor;\nimport jakarta.decorator.Decorator;\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\nimport jakarta.enterprise.context.RequestScoped;\n\n" +
                "// Invalid: @Interceptor with @ApplicationScoped\n@Interceptor\n@ApplicationScoped\npublic class InterceptorDecoratorScopes {\n}\n\n" +
                "// Invalid: @Decorator with @RequestScoped\n@Decorator\n@RequestScoped\nclass InvalidDecoratorWithRequestScoped {\n}\n\n" +
                "// Valid: @Interceptor with @Dependent\n@Interceptor\n@Dependent\nclass ValidInterceptorWithDependent {\n}\n\n" +
                "// Valid: @Decorator with @Dependent\n@Decorator\n@Dependent\nclass ValidDecoratorWithDependent {\n}\n\n" +
                "// Invalid: @Interceptor with @Dependent and @SessionScoped (has invalid scope)\n@Dependent\n@Interceptor\n" +
                "class InvalidInterceptorWithMultipleScopes {\n}\n\n// Valid: @Decorator with no scope annotation (defaults to @Dependent)\n" +
                "@Decorator\nclass ValidDecoratorWithNoScope {\n}\n\n// Valid: @Interceptor with no scope annotation (defaults to @Dependent)\n" +
                "@Interceptor\nclass ValidInterceptorWithNoScope {\n}\n";
        TextEdit interceptorMultiScopesEdit = te(0, 0, 49, 0, newText3);
        CodeAction interceptorMultiScopesAction = ca(uri, "Replace current scope with @Dependent",
                interceptorWithMultipleScopes, interceptorMultiScopesEdit);
        assertJavaCodeAction(interceptorMultiScopesParams, utils, interceptorMultiScopesAction);
    }
}
