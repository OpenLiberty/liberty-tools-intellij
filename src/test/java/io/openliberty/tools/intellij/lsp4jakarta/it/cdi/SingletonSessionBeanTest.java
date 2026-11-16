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

    private static final String CDI_SESSIONBEAN_PATH =
            "/src/main/java/io/openliberty/sample/jakarta/cdi/sessionbean/";

    private String getUri(Module module, String fileName) {
        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + CDI_SESSIONBEAN_PATH + fileName);
        return VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();
    }

    /**
     * SingletonSessionBean.java — line 8: "public class SingletonSessionBean {" → col 13..33
     */
    @Test
    public void testSingletonWithRequestScoped() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
        String uri = getUri(module, "SingletonSessionBean.java");

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        JsonArray dataRequestScoped = new JsonArray();
        dataRequestScoped.add("jakarta.enterprise.context.RequestScoped");
        Diagnostic illegalRequestScope = d(8, 13, 33,
                "A singleton session bean must be annotated with either @ApplicationScoped or @Dependent.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidSingletonSessionBeanScope", dataRequestScoped);

        assertJavaDiagnostics(diagnosticsParams, utils, illegalRequestScope);

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, illegalRequestScope);

        String removeSingletonText = "package io.openliberty.sample.jakarta.cdi.sessionbean;\n\n" +
                "import jakarta.ejb.Singleton;\nimport jakarta.enterprise.context.RequestScoped;\n\n" +
                "// Test case 1: Singleton with invalid scope (RequestScoped) - should report error\n" +
                "@RequestScoped\npublic class SingletonSessionBean {\n}\n";
        String replaceWithDependentText = "package io.openliberty.sample.jakarta.cdi.sessionbean;\n\n" +
                "import jakarta.ejb.Singleton;\nimport jakarta.enterprise.context.Dependent;\n\n" +
                "// Test case 1: Singleton with invalid scope (RequestScoped) - should report error\n" +
                "@Dependent\n@Singleton\npublic class SingletonSessionBean {\n}\n";

        TextEdit removeSingleton = te(0, 0, 10, 0, removeSingletonText);
        TextEdit replaceWithDependent = te(0, 0, 10, 0, replaceWithDependentText);
        CodeAction removeSingletonAction = ca(uri, "Remove @Singleton", illegalRequestScope, removeSingleton);
        CodeAction replaceWithDependentAction = ca(uri, "Replace current scope with @Dependent", illegalRequestScope, replaceWithDependent);
        assertJavaCodeAction(codeActionParams, utils, removeSingletonAction, replaceWithDependentAction);
    }

    /**
     * SingletonWithSessionScope.java — line 8: "public class SingletonWithSessionScope {" → col 13..38
     */
    @Test
    public void testSingletonWithSessionScope() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
        String uri = getUri(module, "SingletonWithSessionScope.java");

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        JsonArray dataSessionScoped = new JsonArray();
        dataSessionScoped.add("jakarta.enterprise.context.SessionScoped");
        Diagnostic illegalSessionScope = d(8, 13, 38,
                "A singleton session bean must be annotated with either @ApplicationScoped or @Dependent.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidSingletonSessionBeanScope", dataSessionScoped);

        assertJavaDiagnostics(diagnosticsParams, utils, illegalSessionScope);

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, illegalSessionScope);

        String removeSingletonText = "package io.openliberty.sample.jakarta.cdi.sessionbean;\n\n" +
                "import jakarta.ejb.Singleton;\nimport jakarta.enterprise.context.SessionScoped;\n\n" +
                "// Test case 2: Singleton with invalid scope (SessionScoped) - should report error\n" +
                "@SessionScoped\npublic class SingletonWithSessionScope {\n}\n";
        String replaceWithDependentText = "package io.openliberty.sample.jakarta.cdi.sessionbean;\n\n" +
                "import jakarta.ejb.Singleton;\nimport jakarta.enterprise.context.Dependent;\n\n" +
                "// Test case 2: Singleton with invalid scope (SessionScoped) - should report error\n" +
                "@Dependent\n@Singleton\npublic class SingletonWithSessionScope {\n}\n";

        TextEdit removeSingleton = te(0, 0, 10, 0, removeSingletonText);
        TextEdit replaceWithDependent = te(0, 0, 10, 0, replaceWithDependentText);
        CodeAction removeSingletonAction = ca(uri, "Remove @Singleton", illegalSessionScope, removeSingleton);
        CodeAction replaceWithDependentAction = ca(uri, "Replace current scope with @Dependent", illegalSessionScope, replaceWithDependent);
        assertJavaCodeAction(codeActionParams, utils, removeSingletonAction, replaceWithDependentAction);
    }

    /**
     * SingletonWithMixedInvalidAndApplicationScoped.java — line 10 → col 13..58
     */
    @Test
    public void testSingletonWithMixedInvalidAndApplicationScoped() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
        String uri = getUri(module, "SingletonWithMixedInvalidAndApplicationScoped.java");

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        JsonArray dataInvalidScope = new JsonArray();
        dataInvalidScope.add("jakarta.enterprise.context.ApplicationScoped");
        dataInvalidScope.add("jakarta.enterprise.context.RequestScoped");
        Diagnostic illegalScopeWithApplication = d(10, 13, 58,
                "A singleton session bean must be annotated with either @ApplicationScoped or @Dependent.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidSingletonSessionBeanScope", dataInvalidScope);

        JsonArray dataMultipleScopes = new JsonArray();
        dataMultipleScopes.add("jakarta.enterprise.context.ApplicationScoped");
        dataMultipleScopes.add("jakarta.enterprise.context.RequestScoped");
        Diagnostic multipleScopesDecl = d(10, 13, 58,
                "Scope type annotations must be specified by a managed bean class at most once.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidScopeDecl", dataMultipleScopes);

        assertJavaDiagnostics(diagnosticsParams, utils, illegalScopeWithApplication, multipleScopesDecl);

        // Quick fix for InvalidScopeDecl: remove one of the conflicting scopes.
        // SCOPE_FQ_NAMES is a HashSet, so managedBeanAnnotations order is hash-based.
        // Actual observed order: ApplicationScoped removed first, RequestScoped removed second.
        // The quick fix does not remove unused imports, so both imports are retained in both actions.
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, multipleScopesDecl);

        String removeApplicationScopedText = "package io.openliberty.sample.jakarta.cdi.sessionbean;\n\n" +
                "import jakarta.ejb.Singleton;\nimport jakarta.enterprise.context.RequestScoped;\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n\n" +
                "// Test case 6: Singleton with mixed valid and invalid scopes (RequestScoped + ApplicationScoped) - should report error\n" +
                "@Singleton\n@RequestScoped\npublic class SingletonWithMixedInvalidAndApplicationScoped {\n}\n";
        String removeRequestScopedText = "package io.openliberty.sample.jakarta.cdi.sessionbean;\n\n" +
                "import jakarta.ejb.Singleton;\nimport jakarta.enterprise.context.RequestScoped;\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n\n" +
                "// Test case 6: Singleton with mixed valid and invalid scopes (RequestScoped + ApplicationScoped) - should report error\n" +
                "@Singleton\n@ApplicationScoped\npublic class SingletonWithMixedInvalidAndApplicationScoped {\n}\n";

        TextEdit removeApplicationScoped = te(0, 0, 12, 0, removeApplicationScopedText);
        TextEdit removeRequestScoped = te(0, 0, 12, 0, removeRequestScopedText);
        CodeAction removeApplicationScopedAction = ca(uri, "Remove @ApplicationScoped", multipleScopesDecl, removeApplicationScoped);
        CodeAction removeRequestScopedAction = ca(uri, "Remove @RequestScoped", multipleScopesDecl, removeRequestScoped);
        assertJavaCodeAction(codeActionParams, utils, removeApplicationScopedAction, removeRequestScopedAction);
    }

    /**
     * SingletonWithMixedInvalidAndDependent.java — line 10 → col 13..50
     */
    @Test
    public void testSingletonWithMixedInvalidAndDependent() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
        String uri = getUri(module, "SingletonWithMixedInvalidAndDependent.java");

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        JsonArray dataInvalidScope = new JsonArray();
        dataInvalidScope.add("jakarta.enterprise.context.Dependent");
        dataInvalidScope.add("jakarta.enterprise.context.SessionScoped");
        Diagnostic illegalScopeWithDependent = d(10, 13, 50,
                "A singleton session bean must be annotated with either @ApplicationScoped or @Dependent.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidSingletonSessionBeanScope", dataInvalidScope);

        JsonArray dataMultipleScopes = new JsonArray();
        dataMultipleScopes.add("jakarta.enterprise.context.Dependent");
        dataMultipleScopes.add("jakarta.enterprise.context.SessionScoped");
        Diagnostic multipleScopesDecl = d(10, 13, 50,
                "Scope type annotations must be specified by a managed bean class at most once.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidScopeDecl", dataMultipleScopes);

        assertJavaDiagnostics(diagnosticsParams, utils, illegalScopeWithDependent, multipleScopesDecl);

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, multipleScopesDecl);

        String removeDependentText = "package io.openliberty.sample.jakarta.cdi.sessionbean;\n\n" +
                "import jakarta.ejb.Singleton;\nimport jakarta.enterprise.context.SessionScoped;\n" +
                "import jakarta.enterprise.context.Dependent;\n\n" +
                "// Test case 7: Singleton with mixed valid and invalid scopes (SessionScoped + Dependent) - should report error\n" +
                "@Singleton\n@SessionScoped\npublic class SingletonWithMixedInvalidAndDependent {\n}\n";
        String removeSessionScopedText = "package io.openliberty.sample.jakarta.cdi.sessionbean;\n\n" +
                "import jakarta.ejb.Singleton;\nimport jakarta.enterprise.context.SessionScoped;\n" +
                "import jakarta.enterprise.context.Dependent;\n\n" +
                "// Test case 7: Singleton with mixed valid and invalid scopes (SessionScoped + Dependent) - should report error\n" +
                "@Singleton\n@Dependent\npublic class SingletonWithMixedInvalidAndDependent {\n}\n";

        TextEdit removeDependent = te(0, 0, 12, 0, removeDependentText);
        TextEdit removeSessionScoped = te(0, 0, 12, 0, removeSessionScopedText);
        CodeAction removeDependentAction = ca(uri, "Remove @Dependent", multipleScopesDecl, removeDependent);
        CodeAction removeSessionScopedAction = ca(uri, "Remove @SessionScoped", multipleScopesDecl, removeSessionScoped);
        assertJavaCodeAction(codeActionParams, utils, removeDependentAction, removeSessionScopedAction);
    }

    /**
     * SingletonWithBothValidScopes.java — line 10 → col 13..41
     */
    @Test
    public void testSingletonWithBothValidScopes() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
        String uri = getUri(module, "SingletonWithBothValidScopes.java");

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        JsonArray dataMultipleScopes = new JsonArray();
        dataMultipleScopes.add("jakarta.enterprise.context.Dependent");
        dataMultipleScopes.add("jakarta.enterprise.context.ApplicationScoped");
        Diagnostic multipleScopesDecl = d(10, 13, 41,
                "Scope type annotations must be specified by a managed bean class at most once.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidScopeDecl", dataMultipleScopes);

        assertJavaDiagnostics(diagnosticsParams, utils, multipleScopesDecl);

        // Quick fix for InvalidScopeDecl: remove one of the conflicting scopes.
        // Diagnostic data order: [Dependent, ApplicationScoped].
        // Loop iteration 1: keep Dependent, remove ApplicationScoped → "Remove @ApplicationScoped"
        // Loop iteration 2: keep ApplicationScoped, remove Dependent → "Remove @Dependent"
        // The quick fix does not remove unused imports, so both imports are retained in both actions.
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, multipleScopesDecl);

        String removeApplicationScopedText = "package io.openliberty.sample.jakarta.cdi.sessionbean;\n\n" +
                "import jakarta.ejb.Singleton;\nimport jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.enterprise.context.Dependent;\n\n" +
                "// Test case 8: Singleton with both valid scopes (ApplicationScoped + Dependent) - should NOT report error\n" +
                "@Singleton\n@Dependent\npublic class SingletonWithBothValidScopes {\n}\n";
        String removeDependentText = "package io.openliberty.sample.jakarta.cdi.sessionbean;\n\n" +
                "import jakarta.ejb.Singleton;\nimport jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.enterprise.context.Dependent;\n\n" +
                "// Test case 8: Singleton with both valid scopes (ApplicationScoped + Dependent) - should NOT report error\n" +
                "@Singleton\n@ApplicationScoped\npublic class SingletonWithBothValidScopes {\n}\n";

        TextEdit removeApplicationScoped = te(0, 0, 12, 0, removeApplicationScopedText);
        TextEdit removeDependent = te(0, 0, 12, 0, removeDependentText);
        CodeAction removeApplicationScopedAction = ca(uri, "Remove @ApplicationScoped", multipleScopesDecl, removeApplicationScoped);
        CodeAction removeDependentAction = ca(uri, "Remove @Dependent", multipleScopesDecl, removeDependent);
        assertJavaCodeAction(codeActionParams, utils, removeApplicationScopedAction, removeDependentAction);
    }

    /**
     * Valid singleton beans — no diagnostics expected.
     */
    @Test
    public void testValidSingletonBeans() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        for (String fileName : new String[]{
                "SingletonWithApplicationScope.java",
                "SingletonWithDependent.java",
                "SingletonWithNoScope.java"}) {
            JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
            diagnosticsParams.setUris(Arrays.asList(getUri(module, fileName)));
            assertJavaDiagnostics(diagnosticsParams, utils);
        }
    }
}
