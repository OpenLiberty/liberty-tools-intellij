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

/**
 * Tests for EJB session synchronization method diagnostics and quickfixes.
 *
 * Validates that methods annotated with @AfterBegin, @BeforeCompletion, or
 * @AfterCompletion must not be declared final or static, and must return void.
 */
@RunWith(JUnit4.class)
public class SessionSyncMethodTest extends BaseJakartaTest {

    private static final String BASE_PATH = "/src/main/java/io/openliberty/sample/jakarta/ejb/session_synchronization_method/";

    // -----------------------------------------------------------------------
    // Diagnostic + QuickFix: @AfterBegin method must not be final
    // -----------------------------------------------------------------------

    @Test
    public void testFinalSessionSyncMethodDiagnosticAndQuickFix() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + BASE_PATH + "InvalidFinalSessionSyncMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 12 (0-based): "    @AfterBegin"
        // Line 13 (0-based): "    public final void beginSync() {"
        // method name "beginSync": "    public final void " = 22 chars (4+7+6+5)
        Diagnostic expectedDiagnostic = d(13, 22, 31,
                "@AfterBegin session synchronization method must not be declared as final.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidSessionSyncMethodFinal");

        assertJavaDiagnostics(diagnosticsParams, utils, expectedDiagnostic);

        // QuickFix: Remove the 'final' modifier
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, expectedDiagnostic);
        String newText = "package io.openliberty.sample.jakarta.ejb.session_synchronization_method;\n"
                + "\n"
                + "import jakarta.ejb.Stateful;\n"
                + "import jakarta.ejb.AfterBegin;\n"
                + "\n"
                + "/**\n"
                + " * Invalid session bean - @AfterBegin method must not be declared final.\n"
                + " */\n"
                + "@Stateful\n"
                + "public class InvalidFinalSessionSyncMethod {\n"
                + "\n"
                + "    // Error: @AfterBegin method must not be declared final\n"
                + "    @AfterBegin\n"
                + "    public void beginSync() {\n"
                + "    }\n"
                + "}\n";
        TextEdit expectedTextEdit = te(0, 0, 16, 0, newText);
        CodeAction expectedCodeAction = ca(uri, "Remove the 'final' modifier from this method",
                expectedDiagnostic, expectedTextEdit);
        assertJavaCodeAction(codeActionParams, utils, expectedCodeAction);
    }

    // -----------------------------------------------------------------------
    // Diagnostic + QuickFix: @BeforeCompletion method must not be static
    // -----------------------------------------------------------------------

    @Test
    public void testStaticSessionSyncMethodDiagnosticAndQuickFix() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + BASE_PATH + "InvalidStaticSessionSyncMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 12 (0-based): "    @BeforeCompletion"
        // Line 13 (0-based): "    public static void beforeCommit() {"
        // method name "beforeCommit": "    public static void " = 23 chars (4+7+7+5)
        Diagnostic expectedDiagnostic = d(13, 23, 35,
                "@BeforeCompletion session synchronization method must not be declared as static.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidSessionSyncMethodStatic");

        assertJavaDiagnostics(diagnosticsParams, utils, expectedDiagnostic);

        // QuickFix: Remove the 'static' modifier
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, expectedDiagnostic);
        String newText = "package io.openliberty.sample.jakarta.ejb.session_synchronization_method;\n"
                + "\n"
                + "import jakarta.ejb.Stateful;\n"
                + "import jakarta.ejb.BeforeCompletion;\n"
                + "\n"
                + "/**\n"
                + " * Invalid session bean - @BeforeCompletion method must not be declared static.\n"
                + " */\n"
                + "@Stateful\n"
                + "public class InvalidStaticSessionSyncMethod {\n"
                + "\n"
                + "    // Error: @BeforeCompletion method must not be declared static\n"
                + "    @BeforeCompletion\n"
                + "    public void beforeCommit() {\n"
                + "    }\n"
                + "}\n";
        TextEdit expectedTextEdit = te(0, 0, 16, 0, newText);
        CodeAction expectedCodeAction = ca(uri, "Remove the 'static' modifier from this method",
                expectedDiagnostic, expectedTextEdit);
        assertJavaCodeAction(codeActionParams, utils, expectedCodeAction);
    }

    // -----------------------------------------------------------------------
    // Diagnostic + QuickFix: @AfterCompletion method must return void
    // -----------------------------------------------------------------------

    @Test
    public void testNonVoidSessionSyncMethodDiagnosticAndQuickFix() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + BASE_PATH + "InvalidNonVoidSessionSyncMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));
        Diagnostic expectedDiagnostic = d(13, 19, 32,
                "@AfterCompletion session synchronization method must be of type void.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidSessionSyncMethodNonVoid");

        assertJavaDiagnostics(diagnosticsParams, utils, expectedDiagnostic);

        // QuickFix: Change return type to void
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, expectedDiagnostic);
        String newText = "package io.openliberty.sample.jakarta.ejb.session_synchronization_method;\n"
                + "\n"
                + "import jakarta.ejb.Stateful;\n"
                + "import jakarta.ejb.AfterCompletion;\n"
                + "\n"
                + "/**\n"
                + " * Invalid session bean - @AfterCompletion method must return void.\n"
                + " */\n"
                + "@Stateful\n"
                + "public class InvalidNonVoidSessionSyncMethod {\n"
                + "\n"
                + "    // Error: @AfterCompletion method must return void\n"
                + "    @AfterCompletion\n"
                + "    public void afterComplete(boolean committed) {\n"
                + "        return committed;\n"
                + "    }\n"
                + "}\n";
        TextEdit expectedTextEdit = te(0, 0, 17, 0, newText);
        CodeAction expectedCodeAction = ca(uri, "Change return type to void", expectedDiagnostic, expectedTextEdit);
        assertJavaCodeAction(codeActionParams, utils, expectedCodeAction);
    }

    // -----------------------------------------------------------------------
    // Diagnostic + QuickFix: mixed illegal modifiers on session synchronization methods
    // -----------------------------------------------------------------------

    @Test
    public void testMixedModifiersSessionSyncMethodDiagnosticsAndQuickFixes() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + BASE_PATH + "InvalidMixedModifiersSessionSyncMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));
        Diagnostic finalOnBeginSync = d(16, 29, 43,
                "@AfterBegin session synchronization method must not be declared as final.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidSessionSyncMethodFinal");
        Diagnostic staticOnBeginSync = d(16, 29, 43,
                "@AfterBegin session synchronization method must not be declared as static.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidSessionSyncMethodStatic");

        Diagnostic staticOnBeforeCommit = d(21, 26, 43,
                "@BeforeCompletion session synchronization method must not be declared as static.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidSessionSyncMethodStatic");
        Diagnostic nonVoidOnBeforeCommit = d(21, 26, 43,
                "@BeforeCompletion session synchronization method must be of type void.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidSessionSyncMethodNonVoid");
        
        Diagnostic finalOnAfterComplete = d(27, 21, 39,
                "@AfterCompletion session synchronization method must not be declared as final.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidSessionSyncMethodFinal");
        Diagnostic nonVoidOnAfterComplete = d(27, 21, 39,
                "@AfterCompletion session synchronization method must be of type void.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidSessionSyncMethodNonVoid");

        // Engine emits diagnostics in reverse method order (last method first)
        assertJavaDiagnostics(diagnosticsParams, utils,
                finalOnAfterComplete, nonVoidOnAfterComplete,
                staticOnBeforeCommit, nonVoidOnBeforeCommit,
                finalOnBeginSync, staticOnBeginSync);

        // Shared file header used in all expected fixed texts
        final String fileHeader = "package io.openliberty.sample.jakarta.ejb.session_synchronization_method;\n"
                + "\n"
                + "import jakarta.ejb.Stateful;\n"
                + "import jakarta.ejb.AfterBegin;\n"
                + "import jakarta.ejb.BeforeCompletion;\n"
                + "import jakarta.ejb.AfterCompletion;\n"
                + "\n"
                + "/**\n"
                + " * Invalid session bean - session synchronization methods with mixed illegal modifiers.\n"
                + " * Each method combines more than one violation to exercise multiple diagnostics on a single method.\n"
                + " */\n"
                + "@Stateful\n"
                + "public class InvalidMixedModifiersSessionSyncMethod {\n"
                + "\n"
                + "    // Errors: must not be declared final AND must not be declared static\n"
                + "    @AfterBegin\n";
        final String fileFooter = "\n"
                + "    // Errors: must not be declared static AND must be of type void\n"
                + "    @BeforeCompletion\n"
                + "    public static boolean beforeCommitMixed() {\n"
                + "        return false;\n"
                + "    }\n"
                + "\n"
                + "    // Errors: must not be declared final AND must be of type void\n"
                + "    @AfterCompletion\n"
                + "    public final int afterCompleteMixed(boolean committed) {\n"
                + "        return 0;\n"
                + "    }\n"
                + "}\n";

        // QuickFix 1: remove 'final' from beginSyncMixed — "public static final void" -> "public static void"
        JakartaJavaCodeActionParams caFinalOnBeginSync = createCodeActionParams(uri, finalOnBeginSync);
        TextEdit teFinalOnBeginSync = te(0, 0, 31, 0,
                fileHeader
                + "    public static void beginSyncMixed() {\n"
                + "    }\n"
                + fileFooter);
        assertJavaCodeAction(caFinalOnBeginSync, utils,
                ca(uri, "Remove the 'final' modifier from this method", finalOnBeginSync, teFinalOnBeginSync));

        // QuickFix 2: remove 'static' from beginSyncMixed — engine leaves a double space: "public  final void"
        JakartaJavaCodeActionParams caStaticOnBeginSync = createCodeActionParams(uri, staticOnBeginSync);
        TextEdit teStaticOnBeginSync = te(0, 0, 31, 0,
                fileHeader
                + "    public  final void beginSyncMixed() {\n"
                + "    }\n"
                + fileFooter);
        assertJavaCodeAction(caStaticOnBeginSync, utils,
                ca(uri, "Remove the 'static' modifier from this method", staticOnBeginSync, teStaticOnBeginSync));

        // Shared middle section (between beginSyncMixed and afterCompleteMixed) used in fixes 3 & 4
        final String beginSyncFixed = "    public static final void beginSyncMixed() {\n"
                + "    }\n"
                + "\n"
                + "    // Errors: must not be declared static AND must be of type void\n"
                + "    @BeforeCompletion\n";
        final String afterCompletionBlock = "\n"
                + "    // Errors: must not be declared final AND must be of type void\n"
                + "    @AfterCompletion\n"
                + "    public final int afterCompleteMixed(boolean committed) {\n"
                + "        return 0;\n"
                + "    }\n"
                + "}\n";

        // QuickFix 3: remove 'static' from beforeCommitMixed — "public static boolean" -> "public boolean"
        JakartaJavaCodeActionParams caStaticOnBeforeCommit = createCodeActionParams(uri, staticOnBeforeCommit);
        TextEdit teStaticOnBeforeCommit = te(0, 0, 31, 0,
                fileHeader
                + beginSyncFixed
                + "    public boolean beforeCommitMixed() {\n"
                + "        return false;\n"
                + "    }\n"
                + afterCompletionBlock);
        assertJavaCodeAction(caStaticOnBeforeCommit, utils,
                ca(uri, "Remove the 'static' modifier from this method", staticOnBeforeCommit, teStaticOnBeforeCommit));

        // QuickFix 4: change return type of beforeCommitMixed to void — only return type changes, body kept as-is
        JakartaJavaCodeActionParams caNonVoidOnBeforeCommit = createCodeActionParams(uri, nonVoidOnBeforeCommit);
        TextEdit teNonVoidOnBeforeCommit = te(0, 0, 31, 0,
                fileHeader
                + beginSyncFixed
                + "    public static void beforeCommitMixed() {\n"
                + "        return false;\n"
                + "    }\n"
                + afterCompletionBlock);
        assertJavaCodeAction(caNonVoidOnBeforeCommit, utils,
                ca(uri, "Change return type to void", nonVoidOnBeforeCommit, teNonVoidOnBeforeCommit));

        // Shared section before afterCompleteMixed used in fixes 5 & 6
        final String beforeAfterCompletionBlock = "    public static final void beginSyncMixed() {\n"
                + "    }\n"
                + "\n"
                + "    // Errors: must not be declared static AND must be of type void\n"
                + "    @BeforeCompletion\n"
                + "    public static boolean beforeCommitMixed() {\n"
                + "        return false;\n"
                + "    }\n"
                + "\n"
                + "    // Errors: must not be declared final AND must be of type void\n"
                + "    @AfterCompletion\n";

        // QuickFix 5: remove 'final' from afterCompleteMixed — "public final int" -> "public int"
        JakartaJavaCodeActionParams caFinalOnAfterComplete = createCodeActionParams(uri, finalOnAfterComplete);
        TextEdit teFinalOnAfterComplete = te(0, 0, 31, 0,
                fileHeader
                + beforeAfterCompletionBlock
                + "    public int afterCompleteMixed(boolean committed) {\n"
                + "        return 0;\n"
                + "    }\n"
                + "}\n");
        assertJavaCodeAction(caFinalOnAfterComplete, utils,
                ca(uri, "Remove the 'final' modifier from this method", finalOnAfterComplete, teFinalOnAfterComplete));

        // QuickFix 6: change return type of afterCompleteMixed to void — only return type changes, body kept as-is
        JakartaJavaCodeActionParams caNonVoidOnAfterComplete = createCodeActionParams(uri, nonVoidOnAfterComplete);
        TextEdit teNonVoidOnAfterComplete = te(0, 0, 31, 0,
                fileHeader
                + beforeAfterCompletionBlock
                + "    public final void afterCompleteMixed(boolean committed) {\n"
                + "        return 0;\n"
                + "    }\n"
                + "}\n");
        assertJavaCodeAction(caNonVoidOnAfterComplete, utils,
                ca(uri, "Change return type to void", nonVoidOnAfterComplete, teNonVoidOnAfterComplete));
    }

    // -----------------------------------------------------------------------
    // Negative: valid session synchronization methods produce no diagnostics
    // -----------------------------------------------------------------------

    @Test
    public void testValidSessionSyncMethodsProduceNoDiagnostics() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + BASE_PATH + "ValidSessionSyncMethods.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }
}
