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
 * Tests for CDI specialized beans that declare an explicit @Named annotation.
 *
 * <p>Per CDI 3.0 specification §4.3 (direct and indirect specialization), a bean
 * annotated with {@code @Specializes} inherits the bean name of the bean it
 * overrides. Declaring {@code @Named} on a specialized bean is therefore invalid
 * and the container will treat it as a definition error.</p>
 *
 * @see <a href="https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0#direct_and_indirect_specialization">CDI 3.0 §4.3</a>
 */
@RunWith(JUnit4.class)
public class SpecializedBeanNamedTest extends BaseJakartaTest {

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

        assertJavaDiagnostics(diagnosticsParams, utils, namedDiagnostic);

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

        assertJavaDiagnostics(diagnosticsParams, utils, namedDiagnostic);

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
}
