/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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

package io.openliberty.tools.intellij.lsp4jakarta.it.di;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.lsp4jakarta.it.core.BaseJakartaTest;
import io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;

@RunWith(JUnit4.class)
public class DependencyInjectionTest extends BaseJakartaTest {

    @Test
    public void DependencyInjectionDiagnostics() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/di/GreetingServlet.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        /* create expected diagnostics
         * 
         */
        Diagnostic d1 = JakartaForJavaAssert.d(17, 27, 35, "The @Inject annotation must not define a final field.",
                DiagnosticSeverity.Error, "jakarta-di", "RemoveInjectOrFinal");
        d1.setData("io.openliberty.sample.jakarta.di.Greeting");

        Diagnostic d2 = JakartaForJavaAssert.d(33, 25, 39, "The @Inject annotation must not define an abstract method.",
                DiagnosticSeverity.Error, "jakarta-di", "RemoveInjectOrAbstract");
        d2.setData("void");
        
        Diagnostic d3 = JakartaForJavaAssert.d(26, 22, 33, "The @Inject annotation must not define a final method.",
                DiagnosticSeverity.Error, "jakarta-di", "RemoveInjectOrFinal");
        d3.setData("void");
 
        Diagnostic d4 = JakartaForJavaAssert.d(43, 23, 36, "The @Inject annotation must not define a generic method.",
                DiagnosticSeverity.Error, "jakarta-di", "RemoveInjectForGeneric");
        d4.setData("java.util.List<T>");
        
