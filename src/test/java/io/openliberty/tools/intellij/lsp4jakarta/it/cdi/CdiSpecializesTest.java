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
 * Tests for CDI @Specializes validation.
 *
 * Per CDI spec section 3.1.4, a class annotated with @Specializes must directly
 * extend a managed bean (one whose immediate superclass carries a CDI scope annotation).
 *
 * @see <a href="https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0#specializing_a_managed_bean">CDI 3.0 §3.1.4</a>
 */
@RunWith(JUnit4.class)
public class CdiSpecializesTest extends BaseJakartaTest {

    /**
     * Tests that a class annotated with @Specializes whose direct superclass has no
     * scope annotation triggers a diagnostic error.
     *
     * Expected: Error on the class name of SpecializesWithNonBeanSuperclass.
     */
    @Test
    public void testSpecializesWithNonBeanSuperclass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/SpecializesWithNonBeanSuperclass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 12 (1-based) = line 11 (0-based)
        // "SpecializesWithNonBeanSuperclass" starts at col 13, ends at col 45
        Diagnostic unscopedSuperclassDiagnostic = d(11, 13, 45,
                "A bean annotated with @Specializes must directly extend the bean class of another CDI managed bean with a scope annotation.",
                DiagnosticSeverity.Error,
                "jakarta-cdi",
                "InvalidSpecializesAnnotationOnNonBeanSuperclass");

