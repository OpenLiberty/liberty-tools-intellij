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
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;

import static io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert.*;

/**
 * Tests for CDI diagnostics and quick-fixes for orphan disposer methods —
 * disposer methods that have no matching {@code @Produces} producer in the same class.
 *
 * Specification reference:
 * https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0#disposer_method_resolution
 */
@RunWith(JUnit4.class)
public class OrphanDisposerTest extends BaseJakartaTest {

    /**
     * A class with a co-located {@code @Produces} and {@code @Disposes} pair must
     * produce no {@code InvalidOrphanDisposerMethod} diagnostic.
     */
    @Test
    public void orphanDisposerNoDiagnosticWhenProducerMethodPresent() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                        + "/src/main/java/io/openliberty/sample/jakarta/cdi/ValidProducerDisposer.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No orphan-disposer diagnostic expected — @Produces method and disposer are co-located.
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /**
     * A class where the matching producer is a {@code @Produces} field (not a method)
     * must also produce no {@code InvalidOrphanDisposerMethod} diagnostic.
     */
    @Test
    public void orphanDisposerNoDiagnosticWhenProducerFieldPresent() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                        + "/src/main/java/io/openliberty/sample/jakarta/cdi/ValidProducerFieldDisposer.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No orphan-disposer diagnostic expected — @Produces field and disposer are co-located.
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /**
     * A class with a {@code @Disposes} method but no matching {@code @Produces} must
     * produce an {@code InvalidOrphanDisposerMethod} diagnostic on the disposer method,
     * and the quick-fix must remove the {@code @Disposes} annotation from the parameter.
     */
    @Test
    public void orphanDisposerDiagnosticAndQuickFix() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                        + "/src/main/java/io/openliberty/sample/jakarta/cdi/OrphanDisposer.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // OrphanDisposer.java — cleanup() is at line 11 (0-based), "cleanup" spans cols 16-23.
        Diagnostic orphanDiag = d(11, 16, 23,
                "A disposer method must have a corresponding producer method or producer field in the same class. The @Disposes parameter type 'Connection' has no matching @Produces method or field in this class.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidOrphanDisposerMethod");

        assertJavaDiagnostics(diagnosticsParams, utils, orphanDiag);

        // Quick-fix: remove @Disposes from parameter 'conn'. The fix replaces the whole file.
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, orphanDiag);

        String newText = "package io.openliberty.sample.jakarta.cdi;\n\n"
                + "import jakarta.enterprise.context.ApplicationScoped;\n"
                + "import jakarta.enterprise.inject.Disposes;\n\n"
                + "/**\n"
                + " * Invalid: a disposer method with no matching producer in this class.\n"
                + " */\n"
                + "@ApplicationScoped\n"
                + "public class OrphanDisposer {\n\n"
                + "    public void cleanup(Connection conn) {\n"
                + "        conn.close();\n"
                + "    }\n\n"
                + "    public static class Connection {\n"
                + "        public void close() {\n"
                + "        }\n"
                + "    }\n"
                + "}\n";

        TextEdit editRemoveDisposes = te(0, 0, 19, 0, newText);
        CodeAction removeDisposesAction = ca(uri, "Remove the @Disposes modifier from parameter conn",
                orphanDiag, editRemoveDisposes);

        assertJavaCodeAction(codeActionParams, utils, removeDisposesAction);
    }
}