        Diagnostic d5 = JakartaForJavaAssert.d(37, 23, 35, "The @Inject annotation must not define a static method.",
                DiagnosticSeverity.Error, "jakarta-di", "RemoveInjectOrStatic");
        d5.setData("void");
        

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3, d4, d5);


        JakartaJavaCodeActionParams codeActionParams = JakartaForJavaAssert.createCodeActionParams(uri, d1);
        String textD1 = "package io.openliberty.sample.jakarta.di;\n\n" +
                "import jakarta.inject.Inject;\n" +
                "import jakarta.enterprise.inject.Produces;\n\n" +
                "import java.util.ArrayList;\n" +
                "import java.util.List;\n\n" +
                "public abstract class GreetingServlet {\n\n    " +
                "/**\n     *\n     */\n    " +
                "private static final long serialVersionUID = 1L;\n\n    " +
                "// d1: test code for @Inject fields cannot be final\n    " +
                "@Inject\n    private Greeting greeting = new Greeting();\n\n    " +
                "@Produces\n    " +
                "public GreetingNoDefaultConstructor getInstance() {\n        " +
                "return new GreetingNoDefaultConstructor(\"Howdy\");\n    }\n\n    " +
                "// d2\n    " +
                "@Inject\n    public final void injectFinal() {\n        " +
                "// test code for @Inject methods cannot be final\n        " +
                "return;\n    }\n\n    " +
                "// d3: test code for @Inject methods cannot be abstract\n    " +
                "@Inject\n    public abstract void injectAbstract();\n\n    " +
                "// d4: test code for @Inject methods cannot be static\n    " +
                "@Inject\n    " +
                "public static void injectStatic() {\n        " +
                "return;\n    }\n\n    " +
                "// d5: test code for @Inject methods cannot be generic\n    " +
                "@Inject\n    " +
                "public <T> List<T> injectGeneric(T arg) {\n        " +
                "// do nothing\n        " +
                "return new ArrayList<T>();\n    " +
                "};\n\n}\n";

        String newText = "package io.openliberty.sample.jakarta.di;\n\n" +
                "import jakarta.inject.Inject;\nimport jakarta.enterprise.inject.Produces;\n\n" +
                "import java.util.ArrayList;\nimport java.util.List;\n\n" +
                "public abstract class GreetingServlet {\n\n    /**\n     *\n" +
                "     */\n    private static final long serialVersionUID = 1L;\n\n" +
                "    // d1: test code for @Inject fields cannot be final\n" +
                "    private final Greeting greeting = new Greeting();\n\n" +
                "    @Produces\n    public GreetingNoDefaultConstructor getInstance() {\n" +
                "        return new GreetingNoDefaultConstructor(\"Howdy\");\n    }\n\n" +
                "    // d2\n    @Inject\n    public final void injectFinal() {\n" +
                "        // test code for @Inject methods cannot be final\n" +
                "        return;\n    }\n\n    // d3: test code for @Inject methods cannot be abstract\n" +
                "    @Inject\n    public abstract void injectAbstract();\n\n" +
                "    // d4: test code for @Inject methods cannot be static\n    @Inject\n" +
                "    public static void injectStatic() {\n        return;\n    }\n\n" +
                "    // d5: test code for @Inject methods cannot be generic\n    @Inject\n" +
                "    public <T> List<T> injectGeneric(T arg) {\n" +
                "        // do nothing\n        return new ArrayList<T>();\n    };\n\n}\n";

        TextEdit te = JakartaForJavaAssert.te(0, 0, 49, 0, newText);
        TextEdit te1 = JakartaForJavaAssert.te(0, 0, 49, 0, textD1);
        CodeAction ca = JakartaForJavaAssert.ca(uri, "Remove @Inject", d1, te);
        CodeAction ca1 = JakartaForJavaAssert.ca(uri, "Remove the 'final' modifier from this field", d1, te1);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams, utils, ca, ca1);


        String textD2 = "package io.openliberty.sample.jakarta.di;\n\n" +
                "import jakarta.inject.Inject;\n" +
                "import jakarta.enterprise.inject.Produces;\n\n" +
                "import java.util.ArrayList;\n" +
                "import java.util.List;\n\n" +
                "public abstract class GreetingServlet {\n\n    " +
                "/**\n     *\n     */\n    " +
                "private static final long serialVersionUID = 1L;\n\n    " +
                "// d1: test code for @Inject fields cannot be final\n    " +
                "@Inject\n    private final Greeting greeting = new Greeting();\n\n    " +
                "@Produces\n    public GreetingNoDefaultConstructor getInstance() {\n        " +
                "return new GreetingNoDefaultConstructor(\"Howdy\");\n    }\n\n    " +
                "// d2\n    @Inject\n    public final void injectFinal() {\n        " +
                "// test code for @Inject methods cannot be final\n        " +
                "return;\n    }\n\n    " +
                "// d3: test code for @Inject methods cannot be abstract\n    " +
                "@Inject\n    public void injectAbstract();\n\n    " +
                "// d4: test code for @Inject methods cannot be static\n    " +
                "@Inject\n    public static void injectStatic() {\n        " +
                "return;\n    }\n\n    " +
                "// d5: test code for @Inject methods cannot be generic\n    " +
                "@Inject\n    " +
                "public <T> List<T> injectGeneric(T arg) {\n        " +
                "// do nothing\n        " +
                "return new ArrayList<T>();\n    " +
                "};\n\n}\n";

        String newText1 = "package io.openliberty.sample.jakarta.di;\n\n" +
                "import jakarta.inject.Inject;\nimport jakarta.enterprise.inject.Produces;\n\n" +
                "import java.util.ArrayList;\nimport java.util.List;\n\n" +
                "public abstract class GreetingServlet {\n\n    /**\n     *\n     */\n" +
                "    private static final long serialVersionUID = 1L;\n\n" +
                "    // d1: test code for @Inject fields cannot be final\n    @Inject\n" +
                "    private final Greeting greeting = new Greeting();\n\n    @Produces\n" +
                "    public GreetingNoDefaultConstructor getInstance() {\n" +
                "        return new GreetingNoDefaultConstructor(\"Howdy\");\n    }\n\n" +
                "    // d2\n    @Inject\n    public final void injectFinal() {\n" +
                "        // test code for @Inject methods cannot be final\n        return;\n" +
                "    }\n\n    // d3: test code for @Inject methods cannot be abstract\n" +
                "    public abstract void injectAbstract();\n\n" +
                "    // d4: test code for @Inject methods cannot be static\n    @Inject\n" +
                "    public static void injectStatic() {\n        return;\n    }\n\n" +
                "    // d5: test code for @Inject methods cannot be generic\n    @Inject\n" +
                "    public <T> List<T> injectGeneric(T arg) {\n        // do nothing\n" +
                "        return new ArrayList<T>();\n    };\n\n}\n";

        codeActionParams = JakartaForJavaAssert.createCodeActionParams(uri, d2);
        te = JakartaForJavaAssert.te(0, 0, 49, 0, newText1);
        te1 = JakartaForJavaAssert.te(0, 0, 49, 0, textD2);
        ca = JakartaForJavaAssert.ca(uri, "Remove @Inject", d2, te);
        ca1 = JakartaForJavaAssert.ca(uri, "Remove the 'abstract' modifier from this method", d2, te1);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams, utils, ca, ca1);


        String textD3 = "package io.openliberty.sample.jakarta.di;\n\n" +
                "import jakarta.inject.Inject;\n" +
                "import jakarta.enterprise.inject.Produces;\n\n" +
                "import java.util.ArrayList;\n" +
                "import java.util.List;\n\n" +
                "public abstract class GreetingServlet {\n\n    " +
                "/**\n     *\n     */\n    " +
                "private static final long serialVersionUID = 1L;\n\n" +
                "    // d1: test code for @Inject fields cannot be final\n    " +
                "@Inject\n    private final Greeting greeting = new Greeting();\n\n    " +
                "@Produces\n    " +
                "public GreetingNoDefaultConstructor getInstance() {\n        " +
                "return new GreetingNoDefaultConstructor(\"Howdy\");\n    " +
                "}\n\n    " +
                "// d2\n    @Inject\n    public void injectFinal() {\n        " +
                "// test code for @Inject methods cannot be final\n        " +
                "return;\n    }\n\n    " +
                "// d3: test code for @Inject methods cannot be abstract\n    " +
                "@Inject\n    public abstract void injectAbstract();\n\n    " +
                "// d4: test code for @Inject methods cannot be static\n    " +
                "@Inject\n    public static void injectStatic() {\n        " +
                "return;\n    }\n\n    " +
                "// d5: test code for @Inject methods cannot be generic\n    " +
                "@Inject\n    " +
                "public <T> List<T> injectGeneric(T arg) {\n        " +
                "// do nothing\n        " +
                "return new ArrayList<T>();\n    };\n\n}\n";

        String newText2 = "package io.openliberty.sample.jakarta.di;\n\nimport jakarta.inject.Inject;\n" +
                "import jakarta.enterprise.inject.Produces;\n\nimport java.util.ArrayList;\n" +
                "import java.util.List;\n\npublic abstract class GreetingServlet {\n\n    /**\n     *\n" +
                "     */\n    private static final long serialVersionUID = 1L;\n\n" +
                "    // d1: test code for @Inject fields cannot be final\n    @Inject\n" +
                "    private final Greeting greeting = new Greeting();\n\n    @Produces\n" +
                "    public GreetingNoDefaultConstructor getInstance() {\n" +
                "        return new GreetingNoDefaultConstructor(\"Howdy\");\n    }\n\n    // d2\n" +
                "    public final void injectFinal() {\n" +
                "        // test code for @Inject methods cannot be final\n        return;\n" +
                "    }\n\n    // d3: test code for @Inject methods cannot be abstract\n    @Inject\n" +
                "    public abstract void injectAbstract();\n\n" +
                "    // d4: test code for @Inject methods cannot be static\n    @Inject\n" +
                "    public static void injectStatic() {\n        return;\n    }\n\n" +
                "    // d5: test code for @Inject methods cannot be generic\n    @Inject\n" +
                "    public <T> List<T> injectGeneric(T arg) {\n        // do nothing\n" +
                "        return new ArrayList<T>();\n    };\n\n}\n";

        codeActionParams = JakartaForJavaAssert.createCodeActionParams(uri, d3);
        te = JakartaForJavaAssert.te(0, 0, 49, 0, newText2);
        te1 = JakartaForJavaAssert.te(0, 0, 49, 0, textD3);
        ca = JakartaForJavaAssert.ca(uri, "Remove @Inject", d3, te);
        ca1 = JakartaForJavaAssert.ca(uri, "Remove the 'final' modifier from this method", d3, te1);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams, utils, ca, ca1);


        String newText3 = "package io.openliberty.sample.jakarta.di;\n\nimport jakarta.inject.Inject;\n" +
                "import jakarta.enterprise.inject.Produces;\n\nimport java.util.ArrayList;\n" +
                "import java.util.List;\n\npublic abstract class GreetingServlet {\n\n    /**\n     *\n     */\n" +
                "    private static final long serialVersionUID = 1L;\n\n" +
                "    // d1: test code for @Inject fields cannot be final\n    @Inject\n" +
                "    private final Greeting greeting = new Greeting();\n\n    @Produces\n" +
                "    public GreetingNoDefaultConstructor getInstance() {\n" +
                "        return new GreetingNoDefaultConstructor(\"Howdy\");\n    }\n\n" +
                "    // d2\n    @Inject\n    public final void injectFinal() {\n" +
                "        // test code for @Inject methods cannot be final\n" +
                "        return;\n    }\n\n    // d3: test code for @Inject methods cannot be abstract\n" +
                "    @Inject\n    public abstract void injectAbstract();\n\n" +
                "    // d4: test code for @Inject methods cannot be static\n    @Inject\n" +
                "    public static void injectStatic() {\n        return;\n    }\n\n" +
                "    // d5: test code for @Inject methods cannot be generic\n" +
                "    public <T> List<T> injectGeneric(T arg) {\n        // do nothing\n" +
                "        return new ArrayList<T>();\n    };\n\n}\n";

        codeActionParams = JakartaForJavaAssert.createCodeActionParams(uri, d4);
        te = JakartaForJavaAssert.te(0, 0, 49, 0, newText3);
        ca = JakartaForJavaAssert.ca(uri, "Remove @Inject", d4, te);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams, utils, ca);

        String textD5 = "package io.openliberty.sample.jakarta.di;\n\n" +
                "import jakarta.inject.Inject;\n" +
                "import jakarta.enterprise.inject.Produces;\n\n" +
                "import java.util.ArrayList;\nimport java.util.List;\n\n" +
                "public abstract class GreetingServlet " +
                "{\n\n    /**\n     *\n     */\n    " +
                "private static final long serialVersionUID = 1L;\n\n    " +
                "// d1: test code for @Inject fields cannot be final\n    " +
                "@Inject\n    " +
                "private final Greeting greeting = new Greeting();\n\n    " +
                "@Produces\n    " +
                "public GreetingNoDefaultConstructor getInstance() {\n        " +
                "return new GreetingNoDefaultConstructor(\"Howdy\");\n    }\n\n    " +
                "// d2\n    @Inject\n    " +
                "public final void injectFinal() {\n        " +
                "// test code for @Inject methods cannot be final\n        " +
                "return;\n    }\n\n    " +
                "// d3: test code for @Inject methods cannot be abstract\n    " +
                "@Inject\n    " +
                "public abstract void injectAbstract();\n\n    " +
                "// d4: test code for @Inject methods cannot be static\n    " +
                "@Inject\n    public void injectStatic() {\n        " +
                "return;\n    }\n\n    " +
                "// d5: test code for @Inject methods cannot be generic\n    " +
                "@Inject\n    " +
                "public <T> List<T> injectGeneric(T arg) {\n        " +
                "// do nothing\n        return new ArrayList<T>();\n    };\n\n}\n";

        String newText4 = "package io.openliberty.sample.jakarta.di;\n\nimport jakarta.inject.Inject;\n" +
                "import jakarta.enterprise.inject.Produces;\n\nimport java.util.ArrayList;\n" +
                "import java.util.List;\n\n" +
                "public abstract class GreetingServlet {\n\n    /**\n     *\n     */\n" +
                "    private static final long serialVersionUID = 1L;\n\n" +
                "    // d1: test code for @Inject fields cannot be final\n" +
                "    @Inject\n    private final Greeting greeting = new Greeting();\n\n    @Produces\n" +
                "    public GreetingNoDefaultConstructor getInstance() {\n" +
                "        return new GreetingNoDefaultConstructor(\"Howdy\");\n    }\n\n    // d2\n    @Inject\n" +
                "    public final void injectFinal() {\n        // test code for @Inject methods cannot be final\n" +
                "        return;\n    }\n\n    // d3: test code for @Inject methods cannot be abstract\n    @Inject\n" +
                "    public abstract void injectAbstract();\n\n" +
                "    // d4: test code for @Inject methods cannot be static\n    public static void injectStatic() {\n" +
                "        return;\n    }\n\n    // d5: test code for @Inject methods cannot be generic\n    @Inject\n" +
                "    public <T> List<T> injectGeneric(T arg) {\n        // do nothing\n" +
                "        return new ArrayList<T>();\n    };\n\n}\n";

        codeActionParams = JakartaForJavaAssert.createCodeActionParams(uri, d5);
        te = JakartaForJavaAssert.te(0, 0, 49, 0, newText4);
        te1 = JakartaForJavaAssert.te(0, 0, 49, 0, textD5);
        ca = JakartaForJavaAssert.ca(uri, "Remove @Inject", d5, te);
        ca1 = JakartaForJavaAssert.ca(uri, "Remove the 'static' modifier from this method", d5, te1);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams, utils, ca, ca1);

    }
}
