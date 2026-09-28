/*******************************************************************************
 * Copyright (c) 2021, 2026 IBM Corporation and others.
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

import com.google.gson.Gson;
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
import java.util.List;

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
        d3.setData(new Gson().toJsonTree(List.of("java.lang.Exception")));

        Diagnostic d4 = d(43, 16, 32, "A method with the @PostConstruct annotation must not throw checked exceptions.",
                DiagnosticSeverity.Error, "jakarta-annotations", "PostConstructException");
        d4.setData(new Gson().toJsonTree(List.of("java.io.IOException")));

        Diagnostic d5 = d(48, 16, 31, "A method with the @PostConstruct annotation must not throw checked exceptions.",
                DiagnosticSeverity.Error, "jakarta-annotations", "PostConstructException");
        d5.setData(new Gson().toJsonTree(Arrays.asList("io.openliberty.sample.jakarta.annotations.CustomCheckedException","java.io.IOException")));

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

        String newText31 = "package io.openliberty.sample.jakarta.annotations;\n\nimport jakarta.annotation.PostConstruct;\n" +
                "import jakarta.annotation.Resource;\n\nimport java.io.IOException;\n\n@Resource(type = Object.class, name = \"aa\")\n" +
                "public class PostConstructAnnotation {\n\n    private Integer studentId;\n\n    private boolean isHappy;\n\n    " +
                "private boolean isSad;\n\n    private String emailAddress;\n\n    @PostConstruct()\n    public Integer getStudentId() {\n        " +
                "return this.studentId;\n    }\n\n    @PostConstruct\n    public void getHappiness(String type) {\n    }\n\n    " +
                "public void throwTantrum() throws Exception {\n        System.out.println(\"I'm sad\");\n    }\n\n    " +
                "@PostConstruct\n    public void throwRuntimeException() throws RuntimeException {\n        " +
                "System.out.println(\"RuntimeException\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwNullPointerException() throws NullPointerException {\n        " +
                "System.out.println(\"NullPointerException\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwIOException() throws IOException {\n        System.out.println(\"IOException\");\n    }\n\n    " +
                "@PostConstruct\n    public void throwExceptions() throws CustomCheckedException, CustomUncheckedException, IOException {\n        " +
                "System.out.println(\"throwExceptions\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwCustomUnCheckedException() throws CustomUncheckedException {\n        " +
                "System.out.println(\"CustomUncheckedException\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwError() throws Error {\n        System.out.println(\"throwError\");\n    }\n\n}";

        String newText32 = "package io.openliberty.sample.jakarta.annotations;\n\nimport jakarta.annotation.PostConstruct;\n" +
                "import jakarta.annotation.Resource;\n\nimport java.io.IOException;\n\n@Resource(type = Object.class, name = \"aa\")\n" +
                "public class PostConstructAnnotation {\n\n    private Integer studentId;\n\n    private boolean isHappy;\n\n    " +
                "private boolean isSad;\n\n    private String emailAddress;\n\n    @PostConstruct()\n    public Integer getStudentId() {\n        " +
                "return this.studentId;\n    }\n\n    @PostConstruct\n    public void getHappiness(String type) {\n    }\n\n    " +
                "@PostConstruct\n    public void throwTantrum()  {\n        System.out.println(\"I'm sad\");\n    }\n\n    " +
                "@PostConstruct\n    public void throwRuntimeException() throws RuntimeException {\n        " +
                "System.out.println(\"RuntimeException\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwNullPointerException() throws NullPointerException {\n        " +
                "System.out.println(\"NullPointerException\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwIOException() throws IOException {\n        System.out.println(\"IOException\");\n    }\n\n    " +
                "@PostConstruct\n    public void throwExceptions() throws CustomCheckedException, CustomUncheckedException, IOException {\n        " +
                "System.out.println(\"throwExceptions\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwCustomUnCheckedException() throws CustomUncheckedException {\n        " +
                "System.out.println(\"CustomUncheckedException\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwError() throws Error {\n        System.out.println(\"throwError\");\n    }\n\n}";

        JakartaJavaCodeActionParams codeActionParams31 = createCodeActionParams(uri, d3);
        TextEdit te30 = te(0, 0, 62, 1, newText31);
        TextEdit te31 = te(0, 0, 62, 1, newText32);
        CodeAction ca30 = ca(uri, "Remove @PostConstruct", d3, te30);
        CodeAction ca31 = ca(uri, "Remove all checked exceptions.", d3, te31);
        assertJavaCodeAction(codeActionParams31, utils, ca30, ca31);

        String newText41 = "package io.openliberty.sample.jakarta.annotations;\n\nimport jakarta.annotation.PostConstruct;\n" +
                "import jakarta.annotation.Resource;\n\nimport java.io.IOException;\n\n@Resource(type = Object.class, name = \"aa\")\n" +
                "public class PostConstructAnnotation {\n\n    private Integer studentId;\n\n    private boolean isHappy;\n\n    " +
                "private boolean isSad;\n\n    private String emailAddress;\n\n    @PostConstruct()\n    " +
                "public Integer getStudentId() {\n        return this.studentId;\n    }\n\n    @PostConstruct\n    " +
                "public void getHappiness(String type) {\n    }\n\n    @PostConstruct\n    public void throwTantrum() throws Exception {\n        " +
                "System.out.println(\"I'm sad\");\n    }\n\n    @PostConstruct\n    public void throwRuntimeException() throws RuntimeException {\n        " +
                "System.out.println(\"RuntimeException\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwNullPointerException() throws NullPointerException {\n        " +
                "System.out.println(\"NullPointerException\");\n    }\n\n    public void throwIOException() throws IOException {\n        " +
                "System.out.println(\"IOException\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwExceptions() throws CustomCheckedException, CustomUncheckedException, IOException {\n        " +
                "System.out.println(\"throwExceptions\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwCustomUnCheckedException() throws CustomUncheckedException {\n        " +
                "System.out.println(\"CustomUncheckedException\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwError() throws Error {\n        System.out.println(\"throwError\");\n    }\n\n}";

        String newText42 = "package io.openliberty.sample.jakarta.annotations;\n\nimport jakarta.annotation.PostConstruct;\n" +
                "import jakarta.annotation.Resource;\n\nimport java.io.IOException;\n\n@Resource(type = Object.class, name = \"aa\")\n" +
                "public class PostConstructAnnotation {\n\n    private Integer studentId;\n\n    private boolean isHappy;\n\n    " +
                "private boolean isSad;\n\n    private String emailAddress;\n\n    @PostConstruct()\n    public Integer getStudentId() {\n        " +
                "return this.studentId;\n    }\n\n    @PostConstruct\n    public void getHappiness(String type) {\n    }\n\n    " +
                "@PostConstruct\n    public void throwTantrum() throws Exception {\n        System.out.println(\"I'm sad\");\n    }\n\n    " +
                "@PostConstruct\n    public void throwRuntimeException() throws RuntimeException {\n        " +
                "System.out.println(\"RuntimeException\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwNullPointerException() throws NullPointerException {\n        " +
                "System.out.println(\"NullPointerException\");\n    }\n\n    @PostConstruct\n    public void throwIOException()  {\n        " +
                "System.out.println(\"IOException\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwExceptions() throws CustomCheckedException, CustomUncheckedException, IOException {\n        " +
                "System.out.println(\"throwExceptions\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwCustomUnCheckedException() throws CustomUncheckedException {\n        " +
                "System.out.println(\"CustomUncheckedException\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwError() throws Error {\n        System.out.println(\"throwError\");\n    }\n\n}";

        JakartaJavaCodeActionParams codeActionParams41 = createCodeActionParams(uri, d4);
        TextEdit te4 = te(0, 0, 62, 1, newText41);
        TextEdit te41 = te(0, 0, 62, 1, newText42);
        CodeAction ca4 = ca(uri, "Remove @PostConstruct", d4, te4);
        CodeAction ca41 = ca(uri, "Remove all checked exceptions.", d4, te41);
        assertJavaCodeAction(codeActionParams41, utils, ca4, ca41);

        String newText51 = "package io.openliberty.sample.jakarta.annotations;\n\nimport jakarta.annotation.PostConstruct;\n" +
                "import jakarta.annotation.Resource;\n\nimport java.io.IOException;\n\n" +
                "@Resource(type = Object.class, name = \"aa\")\npublic class PostConstructAnnotation {\n\n    " +
                "private Integer studentId;\n\n    private boolean isHappy;\n\n    private boolean isSad;\n\n    " +
                "private String emailAddress;\n\n    @PostConstruct()\n    public Integer getStudentId() {\n        " +
                "return this.studentId;\n    }\n\n    @PostConstruct\n    public void getHappiness(String type) {\n    }\n\n    " +
                "@PostConstruct\n    public void throwTantrum() throws Exception {\n        System.out.println(\"I'm sad\");\n    }\n\n    " +
                "@PostConstruct\n    public void throwRuntimeException() throws RuntimeException {\n        " +
                "System.out.println(\"RuntimeException\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwNullPointerException() throws NullPointerException {\n        " +
                "System.out.println(\"NullPointerException\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwIOException() throws IOException {\n        System.out.println(\"IOException\");\n    }\n\n    " +
                "public void throwExceptions() throws CustomCheckedException, CustomUncheckedException, IOException {\n        " +
                "System.out.println(\"throwExceptions\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwCustomUnCheckedException() throws CustomUncheckedException {\n        " +
                "System.out.println(\"CustomUncheckedException\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwError() throws Error {\n        System.out.println(\"throwError\");\n    }\n\n}";

        String newText52 = "package io.openliberty.sample.jakarta.annotations;\n\nimport jakarta.annotation.PostConstruct;\n" +
                "import jakarta.annotation.Resource;\n\nimport java.io.IOException;\n\n@Resource(type = Object.class, name = \"aa\")\n" +
                "public class PostConstructAnnotation {\n\n    private Integer studentId;\n\n    private boolean isHappy;\n\n    " +
                "private boolean isSad;\n\n    private String emailAddress;\n\n    @PostConstruct()\n    public Integer getStudentId() {\n        " +
                "return this.studentId;\n    }\n\n    @PostConstruct\n    public void getHappiness(String type) {\n    }\n\n    " +
                "@PostConstruct\n    public void throwTantrum() throws Exception {\n        System.out.println(\"I'm sad\");\n    }\n\n    " +
                "@PostConstruct\n    public void throwRuntimeException() throws RuntimeException {\n        " +
                "System.out.println(\"RuntimeException\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwNullPointerException() throws NullPointerException {\n        " +
                "System.out.println(\"NullPointerException\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwIOException() throws IOException {\n        System.out.println(\"IOException\");\n    }\n\n    " +
                "@PostConstruct\n    public void throwExceptions() throws  CustomUncheckedException {\n        " +
                "System.out.println(\"throwExceptions\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwCustomUnCheckedException() throws CustomUncheckedException {\n        " +
                "System.out.println(\"CustomUncheckedException\");\n    }\n\n    @PostConstruct\n    " +
                "public void throwError() throws Error {\n        System.out.println(\"throwError\");\n    }\n\n}";

        JakartaJavaCodeActionParams codeActionParams51 = createCodeActionParams(uri, d5);
        TextEdit te5 = te(0, 0, 62, 1, newText51);
        TextEdit te51 = te(0, 0, 62, 1, newText52);
        CodeAction ca5 = ca(uri, "Remove @PostConstruct", d5, te5);
        CodeAction ca51 = ca(uri, "Remove all checked exceptions.", d5, te51);
        assertJavaCodeAction(codeActionParams51, utils, ca5, ca51);
    }
}
