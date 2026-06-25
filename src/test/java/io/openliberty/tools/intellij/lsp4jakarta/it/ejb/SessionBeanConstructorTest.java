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
    public void testInvalidStatelessBeanPrivate() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/InvalidStatelessBeanPrivate.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test expected diagnostic
        Diagnostic expectedDiagnostic = d(5, 13, 40,
                "Session beans must have a public no-arg constructor.",
                DiagnosticSeverity.Error, "jakarta-ejb", "MissingPublicNoArgConstructor");

        assertJavaDiagnostics(diagnosticsParams, utils, expectedDiagnostic);

        // Test expected quick-fix
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, expectedDiagnostic);
        String newText = "package io.openliberty.sample.jakarta.ejb;" +
                "\n\nimport jakarta.ejb.Stateless;" +
                "\n\n@Stateless" +
                "\npublic class InvalidStatelessBeanPrivate {" +
                "\n    private String name;" +
                "\n\n    public InvalidStatelessBeanPrivate() {" +
                "\n    }" +
                "\n\n    // Private constructor - should trigger diagnostic" +
                "\n    private InvalidStatelessBeanPrivate(String name) {" +
                "\n        this.name = name;" +
                "\n    }" +
                "\n\n    public String getName() {" +
                "\n        return name;" +
                "\n    }" +
                "\n}\n";
        TextEdit expectedTextEdit = te(0, 0, 17, 0, newText);
        CodeAction expectedCodeAction = ca(uri, "Add a no-arg public constructor to this class", expectedDiagnostic, expectedTextEdit);
        assertJavaCodeAction(codeActionParams, utils, expectedCodeAction);
    }

    @Test
    public void testInvalidStatefulBeanPrivate() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/InvalidStatefulBeanPrivate.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test expected diagnostic
        Diagnostic expectedDiagnostic = d(5, 13, 39,
                "Session beans must have a public no-arg constructor.",
                DiagnosticSeverity.Error, "jakarta-ejb", "MissingPublicNoArgConstructor");

        assertJavaDiagnostics(diagnosticsParams, utils, expectedDiagnostic);

        // Test expected quick-fix
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, expectedDiagnostic);
        String newText = "package io.openliberty.sample.jakarta.ejb;" +
                "\n\nimport jakarta.ejb.Stateful;" +
                "\n\n@Stateful" +
                "\npublic class InvalidStatefulBeanPrivate {" +
                "\n    private int count;" +
                "\n\n    public InvalidStatefulBeanPrivate() {" +
                "\n    }" +
                "\n\n    // Private constructor - should trigger diagnostic" +
                "\n    private InvalidStatefulBeanPrivate(int count) {" +
                "\n        this.count = count;" +
                "\n    }" +
                "\n\n    public int getCount() {" +
                "\n        return count;" +
                "\n    }" +
                "\n}\n";
        TextEdit expectedTextEdit = te(0, 0, 17, 0, newText);
        CodeAction expectedCodeAction = ca(uri, "Add a no-arg public constructor to this class", expectedDiagnostic, expectedTextEdit);
        assertJavaCodeAction(codeActionParams, utils, expectedCodeAction);
    }

    @Test
    public void testInvalidSingletonBeanPrivate() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/InvalidSingletonBeanPrivate.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test expected diagnostic
        Diagnostic expectedDiagnostic = d(5, 13, 40,
                "Session beans must have a public no-arg constructor.",
                DiagnosticSeverity.Error, "jakarta-ejb", "MissingPublicNoArgConstructor");

        assertJavaDiagnostics(diagnosticsParams, utils, expectedDiagnostic);

        // Test expected quick-fix
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, expectedDiagnostic);
        String newText = "package io.openliberty.sample.jakarta.ejb;" +
                "\n\nimport jakarta.ejb.Singleton;" +
                "\n\n@Singleton" +
                "\npublic class InvalidSingletonBeanPrivate {" +
                "\n    private String config;" +
                "\n\n    public InvalidSingletonBeanPrivate() {" +
                "\n    }" +
                "\n\n    // Private constructor - should trigger diagnostic" +
                "\n    private InvalidSingletonBeanPrivate(String config) {" +
                "\n        this.config = config;" +
                "\n    }" +
                "\n\n    public String getConfig() {" +
                "\n        return config;" +
                "\n    }" +
                "\n}\n";
        TextEdit expectedTextEdit = te(0, 0, 17, 0, newText);
        CodeAction expectedCodeAction = ca(uri, "Add a no-arg public constructor to this class", expectedDiagnostic, expectedTextEdit);
        assertJavaCodeAction(codeActionParams, utils, expectedCodeAction);
    }
    @Test
    public void testInvalidStatelessBeanPublic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/InvalidStatelessBeanPublic.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test expected diagnostic
        Diagnostic expectedDiagnostic = d(5, 13, 39,
                "Session beans must have a public no-arg constructor.",
                DiagnosticSeverity.Error, "jakarta-ejb", "MissingPublicNoArgConstructor");

        assertJavaDiagnostics(diagnosticsParams, utils, expectedDiagnostic);

        // Test expected quick-fix
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, expectedDiagnostic);
        String newText = "package io.openliberty.sample.jakarta.ejb;" +
                "\n\nimport jakarta.ejb.Stateless;" +
                "\n\n@Stateless" +
                "\npublic class InvalidStatelessBeanPublic {" +
                "\n    private String name;" +
                "\n\n    public InvalidStatelessBeanPublic() {" +
                "\n    }" +
                "\n\n    // Public parameterized constructor - should trigger diagnostic" +
                "\n    public InvalidStatelessBeanPublic(String name) {" +
                "\n        this.name = name;" +
                "\n    }" +
                "\n\n    public String getName() {" +
                "\n        return name;" +
                "\n    }" +
                "\n}\n";
        TextEdit expectedTextEdit = te(0, 0, 17, 0, newText);
        CodeAction expectedCodeAction = ca(uri, "Add a no-arg public constructor to this class", expectedDiagnostic, expectedTextEdit);
        assertJavaCodeAction(codeActionParams, utils, expectedCodeAction);
    }

    @Test
    public void testInvalidStatefulBeanPublic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/InvalidStatefulBeanPublic.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test expected diagnostic
        Diagnostic expectedDiagnostic = d(5, 13, 38,
                "Session beans must have a public no-arg constructor.",
                DiagnosticSeverity.Error, "jakarta-ejb", "MissingPublicNoArgConstructor");

        assertJavaDiagnostics(diagnosticsParams, utils, expectedDiagnostic);

        // Test expected quick-fix
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, expectedDiagnostic);
        String newText = "package io.openliberty.sample.jakarta.ejb;" +
                "\n\nimport jakarta.ejb.Stateful;" +
                "\n\n@Stateful" +
                "\npublic class InvalidStatefulBeanPublic {" +
                "\n    private int count;" +
                "\n\n    public InvalidStatefulBeanPublic() {" +
                "\n    }" +
                "\n\n    // Public parameterized constructor - should trigger diagnostic" +
                "\n    public InvalidStatefulBeanPublic(int count) {" +
                "\n        this.count = count;" +
                "\n    }" +
                "\n\n    public int getCount() {" +
                "\n        return count;" +
                "\n    }" +
                "\n}\n";
        TextEdit expectedTextEdit = te(0, 0, 17, 0, newText);
        CodeAction expectedCodeAction = ca(uri, "Add a no-arg public constructor to this class", expectedDiagnostic, expectedTextEdit);
        assertJavaCodeAction(codeActionParams, utils, expectedCodeAction);
    }

    @Test
    public void testInvalidSingletonBeanPublic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/InvalidSingletonBeanPublic.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test expected diagnostic
        Diagnostic expectedDiagnostic = d(5, 13, 39,
                "Session beans must have a public no-arg constructor.",
                DiagnosticSeverity.Error, "jakarta-ejb", "MissingPublicNoArgConstructor");

        assertJavaDiagnostics(diagnosticsParams, utils, expectedDiagnostic);

        // Test expected quick-fix
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, expectedDiagnostic);
        String newText = "package io.openliberty.sample.jakarta.ejb;" +
                "\n\nimport jakarta.ejb.Singleton;" +
                "\n\n@Singleton" +
                "\npublic class InvalidSingletonBeanPublic {" +
                "\n    private String config;" +
                "\n\n    public InvalidSingletonBeanPublic() {" +
                "\n    }" +
                "\n\n    // Public parameterized constructor - should trigger diagnostic" +
                "\n    public InvalidSingletonBeanPublic(String config) {" +
                "\n        this.config = config;" +
                "\n    }" +
                "\n\n    public String getConfig() {" +
                "\n        return config;" +
                "\n    }" +
                "\n}\n";
        TextEdit expectedTextEdit = te(0, 0, 17, 0, newText);
        CodeAction expectedCodeAction = ca(uri, "Add a no-arg public constructor to this class", expectedDiagnostic, expectedTextEdit);
        assertJavaCodeAction(codeActionParams, utils, expectedCodeAction);
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