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
 * Tests for the @EmbeddedId / @Embeddable diagnostic.
 *
 * Specification: Jakarta Persistence 3.0, Section 11.1.14
 * "The primary key class must be annotated with the Embeddable annotation."
 *
 * @see <a href="https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0#a14687">Jakarta Persistence 3.0 §11.1.14</a>
 */
@RunWith(JUnit4.class)
public class EmbeddedIdAnnotationTest extends BaseJakartaTest {

    /**
     * A field annotated with @EmbeddedId whose declared type does NOT carry @Embeddable
     * must produce a diagnostic at the field name.
     */
    @Test
    public void testEmbeddedIdFieldTypeNotAnnotatedWithEmbeddable() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/embeddedid/EmployeeWithInvalidEmbeddedId.java");
        assertNotNull("Test resource file not found: EmployeeWithInvalidEmbeddedId.java", javaFile);
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 10 (0-based: 9): "    private EmployeeIdMissingEmbeddable id;"
        // field name "id" starts at col 40, ends at col 42
        Diagnostic embeddedIdFieldDiagnostic = d(9, 40, 42,
                "The composite primary key class 'EmployeeIdMissingEmbeddable' used in the @EmbeddedId field or property must be annotated with @Embeddable.",
                DiagnosticSeverity.Error, "jakarta-persistence", "EmbeddedIdTypeNotAnnotatedWithEmbeddable");

        assertJavaDiagnostics(diagnosticsParams, utils, embeddedIdFieldDiagnostic);
    }

    /**
     * A field annotated with @EmbeddedId whose declared type IS annotated with @Embeddable
     * must NOT produce a diagnostic.
     */
    @Test
    public void testEmbeddedIdFieldTypeAnnotatedWithEmbeddable() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/embeddedid/EmployeeWithValidEmbeddedId.java");
        assertNotNull("Test resource file not found: EmployeeWithValidEmbeddedId.java", javaFile);
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No diagnostic expected: EmployeeIdWithEmbeddable is annotated with @Embeddable
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /**
     * A method annotated with @EmbeddedId whose declared return type does NOT carry @Embeddable
     * must produce a diagnostic at the method name.
     */
    @Test
    public void testEmbeddedIdMethodTypeNotAnnotatedWithEmbeddable() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/embeddedid/EmployeeWithInvalidEmbeddedIdOnMethod.java");
        assertNotNull("Test resource file not found: EmployeeWithInvalidEmbeddedIdOnMethod.java", javaFile);
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 12 (0-based: 11): "    public EmployeeIdMissingEmbeddable getId() {"
        // method name "getId" starts at col 39, ends at col 44
        Diagnostic embeddedIdMethodDiagnostic = d(11, 39, 44,
                "The composite primary key class 'EmployeeIdMissingEmbeddable' used in the @EmbeddedId field or property must be annotated with @Embeddable.",
                DiagnosticSeverity.Error, "jakarta-persistence", "EmbeddedIdTypeNotAnnotatedWithEmbeddable");

        assertJavaDiagnostics(diagnosticsParams, utils, embeddedIdMethodDiagnostic);
    }

    /**
     * A method annotated with @EmbeddedId whose declared return type IS annotated with @Embeddable
     * must NOT produce a diagnostic.
     */
    @Test
    public void testEmbeddedIdMethodTypeAnnotatedWithEmbeddable() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/embeddedid/EmployeeWithValidEmbeddedIdOnMethod.java");
        assertNotNull("Test resource file not found: EmployeeWithValidEmbeddedIdOnMethod.java", javaFile);
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No diagnostic expected: EmployeeIdWithEmbeddable is annotated with @Embeddable
        assertJavaDiagnostics(diagnosticsParams, utils);
    }
}
