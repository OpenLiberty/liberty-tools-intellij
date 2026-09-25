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
 * Integration tests for the {@code @NamedEntityGraph} graph-name uniqueness rule.
 *
 * <p>Rule: Graph names must be unique within the persistence unit.
 * Spec §3.7.4:
 * <a href="https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0.html#a13662">§a13662</a>
 */
@RunWith(JUnit4.class)
public class NamedEntityGraphDiagnosticsTest extends BaseJakartaTest {

    /**
     * An entity whose {@code @NamedEntityGraph} name {@code "User.graph"} is also
     * declared on {@code NamedEntityGraphDuplicate2} must produce a diagnostic.
     *
     * <p>The annotation {@code @NamedEntityGraph(name = "User.graph")} is at
     * 0-based line 7, cols 0–38 in the test resource file.
     *
     * Spec §3.7.4:
     * https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0.html#a13662
     */
    @Test
    public void duplicateNamedEntityGraphNameInDuplicate1() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/NamedEntityGraphDuplicate1.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // @NamedEntityGraph(name = "User.graph") at 0-based line 7, cols 0-38
        Diagnostic duplicateGraphNameDiag = d(7, 0, 38,
                "The @NamedEntityGraph name 'User.graph' must be unique within the persistence unit.",
                DiagnosticSeverity.Error, "jakarta-persistence", "DuplicateNamedEntityGraphName");

        assertJavaDiagnostics(diagnosticsParams, utils, duplicateGraphNameDiag);
    }

    /**
     * The second entity declaring the duplicate graph name {@code "User.graph"} must
     * also independently produce a diagnostic when that file is analysed.
     *
     * Spec §3.7.4:
     * https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0.html#a13662
     */
    @Test
    public void duplicateNamedEntityGraphNameInDuplicate2() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/NamedEntityGraphDuplicate2.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // @NamedEntityGraph(name = "User.graph") at 0-based line 7, cols 0-38
        Diagnostic duplicateGraphNameDiag = d(7, 0, 38,
                "The @NamedEntityGraph name 'User.graph' must be unique within the persistence unit.",
                DiagnosticSeverity.Error, "jakarta-persistence", "DuplicateNamedEntityGraphName");

        assertJavaDiagnostics(diagnosticsParams, utils, duplicateGraphNameDiag);
    }

    /**
     * An entity whose {@code @NamedEntityGraph} name {@code "Admin.uniqueGraph"} is
     * not declared by any other entity must produce no diagnostics.
     *
     * Spec §3.7.4:
     * https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0.html#a13662
     */
    @Test
    public void uniqueNamedEntityGraphNameValid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/NamedEntityGraphUnique.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // "Admin.uniqueGraph" is unique — no diagnostics expected.
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    // -------------------------------------------------------------------------
    // Guard: files that must bypass the project-wide scan
    // -------------------------------------------------------------------------

    /**
     * A plain class with no persistence annotations must not produce any
     * diagnostic — and the file-level guard must prevent the project-wide
     * scan from being triggered at all.
     *
     * Spec §3.7.4:
     * https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0.html#a13662
     */
    @Test
    public void nonEntityClassNoScanTriggered() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/NonEntityClass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No @Entity + no @NamedEntityGraph(s) → guard fires, no diagnostics.
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /**
     * An {@code @Entity} class that has neither {@code @NamedEntityGraph} nor
     * {@code @NamedEntityGraphs} must not produce any diagnostic — and the
     * file-level guard must prevent the project-wide scan from being triggered.
     *
     * Spec §3.7.4:
     * https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0.html#a13662
     */
    @Test
    public void entityWithoutGraphAnnotationNoScanTriggered() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityWithoutGraphAnnotation.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // @Entity present but no @NamedEntityGraph(s) → guard fires, no diagnostics.
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    // -------------------------------------------------------------------------
    // @NamedEntityGraphs container annotation
    // -------------------------------------------------------------------------

    /**
     * Entity using {@code @NamedEntityGraphs} where one inner graph duplicates
     * {@code "User.graph"} (also declared in {@code NamedEntityGraphDuplicate1}
     * and {@code NamedEntityGraphDuplicate2}).
     *
     * <p>The inner {@code @NamedEntityGraph(name = "User.graph")} is at
     * 0-based line 9, cols 8–46 in the test resource file.
     * The inner {@code @NamedEntityGraph(name = "Container.uniqueGraph")} is unique
     * and must not produce a diagnostic.
     *
     * Spec §3.7.4:
     * https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0.html#a13662
     */
    @Test
    public void namedEntityGraphsContainerWithDuplicateName() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/NamedEntityGraphsContainerDuplicate.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // inner @NamedEntityGraph(name = "User.graph") at 0-based line 9, cols 8-46
        Diagnostic duplicateContainerGraphDiag = d(9, 8, 46,
                "The @NamedEntityGraph name 'User.graph' must be unique within the persistence unit.",
                DiagnosticSeverity.Error, "jakarta-persistence", "DuplicateNamedEntityGraphName");

        assertJavaDiagnostics(diagnosticsParams, utils, duplicateContainerGraphDiag);
    }

    /**
     * Entity using {@code @NamedEntityGraphs} where both inner graphs have
     * unique names — no diagnostics expected.
     *
     * Spec §3.7.4:
     * https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0.html#a13662
     */
    @Test
    public void namedEntityGraphsContainerAllUnique() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/NamedEntityGraphsContainerUnique.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Both "Container.graphA" and "Container.graphB" are unique — no diagnostics.
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /**
     * Entity using {@code @NamedEntityGraphs} where both inner graphs share the
     * same name {@code "Self.duplicate"} — both must produce a diagnostic.
     *
     * <p>Both inner annotations are at 0-based lines 9 and 10, each starting
     * at col 8. {@code @NamedEntityGraph(name = "Self.duplicate")} = 42 chars
     * → cols 8–50.
     *
     * Spec §3.7.4:
     * https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0.html#a13662
     */
    @Test
    public void namedEntityGraphsSelfDuplicateBothFlagged() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/NamedEntityGraphsSelfDuplicate.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Both inner @NamedEntityGraph(name = "Self.duplicate") at lines 9 and 10, cols 8-50
        Diagnostic firstSelfDuplicateDiag = d(9, 8, 50,
                "The @NamedEntityGraph name 'Self.duplicate' must be unique within the persistence unit.",
                DiagnosticSeverity.Error, "jakarta-persistence", "DuplicateNamedEntityGraphName");
        Diagnostic secondSelfDuplicateDiag = d(10, 8, 50,
                "The @NamedEntityGraph name 'Self.duplicate' must be unique within the persistence unit.",
                DiagnosticSeverity.Error, "jakarta-persistence", "DuplicateNamedEntityGraphName");

        assertJavaDiagnostics(diagnosticsParams, utils, firstSelfDuplicateDiag, secondSelfDuplicateDiag);
    }
}
