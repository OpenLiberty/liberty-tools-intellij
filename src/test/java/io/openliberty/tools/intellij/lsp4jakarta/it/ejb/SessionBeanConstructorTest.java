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
public class SessionBeanConstructorTest extends BaseJakartaTest {

    @Test
    public void testInvalidStatelessBean() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/InvalidStatelessBean.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test expected diagnostic
        Diagnostic d = d(5, 13, 33,
                "Session beans must have a public no-arg constructor.",
                DiagnosticSeverity.Error, "jakarta-ejb", "MissingPublicNoArgConstructor");

        assertJavaDiagnostics(diagnosticsParams, utils, d);

        // Test expected quick-fix
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);
        String newText = "package io.openliberty.sample.jakarta.ejb;" +
                "\n\nimport jakarta.ejb.Stateless;" +
                "\n\n@Stateless" +
                "\npublic class InvalidStatelessBean {" +
                "\n    private String name;" +
                "\n\n    public InvalidStatelessBean() {" +
                "\n    }" +
                "\n\n    // Private constructor - should trigger diagnostic" +
                "\n    private InvalidStatelessBean(String name) {" +
                "\n        this.name = name;" +
                "\n    }" +
                "\n\n    public String getName() {" +
                "\n        return name;" +
                "\n    }" +
                "\n}";
        TextEdit te = te(0, 0, 16, 1, newText);
        CodeAction ca = ca(uri, "Add a no-arg public constructor to this class", d, te);
        assertJavaCodeAction(codeActionParams, utils, ca);
    }

    @Test
    public void testInvalidStatefulBean() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/InvalidStatefulBean.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test expected diagnostic
        Diagnostic d = d(5, 13, 32,
                "Session beans must have a public no-arg constructor.",
                DiagnosticSeverity.Error, "jakarta-ejb", "MissingPublicNoArgConstructor");

        assertJavaDiagnostics(diagnosticsParams, utils, d);

        // Test expected quick-fix
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);
        String newText = "package io.openliberty.sample.jakarta.ejb;" +
                "\n\nimport jakarta.ejb.Stateful;" +
                "\n\n@Stateful" +
                "\npublic class InvalidStatefulBean {" +
                "\n    private int count;" +
                "\n\n    public InvalidStatefulBean() {" +
                "\n    }" +
                "\n\n    // Private constructor - should trigger diagnostic" +
                "\n    private InvalidStatefulBean(int count) {" +
                "\n        this.count = count;" +
                "\n    }" +
                "\n\n    public int getCount() {" +
                "\n        return count;" +
                "\n    }" +
                "\n}";
        TextEdit te = te(0, 0, 16, 1, newText);
        CodeAction ca = ca(uri, "Add a no-arg public constructor to this class", d, te);
        assertJavaCodeAction(codeActionParams, utils, ca);
    }

    @Test
    public void testInvalidSingletonBean() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/InvalidSingletonBean.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test expected diagnostic
        Diagnostic d = d(5, 13, 33,
                "Session beans must have a public no-arg constructor.",
                DiagnosticSeverity.Error, "jakarta-ejb", "MissingPublicNoArgConstructor");

        assertJavaDiagnostics(diagnosticsParams, utils, d);

        // Test expected quick-fix
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d);
        String newText = "package io.openliberty.sample.jakarta.ejb;" +
                "\n\nimport jakarta.ejb.Singleton;" +
                "\n\n@Singleton" +
                "\npublic class InvalidSingletonBean {" +
                "\n    private String config;" +
                "\n\n    public InvalidSingletonBean() {" +
                "\n    }" +
                "\n\n    // Private constructor - should trigger diagnostic" +
                "\n    private InvalidSingletonBean(String config) {" +
                "\n        this.config = config;" +
                "\n    }" +
                "\n\n    public String getConfig() {" +
                "\n        return config;" +
                "\n    }" +
                "\n}";
        TextEdit te = te(0, 0, 16, 1, newText);
        CodeAction ca = ca(uri, "Add a no-arg public constructor to this class", d, te);
        assertJavaCodeAction(codeActionParams, utils, ca);
    }

    @Test
    public void testValidStatelessBean() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/ValidStatelessBean.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }
}