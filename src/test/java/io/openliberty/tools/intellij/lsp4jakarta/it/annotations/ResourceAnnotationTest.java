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
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
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
    public void ResourceAnnotation() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/annotations/ResourceAnnotation.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // expected annotations
        Diagnostic d1 = d(22, 0, 22, "The @Resource annotation must define the attribute 'type'.",
                DiagnosticSeverity.Error, "jakarta-annotations", "MissingResourceTypeAttribute");

        Diagnostic d2 = d(39, 0, 30, "The @Resource annotation must define the attribute 'name'.",
                DiagnosticSeverity.Error, "jakarta-annotations", "MissingResourceNameAttribute");

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2);

        //TODO: uncomment this if condition, once refactoring is done for all the quick fixes.
//        if (CHECK_CODE_ACTIONS) {
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d1);
        String newText = "package io.openliberty.sample.jakarta.annotations;\n\nimport jakarta.annotation.Resource;" +
                "\n\n@Resource(type = Object.class, name = \"aa\")\npublic class ResourceAnnotation " +
                "{\n\n    private Integer studentId;\n\n\n	@Resource(shareable = true)\n    " +
                "private boolean isHappy;\n\n	@Resource(name = \"test\")\n   " +
                " private boolean isSad;\n\n\n    private String emailAddress;\n\n\n}\n\n" +
                "@Resource(name = \"aa\",type=\"\")\nclass PostDoctoralStudent {\n\n    " +
                "private Integer studentId;\n\n\n	@Resource(shareable = true)\n    " +
                "private boolean isHappy;\n\n	@Resource\n    private boolean isSad;\n\n\n    " +
                "private String emailAddress;\n\n}\n\n@Resource(type = Object.class)\nclass " +
                "MasterStudent {\n\n    private Integer studentId;\n\n} \n";
        TextEdit te = te(0, 0, 45, 0, newText);
        CodeAction ca = ca(uri, "Add type to jakarta.annotation.Resource", d1, te);
        assertJavaCodeAction(codeActionParams, utils, ca);

        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d2);
        newText = "package io.openliberty.sample.jakarta.annotations;\n\n" +
                "import jakarta.annotation.Resource;\n\n@Resource(type = Object.class, " +
                "name = \"aa\")\npublic class ResourceAnnotation {\n\n    private Integer studentId;" +
                "\n\n\n	@Resource(shareable = true)\n    private boolean isHappy;\n\n	" +
                "@Resource(name = \"test\")\n    private boolean isSad;\n\n\n    " +
                "private String emailAddress;\n\n\n}\n\n@Resource(name = \"aa\")\n" +
                "class PostDoctoralStudent {\n\n    private Integer studentId;\n\n\n	" +
                "@Resource(shareable = true)\n    private boolean isHappy;\n\n	@Resource\n    " +
                "private boolean isSad;\n\n\n    private String emailAddress;\n\n}\n\n" +
                "@Resource(type = Object.class, name=\"\")\nclass MasterStudent {\n\n    " +
                "private Integer studentId;\n\n} \n";
        TextEdit te1 = te(0, 0, 45, 0, newText);
        CodeAction ca1 = ca(uri, "Add name to jakarta.annotation.Resource", d2, te1);
        assertJavaCodeAction(codeActionParams1, utils, ca1);
//        }
    }
}
