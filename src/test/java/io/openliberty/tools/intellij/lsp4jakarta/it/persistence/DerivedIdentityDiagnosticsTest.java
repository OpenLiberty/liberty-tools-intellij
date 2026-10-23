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

import static io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert.assertJavaDiagnostics;
import static io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert.d;

/**
 * Tests for {@link io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence.DerivedIdentityDiagnosticsCollector}
 * covering the derived-identity rules of Jakarta Persistence 3.0 §2.4.1 and §11.1.20.
 */
@RunWith(JUnit4.class)
public class DerivedIdentityDiagnosticsTest extends BaseJakartaTest {

    // ---- Rule 1: @MapsId must be on @OneToOne / @ManyToOne ------------------------------------

    /**
     * {@code @MapsId} on a plain {@code @Id long id} field — not a relationship.
     * Field "id" at 0-based line 14, cols 17-19.
     */
    @Test
    public void testMapsIdOnNonRelationshipField() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/derivedidentity/MapsIdOnNonRelationshipField.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic mapsIdOnInvalidRelationship = d(15, 17, 19,
                "@MapsId must only be applied to a field or property also annotated with @OneToOne or @ManyToOne.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MapsIdOnInvalidRelationship");

        assertJavaDiagnostics(diagnosticsParams, utils, mapsIdOnInvalidRelationship);
    }

    // ---- Rule 2: @MapsId("value") requires a composite key ------------------------------------

    /**
     * {@code @MapsId("empId")} with only a simple {@code @Id} — no composite key.
     * Field "employee" at 0-based line 18, cols 21-29.
     */
    @Test
    public void testMapsIdWithValueRequiresCompositeKey() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/derivedidentity/MapsIdWithSimpleKey.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic mapsIdRequiresCompositeKey = d(19, 21, 29,
                "@MapsId that specifies a value can only be used on an entity that declares a composite primary key via @EmbeddedId or @IdClass.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MapsIdRequiresCompositeKey");

        assertJavaDiagnostics(diagnosticsParams, utils, mapsIdRequiresCompositeKey);
    }

    /** {@code @MapsId("empId")} on an entity with an {@code @IdClass} composite key is valid. */
    @Test
    public void testMapsIdWithValueAndIdClassIsValid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/derivedidentity/ValidMapsIdWithIdClass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /** Spec §2.4.1.1 Example 4b: value-less {@code @MapsId} with a simple {@code @Id} is valid. */
    @Test
    public void testValuelessMapsIdWithSimpleKeyIsValid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/derivedidentity/ValidMapsIdSimpleKey.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    // ---- Rule 4: @MapsId value must name an attribute of the @EmbeddedId class ----------------

    /**
     * {@code @MapsId("nonExistent")}: "nonExistent" is not an attribute in ProjectId.
     * Field "employee" at 0-based line 18, cols 21-29.
     */
    @Test
    public void testMapsIdValueNotInEmbeddedIdClass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/derivedidentity/MapsIdValueNotInEmbeddedId.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic mapsIdValueNotInEmbeddedIdClass = d(19, 21, 29,
                "The value 'nonExistent' specified in @MapsId does not match any attribute in the @EmbeddedId class 'ProjectId'.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MapsIdValueNotInEmbeddedIdClass");

        assertJavaDiagnostics(diagnosticsParams, utils, mapsIdValueNotInEmbeddedIdClass);
    }

    // ---- Rule 5: attribute type must match the parent PK type ---------------------------------

    /**
     * ProjectId.empId is long but Department's @Id is String — type mismatch.
     * Field "department" at 0-based line 18, cols 23-33.
     */
    @Test
    public void testMapsIdValueTypeMismatchSimpleParent() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/derivedidentity/MapsIdValueTypeMismatch.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic mapsIdValueTypeMismatch = d(19, 23, 33,
                "The type of attribute 'empId' in the @EmbeddedId class 'ProjectId' ('long') is not compatible with the primary key type of the parent entity 'Department' ('java.lang.String').",
                DiagnosticSeverity.Error, "jakarta-persistence", "MapsIdValueTypeMismatch");

        assertJavaDiagnostics(diagnosticsParams, utils, mapsIdValueTypeMismatch);
    }

    /**
     * Spec Example 3b: ChildKey.wrongKey is String but parent's PK (ParentWithEmbeddedId) is ProjectId.
     * Field "parent" at 0-based line 19, cols 33-39.
     */
    @Test
    public void testMapsIdValueTypeMismatchCompositeParent() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/derivedidentity/MapsIdCompositeParentTypeMismatch.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic mapsIdValueTypeMismatch = d(20, 33, 39,
                "The type of attribute 'wrongKey' in the @EmbeddedId class 'ChildKey' ('java.lang.String') is not compatible with the primary key type of the parent entity 'ParentWithEmbeddedId' ('io.openliberty.sample.jakarta.persistence.derivedidentity.ProjectId').",
                DiagnosticSeverity.Error, "jakarta-persistence", "MapsIdValueTypeMismatch");

        assertJavaDiagnostics(diagnosticsParams, utils, mapsIdValueTypeMismatch);
    }

    /** Property-access style: GetterAccessKey.isActive() is boolean but Department's @Id is String: "department" at line 20, cols 23-33. */
    @Test
    public void testMapsIdValueTypeMismatchIsPropertyAccess() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/derivedidentity/MapsIdIsPropertyAccessTypeMismatch.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic mapsIdValueTypeMismatch = d(20, 23, 33,
                "The type of attribute 'active' in the @EmbeddedId class 'GetterAccessKey' ('boolean') is not compatible with the primary key type of the parent entity 'Department' ('java.lang.String').",
                DiagnosticSeverity.Error, "jakarta-persistence", "MapsIdValueTypeMismatch");

        assertJavaDiagnostics(diagnosticsParams, utils, mapsIdValueTypeMismatch);
    }

    // ---- Value-less @MapsId with @EmbeddedId (spec Examples 5b / 6b) --------------------------

    /**
     * Embedded id ChildKey differs from parent's PK type ProjectId — type mismatch.
     * Field "parent" at 0-based line 18, cols 33-39.
     */
    @Test
    public void testValuelessMapsIdPrimaryKeyTypeMismatch() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/derivedidentity/MapsIdValuelessPkTypeMismatch.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic mapsIdPrimaryKeyTypeMismatch = d(19, 33, 39,
                "The primary key type of the Entity ('io.openliberty.sample.jakarta.persistence.derivedidentity.ChildKey') must be same as the primary key type of the Relational Entity 'ParentWithEmbeddedId' ('io.openliberty.sample.jakarta.persistence.derivedidentity.ProjectId') when @MapsId is specified without a value.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MapsIdPrimaryKeyTypeMismatch");

        assertJavaDiagnostics(diagnosticsParams, utils, mapsIdPrimaryKeyTypeMismatch);
    }

    /** Simple @Id (String) differs from parent's simple PK (Employee.id: long). */
    @Test
    public void testValuelessMapsIdSimpleKeyTypeMismatch() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/derivedidentity/MapsIdSimpleKeyTypeMismatch.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic mapsIdPrimaryKeyTypeMismatch = d(20, 21, 29,
                "The primary key type of the Entity ('java.lang.String') must be same as the primary key type of the Relational Entity 'Employee' ('long') when @MapsId is specified without a value.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MapsIdPrimaryKeyTypeMismatch");

        assertJavaDiagnostics(diagnosticsParams, utils, mapsIdPrimaryKeyTypeMismatch);
    }

    /** Spec Examples 5b/6b: embedded id type ProjectId matches parent's PK type — valid. */
    @Test
    public void testValuelessMapsIdWithMatchingEmbeddedIdIsValid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/derivedidentity/ValidMapsIdValuelessEmbeddedId.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    // ---- Valid cases --------------------------------------------------------------------------

    /** Spec Example 1b: {@code @MapsId("empId")} where ProjectId.empId (long) matches Employee's @Id (long). */
    @Test
    public void testValidMapsIdWithValue() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/derivedidentity/ValidDerivedIdentityEmbeddedId.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /** Spec Example 3b: ChildKey.parentKey has type ProjectId matching parent's @EmbeddedId type. */
    @Test
    public void testValidMapsIdWithCompositeParent() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/derivedidentity/ValidMapsIdCompositeParent.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /** Property-access style with getEmpId() resolving long matching Employee's @Id (long). */
    @Test
    public void testValidMapsIdPropertyAccess() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/derivedidentity/ValidMapsIdPropertyAccess.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }
}
