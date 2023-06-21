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
public class ResourceAnnotationTest extends BaseJakartaTest {

    @Test
    @Ignore
    public void ResourceAnnotation() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/annotations/ResourceAnnotation.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaDiagnosticsParams diagnosticsParams = new JakartaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // expected annotations
        Diagnostic d1 = d(22, 0, 22, "The @Resource annotation must define the attribute 'type'.",
                DiagnosticSeverity.Error, "jakarta-annotations", "MissingResourceTypeAttribute");

        Diagnostic d2 = d(39, 0, 30, "The @Resource annotation must define the attribute 'name'.",
                DiagnosticSeverity.Error, "jakarta-annotations", "MissingResourceNameAttribute");


        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2);
        
        
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d1);
        TextEdit te = te(22, 0, 22, 22, "@Resource(name = \"aa\", type = \"\")");
        CodeAction ca= ca(uri, "Add type to jakarta.annotation.Resource", d1, te);
        assertJavaCodeAction(codeActionParams, utils, ca);
        
        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d2);
        TextEdit te1 = te(39, 0, 39, 30, "@Resource(type = \"\", name = \"\")");
        CodeAction ca1= ca(uri, "Add name to jakarta.annotation.Resource", d2, te1);
        assertJavaCodeAction(codeActionParams1, utils, ca1);

    }

}
