/*******************************************************************************
* Copyright (c) 2021, 2024 IBM Corporation and others.
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
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;

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
        
        Diagnostic d1 = d(20, 16, 28, "A method with the @PreDestroy annotation must not have any parameters.",
                DiagnosticSeverity.Error, "jakarta-annotations", "PreDestroyParams");
        
        Diagnostic d2 = d(26, 20, 31, "A method with the @PreDestroy annotation must not be static.",
                DiagnosticSeverity.Error, "jakarta-annotations", "PreDestroyStatic");
        d2.setData("makeUnhappy");
        
        Diagnostic d3 = d(31, 13, 25, "A method with the @PreDestroy annotation must not throw checked exceptions.",
                DiagnosticSeverity.Warning, "jakarta-annotations", "PreDestroyException");

        assertJavaDiagnostics(diagnosticsParams, utils, d2, d1, d3);

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d1);
        String newText = "package io.openliberty.sample.jakarta.annotations;\n\n" +
                "import jakarta.annotation.PreDestroy;\n" +
                "import jakarta.annotation.Resource;\n\n" +
                "@Resource(type = Object.class, name = \"aa\") \n" +
                "public class PreDestroyAnnotation { \n\n" +
                "    private Integer studentId;\n	\n    private boolean isHappy;\n\n" +
                "    private boolean isSad;\n	\n	@PreDestroy()\n" +
                "	public Integer getStudentId() {\n" +
                "		return this.studentId;\n	}\n	\n" +
                "	public boolean getHappiness(String type) {\n" +
                "		if (type.equals(\"happy\")) return this.isHappy;\n" +
                "		return this.isSad;\n	}\n	\n" +
                "	@PreDestroy()\n	public static void makeUnhappy() {\n" +
                "		System.out.println(\"I'm sad\");\n	}\n	\n" +
                "	@PreDestroy()\n	public void throwTantrum() throws Exception {\n" +
                "		System.out.println(\"I'm sad\");\n" +
                "	}\n\n\n    private String emailAddress;\n\n\n}\n\n\n\n";

        String newText3 = "package io.openliberty.sample.jakarta.annotations;\n\n" +
                "import jakarta.annotation.PreDestroy;\n" +
                "import jakarta.annotation.Resource;\n\n" +
                "@Resource(type = Object.class, name = \"aa\") \n" +
                "public class PreDestroyAnnotation { \n\n    " +
                "private Integer studentId;\n	\n    " +
                "private boolean isHappy;\n\n    " +
                "private boolean isSad;\n	\n	" +
                "@PreDestroy()\n	" +
                "public Integer getStudentId() {\n		" +
                "return this.studentId;\n	}\n	\n	" +
                "@PreDestroy()\n	" +
                "public boolean getHappiness() {\n		" +
                "if (type.equals(\"happy\")) return this.isHappy;\n		" +
                "return this.isSad;\n	}\n	\n	" +
                "@PreDestroy()\n	" +
                "public static void makeUnhappy() {\n		" +
                "System.out.println(\"I'm sad\");\n	}\n	\n	" +
                "@PreDestroy()\n	" +
                "public void throwTantrum() throws Exception {\n		" +
                "System.out.println(\"I'm sad\");\n	}\n\n\n    " +
                "private String emailAddress;\n\n\n}\n\n\n\n";

        TextEdit te = te(0, 0, 43, 0, newText);
        CodeAction ca = ca(uri, "Remove @PreDestroy", d1, te);
        TextEdit te1 = te(0, 0, 43, 0, newText3);
        CodeAction ca1 = ca(uri, "Remove all parameters", d1, te1);
        assertJavaCodeAction(codeActionParams, utils, ca, ca1);

        String newText1 = "package io.openliberty.sample.jakarta.annotations;\n\n" +
                "import jakarta.annotation.PreDestroy;\n" +
                "import jakarta.annotation.Resource;\n\n" +
                "@Resource(type = Object.class, name = \"aa\") \n" +
                "public class PreDestroyAnnotation { \n\n" +
                "    private Integer studentId;\n	\n    private boolean isHappy;\n\n" +
                "    private boolean isSad;\n	\n	@PreDestroy()\n" +
                "	public Integer getStudentId() {\n		return this.studentId;\n" +
                "	}\n	\n	@PreDestroy()\n	public boolean getHappiness(String type) {\n" +
                "		if (type.equals(\"happy\")) return this.isHappy;\n" +
                "		return this.isSad;\n	}\n	\n" +
                "	public static void makeUnhappy() {\n" +
                "		System.out.println(\"I'm sad\");\n	}\n	\n	@PreDestroy()\n" +
                "	public void throwTantrum() throws Exception {\n" +
                "		System.out.println(\"I'm sad\");\n	}\n\n\n" +
                "    private String emailAddress;\n\n\n}\n\n\n\n";

        String newText2 = "package io.openliberty.sample.jakarta.annotations;\n\n" +
                "import jakarta.annotation.PreDestroy;\n" +
                "import jakarta.annotation.Resource;\n\n" +
                "@Resource(type = Object.class, name = \"aa\") \n" +
                "public class PreDestroyAnnotation { \n\n" +
                "    private Integer studentId;\n	\n    private boolean isHappy;\n\n" +
                "    private boolean isSad;\n	\n	@PreDestroy()\n" +
                "	public Integer getStudentId() {\n		return this.studentId;\n" +
                "	}\n	\n	@PreDestroy()\n	public boolean getHappiness(String type) {\n" +
                "		if (type.equals(\"happy\")) return this.isHappy;\n" +
                "		return this.isSad;\n	}\n	\n" +
                "	@PreDestroy()\n	public void makeUnhappy() {\n" +
                "		System.out.println(\"I'm sad\");\n	}\n	\n	@PreDestroy()\n" +
                "	public void throwTantrum() throws Exception {\n" +
                "		System.out.println(\"I'm sad\");\n	}\n\n\n" +
                "    private String emailAddress;\n\n\n}\n\n\n\n";

        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d2);
        TextEdit te2 = te(0, 0, 43, 0, newText1);
        CodeAction ca2 = ca(uri, "Remove @PreDestroy", d2, te2);
        TextEdit te3 = te(0, 0, 43, 0, newText2);
        CodeAction ca3 = ca(uri, "Remove the 'static' modifier from this method", d2, te3);
        assertJavaCodeAction(codeActionParams1, utils, ca2, ca3);
    }

}
