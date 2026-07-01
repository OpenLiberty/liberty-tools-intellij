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
 * Tests for CDI @Named qualifier validation on injection points.
 *
 * According to CDI specification section 3.9:
 * - @Named on field injection without value is VALID (field name is assumed)
 * - @Named on constructor/method parameters without value is INVALID (definition error)
 */
@RunWith(JUnit4.class)
public class NamedQualifierInjectionPointTest extends BaseJakartaTest {

    @Test
    public void namedQualifierOnInjectionPoints() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/NamedQualifierInjectionPoint.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test expected diagnostics for @Named without value on non-field injection points

        // Line 29 (0-based: 28): @Named on constructor parameter without value
        Diagnostic constructorParamDiagnostic = d(28, 40, 46,
                "@Named annotation on constructor or method parameter must specify a value attribute. Field injection points can omit the value, but constructor and method parameters cannot.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidNamedAnnotationOnNonFieldInjectionPoint");

        // Line 36 (0-based: 35): @Named on method parameter without value
        Diagnostic methodParamDiagnostic = d(35, 28, 34,
                "@Named annotation on constructor or method parameter must specify a value attribute. Field injection points can omit the value, but constructor and method parameters cannot.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidNamedAnnotationOnNonFieldInjectionPoint");

        // Line 43 (0-based: 42): @Named on initializer method parameter without value
        Diagnostic initializerParamDiagnostic = d(42, 27, 33,
                "@Named annotation on constructor or method parameter must specify a value attribute. Field injection points can omit the value, but constructor and method parameters cannot.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidNamedAnnotationOnNonFieldInjectionPoint");

        assertJavaDiagnostics(diagnosticsParams, utils, constructorParamDiagnostic, methodParamDiagnostic, initializerParamDiagnostic);

