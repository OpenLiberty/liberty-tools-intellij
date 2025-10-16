/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
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
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;

@RunWith(JUnit4.class)
public class InnerClassInjectionTest extends BaseJakartaTest {

    @Test
    public void DependencyInjectionDiagnostics() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/di/InnerClassInjection.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d1 = JakartaForJavaAssert.d(22, 22, 26, "Cannot inject " +
                        "non-static inner class. Injection target must be a top-level or static nested class.",
                DiagnosticSeverity.Error, "jakarta-di", "RemoveInjectForInnerClass");
        d1.setData("io.openliberty.sample.jakarta.di.InnerClassInjection.InnerBean");
        Diagnostic d2 = JakartaForJavaAssert.d(29, 16, 23, "Cannot inject " +
                        "non-static inner class. Injection target must be a top-level or static nested class.",
                DiagnosticSeverity.Error, "jakarta-di", "RemoveInjectForInnerClass");
        d2.setData("void");
        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d1, d2);


        JakartaJavaCodeActionParams codeActionParams1 = JakartaForJavaAssert.createCodeActionParams(uri, d1);
        String newText11 = "/*******************************************************************************\n * Copyright (c) 2025 IBM Corporation and others.\n *\n * This program and the accompanying materials are made available under the\n * terms of the Eclipse Public License v. 2.0 which is available at\n * http://www.eclipse.org/legal/epl-2.0.\n *\n * SPDX-License-Identifier: EPL-2.0\n *\n * Contributors:\n *     IBM Corporation - initial implementation\n *******************************************************************************/\npackage io.openliberty.sample.jakarta.di;\n\nimport jakarta.enterprise.context.ApplicationScoped;\nimport jakarta.inject.Inject;\nimport jakarta.mail.Service;\n\n@ApplicationScoped\npublic class InnerClassInjection {\n\n    @Inject\n    private InnerBean bean;\n\n    public InnerBean getBean() {\n        return bean;\n    }\n\n    @Inject\n    public void setBean(InnerClassInjection.InnerBean bean) {\n        this.bean = bean;\n    }\n\n    public static class InnerBean {\n\n        @Inject\n        private Service service;\n    }\n}";

        String newText12 = "/*******************************************************************************\n * Copyright (c) 2025 IBM Corporation and others.\n *\n * This program and the accompanying materials are made available under the\n * terms of the Eclipse Public License v. 2.0 which is available at\n * http://www.eclipse.org/legal/epl-2.0.\n *\n * SPDX-License-Identifier: EPL-2.0\n *\n * Contributors:\n *     IBM Corporation - initial implementation\n *******************************************************************************/\npackage io.openliberty.sample.jakarta.di;\n\nimport jakarta.enterprise.context.ApplicationScoped;\nimport jakarta.inject.Inject;\nimport jakarta.mail.Service;\n\n@ApplicationScoped\npublic class InnerClassInjection {\n\n    private InnerBean bean;\n\n    public InnerBean getBean() {\n        return bean;\n    }\n\n    @Inject\n    public void setBean(InnerClassInjection.InnerBean bean) {\n        this.bean = bean;\n    }\n\n    public class InnerBean {\n\n        @Inject\n        private Service service;\n    }\n}";

        TextEdit te11 = JakartaForJavaAssert.te(0, 0, 38, 1, newText11);
        TextEdit te12 = JakartaForJavaAssert.te(0, 0, 38, 1, newText12);
        CodeAction ca11 = JakartaForJavaAssert.ca(uri, "Add 'static' modifier to the nested class", d1, te11);
        CodeAction ca12 = JakartaForJavaAssert.ca(uri, "Remove @Inject", d1, te12);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams1, utils, ca11, ca12);

        JakartaJavaCodeActionParams codeActionParams2 = JakartaForJavaAssert.createCodeActionParams(uri, d2);

        String newText21 = "/*******************************************************************************\n * Copyright (c) 2025 IBM Corporation and others.\n *\n * This program and the accompanying materials are made available under the\n * terms of the Eclipse Public License v. 2.0 which is available at\n * http://www.eclipse.org/legal/epl-2.0.\n *\n * SPDX-License-Identifier: EPL-2.0\n *\n * Contributors:\n *     IBM Corporation - initial implementation\n *******************************************************************************/\npackage io.openliberty.sample.jakarta.di;\n\nimport jakarta.enterprise.context.ApplicationScoped;\nimport jakarta.inject.Inject;\nimport jakarta.mail.Service;\n\n@ApplicationScoped\npublic class InnerClassInjection {\n\n    @Inject\n    private InnerBean bean;\n\n    public InnerBean getBean() {\n        return bean;\n    }\n\n    @Inject\n    public void setBean(InnerClassInjection.InnerBean bean) {\n        this.bean = bean;\n    }\n\n    public static class InnerBean {\n\n        @Inject\n        private Service service;\n    }\n}";

        String newText22 = "/*******************************************************************************\n * Copyright (c) 2025 IBM Corporation and others.\n *\n * This program and the accompanying materials are made available under the\n * terms of the Eclipse Public License v. 2.0 which is available at\n * http://www.eclipse.org/legal/epl-2.0.\n *\n * SPDX-License-Identifier: EPL-2.0\n *\n * Contributors:\n *     IBM Corporation - initial implementation\n *******************************************************************************/\npackage io.openliberty.sample.jakarta.di;\n\nimport jakarta.enterprise.context.ApplicationScoped;\nimport jakarta.inject.Inject;\nimport jakarta.mail.Service;\n\n@ApplicationScoped\npublic class InnerClassInjection {\n\n    @Inject\n    private InnerBean bean;\n\n    public InnerBean getBean() {\n        return bean;\n    }\n\n    public void setBean(InnerClassInjection.InnerBean bean) {\n        this.bean = bean;\n    }\n\n    public class InnerBean {\n\n        @Inject\n        private Service service;\n    }\n}";

        TextEdit te21 = JakartaForJavaAssert.te(0, 0, 38, 1, newText21);
        TextEdit te22 = JakartaForJavaAssert.te(0, 0, 38, 1, newText22);
        CodeAction ca21 = JakartaForJavaAssert.ca(uri, "Add 'static' modifier to the nested class", d2, te21);
        CodeAction ca22 = JakartaForJavaAssert.ca(uri, "Remove @Inject", d2, te22);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams2, utils, ca21, ca22);


    }
}
