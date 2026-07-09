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

    /**
     * InvalidSessionBeanModifiers.java — tests for non-public, final, abstract violations.
     *
     * Line 14 (0-based): "class NotPublicStatelessBean {"
     *   → name "NotPublicStatelessBean" col 6..28 → InvalidModifierNotPublic
     *
     * Line 21 (0-based): "public final class FinalStatelessBean {"
     *   → name "FinalStatelessBean" col 19..37 → InvalidModifierFinal
     *
     * Line 28 (0-based): "abstract class AbstractStatefulBean {"
     *   → name "AbstractStatefulBean" col 15..35 → InvalidModifierNotPublic + InvalidModifierAbstract
     *
     * Line 35 (0-based): "final class FinalNotPublicSingletonBean {"
     *   → name "FinalNotPublicSingletonBean" col 12..39 → InvalidModifierNotPublic + InvalidModifierFinal
     */
    @Test
    public void testInvalidSessionBeanModifiers() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/InvalidSessionBeanModifiers.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // NotPublicStatelessBean — not public
        Diagnostic notPublic = d(14, 6, 28,
                "A session bean class must be declared public.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidModifierNotPublic");

        // FinalStatelessBean — final
        Diagnostic isFinal = d(21, 19, 37,
                "A session bean class must not be declared final.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidModifierFinal");

        // AbstractStatefulBean — not public and abstract
        Diagnostic abstractNotPublic = d(28, 15, 35,
                "A session bean class must be declared public.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidModifierNotPublic");
        Diagnostic isAbstract = d(28, 15, 35,
                "A session bean class must not be declared abstract.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidModifierAbstract");

        // FinalNotPublicSingletonBean — not public and final
        Diagnostic finalNotPublic = d(35, 12, 39,
                "A session bean class must be declared public.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidModifierNotPublic");
        Diagnostic finalIsFinal = d(35, 12, 39,
                "A session bean class must not be declared final.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidModifierFinal");

        assertJavaDiagnostics(diagnosticsParams, utils,
                notPublic, isFinal, abstractNotPublic, isAbstract, finalNotPublic, finalIsFinal);
    }

    /**
     * NestedSessionBeanWrapper.java — tests that a nested session bean triggers
     * InvalidNotTopLevelClass.
     *
     * Line 12 (0-based): "    public class NestedStatefulBean {"
     *   → name "NestedStatefulBean" col 18..36 → InvalidNotTopLevelClass
     */
    @Test
    public void testNestedSessionBeanNotTopLevel() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/NestedSessionBeanWrapper.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic notTopLevel = d(12, 17, 35,
                "A session bean class must be a top-level class.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidNotTopLevelClass");

        assertJavaDiagnostics(diagnosticsParams, utils, notTopLevel);
    }

    /**
     * ValidSessionBeans.java — no diagnostics expected for any of the three
     * valid session bean classes.
     */
    @Test
    public void testValidSessionBeans() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/ValidSessionBeans.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }
}
