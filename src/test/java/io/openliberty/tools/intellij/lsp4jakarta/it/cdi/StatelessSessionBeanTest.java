/*******************************************************************************
 * Copyright (c) 2021, 2026 IBM Corporation and others.
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
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;

import static io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert.*;

@RunWith(JUnit4.class)
public class StatelessSessionBeanTest extends BaseJakartaTest {

    @Test
    public void statelessSessionBeanWithIllegalScope() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/StatelessSessionBean.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test expected diagnostics (order matches actual diagnostic order: line 23, 16, 10)
        Diagnostic withRequestScoped = d(23, 6, 33,
                "A stateless session bean belongs to the @Dependent scope; any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidStatelessSessionBeanWithIllegalScope");

        Diagnostic withSessionScoped = d(16, 6, 32,
                "A stateless session bean belongs to the @Dependent scope; any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidStatelessSessionBeanWithIllegalScope");

        Diagnostic withDependentAndRequest = d(10, 13, 33,
                "A stateless session bean belongs to the @Dependent scope; any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidStatelessSessionBeanWithIllegalScope");

        assertJavaDiagnostics(diagnosticsParams, utils, withRequestScoped, withSessionScoped, withDependentAndRequest);
        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, withRequestScoped);
        String newText = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.ejb.Stateless;\nimport jakarta.enterprise.context.RequestScoped;\n" +
                "import jakarta.enterprise.context.SessionScoped;\nimport jakarta.enterprise.context.Dependent;\n\n// Invalid: Stateless with RequestScoped\n" +
                "@Stateless\n@RequestScoped\npublic class StatelessSessionBean {\n}\n\n// Invalid: Stateless with SessionScoped\n" +
                "@Stateless\n@SessionScoped\nclass StatelessWithSessionScoped {\n}\n\n// Invalid: Stateless with multiple scopes including Dependent\n" +
                "@Dependent\n@RequestScoped\nclass StatelessWithMultipleScopes {\n}\n\n// Valid: Stateless with no explicit scope (defaults to @Dependent)\n" +
                "@Stateless\nclass StatelessWithNoScope {\n}\n\n// Valid: Stateless with only Dependent\n" +
                "@Stateless\n@Dependent\nclass StatelessWithDependent {\n}\n";
        String newText0 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.ejb.Stateless;\nimport jakarta.enterprise.context.Dependent;\n" +
                "import jakarta.enterprise.context.SessionScoped;\n\n// Invalid: Stateless with RequestScoped\n" +
                "@Stateless\n@RequestScoped\npublic class StatelessSessionBean {\n}\n\n// Invalid: Stateless with SessionScoped\n" +
                "@Stateless\n@SessionScoped\nclass StatelessWithSessionScoped {\n}\n\n// Invalid: Stateless with multiple scopes including Dependent\n" +
                "@Dependent\n@Stateless\nclass StatelessWithMultipleScopes {\n}\n\n// Valid: Stateless with no explicit scope (defaults to @Dependent)\n" +
                "@Stateless\nclass StatelessWithNoScope {\n}\n\n// Valid: Stateless with only Dependent\n" +
                "@Stateless\n@Dependent\nclass StatelessWithDependent {\n}\n";
        TextEdit te1 = te(0, 0, 36, 0, newText);
        TextEdit te2 = te(0, 0, 36, 0, newText0);
        CodeAction ca1 = ca(uri, "Remove @Stateless", withRequestScoped, te1);
        CodeAction ca2 = ca(uri, "Replace current scope with @Dependent", withRequestScoped, te2);
        assertJavaCodeAction(codeActionParams1, utils, ca1, ca2);

        codeActionParams1 = createCodeActionParams(uri, withSessionScoped);
        newText = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.ejb.Stateless;\nimport jakarta.enterprise.context.RequestScoped;\n" +
                "import jakarta.enterprise.context.SessionScoped;\nimport jakarta.enterprise.context.Dependent;\n\n// Invalid: Stateless with RequestScoped\n" +
                "@Stateless\n@RequestScoped\npublic class StatelessSessionBean {\n}\n\n// Invalid: Stateless with SessionScoped\n" +
                "@SessionScoped\nclass StatelessWithSessionScoped {\n}\n\n// Invalid: Stateless with multiple scopes including Dependent\n" +
                "@Stateless\n@Dependent\n@RequestScoped\nclass StatelessWithMultipleScopes {\n}\n\n// Valid: Stateless with no explicit scope (defaults to @Dependent)\n" +
                "@Stateless\nclass StatelessWithNoScope {\n}\n\n// Valid: Stateless with only Dependent\n@Stateless\n@Dependent\nclass StatelessWithDependent {\n}\n";
        newText0 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.ejb.Stateless;\nimport jakarta.enterprise.context.RequestScoped;\n" +
                "import jakarta.enterprise.context.Dependent;\n\n// Invalid: Stateless with RequestScoped\n@Stateless\n@RequestScoped\n" +
                "public class StatelessSessionBean {\n}\n\n// Invalid: Stateless with SessionScoped\n" +
                "@Dependent\n@Stateless\nclass StatelessWithSessionScoped {\n}\n\n// Invalid: Stateless with multiple scopes including Dependent\n" +
                "@Stateless\n@Dependent\n@RequestScoped\nclass StatelessWithMultipleScopes {\n}\n\n// Valid: Stateless with no explicit scope (defaults to @Dependent)\n" +
                "@Stateless\nclass StatelessWithNoScope {\n}\n\n// Valid: Stateless with only Dependent\n@Stateless\n@Dependent\nclass StatelessWithDependent {\n}\n";
        te1 = te(0, 0, 36, 0, newText);
        te2 = te(0, 0, 36, 0, newText0);
        ca1 = ca(uri, "Remove @Stateless", withSessionScoped, te1);
        ca2 = ca(uri, "Replace current scope with @Dependent", withSessionScoped, te2);
        assertJavaCodeAction(codeActionParams1, utils, ca1, ca2);

        codeActionParams1 = createCodeActionParams(uri, withDependentAndRequest);
        newText = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.ejb.Stateless;\nimport jakarta.enterprise.context.RequestScoped;\n" +
                "import jakarta.enterprise.context.SessionScoped;\nimport jakarta.enterprise.context.Dependent;\n\n// Invalid: Stateless with RequestScoped\n" +
                "@RequestScoped\npublic class StatelessSessionBean {\n}\n\n// Invalid: Stateless with SessionScoped\n" +
                "@Stateless\n@SessionScoped\nclass StatelessWithSessionScoped {\n}\n\n// Invalid: Stateless with multiple scopes including Dependent\n" +
                "@Stateless\n@Dependent\n@RequestScoped\nclass StatelessWithMultipleScopes {\n}\n\n// Valid: Stateless with no explicit scope (defaults to @Dependent)\n" +
                "@Stateless\nclass StatelessWithNoScope {\n}\n\n// Valid: Stateless with only Dependent\n@Stateless\n@Dependent\nclass StatelessWithDependent {\n}\n";
        newText0 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.ejb.Stateless;\nimport jakarta.enterprise.context.SessionScoped;\n" +
                "import jakarta.enterprise.context.Dependent;\n\n// Invalid: Stateless with RequestScoped\n" +
                "@Dependent\n@Stateless\npublic class StatelessSessionBean {\n}\n\n// Invalid: Stateless with SessionScoped\n" +
                "@Stateless\n@SessionScoped\nclass StatelessWithSessionScoped {\n}\n\n// Invalid: Stateless with multiple scopes including Dependent\n" +
                "@Stateless\n@Dependent\n@RequestScoped\nclass StatelessWithMultipleScopes {\n}\n\n// Valid: Stateless with no explicit scope (defaults to @Dependent)\n" +
                "@Stateless\nclass StatelessWithNoScope {\n}\n\n// Valid: Stateless with only Dependent\n@Stateless\n@Dependent\nclass StatelessWithDependent {\n}\n";
        te1 = te(0, 0, 36, 0, newText);
        te2 = te(0, 0, 36, 0, newText0);
        ca1 = ca(uri, "Remove @Stateless", withDependentAndRequest, te1);
        ca2 = ca(uri, "Replace current scope with @Dependent", withDependentAndRequest, te2);
        assertJavaCodeAction(codeActionParams1, utils, ca1, ca2);
    }
}
