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
package io.openliberty.tools.intellij.lsp4jakarta.it.security;

import java.io.File;
import java.util.Arrays;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.lsp4jakarta.it.core.BaseJakartaTest;
import io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SecurityIdentityStoreTest extends BaseJakartaTest {

    @Test
    public void ldapIdentityStoreValidTest() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/security/identitystore/LdapIdentityStoreValid.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No diagnostics expected for valid identity store
        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void ldapIdentityStoreMissingApplicationScopedTest() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/security/identitystore/LdapIdentityStoreMissingApplicationScoped.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostic for missing @ApplicationScoped (on class name)
        Diagnostic missingApplicationScopedDiagnostic = JakartaForJavaAssert.d(9, 13, 54,
                "A class annotated with @LdapIdentityStoreDefinition must be annotated with @ApplicationScoped.",
                DiagnosticSeverity.Error, "jakarta-security", "MissingApplicationScopedOnIdentityStoreDefinition");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, missingApplicationScopedDiagnostic);
    }

    @Test
    public void ldapIdentityStoreWithWrongScopeTest() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/security/identitystore/LdapIdentityStoreWithWrongScope.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostic for wrong scope (only one diagnostic on class name)
        Diagnostic wrongScopeDiagnostic = JakartaForJavaAssert.d(11, 13, 44,
                "A class annotated with @LdapIdentityStoreDefinition must be annotated with @ApplicationScoped, instead of @RequestScoped.",
                DiagnosticSeverity.Error, "jakarta-security", "InvalidScopeOnIdentityStoreDefinition");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, wrongScopeDiagnostic);
    }

    @Test
    public void databaseIdentityStoreValidTest() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/security/identitystore/DatabaseIdentityStoreValid.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No diagnostics expected for valid identity store
        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void databaseIdentityStoreMissingApplicationScopedTest() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/security/identitystore/DatabaseIdentityStoreMissingApplicationScoped.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostic for missing @ApplicationScoped (on class name)
        Diagnostic missingApplicationScopedDiagnostic = JakartaForJavaAssert.d(9, 13, 58,
                "A class annotated with @DatabaseIdentityStoreDefinition must be annotated with @ApplicationScoped.",
                DiagnosticSeverity.Error, "jakarta-security", "MissingApplicationScopedOnIdentityStoreDefinition");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, missingApplicationScopedDiagnostic);
    }

    @Test
    public void databaseIdentityStoreWithWrongScopeTest() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/security/identitystore/DatabaseIdentityStoreWithWrongScope.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostic for wrong scope (only one diagnostic on class name)
        Diagnostic wrongScopeDiagnostic = JakartaForJavaAssert.d(11, 13, 48,
                "A class annotated with @DatabaseIdentityStoreDefinition must be annotated with @ApplicationScoped, instead of @RequestScoped.",
                DiagnosticSeverity.Error, "jakarta-security", "InvalidScopeOnIdentityStoreDefinition");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, wrongScopeDiagnostic);
    }

    @Test
    public void ldapIdentityStoreWithInterceptorScopeTest() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/security/identitystore/LdapIdentityStoreWithInterceptorScope.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostic for @Interceptor scope annotation
        Diagnostic wrongScopeDiagnostic = JakartaForJavaAssert.d(13, 13, 50,
                "A class annotated with @LdapIdentityStoreDefinition must be annotated with @ApplicationScoped, instead of @Interceptor.",
                DiagnosticSeverity.Error, "jakarta-security", "InvalidScopeOnIdentityStoreDefinition");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, wrongScopeDiagnostic);
    }

    @Test
    public void databaseIdentityStoreWithDecoratorScopeTest() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/security/identitystore/DatabaseIdentityStoreWithDecoratorScope.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostic for @Decorator missing @Delegate injection point (from jakarta-cdi)
        Diagnostic decoratorDelegateDiagnostic = JakartaForJavaAssert.d(11, 13, 52,
                "A decorator must declare exactly one injection point annotated with @Delegate.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidDecoratorDelegateInjectionPoints");

        // Test diagnostic for @Decorator scope annotation (from jakarta-security)
        Diagnostic wrongScopeDiagnostic = JakartaForJavaAssert.d(11, 13, 52,
                "A class annotated with @DatabaseIdentityStoreDefinition must be annotated with @ApplicationScoped, instead of @Decorator.",
                DiagnosticSeverity.Error, "jakarta-security", "InvalidScopeOnIdentityStoreDefinition");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, decoratorDelegateDiagnostic, wrongScopeDiagnostic);
    }
}