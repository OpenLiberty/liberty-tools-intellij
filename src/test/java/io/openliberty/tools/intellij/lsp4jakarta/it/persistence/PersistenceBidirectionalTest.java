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
 * Integration tests for {@code PersistenceBidirectionalDiagnosticsCollector}
 * — bidirectional JPA relationship validation (issue #726).
 *
 * <p>Two rules from Jakarta Persistence 3.0 §2.9 are verified:
 * <ol>
 * <li>The inverse side of a bidirectional {@code @OneToMany}, {@code @OneToOne},
 * or {@code @ManyToMany} relationship must declare {@code mappedBy}.</li>
 * <li>The inverse side must not carry a {@code @JoinTable} annotation.</li>
 * </ol>
 */
@RunWith(JUnit4.class)
public class PersistenceBidirectionalTest extends BaseJakartaTest {

    private static final String BIDIRECTIONAL_PKG =
            "/src/main/java/io/openliberty/sample/jakarta/persistence/bidirectional/";

    // -------------------------------------------------------------------------
    // Rule 1 — inverse side missing mappedBy
    // -------------------------------------------------------------------------

    @Test
    public void testBidirectionalMissingMappedByOneToMany() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + BIDIRECTIONAL_PKG + "BidirectionalDepartment.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // @OneToMany without mappedBy on the inverse side — BidirectionalEmployee has @ManyToOne back-ref.
        Diagnostic missingMappedByDiagnostic = d(23, 40, 49,
                "The inverse side of a bidirectional @OneToMany relationship must declare the mappedBy attribute to reference the owning side field.",
                DiagnosticSeverity.Error, "jakarta-persistence", "InverseSideMissingMappedBy");

        assertJavaDiagnostics(diagnosticsParams, utils, missingMappedByDiagnostic);
    }

    @Test
    public void testBidirectionalMissingMappedByOneToOne() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + BIDIRECTIONAL_PKG + "BidirectionalPerson.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // @OneToOne without mappedBy — BidirectionalAddress has a @OneToOne(mappedBy="address") back-ref.
        Diagnostic missingMappedByDiagnostic = d(21, 33, 40,
                "The inverse side of a bidirectional @OneToOne relationship must declare the mappedBy attribute to reference the owning side field.",
                DiagnosticSeverity.Error, "jakarta-persistence", "InverseSideMissingMappedBy");

        assertJavaDiagnostics(diagnosticsParams, utils, missingMappedByDiagnostic);
    }

    @Test
    public void testBidirectionalMissingMappedByManyToMany() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + BIDIRECTIONAL_PKG + "BidirectionalCourse.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // @ManyToMany without mappedBy — BidirectionalStudent has a @ManyToMany(mappedBy="students") back-ref.
        Diagnostic manyToManyMissingMappedByDiagnostic = d(22, 39, 47,
                "The inverse side of a bidirectional @ManyToMany relationship must declare the mappedBy attribute to reference the owning side field.",
                DiagnosticSeverity.Error, "jakarta-persistence", "InverseSideMissingMappedBy");

        assertJavaDiagnostics(diagnosticsParams, utils, manyToManyMissingMappedByDiagnostic);
    }

    // -------------------------------------------------------------------------
    // Rule 2 — @JoinTable on the inverse side
    // -------------------------------------------------------------------------

    @Test
    public void testBidirectionalJoinTableOnOneToManyInverse() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + BIDIRECTIONAL_PKG + "BidirectionalInverseJoinTable.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // @OneToMany(mappedBy=...) + @JoinTable on the inverse side — invalid.
        Diagnostic joinTableOnInverseDiagnostic = d(25, 40, 49,
                "The @JoinTable annotation must not be used on the inverse side of a relationship. Move @JoinTable to the owning side field or remove it.",
                DiagnosticSeverity.Error, "jakarta-persistence", "JoinTableOnInverseSide");

        assertJavaDiagnostics(diagnosticsParams, utils, joinTableOnInverseDiagnostic);
    }

    @Test
    public void testBidirectionalJoinTableOnManyToManyInverse() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + BIDIRECTIONAL_PKG + "BidirectionalPost.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // @ManyToMany(mappedBy=...) + @JoinTable on the inverse side — invalid.
        Diagnostic joinTableOnManyToManyInverseDiagnostic = d(24, 35, 39,
                "The @JoinTable annotation must not be used on the inverse side of a relationship. Move @JoinTable to the owning side field or remove it.",
                DiagnosticSeverity.Error, "jakarta-persistence", "JoinTableOnInverseSide");

        assertJavaDiagnostics(diagnosticsParams, utils, joinTableOnManyToManyInverseDiagnostic);
    }

    // -------------------------------------------------------------------------
    // Negative tests — no diagnostic expected
    // -------------------------------------------------------------------------

    @Test
    public void testBidirectionalValidOneToManyInverseSide() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + BIDIRECTIONAL_PKG + "BidirectionalDepartmentValid.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // @OneToMany(mappedBy="department") with no @JoinTable — valid inverse side.
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void testBidirectionalValidOneToOneInverseSide() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + BIDIRECTIONAL_PKG + "BidirectionalPersonValid.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // @OneToOne(mappedBy="resident") — valid inverse side, no diagnostic expected.
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void testBidirectionalUnidirectionalOneToMany() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + BIDIRECTIONAL_PKG + "BidirectionalOrder.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Unidirectional @OneToMany — BidirectionalOrderItem has no back-reference.
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void testBidirectionalUnidirectionalManyToMany() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + BIDIRECTIONAL_PKG + "BidirectionalProduct.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Unidirectional @ManyToMany — BidirectionalCategory has no back-reference.
        assertJavaDiagnostics(diagnosticsParams, utils);
    }
}
