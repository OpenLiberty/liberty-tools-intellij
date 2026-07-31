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
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
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

@RunWith(JUnit4.class)
public class JakartaPersistenceGeneratorAndSecondaryTableTest extends BaseJakartaTest {

    // -------------------------------------------------------------------------
    // @TableGenerator / @TableGenerators diagnostics
    // -------------------------------------------------------------------------

    @Test
    public void testTableGeneratorEmptyNameOnTypeFieldAndMethod() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/TableGeneratorEmptyNameOnTypeFieldMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic tableGeneratorEmptyNameOnType = d(9, 0, 26,
                Messages.getMessage("TableGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "TableGeneratorInvalidEmptyName");
        Diagnostic tableGeneratorEmptyNameOnField = d(14, 4, 30,
                Messages.getMessage("TableGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "TableGeneratorInvalidEmptyName");
        Diagnostic tableGeneratorEmptyNameOnMethod = d(17, 4, 30,
                Messages.getMessage("TableGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "TableGeneratorInvalidEmptyName");

        assertJavaDiagnostics(diagnosticsParams, utils,
                tableGeneratorEmptyNameOnType, tableGeneratorEmptyNameOnField, tableGeneratorEmptyNameOnMethod);
    }

    @Test
    public void testTableGeneratorsMissingTableGeneratorMappingOnTypeFieldAndMethod() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/TableGeneratorsMissingTableGeneratorMappingOnTypeFieldMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic tableGeneratorsMissingMappingOnType = d(9, 0, 20,
                Messages.getMessage("TableGeneratorsMissingTableGeneratorMapping"),
                DiagnosticSeverity.Error, "jakarta-persistence", "TableGeneratorsMissingTableGeneratorMapping");
        Diagnostic tableGeneratorsMissingMappingOnField = d(14, 4, 24,
                Messages.getMessage("TableGeneratorsMissingTableGeneratorMapping"),
                DiagnosticSeverity.Error, "jakarta-persistence", "TableGeneratorsMissingTableGeneratorMapping");
        Diagnostic tableGeneratorsMissingMappingOnMethod = d(17, 4, 24,
                Messages.getMessage("TableGeneratorsMissingTableGeneratorMapping"),
                DiagnosticSeverity.Error, "jakarta-persistence", "TableGeneratorsMissingTableGeneratorMapping");

        assertJavaDiagnostics(diagnosticsParams, utils,
                tableGeneratorsMissingMappingOnType, tableGeneratorsMissingMappingOnField, tableGeneratorsMissingMappingOnMethod);
    }

    @Test
    public void testTableGeneratorsWithEmptyNameOnTypeFieldAndMethod() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/TableGeneratorsWithEmptyNameOnTypeFieldMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic tableGeneratorEmptyNameOnType = d(10, 19, 45,
                Messages.getMessage("TableGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "TableGeneratorInvalidEmptyName");
        Diagnostic tableGeneratorEmptyNameOnField = d(15, 23, 49,
                Messages.getMessage("TableGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "TableGeneratorInvalidEmptyName");
        Diagnostic tableGeneratorEmptyNameOnMethod = d(18, 23, 49,
                Messages.getMessage("TableGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "TableGeneratorInvalidEmptyName");

        assertJavaDiagnostics(diagnosticsParams, utils,
                tableGeneratorEmptyNameOnType, tableGeneratorEmptyNameOnField, tableGeneratorEmptyNameOnMethod);
    }

    @Test
    public void testTableGeneratorsWithMultipleEmptyNamesOnTypeFieldAndMethod() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/TableGeneratorsWithMultipleEmptyNamesOnTypeFieldMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic tableGeneratorFirstEmptyNameOnType = d(10, 19, 48,
                Messages.getMessage("TableGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "TableGeneratorInvalidEmptyName");
        Diagnostic tableGeneratorSecondEmptyNameOnType = d(10, 83, 109,
                Messages.getMessage("TableGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "TableGeneratorInvalidEmptyName");
        Diagnostic tableGeneratorFirstEmptyNameOnField = d(15, 23, 52,
                Messages.getMessage("TableGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "TableGeneratorInvalidEmptyName");
        Diagnostic tableGeneratorSecondEmptyNameOnField = d(15, 87, 113,
                Messages.getMessage("TableGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "TableGeneratorInvalidEmptyName");
        Diagnostic tableGeneratorFirstEmptyNameOnMethod = d(18, 23, 52,
                Messages.getMessage("TableGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "TableGeneratorInvalidEmptyName");
        Diagnostic tableGeneratorSecondEmptyNameOnMethod = d(18, 87, 113,
                Messages.getMessage("TableGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "TableGeneratorInvalidEmptyName");

        assertJavaDiagnostics(diagnosticsParams, utils,
                tableGeneratorFirstEmptyNameOnType, tableGeneratorSecondEmptyNameOnType,
                tableGeneratorFirstEmptyNameOnField, tableGeneratorSecondEmptyNameOnField,
                tableGeneratorFirstEmptyNameOnMethod, tableGeneratorSecondEmptyNameOnMethod);
    }

    // -------------------------------------------------------------------------
    // @SequenceGenerator / @SequenceGenerators diagnostics
    // -------------------------------------------------------------------------

    @Test
    public void testSequenceGeneratorEmptyNameOnTypeFieldAndMethod() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/SequenceGeneratorEmptyNameOnTypeFieldMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic sequenceGeneratorEmptyNameOnType = d(9, 0, 29,
                Messages.getMessage("SequenceGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "SequenceGeneratorInvalidEmptyName");
        Diagnostic sequenceGeneratorEmptyNameOnField = d(14, 4, 33,
                Messages.getMessage("SequenceGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "SequenceGeneratorInvalidEmptyName");
        Diagnostic sequenceGeneratorEmptyNameOnMethod = d(17, 4, 33,
                Messages.getMessage("SequenceGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "SequenceGeneratorInvalidEmptyName");

        assertJavaDiagnostics(diagnosticsParams, utils,
                sequenceGeneratorEmptyNameOnType, sequenceGeneratorEmptyNameOnField, sequenceGeneratorEmptyNameOnMethod);
    }

    @Test
    public void testSequenceGeneratorsMissingSequenceGeneratorMappingOnTypeFieldAndMethod() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/SequenceGeneratorsMissingSequenceGeneratorMappingOnTypeFieldMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic sequenceGeneratorsMissingMappingOnType = d(9, 0, 23,
                Messages.getMessage("SequenceGeneratorsMissingSequenceGeneratorMapping"),
                DiagnosticSeverity.Error, "jakarta-persistence", "SequenceGeneratorsMissingSequenceGeneratorMapping");
        Diagnostic sequenceGeneratorsMissingMappingOnField = d(14, 4, 27,
                Messages.getMessage("SequenceGeneratorsMissingSequenceGeneratorMapping"),
                DiagnosticSeverity.Error, "jakarta-persistence", "SequenceGeneratorsMissingSequenceGeneratorMapping");
        Diagnostic sequenceGeneratorsMissingMappingOnMethod = d(17, 4, 27,
                Messages.getMessage("SequenceGeneratorsMissingSequenceGeneratorMapping"),
                DiagnosticSeverity.Error, "jakarta-persistence", "SequenceGeneratorsMissingSequenceGeneratorMapping");

        assertJavaDiagnostics(diagnosticsParams, utils,
                sequenceGeneratorsMissingMappingOnType, sequenceGeneratorsMissingMappingOnField, sequenceGeneratorsMissingMappingOnMethod);
    }

    @Test
    public void testSequenceGeneratorsWithEmptyNameOnTypeFieldAndMethod() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/SequenceGeneratorsWithEmptyNameOnTypeFieldMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic sequenceGeneratorEmptyNameOnType = d(10, 22, 51,
                Messages.getMessage("SequenceGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "SequenceGeneratorInvalidEmptyName");
        Diagnostic sequenceGeneratorEmptyNameOnField = d(15, 26, 55,
                Messages.getMessage("SequenceGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "SequenceGeneratorInvalidEmptyName");
        Diagnostic sequenceGeneratorEmptyNameOnMethod = d(18, 26, 55,
                Messages.getMessage("SequenceGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "SequenceGeneratorInvalidEmptyName");

        assertJavaDiagnostics(diagnosticsParams, utils,
                sequenceGeneratorEmptyNameOnType, sequenceGeneratorEmptyNameOnField, sequenceGeneratorEmptyNameOnMethod);
    }

    @Test
    public void testSequenceGeneratorsWithMultipleEmptyNamesOnTypeFieldAndMethod() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/SequenceGeneratorsWithMultipleEmptyNamesOnTypeFieldMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic sequenceGeneratorFirstEmptyNameOnType = d(10, 22, 54,
                Messages.getMessage("SequenceGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "SequenceGeneratorInvalidEmptyName");
        Diagnostic sequenceGeneratorSecondEmptyNameOnType = d(10, 92, 121,
                Messages.getMessage("SequenceGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "SequenceGeneratorInvalidEmptyName");
        Diagnostic sequenceGeneratorFirstEmptyNameOnField = d(15, 26, 58,
                Messages.getMessage("SequenceGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "SequenceGeneratorInvalidEmptyName");
        Diagnostic sequenceGeneratorSecondEmptyNameOnField = d(15, 96, 125,
                Messages.getMessage("SequenceGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "SequenceGeneratorInvalidEmptyName");
        Diagnostic sequenceGeneratorFirstEmptyNameOnMethod = d(18, 26, 58,
                Messages.getMessage("SequenceGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "SequenceGeneratorInvalidEmptyName");
        Diagnostic sequenceGeneratorSecondEmptyNameOnMethod = d(18, 96, 125,
                Messages.getMessage("SequenceGeneratorInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "SequenceGeneratorInvalidEmptyName");

        assertJavaDiagnostics(diagnosticsParams, utils,
                sequenceGeneratorFirstEmptyNameOnType, sequenceGeneratorSecondEmptyNameOnType,
                sequenceGeneratorFirstEmptyNameOnField, sequenceGeneratorSecondEmptyNameOnField,
                sequenceGeneratorFirstEmptyNameOnMethod, sequenceGeneratorSecondEmptyNameOnMethod);
    }

    // -------------------------------------------------------------------------
    // @SecondaryTable / @SecondaryTables diagnostics (TYPE only)
    // -------------------------------------------------------------------------

    @Test
    public void testSecondaryTableEmptyName() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/SecondaryTableEmptyName.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic secondaryTableEmptyName = d(8, 0, 26,
                Messages.getMessage("SecondaryTableInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "SecondaryTableInvalidEmptyName");

        assertJavaDiagnostics(diagnosticsParams, utils, secondaryTableEmptyName);
    }

    @Test
    public void testSecondaryTablesMissingSecondaryTableMapping() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/SecondaryTablesMissingSecondaryTableMapping.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic secondaryTablesMissingMapping = d(8, 0, 20,
                Messages.getMessage("SecondaryTablesMissingSecondaryTableMapping"),
                DiagnosticSeverity.Error, "jakarta-persistence", "SecondaryTablesMissingSecondaryTableMapping");

        assertJavaDiagnostics(diagnosticsParams, utils, secondaryTablesMissingMapping);
    }

    @Test
    public void testSecondaryTablesWithEmptyName() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/SecondaryTablesWithEmptyName.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic secondaryTableEmptyNameInContainer = d(10, 4, 30,
                Messages.getMessage("SecondaryTableInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "SecondaryTableInvalidEmptyName");

        assertJavaDiagnostics(diagnosticsParams, utils, secondaryTableEmptyNameInContainer);
    }

    @Test
    public void testSecondaryTablesWithMultipleEmptyNames() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/SecondaryTablesWithMultipleEmptyNames.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic secondaryTableFirstEmptyNameInContainer = d(10, 19, 48,
                Messages.getMessage("SecondaryTableInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "SecondaryTableInvalidEmptyName");
        Diagnostic secondaryTableSecondEmptyNameInContainer = d(10, 83, 109,
                Messages.getMessage("SecondaryTableInvalidEmptyName"),
                DiagnosticSeverity.Error, "jakarta-persistence", "SecondaryTableInvalidEmptyName");

        assertJavaDiagnostics(diagnosticsParams, utils,
                secondaryTableFirstEmptyNameInContainer, secondaryTableSecondEmptyNameInContainer);
    }

    // -------------------------------------------------------------------------
    // Valid cases — no diagnostics expected
    // -------------------------------------------------------------------------

    @Test
    public void testTableGeneratorValidNameOnTypeFieldAndMethod() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/TableGeneratorValidNameOnTypeFieldMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No diagnostics expected — all @TableGenerator names are non-empty
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void testTableGeneratorsValidNamesOnTypeFieldAndMethod() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/TableGeneratorsValidNamesOnTypeFieldMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No diagnostics expected — all nested @TableGenerator names are non-empty
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void testSequenceGeneratorValidNameOnTypeFieldAndMethod() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/SequenceGeneratorValidNameOnTypeFieldMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No diagnostics expected — all @SequenceGenerator names are non-empty
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void testSequenceGeneratorsValidNamesOnTypeFieldAndMethod() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/SequenceGeneratorsValidNamesOnTypeFieldMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No diagnostics expected — all nested @SequenceGenerator names are non-empty
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void testSecondaryTableValidName() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/SecondaryTableValidName.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No diagnostics expected — @SecondaryTable name is non-empty
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void testSecondaryTablesValidNames() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/SecondaryTablesValidNames.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No diagnostics expected — all nested @SecondaryTable names are non-empty
        assertJavaDiagnostics(diagnosticsParams, utils);
    }
}
