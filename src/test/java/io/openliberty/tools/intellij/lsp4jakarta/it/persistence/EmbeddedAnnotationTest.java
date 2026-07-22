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
package io.openliberty.tools.intellij.lsp4jakarta.it.persistence;

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
 * Tests for the @Embedded / @Embeddable diagnostic rule.
 *
 * Spec: Jakarta Persistence 3.0, Section 11.1.16 —
 * "The embeddable class must be annotated as Embeddable."
 */
@RunWith(JUnit4.class)
public class EmbeddedAnnotationTest extends BaseJakartaTest {

    // -----------------------------------------------------------------------
    // Field-level tests
    // -----------------------------------------------------------------------

    /**
     * Field-level INVALID: @Embedded field whose declared type lacks @Embeddable.
     * Diagnostic must fire on the field name identifier.
     *
     * Source file line 25: "    private AddressNotEmbeddable address;"
     * "address" starts at col 33, ends at col 40 (0-based line 24).
     */
    @Test
    public void testEmbeddedFieldTypeNotAnnotatedWithEmbeddable() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EmbeddedWithoutEmbeddable.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic embeddedFieldNotEmbeddable = d(24, 33, 40,
                "The type 'AddressNotEmbeddable' used in the @Embedded field or property must be annotated with @Embeddable.",
                DiagnosticSeverity.Error, "jakarta-persistence", "EmbeddedTypeNotAnnotatedWithEmbeddable");

        assertJavaDiagnostics(diagnosticsParams, utils, embeddedFieldNotEmbeddable);
    }

    /**
     * Field-level VALID: @Embedded field whose declared type is correctly annotated with @Embeddable.
     * No diagnostic must be produced.
     */
    @Test
    public void testEmbeddedFieldTypeWithEmbeddable() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EmbeddedWithEmbeddable.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    // -----------------------------------------------------------------------
    // Method (property accessor) level tests
    // -----------------------------------------------------------------------

    /**
     * Method-level INVALID: @Embedded property accessor whose return type lacks @Embeddable.
     * Diagnostic must fire on the method name identifier.
     *
     * Source file line 27: "    public AddressNotEmbeddable getAddress() {"
     * "getAddress" starts at col 32, ends at col 42 (0-based line 26).
     */
    @Test
    public void testEmbeddedMethodReturnTypeNotAnnotatedWithEmbeddable() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EmbeddedMethodWithoutEmbeddable.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic embeddedMethodNotEmbeddable = d(26, 32, 42,
                "The type 'AddressNotEmbeddable' used in the @Embedded field or property must be annotated with @Embeddable.",
                DiagnosticSeverity.Error, "jakarta-persistence", "EmbeddedTypeNotAnnotatedWithEmbeddable");

        assertJavaDiagnostics(diagnosticsParams, utils, embeddedMethodNotEmbeddable);
    }

    /**
     * Method-level VALID: @Embedded property accessor whose return type is correctly annotated with @Embeddable.
     * No diagnostic must be produced.
     */
    @Test
    public void testEmbeddedMethodReturnTypeWithEmbeddable() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EmbeddedMethodWithEmbeddable.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }
}
