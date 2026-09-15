/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

/** Tests for the inconsistent specialization diagnostic: only one bean may specialize a given base bean. */
@RunWith(JUnit4.class)
public class InconsistentSpecializationTest extends BaseJakartaTest {

    private static String msg(String beanName, String baseTypeFqName) {
        return "Inconsistent specialization: bean '" + beanName
                + "' and at least one other bean both specialize the same base type '"
                + baseTypeFqName + "'. Only one bean may specialize a given base bean.";
    }

    /** No diagnostic expected on a base bean that does not carry @Specializes. */
    @Test
    public void noDiagnosticOnBaseBeanWithoutSpecializes() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                        + "/src/main/java/io/openliberty/sample/jakarta/cdi/specializes/BaseBean.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /** No diagnostic expected when only one bean specializes a given base. */
    @Test
    public void noDiagnosticWhenOnlyOneBeanSpecializesABase() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                        + "/src/main/java/io/openliberty/sample/jakarta/cdi/specializes/ValidSpecializer.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /** Diagnostic expected on SpecializerA when SpecializerB also specializes BaseBean. */
    @Test
    public void inconsistentSpecializationOnFirstBean() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                        + "/src/main/java/io/openliberty/sample/jakarta/cdi/specializes/SpecializerA.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d = d(8, 13, 25,
                msg("SpecializerA",
                        "io.openliberty.sample.jakarta.cdi.specializes.BaseBean"),
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInconsistentSpecialization");

        assertJavaDiagnostics(diagnosticsParams, utils, d);
    }

    /** Diagnostic expected on SpecializerB when SpecializerA also specializes BaseBean. */
    @Test
    public void inconsistentSpecializationOnSecondBean() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                        + "/src/main/java/io/openliberty/sample/jakarta/cdi/specializes/SpecializerB.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d = d(8, 13, 25,
                msg("SpecializerB",
                        "io.openliberty.sample.jakarta.cdi.specializes.BaseBean"),
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInconsistentSpecialization");

        assertJavaDiagnostics(diagnosticsParams, utils, d);
    }

    /** Diagnostic expected on TransitiveSpecializerB when it transitively specializes the same base as TransitiveSpecializerA. */
    @Test
    public void inconsistentSpecializationOnTransitiveBean() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                        + "/src/main/java/io/openliberty/sample/jakarta/cdi/specializes/TransitiveSpecializerB.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d = d(8, 13, 35,
                msg("TransitiveSpecializerB",
                        "io.openliberty.sample.jakarta.cdi.specializes.TransitiveBaseBean"),
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInconsistentSpecialization");

        assertJavaDiagnostics(diagnosticsParams, utils, d);
    }
}
