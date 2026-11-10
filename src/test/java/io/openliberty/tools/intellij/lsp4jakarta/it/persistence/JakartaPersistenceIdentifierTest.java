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

@RunWith(JUnit4.class)
public class JakartaPersistenceIdentifierTest extends BaseJakartaTest {

    @Test
    public void testMultipleEmbeddedId() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityMultipleEmbeddedId.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic firstEmbeddedIdDiagnostic = d(9, 25, 28,
                "An entity must not declare more than one @EmbeddedId annotation.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MultipleEmbeddedIdAnnotations");

        Diagnostic secondEmbeddedIdDiagnostic = d(12, 25, 28,
                "An entity must not declare more than one @EmbeddedId annotation.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MultipleEmbeddedIdAnnotations");

        assertJavaDiagnostics(diagnosticsParams, utils, firstEmbeddedIdDiagnostic, secondEmbeddedIdDiagnostic);

        // Quick fix: Remove @EmbeddedId from id1
        String removeFirstEmbeddedId = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.EmbeddedId;\n" +
                "import jakarta.persistence.Entity;\n\n" +
                "@Entity\n" +
                "public class EntityMultipleEmbeddedId {\n\n" +
                "    private CompositeKey id1;\n\n" +
                "    @EmbeddedId\n" +
                "    private CompositeKey id2;\n\n" +
                "    public EntityMultipleEmbeddedId() {\n" +
                "    }\n" +
                "}\n";

        JakartaJavaCodeActionParams removeFirstEmbeddedIdParams = createCodeActionParams(uri, firstEmbeddedIdDiagnostic);
        TextEdit removeFirstEmbeddedIdEdit = te(0, 0, 17, 0, removeFirstEmbeddedId);
        CodeAction removeFirstEmbeddedIdAction = ca(uri, "Remove @EmbeddedId", firstEmbeddedIdDiagnostic, removeFirstEmbeddedIdEdit);
        assertJavaCodeAction(removeFirstEmbeddedIdParams, utils, removeFirstEmbeddedIdAction);

        // Quick fix: Remove @EmbeddedId from id2
        String removeSecondEmbeddedId = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.EmbeddedId;\n" +
                "import jakarta.persistence.Entity;\n\n" +
                "@Entity\n" +
                "public class EntityMultipleEmbeddedId {\n\n" +
                "    @EmbeddedId\n" +
                "    private CompositeKey id1;\n\n" +
                "    private CompositeKey id2;\n\n" +
                "    public EntityMultipleEmbeddedId() {\n" +
                "    }\n" +
                "}\n";

