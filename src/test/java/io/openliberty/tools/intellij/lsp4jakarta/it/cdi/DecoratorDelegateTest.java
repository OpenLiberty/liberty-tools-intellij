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
 * Tests for CDI decorator @Delegate injection point validation.
 *
 * A decorator must declare exactly one injection point annotated with @Delegate.
 *
 * @see https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0#delegate_attribute
 */
@RunWith(JUnit4.class)
public class DecoratorDelegateTest extends BaseJakartaTest {

    /**
     * Test that a decorator with multiple @Delegate fields triggers diagnostics at each field.
     *
     * Expected: Error on each @Delegate field indicating 2 @Delegate injection points found.
     */
    @Test
    public void testDecoratorWithMultipleDelegateFields() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/decorator/DecoratorWithMultipleDelegates.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Expected diagnostics on each @Delegate field
        // Line 16 (0-based: 15), field name "delegateA" starts at column 27, ends at column 36
        Diagnostic delegateADiagnostic = d(15, 27, 36,
                "A decorator must declare exactly one injection point annotated with @Delegate, but found 2.",
                DiagnosticSeverity.Error,
                "jakarta-cdi",
                "InvalidDecoratorDelegateInjectionPoints");

        // Line 20 (0-based: 19), field name "delegateB" starts at column 27, ends at column 36
        Diagnostic delegateBDiagnostic = d(19, 27, 36,
                "A decorator must declare exactly one injection point annotated with @Delegate, but found 2.",
                DiagnosticSeverity.Error,
                "jakarta-cdi",
                "InvalidDecoratorDelegateInjectionPoints");

