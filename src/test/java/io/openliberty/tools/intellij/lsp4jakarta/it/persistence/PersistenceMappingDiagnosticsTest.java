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
 * Tests for validation implemented in
 * {@link io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence.PersistenceMappingDiagnosticsCollector}.
 */
@RunWith(JUnit4.class)
public class PersistenceMappingDiagnosticsTest extends BaseJakartaTest {

    // -----------------------------------------------------------------------
    // @AttributeOverride — valid cases (no diagnostic expected)
    // -----------------------------------------------------------------------

    @Test
    public void validEmbeddableOverride_nodiagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/attributeoverride/ValidEmbeddableOverride.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils /* no diagnostics expected */);
    }

    @Test
    public void validSuperclassOverride_nodiagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/attributeoverride/ValidSuperclassOverride.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils /* no diagnostics expected */);
    }

    @Test
    public void validContainerOverride_nodiagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/attributeoverride/ValidContainerOverride.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils /* no diagnostics expected */);
    }

    @Test
    public void validDeepChainOverride_nodiagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/attributeoverride/ValidDeepChainOverride.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils /* no diagnostics expected */);
    }

    @Test
    public void validDotNotationOverride_nodiagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/attributeoverride/ValidDotNotationOverride.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils /* no diagnostics expected */);
    }

    @Test
    public void validMapValueOverride_nodiagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/attributeoverride/ValidMapValueOverride.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils /* no diagnostics expected */);
    }

    // -----------------------------------------------------------------------
    // @AttributeOverride — invalid cases (diagnostic expected)
    // -----------------------------------------------------------------------

    @Test
    public void invalidEmbeddableOverride_diagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/attributeoverride/InvalidEmbeddableOverride.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 28 (0-based 27): @AttributeOverride(name = "zipcode", column = @Column(name = "ADDR_ZIP"))
        Diagnostic zipcodeNotInAddress = d(27, 4, 77,
                "The name \"zipcode\" in @AttributeOverride does not match any declared field or property in \"Address\".",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidAttributeOverrideName");

        assertJavaDiagnostics(diagnosticsParams, utils, zipcodeNotInAddress);
    }

    @Test
    public void invalidSuperclassOverride_diagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/attributeoverride/InvalidSuperclassOverride.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 21 (0-based 20): @AttributeOverride(name = "salary", column = @Column(name = "EMP_SALARY"))
        Diagnostic salaryNotInPerson = d(20, 0, 74,
                "The name \"salary\" in @AttributeOverride does not match any declared field or property in \"Person\".",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidAttributeOverrideName");

        assertJavaDiagnostics(diagnosticsParams, utils, salaryNotInPerson);
    }

    @Test
    public void invalidContainerOneEntry_diagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/attributeoverride/InvalidContainerOneEntry.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 24 (0-based 23): @AttributeOverride(name = "bonus", column = @Column(name = "MGR_BONUS"))
        Diagnostic bonusNotInPerson = d(23, 4, 76,
                "The name \"bonus\" in @AttributeOverride does not match any declared field or property in \"Person\".",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidAttributeOverrideName");

        assertJavaDiagnostics(diagnosticsParams, utils, bonusNotInPerson);
    }

    @Test
    public void invalidDotNotationSecondSegment_diagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/attributeoverride/InvalidDotNotationSecondSegment.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 29 (0-based 28): @AttributeOverride(name = "zipcode.postcode", ...)
        Diagnostic postcodeNotInZipcode = d(28, 4, 85,
                "The name \"zipcode.postcode\" in @AttributeOverride cannot be resolved: \"postcode\" does not exist in \"Zipcode\".",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidAttributeOverrideName");

        assertJavaDiagnostics(diagnosticsParams, utils, postcodeNotInZipcode);
    }

    @Test
    public void invalidDotNotationFirstSegment_diagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/attributeoverride/InvalidDotNotationFirstSegment.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 29 (0-based 28): @AttributeOverride(name = "location.zip", ...)
        Diagnostic locationNotInAddressWithZipcode = d(28, 4, 81,
                "The name \"location.zip\" in @AttributeOverride cannot be resolved: \"location\" does not exist in \"AddressWithZipcode\".",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidAttributeOverrideName");

        assertJavaDiagnostics(diagnosticsParams, utils, locationNotInAddressWithZipcode);
    }

    @Test
    public void invalidMapMissingPrefix_diagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/attributeoverride/InvalidMapMissingPrefix.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 29 (0-based 28): @AttributeOverride(name = "city", column = @Column(name = "PROP_CITY"))
        Diagnostic cityMissingKeyOrValuePrefix = d(28, 4, 75,
                "The name \"city\" in @AttributeOverride on a Map @ElementCollection must be prefixed with \"key.\" or \"value.\".",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidAttributeOverrideName");

        assertJavaDiagnostics(diagnosticsParams, utils, cityMissingKeyOrValuePrefix);
    }

    // -----------------------------------------------------------------------
    // @AssociationOverride — valid cases (no diagnostic expected)
    // -----------------------------------------------------------------------

    @Test
    public void validAssociationEmbeddableOverride_nodiagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/associationoverride/ValidEmbeddableOverride.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils /* no diagnostics expected */);
    }

    @Test
    public void validAssociationSuperclassOverride_nodiagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/associationoverride/ValidSuperclassOverride.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils /* no diagnostics expected */);
    }

    @Test
    public void validAssociationContainerOverride_nodiagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/associationoverride/ValidContainerOverride.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils /* no diagnostics expected */);
    }

    @Test
    public void validAssociationDeepChainOverride_nodiagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/associationoverride/ValidDeepChainOverride.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils /* no diagnostics expected */);
    }

    @Test
    public void validAssociationDotNotationOverride_nodiagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/associationoverride/ValidDotNotationOverride.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils /* no diagnostics expected */);
    }

    // -----------------------------------------------------------------------
    // @AssociationOverride — invalid cases (diagnostic expected)
    // -----------------------------------------------------------------------

    @Test
    public void invalidAssociationEmbeddableOverride_diagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/associationoverride/InvalidEmbeddableOverride.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 28 (0-based 27): @AssociationOverride(name = "director", joinColumns = @JoinColumn(name = "DIR_ID"))
        Diagnostic directorNotInDepartment = d(27, 4, 87,
                "The name \"director\" in @AssociationOverride does not match any declared field or property in \"Department\".",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidAssociationOverrideName");

        assertJavaDiagnostics(diagnosticsParams, utils, directorNotInDepartment);
    }

    @Test
    public void invalidAssociationSuperclassOverride_diagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/associationoverride/InvalidSuperclassOverride.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 21 (0-based 20): @AssociationOverride(name = "mentor", joinColumns = @JoinColumn(name = "MENTOR_ID"))
        Diagnostic mentorNotInPerson = d(20, 0, 84,
                "The name \"mentor\" in @AssociationOverride does not match any declared field or property in \"Person\".",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidAssociationOverrideName");

        assertJavaDiagnostics(diagnosticsParams, utils, mentorNotInPerson);
    }

    @Test
    public void invalidAssociationContainerOneEntry_diagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/associationoverride/InvalidContainerOneEntry.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 24 (0-based 23): @AssociationOverride(name = "mentor", ...)
        Diagnostic mentorNotInPerson = d(23, 4, 92,
                "The name \"mentor\" in @AssociationOverride does not match any declared field or property in \"Person\".",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidAssociationOverrideName");

        assertJavaDiagnostics(diagnosticsParams, utils, mentorNotInPerson);
    }

    @Test
    public void invalidAssociationDotNotationSecondSegment_diagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/associationoverride/InvalidDotNotationSecondSegment.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 29 (0-based 28): @AssociationOverride(name = "subDept.owner", ...)
        Diagnostic ownerNotInSubTeam = d(28, 4, 94,
                "The name \"subDept.owner\" in @AssociationOverride cannot be resolved: \"owner\" does not exist in \"SubTeam\".",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidAssociationOverrideName");

        assertJavaDiagnostics(diagnosticsParams, utils, ownerNotInSubTeam);
    }

    @Test
    public void invalidAssociationDotNotationFirstSegment_diagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/associationoverride/InvalidDotNotationFirstSegment.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 29 (0-based 28): @AssociationOverride(name = "division.coordinator", ...)
        Diagnostic divisionNotInDepartmentWithTeam = d(28, 4, 101,
                "The name \"division.coordinator\" in @AssociationOverride cannot be resolved: \"division\" does not exist in \"DepartmentWithTeam\".",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidAssociationOverrideName");

        assertJavaDiagnostics(diagnosticsParams, utils, divisionNotInDepartmentWithTeam);
    }

    // -----------------------------------------------------------------------
    // @AttributeOverride — method-level (property-based access)
    // -----------------------------------------------------------------------

    @Test
    public void validPropertyBasedAttributeOverride_nodiagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/attributeoverride/ValidPropertyBasedOverride.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils /* no diagnostics expected */);
    }

    @Test
    public void invalidPropertyBasedAttributeOverride_diagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/attributeoverride/InvalidPropertyBasedOverride.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 30 (0-based 29): @AttributeOverride(name = "zipcode", column = @Column(name = "ADDR_ZIP"))
        Diagnostic zipcodeNotInAddress = d(29, 4, 77,
                "The name \"zipcode\" in @AttributeOverride does not match any declared field or property in \"Address\".",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidAttributeOverrideName");

        assertJavaDiagnostics(diagnosticsParams, utils, zipcodeNotInAddress);
    }

    // -----------------------------------------------------------------------
    // @AssociationOverride — method-level (property-based access)
    // -----------------------------------------------------------------------

    @Test
    public void validPropertyBasedAssociationOverride_nodiagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/associationoverride/ValidPropertyBasedOverride.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils /* no diagnostics expected */);
    }

    @Test
    public void invalidPropertyBasedAssociationOverride_diagnostic() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/associationoverride/InvalidPropertyBasedOverride.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 30 (0-based 29): @AssociationOverride(name = "director", joinColumns = @JoinColumn(name = "DIR_ID"))
        Diagnostic directorNotInDepartment = d(29, 4, 87,
                "The name \"director\" in @AssociationOverride does not match any declared field or property in \"Department\".",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidAssociationOverrideName");

        assertJavaDiagnostics(diagnosticsParams, utils, directorNotInDepartment);
    }
}
