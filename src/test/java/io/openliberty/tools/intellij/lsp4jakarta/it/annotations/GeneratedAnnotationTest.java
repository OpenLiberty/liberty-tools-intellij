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
import io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;

@RunWith(JUnit4.class)
public class GeneratedAnnotationTest extends BaseJakartaTest {

    @Test
    public void GeneratedAnnotation() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/annotations/GeneratedAnnotation.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // expected annotations
        Diagnostic d1 = JakartaForJavaAssert.d(7, 4, 63,
                "The @Generated annotation must define the attribute 'date' following the ISO 8601 standard.",
                DiagnosticSeverity.Error, "jakarta-annotations", "InvalidDateFormat");
        
        Diagnostic d2 = JakartaForJavaAssert.d(13, 4, 70,
                "The @Generated annotation must define the attribute 'date' following the ISO 8601 standard.",
                DiagnosticSeverity.Error, "jakarta-annotations", "InvalidDateFormat");

        
        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d1, d2);
    }

    @Test
    public void testIncorrectGeneratedAnnotation() throws Exception {
        // Set up the module and file where a non-Jakarta Generated annotation is used
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        // The file path to a Java file that includes an incorrectly qualified Generated annotation
        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) +
                        "/src/main/java/io/openliberty/sample/jakarta/annotations/IncorrectGeneratedAnnotation.java"
        );
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        // Adding a test to ensure no diagnostics are triggered for any non-matching annotation or import path similar to "jakarta.annotation.Generated"
        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Ensure no diagnostics are generated for any annotation or import that is not exactly "jakarta.annotation.Generated"
        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils);
    }

}
