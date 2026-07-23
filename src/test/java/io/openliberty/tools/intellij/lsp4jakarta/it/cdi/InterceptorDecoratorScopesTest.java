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

        // Line 53: Invalid interceptor with @ApplicationScoped
        Diagnostic interceptorAppScoped = d(53, 6, 38, "Interceptors and decorators must be annotated with the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecorator",
                createJsonArray("jakarta.enterprise.context.ApplicationScoped"));

        // Line 60: Invalid interceptor with @SessionScoped
        Diagnostic interceptorSessionScoped = d(60, 6, 34, "Interceptors and decorators must be annotated with the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecorator",
                createJsonArray("jakarta.enterprise.context.SessionScoped"));

        // Line 68: Invalid interceptor with multiple scopes - InvalidScopeDecl
        Diagnostic interceptorMultiScopeDecl = d(68, 6, 42, "Scope type annotations must be specified by a managed bean class at most once.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidScopeDecl",
                createJsonArray("jakarta.enterprise.context.SessionScoped", "jakarta.enterprise.context.ApplicationScoped"));

        // Line 68: Invalid interceptor with multiple scopes - InvalidInterceptorOrDecorator
        Diagnostic interceptorMultiScope = d(68, 6, 42, "Interceptors and decorators must be annotated with the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecorator",
                createJsonArray("jakarta.enterprise.context.ApplicationScoped", "jakarta.enterprise.context.SessionScoped"));

        // Line 76: Invalid decorator with @ApplicationScoped
        Diagnostic decoratorAppScoped = d(76, 6, 36, "Interceptors and decorators must be annotated with the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecorator",
                createJsonArray("jakarta.enterprise.context.ApplicationScoped"));

        // Line 85: Invalid decorator with @SessionScoped
        Diagnostic decoratorSessionScoped = d(85, 6, 32, "Interceptors and decorators must be annotated with the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecorator",
                createJsonArray("jakarta.enterprise.context.SessionScoped"));

        // Line 95: Invalid decorator with multiple scopes - InvalidScopeDecl
        Diagnostic decoratorMultiScopeDecl = d(95, 6, 40, "Scope type annotations must be specified by a managed bean class at most once.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidScopeDecl",
                createJsonArray("jakarta.enterprise.context.ConversationScoped", "jakarta.enterprise.context.RequestScoped"));

        // Line 95: Invalid decorator with multiple scopes - InvalidInterceptorOrDecorator
        Diagnostic decoratorMultiScope = d(95, 6, 40, "Interceptors and decorators must be annotated with the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecorator",
                createJsonArray("jakarta.enterprise.context.RequestScoped", "jakarta.enterprise.context.ConversationScoped"));

        // Line 107: Invalid interceptor with custom normal scope
        Diagnostic interceptorCustomScope = d(107, 6, 38, "Interceptors and decorators must be annotated with the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecorator",
                createJsonArray("io.openliberty.sample.jakarta.cdi.CustomNormalScope"));

        // Line 113: Invalid decorator with custom normal scope
        Diagnostic decoratorCustomScope = d(113, 6, 36, "Interceptors and decorators must be annotated with the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecorator",
                createJsonArray("io.openliberty.sample.jakarta.cdi.CustomNormalScope"));

        // Line 124: Invalid interceptor with mixed scopes
        Diagnostic interceptorMixedScopes = d(124, 6, 32, "Interceptors and decorators must be annotated with the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecorator",
                createJsonArray("jakarta.enterprise.context.ApplicationScoped", "io.openliberty.sample.jakarta.cdi.CustomNormalScope"));

        // Line 131: Invalid decorator with mixed scopes
        Diagnostic decoratorMixedScopes = d(131, 6, 30, "Interceptors and decorators must be annotated with the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecorator",
                createJsonArray("jakarta.enterprise.context.ApplicationScoped", "io.openliberty.sample.jakarta.cdi.CustomNormalScope"));

        assertJavaDiagnostics(diagnosticsParams, utils, interceptorAppScoped, interceptorSessionScoped,
                interceptorMultiScopeDecl, interceptorMultiScope, decoratorAppScoped, decoratorSessionScoped,
                decoratorMultiScopeDecl, decoratorMultiScope, interceptorCustomScope, decoratorCustomScope,
                interceptorMixedScopes, decoratorMixedScopes);

        // Test quickfix for interceptor with @ApplicationScoped (line 41)
        String newText1 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.decorator.Decorator;\n" +
                "import jakarta.decorator.Delegate;\n" +
                "import jakarta.enterprise.context.SessionScoped;\n" +
                "import jakarta.enterprise.context.RequestScoped;\n" +
                "import jakarta.enterprise.context.ConversationScoped;\n" +
                "import jakarta.enterprise.context.Dependent;\n" +
                "import jakarta.inject.Inject;\n\n" +
                "// ========== Valid Interceptors ==========\n\n" +
                "// Valid interceptor with explicit @Dependent scope\n" +
                "@Interceptor\n" +
                "@Dependent\n" +
                "class ValidInterceptorWithDependent {\n" +
                "}\n\n" +
                "// Valid interceptor with no scope (defaults to @Dependent)\n" +
                "@Interceptor\n" +
                "class ValidInterceptorWithNoScope {\n" +
                "}\n\n" +
                "// ========== Valid Decorators ==========\n\n" +
                "// Valid decorator with explicit @Dependent scope\n" +
                "@Decorator\n" +
                "@Dependent\n" +
                "class ValidDecoratorWithDependent {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Valid decorator with no scope (defaults to @Dependent)\n" +
                "@Decorator\n" +
                "class ValidDecoratorWithNoScope {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// ========== Invalid Interceptors with Built-in Normal Scopes ==========\n\n" +
                "// Invalid interceptor with @ApplicationScoped\n" +
                "@Dependent\n" +
                "@Interceptor\n" +
                "class InterceptorWithApplicationScoped {\n" +
                "}\n\n" +
                "// Invalid interceptor with @SessionScoped\n" +
                "@Interceptor\n" +
                "@SessionScoped\n" +
                "class InterceptorWithSessionScoped {\n" +
                "}\n\n" +
                "// Invalid interceptor with multiple scopes including illegal ones\n" +
                "@Interceptor\n" +
                "@ApplicationScoped\n" +
                "@SessionScoped\n" +
                "class InterceptorWithMultipleIllegalScopes {\n" +
                "}\n\n" +
                "// ========== Invalid Decorators with Built-in Normal Scopes ==========\n\n" +
                "// Invalid decorator with @ApplicationScoped\n" +
                "@Decorator\n" +
                "@ApplicationScoped\n" +
                "class DecoratorWithApplicationScoped {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid decorator with @SessionScoped\n" +
                "@Decorator\n" +
                "@SessionScoped\n" +
                "class DecoratorWithSessionScoped {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid decorator with multiple scopes including illegal ones\n" +
                "@Decorator\n" +
                "@RequestScoped\n" +
                "@ConversationScoped\n" +
                "class DecoratorWithMultipleIllegalScopes {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// ========== Invalid Interceptors/Decorators with Custom Normal Scopes ==========\n\n" +
                "// Invalid interceptor with custom normal scope\n" +
                "@Interceptor\n" +
                "@CustomNormalScope\n" +
                "class InterceptorWithCustomNormalScope {\n" +
                "}\n\n" +
                "// Invalid decorator with custom normal scope\n" +
                "@Decorator\n" +
                "@CustomNormalScope\n" +
                "class DecoratorWithCustomNormalScope {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid interceptor with both built-in and custom normal scopes\n" +
                "@Interceptor\n" +
                "@ApplicationScoped\n" +
                "@CustomNormalScope\n" +
                "class InterceptorWithMixedScopes {\n" +
                "}\n\n" +
                "// Invalid decorator with both built-in and custom normal scopes\n" +
                "@Decorator\n" +
                "@ApplicationScoped\n" +
                "@CustomNormalScope\n" +
                "class DecoratorWithMixedScopes {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n";
        TextEdit replaceAppScopedEdit = te(0, 0, 128, 0, newText1);
        CodeAction replaceAppScopedAction = ca(uri, "Replace @ApplicationScoped with @Dependent", interceptorAppScoped, replaceAppScopedEdit);
        assertJavaCodeAction(createCodeActionParams(uri, interceptorAppScoped), utils, replaceAppScopedAction);

        // Test quickfix for interceptor with multiple scopes (line 54)
        String newText2 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.decorator.Decorator;\n" +
                "import jakarta.decorator.Delegate;\n" +
                "import jakarta.enterprise.context.RequestScoped;\n" +
                "import jakarta.enterprise.context.ConversationScoped;\n" +
                "import jakarta.enterprise.context.Dependent;\n" +
                "import jakarta.inject.Inject;\n\n" +
                "// ========== Valid Interceptors ==========\n\n" +
                "// Valid interceptor with explicit @Dependent scope\n" +
                "@Interceptor\n" +
                "@Dependent\n" +
                "class ValidInterceptorWithDependent {\n" +
                "}\n\n" +
                "// Valid interceptor with no scope (defaults to @Dependent)\n" +
                "@Interceptor\n" +
                "class ValidInterceptorWithNoScope {\n" +
                "}\n\n" +
                "// ========== Valid Decorators ==========\n\n" +
                "// Valid decorator with explicit @Dependent scope\n" +
                "@Decorator\n" +
                "@Dependent\n" +
                "class ValidDecoratorWithDependent {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Valid decorator with no scope (defaults to @Dependent)\n" +
                "@Decorator\n" +
                "class ValidDecoratorWithNoScope {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// ========== Invalid Interceptors with Built-in Normal Scopes ==========\n\n" +
                "// Invalid interceptor with @ApplicationScoped\n" +
                "@Interceptor\n" +
                "@ApplicationScoped\n" +
                "class InterceptorWithApplicationScoped {\n" +
                "}\n\n" +
                "// Invalid interceptor with @SessionScoped\n" +
                "@Interceptor\n" +
                "@SessionScoped\n" +
                "class InterceptorWithSessionScoped {\n" +
                "}\n\n" +
                "// Invalid interceptor with multiple scopes including illegal ones\n" +
                "@Dependent\n" +
                "@Interceptor\n" +
                "class InterceptorWithMultipleIllegalScopes {\n" +
                "}\n\n" +
                "// ========== Invalid Decorators with Built-in Normal Scopes ==========\n\n" +
                "// Invalid decorator with @ApplicationScoped\n" +
                "@Decorator\n" +
                "@ApplicationScoped\n" +
                "class DecoratorWithApplicationScoped {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid decorator with @SessionScoped\n" +
                "@Decorator\n" +
                "@SessionScoped\n" +
                "class DecoratorWithSessionScoped {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid decorator with multiple scopes including illegal ones\n" +
                "@Decorator\n" +
                "@RequestScoped\n" +
                "@ConversationScoped\n" +
                "class DecoratorWithMultipleIllegalScopes {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// ========== Invalid Interceptors/Decorators with Custom Normal Scopes ==========\n\n" +
                "// Invalid interceptor with custom normal scope\n" +
                "@Interceptor\n" +
                "@CustomNormalScope\n" +
                "class InterceptorWithCustomNormalScope {\n" +
                "}\n\n" +
                "// Invalid decorator with custom normal scope\n" +
                "@Decorator\n" +
                "@CustomNormalScope\n" +
                "class DecoratorWithCustomNormalScope {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid interceptor with both built-in and custom normal scopes\n" +
                "@Interceptor\n" +
                "@ApplicationScoped\n" +
                "@CustomNormalScope\n" +
                "class InterceptorWithMixedScopes {\n" +
                "}\n\n" +
                "// Invalid decorator with both built-in and custom normal scopes\n" +
                "@Decorator\n" +
                "@ApplicationScoped\n" +
                "@CustomNormalScope\n" +
                "class DecoratorWithMixedScopes {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n";
        TextEdit replaceMultipleScopesEdit = te(0, 0, 128, 0, newText2);
        CodeAction replaceMultipleScopesAction = ca(uri, "Replace @ApplicationScoped and @SessionScoped with @Dependent", interceptorMultiScope, replaceMultipleScopesEdit);
        assertJavaCodeAction(createCodeActionParams(uri, interceptorMultiScope), utils, replaceMultipleScopesAction);

        // Test quickfix for decorator with @ApplicationScoped (line 62)
        String newText3 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.decorator.Decorator;\n" +
                "import jakarta.decorator.Delegate;\n" +
                "import jakarta.enterprise.context.SessionScoped;\n" +
                "import jakarta.enterprise.context.RequestScoped;\n" +
                "import jakarta.enterprise.context.ConversationScoped;\n" +
                "import jakarta.enterprise.context.Dependent;\n" +
                "import jakarta.inject.Inject;\n\n" +
                "// ========== Valid Interceptors ==========\n\n" +
                "// Valid interceptor with explicit @Dependent scope\n" +
                "@Interceptor\n" +
                "@Dependent\n" +
                "class ValidInterceptorWithDependent {\n" +
                "}\n\n" +
                "// Valid interceptor with no scope (defaults to @Dependent)\n" +
                "@Interceptor\n" +
                "class ValidInterceptorWithNoScope {\n" +
                "}\n\n" +
                "// ========== Valid Decorators ==========\n\n" +
                "// Valid decorator with explicit @Dependent scope\n" +
                "@Decorator\n" +
                "@Dependent\n" +
                "class ValidDecoratorWithDependent {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Valid decorator with no scope (defaults to @Dependent)\n" +
                "@Decorator\n" +
                "class ValidDecoratorWithNoScope {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// ========== Invalid Interceptors with Built-in Normal Scopes ==========\n\n" +
                "// Invalid interceptor with @ApplicationScoped\n" +
                "@Interceptor\n" +
                "@ApplicationScoped\n" +
                "class InterceptorWithApplicationScoped {\n" +
                "}\n\n" +
                "// Invalid interceptor with @SessionScoped\n" +
                "@Interceptor\n" +
                "@SessionScoped\n" +
                "class InterceptorWithSessionScoped {\n" +
                "}\n\n" +
                "// Invalid interceptor with multiple scopes including illegal ones\n" +
                "@Interceptor\n" +
                "@ApplicationScoped\n" +
                "@SessionScoped\n" +
                "class InterceptorWithMultipleIllegalScopes {\n" +
                "}\n\n" +
                "// ========== Invalid Decorators with Built-in Normal Scopes ==========\n\n" +
                "// Invalid decorator with @ApplicationScoped\n" +
                "@Dependent\n" +
                "@Decorator\n" +
                "class DecoratorWithApplicationScoped {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid decorator with @SessionScoped\n" +
                "@Decorator\n" +
                "@SessionScoped\n" +
                "class DecoratorWithSessionScoped {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid decorator with multiple scopes including illegal ones\n" +
                "@Decorator\n" +
                "@RequestScoped\n" +
                "@ConversationScoped\n" +
                "class DecoratorWithMultipleIllegalScopes {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// ========== Invalid Interceptors/Decorators with Custom Normal Scopes ==========\n\n" +
                "// Invalid interceptor with custom normal scope\n" +
                "@Interceptor\n" +
                "@CustomNormalScope\n" +
                "class InterceptorWithCustomNormalScope {\n" +
                "}\n\n" +
                "// Invalid decorator with custom normal scope\n" +
                "@Decorator\n" +
                "@CustomNormalScope\n" +
                "class DecoratorWithCustomNormalScope {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid interceptor with both built-in and custom normal scopes\n" +
                "@Interceptor\n" +
                "@ApplicationScoped\n" +
                "@CustomNormalScope\n" +
                "class InterceptorWithMixedScopes {\n" +
                "}\n\n" +
                "// Invalid decorator with both built-in and custom normal scopes\n" +
                "@Decorator\n" +
                "@ApplicationScoped\n" +
                "@CustomNormalScope\n" +
                "class DecoratorWithMixedScopes {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n";
        TextEdit replaceDecoratorAppScopedEdit = te(0, 0, 128, 0, newText3);
        CodeAction replaceDecoratorAppScopedAction = ca(uri, "Replace @ApplicationScoped with @Dependent", decoratorAppScoped, replaceDecoratorAppScopedEdit);
        assertJavaCodeAction(createCodeActionParams(uri, decoratorAppScoped), utils, replaceDecoratorAppScopedAction);

        // Test quickfix for interceptor with custom scope (line 83)
        String newText4 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.decorator.Decorator;\n" +
                "import jakarta.decorator.Delegate;\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.enterprise.context.SessionScoped;\n" +
                "import jakarta.enterprise.context.RequestScoped;\n" +
                "import jakarta.enterprise.context.ConversationScoped;\n" +
                "import jakarta.enterprise.context.Dependent;\n" +
                "import jakarta.inject.Inject;\n\n" +
                "// ========== Valid Interceptors ==========\n\n" +
                "// Valid interceptor with explicit @Dependent scope\n" +
                "@Interceptor\n" +
                "@Dependent\n" +
                "class ValidInterceptorWithDependent {\n" +
                "}\n\n" +
                "// Valid interceptor with no scope (defaults to @Dependent)\n" +
                "@Interceptor\n" +
                "class ValidInterceptorWithNoScope {\n" +
                "}\n\n" +
                "// ========== Valid Decorators ==========\n\n" +
                "// Valid decorator with explicit @Dependent scope\n" +
                "@Decorator\n" +
                "@Dependent\n" +
                "class ValidDecoratorWithDependent {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Valid decorator with no scope (defaults to @Dependent)\n" +
                "@Decorator\n" +
                "class ValidDecoratorWithNoScope {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// ========== Invalid Interceptors with Built-in Normal Scopes ==========\n\n" +
                "// Invalid interceptor with @ApplicationScoped\n" +
                "@Interceptor\n" +
                "@ApplicationScoped\n" +
                "class InterceptorWithApplicationScoped {\n" +
                "}\n\n" +
                "// Invalid interceptor with @SessionScoped\n" +
                "@Interceptor\n" +
                "@SessionScoped\n" +
                "class InterceptorWithSessionScoped {\n" +
                "}\n\n" +
                "// Invalid interceptor with multiple scopes including illegal ones\n" +
                "@Interceptor\n" +
                "@ApplicationScoped\n" +
                "@SessionScoped\n" +
                "class InterceptorWithMultipleIllegalScopes {\n" +
                "}\n\n" +
                "// ========== Invalid Decorators with Built-in Normal Scopes ==========\n\n" +
                "// Invalid decorator with @ApplicationScoped\n" +
                "@Decorator\n" +
                "@ApplicationScoped\n" +
                "class DecoratorWithApplicationScoped {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid decorator with @SessionScoped\n" +
                "@Decorator\n" +
                "@SessionScoped\n" +
                "class DecoratorWithSessionScoped {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid decorator with multiple scopes including illegal ones\n" +
                "@Decorator\n" +
                "@RequestScoped\n" +
                "@ConversationScoped\n" +
                "class DecoratorWithMultipleIllegalScopes {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// ========== Invalid Interceptors/Decorators with Custom Normal Scopes ==========\n\n" +
                "// Invalid interceptor with custom normal scope\n" +
                "@Dependent\n" +
                "@Interceptor\n" +
                "class InterceptorWithCustomNormalScope {\n" +
                "}\n\n" +
                "// Invalid decorator with custom normal scope\n" +
                "@Decorator\n" +
                "@CustomNormalScope\n" +
                "class DecoratorWithCustomNormalScope {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid interceptor with both built-in and custom normal scopes\n" +
                "@Interceptor\n" +
                "@ApplicationScoped\n" +
                "@CustomNormalScope\n" +
                "class InterceptorWithMixedScopes {\n" +
                "}\n\n" +
                "// Invalid decorator with both built-in and custom normal scopes\n" +
                "@Decorator\n" +
                "@ApplicationScoped\n" +
                "@CustomNormalScope\n" +
                "class DecoratorWithMixedScopes {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n";
        TextEdit replaceCustomScopeEdit = te(0, 0, 128, 0, newText4);
        CodeAction replaceCustomScopeAction = ca(uri, "Replace @CustomNormalScope with @Dependent", interceptorCustomScope, replaceCustomScopeEdit);
        assertJavaCodeAction(createCodeActionParams(uri, interceptorCustomScope), utils, replaceCustomScopeAction);

        // Test quickfix for interceptor with @SessionScoped (line 47)
        String newText5 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.decorator.Decorator;\n" +
                "import jakarta.decorator.Delegate;\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.enterprise.context.RequestScoped;\n" +
                "import jakarta.enterprise.context.ConversationScoped;\n" +
                "import jakarta.enterprise.context.Dependent;\n" +
                "import jakarta.inject.Inject;\n\n" +
                "// ========== Valid Interceptors ==========\n\n" +
                "// Valid interceptor with explicit @Dependent scope\n" +
                "@Interceptor\n" +
                "@Dependent\n" +
                "class ValidInterceptorWithDependent {\n" +
                "}\n\n" +
                "// Valid interceptor with no scope (defaults to @Dependent)\n" +
                "@Interceptor\n" +
                "class ValidInterceptorWithNoScope {\n" +
                "}\n\n" +
                "// ========== Valid Decorators ==========\n\n" +
                "// Valid decorator with explicit @Dependent scope\n" +
                "@Decorator\n" +
                "@Dependent\n" +
                "class ValidDecoratorWithDependent {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Valid decorator with no scope (defaults to @Dependent)\n" +
                "@Decorator\n" +
                "class ValidDecoratorWithNoScope {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// ========== Invalid Interceptors with Built-in Normal Scopes ==========\n\n" +
                "// Invalid interceptor with @ApplicationScoped\n" +
                "@Interceptor\n" +
                "@ApplicationScoped\n" +
                "class InterceptorWithApplicationScoped {\n" +
                "}\n\n" +
                "// Invalid interceptor with @SessionScoped\n" +
                "@Dependent\n" +
                "@Interceptor\n" +
                "class InterceptorWithSessionScoped {\n" +
                "}\n\n" +
                "// Invalid interceptor with multiple scopes including illegal ones\n" +
                "@Interceptor\n" +
                "@ApplicationScoped\n" +
                "@SessionScoped\n" +
                "class InterceptorWithMultipleIllegalScopes {\n" +
                "}\n\n" +
                "// ========== Invalid Decorators with Built-in Normal Scopes ==========\n\n" +
                "// Invalid decorator with @ApplicationScoped\n" +
                "@Decorator\n" +
                "@ApplicationScoped\n" +
                "class DecoratorWithApplicationScoped {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid decorator with @SessionScoped\n" +
                "@Decorator\n" +
                "@SessionScoped\n" +
                "class DecoratorWithSessionScoped {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid decorator with multiple scopes including illegal ones\n" +
                "@Decorator\n" +
                "@RequestScoped\n" +
                "@ConversationScoped\n" +
                "class DecoratorWithMultipleIllegalScopes {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// ========== Invalid Interceptors/Decorators with Custom Normal Scopes ==========\n\n" +
                "// Invalid interceptor with custom normal scope\n" +
                "@Interceptor\n" +
                "@CustomNormalScope\n" +
                "class InterceptorWithCustomNormalScope {\n" +
                "}\n\n" +
                "// Invalid decorator with custom normal scope\n" +
                "@Decorator\n" +
                "@CustomNormalScope\n" +
                "class DecoratorWithCustomNormalScope {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid interceptor with both built-in and custom normal scopes\n" +
                "@Interceptor\n" +
                "@ApplicationScoped\n" +
                "@CustomNormalScope\n" +
                "class InterceptorWithMixedScopes {\n" +
                "}\n\n" +
                "// Invalid decorator with both built-in and custom normal scopes\n" +
                "@Decorator\n" +
                "@ApplicationScoped\n" +
                "@CustomNormalScope\n" +
                "class DecoratorWithMixedScopes {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n";
        TextEdit replaceInterceptorSessionScopedEdit = te(0, 0, 128, 0, newText5);
        CodeAction replaceInterceptorSessionScopedAction = ca(uri, "Replace @SessionScoped with @Dependent", interceptorSessionScoped, replaceInterceptorSessionScopedEdit);
        assertJavaCodeAction(createCodeActionParams(uri, interceptorSessionScoped), utils, replaceInterceptorSessionScopedAction);

        // Test quickfix for decorator with @SessionScoped (line 71)
        String newText6 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.decorator.Decorator;\n" +
                "import jakarta.decorator.Delegate;\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.enterprise.context.RequestScoped;\n" +
                "import jakarta.enterprise.context.ConversationScoped;\n" +
                "import jakarta.enterprise.context.Dependent;\n" +
                "import jakarta.inject.Inject;\n\n" +
                "// ========== Valid Interceptors ==========\n\n" +
                "// Valid interceptor with explicit @Dependent scope\n" +
                "@Interceptor\n" +
                "@Dependent\n" +
                "class ValidInterceptorWithDependent {\n" +
                "}\n\n" +
                "// Valid interceptor with no scope (defaults to @Dependent)\n" +
                "@Interceptor\n" +
                "class ValidInterceptorWithNoScope {\n" +
                "}\n\n" +
                "// ========== Valid Decorators ==========\n\n" +
                "// Valid decorator with explicit @Dependent scope\n" +
                "@Decorator\n" +
                "@Dependent\n" +
                "class ValidDecoratorWithDependent {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Valid decorator with no scope (defaults to @Dependent)\n" +
                "@Decorator\n" +
                "class ValidDecoratorWithNoScope {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// ========== Invalid Interceptors with Built-in Normal Scopes ==========\n\n" +
                "// Invalid interceptor with @ApplicationScoped\n" +
                "@Interceptor\n" +
                "@ApplicationScoped\n" +
                "class InterceptorWithApplicationScoped {\n" +
                "}\n\n" +
                "// Invalid interceptor with @SessionScoped\n" +
                "@Interceptor\n" +
                "@SessionScoped\n" +
                "class InterceptorWithSessionScoped {\n" +
                "}\n\n" +
                "// Invalid interceptor with multiple scopes including illegal ones\n" +
                "@Interceptor\n" +
                "@ApplicationScoped\n" +
                "@SessionScoped\n" +
                "class InterceptorWithMultipleIllegalScopes {\n" +
                "}\n\n" +
                "// ========== Invalid Decorators with Built-in Normal Scopes ==========\n\n" +
                "// Invalid decorator with @ApplicationScoped\n" +
                "@Decorator\n" +
                "@ApplicationScoped\n" +
                "class DecoratorWithApplicationScoped {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid decorator with @SessionScoped\n" +
                "@Dependent\n" +
                "@Decorator\n" +
                "class DecoratorWithSessionScoped {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid decorator with multiple scopes including illegal ones\n" +
                "@Decorator\n" +
                "@RequestScoped\n" +
                "@ConversationScoped\n" +
                "class DecoratorWithMultipleIllegalScopes {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// ========== Invalid Interceptors/Decorators with Custom Normal Scopes ==========\n\n" +
                "// Invalid interceptor with custom normal scope\n" +
                "@Interceptor\n" +
                "@CustomNormalScope\n" +
                "class InterceptorWithCustomNormalScope {\n" +
                "}\n\n" +
                "// Invalid decorator with custom normal scope\n" +
                "@Decorator\n" +
                "@CustomNormalScope\n" +
                "class DecoratorWithCustomNormalScope {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid interceptor with both built-in and custom normal scopes\n" +
                "@Interceptor\n" +
                "@ApplicationScoped\n" +
                "@CustomNormalScope\n" +
                "class InterceptorWithMixedScopes {\n" +
                "}\n\n" +
                "// Invalid decorator with both built-in and custom normal scopes\n" +
                "@Decorator\n" +
                "@ApplicationScoped\n" +
                "@CustomNormalScope\n" +
                "class DecoratorWithMixedScopes {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n";
        TextEdit replaceDecoratorSessionScopedEdit = te(0, 0, 128, 0, newText6);
        CodeAction replaceDecoratorSessionScopedAction = ca(uri, "Replace @SessionScoped with @Dependent", decoratorSessionScoped, replaceDecoratorSessionScopedEdit);
        assertJavaCodeAction(createCodeActionParams(uri, decoratorSessionScoped), utils, replaceDecoratorSessionScopedAction);

        // Test quickfix for decorator with multiple scopes (line 81)
        String newText7 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.decorator.Decorator;\n" +
                "import jakarta.decorator.Delegate;\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.enterprise.context.SessionScoped;\n" +
                "import jakarta.enterprise.context.Dependent;\n" +
                "import jakarta.inject.Inject;\n\n" +
                "// ========== Valid Interceptors ==========\n\n" +
                "// Valid interceptor with explicit @Dependent scope\n" +
                "@Interceptor\n" +
                "@Dependent\n" +
                "class ValidInterceptorWithDependent {\n" +
                "}\n\n" +
                "// Valid interceptor with no scope (defaults to @Dependent)\n" +
                "@Interceptor\n" +
                "class ValidInterceptorWithNoScope {\n" +
                "}\n\n" +
                "// ========== Valid Decorators ==========\n\n" +
                "// Valid decorator with explicit @Dependent scope\n" +
                "@Decorator\n" +
                "@Dependent\n" +
                "class ValidDecoratorWithDependent {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Valid decorator with no scope (defaults to @Dependent)\n" +
                "@Decorator\n" +
                "class ValidDecoratorWithNoScope {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// ========== Invalid Interceptors with Built-in Normal Scopes ==========\n\n" +
                "// Invalid interceptor with @ApplicationScoped\n" +
                "@Interceptor\n" +
                "@ApplicationScoped\n" +
                "class InterceptorWithApplicationScoped {\n" +
                "}\n\n" +
                "// Invalid interceptor with @SessionScoped\n" +
                "@Interceptor\n" +
                "@SessionScoped\n" +
                "class InterceptorWithSessionScoped {\n" +
                "}\n\n" +
                "// Invalid interceptor with multiple scopes including illegal ones\n" +
                "@Interceptor\n" +
                "@ApplicationScoped\n" +
                "@SessionScoped\n" +
                "class InterceptorWithMultipleIllegalScopes {\n" +
                "}\n\n" +
                "// ========== Invalid Decorators with Built-in Normal Scopes ==========\n\n" +
                "// Invalid decorator with @ApplicationScoped\n" +
                "@Decorator\n" +
                "@ApplicationScoped\n" +
                "class DecoratorWithApplicationScoped {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid decorator with @SessionScoped\n" +
                "@Decorator\n" +
                "@SessionScoped\n" +
                "class DecoratorWithSessionScoped {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid decorator with multiple scopes including illegal ones\n" +
                "@Dependent\n" +
                "@Decorator\n" +
                "class DecoratorWithMultipleIllegalScopes {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// ========== Invalid Interceptors/Decorators with Custom Normal Scopes ==========\n\n" +
                "// Invalid interceptor with custom normal scope\n" +
                "@Interceptor\n" +
                "@CustomNormalScope\n" +
                "class InterceptorWithCustomNormalScope {\n" +
                "}\n\n" +
                "// Invalid decorator with custom normal scope\n" +
                "@Decorator\n" +
                "@CustomNormalScope\n" +
                "class DecoratorWithCustomNormalScope {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid interceptor with both built-in and custom normal scopes\n" +
                "@Interceptor\n" +
                "@ApplicationScoped\n" +
                "@CustomNormalScope\n" +
                "class InterceptorWithMixedScopes {\n" +
                "}\n\n" +
                "// Invalid decorator with both built-in and custom normal scopes\n" +
                "@Decorator\n" +
                "@ApplicationScoped\n" +
                "@CustomNormalScope\n" +
                "class DecoratorWithMixedScopes {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n";
        TextEdit replaceDecoratorMultipleScopesEdit = te(0, 0, 128, 0, newText7);
        CodeAction replaceDecoratorMultipleScopesAction = ca(uri, "Replace @RequestScoped and @ConversationScoped with @Dependent", decoratorMultiScope, replaceDecoratorMultipleScopesEdit);
        assertJavaCodeAction(createCodeActionParams(uri, decoratorMultiScope), utils, replaceDecoratorMultipleScopesAction);

        // Test quickfix for decorator with custom scope (line 98)
        String newText8 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.decorator.Decorator;\n" +
                "import jakarta.decorator.Delegate;\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.enterprise.context.SessionScoped;\n" +
                "import jakarta.enterprise.context.RequestScoped;\n" +
                "import jakarta.enterprise.context.ConversationScoped;\n" +
                "import jakarta.enterprise.context.Dependent;\n" +
                "import jakarta.inject.Inject;\n\n" +
                "// ========== Valid Interceptors ==========\n\n" +
                "// Valid interceptor with explicit @Dependent scope\n" +
                "@Interceptor\n" +
                "@Dependent\n" +
                "class ValidInterceptorWithDependent {\n" +
                "}\n\n" +
                "// Valid interceptor with no scope (defaults to @Dependent)\n" +
                "@Interceptor\n" +
                "class ValidInterceptorWithNoScope {\n" +
                "}\n\n" +
                "// ========== Valid Decorators ==========\n\n" +
                "// Valid decorator with explicit @Dependent scope\n" +
                "@Decorator\n" +
                "@Dependent\n" +
                "class ValidDecoratorWithDependent {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Valid decorator with no scope (defaults to @Dependent)\n" +
                "@Decorator\n" +
                "class ValidDecoratorWithNoScope {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// ========== Invalid Interceptors with Built-in Normal Scopes ==========\n\n" +
                "// Invalid interceptor with @ApplicationScoped\n" +
                "@Interceptor\n" +
                "@ApplicationScoped\n" +
                "class InterceptorWithApplicationScoped {\n" +
                "}\n\n" +
                "// Invalid interceptor with @SessionScoped\n" +
                "@Interceptor\n" +
                "@SessionScoped\n" +
                "class InterceptorWithSessionScoped {\n" +
                "}\n\n" +
                "// Invalid interceptor with multiple scopes including illegal ones\n" +
                "@Interceptor\n" +
                "@ApplicationScoped\n" +
                "@SessionScoped\n" +
                "class InterceptorWithMultipleIllegalScopes {\n" +
                "}\n\n" +
                "// ========== Invalid Decorators with Built-in Normal Scopes ==========\n\n" +
                "// Invalid decorator with @ApplicationScoped\n" +
                "@Decorator\n" +
                "@ApplicationScoped\n" +
                "class DecoratorWithApplicationScoped {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid decorator with @SessionScoped\n" +
                "@Decorator\n" +
                "@SessionScoped\n" +
                "class DecoratorWithSessionScoped {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid decorator with multiple scopes including illegal ones\n" +
                "@Decorator\n" +
                "@RequestScoped\n" +
                "@ConversationScoped\n" +
                "class DecoratorWithMultipleIllegalScopes {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// ========== Invalid Interceptors/Decorators with Custom Normal Scopes ==========\n\n" +
                "// Invalid interceptor with custom normal scope\n" +
                "@Interceptor\n" +
                "@CustomNormalScope\n" +
                "class InterceptorWithCustomNormalScope {\n" +
                "}\n\n" +
                "// Invalid decorator with custom normal scope\n" +
                "@Dependent\n" +
                "@Decorator\n" +
                "class DecoratorWithCustomNormalScope {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n\n" +
                "// Invalid interceptor with both built-in and custom normal scopes\n" +
                "@Interceptor\n" +
                "@ApplicationScoped\n" +
                "@CustomNormalScope\n" +
                "class InterceptorWithMixedScopes {\n" +
                "}\n\n" +
                "// Invalid decorator with both built-in and custom normal scopes\n" +
                "@Decorator\n" +
                "@ApplicationScoped\n" +
                "@CustomNormalScope\n" +
                "class DecoratorWithMixedScopes {\n" +
                "    @Inject\n" +
                "    @Delegate\n" +
                "    private Object delegate;\n" +
                "}\n";
        TextEdit replaceDecoratorCustomScopeEdit = te(0, 0, 128, 0, newText8);
        CodeAction replaceDecoratorCustomScopeAction = ca(uri, "Replace @CustomNormalScope with @Dependent", decoratorCustomScope, replaceDecoratorCustomScopeEdit);
        assertJavaCodeAction(createCodeActionParams(uri, decoratorCustomScope), utils, replaceDecoratorCustomScopeAction);
    }

    private JsonArray createJsonArray(String... values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }
}
