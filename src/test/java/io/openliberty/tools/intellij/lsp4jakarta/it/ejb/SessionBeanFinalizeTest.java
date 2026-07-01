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

package io.openliberty.tools.intellij.lsp4jakarta.it.ejb;

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
public class SessionBeanFinalizeTest extends BaseJakartaTest {

    @Test
    public void testInvalidStatelessBeanFinalize() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/InvalidStatelessBeanFinalize.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test expected diagnostic
        Diagnostic expectedDiagnostic = d(18, 19, 27,
                "Session bean classes must not override or define the finalize() method.",
                DiagnosticSeverity.Error, "jakarta-ejb", "SessionBeanFinalizeMethod");

        assertJavaDiagnostics(diagnosticsParams, utils, expectedDiagnostic);

        // Test expected quick-fix
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, expectedDiagnostic);
        String newText = "package io.openliberty.sample.jakarta.ejb;" +
                "\n\nimport jakarta.ejb.Stateless;" +
                "\n\n@Stateless" +
                "\npublic class InvalidStatelessBeanFinalize {" +
                "\n    private String data;" +
                "\n\n    public String getData() {" +
                "\n        return data;" +
                "\n    }" +
                "\n\n    public void setData(String data) {" +
                "\n        this.data = data;" +
                "\n    }" +
                "\n\n}";
        TextEdit expectedTextEdit = te(0, 0, 22, 1, newText);
        CodeAction expectedCodeAction = ca(uri, "Remove the finalize() method", expectedDiagnostic, expectedTextEdit);
        assertJavaCodeAction(codeActionParams, utils, expectedCodeAction);
    }

    @Test
    public void testInvalidStatefulBeanFinalize() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/InvalidStatefulBeanFinalize.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test expected diagnostic
        Diagnostic expectedDiagnostic = d(18, 19, 27,
                "Session bean classes must not override or define the finalize() method.",
                DiagnosticSeverity.Error, "jakarta-ejb", "SessionBeanFinalizeMethod");

        assertJavaDiagnostics(diagnosticsParams, utils, expectedDiagnostic);

        // Test expected quick-fix
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, expectedDiagnostic);
        String newText = "package io.openliberty.sample.jakarta.ejb;" +
                "\n\nimport jakarta.ejb.Stateful;" +
                "\n\n@Stateful" +
                "\npublic class InvalidStatefulBeanFinalize {" +
                "\n    private int count;" +
                "\n\n    public int getCount() {" +
                "\n        return count;" +
                "\n    }" +
                "\n\n    public void setCount(int count) {" +
                "\n        this.count = count;" +
                "\n    }" +
                "\n\n}";
        TextEdit expectedTextEdit = te(0, 0, 22, 1, newText);
        CodeAction expectedCodeAction = ca(uri, "Remove the finalize() method", expectedDiagnostic, expectedTextEdit);
        assertJavaCodeAction(codeActionParams, utils, expectedCodeAction);
    }

    @Test
    public void testInvalidSingletonBeanFinalize() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/InvalidSingletonBeanFinalize.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test expected diagnostic
        Diagnostic expectedDiagnostic = d(18, 19, 27,
                "Session bean classes must not override or define the finalize() method.",
                DiagnosticSeverity.Error, "jakarta-ejb", "SessionBeanFinalizeMethod");

        assertJavaDiagnostics(diagnosticsParams, utils, expectedDiagnostic);

        // Test expected quick-fix
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, expectedDiagnostic);
        String newText = "package io.openliberty.sample.jakarta.ejb;" +
                "\n\nimport jakarta.ejb.Singleton;" +
                "\n\n@Singleton" +
                "\npublic class InvalidSingletonBeanFinalize {" +
                "\n    private String config;" +
                "\n\n    public String getConfig() {" +
                "\n        return config;" +
                "\n    }" +
                "\n\n    public void setConfig(String config) {" +
                "\n        this.config = config;" +
                "\n    }" +
                "\n\n}";
        TextEdit expectedTextEdit = te(0, 0, 22, 1, newText);
        CodeAction expectedCodeAction = ca(uri, "Remove the finalize() method", expectedDiagnostic, expectedTextEdit);
        assertJavaCodeAction(codeActionParams, utils, expectedCodeAction);
    }

    @Test
    public void testValidStatelessBeanNoFinalize() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/ValidStatelessBeanNoFinalize.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Should not report any diagnostics for valid bean without finalize()
        assertJavaDiagnostics(diagnosticsParams, utils);
    }
}