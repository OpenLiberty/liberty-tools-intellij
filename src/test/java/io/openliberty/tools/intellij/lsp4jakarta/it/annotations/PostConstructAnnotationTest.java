/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
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
package io.openliberty.tools.intellij.lsp4jakarta.it.annotations;

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
public class PostConstructAnnotationTest extends BaseJakartaTest {

    @Test
    public void GeneratedAnnotation() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/annotations/PostConstructAnnotation.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // expected Diagnostics

        Diagnostic d1 = d(19, 19, 31, "A method with the @PostConstruct annotation must be void.",
                DiagnosticSeverity.Error, "jakarta-annotations", "PostConstructReturnType");

        Diagnostic d2 = d(24, 16, 28, "A method with the @PostConstruct annotation must not have any parameters.",
                DiagnosticSeverity.Error, "jakarta-annotations", "PostConstructParams");

        Diagnostic d3 = d(28, 16, 28, "A method with the @PostConstruct annotation must not throw checked exceptions.",
                DiagnosticSeverity.Error, "jakarta-annotations", "PostConstructException");

        Diagnostic d4 = d(43, 16, 32, "A method with the @PostConstruct annotation must not throw checked exceptions.",
                DiagnosticSeverity.Error, "jakarta-annotations", "PostConstructException");

