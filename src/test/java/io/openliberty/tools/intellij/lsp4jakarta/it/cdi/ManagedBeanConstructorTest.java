/*******************************************************************************
* Copyright (c) 2021, 2023 IBM Corporation.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     Hani Damlaj
*******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.it.cdi;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.lsp4jakarta.it.core.BaseJakartaTest;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4jakarta.commons.JakartaDiagnosticsParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;

import static io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert.*;

@RunWith(JUnit4.class)
public class ManagedBeanConstructorTest extends BaseJakartaTest {

    @Test
    public void managedBeanAnnotations() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/ManagedBeanConstructor.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaDiagnosticsParams diagnosticsParams = new JakartaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // test expected diagnostic
        Diagnostic d = d(21, 8, 30,
                "The @Inject annotation must define a managed bean constructor that takes parameters, or the managed bean must resolve to having a no-arg constructor instead.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidManagedBeanConstructor");

        assertJavaDiagnostics(diagnosticsParams, utils, d);

        //TODO Enable the remaining test cases once the refactoring is completed
        if (CHECK_CODE_ACTIONS) {
            TextEdit te1 = te(15, 44, 21, 1,
                    "\nimport jakarta.inject.Inject;\n\n@Dependent\npublic class ManagedBeanConstructor {\n	private int a;\n	\n	@Inject\n	");
            CodeAction ca1 = ca(uri, "Insert @Inject", d, te1);
        }

        // test expected quick-fix
        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d);
        TextEdit te2 = te(0, 0, 25, 0, "/*******************************************************************************\n * Copyright (c) 2021 IBM Corporation.\n *\n * This program and the accompanying materials are made available under the\n * terms of the Eclipse Public License v. 2.0 which is available at\n * http://www.eclipse.org/legal/epl-2.0.\n *\n * SPDX-License-Identifier: EPL-2.0\n *\n * Contributors:\n *     Hani Damlaj\n *******************************************************************************/\n\npackage io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.Dependent;\n\n@Dependent\npublic class ManagedBeanConstructor {\n    private int a;\n\n    protected ManagedBeanConstructor() {\n    }\n\n    public ManagedBeanConstructor(int a) {\n        this.a = a;\n    }\n}\n");
        TextEdit te3 = te(0, 0, 25, 0, "/*******************************************************************************\n * Copyright (c) 2021 IBM Corporation.\n *\n * This program and the accompanying materials are made available under the\n * terms of the Eclipse Public License v. 2.0 which is available at\n * http://www.eclipse.org/legal/epl-2.0.\n *\n * SPDX-License-Identifier: EPL-2.0\n *\n * Contributors:\n *     Hani Damlaj\n *******************************************************************************/\n\npackage io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.Dependent;\n\n@Dependent\npublic class ManagedBeanConstructor {\n    private int a;\n\n    public ManagedBeanConstructor() {\n    }\n\n    public ManagedBeanConstructor(int a) {\n        this.a = a;\n    }\n}\n");
        CodeAction ca2 = ca(uri, Messages.getMessage("AddProtectedConstructor"), d, te2);
        CodeAction ca3 = ca(uri, Messages.getMessage("AddPublicConstructor"), d, te3);
        assertJavaCodeAction(codeActionParams1, utils, ca2, ca3);

    }
}
