/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation, Matthew Shocrylas, Bera Sogut and others.
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
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;

@RunWith(JUnit4.class)
public class ResourceMethodTest extends BaseJakartaTest {

    @Test
    public void NonPublicMethod() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/jaxrs/NotPublicResourceMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));


        Diagnostic d = JakartaForJavaAssert.d(20, 17, 30, "Only public methods can be exposed as resource methods.",
                DiagnosticSeverity.Error, "jakarta-jax_rs", "NonPublicResourceMethod");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d);

        String newText = "/*******************************************************************************\n" +
                " * Copyright (c) 2021 IBM Corporation, Matthew Shocrylas and others.\n *\n" +
                " * This program and the accompanying materials are made available under the\n" +
                " * terms of the Eclipse Public License v. 2.0 which is available at\n" +
                " * http://www.eclipse.org/legal/epl-2.0.\n *\n * SPDX-License-Identifier: EPL-2.0\n *\n" +
                " * Contributors:\n *     IBM Corporation, Matthew Shocrylas - initial API and implementation\n" +
                " *******************************************************************************/\n\n" +
                "package io.openliberty.sample.jakarta.jax_rs;\n\nimport jakarta.ws.rs.HEAD;\n\n" +
                "public class NotPublicResourceMethod {\n\n    @HEAD\n    public void privateMethod() {\n\n    }\n\n}\n";

        JakartaJavaCodeActionParams codeActionParams = JakartaForJavaAssert.createCodeActionParams(uri, d);
        TextEdit te = JakartaForJavaAssert.te(0, 0, 25, 0, newText); // range may need to change
        CodeAction ca = JakartaForJavaAssert.ca(uri, "Make method public", d, te);

        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams, utils, ca);
    }

    @Test
    public void multipleEntityParamsMethod() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/jaxrs/MultipleEntityParamsResourceMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d1 = JakartaForJavaAssert.d(22, 13, 46, "Resource methods cannot have more than one entity parameter.",
                DiagnosticSeverity.Error, "jakarta-jax_rs", "ResourceMethodMultipleEntityParams");
        Diagnostic d2 = JakartaForJavaAssert.d(32, 13, 55, "Resource methods cannot have more than one entity parameter.",
                DiagnosticSeverity.Error, "jakarta-jax_rs", "ResourceMethodMultipleEntityParams");
        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d1, d2);

        // Test for quick-fix code action
        JakartaJavaCodeActionParams codeActionParams = JakartaForJavaAssert.createCodeActionParams(uri, d1);
        String newText1 = "/*******************************************************************************\n*" +
                " Copyright (c) 2021 IBM Corporation.\n*\n*" +
                " This program and the accompanying materials are made available under the\n" +
                "* terms of the Eclipse Public License v. 2.0 which is available at\n" +
                "* http://www.eclipse.org/legal/epl-2.0.\n*\n" +
                "* SPDX-License-Identifier: EPL-2.0\n*\n" +
                "* Contributors:\n*     Bera Sogut\n" +
                "*******************************************************************************/\n\n" +
                "package io.openliberty.sample.jakarta.jax_rs;\n\n" +
                "import jakarta.ws.rs.BeanParam;\n" +
                "import jakarta.ws.rs.DELETE;\n" +
                "import jakarta.ws.rs.FormParam;\n\n" +
                "public class MultipleEntityParamsResourceMethod {\n\n\t" +
                "@DELETE\n\t" +
                "public void resourceMethodWithTwoEntityParams(String entityParam1, @FormParam(value = \"\") String nonEntityParam) {\n        " +
                "\n    }\n\n\t" +
                "@DELETE\n\tpublic void resourceMethodWithTwoBeanParams(@BeanParam RequestBean requestBean, @BeanParam SessionBean sessionBean) {\n        \n    }\n\n\t" +
                "@DELETE\n\tpublic void resourceMethodWithBeanParamsAndEnityParams(String entityString, int entityInt, @BeanParam RequestBean requestBean, @BeanParam SessionBean sessionBean) {\n        \n    }\n}\n";
        TextEdit te1 = JakartaForJavaAssert.te(0, 0, 36, 0, newText1);
        CodeAction ca1 = JakartaForJavaAssert.ca(uri, "Remove all entity parameters except entityParam1", d1, te1);

        String newText2 = "/*******************************************************************************\n*" +
                " Copyright (c) 2021 IBM Corporation.\n*\n*" +
                " This program and the accompanying materials are made available under the\n" +
                "* terms of the Eclipse Public License v. 2.0 which is available at\n" +
                "* http://www.eclipse.org/legal/epl-2.0.\n" +
                "*\n* SPDX-License-Identifier: EPL-2.0\n" +
                "*\n* Contributors:\n*     Bera Sogut\n" +
                "*******************************************************************************/\n\n" +
                "package io.openliberty.sample.jakarta.jax_rs;\n\n" +
                "import jakarta.ws.rs.BeanParam;\n" +
                "import jakarta.ws.rs.DELETE;\n" +
                "import jakarta.ws.rs.FormParam;\n\n" +
                "public class MultipleEntityParamsResourceMethod {\n\n\t" +
                "@DELETE\n\t" +
                "public void resourceMethodWithTwoEntityParams(@FormParam(value = \"\") String nonEntityParam, int entityParam2) {\n        " +
                "\n    }\n\n\t" +
                "@DELETE\n\tpublic void resourceMethodWithTwoBeanParams(@BeanParam RequestBean requestBean, @BeanParam SessionBean sessionBean) {\n        \n    }\n\n\t" +
                "@DELETE\n\tpublic void resourceMethodWithBeanParamsAndEnityParams(String entityString, int entityInt, @BeanParam RequestBean requestBean, @BeanParam SessionBean sessionBean) {\n        \n    }\n}\n";
        TextEdit te2 = JakartaForJavaAssert.te(0, 0, 36, 0, newText2);
        CodeAction ca2 = JakartaForJavaAssert.ca(uri, "Remove all entity parameters except entityParam2", d1, te2);

        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams, utils, ca1, ca2);
    }

}
