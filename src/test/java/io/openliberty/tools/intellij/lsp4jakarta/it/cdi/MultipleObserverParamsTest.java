/*******************************************************************************
* Copyright (c) 2026 IBM Corporation.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     IBM Corporation
*******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.it.cdi;

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
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;

import static io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert.*;

@RunWith(JUnit4.class)
public class MultipleObserverParamsTest extends BaseJakartaTest {

    @Test
    public void multipleObserverParams() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/MultipleObserverParams.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // test expected diagnostics
        // Line 16: invalidTwoObserves method has two @Observes parameters
        Diagnostic d1 = d(15, 16, 34,
                "A method cannot have more than one parameter annotated with @Observes or @ObservesAsync.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidMultipleObserverParams");

        // Line 20: invalidObservesAndObservesAsync method has @Observes and @ObservesAsync parameters
        Diagnostic d2 = d(19, 16, 47,
                "A method cannot have more than one parameter annotated with @Observes or @ObservesAsync.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidMultipleObserverParams");

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2);

        // Test code actions for d1 (invalidTwoObserves)
        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d1);

        // Expected text after removing @Observes from event1
        String newText1 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\npublic class MultipleObserverParams {\n\n    " +
                "public void validSingleObserves(@Observes String event) {\n        System.out.println(\"Event: \" + event);\n    }\n\n    " +
                "public void validSingleObservesAsync(@ObservesAsync String event) {\n        System.out.println(\"Event: \" + event);\n    }\n\n    " +
                "public void invalidTwoObserves(String event1, @Observes String event2) {\n        " +
                "System.out.println(\"Events: \" + event1 + \", \" + event2);\n    }\n\n    " +
                "public void invalidObservesAndObservesAsync(@Observes String event1, @ObservesAsync String event2) {\n        " +
                "System.out.println(\"Events: \" + event1 + \", \" + event2);\n    }\n}\n";

        // Expected text after removing @Observes from event2
        String newText2 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\npublic class MultipleObserverParams {\n\n    " +
                "public void validSingleObserves(@Observes String event) {\n        System.out.println(\"Event: \" + event);\n    }\n\n    " +
                "public void validSingleObservesAsync(@ObservesAsync String event) {\n        System.out.println(\"Event: \" + event);\n    }\n\n    " +
                "public void invalidTwoObserves(@Observes String event1, String event2) {\n        " +
                "System.out.println(\"Events: \" + event1 + \", \" + event2);\n    }\n\n    " +
                "public void invalidObservesAndObservesAsync(@Observes String event1, @ObservesAsync String event2) {\n        " +
                "System.out.println(\"Events: \" + event1 + \", \" + event2);\n    }\n}\n";

        TextEdit te1 = te(0, 0, 23, 0, newText1);
        TextEdit te2 = te(0, 0, 23, 0, newText2);
        CodeAction ca1 = ca(uri, "Remove the @Observes modifier from parameter event1", d1, te1);
        CodeAction ca2 = ca(uri, "Remove the @Observes modifier from parameter event2", d1, te2);

        assertJavaCodeAction(codeActionParams1, utils, ca1, ca2);

        // Test code actions for d2 (invalidObservesAndObservesAsync)
        JakartaJavaCodeActionParams codeActionParams2 = createCodeActionParams(uri, d2);

        // Expected text after removing @Observes from event1
        String newText3 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\npublic class MultipleObserverParams {\n\n    " +
                "public void validSingleObserves(@Observes String event) {\n        System.out.println(\"Event: \" + event);\n    }\n\n    " +
                "public void validSingleObservesAsync(@ObservesAsync String event) {\n        System.out.println(\"Event: \" + event);\n    }\n\n    " +
                "public void invalidTwoObserves(@Observes String event1, @Observes String event2) {\n        " +
                "System.out.println(\"Events: \" + event1 + \", \" + event2);\n    }\n\n    " +
                "public void invalidObservesAndObservesAsync(String event1, @ObservesAsync String event2) {\n        " +
                "System.out.println(\"Events: \" + event1 + \", \" + event2);\n    }\n}\n";

        // Expected text after removing @ObservesAsync from event2
        String newText4 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\npublic class MultipleObserverParams {\n\n    " +
                "public void validSingleObserves(@Observes String event) {\n        System.out.println(\"Event: \" + event);\n    }\n\n    " +
                "public void validSingleObservesAsync(@ObservesAsync String event) {\n        System.out.println(\"Event: \" + event);\n    }\n\n    " +
                "public void invalidTwoObserves(@Observes String event1, @Observes String event2) {\n        " +
                "System.out.println(\"Events: \" + event1 + \", \" + event2);\n    }\n\n    " +
                "public void invalidObservesAndObservesAsync(@Observes String event1, String event2) {\n        " +
                "System.out.println(\"Events: \" + event1 + \", \" + event2);\n    }\n}\n";
        TextEdit te3 = te(0, 0, 23, 0, newText3);
        TextEdit te4 = te(0, 0, 23, 0, newText4);
        CodeAction ca3 = ca(uri, "Remove the @Observes modifier from parameter event1", d2, te3);
        CodeAction ca4 = ca(uri, "Remove the @ObservesAsync modifier from parameter event2", d2, te4);

        assertJavaCodeAction(codeActionParams2, utils, ca3, ca4);
    }
}
