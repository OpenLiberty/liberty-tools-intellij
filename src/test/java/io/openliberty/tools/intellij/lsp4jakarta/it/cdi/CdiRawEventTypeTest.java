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
 * Tests for CDI raw {@code Event} type injection point diagnostic and quickfix.
 *
 * <p>According to CDI 3.0 specification section 10.2.4:
 * "If an injection point of raw type Event is defined, the container automatically
 * detects the problem and treats it as a definition error."
 *
 * <p>Tests cover:
 * <ul>
 * <li>Raw {@code Event} {@code @Inject} field — diagnostic (invalid)</li>
 * <li>Parameterized {@code Event<T>} fields — no diagnostic (valid)</li>
 * <li>Raw {@code Event} method parameter — diagnostic on method name (invalid)</li>
 * <li>Parameterized {@code Event<T>} method parameter — no diagnostic (valid)</li>
 * <li>Mixed params with one raw {@code Event} — one diagnostic per method</li>
 * <li>Multiple raw {@code Event} params in one method — one diagnostic (not per-param)</li>
 * <li>Raw {@code Event} field without {@code @Inject} — no diagnostic</li>
 * <li>Method returning raw {@code Event} — no diagnostic (not an injection point)</li>
 * <li>Raw {@code Event} field in a nested class — diagnostic fires</li>
 * </ul>
 */
@RunWith(JUnit4.class)
public class CdiRawEventTypeTest extends BaseJakartaTest {

    private static final String REMOVE_INJECT = "Remove @Inject";

    @Test
    public void rawEventInjectionPointDiagnostics() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/RawEventInjectionPoint.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // --- Invalid cases ---

        Diagnostic rawEventFieldDiagnostic = d(17, 10, 18,
                "An injection point of raw type Event is not valid. Specify a type parameter, for example Event<String> or Event<MyType>.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidRawEventTypeInjectionPoint");

        Diagnostic rawEventMethodParamDiagnostic = d(32, 16, 27,
                "An injection point of raw type Event is not valid. Specify a type parameter, for example Event<String> or Event<MyType>.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidRawEventTypeInjectionPoint");

        Diagnostic rawEventMixedParamDiagnostic = d(42, 16, 24,
                "An injection point of raw type Event is not valid. Specify a type parameter, for example Event<String> or Event<MyType>.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidRawEventTypeInjectionPoint");

        Diagnostic rawEventMultipleParamsDiagnostic = d(47, 16, 36,
                "An injection point of raw type Event is not valid. Specify a type parameter, for example Event<String> or Event<MyType>.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidRawEventTypeInjectionPoint");

        Diagnostic rawEventNestedClassDiagnostic = d(59, 14, 29,
                "An injection point of raw type Event is not valid. Specify a type parameter, for example Event<String> or Event<MyType>.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidRawEventTypeInjectionPoint");

        assertJavaDiagnostics(diagnosticsParams, utils,
                rawEventFieldDiagnostic,
                rawEventMethodParamDiagnostic,
                rawEventMixedParamDiagnostic,
                rawEventMultipleParamsDiagnostic,
                rawEventNestedClassDiagnostic);

