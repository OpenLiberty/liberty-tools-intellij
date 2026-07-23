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

@RunWith(JUnit4.class)
public class SessionBeanModifierTest extends BaseJakartaTest {

    private static final String EJB_SESSIONBEAN_PATH =
            "/src/main/java/io/openliberty/sample/jakarta/ejb/sessionbean/";

    private String getUri(Module module, String fileName) {
        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(
                ModuleUtilCore.getModuleDirPath(module) + EJB_SESSIONBEAN_PATH + fileName);
        return VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();
    }

    /**
     * NotPublicStatelessBean.java — line 6 (0-based):
     *   "class NotPublicStatelessBean {"
     *   → name "NotPublicStatelessBean" col 6..28 → InvalidModifierNotPublic
     */
    @Test
    public void testNotPublicStatelessBean() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(getUri(module, "NotPublicStatelessBean.java")));

        Diagnostic notPublic = d(6, 6, 28,
                "A session bean class must be declared public.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidModifierNotPublic");

        assertJavaDiagnostics(diagnosticsParams, utils, notPublic);

        String uri = getUri(module, "NotPublicStatelessBean.java");
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, notPublic);
        String makePublicText = "package io.openliberty.sample.jakarta.ejb.classconstraints;\n\nimport jakarta.ejb.Stateless;\n\n// Invalid: session bean class is not declared public (package-private).\n@Stateless\npublic\nclass NotPublicStatelessBean {\n    public NotPublicStatelessBean() {\n    }\n}\n";
        String removeAnnotationText = "package io.openliberty.sample.jakarta.ejb.classconstraints;\n\nimport jakarta.ejb.Stateless;\n\n// Invalid: session bean class is not declared public (package-private).\nclass NotPublicStatelessBean {\n    public NotPublicStatelessBean() {\n    }\n}\n";
        TextEdit makePublic = te(0, 0, 10, 0, makePublicText);
        TextEdit removeAnnotation = te(0, 0, 10, 0, removeAnnotationText);
        CodeAction makePublicAction = ca(uri, "Change modifier to public", notPublic, makePublic);
        CodeAction removeAnnotationAction = ca(uri, "Remove @Stateless", notPublic, removeAnnotation);
        assertJavaCodeAction(codeActionParams, utils, makePublicAction, removeAnnotationAction);
    }

    /**
     * FinalStatelessBean.java — line 6 (0-based):
     *   "public final class FinalStatelessBean {"
     *   → name "FinalStatelessBean" col 19..37 → InvalidModifierFinal
     */
    @Test
    public void testFinalStatelessBean() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(getUri(module, "FinalStatelessBean.java")));

        Diagnostic isFinal = d(6, 19, 37,
                "A session bean class must not be declared final.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidModifierFinal");

        assertJavaDiagnostics(diagnosticsParams, utils, isFinal);

        String uri = getUri(module, "FinalStatelessBean.java");
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, isFinal);
        // "Remove @Stateless" removes the annotation line; file goes from 10 to 9 lines but range covers full original (0,0)-(10,0)
        String removeStatelessText = "package io.openliberty.sample.jakarta.ejb.classconstraints;\n\nimport jakarta.ejb.Stateless;\n\n// Invalid: session bean class is declared final.\npublic final class FinalStatelessBean {\n    public FinalStatelessBean() {\n    }\n}\n";
        // "Remove the 'final' modifier" removes "final " from the class declaration; @Stateless annotation is retained
        String removeFinalText = "package io.openliberty.sample.jakarta.ejb.classconstraints;\n\nimport jakarta.ejb.Stateless;\n\n// Invalid: session bean class is declared final.\n@Stateless\npublic class FinalStatelessBean {\n    public FinalStatelessBean() {\n    }\n}\n";
        TextEdit removeAnnotation = te(0, 0, 10, 0, removeStatelessText);
        TextEdit removeFinal = te(0, 0, 10, 0, removeFinalText);
        CodeAction removeAnnotationAction = ca(uri, "Remove @Stateless", isFinal, removeAnnotation);
        CodeAction removeFinalAction = ca(uri, "Remove the 'final' modifier from this class", isFinal, removeFinal);
        assertJavaCodeAction(codeActionParams, utils, removeAnnotationAction, removeFinalAction);
    }

    /**
     * AbstractStatefulBean.java — line 6 (0-based):
     *   "public abstract class AbstractStatefulBean {"
     *   → name "AbstractStatefulBean" col 22..42 → InvalidModifierAbstract
     */
    @Test
    public void testAbstractStatefulBean() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(getUri(module, "AbstractStatefulBean.java")));

        Diagnostic isAbstract = d(6, 22, 42,
                "A session bean class must not be declared abstract.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidModifierAbstract");

        assertJavaDiagnostics(diagnosticsParams, utils, isAbstract);

        String uri = getUri(module, "AbstractStatefulBean.java");
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, isAbstract);
        String removeStatefulText = "package io.openliberty.sample.jakarta.ejb.classconstraints;\n\nimport jakarta.ejb.Stateful;\n\n// Invalid: session bean class is declared abstract.\npublic abstract class AbstractStatefulBean {\n    public AbstractStatefulBean() {\n    }\n}\n";
        // "Remove the 'abstract' modifier" removes "abstract " from the class declaration; @Stateful annotation is retained
        String removeAbstractText = "package io.openliberty.sample.jakarta.ejb.classconstraints;\n\nimport jakarta.ejb.Stateful;\n\n// Invalid: session bean class is declared abstract.\n@Stateful\npublic class AbstractStatefulBean {\n    public AbstractStatefulBean() {\n    }\n}\n";
        TextEdit removeAnnotation = te(0, 0, 10, 0, removeStatefulText);
        TextEdit removeAbstract = te(0, 0, 10, 0, removeAbstractText);
        CodeAction removeAnnotationAction = ca(uri, "Remove @Stateful", isAbstract, removeAnnotation);
        CodeAction removeAbstractAction = ca(uri, "Remove the 'abstract' modifier from this class", isAbstract, removeAbstract);
        assertJavaCodeAction(codeActionParams, utils, removeAnnotationAction, removeAbstractAction);
    }

    /**
     * FinalNotPublicSingletonBean.java — line 6 (0-based):
     *   "public final class FinalNotPublicSingletonBean {"
     *   → name "FinalNotPublicSingletonBean" col 19..46 → InvalidModifierFinal
     */
    @Test
    public void testFinalNotPublicSingletonBean() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(getUri(module, "FinalNotPublicSingletonBean.java")));

        Diagnostic isFinal = d(6, 19, 46,
                "A session bean class must not be declared final.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidModifierFinal");

        assertJavaDiagnostics(diagnosticsParams, utils, isFinal);

        String uri = getUri(module, "FinalNotPublicSingletonBean.java");
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, isFinal);
        String removeSingletonText = "package io.openliberty.sample.jakarta.ejb.classconstraints;\n\nimport jakarta.ejb.Singleton;\n\n// Invalid: session bean class is declared final.\npublic final class FinalNotPublicSingletonBean {\n    public FinalNotPublicSingletonBean() {\n    }\n}\n";
        // "Remove the 'final' modifier" removes "final " from the class declaration; @Singleton annotation is retained
        String removeFinalText = "package io.openliberty.sample.jakarta.ejb.classconstraints;\n\nimport jakarta.ejb.Singleton;\n\n// Invalid: session bean class is declared final.\n@Singleton\npublic class FinalNotPublicSingletonBean {\n    public FinalNotPublicSingletonBean() {\n    }\n}\n";
        TextEdit removeAnnotation = te(0, 0, 10, 0, removeSingletonText);
        TextEdit removeFinal = te(0, 0, 10, 0, removeFinalText);
        CodeAction removeAnnotationAction = ca(uri, "Remove @Singleton", isFinal, removeAnnotation);
        CodeAction removeFinalAction = ca(uri, "Remove the 'final' modifier from this class", isFinal, removeFinal);
        assertJavaCodeAction(codeActionParams, utils, removeAnnotationAction, removeFinalAction);
    }

    /**
     * NestedSessionBeanWrapper.java — line 12 (0-based):
     *   "    public class NestedStatefulBean {"
     *   → name "NestedStatefulBean" col 17..35 → InvalidNotTopLevelClass
     */
    @Test
    public void testNestedSessionBeanNotTopLevel() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(getUri(module, "NestedSessionBeanWrapper.java")));

        Diagnostic notTopLevel = d(12, 17, 35,
                "A session bean class must be a top-level class.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidNotTopLevelClass");

        assertJavaDiagnostics(diagnosticsParams, utils, notTopLevel);

        String uri = getUri(module, "NestedSessionBeanWrapper.java");
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, notTopLevel);
        // NestedSessionBeanWrapper.java has 17 lines; "Remove @Stateful" removes the annotation from the inner class
        String removeStatefulText = "package io.openliberty.sample.jakarta.ejb.classconstraints;\n\nimport jakarta.ejb.Stateful;\n\n/**\n * Test resource for the top-level class constraint.\n * The inner class NestedStatefulBean is annotated with @Stateful but is not\n * a top-level class, which violates Jakarta Enterprise Beans 4.0 spec section 4.1.\n */\npublic class NestedSessionBeanWrapper {\n\n    public class NestedStatefulBean {\n        public NestedStatefulBean() {\n        }\n    }\n}\n";
        TextEdit removeAnnotation = te(0, 0, 17, 0, removeStatefulText);
        CodeAction removeAnnotationAction = ca(uri, "Remove @Stateful", notTopLevel, removeAnnotation);
        assertJavaCodeAction(codeActionParams, utils, removeAnnotationAction);
    }

    /**
     * Valid session beans — no diagnostics expected for any of the three files.
     */
    @Test
    public void testValidSessionBeans() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        for (String fileName : new String[]{
                "ValidStatelessBeanExplicit.java",
                "ValidStatefulBeanNoConstructor.java",
                "ValidSingletonBeanExplicit.java"}) {
            JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
            diagnosticsParams.setUris(Arrays.asList(getUri(module, fileName)));
            assertJavaDiagnostics(diagnosticsParams, utils);
        }
    }
}
