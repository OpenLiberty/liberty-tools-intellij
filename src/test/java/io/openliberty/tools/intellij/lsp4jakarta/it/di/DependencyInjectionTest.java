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

package io.openliberty.tools.intellij.lsp4jakarta.it.di;

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
public class DependencyInjectionTest extends BaseJakartaTest {

    @Test
    @Ignore
    public void DependencyInjectionDiagnostics() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/di/GreetingServlet.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaDiagnosticsParams diagnosticsParams = new JakartaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        /* create expected diagnostics
         * 
         */
        Diagnostic d1 = JakartaForJavaAssert.d(17, 27, 35, "The @Inject annotation must not define a final field.",
                DiagnosticSeverity.Error, "jakarta-di", "RemoveInjectOrFinal");
        d1.setData("field");

        Diagnostic d2 = JakartaForJavaAssert.d(33, 25, 39, "The @Inject annotation must not define an abstract method.",
                DiagnosticSeverity.Error, "jakarta-di", "RemoveInjectOrAbstract");
        d2.setData("method");
        
        Diagnostic d3 = JakartaForJavaAssert.d(26, 22, 33, "The @Inject annotation must not define a final method.",
                DiagnosticSeverity.Error, "jakarta-di", "RemoveInjectOrFinal");
        d3.setData("method");
 
        Diagnostic d4 = JakartaForJavaAssert.d(43, 23, 36, "The @Inject annotation must not define a generic method.",
                DiagnosticSeverity.Error, "jakarta-di", "RemoveInjectForGeneric");
        d4.setData("method");
        
        Diagnostic d5 = JakartaForJavaAssert.d(37, 23, 35, "The @Inject annotation must not define a static method.",
                DiagnosticSeverity.Error, "jakarta-di", "RemoveInjectOrStatic");
        d5.setData("method");
        

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3, d4, d5);
        
        
        /* create expected quickFixes
         * 
         */
        
        // for d1
        JakartaJavaCodeActionParams codeActionParams = JakartaForJavaAssert.createCodeActionParams(uri, d1);
        TextEdit te = JakartaForJavaAssert.te(16, 4, 17, 4,
                "");
        CodeAction ca = JakartaForJavaAssert.ca(uri, "Remove @Inject", d1, te);
        TextEdit te1 = JakartaForJavaAssert.te(17, 11, 17, 17,
                "");
        CodeAction ca1 = JakartaForJavaAssert.ca(uri, "Remove the 'final' modifier from this field", d1, te1);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams, utils, ca, ca1);
        
        // for d2
        codeActionParams = JakartaForJavaAssert.createCodeActionParams(uri, d2);
        te = JakartaForJavaAssert.te(32, 4, 33, 4,
                "");
        ca = JakartaForJavaAssert.ca(uri, "Remove @Inject", d2, te);
        te1 = JakartaForJavaAssert.te(33, 10, 33, 19,
                "");
        ca1 = JakartaForJavaAssert.ca(uri, "Remove the 'abstract' modifier from this method", d2, te1);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams, utils, ca, ca1);
        
        // for d3
        codeActionParams = JakartaForJavaAssert.createCodeActionParams(uri, d3);
        te = JakartaForJavaAssert.te(25, 4, 26, 4,
                "");
        ca = JakartaForJavaAssert.ca(uri, "Remove @Inject", d3, te);
        te1 = JakartaForJavaAssert.te(26, 10, 26, 16,
                "");
        ca1 = JakartaForJavaAssert.ca(uri, "Remove the 'final' modifier from this method", d3, te1);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams, utils, ca, ca1);
        
        // for d4
        codeActionParams = JakartaForJavaAssert.createCodeActionParams(uri, d4);
        te = JakartaForJavaAssert.te(42, 4, 43, 4,
                "");
        ca = JakartaForJavaAssert.ca(uri, "Remove @Inject", d4, te);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams, utils, ca);
        
        // for d5
        codeActionParams = JakartaForJavaAssert.createCodeActionParams(uri, d5);
        te = JakartaForJavaAssert.te(36, 4, 37, 4,
                "");
        ca = JakartaForJavaAssert.ca(uri, "Remove @Inject", d5, te);
        te1 = JakartaForJavaAssert.te(37, 10, 37, 17,
                "");
        ca1 = JakartaForJavaAssert.ca(uri, "Remove the 'static' modifier from this method", d5, te1);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams, utils, ca, ca1);
    }
}
