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
package io.openliberty.tools.intellij.lsp4jakarta.it.interceptor;

import java.io.File;
import java.util.Arrays;

import com.google.gson.Gson;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.lsp4jakarta.it.core.BaseJakartaTest;
import io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert;
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

@RunWith(JUnit4.class)
public class JakartaInterceptorTest extends BaseJakartaTest {

    @Test
    public void invalidInterceptorTest() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/interceptor/InvalidInterceptor.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostics
        Diagnostic d1 = JakartaForJavaAssert.d(5, 13, 31,
                "Missing Public NoArgsConstructor. Class InvalidInterceptor is of Interceptor type, but does not declare a public no-argument constructor.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "RemoveInterceptorAnnotationOnNoArgsConstructor");
        Diagnostic d2 = JakartaForJavaAssert.d(32, 14, 37,
                "Missing Public NoArgsConstructor. Class InnerInvalidInterceptor is of Interceptor type, but does not declare a public no-argument constructor.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "RemoveInterceptorAnnotationOnNoArgsConstructor");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d1, d2);
    }
    @Test
    public void invalidAbstractInterceptorTest() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/interceptor/InvalidAbstractInterceptor.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostics
        Diagnostic d1 = JakartaForJavaAssert.d(5, 22, 40,
                "The class InvalidInterceptor should not contain the abstract modifier. If it contains the abstract modifier, the class should not be annotated with @Interceptor.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "RemoveInterceptorAnnotationOnAbstractClass");
    }

    @Test
    public void invalidInterceptorMethodProceedTest() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/interceptor/InvalidInterceptorMethodsProceed.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostics
        Diagnostic aroundInvokeInvalidProceed = JakartaForJavaAssert.d(17, 18, 38,
                "Interceptor methods must always call the InvocationContext.proceed method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodsProceedMissing");
        Diagnostic aroundConstructInvalidProceed = JakartaForJavaAssert.d(23, 18, 41,
                "Interceptor methods must always call the InvocationContext.proceed method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodsProceedMissing");
        Diagnostic aroundTimeoutInvalidProceed = JakartaForJavaAssert.d(29, 18, 39,
                "Interceptor methods must always call the InvocationContext.proceed method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodsProceedMissing");
        Diagnostic postConstructInvalidProceed = JakartaForJavaAssert.d(35, 16, 36,
                "Interceptor methods must always call the InvocationContext.proceed method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodsProceedMissing");
        Diagnostic preDestroyInvalidProceed = JakartaForJavaAssert.d(40, 16, 33,
                "Interceptor methods must always call the InvocationContext.proceed method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodsProceedMissing");
        Diagnostic aroundInvokeInvalidProceedChild = JakartaForJavaAssert.d(51, 22, 47,
                "Interceptor methods must always call the InvocationContext.proceed method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodsProceedMissing");
        Diagnostic postConstructInvalidProceedChild = JakartaForJavaAssert.d(67, 20, 45,
                "Interceptor methods must always call the InvocationContext.proceed method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodsProceedMissing");
        Diagnostic preDestroyInvalidProceedChild = JakartaForJavaAssert.d(72, 20, 42,
                "Interceptor methods must always call the InvocationContext.proceed method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodsProceedMissing");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, aroundInvokeInvalidProceed, aroundConstructInvalidProceed,
                aroundTimeoutInvalidProceed, postConstructInvalidProceed, preDestroyInvalidProceed, aroundInvokeInvalidProceedChild,
                postConstructInvalidProceedChild, preDestroyInvalidProceedChild);
    }

    @Test
    public void invalidAroundInvokeMethodModifiersTest() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/interceptor/InvalidAroundInvokeMethods.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostics for invalid method modifiers
        Diagnostic finalModifierDiagnostic = JakartaForJavaAssert.d(8, 24, 32,
                "AroundInvoke interceptor method must not be declared as a final method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodAnnotationOnFinalMethod",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundInvoke")));

        Diagnostic abstractModifierDiagnostic = JakartaForJavaAssert.d(13, 27, 38,
                "AroundInvoke interceptor method must not be declared as an abstract method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodAnnotationOnAbstractMethod",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundInvoke")));

        Diagnostic duplicateAroundInvoke1 = JakartaForJavaAssert.d(13, 27, 38,
                "Only one method with @AroundInvoke annotation is allowed per class. Multiple methods with the same interceptor annotation type are not permitted.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidMultipleInterceptorMethodsOfSameType",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundInvoke")));

        Diagnostic proceedDiagnostic = JakartaForJavaAssert.d(13, 27, 38,
                "Interceptor methods must always call the InvocationContext.proceed method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodsProceedMissing");

        Diagnostic staticModifierDiagnostic = JakartaForJavaAssert.d(16, 25, 34,
                "AroundInvoke interceptor method must not be declared as a static method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodAnnotationOnStaticMethod",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundInvoke")));

        Diagnostic invalidAbstractClassDiagnostic = JakartaForJavaAssert.d(5, 22, 48,
                "The class InvalidAroundInvokeMethods should not contain the abstract modifier. If it contains the abstract modifier, the class should not be annotated with @Interceptor.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "RemoveInterceptorAnnotationOnAbstractClass");

        // Test diagnostics for duplicate interceptor methods (skip first occurrence)
        Diagnostic duplicateAroundInvoke2 = JakartaForJavaAssert.d(16, 25, 34,
                "Only one method with @AroundInvoke annotation is allowed per class. Multiple methods with the same interceptor annotation type are not permitted.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidMultipleInterceptorMethodsOfSameType",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundInvoke")));

        Diagnostic duplicateAroundInvoke3 = JakartaForJavaAssert.d(21, 18, 26,
                "Only one method with @AroundInvoke annotation is allowed per class. Multiple methods with the same interceptor annotation type are not permitted.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidMultipleInterceptorMethodsOfSameType",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundInvoke")));

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, finalModifierDiagnostic, abstractModifierDiagnostic, duplicateAroundInvoke1,
                proceedDiagnostic, staticModifierDiagnostic, invalidAbstractClassDiagnostic, duplicateAroundInvoke2, duplicateAroundInvoke3);

        // Test code actions for final modifier
        JakartaJavaCodeActionParams codeActionParams1 = JakartaForJavaAssert.createCodeActionParams(uri, finalModifierDiagnostic);
        String newText = "package io.openliberty.sample.jakarta.interceptor;\n\nimport jakarta.interceptor.AroundInvoke;\nimport jakarta.interceptor.InvocationContext;\n\npublic abstract class InvalidAroundInvokeMethods {\n\n\tpublic final Object logFinal(InvocationContext ctx) throws Exception {\n        return ctx.proceed();\n    }\n\n    @AroundInvoke\n    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n\n    @AroundInvoke\n    public static Object logStatic(InvocationContext ctx) throws Exception {\n        return ctx.proceed();\n    }\n\n    @AroundInvoke\n    public Object logValid(InvocationContext ctx) throws Exception {\n        return ctx.proceed();\n    }\n}";

        String newText1 = "package io.openliberty.sample.jakarta.interceptor;\n" +
                "\n" +
                "import jakarta.interceptor.AroundInvoke;\n" +
                "import jakarta.interceptor.InvocationContext;\n" +
                "\n" +
                "public abstract class InvalidAroundInvokeMethods {\n" +
                "\n" +
                "\t@AroundInvoke\n" +
                "    public Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundInvoke\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n" +
                "\n" +
                "    @AroundInvoke\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundInvoke\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        TextEdit removeAroundInvokeOnFinalEdit = JakartaForJavaAssert.te(0, 0, 24, 1, newText);
        TextEdit removeFinalEdit = JakartaForJavaAssert.te(0, 0, 24, 1, newText1);
        CodeAction removeAroundInvokeOnFinalAction = JakartaForJavaAssert.ca(uri, "Remove @AroundInvoke", finalModifierDiagnostic, removeAroundInvokeOnFinalEdit);
        CodeAction removeFinalAction = JakartaForJavaAssert.ca(uri, "Remove the 'final' modifier from this method", finalModifierDiagnostic, removeFinalEdit);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams1, utils, removeAroundInvokeOnFinalAction, removeFinalAction);

        // Test code actions for abstract modifier
        JakartaJavaCodeActionParams codeActionParams2 = JakartaForJavaAssert.createCodeActionParams(uri, abstractModifierDiagnostic);
        String newText2 = "package io.openliberty.sample.jakarta.interceptor;\n" +
                "\n" +
                "import jakarta.interceptor.AroundInvoke;\n" +
                "import jakarta.interceptor.InvocationContext;\n" +
                "\n" +
                "public abstract class InvalidAroundInvokeMethods {\n" +
                "\n" +
                "\t@AroundInvoke\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n" +
                "\n" +
                "    @AroundInvoke\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundInvoke\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        String newText3 = "package io.openliberty.sample.jakarta.interceptor;\n" +
                "\n" +
                "import jakarta.interceptor.AroundInvoke;\n" +
                "import jakarta.interceptor.InvocationContext;\n" +
                "\n" +
                "public abstract class InvalidAroundInvokeMethods {\n" +
                "\n" +
                "\t@AroundInvoke\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundInvoke\n" +
                "    public Object logAbstract(InvocationContext ctx) throws Exception;\n" +
                "\n" +
                "    @AroundInvoke\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundInvoke\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        TextEdit removeAroundInvokeOnAbstractEdit = JakartaForJavaAssert.te(0, 0, 24, 1, newText2);
        TextEdit removeAbstractEdit = JakartaForJavaAssert.te(0, 0, 24, 1, newText3);
        CodeAction removeAroundInvokeOnAbstractAction = JakartaForJavaAssert.ca(uri, "Remove @AroundInvoke", abstractModifierDiagnostic, removeAroundInvokeOnAbstractEdit);
        CodeAction removeAbstractAction = JakartaForJavaAssert.ca(uri, "Remove the 'abstract' modifier from this method", abstractModifierDiagnostic, removeAbstractEdit);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams2, utils, removeAroundInvokeOnAbstractAction, removeAbstractAction);

        // Test code actions for static modifier
        JakartaJavaCodeActionParams codeActionParams3 = JakartaForJavaAssert.createCodeActionParams(uri, staticModifierDiagnostic);
        String newText4 = "package io.openliberty.sample.jakarta.interceptor;\n" +
                "\n" +
                "import jakarta.interceptor.AroundInvoke;\n" +
                "import jakarta.interceptor.InvocationContext;\n" +
                "\n" +
                "public abstract class InvalidAroundInvokeMethods {\n" +
                "\n" +
                "\t@AroundInvoke\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundInvoke\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n" +
                "\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundInvoke\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        String newText5 = "package io.openliberty.sample.jakarta.interceptor;\n" +
                "\n" +
                "import jakarta.interceptor.AroundInvoke;\n" +
                "import jakarta.interceptor.InvocationContext;\n" +
                "\n" +
                "public abstract class InvalidAroundInvokeMethods {\n" +
                "\n" +
                "\t@AroundInvoke\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundInvoke\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n" +
                "\n" +
                "    @AroundInvoke\n" +
                "    public Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundInvoke\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        TextEdit removeAroundInvokeOnStaticEdit = JakartaForJavaAssert.te(0, 0, 24, 1, newText4);
        TextEdit removeStaticEdit = JakartaForJavaAssert.te(0, 0, 24, 1, newText5);
        CodeAction removeAroundInvokeOnStaticAction = JakartaForJavaAssert.ca(uri, "Remove @AroundInvoke", staticModifierDiagnostic, removeAroundInvokeOnStaticEdit);
        CodeAction removeStaticAction = JakartaForJavaAssert.ca(uri, "Remove the 'static' modifier from this method", staticModifierDiagnostic, removeStaticEdit);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams3, utils, removeAroundInvokeOnStaticAction, removeStaticAction);
    }

    @Test
    public void invalidAroundConstructMethodModifiersTest() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/interceptor/InvalidAroundConstructMethods.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostics for invalid method modifiers
        Diagnostic finalModifierDiagnostic = JakartaForJavaAssert.d(8, 24, 32,
                "AroundConstruct interceptor method must not be declared as a final method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodAnnotationOnFinalMethod",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundConstruct")));

        Diagnostic abstractModifierDiagnostic = JakartaForJavaAssert.d(13, 27, 38,
                "AroundConstruct interceptor method must not be declared as an abstract method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodAnnotationOnAbstractMethod",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundConstruct")));

        Diagnostic duplicateAroundConstruct1 = JakartaForJavaAssert.d(13, 27, 38,
                "Only one method with @AroundConstruct annotation is allowed per class. Multiple methods with the same interceptor annotation type are not permitted.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidMultipleInterceptorMethodsOfSameType",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundConstruct")));

        Diagnostic proceedDiagnostics = JakartaForJavaAssert.d(13, 27, 38,
                "Interceptor methods must always call the InvocationContext.proceed method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodsProceedMissing");

        Diagnostic staticModifierDiagnostic = JakartaForJavaAssert.d(16, 25, 34,
                "AroundConstruct lifecycle callback interceptor method must not be declared as static except in an application client.",
                DiagnosticSeverity.Warning, "jakarta-interceptor", "InvalidInterceptorMethodAnnotationOnStaticMethod",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundConstruct")));

        Diagnostic invalidAbstractClassDiagnostics = JakartaForJavaAssert.d(5, 22, 51,
                "The class InvalidAroundConstructMethods should not contain the abstract modifier. If it contains the abstract modifier, the class should not be annotated with @Interceptor.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "RemoveInterceptorAnnotationOnAbstractClass");

        Diagnostic multipleFinalModifierDiagnostic = JakartaForJavaAssert.d(21, 31, 50,
                "AroundConstruct interceptor method must not be declared as a final method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodAnnotationOnFinalMethod",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundConstruct")));

        Diagnostic multipleStaticModifierDiagnostic = JakartaForJavaAssert.d(21, 31, 50,
                "AroundConstruct lifecycle callback interceptor method must not be declared as static except in an application client.",
                DiagnosticSeverity.Warning, "jakarta-interceptor", "InvalidInterceptorMethodAnnotationOnStaticMethod",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundConstruct")));

        // Test diagnostics for duplicate interceptor methods (skip first occurrence)
        Diagnostic duplicateAroundConstruct2 = JakartaForJavaAssert.d(16, 25, 34,
                "Only one method with @AroundConstruct annotation is allowed per class. Multiple methods with the same interceptor annotation type are not permitted.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidMultipleInterceptorMethodsOfSameType",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundConstruct")));

        Diagnostic duplicateAroundConstruct3 = JakartaForJavaAssert.d(21, 31, 50,
                "Only one method with @AroundConstruct annotation is allowed per class. Multiple methods with the same interceptor annotation type are not permitted.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidMultipleInterceptorMethodsOfSameType",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundConstruct")));

        Diagnostic duplicateAroundConstruct4 = JakartaForJavaAssert.d(26, 18, 26,
                "Only one method with @AroundConstruct annotation is allowed per class. Multiple methods with the same interceptor annotation type are not permitted.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidMultipleInterceptorMethodsOfSameType",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundConstruct")));

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, finalModifierDiagnostic, abstractModifierDiagnostic, duplicateAroundConstruct1,
                proceedDiagnostics, staticModifierDiagnostic, invalidAbstractClassDiagnostics, multipleFinalModifierDiagnostic, multipleStaticModifierDiagnostic,
                duplicateAroundConstruct2, duplicateAroundConstruct3, duplicateAroundConstruct4);

        // Test code actions for final modifier
        JakartaJavaCodeActionParams codeActionParams1 = JakartaForJavaAssert.createCodeActionParams(uri, finalModifierDiagnostic);
        String newText6 = "package io.openliberty.sample.jakarta.interceptor;\n" +
                "\n" +
                "import jakarta.interceptor.AroundConstruct;\n" +
                "import jakarta.interceptor.InvocationContext;\n" +
                "\n" +
                "public abstract class InvalidAroundConstructMethods {\n" +
                "\n" +
                "\tpublic final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public static final Object logMulipleModifiers(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        String newText7 = "package io.openliberty.sample.jakarta.interceptor;\n" +
                "\n" +
                "import jakarta.interceptor.AroundConstruct;\n" +
                "import jakarta.interceptor.InvocationContext;\n" +
                "\n" +
                "public abstract class InvalidAroundConstructMethods {\n" +
                "\n" +
                "\t@AroundConstruct\n" +
                "    public Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public static final Object logMulipleModifiers(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        TextEdit removeAroundConstructOnFinalEdit = JakartaForJavaAssert.te(0, 0, 29, 1, newText6);
        TextEdit removeFinalEdit = JakartaForJavaAssert.te(0, 0, 29, 1, newText7);
        CodeAction removeAroundConstructOnFinalAction = JakartaForJavaAssert.ca(uri, "Remove @AroundConstruct", finalModifierDiagnostic, removeAroundConstructOnFinalEdit);
        CodeAction removeFinalAction = JakartaForJavaAssert.ca(uri, "Remove the 'final' modifier from this method", finalModifierDiagnostic, removeFinalEdit);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams1, utils, removeAroundConstructOnFinalAction, removeFinalAction);

        // Test code actions for abstract modifier
        JakartaJavaCodeActionParams codeActionParams2 = JakartaForJavaAssert.createCodeActionParams(uri, abstractModifierDiagnostic);
        String newText8 = "package io.openliberty.sample.jakarta.interceptor;\n" +
                "\n" +
                "import jakarta.interceptor.AroundConstruct;\n" +
                "import jakarta.interceptor.InvocationContext;\n" +
                "\n" +
                "public abstract class InvalidAroundConstructMethods {\n" +
                "\n" +
                "\t@AroundConstruct\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public static final Object logMulipleModifiers(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        String newText9 = "package io.openliberty.sample.jakarta.interceptor;\n" +
                "\n" +
                "import jakarta.interceptor.AroundConstruct;\n" +
                "import jakarta.interceptor.InvocationContext;\n" +
                "\n" +
                "public abstract class InvalidAroundConstructMethods {\n" +
                "\n" +
                "\t@AroundConstruct\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public Object logAbstract(InvocationContext ctx) throws Exception;\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public static final Object logMulipleModifiers(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        TextEdit removeAroundConstructOnAbstractEdit = JakartaForJavaAssert.te(0, 0, 29, 1, newText8);
        TextEdit removeAbstractEdit = JakartaForJavaAssert.te(0, 0, 29, 1, newText9);
        CodeAction removeAroundConstructOnAbstractAction = JakartaForJavaAssert.ca(uri, "Remove @AroundConstruct", abstractModifierDiagnostic, removeAroundConstructOnAbstractEdit);
        CodeAction removeAbstractAction = JakartaForJavaAssert.ca(uri, "Remove the 'abstract' modifier from this method", abstractModifierDiagnostic, removeAbstractEdit);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams2, utils, removeAroundConstructOnAbstractAction, removeAbstractAction);

        // Test code actions for static modifier
        JakartaJavaCodeActionParams codeActionParams3 = JakartaForJavaAssert.createCodeActionParams(uri, staticModifierDiagnostic);
        String newText10 = "package io.openliberty.sample.jakarta.interceptor;\n" +
                "\n" +
                "import jakarta.interceptor.AroundConstruct;\n" +
                "import jakarta.interceptor.InvocationContext;\n" +
                "\n" +
                "public abstract class InvalidAroundConstructMethods {\n" +
                "\n" +
                "\t@AroundConstruct\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n" +
                "\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public static final Object logMulipleModifiers(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        String newText11 = "package io.openliberty.sample.jakarta.interceptor;\n" +
                "\n" +
                "import jakarta.interceptor.AroundConstruct;\n" +
                "import jakarta.interceptor.InvocationContext;\n" +
                "\n" +
                "public abstract class InvalidAroundConstructMethods {\n" +
                "\n" +
                "\t@AroundConstruct\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public static final Object logMulipleModifiers(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        TextEdit removeAroundConstructOnStaticEdit = JakartaForJavaAssert.te(0, 0, 29, 1, newText10);
        TextEdit removeStaticEdit = JakartaForJavaAssert.te(0, 0, 29, 1, newText11);
        CodeAction removeAroundConstructOnStaticAction = JakartaForJavaAssert.ca(uri, "Remove @AroundConstruct", staticModifierDiagnostic, removeAroundConstructOnStaticEdit);
        CodeAction removeStaticAction = JakartaForJavaAssert.ca(uri, "Remove the 'static' modifier from this method", staticModifierDiagnostic, removeStaticEdit);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams3, utils, removeAroundConstructOnStaticAction, removeStaticAction);

        // Test code actions for multiple modifiers
        JakartaJavaCodeActionParams codeActionParams4 = JakartaForJavaAssert.createCodeActionParams(uri, multipleStaticModifierDiagnostic);
        String newText12 = "package io.openliberty.sample.jakarta.interceptor;\n" +
                "\n" +
                "import jakarta.interceptor.AroundConstruct;\n" +
                "import jakarta.interceptor.InvocationContext;\n" +
                "\n" +
                "public abstract class InvalidAroundConstructMethods {\n" +
                "\n" +
                "\t@AroundConstruct\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    public static final Object logMulipleModifiers(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";
        String newText13 = "package io.openliberty.sample.jakarta.interceptor;\n" +
                "\n" +
                "import jakarta.interceptor.AroundConstruct;\n" +
                "import jakarta.interceptor.InvocationContext;\n" +
                "\n" +
                "public abstract class InvalidAroundConstructMethods {\n" +
                "\n" +
                "\t@AroundConstruct\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public  final Object logMulipleModifiers(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";
        TextEdit removeAroundConstructOnMultipleModifiersEdit = JakartaForJavaAssert.te(0, 0, 29, 1, newText12);
        TextEdit removeStaticOnMultipleModifiersEdit = JakartaForJavaAssert.te(0, 0, 29, 1, newText13);
        CodeAction removeAroundConstructOnMultipleModifiersAction = JakartaForJavaAssert.ca(uri, "Remove @AroundConstruct", multipleStaticModifierDiagnostic, removeAroundConstructOnMultipleModifiersEdit);
        CodeAction removeStaticOnMultipleModifiersAction = JakartaForJavaAssert.ca(uri, "Remove the 'static' modifier from this method", multipleStaticModifierDiagnostic, removeStaticOnMultipleModifiersEdit);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams4, utils, removeAroundConstructOnMultipleModifiersAction, removeStaticOnMultipleModifiersAction);

        JakartaJavaCodeActionParams codeActionParams5 = JakartaForJavaAssert.createCodeActionParams(uri, multipleFinalModifierDiagnostic);
        String newText14 = "package io.openliberty.sample.jakarta.interceptor;\n" +
                "\n" +
                "import jakarta.interceptor.AroundConstruct;\n" +
                "import jakarta.interceptor.InvocationContext;\n" +
                "\n" +
                "public abstract class InvalidAroundConstructMethods {\n" +
                "\n" +
                "\t@AroundConstruct\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    public static final Object logMulipleModifiers(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";
        String newText15 = "package io.openliberty.sample.jakarta.interceptor;\n" +
                "\n" +
                "import jakarta.interceptor.AroundConstruct;\n" +
                "import jakarta.interceptor.InvocationContext;\n" +
                "\n" +
                "public abstract class InvalidAroundConstructMethods {\n" +
                "\n" +
                "\t@AroundConstruct\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public static Object logMulipleModifiers(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundConstruct\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";
        TextEdit removeAroundConstructOnMultipleFinalEdit = JakartaForJavaAssert.te(0, 0, 29, 1, newText14);
        TextEdit removeFinalOnMultipleModifiersEdit = JakartaForJavaAssert.te(0, 0, 29, 1, newText15);
        CodeAction removeAroundConstructOnMultipleFinalAction = JakartaForJavaAssert.ca(uri, "Remove @AroundConstruct", multipleFinalModifierDiagnostic, removeAroundConstructOnMultipleFinalEdit);
        CodeAction removeFinalOnMultipleModifiersAction = JakartaForJavaAssert.ca(uri, "Remove the 'final' modifier from this method", multipleFinalModifierDiagnostic, removeFinalOnMultipleModifiersEdit);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams5, utils, removeAroundConstructOnMultipleFinalAction, removeFinalOnMultipleModifiersAction);
    }

    @Test
    public void invalidAroundTimeoutMethodModifiersTest() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/interceptor/InvalidAroundTimeoutMethods.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostics for invalid method modifiers
        Diagnostic finalModifierDiagnostic = JakartaForJavaAssert.d(8, 24, 32,
                "AroundTimeout interceptor method must not be declared as a final method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodAnnotationOnFinalMethod",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundTimeout")));

        Diagnostic abstractModifierDiagnostic = JakartaForJavaAssert.d(13, 27, 38,
                "AroundTimeout interceptor method must not be declared as an abstract method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodAnnotationOnAbstractMethod",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundTimeout")));

        Diagnostic duplicateAroundTimeout1 = JakartaForJavaAssert.d(13, 27, 38,
                "Only one method with @AroundTimeout annotation is allowed per class. Multiple methods with the same interceptor annotation type are not permitted.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidMultipleInterceptorMethodsOfSameType",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundTimeout")));

        Diagnostic proceedDiagnostic = JakartaForJavaAssert.d(13, 27, 38,
                "Interceptor methods must always call the InvocationContext.proceed method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodsProceedMissing");

        Diagnostic staticModifierDiagnostic = JakartaForJavaAssert.d(16, 25, 34,
                "AroundTimeout interceptor method must not be declared as a static method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodAnnotationOnStaticMethod",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundTimeout")));

        Diagnostic invalidAbstractClassDiagnostic = JakartaForJavaAssert.d(5, 22, 49,
                "The class InvalidAroundTimeoutMethods should not contain the abstract modifier. If it contains the abstract modifier, the class should not be annotated with @Interceptor.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "RemoveInterceptorAnnotationOnAbstractClass");

        // Test diagnostics for duplicate interceptor methods (skip first occurrence)
        Diagnostic duplicateAroundTimeout2 = JakartaForJavaAssert.d(16, 25, 34,
                "Only one method with @AroundTimeout annotation is allowed per class. Multiple methods with the same interceptor annotation type are not permitted.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidMultipleInterceptorMethodsOfSameType",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundTimeout")));

        Diagnostic duplicateAroundTimeout3 = JakartaForJavaAssert.d(21, 18, 26,
                "Only one method with @AroundTimeout annotation is allowed per class. Multiple methods with the same interceptor annotation type are not permitted.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidMultipleInterceptorMethodsOfSameType",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundTimeout")));

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, finalModifierDiagnostic, abstractModifierDiagnostic, duplicateAroundTimeout1, proceedDiagnostic,
                staticModifierDiagnostic, invalidAbstractClassDiagnostic, duplicateAroundTimeout2, duplicateAroundTimeout3);

        // Test code actions for final modifier
        JakartaJavaCodeActionParams codeActionParams1 = JakartaForJavaAssert.createCodeActionParams(uri, finalModifierDiagnostic);
        String newText12 = "package io.openliberty.sample.jakarta.interceptor;\n" +
                "\n" +
                "import jakarta.interceptor.AroundTimeout;\n" +
                "import jakarta.interceptor.InvocationContext;\n" +
                "\n" +
                "public abstract class InvalidAroundTimeoutMethods {\n" +
                "\n" +
                "\tpublic final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundTimeout\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n" +
                "\n" +
                "    @AroundTimeout\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundTimeout\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        String newText13 = "package io.openliberty.sample.jakarta.interceptor;\n" +
                "\n" +
                "import jakarta.interceptor.AroundTimeout;\n" +
                "import jakarta.interceptor.InvocationContext;\n" +
                "\n" +
                "public abstract class InvalidAroundTimeoutMethods {\n" +
                "\n" +
                "\t@AroundTimeout\n" +
                "    public Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundTimeout\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n" +
                "\n" +
                "    @AroundTimeout\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundTimeout\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        TextEdit removeAroundTimeoutOnFinalEdit = JakartaForJavaAssert.te(0, 0, 24, 1, newText12);
        TextEdit removeFinalEdit = JakartaForJavaAssert.te(0, 0, 24, 1, newText13);
        CodeAction removeAroundTimeoutOnFinalAction = JakartaForJavaAssert.ca(uri, "Remove @AroundTimeout", finalModifierDiagnostic, removeAroundTimeoutOnFinalEdit);
        CodeAction removeFinalAction = JakartaForJavaAssert.ca(uri, "Remove the 'final' modifier from this method", finalModifierDiagnostic, removeFinalEdit);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams1, utils, removeAroundTimeoutOnFinalAction, removeFinalAction);

        // Test code actions for abstract modifier
        JakartaJavaCodeActionParams codeActionParams2 = JakartaForJavaAssert.createCodeActionParams(uri, abstractModifierDiagnostic);
        String newText14 = "package io.openliberty.sample.jakarta.interceptor;\n" +
                "\n" +
                "import jakarta.interceptor.AroundTimeout;\n" +
                "import jakarta.interceptor.InvocationContext;\n" +
                "\n" +
                "public abstract class InvalidAroundTimeoutMethods {\n" +
                "\n" +
                "\t@AroundTimeout\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n" +
                "\n" +
                "    @AroundTimeout\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundTimeout\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        String newText15 = "package io.openliberty.sample.jakarta.interceptor;\n" +
                "\n" +
                "import jakarta.interceptor.AroundTimeout;\n" +
                "import jakarta.interceptor.InvocationContext;\n" +
                "\n" +
                "public abstract class InvalidAroundTimeoutMethods {\n" +
                "\n" +
                "\t@AroundTimeout\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundTimeout\n" +
                "    public Object logAbstract(InvocationContext ctx) throws Exception;\n" +
                "\n" +
                "    @AroundTimeout\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundTimeout\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        TextEdit removeAroundTimeoutOnAbstractEdit = JakartaForJavaAssert.te(0, 0, 24, 1, newText14);
        TextEdit removeAbstractEdit = JakartaForJavaAssert.te(0, 0, 24, 1, newText15);
        CodeAction removeAroundTimeoutOnAbstractAction = JakartaForJavaAssert.ca(uri, "Remove @AroundTimeout", abstractModifierDiagnostic, removeAroundTimeoutOnAbstractEdit);
        CodeAction removeAbstractAction = JakartaForJavaAssert.ca(uri, "Remove the 'abstract' modifier from this method", abstractModifierDiagnostic, removeAbstractEdit);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams2, utils, removeAroundTimeoutOnAbstractAction, removeAbstractAction);

        // Test code actions for static modifier
        JakartaJavaCodeActionParams codeActionParams3 = JakartaForJavaAssert.createCodeActionParams(uri, staticModifierDiagnostic);
        String newText16 = "package io.openliberty.sample.jakarta.interceptor;\n" +
                "\n" +
                "import jakarta.interceptor.AroundTimeout;\n" +
                "import jakarta.interceptor.InvocationContext;\n" +
                "\n" +
                "public abstract class InvalidAroundTimeoutMethods {\n" +
                "\n" +
                "\t@AroundTimeout\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundTimeout\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n" +
                "\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundTimeout\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        String newText17 = "package io.openliberty.sample.jakarta.interceptor;\n" +
                "\n" +
                "import jakarta.interceptor.AroundTimeout;\n" +
                "import jakarta.interceptor.InvocationContext;\n" +
                "\n" +
                "public abstract class InvalidAroundTimeoutMethods {\n" +
                "\n" +
                "\t@AroundTimeout\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundTimeout\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n" +
                "\n" +
                "    @AroundTimeout\n" +
                "    public Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "\n" +
                "    @AroundTimeout\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        TextEdit removeAroundTimeoutOnStaticEdit = JakartaForJavaAssert.te(0, 0, 24, 1, newText16);
        TextEdit removeStaticEdit = JakartaForJavaAssert.te(0, 0, 24, 1, newText17);
        CodeAction removeAroundTimeoutOnStaticAction = JakartaForJavaAssert.ca(uri, "Remove @AroundTimeout", staticModifierDiagnostic, removeAroundTimeoutOnStaticEdit);
        CodeAction removeStaticAction = JakartaForJavaAssert.ca(uri, "Remove the 'static' modifier from this method", staticModifierDiagnostic, removeStaticEdit);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams3, utils, removeAroundTimeoutOnStaticAction, removeStaticAction);
    }

    @Test
    public void invalidPostConstructMethodModifiersTest() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/interceptor/InvalidPostConstructMethods.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostics for abstract class
        Diagnostic abstractClassDiagnostic = JakartaForJavaAssert.d(7, 22, 49,
                "The class InvalidPostConstructMethods should not contain the abstract modifier. If it contains the abstract modifier, the class should not be annotated with @Interceptor.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "RemoveInterceptorAnnotationOnAbstractClass");

        // Test diagnostics for invalid method modifiers
        Diagnostic finalModifierDiagnostic = JakartaForJavaAssert.d(10, 24, 32,
                "PostConstruct interceptor method must not be declared as a final method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodAnnotationOnFinalMethod",
                new Gson().toJsonTree(Arrays.asList("jakarta.annotation.PostConstruct")));

        Diagnostic abstractModifierDiagnostic = JakartaForJavaAssert.d(15, 27, 38,
                "PostConstruct interceptor method must not be declared as an abstract method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodAnnotationOnAbstractMethod",
                new Gson().toJsonTree(Arrays.asList("jakarta.annotation.PostConstruct")));

        Diagnostic duplicatePostConstruct1 = JakartaForJavaAssert.d(15, 27, 38,
                "Only one method with @PostConstruct annotation is allowed per class. Multiple methods with the same interceptor annotation type are not permitted.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidMultipleInterceptorMethodsOfSameType",
                new Gson().toJsonTree(Arrays.asList("jakarta.annotation.PostConstruct")));

        Diagnostic proceedDiagnostic = JakartaForJavaAssert.d(15, 27, 38,
                "Interceptor methods must always call the InvocationContext.proceed method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodsProceedMissing");

        Diagnostic staticModifierDiagnostic = JakartaForJavaAssert.d(18, 25, 34,
                "PostConstruct lifecycle callback interceptor method must not be declared as static except in an application client.",
                DiagnosticSeverity.Warning, "jakarta-interceptor", "InvalidInterceptorMethodAnnotationOnStaticMethod",
                new Gson().toJsonTree(Arrays.asList("jakarta.annotation.PostConstruct")));

        // Test diagnostics for duplicate interceptor methods (skip first occurrence)
        Diagnostic duplicatePostConstruct2 = JakartaForJavaAssert.d(18, 25, 34,
                "Only one method with @PostConstruct annotation is allowed per class. Multiple methods with the same interceptor annotation type are not permitted.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidMultipleInterceptorMethodsOfSameType",
                new Gson().toJsonTree(Arrays.asList("jakarta.annotation.PostConstruct")));

        Diagnostic duplicatePostConstruct3 = JakartaForJavaAssert.d(23, 18, 26,
                "Only one method with @PostConstruct annotation is allowed per class. Multiple methods with the same interceptor annotation type are not permitted.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidMultipleInterceptorMethodsOfSameType",
                new Gson().toJsonTree(Arrays.asList("jakarta.annotation.PostConstruct")));

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, abstractClassDiagnostic, finalModifierDiagnostic,
                abstractModifierDiagnostic, duplicatePostConstruct1, proceedDiagnostic, staticModifierDiagnostic,
                duplicatePostConstruct2, duplicatePostConstruct3);

        // Test code actions for final modifier
        JakartaJavaCodeActionParams codeActionParams1 = JakartaForJavaAssert.createCodeActionParams(uri, finalModifierDiagnostic);
        String newText18 = "package io.openliberty.sample.jakarta.interceptor;\n\n" +
                "import jakarta.annotation.PostConstruct;\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.interceptor.InvocationContext;\n\n" +
                "@Interceptor\n" +
                "public abstract class InvalidPostConstructMethods {\n\n" +
                "	public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n	@PostConstruct\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n\n" +
                "	@PostConstruct\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n" +
                "	@PostConstruct\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        String newText19 = "package io.openliberty.sample.jakarta.interceptor;\n\n" +
                "import jakarta.annotation.PostConstruct;\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.interceptor.InvocationContext;\n\n" +
                "@Interceptor\npublic abstract class InvalidPostConstructMethods {\n\n" +
                "	@PostConstruct\n" +
                "    public Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n" +
                "	@PostConstruct\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n\n" +
                "	@PostConstruct\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n" +
                "	@PostConstruct\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        TextEdit removePostConstructOnFinalEdit = JakartaForJavaAssert.te(0, 0, 26, 1, newText18);
        TextEdit removeFinalEdit = JakartaForJavaAssert.te(0, 0, 26, 1, newText19);
        CodeAction removePostConstructOnFinalAction = JakartaForJavaAssert.ca(uri, "Remove @PostConstruct", finalModifierDiagnostic, removePostConstructOnFinalEdit);
        CodeAction removeFinalAction = JakartaForJavaAssert.ca(uri, "Remove the 'final' modifier from this method", finalModifierDiagnostic, removeFinalEdit);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams1, utils, removePostConstructOnFinalAction, removeFinalAction);

        // Test code actions for abstract modifier
        JakartaJavaCodeActionParams codeActionParams2 = JakartaForJavaAssert.createCodeActionParams(uri, abstractModifierDiagnostic);
        String newText20 = "package io.openliberty.sample.jakarta.interceptor;\n\n" +
                "import jakarta.annotation.PostConstruct;\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.interceptor.InvocationContext;\n\n" +
                "@Interceptor\n" +
                "public abstract class InvalidPostConstructMethods {\n\n" +
                "	@PostConstruct\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n" +
                "\tpublic abstract Object logAbstract(InvocationContext ctx) throws Exception;\n\n" +
                "	@PostConstruct\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n" +
                "	@PostConstruct\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n}";

        String newText21 = "package io.openliberty.sample.jakarta.interceptor;\n\n" +
                "import jakarta.annotation.PostConstruct;\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.interceptor.InvocationContext;\n\n" +
                "@Interceptor\n" +
                "public abstract class InvalidPostConstructMethods {\n\n" +
                "	@PostConstruct\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n" +
                "	@PostConstruct\n" +
                "    public Object logAbstract(InvocationContext ctx) throws Exception;\n\n" +
                "	@PostConstruct\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n" +
                "	@PostConstruct\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        TextEdit removePostConstructOnAbstractEdit = JakartaForJavaAssert.te(0, 0, 26, 1, newText20);
        TextEdit removeAbstractEdit = JakartaForJavaAssert.te(0, 0, 26, 1, newText21);
        CodeAction removePostConstructOnAbstractAction = JakartaForJavaAssert.ca(uri, "Remove @PostConstruct", abstractModifierDiagnostic, removePostConstructOnAbstractEdit);
        CodeAction removeAbstractAction = JakartaForJavaAssert.ca(uri, "Remove the 'abstract' modifier from this method", abstractModifierDiagnostic, removeAbstractEdit);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams2, utils, removePostConstructOnAbstractAction, removeAbstractAction);

        // Test code actions for static modifier
        JakartaJavaCodeActionParams codeActionParams3 = JakartaForJavaAssert.createCodeActionParams(uri, staticModifierDiagnostic);
        String newText22 = "package io.openliberty.sample.jakarta.interceptor;\n\n" +
                "import jakarta.annotation.PostConstruct;\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.interceptor.InvocationContext;\n\n" +
                "@Interceptor\n" +
                "public abstract class InvalidPostConstructMethods {\n\n" +
                "	@PostConstruct\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n" +
                "	@PostConstruct\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n\n" +
                "	public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n" +
                "	@PostConstruct\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        String newText23 = "package io.openliberty.sample.jakarta.interceptor;\n" +
                "\n" +
                "import jakarta.annotation.PostConstruct;\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.interceptor.InvocationContext;\n" +
                "\n" +
                "@Interceptor\n" +
                "public abstract class InvalidPostConstructMethods {\n" +
                "\n" +
                "\t@PostConstruct\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n" +
                "\t@PostConstruct\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n\n" +
                "\t@PostConstruct\n" +
                "    public Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n" +
                "\t@PostConstruct\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        TextEdit removePostConstructOnStaticEdit = JakartaForJavaAssert.te(0, 0, 26, 1, newText22);
        TextEdit removeStaticEdit = JakartaForJavaAssert.te(0, 0, 26, 1, newText23);
        CodeAction removePostConstructOnStaticAction = JakartaForJavaAssert.ca(uri, "Remove @PostConstruct", staticModifierDiagnostic, removePostConstructOnStaticEdit);
        CodeAction removeStaticAction = JakartaForJavaAssert.ca(uri, "Remove the 'static' modifier from this method", staticModifierDiagnostic, removeStaticEdit);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams3, utils, removePostConstructOnStaticAction, removeStaticAction);
    }

    @Test
    public void invalidPreDestroyMethodModifiersTest() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/interceptor/InvalidPreDestroyMethods.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostics for abstract class
        Diagnostic abstractClassDiagnostic = JakartaForJavaAssert.d(7, 22, 46,
                "The class InvalidPreDestroyMethods should not contain the abstract modifier. If it contains the abstract modifier, the class should not be annotated with @Interceptor.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "RemoveInterceptorAnnotationOnAbstractClass");

        // Test diagnostics for invalid method modifiers
        Diagnostic finalModifierDiagnostic = JakartaForJavaAssert.d(10, 24, 32,
                "PreDestroy interceptor method must not be declared as a final method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodAnnotationOnFinalMethod",
                new Gson().toJsonTree(Arrays.asList("jakarta.annotation.PreDestroy")));

        Diagnostic abstractModifierDiagnostic = JakartaForJavaAssert.d(15, 27, 38,
                "PreDestroy interceptor method must not be declared as an abstract method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodAnnotationOnAbstractMethod",
                new Gson().toJsonTree(Arrays.asList("jakarta.annotation.PreDestroy")));

        Diagnostic duplicatePreDestroy1 = JakartaForJavaAssert.d(15, 27, 38,
                "Only one method with @PreDestroy annotation is allowed per class. Multiple methods with the same interceptor annotation type are not permitted.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidMultipleInterceptorMethodsOfSameType",
                new Gson().toJsonTree(Arrays.asList("jakarta.annotation.PreDestroy")));

        Diagnostic proceedDiagnostic = JakartaForJavaAssert.d(15, 27, 38,
                "Interceptor methods must always call the InvocationContext.proceed method.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidInterceptorMethodsProceedMissing");

        Diagnostic staticModifierDiagnostic = JakartaForJavaAssert.d(18, 25, 34,
                "PreDestroy lifecycle callback interceptor method must not be declared as static except in an application client.",
                DiagnosticSeverity.Warning, "jakarta-interceptor", "InvalidInterceptorMethodAnnotationOnStaticMethod",
                new Gson().toJsonTree(Arrays.asList("jakarta.annotation.PreDestroy")));

        // Test diagnostics for duplicate interceptor methods (skip first occurrence)
        Diagnostic duplicatePreDestroy2 = JakartaForJavaAssert.d(18, 25, 34,
                "Only one method with @PreDestroy annotation is allowed per class. Multiple methods with the same interceptor annotation type are not permitted.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidMultipleInterceptorMethodsOfSameType",
                new Gson().toJsonTree(Arrays.asList("jakarta.annotation.PreDestroy")));

        Diagnostic duplicatePreDestroy3 = JakartaForJavaAssert.d(23, 18, 26,
                "Only one method with @PreDestroy annotation is allowed per class. Multiple methods with the same interceptor annotation type are not permitted.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidMultipleInterceptorMethodsOfSameType",
                new Gson().toJsonTree(Arrays.asList("jakarta.annotation.PreDestroy")));

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, abstractClassDiagnostic, finalModifierDiagnostic,
                abstractModifierDiagnostic, duplicatePreDestroy1, proceedDiagnostic, staticModifierDiagnostic,
                duplicatePreDestroy2, duplicatePreDestroy3);

        // Test code actions for final modifier
        JakartaJavaCodeActionParams codeActionParams1 = JakartaForJavaAssert.createCodeActionParams(uri, finalModifierDiagnostic);
        String newText24 = "package io.openliberty.sample.jakarta.interceptor;\n\n" +
                "import jakarta.annotation.PreDestroy;\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.interceptor.InvocationContext;\n\n" +
                "@Interceptor\n" +
                "public abstract class InvalidPreDestroyMethods {\n\n" +
                "	public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n" +
                "	@PreDestroy\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n\n" +
                "	@PreDestroy\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n" +
                "	@PreDestroy\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        String newText25 = "package io.openliberty.sample.jakarta.interceptor;\n\n" +
                "import jakarta.annotation.PreDestroy;\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.interceptor.InvocationContext;\n\n" +
                "@Interceptor\n" +
                "public abstract class InvalidPreDestroyMethods {\n\n" +
                "	@PreDestroy\n" +
                "    public Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n" +
                "	@PreDestroy\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n\n" +
                "	@PreDestroy\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n" +
                "	@PreDestroy\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        TextEdit removePreDestroyOnFinalEdit = JakartaForJavaAssert.te(0, 0, 26, 1, newText24);
        TextEdit removeFinalEdit = JakartaForJavaAssert.te(0, 0, 26, 1, newText25);
        CodeAction removePreDestroyOnFinalAction = JakartaForJavaAssert.ca(uri, "Remove @PreDestroy", finalModifierDiagnostic, removePreDestroyOnFinalEdit);
        CodeAction removeFinalAction = JakartaForJavaAssert.ca(uri, "Remove the 'final' modifier from this method", finalModifierDiagnostic, removeFinalEdit);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams1, utils, removePreDestroyOnFinalAction, removeFinalAction);

        // Test code actions for abstract modifier
        JakartaJavaCodeActionParams codeActionParams2 = JakartaForJavaAssert.createCodeActionParams(uri, abstractModifierDiagnostic);
        String newText26 = "package io.openliberty.sample.jakarta.interceptor;\n\n" +
                "import jakarta.annotation.PreDestroy;\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.interceptor.InvocationContext;\n\n" +
                "@Interceptor\n" +
                "public abstract class InvalidPreDestroyMethods {\n\n" +
                "	@PreDestroy\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n" +
                "\tpublic abstract Object logAbstract(InvocationContext ctx) throws Exception;\n\n" +
                "	@PreDestroy\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n" +
                "	@PreDestroy\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        String newText27 = "package io.openliberty.sample.jakarta.interceptor;\n\n" +
                "import jakarta.annotation.PreDestroy;\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.interceptor.InvocationContext;\n\n" +
                "@Interceptor\n" +
                "public abstract class InvalidPreDestroyMethods {\n\n" +
                "	@PreDestroy\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n" +
                "	@PreDestroy\n" +
                "    public Object logAbstract(InvocationContext ctx) throws Exception;\n\n" +
                "	@PreDestroy\n" +
                "    public static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n" +
                "	@PreDestroy\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        TextEdit removePreDestroyOnAbstractEdit = JakartaForJavaAssert.te(0, 0, 26, 1, newText26);
        TextEdit removeAbstractEdit = JakartaForJavaAssert.te(0, 0, 26, 1, newText27);
        CodeAction removePreDestroyOnAbstractAction = JakartaForJavaAssert.ca(uri, "Remove @PreDestroy", abstractModifierDiagnostic, removePreDestroyOnAbstractEdit);
        CodeAction removeAbstractAction = JakartaForJavaAssert.ca(uri, "Remove the 'abstract' modifier from this method", abstractModifierDiagnostic, removeAbstractEdit);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams2, utils, removePreDestroyOnAbstractAction, removeAbstractAction);

        // Test code actions for static modifier
        JakartaJavaCodeActionParams codeActionParams3 = JakartaForJavaAssert.createCodeActionParams(uri, staticModifierDiagnostic);
        String newText28 = "package io.openliberty.sample.jakarta.interceptor;\n\n" +
                "import jakarta.annotation.PreDestroy;\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.interceptor.InvocationContext;\n\n" +
                "@Interceptor\n" +
                "public abstract class InvalidPreDestroyMethods {\n\n" +
                "	@PreDestroy\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n" +
                "	@PreDestroy\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n\n" +
                "\tpublic static Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n" +
                "	@PreDestroy\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        String newText29 = "package io.openliberty.sample.jakarta.interceptor;\n\n" +
                "import jakarta.annotation.PreDestroy;\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.interceptor.InvocationContext;\n\n" +
                "@Interceptor\n" +
                "public abstract class InvalidPreDestroyMethods {\n\n" +
                "	@PreDestroy\n" +
                "    public final Object logFinal(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n" +
                "	@PreDestroy\n" +
                "    public abstract Object logAbstract(InvocationContext ctx) throws Exception;\n\n" +
                "	@PreDestroy\n" +
                "    public Object logStatic(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n\n" +
                "	@PreDestroy\n" +
                "    public Object logValid(InvocationContext ctx) throws Exception {\n" +
                "        return ctx.proceed();\n" +
                "    }\n" +
                "}";

        TextEdit removePreDestroyOnStaticEdit = JakartaForJavaAssert.te(0, 0, 26, 1, newText28);
        TextEdit removeStaticEdit = JakartaForJavaAssert.te(0, 0, 26, 1, newText29);
        CodeAction removePreDestroyOnStaticAction = JakartaForJavaAssert.ca(uri, "Remove @PreDestroy", staticModifierDiagnostic, removePreDestroyOnStaticEdit);
        CodeAction removeStaticAction = JakartaForJavaAssert.ca(uri, "Remove the 'static' modifier from this method", staticModifierDiagnostic, removeStaticEdit);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams3, utils, removePreDestroyOnStaticAction, removeStaticAction);
    }
  
    @Test
    public void invalidNegativePriorityInterceptorTest() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/interceptor/InvalidNegativePriorityInterceptor.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostics for negative priority values
        // There are 3 diagnostics total:
        // 1. WARNING from annotations for line 8 (general negative priority warning)
        // 2. ERROR from interceptor for line 8 (interceptor-specific negative priority error)
        // 3. ERROR from interceptor for line 18 (inner class interceptor-specific negative priority error)
        Diagnostic negativePriorityWarningOuterClass = JakartaForJavaAssert.d(8, 0, 16,
                "Priority values should generally be non-negative, with negative values reserved for special meanings such as \"undefined\" or \"not specified\".",
                DiagnosticSeverity.Warning, "jakarta-annotations", "PriorityShouldBeNonNegative");
        Diagnostic negativePriorityErrorOuterClass = JakartaForJavaAssert.d(8, 0, 16,
                "Interceptor priority values must not be negative. Negative values are reserved for future use by the specification.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InterceptorNegativePriority");
        Diagnostic negativePriorityErrorInnerClass = JakartaForJavaAssert.d(18, 4, 19,
                "Interceptor priority values must not be negative. Negative values are reserved for future use by the specification.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InterceptorNegativePriority");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, negativePriorityWarningOuterClass,
                negativePriorityErrorOuterClass, negativePriorityErrorInnerClass);
    }

    @Test
    public void testMultipleInterceptorMethodsOfSameType() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/interceptor/MultipleInterceptorMethodsOfSameType.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostics for multiple @AroundInvoke methods
        Diagnostic aroundInvokeDuplicate1 = JakartaForJavaAssert.d(32, 18, 22,
                "Only one method with @AroundInvoke annotation is allowed per class. Multiple methods with the same interceptor annotation type are not permitted.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidMultipleInterceptorMethodsOfSameType",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundInvoke")));

        Diagnostic aroundInvokeDuplicate2 = JakartaForJavaAssert.d(37, 18, 22,
                "Only one method with @AroundInvoke annotation is allowed per class. Multiple methods with the same interceptor annotation type are not permitted.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidMultipleInterceptorMethodsOfSameType",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundInvoke")));

        // Test diagnostics for multiple @AroundTimeout methods
        Diagnostic aroundTimeoutDuplicate = JakartaForJavaAssert.d(48, 18, 26,
                "Only one method with @AroundTimeout annotation is allowed per class. Multiple methods with the same interceptor annotation type are not permitted.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidMultipleInterceptorMethodsOfSameType",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundTimeout")));

        // Test diagnostics for multiple @PostConstruct methods
        Diagnostic postConstructDuplicate = JakartaForJavaAssert.d(59, 16, 21,
                "Only one method with @PostConstruct annotation is allowed per class. Multiple methods with the same interceptor annotation type are not permitted.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidMultipleInterceptorMethodsOfSameType",
                new Gson().toJsonTree(Arrays.asList("jakarta.annotation.PostConstruct")));

        // Test diagnostics for multiple @PreDestroy methods
        Diagnostic preDestroyDuplicate = JakartaForJavaAssert.d(70, 16, 24,
                "Only one method with @PreDestroy annotation is allowed per class. Multiple methods with the same interceptor annotation type are not permitted.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidMultipleInterceptorMethodsOfSameType",
                new Gson().toJsonTree(Arrays.asList("jakarta.annotation.PreDestroy")));

        // Test diagnostics for multiple @AroundConstruct methods
        Diagnostic aroundConstructDuplicate = JakartaForJavaAssert.d(81, 16, 26,
                "Only one method with @AroundConstruct annotation is allowed per class. Multiple methods with the same interceptor annotation type are not permitted.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidMultipleInterceptorMethodsOfSameType",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundConstruct")));

        // Test diagnostics for nested class with multiple @AroundInvoke methods
        Diagnostic nestedAroundInvokeDuplicate = JakartaForJavaAssert.d(94, 22, 29,
                "Only one method with @AroundInvoke annotation is allowed per class. Multiple methods with the same interceptor annotation type are not permitted.",
                DiagnosticSeverity.Error, "jakarta-interceptor", "InvalidMultipleInterceptorMethodsOfSameType",
                new Gson().toJsonTree(Arrays.asList("jakarta.interceptor.AroundInvoke")));

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, aroundInvokeDuplicate1, aroundInvokeDuplicate2,
                aroundTimeoutDuplicate, postConstructDuplicate, preDestroyDuplicate,
                aroundConstructDuplicate, nestedAroundInvokeDuplicate);
    }

    @Test
    public void testValidInterceptorOneMethodPerType() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/interceptor/ValidInterceptorOneMethodPerType.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Assert NO diagnostics for valid code with one method per type
        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils);
    }
}