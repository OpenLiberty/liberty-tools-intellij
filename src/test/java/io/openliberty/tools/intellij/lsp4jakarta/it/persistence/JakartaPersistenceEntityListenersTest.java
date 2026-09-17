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
 * Integration tests for {@code @EntityListeners} diagnostics.
 *
 * <p>Validates Jakarta Persistence 3.0 §3.5.1:
 * An entity listener must be a concrete, non-inner, non-anonymous, non-local class
 * with a public no-argument constructor (explicit or implicit).
 */
@RunWith(JUnit4.class)
public class JakartaPersistenceEntityListenersTest extends BaseJakartaTest {

    // -----------------------------------------------------------------------
    // Valid cases — no diagnostics expected
    // -----------------------------------------------------------------------

    /**
     * Both listeners have valid public no-arg constructors (one explicit, one implicit).
     * No diagnostic should be reported.
     */
    @Test
    public void entityListenersValidConstructors() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                        + "/src/main/java/io/openliberty/sample/jakarta/persistence/entitylisteners/EntityListenersValidConstructor.java");
        assertNotNull("Test resource file not found", javaFile);
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /**
     * Both static nested classes are valid entity listeners.
     * No diagnostic should be reported.
     */
    @Test
    public void entityListenersStaticNestedValid() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                        + "/src/main/java/io/openliberty/sample/jakarta/persistence/entitylisteners/EntityListenersStaticNested.java");
        assertNotNull("Test resource file not found", javaFile);
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    // -----------------------------------------------------------------------
    // Invalid constructor cases
    // -----------------------------------------------------------------------

    /**
     * Two listeners: one with only a protected no-arg constructor and one with only a
     * parameterized constructor. Both violate §3.5.1.
     * Expects a single {@code InvalidConstructorInEntityListener} diagnostic covering
     * the full {@code @EntityListeners} annotation text.
     * <p>
     * The annotation text {@code @EntityListeners({ ProtectedConstructorListener.class, ParameterizedConstructorListener.class })}
     * is 96 characters long, starting at column 0 on line 8 (0-based).
     */
    @Test
    public void entityListenersInvalidConstructors() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                        + "/src/main/java/io/openliberty/sample/jakarta/persistence/entitylisteners/EntityListenersInvalidConstructor.java");
        assertNotNull("Test resource file not found", javaFile);
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 8 (0-based), col 0–96: the full @EntityListeners annotation
        Diagnostic invalidConstructorDiagnostic = d(8, 0, 96,
                "The entity listener class(es) ProtectedConstructorListener, ParameterizedConstructorListener must declare a public no-argument constructor.",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidConstructorInEntityListener");

        assertJavaDiagnostics(diagnosticsParams, utils, invalidConstructorDiagnostic);
    }

    // -----------------------------------------------------------------------
    // Non-instantiable type cases
    // -----------------------------------------------------------------------

    /**
     * Two non-static inner class listeners are non-instantiable.
     * Expects a single {@code InvalidEntityListenerType} diagnostic.
     * <p>
     * Annotation text length: 142 characters on line 8 (0-based).
     */
    @Test
    public void entityListenersNonStaticInnerClasses() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                        + "/src/main/java/io/openliberty/sample/jakarta/persistence/entitylisteners/EntityListenersNonStaticInner.java");
        assertNotNull("Test resource file not found", javaFile);
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 8 (0-based), col 0–142: the full @EntityListeners annotation
        Diagnostic nonInstantiableDiagnostic = d(8, 0, 142,
                "The entity listener class(es) NonStaticInnerImplicitListener, NonStaticInnerExplicitListener must not be abstract, an interface, or a non-static inner class.",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidEntityListenerType");

        assertJavaDiagnostics(diagnosticsParams, utils, nonInstantiableDiagnostic);
    }

    /**
     * One listener is abstract (non-instantiable) and one has a package-private no-arg
     * constructor (invalid). Two diagnostics — one for each violation — should be reported.
     * <p>
     * Annotation text {@code @EntityListeners({ AbstractListener.class, PackagePrivateConstructorListener.class })}
     * is 85 characters long on line 8 (0-based).
     */
    @Test
    public void entityListenersAbstractAndPackagePrivate() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module)
                        + "/src/main/java/io/openliberty/sample/jakarta/persistence/entitylisteners/EntityListenersAbstractAndPackagePrivate.java");
        assertNotNull("Test resource file not found", javaFile);
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 8 (0-based), col 0–85: the full @EntityListeners annotation
        Diagnostic nonInstantiableDiagnostic = d(8, 0, 85,
                "The entity listener class(es) AbstractListener must not be abstract, an interface, or a non-static inner class.",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidEntityListenerType");
        Diagnostic invalidConstructorDiagnostic = d(8, 0, 85,
                "The entity listener class(es) PackagePrivateConstructorListener must declare a public no-argument constructor.",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidConstructorInEntityListener");

        assertJavaDiagnostics(diagnosticsParams, utils, nonInstantiableDiagnostic, invalidConstructorDiagnostic);
    }
}