        assertJavaDiagnostics(diagnosticsParams, utils, delegateADiagnostic, delegateBDiagnostic);
    }

    /**
     * Test that a decorator with no @Delegate injection point triggers a diagnostic.
     *
     * Expected: Error on class name indicating no @Delegate injection point.
     */
    @Test
    public void testDecoratorWithNoDelegate() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/decorator/DecoratorWithNoDelegate.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Expected diagnostic on class name
        // Line 11 (0-based: 10), class name "DecoratorWithNoDelegate" starts at column 13, ends at column 36
        Diagnostic noDelegateDiagnostic = d(10, 13, 36,
                "A decorator must declare exactly one injection point annotated with @Delegate.",
                DiagnosticSeverity.Error,
                "jakarta-cdi",
                "InvalidDecoratorDelegateInjectionPoints");

        assertJavaDiagnostics(diagnosticsParams, utils, noDelegateDiagnostic);
    }

    /**
     * Test that a decorator with @Delegate on both field and constructor parameter triggers diagnostics at each location.
     *
     * Expected: Error on field and parameter indicating 2 @Delegate injection points found.
     */
    @Test
    public void testDecoratorWithMixedDelegateInjectionPoints() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/decorator/DecoratorWithMixedDelegates.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Expected diagnostics on field and constructor parameter
        // Line 16 (0-based: 15), field name "delegateField" starts at column 27, ends at column 40
        Diagnostic fieldDelegateDiagnostic = d(15, 27, 40,
                "A decorator must declare exactly one injection point annotated with @Delegate, but found 2.",
                DiagnosticSeverity.Error,
                "jakarta-cdi",
                "InvalidDecoratorDelegateInjectionPoints");

        // Line 21 (0-based: 20), parameter name "delegate" starts at column 64, ends at column 72
        Diagnostic paramDelegateDiagnostic = d(20, 64, 72,
                "A decorator must declare exactly one injection point annotated with @Delegate, but found 2.",
                DiagnosticSeverity.Error,
                "jakarta-cdi",
                "InvalidDecoratorDelegateInjectionPoints");

        // Only decorator diagnostics expected - constructor has @Inject so no DI diagnostic
        assertJavaDiagnostics(diagnosticsParams, utils, fieldDelegateDiagnostic, paramDelegateDiagnostic);
    }

    /**
     * Test that a valid decorator with exactly one @Delegate field does NOT trigger a diagnostic.
     *
     * Expected: No diagnostics.
     */
    @Test
    public void testValidDecoratorWithDelegateField() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/decorator/ValidDecorator.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No diagnostics expected for valid decorator
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /**
     * Test that a valid decorator with @Delegate on constructor parameter does NOT trigger a diagnostic.
     *
     * Expected: No diagnostics (constructor has @Inject annotation).
     */
    @Test
    public void testValidDecoratorWithDelegateOnConstructorParameter() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/decorator/DecoratorWithDelegateOnConstructor.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No diagnostics expected - constructor has @Inject annotation, so it's valid
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /**
     * Test that a valid decorator with @Delegate on initializer method parameter does NOT trigger a diagnostic.
     *
     * Expected: No diagnostics.
     */
    @Test
    public void testValidDecoratorWithDelegateOnMethodParameter() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/decorator/DecoratorWithDelegateOnMethod.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No diagnostics expected for valid decorator with method injection
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    /**
     * Test that @Delegate on a field without @Inject triggers a diagnostic and offers quickfix.
     *
     * Expected: Error on field indicating @Delegate must be on an injected field.
     */
    @Test
    public void testDelegateOnNonInjectedField() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/decorator/InvalidDelegateLocations.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Expected diagnostic 1: InvalidManagedBeanConstructor on constructor
        // Line 52 (0-based: 51), constructor name starts at column 11, ends at column 48
        Diagnostic constructorManagedBeanDiagnostic = d(51, 11, 48,
                "The @Inject annotation must define a managed bean constructor that takes parameters, or the managed bean must resolve to having a no-arg constructor instead.",
                DiagnosticSeverity.Error,
                "jakarta-cdi",
                "InvalidManagedBeanConstructor");

        // Expected diagnostic 2: InvalidDelegateInjectionPoint on constructor
        // Line 52 (0-based: 51), constructor name starts at column 11, ends at column 48
        Diagnostic constructorParamDiagnostic = d(51, 11, 48,
                "@Delegate must be applied to an injected field, or to a parameter of an initializer or constructor method.",
                DiagnosticSeverity.Error,
                "jakarta-cdi",
                "InvalidDelegateInjectionPoint");

        // Expected diagnostic 3: InvalidDelegateInjectionPoint on method
        // Line 33 (0-based: 32), method name "setDelegate" starts at column 16, ends at column 27
        Diagnostic methodParamDiagnostic = d(32, 16, 27,
                "@Delegate must be applied to an injected field, or to a parameter of an initializer or constructor method.",
                DiagnosticSeverity.Error,
                "jakarta-cdi",
                "InvalidDelegateInjectionPoint");

        // Expected diagnostic 4: InvalidDelegateInjectionPoint on field
        // Line 16 (0-based: 15), field name "delegate" starts at column 27, ends at column 35
        Diagnostic fieldDiagnostic = d(15, 27, 35,
                "@Delegate must be applied to an injected field, or to a parameter of an initializer or constructor method.",
                DiagnosticSeverity.Error,
                "jakarta-cdi",
                "InvalidDelegateInjectionPoint");

        assertJavaDiagnostics(diagnosticsParams, utils, constructorManagedBeanDiagnostic, constructorParamDiagnostic, methodParamDiagnostic, fieldDiagnostic);

        // Test quickfix for field - InsertAnnotationMissingQuickFix rewrites the entire file
        JakartaJavaCodeActionParams fieldCodeActionParams = createCodeActionParams(uri, fieldDiagnostic);
        String fieldFixedContent = "package io.openliberty.sample.jakarta.cdi.decorator;\n\n" +
                "import jakarta.decorator.Decorator;\n" +
                "import jakarta.decorator.Delegate;\n" +
                "import jakarta.enterprise.context.Dependent;\n" +
                "import jakarta.inject.Inject;\n\n" +
                "/**\n * Invalid: @Delegate on field without @Inject\n */\n" +
                "@Decorator\n@Dependent\nclass DelegateOnNonInjectedField implements PaymentService {\n\n" +
                "    @Inject\n    @Delegate\n    private PaymentService delegate;\n\n" +
                "    @Override\n    public void processPayment(double amount) {\n" +
                "        delegate.processPayment(amount);\n    }\n}\n\n" +
                "/**\n * Invalid: @Delegate on method parameter without @Inject on method\n */\n" +
                "@Decorator\n@Dependent\nclass DelegateOnNonInjectedMethodParam implements PaymentService {\n\n" +
                "    private PaymentService delegate;\n\n" +
                "    public void setDelegate(@Delegate PaymentService delegate) {\n" +
                "        this.delegate = delegate;\n    }\n\n" +
                "    @Override\n    public void processPayment(double amount) {\n" +
                "        delegate.processPayment(amount);\n    }\n}\n\n" +
                "/**\n * Invalid: @Delegate on constructor parameter without @Inject on constructor\n */\n" +
                "@Decorator\n@Dependent\nclass DelegateOnNonInjectedConstructorParam implements PaymentService {\n\n" +
                "    private PaymentService delegate;\n\n" +
                "    public DelegateOnNonInjectedConstructorParam(@Delegate PaymentService delegate) {\n" +
                "        this.delegate = delegate;\n    }\n\n" +
                "    @Override\n    public void processPayment(double amount) {\n" +
                "        delegate.processPayment(amount);\n    }\n}\n\n" +
                "/**\n * Valid: @Delegate on field with @Inject\n */\n" +
                "@Decorator\n@Dependent\nclass ValidDelegateOnInjectedField implements PaymentService {\n\n" +
                "    @Inject\n    @Delegate\n    private PaymentService delegate;\n\n" +
                "    @Override\n    public void processPayment(double amount) {\n" +
                "        delegate.processPayment(amount);\n    }\n}\n\n" +
                "/**\n * Valid: @Delegate on constructor parameter with @Inject on constructor\n */\n" +
                "@Decorator\n@Dependent\nclass ValidDelegateOnConstructorParam implements PaymentService {\n\n" +
                "    private PaymentService delegate;\n\n" +
                "    @Inject\n" +
                "    public ValidDelegateOnConstructorParam(@Delegate PaymentService delegate) {\n" +
                "        this.delegate = delegate;\n    }\n\n" +
                "    @Override\n    public void processPayment(double amount) {\n" +
                "        delegate.processPayment(amount);\n    }\n}\n\n" +
                "/**\n * Valid: @Delegate on method parameter with @Inject on method\n */\n" +
                "@Decorator\n@Dependent\nclass ValidDelegateOnMethodParam implements PaymentService {\n\n" +
                "    private PaymentService delegate;\n\n" +
                "    @Inject\n" +
                "    public void setDelegate(@Delegate PaymentService delegate) {\n" +
                "        this.delegate = delegate;\n    }\n\n" +
                "    @Override\n    public void processPayment(double amount) {\n" +
                "        delegate.processPayment(amount);\n    }\n}\n";
        TextEdit fieldTextEdit = te(0, 0, 117, 0, fieldFixedContent);
        CodeAction fieldInsertInjectAction = ca(uri, "Insert @Inject", fieldDiagnostic, fieldTextEdit);
        assertJavaCodeAction(fieldCodeActionParams, utils, fieldInsertInjectAction);

        // Test quickfix for method - InsertAnnotationMissingQuickFix rewrites the entire file
        JakartaJavaCodeActionParams methodCodeActionParams = createCodeActionParams(uri, methodParamDiagnostic);
        String methodFixedContent = "package io.openliberty.sample.jakarta.cdi.decorator;\n\n" +
                "import jakarta.decorator.Decorator;\n" +
                "import jakarta.decorator.Delegate;\n" +
                "import jakarta.enterprise.context.Dependent;\n" +
                "import jakarta.inject.Inject;\n\n" +
                "/**\n * Invalid: @Delegate on field without @Inject\n */\n" +
                "@Decorator\n@Dependent\nclass DelegateOnNonInjectedField implements PaymentService {\n\n" +
                "    @Delegate\n    private PaymentService delegate;\n\n" +
                "    @Override\n    public void processPayment(double amount) {\n" +
                "        delegate.processPayment(amount);\n    }\n}\n\n" +
                "/**\n * Invalid: @Delegate on method parameter without @Inject on method\n */\n" +
                "@Decorator\n@Dependent\nclass DelegateOnNonInjectedMethodParam implements PaymentService {\n\n" +
                "    private PaymentService delegate;\n\n" +
                "    @Inject\n    public void setDelegate(@Delegate PaymentService delegate) {\n" +
                "        this.delegate = delegate;\n    }\n\n" +
                "    @Override\n    public void processPayment(double amount) {\n" +
                "        delegate.processPayment(amount);\n    }\n}\n\n" +
                "/**\n * Invalid: @Delegate on constructor parameter without @Inject on constructor\n */\n" +
                "@Decorator\n@Dependent\nclass DelegateOnNonInjectedConstructorParam implements PaymentService {\n\n" +
                "    private PaymentService delegate;\n\n" +
                "    public DelegateOnNonInjectedConstructorParam(@Delegate PaymentService delegate) {\n" +
                "        this.delegate = delegate;\n    }\n\n" +
                "    @Override\n    public void processPayment(double amount) {\n" +
                "        delegate.processPayment(amount);\n    }\n}\n\n" +
                "/**\n * Valid: @Delegate on field with @Inject\n */\n" +
                "@Decorator\n@Dependent\nclass ValidDelegateOnInjectedField implements PaymentService {\n\n" +
                "    @Inject\n    @Delegate\n    private PaymentService delegate;\n\n" +
                "    @Override\n    public void processPayment(double amount) {\n" +
                "        delegate.processPayment(amount);\n    }\n}\n\n" +
                "/**\n * Valid: @Delegate on constructor parameter with @Inject on constructor\n */\n" +
                "@Decorator\n@Dependent\nclass ValidDelegateOnConstructorParam implements PaymentService {\n\n" +
                "    private PaymentService delegate;\n\n" +
                "    @Inject\n" +
                "    public ValidDelegateOnConstructorParam(@Delegate PaymentService delegate) {\n" +
                "        this.delegate = delegate;\n    }\n\n" +
                "    @Override\n    public void processPayment(double amount) {\n" +
                "        delegate.processPayment(amount);\n    }\n}\n\n" +
                "/**\n * Valid: @Delegate on method parameter with @Inject on method\n */\n" +
                "@Decorator\n@Dependent\nclass ValidDelegateOnMethodParam implements PaymentService {\n\n" +
                "    private PaymentService delegate;\n\n" +
                "    @Inject\n" +
                "    public void setDelegate(@Delegate PaymentService delegate) {\n" +
                "        this.delegate = delegate;\n    }\n\n" +
                "    @Override\n    public void processPayment(double amount) {\n" +
                "        delegate.processPayment(amount);\n    }\n}\n";
        TextEdit methodTextEdit = te(0, 0, 117, 0, methodFixedContent);
        CodeAction methodInsertInjectAction = ca(uri, "Insert @Inject", methodParamDiagnostic, methodTextEdit);
        assertJavaCodeAction(methodCodeActionParams, utils, methodInsertInjectAction);

        // Test quickfix for constructor - InsertAnnotationMissingQuickFix rewrites the entire file
        JakartaJavaCodeActionParams constructorCodeActionParams = createCodeActionParams(uri, constructorParamDiagnostic);
        String constructorFixedContent = "package io.openliberty.sample.jakarta.cdi.decorator;\n\n" +
                "import jakarta.decorator.Decorator;\n" +
                "import jakarta.decorator.Delegate;\n" +
                "import jakarta.enterprise.context.Dependent;\n" +
                "import jakarta.inject.Inject;\n\n" +
                "/**\n * Invalid: @Delegate on field without @Inject\n */\n" +
                "@Decorator\n@Dependent\nclass DelegateOnNonInjectedField implements PaymentService {\n\n" +
                "    @Delegate\n    private PaymentService delegate;\n\n" +
                "    @Override\n    public void processPayment(double amount) {\n" +
                "        delegate.processPayment(amount);\n    }\n}\n\n" +
                "/**\n * Invalid: @Delegate on method parameter without @Inject on method\n */\n" +
                "@Decorator\n@Dependent\nclass DelegateOnNonInjectedMethodParam implements PaymentService {\n\n" +
                "    private PaymentService delegate;\n\n" +
                "    public void setDelegate(@Delegate PaymentService delegate) {\n" +
                "        this.delegate = delegate;\n    }\n\n" +
                "    @Override\n    public void processPayment(double amount) {\n" +
                "        delegate.processPayment(amount);\n    }\n}\n\n" +
                "/**\n * Invalid: @Delegate on constructor parameter without @Inject on constructor\n */\n" +
                "@Decorator\n@Dependent\nclass DelegateOnNonInjectedConstructorParam implements PaymentService {\n\n" +
                "    private PaymentService delegate;\n\n" +
                "    @Inject\n    public DelegateOnNonInjectedConstructorParam(@Delegate PaymentService delegate) {\n" +
                "        this.delegate = delegate;\n    }\n\n" +
                "    @Override\n    public void processPayment(double amount) {\n" +
                "        delegate.processPayment(amount);\n    }\n}\n\n" +
                "/**\n * Valid: @Delegate on field with @Inject\n */\n" +
                "@Decorator\n@Dependent\nclass ValidDelegateOnInjectedField implements PaymentService {\n\n" +
                "    @Inject\n    @Delegate\n    private PaymentService delegate;\n\n" +
                "    @Override\n    public void processPayment(double amount) {\n" +
                "        delegate.processPayment(amount);\n    }\n}\n\n" +
                "/**\n * Valid: @Delegate on constructor parameter with @Inject on constructor\n */\n" +
                "@Decorator\n@Dependent\nclass ValidDelegateOnConstructorParam implements PaymentService {\n\n" +
                "    private PaymentService delegate;\n\n" +
                "    @Inject\n" +
                "    public ValidDelegateOnConstructorParam(@Delegate PaymentService delegate) {\n" +
                "        this.delegate = delegate;\n    }\n\n" +
                "    @Override\n    public void processPayment(double amount) {\n" +
                "        delegate.processPayment(amount);\n    }\n}\n\n" +
                "/**\n * Valid: @Delegate on method parameter with @Inject on method\n */\n" +
                "@Decorator\n@Dependent\nclass ValidDelegateOnMethodParam implements PaymentService {\n\n" +
                "    private PaymentService delegate;\n\n" +
                "    @Inject\n" +
                "    public void setDelegate(@Delegate PaymentService delegate) {\n" +
                "        this.delegate = delegate;\n    }\n\n" +
                "    @Override\n    public void processPayment(double amount) {\n" +
                "        delegate.processPayment(amount);\n    }\n}\n";
        TextEdit constructorTextEdit = te(0, 0, 117, 0, constructorFixedContent);
        CodeAction constructorInsertInjectAction = ca(uri, "Insert @Inject", constructorParamDiagnostic, constructorTextEdit);
        assertJavaCodeAction(constructorCodeActionParams, utils, constructorInsertInjectAction);
    }

    /**
     * Test that decorators with invalid delegate type assignability trigger diagnostics.
     *
     * The file contains multiple test cases:
     * - InvalidDelegateType: delegate type (Logger) doesn't implement PaymentService (field-level)
     * - InvalidDelegateTypePrimitive: delegate type (String) doesn't implement PaymentService (field-level)
     * - InvalidDelegateTypeOnMethod: delegate type (Logger) doesn't implement PaymentService (method-level)
     *
     * Expected: Errors on delegate fields and method parameter indicating type mismatch.
     */
    @Test
    public void testDecoratorWithInvalidDelegateType() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/decorator/DecoratorDelegateTypeAssignability.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Expected diagnostic on InvalidDelegateType class (field-level)
        // Line 18 (0-based: 17), field name "delegate" starts at column 19, ends at column 27
        Diagnostic invalidTypeDiagnostic1 = d(17, 19, 27,
                "The delegate type 'Logger' must implement or extend all decorated types.",
                DiagnosticSeverity.Error,
                "jakarta-cdi",
                "InvalidDecoratorDelegateTypeAssignability");

        // Expected diagnostic on InvalidDelegateTypePrimitive class (field-level)
        // Line 72 (0-based: 71), field name "delegate" starts at column 19, ends at column 27
        Diagnostic invalidTypeDiagnostic2 = d(71, 19, 27,
                "The delegate type 'String' must implement or extend all decorated types.",
                DiagnosticSeverity.Error,
                "jakarta-cdi",
                "InvalidDecoratorDelegateTypeAssignability");

        // Expected diagnostic on InvalidDelegateTypeOnMethod class (method-level)
        // Line 91 (0-based: 90), parameter name "delegate" starts at column 45, ends at column 53
        Diagnostic invalidTypeDiagnostic3 = d(90, 45, 53,
                "The delegate type 'Logger' must implement or extend all decorated types.",
                DiagnosticSeverity.Error,
                "jakarta-cdi",
                "InvalidDecoratorDelegateTypeAssignability");

        assertJavaDiagnostics(diagnosticsParams, utils, invalidTypeDiagnostic1, invalidTypeDiagnostic2, invalidTypeDiagnostic3);
    }

}
