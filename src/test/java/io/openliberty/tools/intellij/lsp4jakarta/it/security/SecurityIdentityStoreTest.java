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

import com.google.gson.JsonArray;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.lsp4jakarta.it.core.BaseJakartaTest;
import io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert;
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

import static io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert.*;

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
        assertJavaDiagnostics(diagnosticsParams, utils);
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
        Diagnostic missingApplicationScopedDiagnostic = d(9, 13, 54,
                "A class annotated with @LdapIdentityStoreDefinition must be annotated with @ApplicationScoped.",
                DiagnosticSeverity.Error, "jakarta-security", "MissingApplicationScopedOnIdentityStoreDefinition");

        assertJavaDiagnostics(diagnosticsParams, utils, missingApplicationScopedDiagnostic);

        // Test quickfix: insert @ApplicationScoped.
        // InsertAnnotationProposal rewrites the entire file, adding the import and annotation.
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, missingApplicationScopedDiagnostic);
        String insertText = "package io.openliberty.sample.jakarta.security.identitystore;\n\n"
                + "import jakarta.enterprise.context.ApplicationScoped;\n"
                + "import jakarta.security.enterprise.identitystore.LdapIdentityStoreDefinition;\n\n"
                + "@ApplicationScoped\n"
                + "@LdapIdentityStoreDefinition(\n"
                + "        url = \"ldap://localhost:10389\",\n"
                + "        callerBaseDn = \"ou=caller,dc=jsr375,dc=net\",\n"
                + "        groupSearchBase = \"ou=group,dc=jsr375,dc=net\"\n"
                + ")\n"
                + "public class LdapIdentityStoreMissingApplicationScoped {\n"
                + "    // Invalid: Missing @ApplicationScoped annotation\n"
                + "}";
        TextEdit insertAnnotation = te(0, 0, 11, 1, insertText);
        CodeAction insertAction = ca(uri, "Insert @ApplicationScoped", missingApplicationScopedDiagnostic, insertAnnotation);
        assertJavaCodeAction(codeActionParams, utils, insertAction);
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
        Diagnostic wrongScopeDiagnostic = d(11, 13, 44,
                "A class annotated with @LdapIdentityStoreDefinition must be annotated with @ApplicationScoped, instead of @RequestScoped.",
                DiagnosticSeverity.Error, "jakarta-security", "InvalidScopeOnIdentityStoreDefinition",
                createJsonArray("jakarta.enterprise.context.RequestScoped"));

        assertJavaDiagnostics(diagnosticsParams, utils, wrongScopeDiagnostic);

        // Test quickfix: replace @RequestScoped with @ApplicationScoped.
        // ReplaceAnnotationProposal rewrites the entire file, removing @RequestScoped and inserting @ApplicationScoped.
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, wrongScopeDiagnostic);
        String replaceText = "package io.openliberty.sample.jakarta.security.identitystore;\n\n"
                + "import jakarta.enterprise.context.ApplicationScoped;\n"
                + "import jakarta.security.enterprise.identitystore.LdapIdentityStoreDefinition;\n\n"
                + "@ApplicationScoped\n"
                + "@LdapIdentityStoreDefinition(\n"
                + "        url = \"ldap://localhost:10389\",\n"
                + "        callerBaseDn = \"ou=caller,dc=jsr375,dc=net\",\n"
                + "        groupSearchBase = \"ou=group,dc=jsr375,dc=net\"\n"
                + ")\n"
                + "public class LdapIdentityStoreWithWrongScope {\n"
                + "    // Invalid: Has @RequestScoped instead of @ApplicationScoped\n"
                + "\n"
                + "    private String searchFilter;\n"
                + "    private boolean useSSL;\n"
                + "\n"
                + "    public String getSearchFilter() {\n"
                + "        return searchFilter;\n"
                + "    }\n"
                + "\n"
                + "    public void setSearchFilter(String searchFilter) {\n"
                + "        this.searchFilter = searchFilter;\n"
                + "    }\n"
                + "\n"
                + "    public boolean isUseSSL() {\n"
                + "        return useSSL;\n"
                + "    }\n"
                + "\n"
                + "    public void setUseSSL(boolean useSSL) {\n"
                + "        this.useSSL = useSSL;\n"
                + "    }\n"
                + "}";
        TextEdit replaceAnnotation = te(0, 0, 32, 1, replaceText);
        CodeAction replaceAction = ca(uri, "Replace @RequestScoped with @ApplicationScoped", wrongScopeDiagnostic, replaceAnnotation);
        assertJavaCodeAction(codeActionParams, utils, replaceAction);
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
        assertJavaDiagnostics(diagnosticsParams, utils);
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
        Diagnostic missingApplicationScopedDiagnostic = d(9, 13, 58,
                "A class annotated with @DatabaseIdentityStoreDefinition must be annotated with @ApplicationScoped.",
                DiagnosticSeverity.Error, "jakarta-security", "MissingApplicationScopedOnIdentityStoreDefinition");

        assertJavaDiagnostics(diagnosticsParams, utils, missingApplicationScopedDiagnostic);

        // Test quickfix: insert @ApplicationScoped.
        // InsertAnnotationProposal rewrites the entire file, adding the import and annotation.
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, missingApplicationScopedDiagnostic);
        String insertText = "package io.openliberty.sample.jakarta.security.identitystore;\n\n"
                + "import jakarta.enterprise.context.ApplicationScoped;\n"
                + "import jakarta.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;\n\n"
                + "@ApplicationScoped\n"
                + "@DatabaseIdentityStoreDefinition(\n"
                + "        dataSourceLookup = \"java:comp/DefaultDataSource\",\n"
                + "        callerQuery = \"select password from caller where name = ?\",\n"
                + "        groupsQuery = \"select group_name from caller_groups where caller_name = ?\"\n"
                + ")\n"
                + "public class DatabaseIdentityStoreMissingApplicationScoped {\n"
                + "    // Invalid: Missing @ApplicationScoped annotation\n"
                + "}";
        TextEdit insertAnnotation = te(0, 0, 11, 1, insertText);
        CodeAction insertAction = ca(uri, "Insert @ApplicationScoped", missingApplicationScopedDiagnostic, insertAnnotation);
        assertJavaCodeAction(codeActionParams, utils, insertAction);
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
        Diagnostic wrongScopeDiagnostic = d(11, 13, 48,
                "A class annotated with @DatabaseIdentityStoreDefinition must be annotated with @ApplicationScoped, instead of @RequestScoped.",
                DiagnosticSeverity.Error, "jakarta-security", "InvalidScopeOnIdentityStoreDefinition",
                createJsonArray("jakarta.enterprise.context.RequestScoped"));

        assertJavaDiagnostics(diagnosticsParams, utils, wrongScopeDiagnostic);

        // Test quickfix: replace @RequestScoped with @ApplicationScoped.
        // ReplaceAnnotationProposal rewrites the entire file, removing @RequestScoped and inserting @ApplicationScoped.
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, wrongScopeDiagnostic);
        String replaceText = "package io.openliberty.sample.jakarta.security.identitystore;\n\n"
                + "import jakarta.enterprise.context.ApplicationScoped;\n"
                + "import jakarta.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;\n\n"
                + "@ApplicationScoped\n"
                + "@DatabaseIdentityStoreDefinition(\n"
                + "        dataSourceLookup = \"java:comp/DefaultDataSource\",\n"
                + "        callerQuery = \"select password from caller where name = ?\",\n"
                + "        groupsQuery = \"select group_name from caller_groups where caller_name = ?\"\n"
                + ")\n"
                + "public class DatabaseIdentityStoreWithWrongScope {\n"
                + "    // Invalid: Has @RequestScoped instead of @ApplicationScoped\n"
                + "\n"
                + "    private String hashAlgorithm;\n"
                + "    private boolean enableCaching;\n"
                + "\n"
                + "    public String getHashAlgorithm() {\n"
                + "        return hashAlgorithm;\n"
                + "    }\n"
                + "\n"
                + "    public void setHashAlgorithm(String hashAlgorithm) {\n"
                + "        this.hashAlgorithm = hashAlgorithm;\n"
                + "    }\n"
                + "\n"
                + "    public boolean isEnableCaching() {\n"
                + "        return enableCaching;\n"
                + "    }\n"
                + "\n"
                + "    public void setEnableCaching(boolean enableCaching) {\n"
                + "        this.enableCaching = enableCaching;\n"
                + "    }\n"
                + "}";
        TextEdit replaceAnnotation = te(0, 0, 32, 1, replaceText);
        CodeAction replaceAction = ca(uri, "Replace @RequestScoped with @ApplicationScoped", wrongScopeDiagnostic, replaceAnnotation);
        assertJavaCodeAction(codeActionParams, utils, replaceAction);
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
        Diagnostic wrongScopeDiagnostic = d(11, 13, 50,
                "A class annotated with @LdapIdentityStoreDefinition must be annotated with @ApplicationScoped, instead of @Interceptor.",
                DiagnosticSeverity.Error, "jakarta-security", "InvalidScopeOnIdentityStoreDefinition",
                createJsonArray("jakarta.interceptor.Interceptor"));

        assertJavaDiagnostics(diagnosticsParams, utils, wrongScopeDiagnostic);
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
        Diagnostic decoratorDelegateDiagnostic = d(11, 13, 52,
                "A decorator must declare exactly one injection point annotated with @Delegate.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidDecoratorDelegateInjectionPoints");

        // Test diagnostic for @Decorator scope annotation (from jakarta-security)
        Diagnostic wrongScopeDiagnostic = d(11, 13, 52,
                "A class annotated with @DatabaseIdentityStoreDefinition must be annotated with @ApplicationScoped, instead of @Decorator.",
                DiagnosticSeverity.Error, "jakarta-security", "InvalidScopeOnIdentityStoreDefinition",
                createJsonArray("jakarta.decorator.Decorator"));

        assertJavaDiagnostics(diagnosticsParams, utils, decoratorDelegateDiagnostic, wrongScopeDiagnostic);
    }

    private JsonArray createJsonArray(String... values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }
}
