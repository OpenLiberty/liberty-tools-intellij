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

import com.google.gson.Gson;
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

    @Test
    public void testValidStatelessBeanNoConstructor() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/ValidStatelessBeanNoConstructor.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No diagnostic expected: a class with no explicit constructors has a default public no-arg constructor
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void testConflictingStatelessStateful() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/InvalidConflictingStatelessStateful.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic conflictingDiagnostic = d(7, 13, 48,
                "A class cannot be annotated with multiple session bean types: @Stateless, @Stateful.",
                DiagnosticSeverity.Error, "jakarta-ejb", "ConflictingSessionBeanAnnotations");
        conflictingDiagnostic.setData(new Gson().toJsonTree(Arrays.asList("jakarta.ejb.Stateless", "jakarta.ejb.Stateful")));

        assertJavaDiagnostics(diagnosticsParams, utils, conflictingDiagnostic);

        // Test quick-fix code actions: one action per annotation to keep
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, conflictingDiagnostic);

        // Keep @Stateless, remove @Stateful (line 6, 0-indexed)
        String keepStatelessText = "package io.openliberty.sample.jakarta.ejb;" +
                "\n\nimport jakarta.ejb.Stateful;" +
                "\nimport jakarta.ejb.Stateless;" +
                "\n\n@Stateless" +
                "\npublic class InvalidConflictingStatelessStateful {" +
                "\n\n    public InvalidConflictingStatelessStateful() {" +
                "\n    }" +
                "\n\n    public String hello() {" +
                "\n        return \"Hello\";" +
                "\n    }" +
                "\n}\n";
        TextEdit removeStatefulEdit = te(0, 0, 16, 0, keepStatelessText);
        CodeAction keepStatelessAction = ca(uri, "Remove @Stateful", conflictingDiagnostic, removeStatefulEdit);

        // Keep @Stateful, remove @Stateless (line 5, 0-indexed)
        String keepStatefulText = "package io.openliberty.sample.jakarta.ejb;" +
                "\n\nimport jakarta.ejb.Stateful;" +
                "\nimport jakarta.ejb.Stateless;" +
                "\n\n@Stateful" +
                "\npublic class InvalidConflictingStatelessStateful {" +
                "\n\n    public InvalidConflictingStatelessStateful() {" +
                "\n    }" +
                "\n\n    public String hello() {" +
                "\n        return \"Hello\";" +
                "\n    }" +
                "\n}\n";
        TextEdit removeStatelessEdit = te(0, 0, 16, 0, keepStatefulText);
        CodeAction keepStatefulAction = ca(uri, "Remove @Stateless", conflictingDiagnostic, removeStatelessEdit);

        assertJavaCodeAction(codeActionParams, utils, keepStatelessAction, keepStatefulAction);
    }

    @Test
    public void testConflictingStatelessSingleton() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/InvalidConflictingStatelessSingleton.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic conflictingDiagnostic = d(7, 13, 49,
                "A class cannot be annotated with multiple session bean types: @Stateless, @Singleton.",
                DiagnosticSeverity.Error, "jakarta-ejb", "ConflictingSessionBeanAnnotations");
        conflictingDiagnostic.setData(new Gson().toJsonTree(Arrays.asList("jakarta.ejb.Stateless", "jakarta.ejb.Singleton")));

        assertJavaDiagnostics(diagnosticsParams, utils, conflictingDiagnostic);

        // Test quick-fix code actions
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, conflictingDiagnostic);

        // Keep @Stateless, remove @Singleton (line 6, 0-indexed)
        String keepStatelessText = "package io.openliberty.sample.jakarta.ejb;" +
                "\n\nimport jakarta.ejb.Singleton;" +
                "\nimport jakarta.ejb.Stateless;" +
                "\n\n@Stateless" +
                "\npublic class InvalidConflictingStatelessSingleton {" +
                "\n\n    public InvalidConflictingStatelessSingleton() {" +
                "\n    }" +
                "\n\n    public String hello() {" +
                "\n        return \"Hello\";" +
                "\n    }" +
                "\n}\n";
        TextEdit removeSingletonEdit = te(0, 0, 16, 0, keepStatelessText);
        CodeAction keepStatelessAction = ca(uri, "Remove @Singleton", conflictingDiagnostic, removeSingletonEdit);

        // Keep @Singleton, remove @Stateless (line 5, 0-indexed)
        String keepSingletonText = "package io.openliberty.sample.jakarta.ejb;" +
                "\n\nimport jakarta.ejb.Singleton;" +
                "\nimport jakarta.ejb.Stateless;" +
                "\n\n@Singleton" +
                "\npublic class InvalidConflictingStatelessSingleton {" +
                "\n\n    public InvalidConflictingStatelessSingleton() {" +
                "\n    }" +
                "\n\n    public String hello() {" +
                "\n        return \"Hello\";" +
                "\n    }" +
                "\n}\n";
        TextEdit removeStatelessEdit = te(0, 0, 16, 0, keepSingletonText);
        CodeAction keepSingletonAction = ca(uri, "Remove @Stateless", conflictingDiagnostic, removeStatelessEdit);

        assertJavaCodeAction(codeActionParams, utils, keepStatelessAction, keepSingletonAction);
    }

    @Test
    public void testConflictingStatefulSingleton() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/InvalidConflictingStatefulSingleton.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic conflictingDiagnostic = d(7, 13, 48,
                "A class cannot be annotated with multiple session bean types: @Stateful, @Singleton.",
                DiagnosticSeverity.Error, "jakarta-ejb", "ConflictingSessionBeanAnnotations");
        conflictingDiagnostic.setData(new Gson().toJsonTree(Arrays.asList("jakarta.ejb.Stateful", "jakarta.ejb.Singleton")));

        assertJavaDiagnostics(diagnosticsParams, utils, conflictingDiagnostic);

        // Test quick-fix code actions
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, conflictingDiagnostic);

        // Keep @Stateful, remove @Singleton (line 6, 0-indexed)
        String keepStatefulText = "package io.openliberty.sample.jakarta.ejb;" +
                "\n\nimport jakarta.ejb.Singleton;" +
                "\nimport jakarta.ejb.Stateful;" +
                "\n\n@Stateful" +
                "\npublic class InvalidConflictingStatefulSingleton {" +
                "\n\n    public InvalidConflictingStatefulSingleton() {" +
                "\n    }" +
                "\n\n    public String hello() {" +
                "\n        return \"Hello\";" +
                "\n    }" +
                "\n}\n";
        TextEdit removeSingletonEdit = te(0, 0, 16, 0, keepStatefulText);
        CodeAction keepStatefulAction = ca(uri, "Remove @Singleton", conflictingDiagnostic, removeSingletonEdit);

        // Keep @Singleton, remove @Stateful (line 5, 0-indexed)
        String keepSingletonText = "package io.openliberty.sample.jakarta.ejb;" +
                "\n\nimport jakarta.ejb.Singleton;" +
                "\nimport jakarta.ejb.Stateful;" +
                "\n\n@Singleton" +
                "\npublic class InvalidConflictingStatefulSingleton {" +
                "\n\n    public InvalidConflictingStatefulSingleton() {" +
                "\n    }" +
                "\n\n    public String hello() {" +
                "\n        return \"Hello\";" +
                "\n    }" +
                "\n}\n";
        TextEdit removeStatefulEdit = te(0, 0, 16, 0, keepSingletonText);
        CodeAction keepSingletonAction = ca(uri, "Remove @Stateful", conflictingDiagnostic, removeStatefulEdit);

        assertJavaCodeAction(codeActionParams, utils, keepStatefulAction, keepSingletonAction);
    }
}