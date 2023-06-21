/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation, Matthew Shocrylas, Bera Sogut and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation, Matthew Shocrylas - initial API and implementation, Bera Sogut
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.it.jaxrs;

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
public class ResourceMethodTest extends BaseJakartaTest {
    
    @Test
    @Ignore
    public void NonPublicMethod() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/jaxrs/NotPublicResourceMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();
        
        JakartaDiagnosticsParams diagnosticsParams = new JakartaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));
        
        
        Diagnostic d = JakartaForJavaAssert.d(20, 17, 30, "Only public methods can be exposed as resource methods",
                DiagnosticSeverity.Error, "jakarta-jax_rs", "NonPublicResourceMethod");
        
        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d);
        
        // Test for quick-fix code action
        JakartaJavaCodeActionParams codeActionParams = JakartaForJavaAssert.createCodeActionParams(uri, d);
        TextEdit te = JakartaForJavaAssert.te(20, 4, 20, 11, "public"); // range may need to change
        CodeAction ca = JakartaForJavaAssert.ca(uri, "Make method public", d, te);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams, utils, ca);
    }

    @Test
    @Ignore
    public void multipleEntityParamsMethod() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/jaxrs/MultipleEntityParamsResourceMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaDiagnosticsParams diagnosticsParams = new JakartaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));


        Diagnostic d = JakartaForJavaAssert.d(21, 13, 46, "Resource methods cannot have more than one entity parameter",
                DiagnosticSeverity.Error, "jakarta-jax_rs", "ResourceMethodMultipleEntityParams");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d);


        // Test for quick-fix code action
        JakartaJavaCodeActionParams codeActionParams = JakartaForJavaAssert.createCodeActionParams(uri, d);

        TextEdit te1 = JakartaForJavaAssert.te(21, 112, 21, 130, "");
        CodeAction ca1 = JakartaForJavaAssert.ca(uri, "Remove all entity parameters except entityParam1", d, te1);

        TextEdit te2 = JakartaForJavaAssert.te(21, 47, 21, 68, "");
        CodeAction ca2 = JakartaForJavaAssert.ca(uri, "Remove all entity parameters except entityParam2", d, te2);

        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams, utils, ca1, ca2);
    }

}
