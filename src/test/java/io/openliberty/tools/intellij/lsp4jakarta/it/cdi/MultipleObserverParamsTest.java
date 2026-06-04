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
        Diagnostic twoObservesDiag = d(15, 16, 34,
                "Parameters event1, event2 are annotated with @Observes or @ObservesAsync, but a method cannot contain more than one such parameter.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidMultipleObserverParams");

        // Line 20: invalidObservesAndObservesAsync method has @Observes and @ObservesAsync parameters
        Diagnostic mixedObserversDiag = d(19, 16, 47,
                "Parameters event1, event2 are annotated with @Observes or @ObservesAsync, but a method cannot contain more than one such parameter.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidMultipleObserverParams");

        assertJavaDiagnostics(diagnosticsParams, utils, twoObservesDiag, mixedObserversDiag);

        // Test code actions for twoObservesDiag (invalidTwoObserves)
        JakartaJavaCodeActionParams twoObservesParams = createCodeActionParams(uri, twoObservesDiag);

        // Expected text after removing @Observes from event1
        String removeEvent1Observes = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\npublic class MultipleObserverParams {\n\n    " +
                "public void validSingleObserves(@Observes String event) {\n        System.out.println(\"Event: \" + event);\n    }\n\n    " +
                "public void validSingleObservesAsync(@ObservesAsync String event) {\n        System.out.println(\"Event: \" + event);\n    }\n\n    " +
                "public void invalidTwoObserves(String event1, @Observes String event2) {\n        " +
                "System.out.println(\"Events: \" + event1 + \", \" + event2);\n    }\n\n    " +
                "public void invalidObservesAndObservesAsync(@Observes String event1, @ObservesAsync String event2) {\n        " +
                "System.out.println(\"Events: \" + event1 + \", \" + event2);\n    }\n}\n";

        // Expected text after removing @Observes from event2
        String removeEvent2Observes = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\npublic class MultipleObserverParams {\n\n    " +
                "public void validSingleObserves(@Observes String event) {\n        System.out.println(\"Event: \" + event);\n    }\n\n    " +
                "public void validSingleObservesAsync(@ObservesAsync String event) {\n        System.out.println(\"Event: \" + event);\n    }\n\n    " +
                "public void invalidTwoObserves(@Observes String event1, String event2) {\n        " +
                "System.out.println(\"Events: \" + event1 + \", \" + event2);\n    }\n\n    " +
                "public void invalidObservesAndObservesAsync(@Observes String event1, @ObservesAsync String event2) {\n        " +
                "System.out.println(\"Events: \" + event1 + \", \" + event2);\n    }\n}\n";

        TextEdit editRemoveEvent1 = te(0, 0, 23, 0, removeEvent1Observes);
        TextEdit editRemoveEvent2 = te(0, 0, 23, 0, removeEvent2Observes);
        CodeAction removeEvent1Action = ca(uri, "Remove the @Observes modifier from parameter event1", twoObservesDiag, editRemoveEvent1);
        CodeAction removeEvent2Action = ca(uri, "Remove the @Observes modifier from parameter event2", twoObservesDiag, editRemoveEvent2);

        assertJavaCodeAction(twoObservesParams, utils, removeEvent1Action, removeEvent2Action);

        // Test code actions for mixedObserversDiag (invalidObservesAndObservesAsync)
        JakartaJavaCodeActionParams mixedObserversParams = createCodeActionParams(uri, mixedObserversDiag);

        // Expected text after removing @Observes from event1
        String removeMixedEvent1 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\npublic class MultipleObserverParams {\n\n    " +
                "public void validSingleObserves(@Observes String event) {\n        System.out.println(\"Event: \" + event);\n    }\n\n    " +
                "public void validSingleObservesAsync(@ObservesAsync String event) {\n        System.out.println(\"Event: \" + event);\n    }\n\n    " +
                "public void invalidTwoObserves(@Observes String event1, @Observes String event2) {\n        " +
                "System.out.println(\"Events: \" + event1 + \", \" + event2);\n    }\n\n    " +
                "public void invalidObservesAndObservesAsync(String event1, @ObservesAsync String event2) {\n        " +
                "System.out.println(\"Events: \" + event1 + \", \" + event2);\n    }\n}\n";

        // Expected text after removing @ObservesAsync from event2
        String removeMixedEvent2 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\npublic class MultipleObserverParams {\n\n    " +
                "public void validSingleObserves(@Observes String event) {\n        System.out.println(\"Event: \" + event);\n    }\n\n    " +
                "public void validSingleObservesAsync(@ObservesAsync String event) {\n        System.out.println(\"Event: \" + event);\n    }\n\n    " +
                "public void invalidTwoObserves(@Observes String event1, @Observes String event2) {\n        " +
                "System.out.println(\"Events: \" + event1 + \", \" + event2);\n    }\n\n    " +
                "public void invalidObservesAndObservesAsync(@Observes String event1, String event2) {\n        " +
                "System.out.println(\"Events: \" + event1 + \", \" + event2);\n    }\n}\n";
        TextEdit editRemoveMixedEvent1 = te(0, 0, 23, 0, removeMixedEvent1);
        TextEdit editRemoveMixedEvent2 = te(0, 0, 23, 0, removeMixedEvent2);
        CodeAction removeMixedEvent1Action = ca(uri, "Remove the @Observes modifier from parameter event1", mixedObserversDiag, editRemoveMixedEvent1);
        CodeAction removeMixedEvent2Action = ca(uri, "Remove the @ObservesAsync modifier from parameter event2", mixedObserversDiag, editRemoveMixedEvent2);

        assertJavaCodeAction(mixedObserversParams, utils, removeMixedEvent1Action, removeMixedEvent2Action);
    }
}
