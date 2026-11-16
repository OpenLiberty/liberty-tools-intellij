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
package io.openliberty.tools.intellij.lsp4jakarta.it.persistence;

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
 * Integration tests for {@code @PersistenceContext} injection rules:
 * <ul>
 *   <li>§7.6 — {@code @PersistenceContext} must only appear in a container-managed component
 *       (CDI bean, EJB session bean, or Servlet component).</li>
 *   <li>§7.6.3 — {@code PersistenceContextType.EXTENDED} can only be used in a {@code @Stateful}
 *       EJB session bean.</li>
 * </ul>
 */
@RunWith(JUnit4.class)
public class PersistenceContextDiagnosticsTest extends BaseJakartaTest {

    // -------------------------------------------------------------------------
    // §7.6 — @PersistenceContext must only appear in a managed component
    // -------------------------------------------------------------------------

    /**
     * @PersistenceContext in a plain Java class (no managed component annotation)
     * must produce a diagnostic.
     * Spec §7.6: https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0#a11791
     */
    @Test
    public void persistenceContextInPlainClass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/context/PersistenceContextInPlainClass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 11 (0-based), col 26–28 → field name "em"
        Diagnostic notInManagedComponentDiag = d(11, 26, 28,
                "@PersistenceContext can only be used in a container-managed component such as a CDI bean, EJB, or Servlet.",
                DiagnosticSeverity.Error, "jakarta-persistence", "PersistenceContextNotInManagedComponent");

