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

        // Test case 1: Singleton with invalid scope (RequestScoped) - should report error
        Diagnostic d1 = d(11, 13, 34,
                "A singleton session bean belongs to either the @ApplicationScoped or @Dependent scope; any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidSingletonSessionBeanScope");

        // Test case 2: Singleton with invalid scope (SessionScoped) - should report error
        Diagnostic d2 = d(17, 6, 32,
                "A singleton session bean belongs to either the @ApplicationScoped or @Dependent scope; any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidSingletonSessionBeanScope");

        // Test case 6: Singleton with mixed valid and invalid scopes (RequestScoped + ApplicationScoped) - should report error
        Diagnostic d3 = d(41, 6, 46,
                "A singleton session bean belongs to either the @ApplicationScoped or @Dependent scope; any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidSingletonSessionBeanScope");

        // Test case 7: Singleton with mixed valid and invalid scopes (SessionScoped + Dependent) - should report error
        Diagnostic d4 = d(48, 6, 41,
                "A singleton session bean belongs to either the @ApplicationScoped or @Dependent scope; any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidSingletonSessionBeanScope");

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3, d4);

        // Test code actions for test case 1
        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d1);
        
        String newText1ApplicationScoped = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.ejb.Singleton;\n" +
                "import jakarta.enterprise.context.RequestScoped;\n" +
                "import jakarta.enterprise.context.SessionScoped;\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.enterprise.context.Dependent;\n\n" +
                "// Test case 1: Singleton with invalid scope (RequestScoped) - should report error\n" +
                "@Singleton\n" +
                "@ApplicationScoped\n" +
                "public class SingletonSessionBean {\n" +
                "}\n\n" +
                "// Test case 2: Singleton with invalid scope (SessionScoped) - should report error\n" +
                "@Singleton\n" +
                "@SessionScoped\n" +
                "class SingletonWithSessionScope {\n" +
                "}\n\n" +
                "// Test case 3: Singleton with valid scope (ApplicationScoped) - should NOT report error\n" +
                "@Singleton\n" +
                "@ApplicationScoped\n" +
                "class SingletonWithApplicationScope {\n" +
                "}\n\n" +
                "// Test case 4: Singleton with valid scope (Dependent) - should NOT report error\n" +
                "@Singleton\n" +
                "@Dependent\n" +
                "class SingletonWithDependent {\n" +
                "}\n\n" +
                "// Test case 5: Singleton with no scope - should NOT report error (uses default)\n" +
                "@Singleton\n" +
                "class SingletonWithNoScope {\n" +
                "}\n\n" +
                "// Test case 6: Singleton with mixed valid and invalid scopes (RequestScoped + ApplicationScoped) - should report error\n" +
                "@Singleton\n" +
                "@RequestScoped\n" +
                "@ApplicationScoped\n" +
                "class SingletonWithMixedInvalidAndApplicationScoped {\n" +
                "}\n\n" +
                "// Test case 7: Singleton with mixed valid and invalid scopes (SessionScoped + Dependent) - should report error\n" +
                "@Singleton\n" +
                "@SessionScoped\n" +
                "@Dependent\n" +
                "class SingletonWithMixedInvalidAndDependent {\n" +
                "}\n\n" +
                "// Test case 8: Singleton with both valid scopes (ApplicationScoped + Dependent) - should NOT report error\n" +
                "@Singleton\n" +
                "@ApplicationScoped\n" +
                "@Dependent\n" +
                "class SingletonWithBothValidScopes {\n" +
                "}\n";
        
        String newText1Dependent = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.ejb.Singleton;\n" +
                "import jakarta.enterprise.context.RequestScoped;\n" +
                "import jakarta.enterprise.context.SessionScoped;\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.enterprise.context.Dependent;\n\n" +
                "// Test case 1: Singleton with invalid scope (RequestScoped) - should report error\n" +
                "@Singleton\n" +
                "@Dependent\n" +
                "public class SingletonSessionBean {\n" +
                "}\n\n" +
                "// Test case 2: Singleton with invalid scope (SessionScoped) - should report error\n" +
                "@Singleton\n" +
                "@SessionScoped\n" +
                "class SingletonWithSessionScope {\n" +
                "}\n\n" +
                "// Test case 3: Singleton with valid scope (ApplicationScoped) - should NOT report error\n" +
                "@Singleton\n" +
                "@ApplicationScoped\n" +
                "class SingletonWithApplicationScope {\n" +
                "}\n\n" +
                "// Test case 4: Singleton with valid scope (Dependent) - should NOT report error\n" +
                "@Singleton\n" +
                "@Dependent\n" +
                "class SingletonWithDependent {\n" +
                "}\n\n" +
                "// Test case 5: Singleton with no scope - should NOT report error (uses default)\n" +
                "@Singleton\n" +
                "class SingletonWithNoScope {\n" +
                "}\n\n" +
                "// Test case 6: Singleton with mixed valid and invalid scopes (RequestScoped + ApplicationScoped) - should report error\n" +
                "@Singleton\n" +
                "@RequestScoped\n" +
                "@ApplicationScoped\n" +
                "class SingletonWithMixedInvalidAndApplicationScoped {\n" +
                "}\n\n" +
                "// Test case 7: Singleton with mixed valid and invalid scopes (SessionScoped + Dependent) - should report error\n" +
                "@Singleton\n" +
                "@SessionScoped\n" +
                "@Dependent\n" +
                "class SingletonWithMixedInvalidAndDependent {\n" +
                "}\n\n" +
                "// Test case 8: Singleton with both valid scopes (ApplicationScoped + Dependent) - should NOT report error\n" +
                "@Singleton\n" +
                "@ApplicationScoped\n" +
                "@Dependent\n" +
                "class SingletonWithBothValidScopes {\n" +
                "}\n";

        TextEdit te1 = te(0, 0, 57, 0, newText1ApplicationScoped);
        CodeAction ca1 = ca(uri, "Replace current scope with @ApplicationScoped", d1, te1);
        
        TextEdit te2 = te(0, 0, 57, 0, newText1Dependent);
        CodeAction ca2 = ca(uri, "Replace current scope with @Dependent", d1, te2);

        assertJavaCodeAction(codeActionParams1, utils, ca1, ca2);
    }
}