        String rawEventFieldFixed =
                "package io.openliberty.sample.jakarta.cdi;\n" +
                "\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.enterprise.event.Event;\n" +
                "import jakarta.inject.Inject;\n" +
                "\n" +
                "/**\n" +
                " * Sample class used to test CDI raw Event type diagnostic.\n" +
                " *\n" +
                " * Invalid cases: @Inject field or method parameter of raw type Event (no type argument).\n" +
                " * Valid cases: @Inject field or method parameter using parameterized Event<T>.\n" +
                " */\n" +
                "@ApplicationScoped\n" +
                "public class RawEventInjectionPoint {\n" +
                "\n" +
                "    // Invalid: raw Event type — no type parameter\n" +
                "    Event rawEvent;\n" +
                "\n" +
                "    // Valid: parameterized Event type\n" +
                "    @Inject\n" +
                "    Event<String> typedEvent;\n" +
                "\n" +
                "    // Valid: another parameterized Event type\n" +
                "    @Inject\n" +
                "    Event<OrderCreated> orderEvent;\n" +
                "\n" +
                "    // Valid: Event field with no @Inject — must NOT be flagged\n" +
                "    Event notInjectedRawEvent;\n" +
                "\n" +
                "    // Invalid: @Inject method parameter of raw Event type\n" +
                "    @Inject\n" +
                "    public void setRawEvent(Event event) {\n" +
                "    }\n" +
                "\n" +
                "    // Valid: @Inject method parameter of parameterized Event type\n" +
                "    @Inject\n" +
                "    public void setTypedEvent(Event<String> event) {\n" +
                "    }\n" +
                "\n" +
                "    // Valid: @Inject method with mixed parameters — only raw Event param triggers diagnostic on method\n" +
                "    @Inject\n" +
                "    public void setMixed(String name, Event rawEvent) {\n" +
                "    }\n" +
                "\n" +
                "    // Invalid: @Inject method with multiple raw Event params — one diagnostic per method (not per param)\n" +
                "    @Inject\n" +
                "    public void setMultipleRawEvents(Event first, Event second) {\n" +
                "    }\n" +
                "\n" +
                "    // Valid: method with raw Event return type — not an injection point, must NOT be flagged\n" +
                "    public Event produceRawEvent() {\n" +
                "        return null;\n" +
                "    }\n" +
                "\n" +
                "    // Invalid: nested class with raw Event injection point\n" +
                "    static class Inner {\n" +
                "\n" +
                "        @Inject\n" +
                "        Event rawEventInInner;\n" +
                "    }\n" +
                "\n" +
                "    /**\n" +
                "     * Simple placeholder class representing a domain event.\n" +
                "     */\n" +
                "    static class OrderCreated {\n" +
                "    }\n" +
                "}\n";

        JakartaJavaCodeActionParams rawEventFieldCodeActionParams = createCodeActionParams(uri, rawEventFieldDiagnostic);
        TextEdit removeRawEventFieldInjectEdit = te(0, 0, 68, 0, rawEventFieldFixed);
        CodeAction removeRawEventFieldInjectAction = ca(uri, REMOVE_INJECT, rawEventFieldDiagnostic, removeRawEventFieldInjectEdit);
        assertJavaCodeAction(rawEventFieldCodeActionParams, utils, removeRawEventFieldInjectAction);

        String setRawEventFixed =
                "package io.openliberty.sample.jakarta.cdi;\n" +
                "\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.enterprise.event.Event;\n" +
                "import jakarta.inject.Inject;\n" +
                "\n" +
                "/**\n" +
                " * Sample class used to test CDI raw Event type diagnostic.\n" +
                " *\n" +
                " * Invalid cases: @Inject field or method parameter of raw type Event (no type argument).\n" +
                " * Valid cases: @Inject field or method parameter using parameterized Event<T>.\n" +
                " */\n" +
                "@ApplicationScoped\n" +
                "public class RawEventInjectionPoint {\n" +
                "\n" +
                "    // Invalid: raw Event type — no type parameter\n" +
                "    @Inject\n" +
                "    Event rawEvent;\n" +
                "\n" +
                "    // Valid: parameterized Event type\n" +
                "    @Inject\n" +
                "    Event<String> typedEvent;\n" +
                "\n" +
                "    // Valid: another parameterized Event type\n" +
                "    @Inject\n" +
                "    Event<OrderCreated> orderEvent;\n" +
                "\n" +
                "    // Valid: Event field with no @Inject — must NOT be flagged\n" +
                "    Event notInjectedRawEvent;\n" +
                "\n" +
                "    // Invalid: @Inject method parameter of raw Event type\n" +
                "    public void setRawEvent(Event event) {\n" +
                "    }\n" +
                "\n" +
                "    // Valid: @Inject method parameter of parameterized Event type\n" +
                "    @Inject\n" +
                "    public void setTypedEvent(Event<String> event) {\n" +
                "    }\n" +
                "\n" +
                "    // Valid: @Inject method with mixed parameters — only raw Event param triggers diagnostic on method\n" +
                "    @Inject\n" +
                "    public void setMixed(String name, Event rawEvent) {\n" +
                "    }\n" +
                "\n" +
                "    // Invalid: @Inject method with multiple raw Event params — one diagnostic per method (not per param)\n" +
                "    @Inject\n" +
                "    public void setMultipleRawEvents(Event first, Event second) {\n" +
                "    }\n" +
                "\n" +
                "    // Valid: method with raw Event return type — not an injection point, must NOT be flagged\n" +
                "    public Event produceRawEvent() {\n" +
                "        return null;\n" +
                "    }\n" +
                "\n" +
                "    // Invalid: nested class with raw Event injection point\n" +
                "    static class Inner {\n" +
                "\n" +
                "        @Inject\n" +
                "        Event rawEventInInner;\n" +
                "    }\n" +
                "\n" +
                "    /**\n" +
                "     * Simple placeholder class representing a domain event.\n" +
                "     */\n" +
                "    static class OrderCreated {\n" +
                "    }\n" +
                "}\n";

