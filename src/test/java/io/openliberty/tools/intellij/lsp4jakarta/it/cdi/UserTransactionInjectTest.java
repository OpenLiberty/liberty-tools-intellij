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

/**
 * Tests for CDI diagnostic: injection of UserTransaction via @Inject in a CDI-managed bean.
 *
 * <p>Per CDI 3.0 specification §18.8 (additional built-in beans), UserTransaction may only be
 * injected using {@code @Resource} in servlets and application clients. Injecting it via
 * {@code @Inject} with the {@code @Default} qualifier in a CDI-managed bean is a definition error.
 *
 * @see <a href="https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0#additional_builtin_beans">CDI 3.0 §18.8</a>
 */
@RunWith(JUnit4.class)
public class UserTransactionInjectTest extends BaseJakartaTest {

    private static final String DIAG_MSG =
            "The @Inject annotation must not be used to inject UserTransaction in a CDI-managed bean.";

    @Test
    public void userTransactionInjectedInCdiBean() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/UserTransactionInjectedInCdiBean.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic appScopedFieldInject = d(13, 28, 43,
                DIAG_MSG,
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidUserTransactionInjectionInCDIBean");

        Diagnostic requestScopedFieldInject = d(25, 28, 30,
                DIAG_MSG,
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidUserTransactionInjectionInCDIBean");

        Diagnostic sessionScopedFieldInject = d(37, 28, 37,
                DIAG_MSG,
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidUserTransactionInjectionInCDIBean");

        Diagnostic initializerMethodParamInject = d(49, 16, 20,
                DIAG_MSG,
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidUserTransactionInjectionInCDIBean");

        assertJavaDiagnostics(diagnosticsParams, utils,
                appScopedFieldInject, requestScopedFieldInject,
                sessionScopedFieldInject, initializerMethodParamInject);

        String afterRemoveInjectOnAppScopedField =
                "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.enterprise.context.RequestScoped;\n" +
                "import jakarta.enterprise.context.SessionScoped;\n" +
                "import jakarta.inject.Inject;\n" +
                "import jakarta.transaction.UserTransaction;\n\n" +
                "// Invalid: injecting UserTransaction via @Inject field in an @ApplicationScoped CDI bean\n" +
                "@ApplicationScoped\n" +
                "public class UserTransactionInjectedInCdiBean {\n\n" +
                "    private UserTransaction userTransaction;\n\n" +
                "    public void doWork() {\n" +
                "        // business logic\n" +
                "    }\n" +
                "}\n\n" +
                "// Invalid: injecting UserTransaction via @Inject field in a @RequestScoped CDI bean\n" +
                "@RequestScoped\n" +
                "class AnotherTransactionalBean {\n\n" +
                "    @Inject\n" +
                "    private UserTransaction tx;\n\n" +
                "    public void process() {\n" +
                "        // business logic\n" +
                "    }\n" +
                "}\n\n" +
                "// Invalid: injecting UserTransaction via @Inject field in a @SessionScoped CDI bean\n" +
                "@SessionScoped\n" +
                "class SessionTransactionalBean implements java.io.Serializable {\n\n" +
                "    @Inject\n" +
                "    private UserTransaction sessionTx;\n\n" +
                "    public void execute() {\n" +
                "        // business logic\n" +
                "    }\n" +
                "}\n\n" +
                "// Invalid: injecting UserTransaction via @Inject initializer method parameter in a CDI bean\n" +
                "@ApplicationScoped\n" +
                "class BeanWithInjectMethod {\n\n" +
                "    @Inject\n" +
                "    public void init(UserTransaction ut) {\n" +
                "        // initializer\n" +
                "    }\n" +
                "}\n";

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, appScopedFieldInject);
        TextEdit removeInjectEdit = te(0, 0, 53, 0, afterRemoveInjectOnAppScopedField);
        CodeAction removeInjectAction = ca(uri, "Remove @Inject", appScopedFieldInject, removeInjectEdit);
        assertJavaCodeAction(codeActionParams, utils, removeInjectAction);
    }

    @Test
    public void validUserTransactionUsage() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/ValidUserTransactionUsage.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }
}
