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

package io.openliberty.tools.intellij.lsp4jakarta.it.ejb;

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
public class SessionBeanModifierTest extends BaseJakartaTest {

    private static final String EJB_SESSIONBEAN_PATH =
            "/src/main/java/io/openliberty/sample/jakarta/ejb/sessionbean/";

    private String getUri(Module module, String fileName) {
        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + EJB_SESSIONBEAN_PATH + fileName);
        return VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();
    }

    /**
     * NotPublicStatelessBean.java — line 6 (0-based):
     *   "class NotPublicStatelessBean {"
     *   → name "NotPublicStatelessBean" col 6..28 → InvalidModifierNotPublic
     */
    @Test
    public void testNotPublicStatelessBean() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(getUri(module, "NotPublicStatelessBean.java")));

        Diagnostic notPublic = d(6, 6, 28,
                "A session bean class must be declared public.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidModifierNotPublic");

        assertJavaDiagnostics(diagnosticsParams, utils, notPublic);
    }

    /**
     * FinalStatelessBean.java — line 6 (0-based):
     *   "public final class FinalStatelessBean {"
     *   → name "FinalStatelessBean" col 19..37 → InvalidModifierFinal
     */
    @Test
    public void testFinalStatelessBean() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(getUri(module, "FinalStatelessBean.java")));

        Diagnostic isFinal = d(6, 19, 37,
                "A session bean class must not be declared final.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidModifierFinal");

        assertJavaDiagnostics(diagnosticsParams, utils, isFinal);
    }

    /**
     * AbstractStatefulBean.java — line 6 (0-based):
     *   "public abstract class AbstractStatefulBean {"
     *   → name "AbstractStatefulBean" col 22..42 → InvalidModifierAbstract
     */
    @Test
    public void testAbstractStatefulBean() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(getUri(module, "AbstractStatefulBean.java")));

        Diagnostic isAbstract = d(6, 22, 42,
                "A session bean class must not be declared abstract.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidModifierAbstract");

        assertJavaDiagnostics(diagnosticsParams, utils, isAbstract);
    }

    /**
     * FinalNotPublicSingletonBean.java — line 6 (0-based):
     *   "public final class FinalNotPublicSingletonBean {"
     *   → name "FinalNotPublicSingletonBean" col 19..46 → InvalidModifierFinal
     */
    @Test
    public void testFinalNotPublicSingletonBean() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(getUri(module, "FinalNotPublicSingletonBean.java")));

        Diagnostic isFinal = d(6, 19, 46,
                "A session bean class must not be declared final.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidModifierFinal");

        assertJavaDiagnostics(diagnosticsParams, utils, isFinal);
    }

    /**
     * NestedSessionBeanWrapper.java — line 12 (0-based):
     *   "    public class NestedStatefulBean {"
     *   → name "NestedStatefulBean" col 17..35 → InvalidNotTopLevelClass
     */
    @Test
    public void testNestedSessionBeanNotTopLevel() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(getUri(module, "NestedSessionBeanWrapper.java")));

        Diagnostic notTopLevel = d(12, 17, 35,
                "A session bean class must be a top-level class.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidNotTopLevelClass");

        assertJavaDiagnostics(diagnosticsParams, utils, notTopLevel);
    }

    /**
     * Valid session beans — no diagnostics expected for any of the three files.
     */
    @Test
    public void testValidSessionBeans() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        for (String fileName : new String[]{
                "ValidStatelessBeanExplicit.java",
                "ValidStatefulBeanNoConstructor.java",
                "ValidSingletonBeanExplicit.java"}) {
            JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
            diagnosticsParams.setUris(Arrays.asList(getUri(module, fileName)));
            assertJavaDiagnostics(diagnosticsParams, utils);
        }
    }
}