        JakartaJavaCodeActionParams removeSecondEmbeddedIdParams = createCodeActionParams(uri, secondEmbeddedIdDiagnostic);
        TextEdit removeSecondEmbeddedIdEdit = te(0, 0, 17, 0, removeSecondEmbeddedId);
        CodeAction removeSecondEmbeddedIdAction = ca(uri, "Remove @EmbeddedId", secondEmbeddedIdDiagnostic, removeSecondEmbeddedIdEdit);
        assertJavaCodeAction(removeSecondEmbeddedIdParams, utils, removeSecondEmbeddedIdAction);
    }

    @Test
    public void testMixedIdentifiers() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityMixedIdentifiers.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // @Id field (id)
        Diagnostic mixedIdDiagnostic = d(10, 17, 19,
                "@Id cannot be combined with @EmbeddedId in the same entity.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MixedIdentifierAnnotations");

        // @EmbeddedId field (compositeId)
        Diagnostic mixedEmbeddedIdDiagnostic = d(13, 25, 36,
                "@EmbeddedId cannot be combined with @Id in the same entity.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MixedIdentifierAnnotations");

        assertJavaDiagnostics(diagnosticsParams, utils, mixedIdDiagnostic, mixedEmbeddedIdDiagnostic);

        // Quick fix on @Id field (id): only offers "Remove @Id" because only @Id is on this field
        String removeIdResult = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.EmbeddedId;\n" +
                "import jakarta.persistence.Entity;\n" +
                "import jakarta.persistence.Id;\n\n" +
                "@Entity\n" +
                "public class EntityMixedIdentifiers {\n\n" +
                "    private Long id;\n\n" +
                "    @EmbeddedId\n" +
                "    private CompositeKey compositeId;\n\n" +
                "    public EntityMixedIdentifiers() {\n" +
                "    }\n" +
                "}\n";

        JakartaJavaCodeActionParams removeIdParams = createCodeActionParams(uri, mixedIdDiagnostic);
        TextEdit removeIdEdit = te(0, 0, 18, 0, removeIdResult);
        CodeAction removeIdAction = ca(uri, "Remove @Id", mixedIdDiagnostic, removeIdEdit);
        assertJavaCodeAction(removeIdParams, utils, removeIdAction);

        // Quick fix on @EmbeddedId field (compositeId): only offers "Remove @EmbeddedId"
        // because only @EmbeddedId is on this field
        String removeEmbeddedIdResult = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.EmbeddedId;\n" +
                "import jakarta.persistence.Entity;\n" +
                "import jakarta.persistence.Id;\n\n" +
                "@Entity\n" +
                "public class EntityMixedIdentifiers {\n\n" +
                "    @Id\n" +
                "    private Long id;\n\n" +
                "    private CompositeKey compositeId;\n\n" +
                "    public EntityMixedIdentifiers() {\n" +
                "    }\n" +
                "}\n";

        JakartaJavaCodeActionParams removeEmbeddedIdParams = createCodeActionParams(uri, mixedEmbeddedIdDiagnostic);
        TextEdit removeEmbeddedIdEdit = te(0, 0, 18, 0, removeEmbeddedIdResult);
        CodeAction removeEmbeddedIdAction = ca(uri, "Remove @EmbeddedId", mixedEmbeddedIdDiagnostic, removeEmbeddedIdEdit);
        assertJavaCodeAction(removeEmbeddedIdParams, utils, removeEmbeddedIdAction);
    }

    @Test
    public void testMultipleEmbeddedIdOnGetter() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityMultipleEmbeddedIdOnGetter.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Diagnostic on getId1() getter
        Diagnostic firstEmbeddedIdDiagnostic = d(15, 24, 30,
                "An entity must not declare more than one @EmbeddedId annotation.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MultipleEmbeddedIdAnnotations");

        // Diagnostic on getId2() getter
        Diagnostic secondEmbeddedIdDiagnostic = d(20, 24, 30,
                "An entity must not declare more than one @EmbeddedId annotation.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MultipleEmbeddedIdAnnotations");

        assertJavaDiagnostics(diagnosticsParams, utils, firstEmbeddedIdDiagnostic, secondEmbeddedIdDiagnostic);

        // Quick fix: Remove @EmbeddedId from getId1
        String removeFirstEmbeddedId = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.EmbeddedId;\n" +
                "import jakarta.persistence.Entity;\n\n" +
                "@Entity\n" +
                "public class EntityMultipleEmbeddedIdOnGetter {\n\n" +
                "    private CompositeKey id1;\n" +
                "    private CompositeKey id2;\n\n" +
                "    public EntityMultipleEmbeddedIdOnGetter() {\n" +
                "    }\n\n" +
                "    public CompositeKey getId1() {\n" +
                "        return id1;\n" +
                "    }\n\n" +
                "    @EmbeddedId\n" +
                "    public CompositeKey getId2() {\n" +
                "        return id2;\n" +
                "    }\n" +
                "}\n";

        JakartaJavaCodeActionParams removeFirstEmbeddedIdParams = createCodeActionParams(uri, firstEmbeddedIdDiagnostic);
        TextEdit removeFirstEmbeddedIdEdit = te(0, 0, 24, 0, removeFirstEmbeddedId);
        CodeAction removeFirstEmbeddedIdAction = ca(uri, "Remove @EmbeddedId", firstEmbeddedIdDiagnostic, removeFirstEmbeddedIdEdit);
        assertJavaCodeAction(removeFirstEmbeddedIdParams, utils, removeFirstEmbeddedIdAction);

        // Quick fix: Remove @EmbeddedId from getId2
        String removeSecondEmbeddedId = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.EmbeddedId;\n" +
                "import jakarta.persistence.Entity;\n\n" +
                "@Entity\n" +
                "public class EntityMultipleEmbeddedIdOnGetter {\n\n" +
                "    private CompositeKey id1;\n" +
                "    private CompositeKey id2;\n\n" +
                "    public EntityMultipleEmbeddedIdOnGetter() {\n" +
                "    }\n\n" +
                "    @EmbeddedId\n" +
                "    public CompositeKey getId1() {\n" +
                "        return id1;\n" +
                "    }\n\n" +
                "    public CompositeKey getId2() {\n" +
                "        return id2;\n" +
                "    }\n" +
                "}\n";

        JakartaJavaCodeActionParams removeSecondEmbeddedIdParams = createCodeActionParams(uri, secondEmbeddedIdDiagnostic);
        TextEdit removeSecondEmbeddedIdEdit = te(0, 0, 24, 0, removeSecondEmbeddedId);
        CodeAction removeSecondEmbeddedIdAction = ca(uri, "Remove @EmbeddedId", secondEmbeddedIdDiagnostic, removeSecondEmbeddedIdEdit);
        assertJavaCodeAction(removeSecondEmbeddedIdParams, utils, removeSecondEmbeddedIdAction);
    }

    @Test
    public void testMixedIdentifiersOnGetter() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityMixedIdentifiersOnGetter.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Diagnostic on @Id getter (getId)
        Diagnostic mixedIdDiagnostic = d(16, 16, 21,
                "@Id cannot be combined with @EmbeddedId in the same entity.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MixedIdentifierAnnotations");

        // Diagnostic on @EmbeddedId getter (getCompositeId)
        Diagnostic mixedEmbeddedIdDiagnostic = d(21, 24, 38,
                "@EmbeddedId cannot be combined with @Id in the same entity.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MixedIdentifierAnnotations");

        assertJavaDiagnostics(diagnosticsParams, utils, mixedIdDiagnostic, mixedEmbeddedIdDiagnostic);

        // Quick fix on @Id getter (getId): only offers "Remove @Id"
        String removeIdResult = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.EmbeddedId;\n" +
                "import jakarta.persistence.Entity;\n" +
                "import jakarta.persistence.Id;\n\n" +
                "@Entity\n" +
                "public class EntityMixedIdentifiersOnGetter {\n\n" +
                "    private Long id;\n" +
                "    private CompositeKey compositeId;\n\n" +
                "    public EntityMixedIdentifiersOnGetter() {\n" +
                "    }\n\n" +
                "    public Long getId() {\n" +
                "        return id;\n" +
                "    }\n\n" +
                "    @EmbeddedId\n" +
                "    public CompositeKey getCompositeId() {\n" +
                "        return compositeId;\n" +
                "    }\n" +
                "}\n";

        JakartaJavaCodeActionParams removeIdParams = createCodeActionParams(uri, mixedIdDiagnostic);
        TextEdit removeIdEdit = te(0, 0, 25, 0, removeIdResult);
        CodeAction removeIdAction = ca(uri, "Remove @Id", mixedIdDiagnostic, removeIdEdit);
        assertJavaCodeAction(removeIdParams, utils, removeIdAction);

        // Quick fix on @EmbeddedId getter (getCompositeId): only offers "Remove @EmbeddedId"
        String removeEmbeddedIdResult = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.EmbeddedId;\n" +
                "import jakarta.persistence.Entity;\n" +
                "import jakarta.persistence.Id;\n\n" +
                "@Entity\n" +
                "public class EntityMixedIdentifiersOnGetter {\n\n" +
                "    private Long id;\n" +
                "    private CompositeKey compositeId;\n\n" +
                "    public EntityMixedIdentifiersOnGetter() {\n" +
                "    }\n\n" +
                "    @Id\n" +
                "    public Long getId() {\n" +
                "        return id;\n" +
                "    }\n\n" +
                "    public CompositeKey getCompositeId() {\n" +
                "        return compositeId;\n" +
                "    }\n" +
                "}\n";

        JakartaJavaCodeActionParams removeEmbeddedIdParams = createCodeActionParams(uri, mixedEmbeddedIdDiagnostic);
        TextEdit removeEmbeddedIdEdit = te(0, 0, 25, 0, removeEmbeddedIdResult);
        CodeAction removeEmbeddedIdAction = ca(uri, "Remove @EmbeddedId", mixedEmbeddedIdDiagnostic, removeEmbeddedIdEdit);
        assertJavaCodeAction(removeEmbeddedIdParams, utils, removeEmbeddedIdAction);
    }

}