        Diagnostic d5 = d(48, 16, 31, "A method with the @PostConstruct annotation must not throw checked exceptions.",
                DiagnosticSeverity.Error, "jakarta-annotations", "PostConstructException");

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3, d4, d5);

        // Starting codeAction tests.
        String newText = "package io.openliberty.sample.jakarta.annotations;\n\nimport jakarta.annotation.PostConstruct;\n" +
                "import jakarta.annotation.Resource;\n\nimport java.io.IOException;\n\n@Resource(type = Object.class, name = \"aa\")\n" +
                "public class PostConstructAnnotation {\n\n    private Integer studentId;\n\n    private boolean isHappy;\n\n    " +
                "private boolean isSad;\n\n    private String emailAddress;\n\n    @PostConstruct()\n    " +
                "public void getStudentId() {\n        return this.studentId;\n    }\n\n    @PostConstruct\n    " +
                "public void getHappiness(String type) {\n    }\n\n    @PostConstruct\n    " +
                "public void throwTantrum() throws Exception {\n        System.out.println(\"I'm sad\");\n    }\n\n    " +
                "@PostConstruct\n    public void throwRuntimeException() throws RuntimeException {\n        " +
                "System.out.println(\"RuntimeException\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwNullPointerException() throws NullPointerException {\n        " +
                "System.out.println(\"NullPointerException\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwIOException() throws IOException {\n        System.out.println(\"IOException\");\n    }\n\n    " +
                "@PostConstruct\n    public void throwExceptions() throws CustomCheckedException, CustomUncheckedException, IOException {\n        " +
                "System.out.println(\"throwExceptions\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwCustomUnCheckedException() throws CustomUncheckedException {\n        " +
                "System.out.println(\"CustomUncheckedException\");\n    }\n\n    " +
                "@PostConstruct\n    public void throwError() throws Error {\n        System.out.println(\"throwError\");\n    }\n\n}";

        JakartaJavaCodeActionParams codeActionParams2 = createCodeActionParams(uri, d1);
        TextEdit te3 = te(0, 0, 62, 1, newText);
        CodeAction ca3 = ca(uri, "Change return type to void", d1, te3);
        assertJavaCodeAction(codeActionParams2, utils, ca3);

        String newText1 = "package io.openliberty.sample.jakarta.annotations;\n\nimport jakarta.annotation.PostConstruct;\n" +
                "import jakarta.annotation.Resource;\n\nimport java.io.IOException;\n\n@Resource(type = Object.class, name = \"aa\")\n" +
                "public class PostConstructAnnotation {\n\n    private Integer studentId;\n\n    private boolean isHappy;\n\n    " +
                "private boolean isSad;\n\n    private String emailAddress;\n\n    @PostConstruct()\n    " +
                "public Integer getStudentId() {\n        return this.studentId;\n    }\n\n    public void getHappiness(String type) {\n    }\n\n    " +
                "@PostConstruct\n    public void throwTantrum() throws Exception {\n        System.out.println(\"I'm sad\");\n    }\n\n    " +
                "@PostConstruct\n    public void throwRuntimeException() throws RuntimeException {\n        " +
                "System.out.println(\"RuntimeException\");\n    }\n\n    " +
                "@PostConstruct\n    public void throwNullPointerException() throws NullPointerException {\n        " +
                "System.out.println(\"NullPointerException\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwIOException() throws IOException {\n        System.out.println(\"IOException\");\n    }\n\n    " +
                "@PostConstruct\n    public void throwExceptions() throws CustomCheckedException, CustomUncheckedException, IOException {\n        " +
                "System.out.println(\"throwExceptions\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwCustomUnCheckedException() throws CustomUncheckedException {\n        " +
                "System.out.println(\"CustomUncheckedException\");\n    }\n\n    @PostConstruct\n    public void throwError() throws Error {\n        " +
                "System.out.println(\"throwError\");\n    }\n\n}";

        String newText2 = "package io.openliberty.sample.jakarta.annotations;\n\nimport jakarta.annotation.PostConstruct;\n" +
                "import jakarta.annotation.Resource;\n\nimport java.io.IOException;\n\n@Resource(type = Object.class, name = \"aa\")\n" +
                "public class PostConstructAnnotation {\n\n    private Integer studentId;\n\n    private boolean isHappy;\n\n    " +
                "private boolean isSad;\n\n    private String emailAddress;\n\n    @PostConstruct()\n    " +
                "public Integer getStudentId() {\n        return this.studentId;\n    }\n\n    @PostConstruct\n    " +
                "public void getHappiness() {\n    }\n\n    @PostConstruct\n    public void throwTantrum() throws Exception {\n        " +
                "System.out.println(\"I'm sad\");\n    }\n\n    @PostConstruct\n    public void throwRuntimeException() throws RuntimeException {\n        " +
                "System.out.println(\"RuntimeException\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwNullPointerException() throws NullPointerException {\n        " +
                "System.out.println(\"NullPointerException\");\n    }\n\n    " +
                "@PostConstruct\n    public void throwIOException() throws IOException {\n        " +
                "System.out.println(\"IOException\");\n    }\n\n    " +
                "@PostConstruct\n    public void throwExceptions() throws CustomCheckedException, CustomUncheckedException, IOException {\n        " +
                "System.out.println(\"throwExceptions\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwCustomUnCheckedException() throws CustomUncheckedException {\n        " +
                "System.out.println(\"CustomUncheckedException\");\n    }\n\n    " +
                "@PostConstruct\n    public void throwError() throws Error {\n        System.out.println(\"throwError\");\n    }\n\n}";

        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d2);
        TextEdit te = te(0, 0, 62, 1, newText1);
        TextEdit te1 = te(0, 0, 62, 1, newText2);
        CodeAction ca = ca(uri, "Remove @PostConstruct", d2, te);
        CodeAction ca1 = ca(uri, "Remove all parameters", d2, te1);
        assertJavaCodeAction(codeActionParams1, utils, ca, ca1);
    }

    @Test
    public void testIncorrectPostConstructAnnotation() throws Exception {
        // Set up the module and file where a non-Jakarta PostConstruct annotation is used
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        // The file path to a Java file that includes an incorrectly qualified PostConstruct annotation
        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) +
                        "/src/main/java/io/openliberty/sample/jakarta/annotations/IncorrectPostConstructAnnotation.java"
        );
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        // Adding a test to ensure no diagnostics are triggered for any non-matching annotation or import path similar to "jakarta.annotation.PostConstruct"
        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Ensure no diagnostics are generated for any annotation or import that is not exactly "jakarta.annotation.PostConstruct"
        assertJavaDiagnostics(diagnosticsParams, utils);
    }
}