        assertJavaDiagnostics(diagnosticsParams, utils, unscopedSuperclassDiagnostic);
    }

    /**
     * Tests that a class annotated with @Specializes whose direct superclass IS a
     * valid CDI bean (@ApplicationScoped) does NOT trigger a diagnostic.
     *
     * Expected: No diagnostics.
     */
    @Test
    public void testSpecializesWithValidBeanSuperclass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/SpecializesWithBeanSuperclass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No diagnostics expected — direct superclass is a CDI bean
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /**
     * Tests that a class annotated with @Specializes that extends a valid CDI bean
     * annotated with @Dependent does NOT trigger a diagnostic.
     *
     * @Dependent is a built-in CDI scope, so @Specializes must be accepted without a diagnostic.
     */
    @Test
    public void testSpecializesWithDependentScopedSuperclass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/SpecializesWithDependentScopedSuperclass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No diagnostics expected — direct superclass is annotated with @Dependent
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /**
     * Tests that a class annotated with @Specializes whose direct superclass has no scope
     * annotation triggers a diagnostic, even though the grandparent class IS a valid CDI bean.
     *
     * CDI spec 3.1.4 requires only the *direct* (immediate) superclass to be a bean.
     * A scoped grandparent does NOT satisfy this requirement.
     *
     * Expected: Error on the class name of SpecializesWithGrandparentBeanOnly.
     */
    @Test
    public void testSpecializesWithGrandparentBeanOnly() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/SpecializesWithGrandparentBeanOnly.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 14 (1-based) = line 13 (0-based)
        // "SpecializesWithGrandparentBeanOnly" starts at col 13, ends at col 47 (34 chars)
        Diagnostic scopedGrandparentOnlyDiagnostic = d(13, 13, 47,
                "A bean annotated with @Specializes must directly extend the bean class of another CDI managed bean with a scope annotation.",
                DiagnosticSeverity.Error,
                "jakarta-cdi",
                "InvalidSpecializesAnnotationOnNonBeanSuperclass");

        assertJavaDiagnostics(diagnosticsParams, utils, scopedGrandparentOnlyDiagnostic);
    }

    private static String msg(String className) {
        return "Specialized bean '" + className + "' must not declare an explicit bean name using @Named. The name is inherited from the bean it specializes.";
    }

    @Test
    public void specializedBeanWithNamedAnnotation() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/SpecializedBeanWithNamed.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 14 (1-indexed) = line 13 (0-indexed), @Named("customService") col 0..23
        Diagnostic namedDiagnostic = d(13, 0, 23,
                msg("SpecializedBeanWithNamed"),
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidSpecializedBeanWithNamedAnnotation");

        Diagnostic specializesDiagnostic = d(16, 13, 37,
                "A bean annotated with @Specializes must directly extend the bean class of another CDI managed bean with a scope annotation.",
                DiagnosticSeverity.Error,
                "jakarta-cdi",
                "InvalidSpecializesAnnotationOnNonBeanSuperclass");

        assertJavaDiagnostics(diagnosticsParams, utils, namedDiagnostic, specializesDiagnostic);

        // Quick-fix: remove @Named("customService") line
        // File has 22 lines; result is the file without the @Named("customService")\n line
        String afterRemoveNamed =
                "package io.openliberty.sample.jakarta.cdi;\n\n" +
                        "import jakarta.enterprise.context.ApplicationScoped;\n" +
                        "import jakarta.enterprise.inject.Specializes;\n" +
                        "import jakarta.inject.Named;\n\n" +
                        "/**\n" +
                        " * Invalid: Specialized bean that declares an explicit bean name using @Named(\"customService\").\n" +
                        " * Per CDI 3.0 spec section 4.3, a specialized bean must not declare an explicit\n" +
                        " * bean name. The name is inherited from the bean it specializes.\n" +
                        " *\n" +
                        " * @see <a href=\"https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0#direct_and_indirect_specialization\">CDI 3.0 §4.3</a>\n" +
                        " */\n" +
                        "@Specializes\n" +
                        "@ApplicationScoped\n" +
                        "public class SpecializedBeanWithNamed {\n\n" +
                        "    public String greet() {\n" +
                        "        return \"Hello from SpecializedBeanWithNamed\";\n" +
                        "    }\n" +
                        "}\n";

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, namedDiagnostic);
        TextEdit removeNamedEdit = te(0, 0, 22, 0, afterRemoveNamed);
        CodeAction removeNamedAction = ca(uri, "Remove @Named", namedDiagnostic, removeNamedEdit);
        assertJavaCodeAction(codeActionParams, utils, removeNamedAction);
    }

    @Test
    public void specializedBeanWithBareNamedAnnotation() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/SpecializedBeanWithBareName.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 16 (1-indexed) = line 15 (0-indexed): @Named, col 0..6
        Diagnostic namedDiagnostic = d(15, 0, 6,
                msg("SpecializedBeanWithBareName"),
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidSpecializedBeanWithNamedAnnotation");

        Diagnostic specializesDiagnostic = d(17, 13, 40,
                "A bean annotated with @Specializes must directly extend the bean class of another CDI managed bean with a scope annotation.",
                DiagnosticSeverity.Error,
                "jakarta-cdi",
                "InvalidSpecializesAnnotationOnNonBeanSuperclass");

        assertJavaDiagnostics(diagnosticsParams, utils, namedDiagnostic, specializesDiagnostic);

        // Quick-fix: remove @Named line
        String afterRemoveNamed =
                "package io.openliberty.sample.jakarta.cdi;\n\n" +
                        "import jakarta.enterprise.context.ApplicationScoped;\n" +
                        "import jakarta.enterprise.inject.Specializes;\n" +
                        "import jakarta.inject.Named;\n\n" +
                        "/**\n" +
                        " * Invalid: Specialized bean that declares a bare @Named annotation (no value).\n" +
                        " * Per CDI 3.0 spec section 4.3, a specialized bean must not declare any @Named\n" +
                        " * annotation — with or without an explicit value. The name is inherited from the\n" +
                        " * bean it specializes.\n" +
                        " *\n" +
                        " * @see <a href=\"https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0#direct_and_indirect_specialization\">CDI 3.0 §4.3</a>\n" +
                        " */\n" +
                        "@Specializes\n" +
                        "@ApplicationScoped\n" +
                        "public class SpecializedBeanWithBareName {\n\n" +
                        "    public String greet() {\n" +
                        "        return \"Hello from SpecializedBeanWithBareName\";\n" +
                        "    }\n" +
                        "}\n";

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, namedDiagnostic);
        TextEdit removeNamedEdit = te(0, 0, 23, 0, afterRemoveNamed);
        CodeAction removeNamedAction = ca(uri, "Remove @Named", namedDiagnostic, removeNamedEdit);
        assertJavaCodeAction(codeActionParams, utils, removeNamedAction);
    }


    @Test
    public void validSpecializedBeanWithoutNamedAnnotation() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/ValidSpecializedBean.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void namedBeanWithoutSpecializesAnnotation() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/NamedWithoutSpecializes.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /**
     * Tests that a class annotated with @Specializes with no superclass at all
     * triggers a diagnostic error.
     *
     * @Specializes requires directly extending a bean class. A class with no
     * superclass cannot satisfy this requirement.
     */
    @Test
    public void testSpecializesWithNoSuperclass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/SpecializesWithNoSuperclass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic noSuperclassDiagnostic = d(11, 13, 40,
                "A bean annotated with @Specializes must directly extend the bean class of another CDI managed bean with a scope annotation.",
                DiagnosticSeverity.Error,
                "jakarta-cdi",
                "InvalidSpecializesAnnotationOnNonBeanSuperclass");

        assertJavaDiagnostics(diagnosticsParams, utils, noSuperclassDiagnostic);
    }

    /**
     * Tests that a class annotated with @Specializes that only implements an
     * interface (no extends clause) triggers a diagnostic error.
     *
     * Implementing an interface does not satisfy the CDI spec requirement that
     * the bean class must directly extend another bean class.
     */
    @Test
    public void testSpecializesWithInterfaceOnly() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/SpecializesWithInterfaceOnly.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic interfaceOnlyDiagnostic = d(11, 13, 41,
                "A bean annotated with @Specializes must directly extend the bean class of another CDI managed bean with a scope annotation.",
                DiagnosticSeverity.Error,
                "jakarta-cdi",
                "InvalidSpecializesAnnotationOnNonBeanSuperclass");

        assertJavaDiagnostics(diagnosticsParams, utils, interfaceOnlyDiagnostic);
    }

    /**
     * Tests that a class annotated with @Specializes that extends a valid CDI bean
     * AND implements an interface does NOT trigger a diagnostic.
     *
     * The presence of the interface is irrelevant — what matters is that the direct
     * superclass is a valid CDI bean.
     */
    @Test
    public void testSpecializesWithBeanSuperclassAndInterface() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/SpecializesWithBeanSuperclassAndInterface.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No diagnostics expected — direct superclass is a valid CDI bean
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /**
     * Tests that a class annotated with @Specializes that extends a bean annotated
     * with a custom normal scope does NOT trigger a diagnostic.
     *
     * CDI spec allows user-defined scope annotations annotated with @NormalScope.
     * A superclass carrying such a custom scope is a valid CDI bean, so @Specializes
     * must be accepted without a diagnostic.
     */
    @Test
    public void testSpecializesWithCustomScopedSuperclass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/SpecializesWithCustomScopedSuperclass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No diagnostics expected — direct superclass is annotated with a custom @NormalScope-based scope
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /**
     * Tests that a class annotated with @Specializes that extends a class carrying a
     * plain custom annotation (NOT meta-annotated with @NormalScope) triggers a diagnostic.
     *
     * The superclass is not a CDI bean because its annotation lacks the @NormalScope
     * meta-annotation, so @Specializes is invalid.
     *
     * Expected: Error on class name indicating the direct superclass is not a bean.
     */
    @Test
    public void testSpecializesWithNonNormalScopedSuperclass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/SpecializesWithNonNormalScopedSuperclass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 13 (1-based) = line 12 (0-based)
        // "SpecializesWithNonNormalScopedSuperclass" starts at col 13, length 40, end col 53
        Diagnostic nonNormalScopeDiagnostic = d(12, 13, 53,
                "A bean annotated with @Specializes must directly extend the bean class of another CDI managed bean with a scope annotation.",
                DiagnosticSeverity.Error,
                "jakarta-cdi",
                "InvalidSpecializesAnnotationOnNonBeanSuperclass");

        assertJavaDiagnostics(diagnosticsParams, utils, nonNormalScopeDiagnostic);
    }
}