        // Test code action for constructorParamDiagnostic - Insert value attribute
        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, constructorParamDiagnostic);
        String newText1 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.inject.Inject;\n" +
                "import jakarta.inject.Named;\n\n" +
                "/**\n" +
                " * Test class for @Named qualifier validation on injection points.\n" +
                " * According to CDI spec section 3.9:\n" +
                " * - @Named on field injection without value is VALID (field name is assumed)\n" +
                " * - @Named on constructor/method parameters without value is INVALID (definition error)\n" +
                " */\n" +
                "@ApplicationScoped\n" +
                "public class NamedQualifierInjectionPoint {\n" +
                "    \n" +
                "    // VALID: Field injection with @Named without value - field name \"paymentService\" is assumed\n" +
                "    @Inject\n" +
                "    @Named\n" +
                "    private String paymentService;\n" +
                "    \n" +
                "    // VALID: Field injection with @Named with value\n" +
                "    @Inject\n" +
                "    @Named(\"customName\")\n" +
                "    private String orderService;\n" +
                "    \n" +
                "    // INVALID: Constructor parameter with @Named without value\n" +
                "    // This should trigger a diagnostic error\n" +
                "    @Inject\n" +
                "    public NamedQualifierInjectionPoint(@Named(value = \"\") String greeting) {\n" +
                "        System.out.println(greeting);\n" +
                "    }\n" +
                "    \n" +
                "    // INVALID: Method parameter with @Named without value\n" +
                "    // This should trigger a diagnostic error\n" +
                "    @Inject\n" +
                "    public void setGreeting(@Named String greeting) {\n" +
                "        System.out.println(greeting);\n" +
                "    }\n" +
                "    \n" +
                "    // INVALID: Initializer method with @Named without value on parameter\n" +
                "    // This should trigger a diagnostic error\n" +
                "    @Inject\n" +
                "    public void initialize(@Named String config) {\n" +
                "        System.out.println(config);\n" +
                "    }\n" +
                "}";
        TextEdit insertValueConstructorEdit = te(0, 0, 45, 1, newText1);
        CodeAction insertValueConstructorAction = ca(uri, "Insert 'value' attribute to @Named", constructorParamDiagnostic, insertValueConstructorEdit);
        assertJavaCodeAction(codeActionParams1, utils, insertValueConstructorAction);

        // Test code action for methodParamDiagnostic - Insert value attribute
        JakartaJavaCodeActionParams codeActionParams2 = createCodeActionParams(uri, methodParamDiagnostic);
        String newText2 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.inject.Inject;\n" +
                "import jakarta.inject.Named;\n\n" +
                "/**\n" +
                " * Test class for @Named qualifier validation on injection points.\n" +
                " * According to CDI spec section 3.9:\n" +
                " * - @Named on field injection without value is VALID (field name is assumed)\n" +
                " * - @Named on constructor/method parameters without value is INVALID (definition error)\n" +
                " */\n" +
                "@ApplicationScoped\n" +
                "public class NamedQualifierInjectionPoint {\n" +
                "    \n" +
                "    // VALID: Field injection with @Named without value - field name \"paymentService\" is assumed\n" +
                "    @Inject\n" +
                "    @Named\n" +
                "    private String paymentService;\n" +
                "    \n" +
                "    // VALID: Field injection with @Named with value\n" +
                "    @Inject\n" +
                "    @Named(\"customName\")\n" +
                "    private String orderService;\n" +
                "    \n" +
                "    // INVALID: Constructor parameter with @Named without value\n" +
                "    // This should trigger a diagnostic error\n" +
                "    @Inject\n" +
                "    public NamedQualifierInjectionPoint(@Named String greeting) {\n" +
                "        System.out.println(greeting);\n" +
                "    }\n" +
                "    \n" +
                "    // INVALID: Method parameter with @Named without value\n" +
                "    // This should trigger a diagnostic error\n" +
                "    @Inject\n" +
                "    public void setGreeting(@Named(value = \"\") String greeting) {\n" +
                "        System.out.println(greeting);\n" +
                "    }\n" +
                "    \n" +
                "    // INVALID: Initializer method with @Named without value on parameter\n" +
                "    // This should trigger a diagnostic error\n" +
                "    @Inject\n" +
                "    public void initialize(@Named String config) {\n" +
                "        System.out.println(config);\n" +
                "    }\n" +
                "}";
        TextEdit insertValueMethodEdit = te(0, 0, 45, 1, newText2);
        CodeAction insertValueMethodAction = ca(uri, "Insert 'value' attribute to @Named", methodParamDiagnostic, insertValueMethodEdit);
        assertJavaCodeAction(codeActionParams2, utils, insertValueMethodAction);

        // Test code action for initializerParamDiagnostic - Insert value attribute
        JakartaJavaCodeActionParams codeActionParams3 = createCodeActionParams(uri, initializerParamDiagnostic);
        String newText3 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.inject.Inject;\n" +
                "import jakarta.inject.Named;\n\n" +
                "/**\n" +
                " * Test class for @Named qualifier validation on injection points.\n" +
                " * According to CDI spec section 3.9:\n" +
                " * - @Named on field injection without value is VALID (field name is assumed)\n" +
                " * - @Named on constructor/method parameters without value is INVALID (definition error)\n" +
                " */\n" +
                "@ApplicationScoped\n" +
                "public class NamedQualifierInjectionPoint {\n" +
                "    \n" +
                "    // VALID: Field injection with @Named without value - field name \"paymentService\" is assumed\n" +
                "    @Inject\n" +
                "    @Named\n" +
                "    private String paymentService;\n" +
                "    \n" +
                "    // VALID: Field injection with @Named with value\n" +
                "    @Inject\n" +
                "    @Named(\"customName\")\n" +
                "    private String orderService;\n" +
                "    \n" +
                "    // INVALID: Constructor parameter with @Named without value\n" +
                "    // This should trigger a diagnostic error\n" +
                "    @Inject\n" +
                "    public NamedQualifierInjectionPoint(@Named String greeting) {\n" +
                "        System.out.println(greeting);\n" +
                "    }\n" +
                "    \n" +
                "    // INVALID: Method parameter with @Named without value\n" +
                "    // This should trigger a diagnostic error\n" +
                "    @Inject\n" +
                "    public void setGreeting(@Named String greeting) {\n" +
                "        System.out.println(greeting);\n" +
                "    }\n" +
                "    \n" +
                "    // INVALID: Initializer method with @Named without value on parameter\n" +
                "    // This should trigger a diagnostic error\n" +
                "    @Inject\n" +
                "    public void initialize(@Named(value = \"\") String config) {\n" +
                "        System.out.println(config);\n" +
                "    }\n" +
                "}";
        TextEdit insertValueInitializerEdit = te(0, 0, 45, 1, newText3);
        CodeAction insertValueInitializerAction = ca(uri, "Insert 'value' attribute to @Named", initializerParamDiagnostic, insertValueInitializerEdit);
        assertJavaCodeAction(codeActionParams3, utils, insertValueInitializerAction);
    }

    @Test
    public void validNamedInjectionPoints() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/ValidNamedInjectionPoint.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // This file contains only VALID uses of @Named - should produce NO diagnostics
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void namedInjectionPointComprehensive() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/NamedInjectionPoint.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test expected diagnostics for comprehensive test file with nested classes

        // Line 22 (0-based: 21): @Named on constructor parameter without value
        Diagnostic constructorDiagnostic = d(21, 31, 37,
                "@Named annotation on constructor or method parameter must specify a value attribute. Field injection points can omit the value, but constructor and method parameters cannot.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidNamedAnnotationOnNonFieldInjectionPoint");

        // Line 36 (0-based: 35): @Named on method parameter without value
        Diagnostic methodDiagnostic = d(35, 28, 34,
                "@Named annotation on constructor or method parameter must specify a value attribute. Field injection points can omit the value, but constructor and method parameters cannot.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidNamedAnnotationOnNonFieldInjectionPoint");

        // Line 49 (0-based: 48): First @Named in nested class with multiple params
        Diagnostic nestedClass1Diagnostic = d(48, 37, 43,
                "@Named annotation on constructor or method parameter must specify a value attribute. Field injection points can omit the value, but constructor and method parameters cannot.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidNamedAnnotationOnNonFieldInjectionPoint");

        // Line 49 (0-based: 48): Second @Named in nested class with multiple params
        Diagnostic nestedClass2Diagnostic = d(48, 61, 67,
                "@Named annotation on constructor or method parameter must specify a value attribute. Field injection points can omit the value, but constructor and method parameters cannot.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidNamedAnnotationOnNonFieldInjectionPoint");

        // Line 57 (0-based: 56): @Named without value in mixed params nested class
        Diagnostic mixedParamDiagnostic = d(56, 60, 66,
                "@Named annotation on constructor or method parameter must specify a value attribute. Field injection points can omit the value, but constructor and method parameters cannot.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidNamedAnnotationOnNonFieldInjectionPoint");

        // Line 64 (0-based: 63): @Named on initializer method parameter without value
        Diagnostic initializerDiagnostic = d(63, 27, 33,
                "@Named annotation on constructor or method parameter must specify a value attribute. Field injection points can omit the value, but constructor and method parameters cannot.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidNamedAnnotationOnNonFieldInjectionPoint");

        assertJavaDiagnostics(diagnosticsParams, utils, constructorDiagnostic, methodDiagnostic,
                nestedClass1Diagnostic, nestedClass2Diagnostic, mixedParamDiagnostic, initializerDiagnostic);

        // Test code action for one of the diagnostics
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, constructorDiagnostic);
        String newText = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n" +
                "import jakarta.inject.Inject;\n" +
                "import jakarta.inject.Named;\n\n" +
                "@ApplicationScoped\n" +
                "public class NamedInjectionPoint {\n" +
                "    \n" +
                "    // VALID: Field injection with @Named without value - field name is assumed\n" +
                "    @Inject\n" +
                "    @Named\n" +
                "    private String paymentService;\n" +
                "    \n" +
                "    // VALID: Field injection with @Named with value\n" +
                "    @Inject\n" +
                "    @Named(\"customName\")\n" +
                "    private String orderService;\n" +
                "    \n" +
                "    // INVALID: Constructor parameter with @Named without value\n" +
                "    @Inject\n" +
                "    public NamedInjectionPoint(@Named(value = \"\") String greeting) {\n" +
                "        System.out.println(greeting);\n" +
                "    }\n" +
                "    \n" +
                "    // VALID: Constructor parameter with @Named with value\n" +
                "    public static class ValidConstructor {\n" +
                "        @Inject\n" +
                "        public ValidConstructor(@Named(\"hello\") String greeting) {\n" +
                "            System.out.println(greeting);\n" +
                "        }\n" +
                "    }\n" +
                "    \n" +
                "    // INVALID: Method parameter with @Named without value\n" +
                "    @Inject\n" +
                "    public void setGreeting(@Named String greeting) {\n" +
                "        System.out.println(greeting);\n" +
                "    }\n" +
                "    \n" +
                "    // VALID: Method parameter with @Named with value\n" +
                "    @Inject\n" +
                "    public void setMessage(@Named(\"welcome\") String message) {\n" +
                "        System.out.println(message);\n" +
                "    }\n" +
                "    \n" +
                "    // INVALID: Multiple constructor parameters with @Named without value\n" +
                "    public static class MultipleInvalidParams {\n" +
                "        @Inject\n" +
                "        public MultipleInvalidParams(@Named String greeting, @Named String farewell) {\n" +
                "            System.out.println(greeting + \" \" + farewell);\n" +
                "        }\n" +
                "    }\n" +
                "    \n" +
                "    // VALID: Mixed - one with value, one without (but still invalid for the one without)\n" +
                "    public static class MixedParams {\n" +
                "        @Inject\n" +
                "        public MixedParams(@Named(\"hello\") String greeting, @Named String farewell) {\n" +
                "            System.out.println(greeting + \" \" + farewell);\n" +
                "        }\n" +
                "    }\n" +
                "    \n" +
                "    // INVALID: Initializer method with @Named without value on parameter\n" +
                "    @Inject\n" +
                "    public void initialize(@Named String config) {\n" +
                "        System.out.println(config);\n" +
                "    }\n" +
                "    \n" +
                "    // VALID: Regular field without @Inject - @Named is not relevant here\n" +
                "    @Named\n" +
                "    private String regularField;\n" +
                "}";
        TextEdit insertValueEdit = te(0, 0, 70, 1, newText);
        CodeAction insertValueAction = ca(uri, "Insert 'value' attribute to @Named", constructorDiagnostic, insertValueEdit);
        assertJavaCodeAction(codeActionParams, utils, insertValueAction);
    }
}