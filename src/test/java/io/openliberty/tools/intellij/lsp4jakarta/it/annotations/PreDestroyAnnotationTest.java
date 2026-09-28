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
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert.*;

@RunWith(JUnit4.class)
public class PreDestroyAnnotationTest extends BaseJakartaTest {

    @Test
    public void GeneratedAnnotation() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/annotations/PreDestroyAnnotation.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // expected annotations
        
        Diagnostic d1 = d(24, 16, 28, "A method with the @PreDestroy annotation must not have any parameters.",
                DiagnosticSeverity.Error, "jakarta-annotations", "PreDestroyParams");
        
        Diagnostic d2 = d(30, 20, 31, "A method with the @PreDestroy annotation must not be static.",
                DiagnosticSeverity.Error, "jakarta-annotations", "PreDestroyStatic");
        d2.setData("makeUnhappy");
        
        Diagnostic d3 = d(55, 16, 37, "A method with the @PreDestroy annotation must not throw checked exceptions.",
                DiagnosticSeverity.Error, "jakarta-annotations", "PreDestroyException");
        d3.setData(new Gson().toJsonTree(List.of("io.openliberty.sample.jakarta.annotations.CustomCheckedException")));

        Diagnostic d4 = d(35, 13, 25, "A method with the @PreDestroy annotation must not throw checked exceptions.",
                DiagnosticSeverity.Error, "jakarta-annotations", "PreDestroyException");
        d4.setData(new Gson().toJsonTree(List.of("java.lang.Exception")));

        Diagnostic d5 = d(50, 16, 32, "A method with the @PreDestroy annotation must not throw checked exceptions.",
                DiagnosticSeverity.Error, "jakarta-annotations", "PreDestroyException");
        d5.setData(new Gson().toJsonTree(List.of("java.io.IOException")));

