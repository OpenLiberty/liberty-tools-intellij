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
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
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
                "Parameters event1, event2 are annotated with @Observes or @ObservesAsync, but a method cannot contain more than one such parameter.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidMultipleObserverParams");

        // Line 20: invalidObservesAndObservesAsync method has @Observes and @ObservesAsync parameters
        Diagnostic d2 = d(19, 16, 47,
                "Parameters event1, event2 are annotated with @Observes or @ObservesAsync, but a method cannot contain more than one such parameter.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidMultipleObserverParams");

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2);
    }
}
