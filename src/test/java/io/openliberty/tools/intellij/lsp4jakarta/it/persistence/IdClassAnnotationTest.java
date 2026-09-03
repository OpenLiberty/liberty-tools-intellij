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
 * Tests for the @IdClass / @Embeddable diagnostic.
 *
 * Specification: Jakarta Persistence 3.0, Section 11.1.14
 * "The primary key class must be annotated with the Embeddable annotation."
 *
 * @see <a href="https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0#a14687">Jakarta Persistence 3.0 §11.1.14</a>
 */
@RunWith(JUnit4.class)
public class IdClassAnnotationTest extends BaseJakartaTest {

    /**
     * A class annotated with @IdClass whose primary key class does NOT carry @Embeddable
     * must produce a diagnostic at the @IdClass annotation range.
     */
    @Test
    public void testIdClassTypeNotAnnotatedWithEmbeddable() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/idclass/OrderWithInvalidIdClass.java");
        assertNotNull("Test resource file not found: OrderWithInvalidIdClass.java", javaFile);
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 8 (0-based: 7): "@IdClass(OrderIdMissingEmbeddable.class)"
        // annotation text range: col 0 to col 40
        Diagnostic idClassMissingEmbeddableDiagnostic = d(7, 0, 40,
                "The primary key class 'OrderIdMissingEmbeddable' used in the @IdClass annotation must be annotated with @Embeddable.",
                DiagnosticSeverity.Error, "jakarta-persistence", "IdClassTypeNotAnnotatedWithEmbeddable");

        assertJavaDiagnostics(diagnosticsParams, utils, idClassMissingEmbeddableDiagnostic);
    }

    /**
     * A class annotated with @IdClass whose primary key class IS annotated with @Embeddable
     * must NOT produce a diagnostic.
     */
    @Test
    public void testIdClassTypeAnnotatedWithEmbeddable() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/idclass/OrderWithValidIdClass.java");
        assertNotNull("Test resource file not found: OrderWithValidIdClass.java", javaFile);
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No diagnostic expected: OrderIdWithEmbeddable is annotated with @Embeddable
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /**
     * A class annotated with @IdClass using a fully qualified primary key class name that does NOT
     * carry @Embeddable must produce a diagnostic at the @IdClass annotation range.
     */
    @Test
    public void testIdClassTypeNotAnnotatedWithEmbeddableFQ() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/idclass/OrderWithFQInvalidIdClass.java");
        assertNotNull("Test resource file not found: OrderWithFQInvalidIdClass.java", javaFile);
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 8 (0-based: 7): "@IdClass(io.openliberty.sample.jakarta.persistence.idclass.OrderIdMissingEmbeddable.class)"
        // annotation text range: col 0 to col 90
        Diagnostic idClassFQMissingEmbeddableDiagnostic = d(7, 0, 90,
                "The primary key class 'OrderIdMissingEmbeddable' used in the @IdClass annotation must be annotated with @Embeddable.",
                DiagnosticSeverity.Error, "jakarta-persistence", "IdClassTypeNotAnnotatedWithEmbeddable");

        assertJavaDiagnostics(diagnosticsParams, utils, idClassFQMissingEmbeddableDiagnostic);
    }

    /**
     * A class annotated with @IdClass using a fully qualified primary key class name that IS
     * annotated with @Embeddable must NOT produce a diagnostic.
     */
    @Test
    public void testIdClassTypeAnnotatedWithEmbeddableFQ() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/idclass/OrderWithFQValidIdClass.java");
        assertNotNull("Test resource file not found: OrderWithFQValidIdClass.java", javaFile);
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No diagnostic expected: OrderIdWithEmbeddable is annotated with @Embeddable (FQ name)
        assertJavaDiagnostics(diagnosticsParams, utils);
    }
}
