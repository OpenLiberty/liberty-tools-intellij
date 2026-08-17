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
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;

import static io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert.*;

/**
 * Tests for CDI {@code @Typed} annotation validation.
 *
 * <p>Per CDI 3.0 specification §2.2.2, if a bean class, producer method, or producer field
 * specifies a {@code @Typed} annotation, and the {@code value} member specifies a class that
 * does not correspond to a type in the unrestricted set of bean types of a bean, the container
 * automatically detects the problem and treats it as a definition error.</p>
 *
 * @see <a href="https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0#restricting_bean_types">CDI 3.0 §2.2.2</a>
 */
@RunWith(JUnit4.class)
public class CdiTypedAnnotationTest extends BaseJakartaTest {

    private static final String CDI_SAMPLE_PATH = "/src/main/java/io/openliberty/sample/jakarta/cdi/";

    private String getFileUri(Module module, String fileName) {
        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + CDI_SAMPLE_PATH + fileName);
        return VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();
    }

    private static String typedMsg(String typeName) {
        return "The @Typed annotation specifies '" + typeName
                + "' which is not part of the unrestricted set of bean types for this bean.";
    }

    @Test
    public void typedAnnotationOnBeanClassInvalid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        String uri = getFileUri(module, "TypedBeanClassInvalid.java");
        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic invalidTypedDiagnostic = d(14, 0, 20,
                typedMsg("String"),
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidTypedAnnotationNonMatchingBeanType");

        assertJavaDiagnostics(diagnosticsParams, utils, invalidTypedDiagnostic);
    }

    @Test
    public void typedAnnotationOnProducerMethodInvalid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        String uri = getFileUri(module, "TypedProducerMethodInvalid.java");
        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic invalidTypedDiagnostic = d(20, 4, 24,
                typedMsg("String"),
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidTypedAnnotationNonMatchingBeanType");

        assertJavaDiagnostics(diagnosticsParams, utils, invalidTypedDiagnostic);
    }

    @Test
    public void typedAnnotationOnProducerFieldInvalid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        String uri = getFileUri(module, "TypedProducerFieldInvalid.java");
        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic invalidTypedDiagnostic = d(17, 4, 24,
                typedMsg("String"),
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidTypedAnnotationNonMatchingBeanType");

        assertJavaDiagnostics(diagnosticsParams, utils, invalidTypedDiagnostic);
    }

    @Test
    public void typedAnnotationMultipleValuesOnlyInvalidFlagged() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        String uri = getFileUri(module, "TypedBeanClassMultipleValues.java");
        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic invalidTypedDiagnostic = d(13, 0, 41,
                typedMsg("String"),
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidTypedAnnotationNonMatchingBeanType");

        assertJavaDiagnostics(diagnosticsParams, utils, invalidTypedDiagnostic);
    }

    @Test
    public void typedAnnotationOnBeanClassValid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        String uri = getFileUri(module, "TypedBeanClassValid.java");
        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void typedAnnotationBeanClassSelfIsValid() throws Exception {
        // @Typed(TypedBeanClassSelf.class) — the class itself is always valid
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        String uri = getFileUri(module, "TypedBeanClassSelf.java");
        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void typedAnnotationBeanClassSupertypeIsValid() throws Exception {
        // @Typed(TypedBeanBase.class) — TypedBeanBase is a direct superclass → valid
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        String uri = getFileUri(module, "TypedBeanClassSupertype.java");
        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void typedAnnotationEmptyArrayIsValid() throws Exception {
        // @Typed({}) — empty array means no types to validate → no diagnostics
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        String uri = getFileUri(module, "TypedBeanClassEmpty.java");
        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void typedAnnotationOnProducerMethodValid() throws Exception {
        // @Typed(List.class) on a method returning List<String> → valid
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        String uri = getFileUri(module, "TypedProducerMethodValid.java");
        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void typedAnnotationOnProducerFieldValid() throws Exception {
        // @Typed(List.class) on a field of type List<String> → valid
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        String uri = getFileUri(module, "TypedProducerFieldValid.java");
        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    // ── Quickfix tests ────────────────────────────────────────────────────────

    @Test
    public void removeTypedAnnotationQuickFixOnBeanClass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        String uri = getFileUri(module, "TypedBeanClassInvalid.java");
        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 15 (0-indexed = 14): @Typed(String.class) at col 0..20
        Diagnostic invalidTypedDiagnostic = d(14, 0, 20,
                typedMsg("String"),
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidTypedAnnotationNonMatchingBeanType");

        // Full-file replacement with line 15 (@Typed(String.class)) removed.
        // File has 22 lines; te(0,0,22,0,...) is the LTI whole-file replacement pattern.
        String afterRemoveTypedBeanClass =
                "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.enterprise.inject.Typed;\n\n" +
                "/**\n" +
                " * Invalid: @Typed specifies String.class which is not in the unrestricted bean\n" +
                " * type set of this class (TypedBeanClassInvalid, TypedBeanBase, TypedShop, Object).\n" +
                " *\n" +
                " * <p>The container must detect this and treat it as a definition error.</p>\n" +
                " *\n" +
                " * @see <a href=\"https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0#restricting_bean_types\">CDI 3.0 §2.2.2</a>\n" +
                " */\n" +
                "@ApplicationScoped\n" +
                "public class TypedBeanClassInvalid extends TypedBeanBase implements TypedShop<String> {\n\n" +
                "    @Override\n" +
                "    public String sell() {\n" +
                "        return \"item\";\n" +
                "    }\n" +
                "}\n";

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, invalidTypedDiagnostic);
        TextEdit removeTypedEdit = te(0, 0, 22, 0, afterRemoveTypedBeanClass);
        CodeAction removeTypedAction = ca(uri, "Remove @Typed", invalidTypedDiagnostic, removeTypedEdit);
        assertJavaCodeAction(codeActionParams, utils, removeTypedAction);
    }

    @Test
    public void removeTypedAnnotationQuickFixOnProducerField() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        String uri = getFileUri(module, "TypedProducerFieldInvalid.java");
        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 18 (0-indexed = 17): "    @Typed(String.class)" at col 4..24
        Diagnostic invalidTypedDiagnostic = d(17, 4, 24,
                typedMsg("String"),
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidTypedAnnotationNonMatchingBeanType");

        // Full-file replacement with line 18 (@Typed(String.class)) removed.
        // File has 20 lines; te(0,0,20,0,...) is the LTI whole-file replacement pattern.
        String afterRemoveTypedProducerField =
                "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import java.util.ArrayList;\n" +
                "import java.util.List;\n\n" +
                "import jakarta.enterprise.inject.Produces;\n" +
                "import jakarta.enterprise.inject.Typed;\n\n" +
                "/**\n" +
                " * Invalid: @Typed on a producer field specifies String.class, which is NOT\n" +
                " * in the unrestricted bean types of the field type List&lt;String&gt;.\n" +
                " *\n" +
                " * @see <a href=\"https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0#restricting_bean_types\">CDI 3.0 §2.2.2</a>\n" +
                " */\n" +
                "public class TypedProducerFieldInvalid {\n\n" +
                "    @Produces\n" +
                "    public List<String> myList = new ArrayList<>();\n" +
                "}\n";

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, invalidTypedDiagnostic);
        TextEdit removeTypedEdit = te(0, 0, 20, 0, afterRemoveTypedProducerField);
        CodeAction removeTypedAction = ca(uri, "Remove @Typed", invalidTypedDiagnostic, removeTypedEdit);
        assertJavaCodeAction(codeActionParams, utils, removeTypedAction);
    }
}