        JakartaJavaCodeActionParams setRawEventCodeActionParams = createCodeActionParams(uri, rawEventMethodParamDiagnostic);
        TextEdit removeSetRawEventInjectEdit = te(0, 0, 68, 0, setRawEventFixed);
        CodeAction removeSetRawEventInjectAction = ca(uri, REMOVE_INJECT, rawEventMethodParamDiagnostic, removeSetRawEventInjectEdit);
        assertJavaCodeAction(setRawEventCodeActionParams, utils, removeSetRawEventInjectAction);

        String setMixedFixed =
                "package io.openliberty.sample.jakarta.cdi;\n" +
                "\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.enterprise.event.Event;\n" +
                "import jakarta.inject.Inject;\n" +
                "\n" +
                "/**\n" +
                " * Sample class used to test CDI raw Event type diagnostic.\n" +
                " *\n" +
                " * Invalid cases: @Inject field or method parameter of raw type Event (no type argument).\n" +
                " * Valid cases: @Inject field or method parameter using parameterized Event<T>.\n" +
                " */\n" +
                "@ApplicationScoped\n" +
                "public class RawEventInjectionPoint {\n" +
                "\n" +
                "    // Invalid: raw Event type — no type parameter\n" +
                "    @Inject\n" +
                "    Event rawEvent;\n" +
                "\n" +
                "    // Valid: parameterized Event type\n" +
                "    @Inject\n" +
                "    Event<String> typedEvent;\n" +
                "\n" +
                "    // Valid: another parameterized Event type\n" +
                "    @Inject\n" +
                "    Event<OrderCreated> orderEvent;\n" +
                "\n" +
                "    // Valid: Event field with no @Inject — must NOT be flagged\n" +
                "    Event notInjectedRawEvent;\n" +
                "\n" +
                "    // Invalid: @Inject method parameter of raw Event type\n" +
                "    @Inject\n" +
                "    public void setRawEvent(Event event) {\n" +
                "    }\n" +
                "\n" +
                "    // Valid: @Inject method parameter of parameterized Event type\n" +
                "    @Inject\n" +
                "    public void setTypedEvent(Event<String> event) {\n" +
                "    }\n" +
                "\n" +
                "    // Valid: @Inject method with mixed parameters — only raw Event param triggers diagnostic on method\n" +
                "    public void setMixed(String name, Event rawEvent) {\n" +
                "    }\n" +
                "\n" +
                "    // Invalid: @Inject method with multiple raw Event params — one diagnostic per method (not per param)\n" +
                "    @Inject\n" +
                "    public void setMultipleRawEvents(Event first, Event second) {\n" +
                "    }\n" +
                "\n" +
                "    // Valid: method with raw Event return type — not an injection point, must NOT be flagged\n" +
                "    public Event produceRawEvent() {\n" +
                "        return null;\n" +
                "    }\n" +
                "\n" +
                "    // Invalid: nested class with raw Event injection point\n" +
                "    static class Inner {\n" +
                "\n" +
                "        @Inject\n" +
                "        Event rawEventInInner;\n" +
                "    }\n" +
                "\n" +
                "    /**\n" +
                "     * Simple placeholder class representing a domain event.\n" +
                "     */\n" +
                "    static class OrderCreated {\n" +
                "    }\n" +
                "}\n";

