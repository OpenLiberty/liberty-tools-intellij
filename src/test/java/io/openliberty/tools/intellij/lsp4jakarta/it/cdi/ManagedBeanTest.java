/*******************************************************************************
 * Copyright (c) 2021, 2026 IBM Corporation and others.
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

import com.google.gson.Gson;
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
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;

import static io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert.*;

@RunWith(JUnit4.class)
public class ManagedBeanTest extends BaseJakartaTest {

    @Test
    public void managedBeanAnnotations() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/ManagedBean.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // test expected diagnostic
        Diagnostic d1 = d(6, 12, 13,
                "The @Dependent annotation must be the only scope defined by a managed bean with a non-static public field.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidManagedBeanAnnotation");
        
        Diagnostic d2 = d(5, 13, 24,
                "The @Dependent annotation must be the only scope defined by a Managed bean class of generic type.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidManagedBeanAnnotation");

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2);
        String newText1 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.enterprise.context.*;\n\n" +
                "@Dependent\npublic class ManagedBean<T> {\n    " +
                "public int a;\n\n\n    " +
                "public ManagedBean() {\n        this.a = 10;\n    }\n}\n";
        String newText2 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.enterprise.context.*;\n\n" +
                "@Dependent\npublic class ManagedBean<T> {\n    public int a;\n\n\n    " +
                "public ManagedBean() {\n        this.a = 10;\n    }\n}\n";

        // Assert for the diagnostic d1
        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d1);
        TextEdit te1 = te(0, 0, 13, 0, newText1);
        CodeAction ca1 = ca(uri, "Replace current scope with @Dependent", d1, te1);
        assertJavaCodeAction(codeActionParams1, utils, ca1);

        // Assert for the diagnostic d2
        JakartaJavaCodeActionParams codeActionParams2 = createCodeActionParams(uri, d2);
        TextEdit te2 = te(0, 0, 13, 0, newText2);
        CodeAction ca2 = ca(uri, "Replace current scope with @Dependent", d2, te2);
        assertJavaCodeAction(codeActionParams2, utils, ca2);
    }

    @Test
    public void ManagedBeanWithDependent() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/ManagedBeanWithDependent.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // test expected diagnostic
        Diagnostic d1 = d(6, 13, 37,
                "The @Dependent annotation must be the only scope defined by a Managed bean class of generic type.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidManagedBeanAnnotation");

        Diagnostic d2 = d(7, 15, 16,
                "The @Dependent annotation must be the only scope defined by a managed bean with a non-static public field.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidManagedBeanAnnotation");

        Diagnostic d3 = d(17, 6, 27,
                "The @Dependent annotation must be the only scope defined by a managed bean with a non-static public field.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidManagedBeanAnnotation");

        Diagnostic d4 = d(18, 15, 16,
                "The @Dependent annotation must be the only scope defined by a managed bean with a non-static public field.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidManagedBeanAnnotation");

        Diagnostic d5 = d(27, 6, 33,
                "The @Dependent annotation must be the only scope defined by a Managed bean class of generic type.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidManagedBeanAnnotation");

        Diagnostic d6 = d(37, 6, 36,
                "Scope type annotations must be specified by a managed bean class at most once.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidScopeDecl");
        d6.setData(new Gson().toJsonTree(Arrays.asList("jakarta.enterprise.context.SessionScoped", "jakarta.enterprise.context.RequestScoped")));

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3, d4, d5, d6);

        String newText1 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.*;\n\n" +
                "@Dependent\npublic class ManagedBeanWithDependent<T> {\n    public int a;\n\n    " +
                "public ManagedBeanWithDependent() {\n        this.a = 10;\n    }\n}\n\n" +
                "@Dependent\n@RequestScoped\n@SessionScoped\nclass NonGenericManagedBean {\n    public int a;\n\n    " +
                "public NonGenericManagedBean() {\n        this.a = 10;\n    }\n}\n\n@RequestScoped\n@SessionScoped\n" +
                "class ManagedBeanWithoutDependent<T> {\n    public static int a;\n\n    " +
                "public ManagedBeanWithoutDependent() {\n        this.a = 10;\n    }\n}\n\n@RequestScoped\n@SessionScoped\n" +
                "class ManagedBeanWithMultipleScopes2 {\n    public static int a;\n\n    " +
                "public ManagedBeanWithMultipleScopes2() {\n        this.a = 10;\n    }\n}";
        String newText2 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.*;\n\n" +
                "@Dependent\npublic class ManagedBeanWithDependent<T> {\n    public int a;\n\n    " +
                "public ManagedBeanWithDependent() {\n        this.a = 10;\n    }\n}\n\n" +
                "@Dependent\n@RequestScoped\n@SessionScoped\nclass NonGenericManagedBean {\n    public int a;\n\n    " +
                "public NonGenericManagedBean() {\n        this.a = 10;\n    }\n}\n\n@RequestScoped\n@SessionScoped\n" +
                "class ManagedBeanWithoutDependent<T> {\n    public static int a;\n\n    " +
                "public ManagedBeanWithoutDependent() {\n        this.a = 10;\n    }\n}\n\n@RequestScoped\n@SessionScoped\n" +
                "class ManagedBeanWithMultipleScopes2 {\n    public static int a;\n\n    " +
                "public ManagedBeanWithMultipleScopes2() {\n        this.a = 10;\n    }\n}";
        String newText3 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.*;\n\n" +
                "@Dependent\n@RequestScoped\npublic class ManagedBeanWithDependent<T> {\n    public int a;\n\n    " +
                "public ManagedBeanWithDependent() {\n        this.a = 10;\n    }\n}\n\n" +
                "@Dependent\nclass NonGenericManagedBean {\n    public int a;\n\n    " +
                "public NonGenericManagedBean() {\n        this.a = 10;\n    }\n}\n\n@RequestScoped\n" +
                "@SessionScoped\nclass ManagedBeanWithoutDependent<T> {\n    public static int a;\n\n    " +
                "public ManagedBeanWithoutDependent() {\n        this.a = 10;\n    }\n}\n\n@RequestScoped\n" +
                "@SessionScoped\nclass ManagedBeanWithMultipleScopes2 {\n    public static int a;\n\n    " +
                "public ManagedBeanWithMultipleScopes2() {\n        this.a = 10;\n    }\n}";
        String newText4 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.*;\n\n" +
                "@Dependent\n@RequestScoped\npublic class ManagedBeanWithDependent<T> {\n    public int a;\n\n    " +
                "public ManagedBeanWithDependent() {\n        this.a = 10;\n    }\n}\n\n@Dependent\n" +
                "class NonGenericManagedBean {\n    public int a;\n\n    " +
                "public NonGenericManagedBean() {\n        this.a = 10;\n    }\n}\n\n@RequestScoped\n@SessionScoped\n" +
                "class ManagedBeanWithoutDependent<T> {\n    public static int a;\n\n    " +
                "public ManagedBeanWithoutDependent() {\n        this.a = 10;\n    }\n}\n\n@RequestScoped\n" +
                "@SessionScoped\nclass ManagedBeanWithMultipleScopes2 {\n    public static int a;\n\n    " +
                "public ManagedBeanWithMultipleScopes2() {\n        this.a = 10;\n    }\n}";
        String newText5 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.*;\n\n" +
                "@Dependent\n@RequestScoped\npublic class ManagedBeanWithDependent<T> {\n    public int a;\n\n    " +
                "public ManagedBeanWithDependent() {\n        this.a = 10;\n    }\n}\n\n@Dependent\n@RequestScoped\n" +
                "@SessionScoped\nclass NonGenericManagedBean {\n    public int a;\n\n    " +
                "public NonGenericManagedBean() {\n        this.a = 10;\n    }\n}\n\n@Dependent\n" +
                "class ManagedBeanWithoutDependent<T> {\n    public static int a;\n\n    " +
                "public ManagedBeanWithoutDependent() {\n        this.a = 10;\n    }\n}\n\n@RequestScoped\n" +
                "@SessionScoped\nclass ManagedBeanWithMultipleScopes2 {\n    public static int a;\n\n    " +
                "public ManagedBeanWithMultipleScopes2() {\n        this.a = 10;\n    }\n}";
        String newText61 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.*;\n\n" +
                "@Dependent\n@RequestScoped\npublic class ManagedBeanWithDependent<T> {\n    public int a;\n\n    " +
                "public ManagedBeanWithDependent() {\n        this.a = 10;\n    }\n}\n\n@Dependent\n@RequestScoped\n" +
                "@SessionScoped\nclass NonGenericManagedBean {\n    public int a;\n\n    " +
                "public NonGenericManagedBean() {\n        this.a = 10;\n    }\n}\n\n@RequestScoped\n" +
                "@SessionScoped\nclass ManagedBeanWithoutDependent<T> {\n    public static int a;\n\n    " +
                "public ManagedBeanWithoutDependent() {\n        this.a = 10;\n    }\n}\n\n@SessionScoped\n" +
                "class ManagedBeanWithMultipleScopes2 {\n    public static int a;\n\n    " +
                "public ManagedBeanWithMultipleScopes2() {\n        this.a = 10;\n    }\n}";
        String newText62 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.*;\n\n" +
                "@Dependent\n@RequestScoped\npublic class ManagedBeanWithDependent<T> {\n    public int a;\n\n    " +
                "public ManagedBeanWithDependent() {\n        this.a = 10;\n    }\n}\n\n@Dependent\n@RequestScoped\n" +
                "@SessionScoped\nclass NonGenericManagedBean {\n    public int a;\n\n    " +
                "public NonGenericManagedBean() {\n        this.a = 10;\n    }\n}\n\n@RequestScoped\n" +
                "@SessionScoped\nclass ManagedBeanWithoutDependent<T> {\n    public static int a;\n\n    " +
                "public ManagedBeanWithoutDependent() {\n        this.a = 10;\n    }\n}\n\n@RequestScoped\n" +
                "class ManagedBeanWithMultipleScopes2 {\n    public static int a;\n\n    " +
                "public ManagedBeanWithMultipleScopes2() {\n        this.a = 10;\n    }\n}";

        // Assert for the diagnostic d1
        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d1);
        TextEdit te1 = te(0, 0, 43, 1, newText1);
        CodeAction ca1 = ca(uri, "Replace current scope with @Dependent", d1, te1);
        assertJavaCodeAction(codeActionParams1, utils, ca1);

        // Assert for the diagnostic d2
        JakartaJavaCodeActionParams codeActionParams2 = createCodeActionParams(uri, d2);
        TextEdit te2 = te(0, 0, 43, 1, newText2);
        CodeAction ca2 = ca(uri, "Replace current scope with @Dependent", d2, te2);
        assertJavaCodeAction(codeActionParams2, utils, ca2);

        // Assert for the diagnostic d3
        JakartaJavaCodeActionParams codeActionParams3 = createCodeActionParams(uri, d3);
        TextEdit te3 = te(0, 0, 43, 1, newText3);
        CodeAction ca3 = ca(uri, "Replace current scope with @Dependent", d3, te3);
        assertJavaCodeAction(codeActionParams3, utils, ca3);

        // Assert for the diagnostic d4
        JakartaJavaCodeActionParams codeActionParams4 = createCodeActionParams(uri, d4);
        TextEdit te4 = te(0, 0, 43, 1, newText4);
        CodeAction ca4 = ca(uri, "Replace current scope with @Dependent", d4, te4);
        assertJavaCodeAction(codeActionParams4, utils, ca4);

        // Assert for the diagnostic d5
        JakartaJavaCodeActionParams codeActionParams5 = createCodeActionParams(uri, d5);
        TextEdit te5 = te(0, 0, 43, 1, newText5);
        CodeAction ca5 = ca(uri, "Replace current scope with @Dependent", d5, te5);
        assertJavaCodeAction(codeActionParams5, utils, ca5);

        // Assert for the diagnostic d6
        JakartaJavaCodeActionParams codeActionParams6 = createCodeActionParams(uri, d6);
        TextEdit te61 = te(0, 0, 43, 1, newText61);
        CodeAction ca61 = ca(uri, "Remove @RequestScoped", d6, te61);
        TextEdit te62 = te(0, 0, 43, 1, newText62);
        CodeAction ca62 = ca(uri, "Remove @SessionScoped", d6, te62);
        assertJavaCodeAction(codeActionParams6, utils, ca61, ca62);
    }

    @Test
    public void scopeDeclaration() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/ScopeDeclaration.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // test expected diagnostic
        Diagnostic d1 = d(12, 16, 17,
                "Scope type annotations must be specified by a producer field at most once.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidScopeDecl");
        d1.setData(new Gson().toJsonTree(Arrays.asList("jakarta.enterprise.context.Dependent",
                "jakarta.enterprise.context.ApplicationScoped", "jakarta.enterprise.inject.Produces")));

        Diagnostic d2 = d(15, 25, 41, "Scope type annotations must be specified " +
                        "by a producer method at most once.", DiagnosticSeverity.Error, "jakarta-cdi",
                "InvalidScopeDecl");
        d2.setData(new Gson().toJsonTree(Arrays.asList("jakarta.enterprise.context.ApplicationScoped",
                "jakarta.enterprise.context.RequestScoped", "jakarta.enterprise.inject.Produces")));

        Diagnostic d3 = d(10, 13, 29, "Scope type annotations must be " +
                        "specified by a managed bean class at most once.", DiagnosticSeverity.Error,
                "jakarta-cdi", "InvalidScopeDecl");
        d3.setData(new Gson().toJsonTree(Arrays.asList("jakarta.enterprise.context.ApplicationScoped",
                "jakarta.enterprise.context.RequestScoped")));

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3);

        // Assert for the diagnostic d1
        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d1);
        String newText = "package io.openliberty.sample.jakarta.cdi;\n\nimport java.util.Collections;" +
                "\nimport java.util.List;\n\nimport jakarta.enterprise.inject.Produces;\n\n" +
                "import jakarta.enterprise.context.*;\n\n@ApplicationScoped @RequestScoped\n" +
                "public class ScopeDeclaration {\n    @Produces  @Dependent\n    private int n;\n   " +
                " \n    @Produces @ApplicationScoped @RequestScoped\n    public List<Integer> getAllProductIds() " +
                "{\n        return Collections.emptyList();\n    }\n}";

        String newText1 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import java.util.Collections;\nimport java.util.List;\n\n" +
                "import jakarta.enterprise.inject.Produces;\n\nimport jakarta.enterprise.context.*;" +
                "\n\n@ApplicationScoped @RequestScoped\npublic class ScopeDeclaration {\n   " +
                " @Produces @ApplicationScoped\n    private int n;\n    \n    @Produces @ApplicationScoped" +
                " @RequestScoped\n    public List<Integer> getAllProductIds() {\n        " +
                "return Collections.emptyList();\n    }\n}";

        TextEdit te1 = te(0, 0, 18, 1, newText);
        TextEdit te2 = te(0, 0, 18, 1, newText1);
        CodeAction ca1 = ca(uri, "Remove @ApplicationScoped", d1, te1);
        CodeAction ca2 = ca(uri, "Remove @Dependent", d1, te2);
        assertJavaCodeAction(codeActionParams1, utils, ca1,ca2);

        // Assert for the diagnostic d2
        JakartaJavaCodeActionParams codeActionParams2 = createCodeActionParams(uri, d2);
        newText = "package io.openliberty.sample.jakarta.cdi;\n\nimport java.util.Collections;\n" +
                "import java.util.List;\n\nimport jakarta.enterprise.inject.Produces;\n\n" +
                "import jakarta.enterprise.context.*;\n\n@ApplicationScoped @RequestScoped\n" +
                "public class ScopeDeclaration {\n    @Produces @ApplicationScoped @Dependent\n   " +
                " private int n;\n    \n    @Produces  @RequestScoped\n    public List<Integer> getAllProductIds()" +
                " {\n        return Collections.emptyList();\n    }\n}";

        newText1 = "package io.openliberty.sample.jakarta.cdi;\n\nimport java.util.Collections;\nimport " +
                "java.util.List;\n\nimport jakarta.enterprise.inject.Produces;\n\nimport " +
                "jakarta.enterprise.context.*;\n\n@ApplicationScoped @RequestScoped\npublic class ScopeDeclaration" +
                " {\n    @Produces @ApplicationScoped @Dependent\n    private int n;\n    \n   " +
                " @Produces @ApplicationScoped\n    public List<Integer> getAllProductIds() {\n      " +
                "  return Collections.emptyList();\n    }\n}";

        TextEdit te3 = te(0, 0, 18, 1, newText1);
        TextEdit te4 = te(0, 0, 18, 1, newText);
        CodeAction ca3 = ca(uri, "Remove @RequestScoped", d2, te3);
        CodeAction ca4 = ca(uri, "Remove @ApplicationScoped", d2, te4);
        assertJavaCodeAction(codeActionParams2, utils, ca4, ca3);

        // Assert for the diagnostic d3
        JakartaJavaCodeActionParams codeActionParams3 = createCodeActionParams(uri, d3);
        newText = "package io.openliberty.sample.jakarta.cdi;\n\nimport java.util.Collections;\n" +
                "import java.util.List;\n\nimport jakarta.enterprise.inject.Produces;\n\n" +
                "import jakarta.enterprise.context.*;\n\n@RequestScoped\npublic class ScopeDeclaration {\n  " +
                "  @Produces @ApplicationScoped @Dependent\n    private int n;\n    \n   " +
                " @Produces @ApplicationScoped @RequestScoped\n    public List<Integer> getAllProductIds() {\n  " +
                "      return Collections.emptyList();\n    }\n}";

        newText1 = "package io.openliberty.sample.jakarta.cdi;\n\nimport java.util.Collections;" +
                "\nimport java.util.List;\n\nimport jakarta.enterprise.inject.Produces;\n\n" +
                "import jakarta.enterprise.context.*;\n\n@ApplicationScoped\npublic class ScopeDeclaration {\n " +
                "   @Produces @ApplicationScoped @Dependent\n    private int n;\n    \n    " +
                "@Produces @ApplicationScoped @RequestScoped\n    public List<Integer> getAllProductIds() {\n   " +
                "     return Collections.emptyList();\n    }\n}";

        TextEdit te5 = te(0, 0, 18, 1, newText1);
        TextEdit te6 = te(0, 0, 18, 1, newText);
        CodeAction ca5 = ca(uri, "Remove @RequestScoped", d3, te5);
        CodeAction ca6 = ca(uri, "Remove @ApplicationScoped", d3, te6);
        assertJavaCodeAction(codeActionParams3, utils, ca6, ca5);
    }

    @Test
    public void producesAndInject() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/ProducesAndInjectTogether.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d1 = d(16, 18, 23, "The @Produces and @Inject annotations must not be used on the same field or property.",
                DiagnosticSeverity.Error, "jakarta-cdi", "RemoveProducesOrInject");

        Diagnostic d2 = d(11, 19, 27, "The @Produces and @Inject annotations must not be used on the same field or property.",
                DiagnosticSeverity.Error, "jakarta-cdi", "RemoveProducesOrInject");

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2);

        String newText = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.enterprise.context.ApplicationScoped; \n" +
                "import jakarta.enterprise.inject.Produces; \n" +
                "import jakarta.inject.Inject;\n\n\n@ApplicationScoped\n" +
                "public class ProducesAndInjectTogether {\n    @Produces\n" +
                "    @Inject\n    private String greeting = \"Hello\";\n" +
                "    \n    \n    @Inject\n    public String greet(String name) {\n" +
                "        return this.greeting + \" \" + name + \"!\";\n    }\n}";
        String newText1 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.enterprise.context.ApplicationScoped; \n" +
                "import jakarta.enterprise.inject.Produces; \n" +
                "import jakarta.inject.Inject;\n\n\n@ApplicationScoped\n" +
                "public class ProducesAndInjectTogether {\n    @Produces\n" +
                "    @Inject\n    private String greeting = \"Hello\";\n    \n    \n" +
                "    @Produces\n    public String greet(String name) {\n" +
                "        return this.greeting + \" \" + name + \"!\";\n    }\n}";

        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d1);

        TextEdit te1 = te(0, 0, 19, 1, newText);
        TextEdit te2 = te(0, 0, 19, 1, newText1);
        CodeAction ca1 = ca(uri, "Remove @Produces", d1, te1);
        CodeAction ca2 = ca(uri, "Remove @Inject", d1, te2);

        assertJavaCodeAction(codeActionParams1, utils, ca2, ca1);

        JakartaJavaCodeActionParams codeActionParams2 = createCodeActionParams(uri, d2);

        String newText2 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.enterprise.context.ApplicationScoped; \n" +
                "import jakarta.enterprise.inject.Produces; \n" +
                "import jakarta.inject.Inject;\n\n\n@ApplicationScoped\n" +
                "public class ProducesAndInjectTogether {\n    @Inject\n" +
                "    private String greeting = \"Hello\";\n    \n    \n" +
                "    @Produces\n    @Inject\n    public String greet(String name) {\n" +
                "        return this.greeting + \" \" + name + \"!\";\n    }\n}";
        String newText3 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.enterprise.context.ApplicationScoped; \n" +
                "import jakarta.enterprise.inject.Produces; \n" +
                "import jakarta.inject.Inject;\n\n\n@ApplicationScoped\n" +
                "public class ProducesAndInjectTogether {\n    @Produces\n" +
                "    private String greeting = \"Hello\";\n    \n    \n" +
                "    @Produces\n    @Inject\n    public String greet(String name) {\n" +
                "        return this.greeting + \" \" + name + \"!\";\n    }\n}";

        TextEdit te3 = te(0, 0, 19, 1, newText2);
        TextEdit te4 = te(0, 0, 19, 1, newText3);
        CodeAction ca3 = ca(uri, "Remove @Produces", d2, te3);
        CodeAction ca4 = ca(uri, "Remove @Inject", d2, te4);

        assertJavaCodeAction(codeActionParams2, utils, ca4, ca3);
    }

    @Test
    public void injectAndDisposesObservesObservesAsync() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/InjectAndDisposesObservesObservesAsync.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d1 = d(10, 18, 31,
                "A bean constructor or a method annotated with @Inject cannot have parameter(s) annotated with @Disposes.",
                DiagnosticSeverity.Error, "jakarta-cdi", "RemoveInjectOrConflictedAnnotations");

        Diagnostic d2 = d(16, 18, 31,
                "A bean constructor or a method annotated with @Inject cannot have parameter(s) annotated with @Observes.",
                DiagnosticSeverity.Error, "jakarta-cdi", "RemoveInjectOrConflictedAnnotations");

        Diagnostic d3 = d(22, 18, 36,
                "A bean constructor or a method annotated with @Inject cannot have parameter(s) annotated with @ObservesAsync.",
                DiagnosticSeverity.Error, "jakarta-cdi", "RemoveInjectOrConflictedAnnotations");

        Diagnostic d4 = d(28, 18, 39,
                "A bean constructor or a method annotated with @Inject cannot have parameter(s) annotated with @Disposes, @Observes.",
                DiagnosticSeverity.Error, "jakarta-cdi", "RemoveInjectOrConflictedAnnotations");

        Diagnostic d5 = d(34, 18, 44,
                "A bean constructor or a method annotated with @Inject cannot have parameter(s) annotated with @Observes, @ObservesAsync.",
                DiagnosticSeverity.Error, "jakarta-cdi", "RemoveInjectOrConflictedAnnotations");

        Diagnostic d6 = d(34, 18, 44,
                "Parameters name1, name2 are annotated with @Observes or @ObservesAsync, but a method cannot contain more than one such parameter.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidMultipleObserverParams");

        Diagnostic d7 = d(40, 18, 44,
                "A bean constructor or a method annotated with @Inject cannot have parameter(s) annotated with @Disposes, @ObservesAsync.",
                DiagnosticSeverity.Error, "jakarta-cdi", "RemoveInjectOrConflictedAnnotations");

        Diagnostic d8 = d(46, 18, 52,
                "A bean constructor or a method annotated with @Inject cannot have parameter(s) annotated with @Disposes, @Observes, @ObservesAsync.",
                DiagnosticSeverity.Error, "jakarta-cdi", "RemoveInjectOrConflictedAnnotations");

        Diagnostic d9 = d(46, 18, 52,
                "Parameters name2, name3 are annotated with @Observes or @ObservesAsync, but a method cannot contain more than one such parameter.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidMultipleObserverParams");

        Diagnostic d10 = d(51, 18, 53,
                "A CDI method must not have parameter(s): name annotated with @Observes and @ObservesAsync.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidObservesObservesAsyncMethodParams");

        Diagnostic d11 = d(51, 18, 53,
                "A bean constructor or a method annotated with @Inject cannot have parameter(s) annotated with @Disposes, @Observes, @ObservesAsync.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidInjectAnnotationOnMultipleMethodParams");

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3, d4, d6, d5, d7, d9, d8, d10, d11);

        //Starting CodeAction tests
        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d1);

        String newText = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.inject.Inject;\nimport jakarta.enterprise.inject.Disposes;\n" +
                "import jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\n" +
                "public class InjectAndDisposesObservesObservesAsync {\n    \n" +
                "    public String greetDisposes(@Disposes String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n" +
                "    @Inject\n    public String greetObserves(@Observes String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Inject\n" +
                "    public String greetObservesAsync(@ObservesAsync String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Inject\n" +
                "    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n" +
                "    \n    \n    @Inject\n" +
                "    public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n" +
                "    @Inject\n    public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Inject\n" +
                "    public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n" +
                "        return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n" +
                "    }\n    \n    @Inject\n    public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n}\n";
        String newText0 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.inject.Inject;\nimport jakarta.enterprise.inject.Disposes;\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\npublic class InjectAndDisposesObservesObservesAsync {\n\n    @Inject\n    " +
                "public String greetDisposes(String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetObserves(@Observes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetObservesAsync(@ObservesAsync String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n}\n";

        TextEdit te1 = te(0, 0, 55, 0, newText);
        TextEdit te2 = te(0, 0, 55, 0, newText0);
        CodeAction ca1 = ca(uri, "Remove @Inject", d1, te1);
        CodeAction ca2 = ca(uri, "Remove the @Disposes modifier from parameter name", d1, te2);

        assertJavaCodeAction(codeActionParams1, utils, ca1, ca2);

        JakartaJavaCodeActionParams codeActionParams2 = createCodeActionParams(uri, d2);

        String newText1 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.inject.Inject;\nimport jakarta.enterprise.inject.Disposes;\n" +
                "import jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n\n" +
                "public class InjectAndDisposesObservesObservesAsync {\n    \n    @Inject\n" +
                "    public String greetDisposes(@Disposes String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n" +
                "    public String greetObserves(@Observes String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n" +
                "    @Inject\n    public String greetObservesAsync(@ObservesAsync String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Inject\n" +
                "    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Inject\n" +
                "    public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Inject\n" +
                "    public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Inject\n" +
                "    public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n" +
                "        return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n    \n" +
                "    @Inject\n    public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n}\n";
        String newText2 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.inject.Inject;\nimport jakarta.enterprise.inject.Disposes;\n" +
                "import jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n\n" +
                "public class InjectAndDisposesObservesObservesAsync {\n\n    @Inject\n    public String greetDisposes(@Disposes String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    public String greetObserves(String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    public String greetObservesAsync(@ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n}\n";

        TextEdit te3 = te(0, 0, 55, 0, newText1);
        TextEdit te4 = te(0, 0, 55, 0, newText2);
        CodeAction ca3 = ca(uri, "Remove @Inject", d2, te3);
        CodeAction ca4 = ca(uri, "Remove the @Observes modifier from parameter name", d2, te4);

        assertJavaCodeAction(codeActionParams2, utils, ca3, ca4);

        JakartaJavaCodeActionParams codeActionParams3 = createCodeActionParams(uri, d3);

        String newText19 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.inject.Inject;\nimport jakarta.enterprise.inject.Disposes;\n" +
                "import jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n\n" +
                "public class InjectAndDisposesObservesObservesAsync {\n    \n    @Inject\n" +
                "    public String greetDisposes(@Disposes String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Inject\n" +
                "    public String greetObserves(@Observes String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n" +
                "    public String greetObservesAsync(@ObservesAsync String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Inject\n" +
                "    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Inject\n" +
                "    public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n" +
                "    @Inject\n    public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Inject\n" +
                "    public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n" +
                "        return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n    \n" +
                "    @Inject\n    public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n}\n";
        String newText3 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.inject.Inject;\nimport jakarta.enterprise.inject.Disposes;\n" +
                "import jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n\n" +
                "public class InjectAndDisposesObservesObservesAsync {\n\n    @Inject\n    public String greetDisposes(@Disposes String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    public String greetObserves(@Observes String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    public String greetObservesAsync(String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n}\n";

        TextEdit te5 = te(0, 0, 55, 0, newText19);
        TextEdit te6 = te(0, 0, 55, 0, newText3);
        CodeAction ca5 = ca(uri, "Remove @Inject", d3, te5);
        CodeAction ca6 = ca(uri, "Remove the @ObservesAsync modifier from parameter name", d3, te6);

        assertJavaCodeAction(codeActionParams3, utils, ca5, ca6);

        JakartaJavaCodeActionParams codeActionParams4 = createCodeActionParams(uri, d4);

        String newText4 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.inject.Inject;\nimport jakarta.enterprise.inject.Disposes;\n" +
                "import jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n\n" +
                "public class InjectAndDisposesObservesObservesAsync {\n    \n    @Inject\n" +
                "    public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n" +
                "    }\n    \n    \n    @Inject\n    public String greetObserves(@Observes String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Inject\n" +
                "    public String greetObservesAsync(@ObservesAsync String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n" +
                "    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Inject\n" +
                "    public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n" +
                "    @Inject\n    public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Inject\n" +
                "    public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n" +
                "        return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n    \n    @Inject\n" +
                "    public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n}\n";
        String newText5 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.inject.Inject;\nimport jakarta.enterprise.inject.Disposes;\n" +
                "import jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n\n" +
                "public class InjectAndDisposesObservesObservesAsync {\n\n    @Inject\n    public String greetDisposes(@Disposes String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    public String greetObserves(@Observes String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    public String greetObservesAsync(@ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    public String greetDisposesObserves(String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n}\n";
        String newText6 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.inject.Inject;\nimport jakarta.enterprise.inject.Disposes;\n" +
                "import jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n\npublic class InjectAndDisposesObservesObservesAsync {\n\n    " +
                "@Inject\n    public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetObserves(@Observes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetObservesAsync(@ObservesAsync String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObserves(@Disposes String name1, String name2) {\n        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    " +
                "@Inject\n    public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n}\n";

        TextEdit te7 = te(0, 0, 55, 0, newText4);
        TextEdit te8 = te(0, 0, 55, 0, newText5);
        TextEdit te9 = te(0, 0, 55, 0, newText6);
        CodeAction ca7 = ca(uri, "Remove @Inject", d4, te7);
        CodeAction ca8 = ca(uri, "Remove the @Disposes modifier from parameter name1", d4, te8);
        CodeAction ca9 = ca(uri, "Remove the @Observes modifier from parameter name2", d4, te9);

        assertJavaCodeAction(codeActionParams4, utils, ca7, ca8, ca9);

        JakartaJavaCodeActionParams codeActionParams5 = createCodeActionParams(uri, d5);

        String newText7 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.inject.Inject;\nimport jakarta.enterprise.inject.Disposes;\n" +
                "import jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n\n" +
                "public class InjectAndDisposesObservesObservesAsync {\n    \n    @Inject\n" +
                "    public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n" +
                "    }\n    \n    \n    @Inject\n    public String greetObserves(@Observes String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Inject\n" +
                "    public String greetObservesAsync(@ObservesAsync String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Inject\n" +
                "    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n" +
                "    public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Inject\n" +
                "    public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Inject\n" +
                "    public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n" +
                "        return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n    \n" +
                "    @Inject\n    public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n}\n";
        String newText8 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.inject.Inject;\nimport jakarta.enterprise.inject.Disposes;\n" +
                "import jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n\npublic class InjectAndDisposesObservesObservesAsync {\n\n    " +
                "@Inject\n    public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetObserves(@Observes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetObservesAsync(@ObservesAsync String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    " +
                "@Inject\n    public String greetObservesObservesAsync(String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n}\n";
        String newText9 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.inject.Inject;\nimport jakarta.enterprise.inject.Disposes;\n" +
                "import jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n\npublic class InjectAndDisposesObservesObservesAsync {\n\n    " +
                "@Inject\n    public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetObserves(@Observes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetObservesAsync(@ObservesAsync String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    " +
                "@Inject\n    public String greetObservesObservesAsync(@Observes String name1, String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    " +
                "@Inject\n    public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n}\n";

        TextEdit te10 = te(0, 0, 55, 0, newText7);
        TextEdit te11 = te(0, 0, 55, 0, newText8);
        TextEdit te12 = te(0, 0, 55, 0, newText9);
        CodeAction ca10 = ca(uri, "Remove @Inject", d5, te10);
        CodeAction ca11 = ca(uri, "Remove the @Observes modifier from parameter name1", d5, te11);
        CodeAction ca12 = ca(uri, "Remove the @ObservesAsync modifier from parameter name2", d5, te12);

        assertJavaCodeAction(codeActionParams5, utils, ca10, ca11, ca12);

        JakartaJavaCodeActionParams codeActionParams6 = createCodeActionParams(uri, d8);

        String newText10 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.inject.Inject;" +
                "\nimport jakarta.enterprise.inject.Disposes;\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\npublic class InjectAndDisposesObservesObservesAsync {\n    \n    " +
                "@Inject\n    public String greetDisposes(@Disposes String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n    \n    \n    " +
                "@Inject\n    public String greetObserves(@Observes String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n    \n    \n    " +
                "@Inject\n    public String greetObservesAsync(@ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n    \n    \n    " +
                "@Inject\n    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    " +
                "@Inject\n    public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    " +
                "@Inject\n    public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n    \n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n}\n";
        String newText11 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.inject.Inject;\n" +
                "import jakarta.enterprise.inject.Disposes;\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\npublic class InjectAndDisposesObservesObservesAsync {\n\n    " +
                "@Inject\n    public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Inject\n    public String greetObserves(@Observes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Inject\n    public String greetObservesAsync(@ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync(String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n}\n";
        String newText12 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.inject.Inject;\n" +
                "import jakarta.enterprise.inject.Disposes;\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\npublic class InjectAndDisposesObservesObservesAsync {\n\n    " +
                "@Inject\n    public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Inject\n    public String greetObserves(@Observes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Inject\n    public String greetObservesAsync(@ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Inject\n    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n}\n";
        String newText132 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.inject.Inject;\n" +
                "import jakarta.enterprise.inject.Disposes;\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\npublic class InjectAndDisposesObservesObservesAsync {\n\n    " +
                "@Inject\n    public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Inject\n    public String greetObserves(@Observes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Inject\n    public String greetObservesAsync(@ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n}\n";

        TextEdit te13 = te(0, 0, 55, 0, newText10);
        TextEdit te14 = te(0, 0, 55, 0, newText11);
        TextEdit te15 = te(0, 0, 55, 0, newText12);
        TextEdit te162 = te(0, 0, 55, 0, newText132);
        CodeAction ca13 = ca(uri, "Remove @Inject", d8, te13);
        CodeAction ca14 = ca(uri, "Remove the @Disposes modifier from parameter name1", d8, te14);
        CodeAction ca15 = ca(uri, "Remove the @Observes modifier from parameter name2", d8, te15);
        CodeAction ca162 = ca(uri, "Remove the @ObservesAsync modifier from parameter name3", d8, te162);

        assertJavaCodeAction(codeActionParams6, utils, ca13, ca14, ca15, ca162);

        JakartaJavaCodeActionParams codeActionParams7 = createCodeActionParams(uri, d7);

        String newText13 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.inject.Inject;\nimport jakarta.enterprise.inject.Disposes;\nimport jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n\npublic class InjectAndDisposesObservesObservesAsync {\n    \n    @Inject\n    public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Inject\n    public String greetObserves(@Observes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Inject\n    public String greetObservesAsync(@ObservesAsync String name) {\n        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Inject\n    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Inject\n    public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Inject\n    public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n    \n    @Inject\n    public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        return \"Hi \" + name + \"!\";\n    }\n}\n";
        String newText14 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.inject.Inject;\nimport jakarta.enterprise.inject.Disposes;\nimport jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n\npublic class InjectAndDisposesObservesObservesAsync {\n\n    @Inject\n    public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    public String greetObserves(@Observes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    public String greetObservesAsync(@ObservesAsync String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    public String greetDisposesObservesAsync(String name1, @ObservesAsync String name2) {\n        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n    @Inject\n    public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        return \"Hi \" + name + \"!\";\n    }\n}\n";
        String newText16 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.inject.Inject;\nimport jakarta.enterprise.inject.Disposes;\nimport jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n\npublic class InjectAndDisposesObservesObservesAsync {\n\n    @Inject\n    public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    public String greetObserves(@Observes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    public String greetObservesAsync(@ObservesAsync String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Inject\n    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    public String greetDisposesObservesAsync(@Disposes String name1, String name2) {\n        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n    @Inject\n    public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        return \"Hi \" + name + \"!\";\n    }\n}\n";

        TextEdit te16 = te(0, 0, 55, 0, newText13);
        TextEdit te17 = te(0, 0, 55, 0, newText14);
        TextEdit te19 = te(0, 0, 55, 0, newText16);
        CodeAction ca16 = ca(uri, "Remove @Inject", d7, te16);
        CodeAction ca17 = ca(uri, "Remove the @Disposes modifier from parameter name1", d7, te17);
        CodeAction ca19 = ca(uri, "Remove the @ObservesAsync modifier from parameter name2", d7, te19);

        assertJavaCodeAction(codeActionParams7, utils, ca16, ca17, ca19);

        JakartaJavaCodeActionParams codeActionParams8 = createCodeActionParams(uri, d11);

        String newText200 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.inject.Inject;\n" +
                "import jakarta.enterprise.inject.Disposes;\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\npublic class InjectAndDisposesObservesObservesAsync {\n    \n    " +
                "@Inject\n    public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    " +
                "@Inject\n    public String greetObserves(@Observes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    " +
                "@Inject\n    public String greetObservesAsync(@ObservesAsync String name) {\n        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    " +
                "@Inject\n    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Inject\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Inject\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n    \n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n}\n";
        String newText201 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.inject.Inject;\n" +
                "import jakarta.enterprise.inject.Disposes;\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\npublic class InjectAndDisposesObservesObservesAsync {\n\n    " +
                "@Inject\n    public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Inject\n    public String greetObserves(@Observes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Inject\n    public String greetObservesAsync(@ObservesAsync String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Inject\n    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync2(@Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n}\n";
        String newText202 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.inject.Inject;\n" +
                "import jakarta.enterprise.inject.Disposes;\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\npublic class InjectAndDisposesObservesObservesAsync {\n\n    " +
                "@Inject\n    public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Inject\n    public String greetObserves(@Observes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Inject\n    public String greetObservesAsync(@ObservesAsync String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Inject\n    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync2(String name) {\n        return \"Hi \" + name + \"!\";\n    }\n}\n";
        String newText203 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.inject.Inject;\n" +
                "import jakarta.enterprise.inject.Disposes;\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\npublic class InjectAndDisposesObservesObservesAsync {\n\n    " +
                "@Inject\n    public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Inject\n    public String greetObserves(@Observes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Inject\n    public String greetObservesAsync(@ObservesAsync String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Inject\n    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n    @Inject\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n}\n";


        TextEdit te200 = te(0, 0, 55, 0, newText200);
        TextEdit te201 = te(0, 0, 55, 0, newText201);
        TextEdit te202 = te(0, 0, 55, 0, newText202);
        TextEdit te203 = te(0, 0, 55, 0, newText203);
        CodeAction ca200 = ca(uri, "Remove @Inject", d11, te200);
        CodeAction ca201 = ca(uri, "Remove the @Disposes modifier from parameter name", d11, te201);
        CodeAction ca202 = ca(uri, "Remove the @Disposes, @Observes, @ObservesAsync modifier from parameter name", d11, te202);
        CodeAction ca203 = ca(uri, "Remove the @Observes, @ObservesAsync modifier from parameter name", d11, te203);

        assertJavaCodeAction(codeActionParams8, utils, ca200, ca201, ca202, ca203);
    }

    @Test
    public void producesAndDisposesObservesObservesAsync() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/ProducesAndDisposesObservesObservesAsync.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic producesWithDisposes = d(12, 18, 31,
                "A producer method cannot have parameter(s) annotated with @Disposes.",
                DiagnosticSeverity.Error, "jakarta-cdi", "RemoveProducesOrConflictedAnnotations");

        Diagnostic producesWithObserves = d(18, 18, 31,
                "A producer method cannot have parameter(s) annotated with @Observes.",
                DiagnosticSeverity.Error, "jakarta-cdi", "RemoveProducesOrConflictedAnnotations");

        Diagnostic producesWithAsync = d(24, 18, 36,
                "A producer method cannot have parameter(s) annotated with @ObservesAsync.",
                DiagnosticSeverity.Error, "jakarta-cdi", "RemoveProducesOrConflictedAnnotations");

        Diagnostic producesDisposesObs = d(30, 18, 39,
                "A producer method cannot have parameter(s) annotated with @Disposes, @Observes.",
                DiagnosticSeverity.Error, "jakarta-cdi", "RemoveProducesOrConflictedAnnotations");

        Diagnostic producesObsAsync = d(36, 18, 44,
                "A producer method cannot have parameter(s) annotated with @Observes, @ObservesAsync.",
                DiagnosticSeverity.Error, "jakarta-cdi", "RemoveProducesOrConflictedAnnotations");
        Diagnostic multiObsParams1 = d(36, 18, 44,
                "Parameters name1, name2 are annotated with @Observes or @ObservesAsync, but a method cannot contain more than one such parameter.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidMultipleObserverParams");

        Diagnostic producesDisposesAsync = d(42, 18, 44,
                "A producer method cannot have parameter(s) annotated with @Disposes, @ObservesAsync.",
                DiagnosticSeverity.Error, "jakarta-cdi", "RemoveProducesOrConflictedAnnotations");

        Diagnostic producesAllThree = d(48, 18, 52,
                "A producer method cannot have parameter(s) annotated with @Disposes, @Observes, @ObservesAsync.",
                DiagnosticSeverity.Error, "jakarta-cdi", "RemoveProducesOrConflictedAnnotations");

        Diagnostic obsBothAnnotations = d(54, 18, 53,
                "A CDI method must not have parameter(s): name annotated with @Observes and @ObservesAsync.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidObservesObservesAsyncMethodParams");

        Diagnostic producesAllThree2 = d(54, 18, 53,
                "A producer method cannot have parameter(s) annotated with @Disposes, @Observes, @ObservesAsync.",
                DiagnosticSeverity.Error, "jakarta-cdi", "RemoveProducesOrConflictedAnnotations");

        Diagnostic disposesWithObs = d(30, 18, 39,
                "A disposer method cannot have parameter(s) annotated with @jakarta.enterprise.event.Observes.",
                DiagnosticSeverity.Error, "jakarta-cdi", "RemoveDisposesOrConflictedAnnotations");

        Diagnostic disposesWithAsync = d(42, 18, 44,
                "A disposer method cannot have parameter(s) annotated with @jakarta.enterprise.event.ObservesAsync.",
                DiagnosticSeverity.Error, "jakarta-cdi", "RemoveDisposesOrConflictedAnnotations");

        Diagnostic disposesObsAsync = d(48, 18, 52,
                "A disposer method cannot have parameter(s) annotated with @jakarta.enterprise.event.Observes, @jakarta.enterprise.event.ObservesAsync.",
                DiagnosticSeverity.Error, "jakarta-cdi", "RemoveDisposesOrConflictedAnnotations");
        Diagnostic multiObsParams2 = d(48, 18, 52,
                "Parameters name2, name3 are annotated with @Observes or @ObservesAsync, but a method cannot contain more than one such parameter.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidMultipleObserverParams");

        Diagnostic disposesObsAsync2 = d(54, 18, 53,
                "A disposer method cannot have parameter(s) annotated with @jakarta.enterprise.event.Observes, @jakarta.enterprise.event.ObservesAsync.",
                DiagnosticSeverity.Error, "jakarta-cdi", "RemoveDisposesOrConflictedAnnotations");
        Diagnostic multiObsParams3 = d(58, 18, 52,
                "Parameters name, name1 are annotated with @Observes or @ObservesAsync, but a method cannot contain more than one such parameter.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidMultipleObserverParams");

        Diagnostic obsBothAnnotations2 = d(58, 18, 52,
                "A CDI method must not have parameter(s): name, name1 annotated with @Observes and @ObservesAsync.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidObservesObservesAsyncMethodParams");

        assertJavaDiagnostics(diagnosticsParams, utils, producesWithDisposes, producesWithObserves, producesWithAsync, producesDisposesObs, multiObsParams1, producesObsAsync, producesDisposesAsync, multiObsParams2, obsBothAnnotations, producesAllThree2, disposesWithObs, disposesWithAsync, producesAllThree, disposesObsAsync, disposesObsAsync2, obsBothAnnotations2, multiObsParams3);

        //Starting CodeAction tests
        JakartaJavaCodeActionParams paramsProducesDisposes = createCodeActionParams(uri, producesWithDisposes);

        String textRemoveProduces1 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n\n" +
                "import jakarta.enterprise.inject.Produces;\n" +
                "import jakarta.enterprise.inject.Disposes;\n" +
                "import jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\n" +
                "@ApplicationScoped\npublic class ProducesAndDisposesObservesObservesAsync {\n" +
                "    public String greetDisposes(@Disposes String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n" +
                "    @Produces\n    public String greetObserves(@Observes String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetObservesAsync(@ObservesAsync String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n" +
                "    }\n    \n    \n    @Produces\n" +
                "    public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n" +
                "    }\n    \n    \n    @Produces\n" +
                "    public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n" +
                "    }\n    \n    \n    @Produces\n" +
                "    public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n" +
                "        return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n    \n    \n" +
                "    @Produces\n    public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n\n"+
                "    public String greetObservesObservesAsyncConflict(@Observes @ObservesAsync String name, @Observes @ObservesAsync String name1) {\n" +
                "        return \"Hi \" + name + \" and \" + name1 + \"!\";\n    }\n}\n";
        String textRemoveDisposes1 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.ApplicationScoped;\n\n" +
                "import jakarta.enterprise.inject.Produces;\n" +
                "import jakarta.enterprise.inject.Disposes;\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\n@ApplicationScoped\n" +
                "public class ProducesAndDisposesObservesObservesAsync {\n    @Produces\n    public String greetDisposes(String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    public String greetObserves(@Observes String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    public String greetObservesAsync(@ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n    "+
                "public String greetObservesObservesAsyncConflict(@Observes @ObservesAsync String name, @Observes @ObservesAsync String name1) {\n        " +
                "return \"Hi \" + name + \" and \" + name1 + \"!\";\n    }\n}\n";

        TextEdit editRemoveProduces1 = te(0, 0, 62, 0, textRemoveProduces1);
        TextEdit editRemoveDisposes1 = te(0, 0, 62, 0, textRemoveDisposes1);
        CodeAction actionRemoveProduces1 = ca(uri, "Remove @Produces", producesWithDisposes, editRemoveProduces1);
        CodeAction actionRemoveDisposes1 = ca(uri, "Remove the @Disposes modifier from parameter name", producesWithDisposes, editRemoveDisposes1);

        assertJavaCodeAction(paramsProducesDisposes, utils, actionRemoveProduces1, actionRemoveDisposes1);

        JakartaJavaCodeActionParams paramsProducesObserves = createCodeActionParams(uri, producesWithObserves);

        String textRemoveProduces2 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n\n" +
                "import jakarta.enterprise.inject.Produces;\n" +
                "import jakarta.enterprise.inject.Disposes;\n" +
                "import jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\n@ApplicationScoped\n" +
                "public class ProducesAndDisposesObservesObservesAsync {\n    @Produces\n" +
                "    public String greetDisposes(@Disposes String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n" +
                "    public String greetObserves(@Observes String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetObservesAsync(@ObservesAsync String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n" +
                "        return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n    \n    \n" +
                "    @Produces\n    public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n\n"+
                "    public String greetObservesObservesAsyncConflict(@Observes @ObservesAsync String name, @Observes @ObservesAsync String name1) {\n" +
                "        return \"Hi \" + name + \" and \" + name1 + \"!\";\n    }\n}\n";
        String textRemoveObserves2 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.ApplicationScoped;\n\n" +
                "import jakarta.enterprise.inject.Produces;\n" +
                "import jakarta.enterprise.inject.Disposes;\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\n@ApplicationScoped\n" +
                "public class ProducesAndDisposesObservesObservesAsync {\n    @Produces\n    public String greetDisposes(@Disposes String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    public String greetObserves(String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    public String greetObservesAsync(@ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n"+
                "    public String greetObservesObservesAsyncConflict(@Observes @ObservesAsync String name, @Observes @ObservesAsync String name1) {\n" +
                "        return \"Hi \" + name + \" and \" + name1 + \"!\";\n    }\n}\n";

        TextEdit editRemoveProduces2 = te(0, 0, 62, 0, textRemoveProduces2);
        TextEdit editRemoveObserves2 = te(0, 0, 62, 0, textRemoveObserves2);
        CodeAction actionRemoveProduces2 = ca(uri, "Remove @Produces", producesWithObserves, editRemoveProduces2);
        CodeAction actionRemoveObserves2 = ca(uri, "Remove the @Observes modifier from parameter name", producesWithObserves, editRemoveObserves2);

        assertJavaCodeAction(paramsProducesObserves, utils, actionRemoveProduces2, actionRemoveObserves2);

        JakartaJavaCodeActionParams paramsProducesAsync = createCodeActionParams(uri, producesWithAsync);

        String textRemoveProduces3 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n\n" +
                "import jakarta.enterprise.inject.Produces;\nimport jakarta.enterprise.inject.Disposes;\n" +
                "import jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n\n" +
                "@ApplicationScoped\npublic class ProducesAndDisposesObservesObservesAsync {\n    @Produces\n" +
                "    public String greetDisposes(@Disposes String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetObserves(@Observes String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n" +
                "    public String greetObservesAsync(@ObservesAsync String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n" +
                "        return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n    \n    \n" +
                "    @Produces\n    public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n\n"+
                "    public String greetObservesObservesAsyncConflict(@Observes @ObservesAsync String name, @Observes @ObservesAsync String name1) {\n" +
                "        return \"Hi \" + name + \" and \" + name1 + \"!\";\n    }\n}\n";
        String textRemoveAsync3 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.ApplicationScoped;\n\n" +
                "import jakarta.enterprise.inject.Produces;\n" +
                "import jakarta.enterprise.inject.Disposes;\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\n@ApplicationScoped\n" +
                "public class ProducesAndDisposesObservesObservesAsync {\n    @Produces\n    public String greetDisposes(@Disposes String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    public String greetObserves(@Observes String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    public String greetObservesAsync(String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n"+
                "    public String greetObservesObservesAsyncConflict(@Observes @ObservesAsync String name, @Observes @ObservesAsync String name1) {\n" +
                "        return \"Hi \" + name + \" and \" + name1 + \"!\";\n    }\n}\n";

        TextEdit editRemoveProduces3 = te(0, 0, 62, 0, textRemoveProduces3);
        TextEdit editRemoveAsync3 = te(0, 0, 62, 0, textRemoveAsync3);
        CodeAction actionRemoveProduces3 = ca(uri, "Remove @Produces", producesWithAsync, editRemoveProduces3);
        CodeAction actionRemoveAsync3 = ca(uri, "Remove the @ObservesAsync modifier from parameter name", producesWithAsync, editRemoveAsync3);

        assertJavaCodeAction(paramsProducesAsync, utils, actionRemoveProduces3, actionRemoveAsync3);

        JakartaJavaCodeActionParams paramsProducesDisposesObs = createCodeActionParams(uri, producesDisposesObs);

        String textRemoveProduces4 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n\n" +
                "import jakarta.enterprise.inject.Produces;\nimport jakarta.enterprise.inject.Disposes;\n" +
                "import jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n\n" +
                "@ApplicationScoped\npublic class ProducesAndDisposesObservesObservesAsync {\n    @Produces\n" +
                "    public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n" +
                "    }\n    \n    \n    @Produces\n    public String greetObserves(@Observes String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetObservesAsync(@ObservesAsync String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n" +
                "    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n" +
                "        return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n    \n    \n" +
                "    @Produces\n    public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n\n"+
                "    public String greetObservesObservesAsyncConflict(@Observes @ObservesAsync String name, @Observes @ObservesAsync String name1) {\n" +
                "        return \"Hi \" + name + \" and \" + name1 + \"!\";\n    }\n}\n";
        String textRemoveDisposes4 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.ApplicationScoped;\n\n" +
                "import jakarta.enterprise.inject.Produces;\nimport jakarta.enterprise.inject.Disposes;\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\n@ApplicationScoped\npublic class ProducesAndDisposesObservesObservesAsync {\n    " +
                "@Produces\n    public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObserves(@Observes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObservesAsync(@ObservesAsync String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObserves(String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n"+
                "    public String greetObservesObservesAsyncConflict(@Observes @ObservesAsync String name, @Observes @ObservesAsync String name1) {\n" +
                "        return \"Hi \" + name + \" and \" + name1 + \"!\";\n    }\n}\n";
        String textRemoveObserves4 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.ApplicationScoped;\n\n" +
                "import jakarta.enterprise.inject.Produces;\nimport jakarta.enterprise.inject.Disposes;\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\n@ApplicationScoped\npublic class ProducesAndDisposesObservesObservesAsync {\n    @Produces\n    " +
                "public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObserves(@Observes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObservesAsync(@ObservesAsync String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObserves(@Disposes String name1, String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n"+
                "    public String greetObservesObservesAsyncConflict(@Observes @ObservesAsync String name, @Observes @ObservesAsync String name1) {\n" +
                "        return \"Hi \" + name + \" and \" + name1 + \"!\";\n    }\n}\n";

        TextEdit editRemoveProduces4 = te(0, 0, 62, 0, textRemoveProduces4);
        TextEdit editRemoveDisposes4 = te(0, 0, 62, 0, textRemoveDisposes4);
        TextEdit editRemoveObserves4 = te(0, 0, 62, 0, textRemoveObserves4);
        CodeAction actionRemoveProduces4 = ca(uri, "Remove @Produces", producesDisposesObs, editRemoveProduces4);
        CodeAction actionRemoveDisposes4 = ca(uri, "Remove the @Disposes modifier from parameter name1", producesDisposesObs, editRemoveDisposes4);
        CodeAction actionRemoveObserves4 = ca(uri, "Remove the @Observes modifier from parameter name2", producesDisposesObs, editRemoveObserves4);

        assertJavaCodeAction(paramsProducesDisposesObs, utils, actionRemoveProduces4, actionRemoveDisposes4, actionRemoveObserves4);

        JakartaJavaCodeActionParams paramsProducesObsAsync = createCodeActionParams(uri, producesObsAsync);

        String textRemoveProduces5 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n\n" +
                "import jakarta.enterprise.inject.Produces;\nimport jakarta.enterprise.inject.Disposes;\n" +
                "import jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n\n" +
                "@ApplicationScoped\npublic class ProducesAndDisposesObservesObservesAsync {\n    @Produces\n" +
                "    public String greetDisposes(@Disposes String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetObserves(@Observes String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetObservesAsync(@ObservesAsync String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n" +
                "    public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n" +
                "        return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n" +
                "    }\n    \n    \n    @Produces\n" +
                "    public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n\n"+
                "    public String greetObservesObservesAsyncConflict(@Observes @ObservesAsync String name, @Observes @ObservesAsync String name1) {\n" +
                "        return \"Hi \" + name + \" and \" + name1 + \"!\";\n    }\n}\n";
        String textRemoveObserves5 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.ApplicationScoped;\n\n" +
                "import jakarta.enterprise.inject.Produces;\nimport jakarta.enterprise.inject.Disposes;\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\n@ApplicationScoped\npublic class ProducesAndDisposesObservesObservesAsync {\n    @Produces\n    " +
                "public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObserves(@Observes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObservesAsync(@ObservesAsync String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObservesObservesAsync(String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n"+
                "    public String greetObservesObservesAsyncConflict(@Observes @ObservesAsync String name, @Observes @ObservesAsync String name1) {\n" +
                "        return \"Hi \" + name + \" and \" + name1 + \"!\";\n    }\n}\n";
        String textRemoveAsync5 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.ApplicationScoped;\n\n" +
                "import jakarta.enterprise.inject.Produces;\nimport jakarta.enterprise.inject.Disposes;\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\n@ApplicationScoped\npublic class ProducesAndDisposesObservesObservesAsync {\n    @Produces\n    " +
                "public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObserves(@Observes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObservesAsync(@ObservesAsync String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n"+
                "    public String greetObservesObservesAsyncConflict(@Observes @ObservesAsync String name, @Observes @ObservesAsync String name1) {\n" +
                "        return \"Hi \" + name + \" and \" + name1 + \"!\";\n    }\n}\n";

        TextEdit editRemoveProduces5 = te(0, 0, 62, 0, textRemoveProduces5);
        TextEdit editRemoveObserves5 = te(0, 0, 62, 0, textRemoveObserves5);
        TextEdit editRemoveAsync5 = te(0, 0, 62, 0, textRemoveAsync5);
        CodeAction actionRemoveProduces5 = ca(uri, "Remove @Produces", producesObsAsync, editRemoveProduces5);
        CodeAction actionRemoveObserves5 = ca(uri, "Remove the @Observes modifier from parameter name1", producesObsAsync, editRemoveObserves5);
        CodeAction actionRemoveAsync5 = ca(uri, "Remove the @ObservesAsync modifier from parameter name2", producesObsAsync, editRemoveAsync5);

        assertJavaCodeAction(paramsProducesObsAsync, utils, actionRemoveProduces5, actionRemoveObserves5, actionRemoveAsync5);

        JakartaJavaCodeActionParams paramsProducesAllThree = createCodeActionParams(uri, producesAllThree);

        String textRemoveProduces6 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.ApplicationScoped;\n\n" +
                "import jakarta.enterprise.inject.Produces;\nimport jakarta.enterprise.inject.Disposes;\n" +
                "import jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n\n" +
                "@ApplicationScoped\npublic class ProducesAndDisposesObservesObservesAsync {\n    @Produces\n    " +
                "public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    " +
                "@Produces\n    public String greetObserves(@Observes String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Produces\n    public String greetObservesAsync(@ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Produces\n    " +
                "public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Produces\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Produces\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n    \n    \n    " +
                "@Produces\n    public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n    public String greetObservesObservesAsyncConflict(@Observes @ObservesAsync String name, @Observes @ObservesAsync String name1) {\n        " +
                "return \"Hi \" + name + \" and \" + name1 + \"!\";\n    }\n}\n";
        String textRemoveDisposes6 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.ApplicationScoped;\n\n" +
                "import jakarta.enterprise.inject.Produces;\nimport jakarta.enterprise.inject.Disposes;\n" +
                "import jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n\n" +
                "@ApplicationScoped\npublic class ProducesAndDisposesObservesObservesAsync {\n    @Produces\n    " +
                "public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Produces\n    public String greetObserves(@Observes String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObservesAsync(@ObservesAsync String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Produces\n    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync(String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n    public String greetObservesObservesAsyncConflict(@Observes @ObservesAsync String name, @Observes @ObservesAsync String name1) {\n        " +
                "return \"Hi \" + name + \" and \" + name1 + \"!\";\n    }\n}\n";
        String textRemoveAsync6 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.ApplicationScoped;\n\n" +
                "import jakarta.enterprise.inject.Produces;\nimport jakarta.enterprise.inject.Disposes;\n" +
                "import jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n\n" +
                "@ApplicationScoped\npublic class ProducesAndDisposesObservesObservesAsync {\n    @Produces\n    " +
                "public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Produces\n    public String greetObserves(@Observes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Produces\n    public String greetObservesAsync(@ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n    public String greetObservesObservesAsyncConflict(@Observes @ObservesAsync String name, @Observes @ObservesAsync String name1) {\n        " +
                "return \"Hi \" + name + \" and \" + name1 + \"!\";\n    }\n}\n";
        String textRemoveObserves6 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.ApplicationScoped;\n\n" +
                "import jakarta.enterprise.inject.Produces;\nimport jakarta.enterprise.inject.Disposes;\n" +
                "import jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n\n" +
                "@ApplicationScoped\npublic class ProducesAndDisposesObservesObservesAsync {\n    " +
                "@Produces\n    public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Produces\n    public String greetObserves(@Observes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Produces\n    public String greetObservesAsync(@ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n    public String greetObservesObservesAsyncConflict(@Observes @ObservesAsync String name, @Observes @ObservesAsync String name1) {\n        " +
                "return \"Hi \" + name + \" and \" + name1 + \"!\";\n    }\n}\n";

        TextEdit editRemoveProduces6 = te(0, 0, 62, 0, textRemoveProduces6);
        TextEdit editRemoveDisposes6 = te(0, 0, 62, 0, textRemoveDisposes6);
        TextEdit editRemoveAsync6 = te(0, 0, 62, 0, textRemoveAsync6);
        TextEdit editRemoveObserves6 = te(0, 0, 62, 0, textRemoveObserves6);
        CodeAction actionRemoveProduces6 = ca(uri, "Remove @Produces", producesAllThree, editRemoveProduces6);
        CodeAction actionRemoveDisposes6 = ca(uri, "Remove the @Disposes modifier from parameter name1", producesAllThree, editRemoveDisposes6);
        CodeAction actionRemoveAsync6 = ca(uri, "Remove the @ObservesAsync modifier from parameter name3", producesAllThree, editRemoveAsync6);
        CodeAction actionRemoveObserves6 = ca(uri, "Remove the @Observes modifier from parameter name2", producesAllThree, editRemoveObserves6);

        assertJavaCodeAction(paramsProducesAllThree, utils, actionRemoveProduces6, actionRemoveDisposes6, actionRemoveObserves6, actionRemoveAsync6);

        JakartaJavaCodeActionParams paramsProducesDisposesAsync = createCodeActionParams(uri, producesDisposesAsync);

        String textRemoveProduces7 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.ApplicationScoped;\n\nimport jakarta.enterprise.inject.Produces;\n" +
                "import jakarta.enterprise.inject.Disposes;\nimport jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n\n" +
                "@ApplicationScoped\npublic class ProducesAndDisposesObservesObservesAsync {\n    @Produces\n    public String greetDisposes(@Disposes String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Produces\n    public String greetObserves(@Observes String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Produces\n    public String greetObservesAsync(@ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Produces\n    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Produces\n    public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Produces\n    public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n    \n    \n    @Produces\n    public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n    public String greetObservesObservesAsyncConflict(@Observes @ObservesAsync String name, @Observes @ObservesAsync String name1) {\n        return \"Hi \" + name + \" and \" + name1 + \"!\";\n    }\n}\n";
        String textRemoveDisposes7 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.ApplicationScoped;\n\nimport jakarta.enterprise.inject.Produces;\n" +
                "import jakarta.enterprise.inject.Disposes;\nimport jakarta.enterprise.event.Observes;\nimport jakarta.enterprise.event.ObservesAsync;\n\n" +
                "@ApplicationScoped\npublic class ProducesAndDisposesObservesObservesAsync {\n    @Produces\n    public String greetDisposes(@Disposes String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    public String greetObserves(@Observes String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    public String greetObservesAsync(@ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesAsync(String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n    " +
                "public String greetObservesObservesAsyncConflict(@Observes @ObservesAsync String name, @Observes @ObservesAsync String name1) {\n        " +
                "return \"Hi \" + name + \" and \" + name1 + \"!\";\n    }\n}\n";
        String textRemoveAsync7 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.ApplicationScoped;\n\n" +
                "import jakarta.enterprise.inject.Produces;\nimport jakarta.enterprise.inject.Disposes;\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\n@ApplicationScoped\npublic class ProducesAndDisposesObservesObservesAsync {\n    " +
                "@Produces\n    public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Produces\n    public String greetObserves(@Observes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Produces\n    public String greetObservesAsync(@ObservesAsync String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    " +
                "@Produces\n    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n    public String greetObservesObservesAsyncConflict(@Observes @ObservesAsync String name, @Observes @ObservesAsync String name1) {\n        " +
                "return \"Hi \" + name + \" and \" + name1 + \"!\";\n    }\n}\n";

        TextEdit editRemoveProduces7 = te(0, 0, 62, 0, textRemoveProduces7);
        TextEdit editRemoveDisposes7 = te(0, 0, 62, 0, textRemoveDisposes7);
        TextEdit editRemoveAsync7 = te(0, 0, 62, 0, textRemoveAsync7);
        CodeAction actionRemoveProduces7 = ca(uri, "Remove @Produces", producesDisposesAsync, editRemoveProduces7);
        CodeAction actionRemoveDisposes7 = ca(uri, "Remove the @Disposes modifier from parameter name1", producesDisposesAsync, editRemoveDisposes7);
        CodeAction actionRemoveAsync7 = ca(uri, "Remove the @ObservesAsync modifier from parameter name2", producesDisposesAsync, editRemoveAsync7);

        assertJavaCodeAction(paramsProducesDisposesAsync, utils, actionRemoveProduces7, actionRemoveDisposes7, actionRemoveAsync7);

        JakartaJavaCodeActionParams paramsProducesAllThree2 = createCodeActionParams(uri, producesAllThree2);

        String textRemoveProduces8 = "package io.openliberty.sample.jakarta.cdi;\n\n" +
                "import jakarta.enterprise.context.ApplicationScoped;\n\n" +
                "import jakarta.enterprise.inject.Produces;\n" +
                "import jakarta.enterprise.inject.Disposes;\n" +
                "import jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\n" +
                "@ApplicationScoped\npublic class ProducesAndDisposesObservesObservesAsync {\n    @Produces\n" +
                "    public String greetDisposes(@Disposes String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetObserves(@Observes String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetObservesAsync(@ObservesAsync String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n" +
                "        return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n    \n    \n    @Produces\n" +
                "    public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n" +
                "        return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n    \n    \n" +
                "    public String greetDisposesObservesObservesAsync2(@Disposes @Observes @ObservesAsync String name) {\n" +
                "        return \"Hi \" + name + \"!\";\n    }\n\n"+
                "    public String greetObservesObservesAsyncConflict(@Observes @ObservesAsync String name, @Observes @ObservesAsync String name1) {\n" +
                "        return \"Hi \" + name + \" and \" + name1 + \"!\";\n    }\n}\n";
        String textRemoveAllAnnotations8 = "package io.openliberty.sample.jakarta.cdi;\n\nimport jakarta.enterprise.context.ApplicationScoped;\n\n" +
                "import jakarta.enterprise.inject.Produces;\nimport jakarta.enterprise.inject.Disposes;\nimport jakarta.enterprise.event.Observes;\n" +
                "import jakarta.enterprise.event.ObservesAsync;\n\n@ApplicationScoped\npublic class ProducesAndDisposesObservesObservesAsync {\n    @Produces\n    " +
                "public String greetDisposes(@Disposes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObserves(@Observes String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObservesAsync(@ObservesAsync String name) {\n        return \"Hi \" + name + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObserves(@Disposes String name1, @Observes String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetObservesObservesAsync(@Observes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesAsync(@Disposes String name1, @ObservesAsync String name2) {\n        " +
                "return \"Hi \" + name1 + \" and \" + name2 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync(@Disposes String name1, @Observes String name2, @ObservesAsync String name3) {\n        " +
                "return \"Hi \" + name1 + \", \" + name2 + \" and \" + name3 + \"!\";\n    }\n\n\n    @Produces\n    " +
                "public String greetDisposesObservesObservesAsync2(String name) {\n        " +
                "return \"Hi \" + name + \"!\";\n    }\n\n"+
                "    public String greetObservesObservesAsyncConflict(@Observes @ObservesAsync String name, @Observes @ObservesAsync String name1) {\n" +
                "        return \"Hi \" + name + \" and \" + name1 + \"!\";\n    }\n}\n";
        TextEdit editRemoveProduces8 = te(0, 0, 62, 0, textRemoveProduces8);
        TextEdit editRemoveAllAnnotations8 = te(0, 0, 62, 0, textRemoveAllAnnotations8);
        CodeAction actionRemoveProduces8 = ca(uri, "Remove @Produces", producesAllThree2, editRemoveProduces8);
        CodeAction actionRemoveAllAnnotations8 = ca(uri, "Remove the @Disposes, @Observes, @ObservesAsync modifier from parameter name", producesAllThree2, editRemoveAllAnnotations8);

        assertJavaCodeAction(paramsProducesAllThree2, utils, actionRemoveProduces8, actionRemoveAllAnnotations8);
    }

    @Test
    public void multipleDisposes() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/MultipleDisposes.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();
        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));
        Diagnostic d = d(9, 18, 23,
                "The @Disposes annotation must not be defined on more than one parameter of a method.",
                DiagnosticSeverity.Error, "jakarta-cdi", "RemoveExtraDisposes");
        assertJavaDiagnostics(diagnosticsParams, utils, d);
    }
}
