/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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

package io.openliberty.tools.intellij.lsp4jakarta.it.servlet;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.lsp4jakarta.it.core.BaseJakartaTest;
import io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4jakarta.commons.JakartaDiagnosticsParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;

@RunWith(JUnit4.class)
public class JakartaServletTest extends BaseJakartaTest {

    @Test
    public void ExtendWebServlet() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/servlet/DontExtendHttpServlet.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaDiagnosticsParams diagnosticsParams = new JakartaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // expected
        Diagnostic d = JakartaForJavaAssert.d(5, 13, 34, "Annotated classes with @WebServlet must extend the HttpServlet class.",
                DiagnosticSeverity.Error, "jakarta-servlet", "ExtendHttpServlet");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d);

        //TODO: this condition will be enabled when all quickfixes are refactored.
        // if (CHECK_CODE_ACTIONS) {
        // test associated quick-fix code action
        JakartaJavaCodeActionParams codeActionParams = JakartaForJavaAssert.createCodeActionParams(uri, d);
        String newText = "package io.openliberty.sample.jakarta.servlet;\n\n" +
                "import jakarta.servlet.annotation.WebServlet;\nimport jakarta.servlet.http.HttpServlet;\n\n" +
                "@WebServlet(name = \"demoServlet\", urlPatterns = {\"/demo\"})\n" +
                "public class DontExtendHttpServlet extends HttpServlet {\n\n}";

        TextEdit te = JakartaForJavaAssert.te(0, 0, 7, 1, newText);
        CodeAction ca = JakartaForJavaAssert.ca(uri, "Let 'DontExtendHttpServlet' extend 'HttpServlet'", d, te);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams, utils, ca);
        // }
    }

    @Test
    public void CompleteWebServletAnnotation() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/servlet/InvalidWebServlet.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaDiagnosticsParams diagnosticsParams = new JakartaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d = JakartaForJavaAssert.d(9, 0, 13,
                "The @WebServlet annotation must define the attribute 'urlPatterns' or 'value'.",
                DiagnosticSeverity.Error, "jakarta-servlet", "CompleteHttpServletAttributes");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d);

        if (CHECK_CODE_ACTIONS) {
            JakartaJavaCodeActionParams codeActionParams = JakartaForJavaAssert.createCodeActionParams(uri, d);
            TextEdit te1 = JakartaForJavaAssert.te(9, 0, 10, 0, "@WebServlet(value = \"\")\n");
            CodeAction ca1 = JakartaForJavaAssert.ca(uri, "Add the `value` attribute to @WebServlet", d, te1);

            TextEdit te2 = JakartaForJavaAssert.te(9, 0, 10, 0, "@WebServlet(urlPatterns = \"\")\n");
            CodeAction ca2 = JakartaForJavaAssert.ca(uri, "Add the `urlPatterns` attribute to @WebServlet", d, te2);
            JakartaForJavaAssert.assertJavaCodeAction(codeActionParams, utils, ca1, ca2);
        }
    }

}