        JakartaJavaCodeActionParams setMixedCodeActionParams = createCodeActionParams(uri, rawEventMixedParamDiagnostic);
        TextEdit removeSetMixedInjectEdit = te(0, 0, 68, 0, setMixedFixed);
        CodeAction removeSetMixedInjectAction = ca(uri, REMOVE_INJECT, rawEventMixedParamDiagnostic, removeSetMixedInjectEdit);
        assertJavaCodeAction(setMixedCodeActionParams, utils, removeSetMixedInjectAction);

        String setMultipleRawEventsFixed =
                "package io.openliberty.sample.jakarta.cdi;\n" +
                "\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.enterprise.event.Event;\n" +
                "import jakarta.inject.Inject;\n" +
                "\n" +
                "/**\n" +
                " * Sample class used to test CDI raw Event type diagnostic.\n" +
                " *\n" +
                " * Invalid cases: @Inject field or method parameter of raw type Event (no type argument).\n" +
                " * Valid cases: @Inject field or method parameter using parameterized Event<T>.\n" +
                " */\n" +
                "@ApplicationScoped\n" +
                "public class RawEventInjectionPoint {\n" +
                "\n" +
                "    // Invalid: raw Event type — no type parameter\n" +
                "    @Inject\n" +
                "    Event rawEvent;\n" +
                "\n" +
                "    // Valid: parameterized Event type\n" +
                "    @Inject\n" +
                "    Event<String> typedEvent;\n" +
                "\n" +
                "    // Valid: another parameterized Event type\n" +
                "    @Inject\n" +
                "    Event<OrderCreated> orderEvent;\n" +
                "\n" +
                "    // Valid: Event field with no @Inject — must NOT be flagged\n" +
                "    Event notInjectedRawEvent;\n" +
                "\n" +
                "    // Invalid: @Inject method parameter of raw Event type\n" +
                "    @Inject\n" +
                "    public void setRawEvent(Event event) {\n" +
                "    }\n" +
                "\n" +
                "    // Valid: @Inject method parameter of parameterized Event type\n" +
                "    @Inject\n" +
                "    public void setTypedEvent(Event<String> event) {\n" +
                "    }\n" +
                "\n" +
                "    // Valid: @Inject method with mixed parameters — only raw Event param triggers diagnostic on method\n" +
                "    @Inject\n" +
                "    public void setMixed(String name, Event rawEvent) {\n" +
                "    }\n" +
                "\n" +
                "    // Invalid: @Inject method with multiple raw Event params — one diagnostic per method (not per param)\n" +
                "    public void setMultipleRawEvents(Event first, Event second) {\n" +
                "    }\n" +
                "\n" +
                "    // Valid: method with raw Event return type — not an injection point, must NOT be flagged\n" +
                "    public Event produceRawEvent() {\n" +
                "        return null;\n" +
                "    }\n" +
                "\n" +
                "    // Invalid: nested class with raw Event injection point\n" +
                "    static class Inner {\n" +
                "\n" +
                "        @Inject\n" +
                "        Event rawEventInInner;\n" +
                "    }\n" +
                "\n" +
                "    /**\n" +
                "     * Simple placeholder class representing a domain event.\n" +
                "     */\n" +
                "    static class OrderCreated {\n" +
                "    }\n" +
                "}\n";

