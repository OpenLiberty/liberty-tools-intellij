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
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;

import static io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert.*;

@RunWith(JUnit4.class)
public class SingletonSessionBeanTest extends BaseJakartaTest {

    @Test
    public void singletonSessionBeanWithInvalidScopes() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/SingletonSessionBean.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test case 1: Singleton with invalid scope (RequestScoped) - should report error on annotation text
        JsonArray data1 = new JsonArray();
        data1.add("jakarta.enterprise.context.RequestScoped");
        Diagnostic dRequestScopedAnnotation = d(11, 13, 33,
                "A singleton session bean belongs to either the @ApplicationScoped or @Dependent scope; any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidSingletonSessionBeanScope", data1);

        // Test case 2: Singleton with invalid scope (SessionScoped) - should report error on class name
        JsonArray data2 = new JsonArray();
        data2.add("jakarta.enterprise.context.SessionScoped");
        Diagnostic dSessionScopedClass = d(17, 6, 31,
                "A singleton session bean belongs to either the @ApplicationScoped or @Dependent scope; any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidSingletonSessionBeanScope", data2);

        // Test case 6: Singleton with mixed valid and invalid scopes (RequestScoped + ApplicationScoped)
        JsonArray data3 = new JsonArray();
        data3.add("jakarta.enterprise.context.ApplicationScoped");
        data3.add("jakarta.enterprise.context.RequestScoped");
        Diagnostic dMixedInvalidAndApplicationScopedInvalidScope = d(41, 6, 51,
                "A singleton session bean belongs to either the @ApplicationScoped or @Dependent scope; any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidSingletonSessionBeanScope", data3);
        
        JsonArray data3b = new JsonArray();
        data3b.add("jakarta.enterprise.context.ApplicationScoped");
        data3b.add("jakarta.enterprise.context.RequestScoped");
        Diagnostic dMixedInvalidAndApplicationScopedMultipleScopes = d(41, 6, 51,
                "Scope type annotations must be specified by a managed bean class at most once.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidScopeDecl", data3b);

        // Test case 7: Singleton with mixed valid and invalid scopes (SessionScoped + Dependent)
        JsonArray data4 = new JsonArray();
        data4.add("jakarta.enterprise.context.Dependent");
        data4.add("jakarta.enterprise.context.SessionScoped");
        Diagnostic dMixedInvalidAndDependentInvalidScope = d(48, 6, 43,
                "A singleton session bean belongs to either the @ApplicationScoped or @Dependent scope; any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidSingletonSessionBeanScope", data4);
        
        JsonArray data4b = new JsonArray();
        data4b.add("jakarta.enterprise.context.Dependent");
        data4b.add("jakarta.enterprise.context.SessionScoped");
        Diagnostic dMixedInvalidAndDependentMultipleScopes = d(48, 6, 43,
                "Scope type annotations must be specified by a managed bean class at most once.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidScopeDecl", data4b);
        
        // Test case 8: Reports InvalidScopeDecl for multiple scopes (even though both are valid)
        JsonArray data5 = new JsonArray();
        data5.add("jakarta.enterprise.context.Dependent");
        data5.add("jakarta.enterprise.context.ApplicationScoped");
        Diagnostic dBothValidScopesMultipleScopes = d(55, 6, 34,
                "Scope type annotations must be specified by a managed bean class at most once.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidScopeDecl", data5);

        assertJavaDiagnostics(diagnosticsParams, utils,
                dRequestScopedAnnotation,
                dSessionScopedClass,
                dMixedInvalidAndApplicationScopedInvalidScope,
                dMixedInvalidAndApplicationScopedMultipleScopes,
                dMixedInvalidAndDependentInvalidScope,
                dMixedInvalidAndDependentMultipleScopes,
                dBothValidScopesMultipleScopes);

