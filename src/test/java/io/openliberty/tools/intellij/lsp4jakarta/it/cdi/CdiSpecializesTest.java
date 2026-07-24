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
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;

import static io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert.*;

/**
 * Tests for CDI @Specializes validation.
 *
 * Per CDI spec section 3.1.4, a class annotated with @Specializes must directly
 * extend a managed bean (one whose immediate superclass carries a CDI scope annotation).
 *
 * @see <a href="https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0#specializing_a_managed_bean">CDI 3.0 §3.1.4</a>
 */
@RunWith(JUnit4.class)
public class CdiSpecializesTest extends BaseJakartaTest {

    /**
     * Tests that a class annotated with @Specializes whose direct superclass has no
     * scope annotation triggers a diagnostic error.
     *
     * Expected: Error on the class name of SpecializesWithNonBeanSuperclass.
     */
    @Test
    public void testSpecializesWithNonBeanSuperclass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/SpecializesWithNonBeanSuperclass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 24 (1-based) = line 23 (0-based)
        // "SpecializesWithNonBeanSuperclass" starts at col 13, ends at col 45
        Diagnostic d = d(23, 13, 45,
                "A bean annotated with @Specializes must directly extend the bean class of another CDI scope managed bean (e.g., @ApplicationScoped, @RequestScoped, @Dependent etc).",
                DiagnosticSeverity.Error,
                "jakarta-cdi",
                "InvalidSpecializesAnnotationOnNonBeanSuperclass");

        assertJavaDiagnostics(diagnosticsParams, utils, d);
    }

    /**
     * Tests that a class annotated with @Specializes whose direct superclass IS a
     * valid CDI bean (@ApplicationScoped) does NOT trigger a diagnostic.
     *
     * Expected: No diagnostics.
     */
    @Test
    public void testSpecializesWithValidBeanSuperclass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/SpecializesWithBeanSuperclass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No diagnostics expected — direct superclass is a CDI bean
        assertJavaDiagnostics(diagnosticsParams, utils);
    }
}