        JakartaJavaCodeActionParams setMultipleRawEventsCodeActionParams = createCodeActionParams(uri, rawEventMultipleParamsDiagnostic);
        TextEdit removeSetMultipleRawEventsInjectEdit = te(0, 0, 68, 0, setMultipleRawEventsFixed);
        CodeAction removeSetMultipleRawEventsInjectAction = ca(uri, REMOVE_INJECT, rawEventMultipleParamsDiagnostic, removeSetMultipleRawEventsInjectEdit);
        assertJavaCodeAction(setMultipleRawEventsCodeActionParams, utils, removeSetMultipleRawEventsInjectAction);

        String rawEventInInnerFixed =
                "package io.openliberty.sample.jakarta.cdi;\n" +
                "\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.enterprise.event.Event;\n" +
                "import jakarta.inject.Inject;\n" +
                "\n" +
                "/**\n" +
                " * Sample class used to test CDI raw Event type diagnostic.\n" +
                " *\n" +
                " * Invalid cases: @Inject field or method parameter of raw type Event (no type argument).\n" +
                " * Valid cases: @Inject field or method parameter using parameterized Event<T>.\n" +
                " */\n" +
                "@ApplicationScoped\n" +
                "public class RawEventInjectionPoint {\n" +
                "\n" +
                "    // Invalid: raw Event type — no type parameter\n" +
                "    @Inject\n" +
                "    Event rawEvent;\n" +
                "\n" +
                "    // Valid: parameterized Event type\n" +
                "    @Inject\n" +
                "    Event<String> typedEvent;\n" +
                "\n" +
                "    // Valid: another parameterized Event type\n" +
                "    @Inject\n" +
                "    Event<OrderCreated> orderEvent;\n" +
                "\n" +
                "    // Valid: Event field with no @Inject — must NOT be flagged\n" +
                "    Event notInjectedRawEvent;\n" +
                "\n" +
                "    // Invalid: @Inject method parameter of raw Event type\n" +
                "    @Inject\n" +
                "    public void setRawEvent(Event event) {\n" +
                "    }\n" +
                "\n" +
                "    // Valid: @Inject method parameter of parameterized Event type\n" +
                "    @Inject\n" +
                "    public void setTypedEvent(Event<String> event) {\n" +
                "    }\n" +
                "\n" +
                "    // Valid: @Inject method with mixed parameters — only raw Event param triggers diagnostic on method\n" +
                "    @Inject\n" +
                "    public void setMixed(String name, Event rawEvent) {\n" +
                "    }\n" +
                "\n" +
                "    // Invalid: @Inject method with multiple raw Event params — one diagnostic per method (not per param)\n" +
                "    @Inject\n" +
                "    public void setMultipleRawEvents(Event first, Event second) {\n" +
                "    }\n" +
                "\n" +
                "    // Valid: method with raw Event return type — not an injection point, must NOT be flagged\n" +
                "    public Event produceRawEvent() {\n" +
                "        return null;\n" +
                "    }\n" +
                "\n" +
                "    // Invalid: nested class with raw Event injection point\n" +
                "    static class Inner {\n" +
                "\n" +
                "        Event rawEventInInner;\n" +
                "    }\n" +
                "\n" +
                "    /**\n" +
                "     * Simple placeholder class representing a domain event.\n" +
                "     */\n" +
                "    static class OrderCreated {\n" +
                "    }\n" +
                "}\n";

        JakartaJavaCodeActionParams rawEventInInnerCodeActionParams = createCodeActionParams(uri, rawEventNestedClassDiagnostic);
        TextEdit removeRawEventInInnerInjectEdit = te(0, 0, 68, 0, rawEventInInnerFixed);
        CodeAction removeRawEventInInnerInjectAction = ca(uri, REMOVE_INJECT, rawEventNestedClassDiagnostic, removeRawEventInInnerInjectEdit);
        assertJavaCodeAction(rawEventInInnerCodeActionParams, utils, removeRawEventInInnerInjectAction);
    }
}