        assertJavaDiagnostics(diagnosticsParams, utils, d2, d1, d3, d4, d5);

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d1);
        String newText = "package io.openliberty.sample.jakarta.annotations;\n\nimport jakarta.annotation.PreDestroy;\n" +
                "import jakarta.annotation.Resource;\n\nimport java.io.IOException;\n\n@Resource(type = Object.class, name = \"aa\") \n" +
                "public class PreDestroyAnnotation { \n\n    private Integer studentId;\n\t\n    private boolean isHappy;\n\n    " +
                "private boolean isSad;\n\n\tprivate String emailAddress;\n\n\t@PreDestroy()\n\t" +
                "public Integer getStudentId() {\n\t\treturn this.studentId;\n\t}\n\t\n\t" +
                "public boolean getHappiness(String type) {\n\t\tif (type.equals(\"happy\")) return this.isHappy;\n\t\treturn this.isSad;\n\t}\n\t\n\t" +
                "@PreDestroy()\n\tpublic static void makeUnhappy() {\n\t\tSystem.out.println(\"I'm sad\");\n\t}\n\t\n\t" +
                "@PreDestroy()\n\tpublic void throwTantrum() throws Exception {\n\t\tSystem.out.println(\"I'm sad\");\n\t}\n\n    " +
                "@PreDestroy()\n    public void throwRuntimeException() throws RuntimeException {\n        " +
                "System.out.println(\"RuntimeException\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwNullPointerException() throws NullPointerException {\n        System.out.println(\"NullPointerException\");\n    }\n\n    " +
                "@PreDestroy()\n    public void throwIOException() throws IOException {\n        System.out.println(\"IOException\");\n    }\n\n    " +
                "@PreDestroy()\n    public void throwCustomExceptions() throws CustomCheckedException, CustomUncheckedException {\n        " +
                "System.out.println(\"throwCustomExceptions\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwCustomUnCheckedException() throws CustomUncheckedException {\n        " +
                "System.out.println(\"CustomUncheckedException\");\n    }\n\n    @PreDestroy()\n    public void throwError() throws Error {\n        " +
                "System.out.println(\"throwError\");\n    }\n}\n\n\n\n";

        String newText3 = "package io.openliberty.sample.jakarta.annotations;\n\nimport jakarta.annotation.PreDestroy;\n" +
                "import jakarta.annotation.Resource;\n\nimport java.io.IOException;\n\n@Resource(type = Object.class, name = \"aa\") \n" +
                "public class PreDestroyAnnotation { \n\n    private Integer studentId;\n\t\n    private boolean isHappy;\n\n    " +
                "private boolean isSad;\n\n\tprivate String emailAddress;\n\n\t@PreDestroy()\n\t" +
                "public Integer getStudentId() {\n\t\treturn this.studentId;\n\t}\n\t\n\t@PreDestroy()\n\t" +
                "public boolean getHappiness() {\n\t\tif (type.equals(\"happy\")) return this.isHappy;\n\t\treturn this.isSad;\n\t}\n\t\n\t" +
                "@PreDestroy()\n\tpublic static void makeUnhappy() {\n\t\tSystem.out.println(\"I'm sad\");\n\t}\n\t\n\t" +
                "@PreDestroy()\n\tpublic void throwTantrum() throws Exception {\n\t\tSystem.out.println(\"I'm sad\");\n\t}\n\n    " +
                "@PreDestroy()\n    public void throwRuntimeException() throws RuntimeException {\n        " +
                "System.out.println(\"RuntimeException\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwNullPointerException() throws NullPointerException {\n        System.out.println(\"NullPointerException\");\n    }\n\n    " +
                "@PreDestroy()\n    public void throwIOException() throws IOException {\n        System.out.println(\"IOException\");\n    }\n\n    " +
                "@PreDestroy()\n    public void throwCustomExceptions() throws CustomCheckedException, CustomUncheckedException {\n        " +
                "System.out.println(\"throwCustomExceptions\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwCustomUnCheckedException() throws CustomUncheckedException {\n        " +
                "System.out.println(\"CustomUncheckedException\");\n    }\n\n    @PreDestroy()\n    public void throwError() throws Error {\n        " +
                "System.out.println(\"throwError\");\n    }\n}\n\n\n\n";

        TextEdit te = te(0, 0, 72, 0, newText);
        CodeAction ca = ca(uri, "Remove @PreDestroy", d1, te);
        TextEdit te1 = te(0, 0, 72, 0, newText3);
        CodeAction ca1 = ca(uri, "Remove all parameters", d1, te1);
        assertJavaCodeAction(codeActionParams, utils, ca, ca1);

        String newText1 = "package io.openliberty.sample.jakarta.annotations;\n\nimport jakarta.annotation.PreDestroy;\n" +
                "import jakarta.annotation.Resource;\n\nimport java.io.IOException;\n\n@Resource(type = Object.class, name = \"aa\") \n" +
                "public class PreDestroyAnnotation { \n\n    private Integer studentId;\n\t\n    private boolean isHappy;\n\n    " +
                "private boolean isSad;\n\n\tprivate String emailAddress;\n\n\t@PreDestroy()\n\t" +
                "public Integer getStudentId() {\n\t\treturn this.studentId;\n\t}\n\t\n\t@PreDestroy()\n\t" +
                "public boolean getHappiness(String type) {\n\t\tif (type.equals(\"happy\")) return this.isHappy;\n\t\treturn this.isSad;\n\t}\n\t\n\t" +
                "public static void makeUnhappy() {\n\t\tSystem.out.println(\"I'm sad\");\n\t}\n\t\n\t@PreDestroy()\n\t" +
                "public void throwTantrum() throws Exception {\n\t\tSystem.out.println(\"I'm sad\");\n\t}\n\n    " +
                "@PreDestroy()\n    public void throwRuntimeException() throws RuntimeException {\n        " +
                "System.out.println(\"RuntimeException\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwNullPointerException() throws NullPointerException {\n        " +
                "System.out.println(\"NullPointerException\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwIOException() throws IOException {\n        System.out.println(\"IOException\");\n    }\n\n    " +
                "@PreDestroy()\n    public void throwCustomExceptions() throws CustomCheckedException, CustomUncheckedException {\n        " +
                "System.out.println(\"throwCustomExceptions\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwCustomUnCheckedException() throws CustomUncheckedException {\n        " +
                "System.out.println(\"CustomUncheckedException\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwError() throws Error {\n        System.out.println(\"throwError\");\n    }\n}\n\n\n\n";

        String newText2 = "package io.openliberty.sample.jakarta.annotations;\n\nimport jakarta.annotation.PreDestroy;\n" +
                "import jakarta.annotation.Resource;\n\nimport java.io.IOException;\n\n@Resource(type = Object.class, name = \"aa\") \n" +
                "public class PreDestroyAnnotation { \n\n    private Integer studentId;\n\t\n    private boolean isHappy;\n\n    " +
                "private boolean isSad;\n\n\tprivate String emailAddress;\n\n\t" +
                "@PreDestroy()\n\tpublic Integer getStudentId() {\n\t\treturn this.studentId;\n\t}\n\t\n\t@PreDestroy()\n\t" +
                "public boolean getHappiness(String type) {\n\t\tif (type.equals(\"happy\")) return this.isHappy;\n\t\treturn this.isSad;\n\t}\n\t\n\t" +
                "@PreDestroy()\n\tpublic void makeUnhappy() {\n\t\tSystem.out.println(\"I'm sad\");\n\t}\n\t\n\t" +
                "@PreDestroy()\n\tpublic void throwTantrum() throws Exception {\n\t\tSystem.out.println(\"I'm sad\");\n\t}\n\n    " +
                "@PreDestroy()\n    public void throwRuntimeException() throws RuntimeException {\n        " +
                "System.out.println(\"RuntimeException\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwNullPointerException() throws NullPointerException {\n        " +
                "System.out.println(\"NullPointerException\");\n    }\n\n    " +
                "@PreDestroy()\n    public void throwIOException() throws IOException {\n        " +
                "System.out.println(\"IOException\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwCustomExceptions() throws CustomCheckedException, CustomUncheckedException {\n        " +
                "System.out.println(\"throwCustomExceptions\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwCustomUnCheckedException() throws CustomUncheckedException {\n        " +
                "System.out.println(\"CustomUncheckedException\");\n    }\n\n    " +
                "@PreDestroy()\n    public void throwError() throws Error {\n        " +
                "System.out.println(\"throwError\");\n    }\n}\n\n\n\n";

        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d2);
        TextEdit te2 = te(0, 0, 72, 0, newText1);
        CodeAction ca2 = ca(uri, "Remove @PreDestroy", d2, te2);
        TextEdit te3 = te(0, 0, 72, 0, newText2);
        CodeAction ca3 = ca(uri, "Remove the 'static' modifier from this method", d2, te3);
        assertJavaCodeAction(codeActionParams1, utils, ca2, ca3);

        String newText31 = "package io.openliberty.sample.jakarta.annotations;\n\nimport jakarta.annotation.PreDestroy;\n" +
                "import jakarta.annotation.Resource;\n\nimport java.io.IOException;\n\n" +
                "@Resource(type = Object.class, name = \"aa\") \npublic class PreDestroyAnnotation { \n\n    " +
                "private Integer studentId;\n\t\n    private boolean isHappy;\n\n    private boolean isSad;\n\n\t" +
                "private String emailAddress;\n\n\t@PreDestroy()\n\tpublic Integer getStudentId() {\n\t\t" +
                "return this.studentId;\n\t}\n\t\n\t@PreDestroy()\n\tpublic boolean getHappiness(String type) {\n\t\t" +
                "if (type.equals(\"happy\")) return this.isHappy;\n\t\treturn this.isSad;\n\t}\n\t\n\t" +
                "@PreDestroy()\n\tpublic static void makeUnhappy() {\n\t\tSystem.out.println(\"I'm sad\");\n\t}\n\t\n\t" +
                "@PreDestroy()\n\tpublic void throwTantrum() throws Exception {\n\t\tSystem.out.println(\"I'm sad\");\n\t}\n\n    " +
                "@PreDestroy()\n    public void throwRuntimeException() throws RuntimeException {\n        " +
                "System.out.println(\"RuntimeException\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwNullPointerException() throws NullPointerException {\n        " +
                "System.out.println(\"NullPointerException\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwIOException() throws IOException {\n        System.out.println(\"IOException\");\n    }\n\n    " +
                "public void throwCustomExceptions() throws CustomCheckedException, CustomUncheckedException {\n        " +
                "System.out.println(\"throwCustomExceptions\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwCustomUnCheckedException() throws CustomUncheckedException {\n        " +
                "System.out.println(\"CustomUncheckedException\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwError() throws Error {\n        System.out.println(\"throwError\");\n    }\n}\n\n\n\n";

        String newText32 = "package io.openliberty.sample.jakarta.annotations;\n\nimport jakarta.annotation.PreDestroy;\n" +
                "import jakarta.annotation.Resource;\n\nimport java.io.IOException;\n\n@Resource(type = Object.class, name = \"aa\") \n" +
                "public class PreDestroyAnnotation { \n\n    private Integer studentId;\n\t\n    private boolean isHappy;\n\n    " +
                "private boolean isSad;\n\n\tprivate String emailAddress;\n\n\t@PreDestroy()\n\tpublic Integer getStudentId() {\n\t\t" +
                "return this.studentId;\n\t}\n\t\n\t@PreDestroy()\n\tpublic boolean getHappiness(String type) {\n\t\t" +
                "if (type.equals(\"happy\")) return this.isHappy;\n\t\treturn this.isSad;\n\t}\n\t\n\t@PreDestroy()\n\t" +
                "public static void makeUnhappy() {\n\t\tSystem.out.println(\"I'm sad\");\n\t}\n\t\n\t@PreDestroy()\n\t" +
                "public void throwTantrum() throws Exception {\n\t\tSystem.out.println(\"I'm sad\");\n\t}\n\n    " +
                "@PreDestroy()\n    public void throwRuntimeException() throws RuntimeException {\n        " +
                "System.out.println(\"RuntimeException\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwNullPointerException() throws NullPointerException {\n        " +
                "System.out.println(\"NullPointerException\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwIOException() throws IOException {\n        System.out.println(\"IOException\");\n    }\n\n    " +
                "@PreDestroy()\n    public void throwCustomExceptions() throws  CustomUncheckedException {\n        " +
                "System.out.println(\"throwCustomExceptions\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwCustomUnCheckedException() throws CustomUncheckedException {\n        " +
                "System.out.println(\"CustomUncheckedException\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwError() throws Error {\n        System.out.println(\"throwError\");\n    }\n}\n\n\n\n";

        JakartaJavaCodeActionParams codeActionParams31 = createCodeActionParams(uri, d3);
        TextEdit te32 = te(0, 0, 72, 0, newText31);
        CodeAction ca32 = ca(uri, "Remove @PreDestroy", d3, te32);
        TextEdit te33 = te(0, 0, 72, 0, newText32);
        CodeAction ca33 = ca(uri, "Remove all checked exceptions.", d3, te33);
        assertJavaCodeAction(codeActionParams31, utils, ca32, ca33);

        String newText41 = "package io.openliberty.sample.jakarta.annotations;\n\nimport jakarta.annotation.PreDestroy;\n" +
                "import jakarta.annotation.Resource;\n\nimport java.io.IOException;\n\n@Resource(type = Object.class, name = \"aa\") \n" +
                "public class PreDestroyAnnotation { \n\n    private Integer studentId;\n\t\n    private boolean isHappy;\n\n    " +
                "private boolean isSad;\n\n\tprivate String emailAddress;\n\n\t@PreDestroy()\n\tpublic Integer getStudentId() {\n\t\t" +
                "return this.studentId;\n\t}\n\t\n\t@PreDestroy()\n\tpublic boolean getHappiness(String type) {\n\t\t" +
                "if (type.equals(\"happy\")) return this.isHappy;\n\t\treturn this.isSad;\n\t}\n\t\n\t@PreDestroy()\n\t" +
                "public static void makeUnhappy() {\n\t\tSystem.out.println(\"I'm sad\");\n\t}\n\t\n\t" +
                "public void throwTantrum() throws Exception {\n\t\tSystem.out.println(\"I'm sad\");\n\t}\n\n    @PreDestroy()\n    " +
                "public void throwRuntimeException() throws RuntimeException {\n        System.out.println(\"RuntimeException\");\n    }\n\n    " +
                "@PreDestroy()\n    public void throwNullPointerException() throws NullPointerException {\n        " +
                "System.out.println(\"NullPointerException\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwIOException() throws IOException {\n        System.out.println(\"IOException\");\n    }\n\n    " +
                "@PreDestroy()\n    public void throwCustomExceptions() throws CustomCheckedException, CustomUncheckedException {\n        " +
                "System.out.println(\"throwCustomExceptions\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwCustomUnCheckedException() throws CustomUncheckedException {\n        " +
                "System.out.println(\"CustomUncheckedException\");\n    }\n\n    @PreDestroy()\n    public void throwError() throws Error {\n        " +
                "System.out.println(\"throwError\");\n    }\n}\n\n\n\n";

        String newText42 = "package io.openliberty.sample.jakarta.annotations;\n\nimport jakarta.annotation.PreDestroy;\n" +
                "import jakarta.annotation.Resource;\n\nimport java.io.IOException;\n\n@Resource(type = Object.class, name = \"aa\") \n" +
                "public class PreDestroyAnnotation { \n\n    private Integer studentId;\n\t\n    private boolean isHappy;\n\n    " +
                "private boolean isSad;\n\n\tprivate String emailAddress;\n\n\t@PreDestroy()\n\tpublic Integer getStudentId() {\n\t\t" +
                "return this.studentId;\n\t}\n\t\n\t@PreDestroy()\n\tpublic boolean getHappiness(String type) {\n\t\t" +
                "if (type.equals(\"happy\")) return this.isHappy;\n\t\treturn this.isSad;\n\t}\n\t\n\t@PreDestroy()\n\t" +
                "public static void makeUnhappy() {\n\t\tSystem.out.println(\"I'm sad\");\n\t}\n\t\n\t@PreDestroy()\n\t" +
                "public void throwTantrum()  {\n\t\tSystem.out.println(\"I'm sad\");\n\t}\n\n    @PreDestroy()\n    " +
                "public void throwRuntimeException() throws RuntimeException {\n        System.out.println(\"RuntimeException\");\n    }\n\n    " +
                "@PreDestroy()\n    public void throwNullPointerException() throws NullPointerException {\n        " +
                "System.out.println(\"NullPointerException\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwIOException() throws IOException {\n        System.out.println(\"IOException\");\n    }\n\n    " +
                "@PreDestroy()\n    public void throwCustomExceptions() throws CustomCheckedException, CustomUncheckedException {\n        " +
                "System.out.println(\"throwCustomExceptions\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwCustomUnCheckedException() throws CustomUncheckedException {\n        " +
                "System.out.println(\"CustomUncheckedException\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwError() throws Error {\n        System.out.println(\"throwError\");\n    }\n}\n\n\n\n";

        JakartaJavaCodeActionParams codeActionParams41 = createCodeActionParams(uri, d4);
        TextEdit te42 = te(0, 0, 72, 0, newText41);
        CodeAction ca42 = ca(uri, "Remove @PreDestroy", d4, te42);
        TextEdit te43 = te(0, 0, 72, 0, newText42);
        CodeAction ca43 = ca(uri, "Remove all checked exceptions.", d4, te43);
        assertJavaCodeAction(codeActionParams41, utils, ca42, ca43);

        String newText51 = "package io.openliberty.sample.jakarta.annotations;\n\nimport jakarta.annotation.PreDestroy;\n" +
                "import jakarta.annotation.Resource;\n\nimport java.io.IOException;\n\n@Resource(type = Object.class, name = \"aa\") \n" +
                "public class PreDestroyAnnotation { \n\n    private Integer studentId;\n\t\n    private boolean isHappy;\n\n    " +
                "private boolean isSad;\n\n\tprivate String emailAddress;\n\n\t@PreDestroy()\n\tpublic Integer getStudentId() {\n\t\t" +
                "return this.studentId;\n\t}\n\t\n\t@PreDestroy()\n\tpublic boolean getHappiness(String type) {\n\t\t" +
                "if (type.equals(\"happy\")) return this.isHappy;\n\t\treturn this.isSad;\n\t}\n\t\n\t@PreDestroy()\n\t" +
                "public static void makeUnhappy() {\n\t\tSystem.out.println(\"I'm sad\");\n\t}\n\t\n\t@PreDestroy()\n\t" +
                "public void throwTantrum() throws Exception {\n\t\tSystem.out.println(\"I'm sad\");\n\t}\n\n    " +
                "@PreDestroy()\n    public void throwRuntimeException() throws RuntimeException {\n        " +
                "System.out.println(\"RuntimeException\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwNullPointerException() throws NullPointerException {\n        " +
                "System.out.println(\"NullPointerException\");\n    }\n\n    public void throwIOException() throws IOException {\n        " +
                "System.out.println(\"IOException\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwCustomExceptions() throws CustomCheckedException, CustomUncheckedException {\n        " +
                "System.out.println(\"throwCustomExceptions\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwCustomUnCheckedException() throws CustomUncheckedException {\n        " +
                "System.out.println(\"CustomUncheckedException\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwError() throws Error {\n        System.out.println(\"throwError\");\n    }\n}\n\n\n\n";

        String newText52 = "package io.openliberty.sample.jakarta.annotations;\n\nimport jakarta.annotation.PreDestroy;\n" +
                "import jakarta.annotation.Resource;\n\nimport java.io.IOException;\n\n@Resource(type = Object.class, name = \"aa\") \n" +
                "public class PreDestroyAnnotation { \n\n    private Integer studentId;\n\t\n    private boolean isHappy;\n\n    " +
                "private boolean isSad;\n\n\tprivate String emailAddress;\n\n\t@PreDestroy()\n\tpublic Integer getStudentId() {\n\t\t" +
                "return this.studentId;\n\t}\n\t\n\t@PreDestroy()\n\tpublic boolean getHappiness(String type) {\n\t\t" +
                "if (type.equals(\"happy\")) return this.isHappy;\n\t\treturn this.isSad;\n\t}\n\t\n\t@PreDestroy()\n\t" +
                "public static void makeUnhappy() {\n\t\tSystem.out.println(\"I'm sad\");\n\t}\n\t\n\t@PreDestroy()\n\t" +
                "public void throwTantrum() throws Exception {\n\t\tSystem.out.println(\"I'm sad\");\n\t}\n\n    " +
                "@PreDestroy()\n    public void throwRuntimeException() throws RuntimeException {\n        " +
                "System.out.println(\"RuntimeException\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwNullPointerException() throws NullPointerException {\n        " +
                "System.out.println(\"NullPointerException\");\n    }\n\n    @PreDestroy()\n    public void throwIOException()  {\n        " +
                "System.out.println(\"IOException\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwCustomExceptions() throws CustomCheckedException, CustomUncheckedException {\n        " +
                "System.out.println(\"throwCustomExceptions\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwCustomUnCheckedException() throws CustomUncheckedException {\n        " +
                "System.out.println(\"CustomUncheckedException\");\n    }\n\n    @PreDestroy()\n    " +
                "public void throwError() throws Error {\n        System.out.println(\"throwError\");\n    }\n}\n\n\n\n";

        JakartaJavaCodeActionParams codeActionParams51 = createCodeActionParams(uri, d5);
        TextEdit te52 = te(0, 0, 72, 0, newText51);
        CodeAction ca52 = ca(uri, "Remove @PreDestroy", d5, te52);
        TextEdit te53 = te(0, 0, 72, 0, newText52);
        CodeAction ca53 = ca(uri, "Remove all checked exceptions.", d5, te53);
        assertJavaCodeAction(codeActionParams51, utils, ca52, ca53);

    }

}