        assertJavaDiagnostics(diagnosticsParams, utils, notInManagedComponentDiag);
    }

    /** @PersistenceContext in a CDI @ApplicationScoped bean — no diagnostic expected. */
    @Test
    public void persistenceContextInCdiBeanValid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/context/PersistenceContextInCdiBeanValid.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /** @PersistenceContext in a CDI @Dependent bean — no diagnostic expected. */
    @Test
    public void persistenceContextInDependentBeanValid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/context/PersistenceContextInDependentBeanValid.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /** @PersistenceContext in a @Stateless EJB — no diagnostic expected. */
    @Test
    public void persistenceContextInEjbValid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/context/PersistenceContextInEjbValid.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /** @PersistenceContext in a @WebServlet servlet — no diagnostic expected. */
    @Test
    public void persistenceContextInServletValid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/context/PersistenceContextInServletValid.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /** @PersistenceContext in a @WebFilter component — no diagnostic expected. */
    @Test
    public void persistenceContextInWebFilterValid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/context/PersistenceContextInWebFilterValid.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /** @PersistenceContext in a @WebListener component — no diagnostic expected. */
    @Test
    public void persistenceContextInWebListenerValid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/context/PersistenceContextInWebListenerValid.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /**
     * Plain class with two @PersistenceContext fields — each field must independently
     * produce a diagnostic.
     * Spec §7.6: https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0#a11791
     */
    @Test
    public void persistenceContextMultipleFieldsInPlainClassInvalid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/context/PersistenceContextMultipleFieldsInPlainClass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // "emA" at 0-based line 11, col 26-29
        Diagnostic emANotInManagedComponentDiag = d(11, 26, 29,
                "@PersistenceContext can only be used in a container-managed component such as a CDI bean, EJB, or Servlet.",
                DiagnosticSeverity.Error, "jakarta-persistence", "PersistenceContextNotInManagedComponent");

        // "emB" at 0-based line 14, col 26-29
        Diagnostic emBNotInManagedComponentDiag = d(14, 26, 29,
                "@PersistenceContext can only be used in a container-managed component such as a CDI bean, EJB, or Servlet.",
                DiagnosticSeverity.Error, "jakarta-persistence", "PersistenceContextNotInManagedComponent");

        assertJavaDiagnostics(diagnosticsParams, utils, emANotInManagedComponentDiag, emBNotInManagedComponentDiag);
    }

    /**
     * @PersistenceContext in an @Entity class — invalid; @Entity is NOT a managed injection
     * component. A diagnostic must be produced.
     * Spec §7.6: https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0#a11791
     */
    @Test
    public void persistenceContextInEntityClassInvalid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/context/PersistenceContextInEntityClass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // field "em" at 0-based line 17, col 26-28
        Diagnostic notInManagedComponentDiag = d(17, 26, 28,
                "@PersistenceContext can only be used in a container-managed component such as a CDI bean, EJB, or Servlet.",
                DiagnosticSeverity.Error, "jakarta-persistence", "PersistenceContextNotInManagedComponent");

        assertJavaDiagnostics(diagnosticsParams, utils, notInManagedComponentDiag);
    }

    /**
     * {@code @PersistenceContext} at the <em>type</em> level on a plain (unmanaged) Java class
     * must produce a diagnostic.
     * Spec §7.6: https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0#a11791
     */
    @Test
    public void persistenceContextOnTypeInPlainClassInvalid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/context/PersistenceContextOnTypeInPlainClass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Class name "PersistenceContextOnTypeInPlainClass" at 0-based line 8, cols 13–49.
        Diagnostic notInManagedComponentDiag = d(8, 13, 49,
                "@PersistenceContext can only be used in a container-managed component such as a CDI bean, EJB, or Servlet.",
                DiagnosticSeverity.Error, "jakarta-persistence", "PersistenceContextNotInManagedComponent");

        assertJavaDiagnostics(diagnosticsParams, utils, notInManagedComponentDiag);
    }

    /**
     * {@code @PersistenceContext} at the <em>type</em> level on a CDI {@code @RequestScoped} bean
     * must NOT produce a diagnostic.
     * Spec §7.6: https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0#a11791
     */
    @Test
    public void persistenceContextOnTypeInCdiBeanValid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/context/PersistenceContextOnTypeInCdiBeanValid.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /**
     * {@code @PersistenceContext} at the <em>method</em> level on a plain (unmanaged) Java class
     * must produce a diagnostic.
     * Spec §7.6: https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0#a11791
     */
    @Test
    public void persistenceContextOnMethodInPlainClassInvalid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/context/PersistenceContextOnMethodInPlainClass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Method name "setEntityManager" at 0-based line 13, cols 16–32.
        Diagnostic notInManagedComponentDiag = d(13, 16, 32,
                "@PersistenceContext can only be used in a container-managed component such as a CDI bean, EJB, or Servlet.",
                DiagnosticSeverity.Error, "jakarta-persistence", "PersistenceContextNotInManagedComponent");

        assertJavaDiagnostics(diagnosticsParams, utils, notInManagedComponentDiag);
    }

    /**
     * {@code @PersistenceContext} at the <em>method</em> level on an EJB {@code @Stateless} session
     * bean must NOT produce a diagnostic.
     * Spec §7.6: https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0#a11791
     */
    @Test
    public void persistenceContextOnMethodInStatelessValid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/context/PersistenceContextOnMethodInStatelessValid.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    // -------------------------------------------------------------------------
    // §7.6.3 — PersistenceContextType.EXTENDED only valid in @Stateful EJB
    // -------------------------------------------------------------------------

    /**
     * @PersistenceContext(type=EXTENDED) in a @Stateful EJB — no diagnostic expected.
     * Spec §7.6.3: https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0#a11810
     */
    @Test
    public void persistenceContextExtendedInStatefulValid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/context/PersistenceContextExtendedInStateful.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /**
     * @PersistenceContext(type=EXTENDED) in a @Stateless EJB must produce a diagnostic.
     * Spec §7.6.3: EXTENDED context can only be initiated within a @Stateful session bean.
     */
    @Test
    public void persistenceContextExtendedInStatelessInvalid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/context/PersistenceContextExtendedInStateless.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 15 (0-based), col 26–28 → field name "em"
        Diagnostic extendedInStatelessDiag = d(15, 26, 28,
                "PersistenceContextType.EXTENDED can only be used in a @Stateful EJB session bean.",
                DiagnosticSeverity.Error, "jakarta-persistence", "ExtendedPersistenceContextInNonStatefulBean");

        assertJavaDiagnostics(diagnosticsParams, utils, extendedInStatelessDiag);
    }

    /**
     * @PersistenceContext(type=EXTENDED) in a @Singleton EJB — invalid per §7.6.3.
     * EXTENDED context can only be initiated within a @Stateful session bean.
     */
    @Test
    public void persistenceContextExtendedInSingletonInvalid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/context/PersistenceContextExtendedInSingleton.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // "em" at 0-based line 14, col 26-28
        Diagnostic extendedInSingletonDiag = d(14, 26, 28,
                "PersistenceContextType.EXTENDED can only be used in a @Stateful EJB session bean.",
                DiagnosticSeverity.Error, "jakarta-persistence", "ExtendedPersistenceContextInNonStatefulBean");

        assertJavaDiagnostics(diagnosticsParams, utils, extendedInSingletonDiag);
    }

    /**
     * @PersistenceContext(type=EXTENDED) in a CDI @ApplicationScoped bean — invalid per §7.6.3.
     * EXTENDED is an EJB-only concept; CDI beans cannot use it.
     */
    @Test
    public void persistenceContextExtendedInCdiBeanInvalid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/context/PersistenceContextExtendedInCdiBean.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // "em" at 0-based line 14, col 26-28
        Diagnostic extendedInCdiBeanDiag = d(14, 26, 28,
                "PersistenceContextType.EXTENDED can only be used in a @Stateful EJB session bean.",
                DiagnosticSeverity.Error, "jakarta-persistence", "ExtendedPersistenceContextInNonStatefulBean");

        assertJavaDiagnostics(diagnosticsParams, utils, extendedInCdiBeanDiag);
    }

    /** @Stateful EJB with default (TRANSACTION) type — no diagnostic expected. */
    @Test
    public void persistenceContextTransactionInStatefulValid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/context/PersistenceContextTransactionInStateful.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /** @Singleton EJB with default (TRANSACTION) type — no diagnostic expected. */
    @Test
    public void persistenceContextInSingletonValid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/context/PersistenceContextInSingletonValid.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }
}