        // Test code actions for dRequestScopedAnnotation (Test case 1)
        // Expected: Remove @Singleton or Remove @RequestScoped
        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, dRequestScopedAnnotation);
        String newText1a = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.ejb.Singleton;\nimport jakarta.enterprise.context.RequestScoped;\n" +
                "import jakarta.enterprise.context.SessionScoped;\nimport jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.enterprise.context.Dependent;\n\n// Test case 1: Singleton with invalid scope (RequestScoped) - should report error\n@RequestScoped\n" +
                "public class SingletonSessionBean {\n}\n\n// Test case 2: Singleton with invalid scope (SessionScoped) - should report error\n@Singleton\n@SessionScoped\n" +
                "class SingletonWithSessionScope {\n}\n\n// Test case 3: Singleton with valid scope (ApplicationScoped) - should NOT report error\n@Singleton\n@ApplicationScoped\n" +
                "class SingletonWithApplicationScope {\n}\n\n// Test case 4: Singleton with valid scope (Dependent) - should NOT report error\n@Singleton\n@Dependent\n" +
                "class SingletonWithDependent {\n}\n\n// Test case 5: Singleton with no scope - should NOT report error (uses default)\n@Singleton\n" +
                "class SingletonWithNoScope {\n}\n\n// Test case 6: Singleton with mixed valid and invalid scopes (RequestScoped + ApplicationScoped) - should report error\n" +
                "@Singleton\n@RequestScoped\n@ApplicationScoped\nclass SingletonWithMixedInvalidAndApplicationScoped {\n}\n\n" +
                "// Test case 7: Singleton with mixed valid and invalid scopes (SessionScoped + Dependent) - " +
                "should report error\n@Singleton\n@SessionScoped\n@Dependent\nclass SingletonWithMixedInvalidAndDependent {\n}\n\n" +
                "// Test case 8: Singleton with both valid scopes (ApplicationScoped + Dependent) - should NOT report error\n@Singleton\n@ApplicationScoped\n@Dependent\n" +
                "class SingletonWithBothValidScopes {\n}";
        TextEdit te1a = te(0, 0, 56, 1, newText1a);
        CodeAction ca1a = ca(uri, "Remove @Singleton", dRequestScopedAnnotation, te1a);
        
        String newText1b = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.ejb.Singleton;\nimport jakarta.enterprise.context.SessionScoped;\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\nimport jakarta.enterprise.context.Dependent;\n\n" +
                "// Test case 1: Singleton with invalid scope (RequestScoped) - should report error\n@Dependent\n@Singleton\npublic class SingletonSessionBean {\n}\n\n" +
                "// Test case 2: Singleton with invalid scope (SessionScoped) - should report error\n@Singleton\n@SessionScoped\nclass SingletonWithSessionScope {\n}\n\n" +
                "// Test case 3: Singleton with valid scope (ApplicationScoped) - should NOT report error\n@Singleton\n@ApplicationScoped\nclass SingletonWithApplicationScope {\n}\n\n" +
                "// Test case 4: Singleton with valid scope (Dependent) - should NOT report error\n@Singleton\n@Dependent\nclass SingletonWithDependent {\n}\n\n" +
                "// Test case 5: Singleton with no scope - should NOT report error (uses default)\n@Singleton\nclass SingletonWithNoScope {\n}\n\n" +
                "// Test case 6: Singleton with mixed valid and invalid scopes (RequestScoped + ApplicationScoped) - should report error\n" +
                "@Singleton\n@RequestScoped\n@ApplicationScoped\nclass SingletonWithMixedInvalidAndApplicationScoped {\n}\n\n" +
                "// Test case 7: Singleton with mixed valid and invalid scopes (SessionScoped + Dependent) - should report error\n" +
                "@Singleton\n@SessionScoped\n@Dependent\nclass SingletonWithMixedInvalidAndDependent {\n}\n\n" +
                "// Test case 8: Singleton with both valid scopes (ApplicationScoped + Dependent) - should NOT report error\n" +
                "@Singleton\n@ApplicationScoped\n@Dependent\nclass SingletonWithBothValidScopes {\n}";
        TextEdit te1b = te(0, 0, 56, 1, newText1b);
        CodeAction ca1b = ca(uri, "Replace current scope with @Dependent", dRequestScopedAnnotation, te1b);
        assertJavaCodeAction(codeActionParams1, utils, ca1a, ca1b);

        // Test code actions for dMixedInvalidAndDependentMultipleScopes (Test case 7)
        // Expected: Remove @SessionScoped or Remove @Dependent
        JakartaJavaCodeActionParams codeActionParams2 = createCodeActionParams(uri, dMixedInvalidAndDependentMultipleScopes);
        String newText2a = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.ejb.Singleton;\nimport jakarta.enterprise.context.RequestScoped;\n" +
                "import jakarta.enterprise.context.SessionScoped;\nimport jakarta.enterprise.context.ApplicationScoped;\nimport jakarta.enterprise.context.Dependent;\n\n" +
                "// Test case 1: Singleton with invalid scope (RequestScoped) - should report error\n@Singleton\n@RequestScoped\npublic class SingletonSessionBean {\n}\n\n" +
                "// Test case 2: Singleton with invalid scope (SessionScoped) - should report error\n@Singleton\n@SessionScoped\nclass SingletonWithSessionScope {\n}\n\n" +
                "// Test case 3: Singleton with valid scope (ApplicationScoped) - should NOT report error\n@Singleton\n@ApplicationScoped\nclass SingletonWithApplicationScope {\n}\n\n" +
                "// Test case 4: Singleton with valid scope (Dependent) - should NOT report error\n@Singleton\n@Dependent\nclass SingletonWithDependent {\n}\n\n" +
                "// Test case 5: Singleton with no scope - should NOT report error (uses default)\n@Singleton\nclass SingletonWithNoScope {\n}\n\n" +
                "// Test case 6: Singleton with mixed valid and invalid scopes (RequestScoped + ApplicationScoped) - should report error\n" +
                "@Singleton\n@RequestScoped\n@ApplicationScoped\nclass SingletonWithMixedInvalidAndApplicationScoped {\n}\n\n" +
                "// Test case 7: Singleton with mixed valid and invalid scopes (SessionScoped + Dependent) - should report error\n@Singleton\n@SessionScoped\n" +
                "class SingletonWithMixedInvalidAndDependent {\n}\n\n// Test case 8: Singleton with both valid scopes (ApplicationScoped + Dependent) - should NOT report error\n" +
                "@Singleton\n@ApplicationScoped\n@Dependent\nclass SingletonWithBothValidScopes {\n}";
        TextEdit te2a = te(0, 0, 56, 1, newText2a);
        CodeAction ca2a = ca(uri, "Remove @Dependent", dMixedInvalidAndDependentMultipleScopes, te2a);
        
        String newText2b = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.ejb.Singleton;\nimport jakarta.enterprise.context.RequestScoped;\n" +
                "import jakarta.enterprise.context.SessionScoped;\nimport jakarta.enterprise.context.ApplicationScoped;\nimport jakarta.enterprise.context.Dependent;\n\n" +
                "// Test case 1: Singleton with invalid scope (RequestScoped) - should report error\n@Singleton\n@RequestScoped\npublic class SingletonSessionBean {\n}\n\n" +
                "// Test case 2: Singleton with invalid scope (SessionScoped) - should report error\n@Singleton\n@SessionScoped\nclass SingletonWithSessionScope {\n}\n\n" +
                "// Test case 3: Singleton with valid scope (ApplicationScoped) - should NOT report error\n@Singleton\n@ApplicationScoped\nclass SingletonWithApplicationScope {\n}\n\n" +
                "// Test case 4: Singleton with valid scope (Dependent) - should NOT report error\n@Singleton\n@Dependent\nclass SingletonWithDependent {\n}\n\n" +
                "// Test case 5: Singleton with no scope - should NOT report error (uses default)\n@Singleton\nclass SingletonWithNoScope {\n}\n\n" +
                "// Test case 6: Singleton with mixed valid and invalid scopes (RequestScoped + ApplicationScoped) - should report error\n@Singleton\n@RequestScoped\n@ApplicationScoped\n" +
                "class SingletonWithMixedInvalidAndApplicationScoped {\n}\n\n// Test case 7: Singleton with mixed valid and invalid scopes (SessionScoped + Dependent) - should report error\n" +
                "@Singleton\n@Dependent\nclass SingletonWithMixedInvalidAndDependent {\n}\n\n" +
                "// Test case 8: Singleton with both valid scopes (ApplicationScoped + Dependent) - should NOT report error\n@Singleton\n@ApplicationScoped\n@Dependent\n" +
                "class SingletonWithBothValidScopes {\n}";
        TextEdit te2b = te(0, 0, 56, 1, newText2b);
        CodeAction ca2b = ca(uri, "Remove @SessionScoped", dMixedInvalidAndDependentMultipleScopes, te2b);
        assertJavaCodeAction(codeActionParams2, utils, ca2a, ca2b);
    }
}
