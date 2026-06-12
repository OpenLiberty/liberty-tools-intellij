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

        // Line 41: Invalid interceptor with @ApplicationScoped
        Diagnostic interceptorAppScoped = d(41, 6, 38, "Interceptors and decorators must be annotated with the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecorator",
                createJsonArray("jakarta.enterprise.context.ApplicationScoped"));

        // Line 47: Invalid interceptor with @SessionScoped
        Diagnostic interceptorSessionScoped = d(47, 6, 34, "Interceptors and decorators must be annotated with the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecorator",
                createJsonArray("jakarta.enterprise.context.SessionScoped"));

        // Line 54: Invalid interceptor with multiple scopes - InvalidScopeDecl
        Diagnostic interceptorMultiScopeDecl = d(54, 6, 42, "Scope type annotations must be specified by a managed bean class at most once.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidScopeDecl",
                createJsonArray("jakarta.enterprise.context.SessionScoped", "jakarta.enterprise.context.ApplicationScoped"));

        // Line 54: Invalid interceptor with multiple scopes - InvalidInterceptorOrDecorator
        Diagnostic interceptorMultiScope = d(54, 6, 42, "Interceptors and decorators must be annotated with the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecorator",
                createJsonArray("jakarta.enterprise.context.ApplicationScoped", "jakarta.enterprise.context.SessionScoped"));

        // Line 62: Invalid decorator with @ApplicationScoped
        Diagnostic decoratorAppScoped = d(62, 6, 36, "Interceptors and decorators must be annotated with the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecorator",
                createJsonArray("jakarta.enterprise.context.ApplicationScoped"));

        // Line 68: Invalid decorator with @SessionScoped
        Diagnostic decoratorSessionScoped = d(68, 6, 32, "Interceptors and decorators must be annotated with the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecorator",
                createJsonArray("jakarta.enterprise.context.SessionScoped"));

        // Line 75: Invalid decorator with multiple scopes - InvalidScopeDecl
        Diagnostic decoratorMultiScopeDecl = d(75, 6, 40, "Scope type annotations must be specified by a managed bean class at most once.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidScopeDecl",
                createJsonArray("jakarta.enterprise.context.ConversationScoped", "jakarta.enterprise.context.RequestScoped"));

        // Line 75: Invalid decorator with multiple scopes - InvalidInterceptorOrDecorator
        Diagnostic decoratorMultiScope = d(75, 6, 40, "Interceptors and decorators must be annotated with the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecorator",
                createJsonArray("jakarta.enterprise.context.RequestScoped", "jakarta.enterprise.context.ConversationScoped"));

        // Line 83: Invalid interceptor with custom normal scope
        Diagnostic interceptorCustomScope = d(83, 6, 38, "Interceptors and decorators must be annotated with the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecorator",
                createJsonArray("io.openliberty.sample.jakarta.cdi.CustomNormalScope"));

        // Line 89: Invalid decorator with custom normal scope
        Diagnostic decoratorCustomScope = d(89, 6, 36, "Interceptors and decorators must be annotated with the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecorator",
                createJsonArray("io.openliberty.sample.jakarta.cdi.CustomNormalScope"));

        // Line 96: Invalid interceptor with mixed scopes
        Diagnostic interceptorMixedScopes = d(96, 6, 32, "Interceptors and decorators must be annotated with the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecorator",
                createJsonArray("jakarta.enterprise.context.ApplicationScoped", "io.openliberty.sample.jakarta.cdi.CustomNormalScope"));

        // Line 103: Invalid decorator with mixed scopes
        Diagnostic decoratorMixedScopes = d(103, 6, 30, "Interceptors and decorators must be annotated with the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInterceptorOrDecorator",
                createJsonArray("jakarta.enterprise.context.ApplicationScoped", "io.openliberty.sample.jakarta.cdi.CustomNormalScope"));

        assertJavaDiagnostics(diagnosticsParams, utils, interceptorAppScoped, interceptorSessionScoped,
                interceptorMultiScopeDecl, interceptorMultiScope, decoratorAppScoped, decoratorSessionScoped,
                decoratorMultiScopeDecl, decoratorMultiScope, interceptorCustomScope, decoratorCustomScope,
                interceptorMixedScopes, decoratorMixedScopes);
    }

    private JsonArray createJsonArray(String... values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }
}
