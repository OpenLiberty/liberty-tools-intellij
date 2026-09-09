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

import com.google.gson.JsonArray;
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

/**
 * Tests that CDI scope inheritance is validated correctly for @Singleton and @Stateless
 * session beans. CDI scope annotations are @Inherited, so when a session bean declares
 * no scope of its own, the nearest ancestor's scope becomes its effective scope. If that
 * inherited scope is not valid for the bean type, a diagnostic must be raised.
 *
 * Test resources are in: src/main/java/io/openliberty/sample/jakarta/cdi/sessionbean/inheritance/
 */
@RunWith(JUnit4.class)
public class SessionBeanScopeInheritanceTest extends BaseJakartaTest {

    private static final String CDI_INHERITANCE_PATH =
            "/src/main/java/io/openliberty/sample/jakarta/cdi/sessionbean/inheritance/";

    private String getUri(Module module, String fileName) {
        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + CDI_INHERITANCE_PATH + fileName);
        return VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();
    }

    // -----------------------------------------------------------------------
    // @Singleton — inherits invalid @RequestScoped from direct superclass
    // File: SingletonInheritsRequestScope.java
    // @Singleton on line 8, class decl on line 9
    // "public class SingletonInheritsRequestScope" -> col 13..42
    // -----------------------------------------------------------------------
    @Test
    public void testSingletonInheritsRequestScope() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
        String uri = getUri(module, "SingletonInheritsRequestScope.java");

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        JsonArray dataRequestScoped = new JsonArray();
        dataRequestScoped.add("jakarta.enterprise.context.RequestScoped");
        Diagnostic inheritedRequestScopeOnSingleton = d(8, 13, 42,
                "Invalid scope @RequestScoped present in the class ScopeInheritanceParentWithRequestScope. A singleton session bean belongs to the @ApplicationScoped or @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidSingletonSessionBeanScope", dataRequestScoped);

        assertJavaDiagnostics(diagnosticsParams, utils, inheritedRequestScopeOnSingleton);
    }

    // -----------------------------------------------------------------------
    // @Singleton — inherits valid @ApplicationScoped: no diagnostic
    // -----------------------------------------------------------------------
    @Test
    public void testSingletonInheritsApplicationScopeIsValid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(getUri(module, "SingletonInheritsApplicationScope.java")));
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    // -----------------------------------------------------------------------
    // @Singleton — inherits valid @Dependent: no diagnostic
    // -----------------------------------------------------------------------
    @Test
    public void testSingletonInheritsDependentScopeIsValid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(getUri(module, "SingletonInheritsDependentScope.java")));
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    // -----------------------------------------------------------------------
    // @Singleton — inherits invalid @SessionScoped from direct superclass
    // File: SingletonInheritsSessionScope.java
    // @Singleton on line 8, class decl on line 9
    // "public class SingletonInheritsSessionScope" -> col 13..42
    // -----------------------------------------------------------------------
    @Test
    public void testSingletonInheritsSessionScope() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
        String uri = getUri(module, "SingletonInheritsSessionScope.java");

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        JsonArray dataSessionScoped = new JsonArray();
        dataSessionScoped.add("jakarta.enterprise.context.SessionScoped");
        Diagnostic inheritedSessionScopeOnSingleton = d(8, 13, 42,
                "Invalid scope @SessionScoped present in the class ScopeInheritanceParentWithSessionScope. A singleton session bean belongs to the @ApplicationScoped or @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidSingletonSessionBeanScope", dataSessionScoped);

        assertJavaDiagnostics(diagnosticsParams, utils, inheritedSessionScopeOnSingleton);
    }

    // -----------------------------------------------------------------------
    // @Singleton — inherits @RequestScoped transitively (grandparent -> intermediate -> child)
    // File: SingletonInheritsRequestScopeTransitively.java
    // @Singleton on line 9, class decl on line 10
    // "public class SingletonInheritsRequestScopeTransitively" -> col 13..54
    // -----------------------------------------------------------------------
    @Test
    public void testSingletonInheritsRequestScopeTransitively() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
        String uri = getUri(module, "SingletonInheritsRequestScopeTransitively.java");

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        JsonArray dataRequestScoped = new JsonArray();
        dataRequestScoped.add("jakarta.enterprise.context.RequestScoped");
        Diagnostic inheritedRequestScopeTransitiveOnSingleton = d(9, 13, 54,
                "Invalid scope @RequestScoped present in the class ScopeInheritanceParentWithRequestScope. A singleton session bean belongs to the @ApplicationScoped or @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidSingletonSessionBeanScope", dataRequestScoped);

        assertJavaDiagnostics(diagnosticsParams, utils, inheritedRequestScopeTransitiveOnSingleton);
    }

    // -----------------------------------------------------------------------
    // @Singleton — own @ApplicationScoped overrides inherited @RequestScoped: no diagnostic
    // -----------------------------------------------------------------------
    @Test
    public void testSingletonWithOwnScopeOverridesInheritedScopeIsValid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(getUri(module, "SingletonWithOwnScopeOverridesInheritedScope.java")));
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    // -----------------------------------------------------------------------
    // @Stateless — inherits invalid @RequestScoped from direct superclass
    // File: StatelessInheritsRequestScope.java
    // @Stateless on line 8, class decl on line 9
    // "public class StatelessInheritsRequestScope" -> col 13..42
    // -----------------------------------------------------------------------
    @Test
    public void testStatelessInheritsRequestScope() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
        String uri = getUri(module, "StatelessInheritsRequestScope.java");

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        JsonArray dataRequestScoped = new JsonArray();
        dataRequestScoped.add("jakarta.enterprise.context.RequestScoped");
        Diagnostic inheritedRequestScopeOnStateless = d(8, 13, 42,
                "Invalid scope @RequestScoped present in the class ScopeInheritanceParentWithRequestScope. A stateless session bean belongs to the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidStatelessSessionBeanScope", dataRequestScoped);

        assertJavaDiagnostics(diagnosticsParams, utils, inheritedRequestScopeOnStateless);
    }

    // -----------------------------------------------------------------------
    // @Stateless — inherits valid @Dependent: no diagnostic
    // -----------------------------------------------------------------------
    @Test
    public void testStatelessInheritsDependentScopeIsValid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(getUri(module, "StatelessInheritsDependentScope.java")));
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    // -----------------------------------------------------------------------
    // @Stateless — inherits invalid @ApplicationScoped from direct superclass
    // File: StatelessInheritsApplicationScope.java
    // @Stateless on line 8, class decl on line 9
    // "public class StatelessInheritsApplicationScope" -> col 13..46
    // -----------------------------------------------------------------------
    @Test
    public void testStatelessInheritsApplicationScope() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
        String uri = getUri(module, "StatelessInheritsApplicationScope.java");

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        JsonArray dataApplicationScoped = new JsonArray();
        dataApplicationScoped.add("jakarta.enterprise.context.ApplicationScoped");
        Diagnostic inheritedApplicationScopeOnStateless = d(8, 13, 46,
                "Invalid scope @ApplicationScoped present in the class ScopeInheritanceParentWithApplicationScope. A stateless session bean belongs to the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidStatelessSessionBeanScope", dataApplicationScoped);

        assertJavaDiagnostics(diagnosticsParams, utils, inheritedApplicationScopeOnStateless);
    }

    // -----------------------------------------------------------------------
    // @Stateless — inherits @RequestScoped transitively (grandparent -> intermediate -> child)
    // File: StatelessInheritsRequestScopeTransitively.java
    // @Stateless on line 9, class decl on line 10
    // "public class StatelessInheritsRequestScopeTransitively" -> col 13..54
    // -----------------------------------------------------------------------
    @Test
    public void testStatelessInheritsRequestScopeTransitively() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
        String uri = getUri(module, "StatelessInheritsRequestScopeTransitively.java");

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        JsonArray dataRequestScoped = new JsonArray();
        dataRequestScoped.add("jakarta.enterprise.context.RequestScoped");
        Diagnostic inheritedRequestScopeTransitiveOnStateless = d(9, 13, 54,
                "Invalid scope @RequestScoped present in the class ScopeInheritanceParentWithRequestScope. A stateless session bean belongs to the @Dependent scope. Any other scope is invalid.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidStatelessSessionBeanScope", dataRequestScoped);

        assertJavaDiagnostics(diagnosticsParams, utils, inheritedRequestScopeTransitiveOnStateless);
    }

    // -----------------------------------------------------------------------
    // @Stateless — own @Dependent overrides inherited @RequestScoped: no diagnostic
    // -----------------------------------------------------------------------
    @Test
    public void testStatelessWithOwnScopeOverridesInheritedScopeIsValid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());
        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(getUri(module, "StatelessWithOwnScopeOverridesInheritedScope.java")));
        assertJavaDiagnostics(diagnosticsParams, utils);
    }
}
