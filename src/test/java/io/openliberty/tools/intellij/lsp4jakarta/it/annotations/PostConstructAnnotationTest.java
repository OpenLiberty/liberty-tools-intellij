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
import org.eclipse.lsp4jakarta.commons.JakartaDiagnosticsParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;

import static io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert.*;

@RunWith(JUnit4.class)
public class PostConstructAnnotationTest extends BaseJakartaTest {

    @Test
    @Ignore
    public void GeneratedAnnotation() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/annotations/PostConstructAnnotation.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaDiagnosticsParams diagnosticsParams = new JakartaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // expected Diagnostics

        Diagnostic d1 = d(15, 19, 31, "A method with the @PostConstruct annotation must be void.",
                DiagnosticSeverity.Error, "jakarta-annotations", "PostConstructReturnType");

        Diagnostic d2 = d(20, 16, 28, "A method with the @PostConstruct annotation must not have any parameters.",
                DiagnosticSeverity.Error, "jakarta-annotations", "PostConstructParams");

        Diagnostic d3 = d(25, 16, 28, "A method with the @PostConstruct annotation must not throw checked exceptions.",
                DiagnosticSeverity.Warning, "jakarta-annotations", "PostConstructException");

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3);

        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d2);
        TextEdit te1 = te(19, 4, 20, 4, "");
        TextEdit te2 = te(20, 29, 20, 40, "");
        CodeAction ca1 = ca(uri, "Remove @PostConstruct", d2, te1);
        CodeAction ca2 = ca(uri, "Remove all parameters", d2, te2);
        assertJavaCodeAction(codeActionParams1, utils, ca1, ca2);
        
        JakartaJavaCodeActionParams codeActionParams2 = createCodeActionParams(uri, d1);
        TextEdit te3 = te(15, 11, 15, 18, "void");
        CodeAction ca3 = ca(uri, "Change return type to void", d1, te3);
        assertJavaCodeAction(codeActionParams2, utils, ca3);
    }

}
