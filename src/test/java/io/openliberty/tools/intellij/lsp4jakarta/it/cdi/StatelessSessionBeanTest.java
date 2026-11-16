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

    private static final String CDI_SESSIONBEAN_PATH =
            "/src/main/java/io/openliberty/sample/jakarta/cdi/sessionbean/";

    private String getUri(Module module, String fileName) {
        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + CDI_SESSIONBEAN_PATH + fileName);
        return VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();
    }

    /**
     * StatelessSessionBean.java — line 8: "public class StatelessSessionBean {" → col 13..33
     */
    @Test
    public void testStatelessWithRequestScoped() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
        String uri = getUri(module, "StatelessSessionBean.java");

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic illegalRequestScope = d(8, 13, 33,
                "A stateless session bean belongs to the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidStatelessSessionBeanScope");

        assertJavaDiagnostics(diagnosticsParams, utils, illegalRequestScope);

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, illegalRequestScope);

        String removeText = "package io.openliberty.sample.jakarta.cdi.sessionbean;\n\n" +
                "import jakarta.ejb.Stateless;\nimport jakarta.enterprise.context.RequestScoped;\n\n" +
                "// Invalid: Stateless with RequestScoped\n" +
                "@RequestScoped\npublic class StatelessSessionBean {\n}\n";
        String replaceText = "package io.openliberty.sample.jakarta.cdi.sessionbean;\n\n" +
                "import jakarta.ejb.Stateless;\nimport jakarta.enterprise.context.Dependent;\n\n" +
                "// Invalid: Stateless with RequestScoped\n" +
                "@Dependent\n@Stateless\npublic class StatelessSessionBean {\n}\n";

        TextEdit removeStateless = te(0, 0, 10, 0, removeText);
        TextEdit replaceWithDependent = te(0, 0, 10, 0, replaceText);
        CodeAction removeStatelessAction = ca(uri, "Remove @Stateless", illegalRequestScope, removeStateless);
        CodeAction replaceWithDependentAction = ca(uri, "Replace current scope with @Dependent", illegalRequestScope, replaceWithDependent);
        assertJavaCodeAction(codeActionParams, utils, removeStatelessAction, replaceWithDependentAction);
    }

    /**
     * StatelessWithSessionScoped.java — line 8: "public class StatelessWithSessionScoped {" → col 13..39
     */
    @Test
    public void testStatelessWithSessionScoped() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
        String uri = getUri(module, "StatelessWithSessionScoped.java");

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic illegalSessionScope = d(8, 13, 39,
                "A stateless session bean belongs to the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidStatelessSessionBeanScope");

        assertJavaDiagnostics(diagnosticsParams, utils, illegalSessionScope);

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, illegalSessionScope);

        String removeText = "package io.openliberty.sample.jakarta.cdi.sessionbean;\n\n" +
                "import jakarta.ejb.Stateless;\nimport jakarta.enterprise.context.SessionScoped;\n\n" +
                "// Invalid: Stateless with SessionScoped\n" +
                "@SessionScoped\npublic class StatelessWithSessionScoped {\n}\n";
        String replaceText = "package io.openliberty.sample.jakarta.cdi.sessionbean;\n\n" +
                "import jakarta.ejb.Stateless;\nimport jakarta.enterprise.context.Dependent;\n\n" +
                "// Invalid: Stateless with SessionScoped\n" +
                "@Dependent\n@Stateless\npublic class StatelessWithSessionScoped {\n}\n";

        TextEdit removeStateless = te(0, 0, 10, 0, removeText);
        TextEdit replaceWithDependent = te(0, 0, 10, 0, replaceText);
        CodeAction removeStatelessAction = ca(uri, "Remove @Stateless", illegalSessionScope, removeStateless);
        CodeAction replaceWithDependentAction = ca(uri, "Replace current scope with @Dependent", illegalSessionScope, replaceWithDependent);
        assertJavaCodeAction(codeActionParams, utils, removeStatelessAction, replaceWithDependentAction);
    }

    /**
     * StatelessWithMultipleScopes.java — line 10: "public class StatelessWithMultipleScopes {" → col 13..40
     */
    @Test
    public void testStatelessWithMultipleScopes() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
        String uri = getUri(module, "StatelessWithMultipleScopes.java");

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic illegalMultipleScopes = d(10, 13, 40,
                "A stateless session bean belongs to the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidStatelessSessionBeanScope");

        assertJavaDiagnostics(diagnosticsParams, utils, illegalMultipleScopes);

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, illegalMultipleScopes);

        String removeText = "package io.openliberty.sample.jakarta.cdi.sessionbean;\n\n" +
                "import jakarta.ejb.Stateless;\nimport jakarta.enterprise.context.Dependent;\n" +
                "import jakarta.enterprise.context.RequestScoped;\n\n" +
                "// Invalid: Stateless with multiple scopes including Dependent\n" +
                "@Dependent\n@RequestScoped\npublic class StatelessWithMultipleScopes {\n}\n";
        String replaceText = "package io.openliberty.sample.jakarta.cdi.sessionbean;\n\n" +
                "import jakarta.ejb.Stateless;\nimport jakarta.enterprise.context.Dependent;\n\n" +
                "// Invalid: Stateless with multiple scopes including Dependent\n" +
                "@Dependent\n@Stateless\npublic class StatelessWithMultipleScopes {\n}\n";

        TextEdit removeStateless = te(0, 0, 12, 0, removeText);
        TextEdit replaceWithDependent = te(0, 0, 12, 0, replaceText);
        CodeAction removeStatelessAction = ca(uri, "Remove @Stateless", illegalMultipleScopes, removeStateless);
        CodeAction replaceWithDependentAction = ca(uri, "Replace current scope with @Dependent", illegalMultipleScopes, replaceWithDependent);
        assertJavaCodeAction(codeActionParams, utils, removeStatelessAction, replaceWithDependentAction);
    }

    /**
     * StatelessWithNoScope.java and StatelessWithDependent.java — no diagnostics expected.
     */
    @Test
    public void testStatelessValidBeans() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        for (String fileName : new String[]{"StatelessWithNoScope.java", "StatelessWithDependent.java"}) {
            JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
            diagnosticsParams.setUris(Arrays.asList(getUri(module, fileName)));
            assertJavaDiagnostics(diagnosticsParams, utils);
        }
    }
}
