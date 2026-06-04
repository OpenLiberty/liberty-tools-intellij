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
 *     IBM Corporation - initial implementation
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
public class DependentScopedConditionalObserverTest extends BaseJakartaTest {

    @Test
    public void testDependentScopedConditionalObserver() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/DependentScopedConditionalObserver.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test case 1: @Dependent with conditional @Observes (should trigger diagnostic)
        Diagnostic observesDiag = d(12, 16, 30,
                "Beans with scope @Dependent may not have conditional observer methods. Observer method 'observerMethod' sets notifyObserver to Reception.IF_EXISTS, which is not permitted for @Dependent scoped bean.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidDependentScopeWithConditionalObserver");

        // Test case 2: @Dependent with conditional @ObservesAsync (should trigger diagnostic)
        Diagnostic asyncDiag = d(21, 16, 30,
                "Beans with scope @Dependent may not have conditional observer methods. Observer method 'observerMethod' sets notifyObserver to Reception.IF_EXISTS, which is not permitted for @Dependent scoped bean.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidDependentScopeWithConditionalObserver");

        assertJavaDiagnostics(diagnosticsParams, utils, observesDiag, asyncDiag);

        // Test code actions for @Observes (3 quick fixes)
        // Note: RemoveNotifyObserverAttributeQuickFix uses ModifyAnnotationProposal which generates full file replacement
        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, observesDiag);
        
        String observesRemoveAttr = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.Dependent;\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\nimport jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n" +
                "import jakarta.enterprise.event.Reception;\n\n// Test case 1: @Dependent with conditional @Observes (should trigger diagnostic)\n" +
                "@Dependent\npublic class DependentScopedConditionalObserver {\n    \n    public void observerMethod(@Observes() String event) {\n        " +
                "// This should trigger a diagnostic\n    }\n}\n\n// Test case 2: @Dependent with conditional @ObservesAsync (should trigger diagnostic)\n" +
                "@Dependent\nclass DependentScopedConditionalObserverAsync {\n    \n    " +
                "public void observerMethod(@ObservesAsync(notifyObserver = Reception.IF_EXISTS) String event) {\n        " +
                "// This should trigger a diagnostic\n    }\n}\n\n// Test case 3: @Dependent with ALWAYS (should NOT trigger diagnostic)\n" +
                "@Dependent\nclass DependentScopedAlwaysObserver {\n    \n    " +
                "public void observerMethod(@Observes(notifyObserver = jakarta.enterprise.event.Reception.ALWAYS) String event) {\n        " +
                "// This should NOT trigger a diagnostic\n    }\n}\n\n" +
                "// Test case 4: @Dependent without notifyObserver attribute (should NOT trigger diagnostic - defaults to ALWAYS)\n" +
                "@Dependent\nclass DependentScopedDefaultObserver {\n    \n    public void observerMethod(@Observes String event) {\n        " +
                "// This should NOT trigger a diagnostic (defaults to ALWAYS)\n    }\n}\n\n" +
                "// Test case 5: @ApplicationScoped with conditional observer (should NOT trigger diagnostic)\n" +
                "@ApplicationScoped\nclass ApplicationScopedConditionalObserver {\n    \n    " +
                "public void observerMethod(@Observes(notifyObserver = Reception.IF_EXISTS) String event) {\n        " +
                "// This should NOT trigger a diagnostic (not @Dependent)\n    }\n}\n";
        String observesRemoveDependent = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.Dependent;\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\nimport jakarta.enterprise.event.Reception;\n\n" +
                "// Test case 1: @Dependent with conditional @Observes (should trigger diagnostic)\npublic class DependentScopedConditionalObserver {\n    \n    " +
                "public void observerMethod(@Observes(notifyObserver = Reception.IF_EXISTS) String event) {\n        // This should trigger a diagnostic\n    }\n}\n\n" +
                "// Test case 2: @Dependent with conditional @ObservesAsync (should trigger diagnostic)\n" +
                "@Dependent\nclass DependentScopedConditionalObserverAsync {\n    \n    " +
                "public void observerMethod(@ObservesAsync(notifyObserver = Reception.IF_EXISTS) String event) {\n        " +
                "// This should trigger a diagnostic\n    }\n}\n\n// Test case 3: @Dependent with ALWAYS (should NOT trigger diagnostic)\n" +
                "@Dependent\nclass DependentScopedAlwaysObserver {\n    \n    " +
                "public void observerMethod(@Observes(notifyObserver = jakarta.enterprise.event.Reception.ALWAYS) String event) {\n        " +
                "// This should NOT trigger a diagnostic\n    }\n}\n\n// Test case 4: " +
                "@Dependent without notifyObserver attribute (should NOT trigger diagnostic - defaults to ALWAYS)\n" +
                "@Dependent\nclass DependentScopedDefaultObserver {\n    \n    public void observerMethod(@Observes String event) {\n        " +
                "// This should NOT trigger a diagnostic (defaults to ALWAYS)\n    }\n}\n\n// Test case 5: " +
                "@ApplicationScoped with conditional observer (should NOT trigger diagnostic)\n@ApplicationScoped\nclass ApplicationScopedConditionalObserver {\n    \n    " +
                "public void observerMethod(@Observes(notifyObserver = Reception.IF_EXISTS) String event) {\n        " +
                "// This should NOT trigger a diagnostic (not @Dependent)\n    }\n}\n";
        String observesRemoveAnnotation = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.Dependent;\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\nimport jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n" +
                "import jakarta.enterprise.event.Reception;\n\n// Test case 1: @Dependent with conditional @Observes (should trigger diagnostic)\n@Dependent\n" +
                "public class DependentScopedConditionalObserver {\n\n    public void observerMethod(String event) {\n        " +
                "// This should trigger a diagnostic\n    }\n}\n\n// Test case 2: @Dependent with conditional @ObservesAsync (should trigger diagnostic)\n" +
                "@Dependent\nclass DependentScopedConditionalObserverAsync {\n\n    " +
                "public void observerMethod(@ObservesAsync(notifyObserver = Reception.IF_EXISTS) String event) {\n        " +
                "// This should trigger a diagnostic\n    }\n}\n\n// Test case 3: @Dependent with ALWAYS (should NOT trigger diagnostic)\n" +
                "@Dependent\nclass DependentScopedAlwaysObserver {\n\n    " +
                "public void observerMethod(@Observes(notifyObserver = jakarta.enterprise.event.Reception.ALWAYS) String event) {\n        " +
                "// This should NOT trigger a diagnostic\n    }\n}\n\n// Test case 4: @Dependent without notifyObserver attribute (should NOT trigger diagnostic - defaults to ALWAYS)\n" +
                "@Dependent\nclass DependentScopedDefaultObserver {\n\n    public void observerMethod(@Observes String event) {\n        " +
                "// This should NOT trigger a diagnostic (defaults to ALWAYS)\n    }\n}\n\n// " +
                "Test case 5: @ApplicationScoped with conditional observer (should NOT trigger diagnostic)\n@ApplicationScoped\nclass ApplicationScopedConditionalObserver {\n\n    " +
                "public void observerMethod(@Observes(notifyObserver = Reception.IF_EXISTS) String event) {\n        " +
                "// This should NOT trigger a diagnostic (not @Dependent)\n    }\n}\n";

        TextEdit observesEditRemoveAttr = te(0, 0, 52, 0, observesRemoveAttr);
        TextEdit observesEditRemoveDependent = te(0, 0, 52, 0, observesRemoveDependent);
        TextEdit observesEditRemoveAnnotation = te(0, 0, 52, 0, observesRemoveAnnotation);
        CodeAction observesActionRemoveAttr = ca(uri, "Remove 'notifyObserver' attribute from @Observes", observesDiag, observesEditRemoveAttr);
        CodeAction observesActionRemoveDependent = ca(uri, "Remove @Dependent", observesDiag, observesEditRemoveDependent);
        CodeAction observesActionRemoveAnnotation = ca(uri, "Remove the @Observes modifier from parameter event", observesDiag, observesEditRemoveAnnotation);
        assertJavaCodeAction(codeActionParams1, utils, observesActionRemoveAttr, observesActionRemoveDependent, observesActionRemoveAnnotation);

        // Test code actions for @ObservesAsync (3 quick fixes)
        JakartaJavaCodeActionParams codeActionParams2 = createCodeActionParams(uri, asyncDiag);
        
        String asyncRemoveAttr = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.Dependent;\nimport jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\nimport jakarta.enterprise.event.Reception;\n\n" +
                "// Test case 1: @Dependent with conditional @Observes (should trigger diagnostic)\n@Dependent\npublic class DependentScopedConditionalObserver {\n    \n    " +
                "public void observerMethod(@Observes(notifyObserver = Reception.IF_EXISTS) String event) {\n        // This should trigger a diagnostic\n    }\n}\n\n" +
                "// Test case 2: @Dependent with conditional @ObservesAsync (should trigger diagnostic)\n@Dependent\nclass DependentScopedConditionalObserverAsync {\n    \n    " +
                "public void observerMethod(@ObservesAsync() String event) {\n        // This should trigger a diagnostic\n    }\n}\n\n" +
                "// Test case 3: @Dependent with ALWAYS (should NOT trigger diagnostic)\n@Dependent\nclass DependentScopedAlwaysObserver {\n    \n    " +
                "public void observerMethod(@Observes(notifyObserver = jakarta.enterprise.event.Reception.ALWAYS) String event) {\n        " +
                "// This should NOT trigger a diagnostic\n    }\n}\n\n// Test case 4: @Dependent without notifyObserver attribute (should NOT trigger diagnostic - defaults to ALWAYS)\n" +
                "@Dependent\nclass DependentScopedDefaultObserver {\n    \n    public void observerMethod(@Observes String event) {\n        " +
                "// This should NOT trigger a diagnostic (defaults to ALWAYS)\n    }\n}\n\n// Test case 5: @ApplicationScoped with conditional observer (should NOT trigger diagnostic)\n" +
                "@ApplicationScoped\nclass ApplicationScopedConditionalObserver {\n    \n    public void observerMethod(@Observes(notifyObserver = Reception.IF_EXISTS) String event) {\n        " +
                "// This should NOT trigger a diagnostic (not @Dependent)\n    }\n}\n";
        String asyncRemoveDependent = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.Dependent;\nimport jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\nimport jakarta.enterprise.event.Reception;\n\n" +
                "// Test case 1: @Dependent with conditional @Observes (should trigger diagnostic)\n@Dependent\npublic class DependentScopedConditionalObserver {\n    \n    " +
                "public void observerMethod(@Observes(notifyObserver = Reception.IF_EXISTS) String event) {\n        // This should trigger a diagnostic\n    }\n}\n\n" +
                "// Test case 2: @Dependent with conditional @ObservesAsync (should trigger diagnostic)\nclass DependentScopedConditionalObserverAsync {\n    \n    " +
                "public void observerMethod(@ObservesAsync(notifyObserver = Reception.IF_EXISTS) String event) {\n        // This should trigger a diagnostic\n    }\n}\n\n" +
                "// Test case 3: @Dependent with ALWAYS (should NOT trigger diagnostic)\n@Dependent\nclass DependentScopedAlwaysObserver {\n    \n    " +
                "public void observerMethod(@Observes(notifyObserver = jakarta.enterprise.event.Reception.ALWAYS) String event) {\n        " +
                "// This should NOT trigger a diagnostic\n    }\n}\n\n// Test case 4: @Dependent without notifyObserver attribute (should NOT trigger diagnostic - defaults to ALWAYS)\n" +
                "@Dependent\nclass DependentScopedDefaultObserver {\n    \n    public void observerMethod(@Observes String event) {\n        " +
                "// This should NOT trigger a diagnostic (defaults to ALWAYS)\n    }\n}\n\n// Test case 5: @ApplicationScoped with conditional observer (should NOT trigger diagnostic)\n" +
                "@ApplicationScoped\nclass ApplicationScopedConditionalObserver {\n    \n    public void observerMethod(@Observes(notifyObserver = Reception.IF_EXISTS) String event) {\n        " +
                "// This should NOT trigger a diagnostic (not @Dependent)\n    }\n}\n";
        String asyncRemoveAnnotation = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.Dependent;\nimport jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\nimport jakarta.enterprise.event.Reception;\n\n" +
                "// Test case 1: @Dependent with conditional @Observes (should trigger diagnostic)\n@Dependent\npublic class DependentScopedConditionalObserver {\n\n    " +
                "public void observerMethod(@Observes(notifyObserver = Reception.IF_EXISTS) String event) {\n        // This should trigger a diagnostic\n    }\n}\n\n" +
                "// Test case 2: @Dependent with conditional @ObservesAsync (should trigger diagnostic)\n@Dependent\nclass DependentScopedConditionalObserverAsync {\n\n    " +
                "public void observerMethod(String event) {\n        // This should trigger a diagnostic\n    }\n}\n\n" +
                "// Test case 3: @Dependent with ALWAYS (should NOT trigger diagnostic)\n@Dependent\nclass DependentScopedAlwaysObserver {\n\n    " +
                "public void observerMethod(@Observes(notifyObserver = jakarta.enterprise.event.Reception.ALWAYS) String event) {\n        " +
                "// This should NOT trigger a diagnostic\n    }\n}\n\n// Test case 4: " +
                "@Dependent without notifyObserver attribute (should NOT trigger diagnostic - defaults to ALWAYS)\n@Dependent\nclass DependentScopedDefaultObserver {\n\n    " +
                "public void observerMethod(@Observes String event) {\n        // This should NOT trigger a diagnostic (defaults to ALWAYS)\n    }\n}\n\n" +
                "// Test case 5: @ApplicationScoped with conditional observer (should NOT trigger diagnostic)\n@ApplicationScoped\nclass ApplicationScopedConditionalObserver {\n\n    " +
                "public void observerMethod(@Observes(notifyObserver = Reception.IF_EXISTS) String event) {\n        " +
                "// This should NOT trigger a diagnostic (not @Dependent)\n    }\n}\n";

        TextEdit asyncEditRemoveAttr = te(0, 0, 52, 0, asyncRemoveAttr);
        TextEdit asyncEditRemoveDependent = te(0, 0, 52, 0, asyncRemoveDependent);
        TextEdit asyncEditRemoveAnnotation = te(0, 0, 52, 0, asyncRemoveAnnotation);
        CodeAction asyncActionRemoveAttr = ca(uri, "Remove 'notifyObserver' attribute from @ObservesAsync", asyncDiag, asyncEditRemoveAttr);
        CodeAction asyncActionRemoveDependent = ca(uri, "Remove @Dependent", asyncDiag, asyncEditRemoveDependent);
        CodeAction asyncActionRemoveAnnotation = ca(uri, "Remove the @ObservesAsync modifier from parameter event", asyncDiag, asyncEditRemoveAnnotation);
        assertJavaCodeAction(codeActionParams2, utils, asyncActionRemoveAttr, asyncActionRemoveDependent, asyncActionRemoveAnnotation);
    }
}
