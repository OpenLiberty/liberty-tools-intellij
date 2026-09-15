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
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
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

/**
 * Tests for CDI ObserverMethod notify-method validation.
 *
 * A class that directly implements ObserverMethod must override at least one of
 * notify(T) or notify(EventContext&lt;T&gt;).
 */
@RunWith(JUnit4.class)
public class ObserverMethodNotifyTest extends BaseJakartaTest {

    /**
     * Verifies that a class implementing ObserverMethod without overriding any
     * notify method triggers {@code InvalidObserverMethodWithoutNotify}.
     */
    @Test
    public void testObserverMethodWithoutNotify() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/AuditObserver.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // AuditObserver: "public class AuditObserver" is on line 20 (1-based) = 19 (0-based).
        // "AuditObserver" starts at col 13 and ends at col 26.
        Diagnostic missingNotifyDiagnostic = d(19, 13, 26,
                "The class 'AuditObserver' implements ObserverMethod but does not override notify(T) or notify(EventContext<T>). " +
                        "At least one of these methods must be overridden for the container to invoke the observer.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidObserverMethodWithoutNotify");

        assertJavaDiagnostics(diagnosticsParams, utils, missingNotifyDiagnostic);

        // Code actions
        JakartaJavaCodeActionParams missingNotifyCodeActionParams = createCodeActionParams(uri, missingNotifyDiagnostic);

        String notifyEventText =
                "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import java.lang.annotation.Annotation;\n" +
                "import java.lang.reflect.Type;\n" +
                "import java.util.Collections;\n" +
                "import java.util.Set;\n\n" +
                "import jakarta.enterprise.event.Reception;\n" +
                "import jakarta.enterprise.event.TransactionPhase;\n" +
                "import jakarta.enterprise.inject.spi.EventContext;\n" +
                "import jakarta.enterprise.inject.spi.ObserverMethod;\n\n" +
                "/**\n" +
                " * Invalid CDI definition: AuditObserver implements ObserverMethod but overrides\n" +
                " * neither notify(T) nor notify(EventContext&lt;T&gt;).\n" +
                " *\n" +
                " * <p>The container will detect this as a definition error at deployment time because\n" +
                " * neither notify overload is provided, so the observer logic cannot be invoked.\n" +
                " */\n" +
                "public class AuditObserver implements ObserverMethod<AuditEvent> {\n\n" +
                "    // Invalid: does not override notify(T) or notify(EventContext<T>)\n\n" +
                "    @Override\n" +
                "    public Class<?> getBeanClass() {\n" +
                "        return AuditObserver.class;\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public Type getObservedType() {\n" +
                "        return AuditEvent.class;\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public Set<Annotation> getObservedQualifiers() {\n" +
                "        return Collections.emptySet();\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public Reception getReception() {\n" +
                "        return Reception.ALWAYS;\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public TransactionPhase getTransactionPhase() {\n" +
                "        return TransactionPhase.IN_PROGRESS;\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public void notify(AuditEvent event) {\n" +
                "    }\n" +
                "}\n";

        TextEdit notifyEventEdit = te(0, 0, 48, 0, notifyEventText);
        CodeAction notifyEventAction = ca(uri, Messages.getMessage("InsertNotifyEventMethod", "AuditEvent"), missingNotifyDiagnostic, notifyEventEdit);

        String notifyEventContextText =
                "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import java.lang.annotation.Annotation;\n" +
                "import java.lang.reflect.Type;\n" +
                "import java.util.Collections;\n" +
                "import java.util.Set;\n\n" +
                "import jakarta.enterprise.event.Reception;\n" +
                "import jakarta.enterprise.event.TransactionPhase;\n" +
                "import jakarta.enterprise.inject.spi.EventContext;\n" +
                "import jakarta.enterprise.inject.spi.ObserverMethod;\n\n" +
                "/**\n" +
                " * Invalid CDI definition: AuditObserver implements ObserverMethod but overrides\n" +
                " * neither notify(T) nor notify(EventContext&lt;T&gt;).\n" +
                " *\n" +
                " * <p>The container will detect this as a definition error at deployment time because\n" +
                " * neither notify overload is provided, so the observer logic cannot be invoked.\n" +
                " */\n" +
                "public class AuditObserver implements ObserverMethod<AuditEvent> {\n\n" +
                "    // Invalid: does not override notify(T) or notify(EventContext<T>)\n\n" +
                "    @Override\n" +
                "    public Class<?> getBeanClass() {\n" +
                "        return AuditObserver.class;\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public Type getObservedType() {\n" +
                "        return AuditEvent.class;\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public Set<Annotation> getObservedQualifiers() {\n" +
                "        return Collections.emptySet();\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public Reception getReception() {\n" +
                "        return Reception.ALWAYS;\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public TransactionPhase getTransactionPhase() {\n" +
                "        return TransactionPhase.IN_PROGRESS;\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public void notify(EventContext<AuditEvent> eventContext) {\n" +
                "    }\n" +
                "}\n";

        TextEdit notifyEventContextEdit = te(0, 0, 48, 0, notifyEventContextText);
        CodeAction notifyEventContextAction = ca(uri, Messages.getMessage("InsertNotifyEventContextMethod", "AuditEvent"), missingNotifyDiagnostic, notifyEventContextEdit);

        assertJavaCodeAction(missingNotifyCodeActionParams, utils, notifyEventAction, notifyEventContextAction);
    }

    /**
     * Verifies that a class implementing ObserverMethod that overrides notify(T)
     * does NOT trigger a diagnostic.
     */
    @Test
    public void testObserverMethodWithNotifyOverride() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/AuditObserverWithNotify.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /**
     * Verifies that a class implementing ObserverMethod that overrides only the
     * {@code notify(EventContext<T>)} overload does NOT trigger a diagnostic.
     * Either overload satisfies the contract.
     */
    @Test
    public void testObserverMethodWithEventContextNotifyOverride() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/AuditObserverWithEventContext.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /**
     * Verifies that a class implementing the raw (non-parameterised) {@code ObserverMethod}
     * type without a notify override triggers the diagnostic.  The quick-fix labels must
     * fall back to {@code Object} because no type argument is present.
     */
    @Test
    public void testObserverMethodRawTypeWithoutNotify() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/AuditObserverRawType.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic rawTypeDiagnostic = d(19, 13, 33,
                "The class 'AuditObserverRawType' implements ObserverMethod but does not override notify(T) or notify(EventContext<T>). " +
                        "At least one of these methods must be overridden for the container to invoke the observer.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidObserverMethodWithoutNotify");

        assertJavaDiagnostics(diagnosticsParams, utils, rawTypeDiagnostic);
    }

    /**
     * Verifies that an abstract class implementing ObserverMethod does NOT trigger a
     * diagnostic. Abstract classes are exempt — they may defer notify to concrete subclasses.
     */
    @Test
    public void testObserverMethodAbstractClassIsExempt() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/AbstractAuditObserver.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }
}
