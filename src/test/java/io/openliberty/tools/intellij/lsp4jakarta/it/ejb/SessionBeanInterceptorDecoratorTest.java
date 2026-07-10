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

/**
 * Tests for session beans annotated with @Interceptor or @Decorator
 */
@RunWith(JUnit4.class)
public class SessionBeanInterceptorDecoratorTest extends BaseJakartaTest {

    @Test
    public void testInvalidSessionBeanWithInterceptorOrDecoratorDiagnosticsAndQuickFixes() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/SessionBeanInterceptorDecorator.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // All 6 invalid session beans should trigger diagnostics
        // Additionally, @Decorator annotated classes also trigger CDI diagnostics about missing @Delegate
        Diagnostic statelessWithInterceptor = d(11, 13, 44,
                "Session beans cannot be annotated with @Interceptor or @Decorator.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidSessionBeanWithInterceptorOrDecorator");

        Diagnostic statelessWithDecorator = d(17, 6, 28,
                "Session beans cannot be annotated with @Interceptor or @Decorator.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidSessionBeanWithInterceptorOrDecorator");

        Diagnostic statelessWithDecoratorCDI = d(17, 6, 28,
                "A decorator must declare exactly one injection point annotated with @Delegate.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidDecoratorDelegateInjectionPoints");

        Diagnostic statefulWithInterceptor = d(23, 6, 29,
                "Session beans cannot be annotated with @Interceptor or @Decorator.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidSessionBeanWithInterceptorOrDecorator");

        Diagnostic statefulWithDecorator = d(29, 6, 27,
                "Session beans cannot be annotated with @Interceptor or @Decorator.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidSessionBeanWithInterceptorOrDecorator");

        Diagnostic statefulWithDecoratorCDI = d(29, 6, 27,
                "A decorator must declare exactly one injection point annotated with @Delegate.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidDecoratorDelegateInjectionPoints");

        Diagnostic singletonWithInterceptor = d(35, 6, 30,
                "Session beans cannot be annotated with @Interceptor or @Decorator.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidSessionBeanWithInterceptorOrDecorator");

        Diagnostic singletonWithDecorator = d(41, 6, 28,
                "Session beans cannot be annotated with @Interceptor or @Decorator.",
                DiagnosticSeverity.Error, "jakarta-ejb", "InvalidSessionBeanWithInterceptorOrDecorator");

        Diagnostic singletonWithDecoratorCDI = d(41, 6, 28,
                "A decorator must declare exactly one injection point annotated with @Delegate.",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidDecoratorDelegateInjectionPoints");

        assertJavaDiagnostics(diagnosticsParams, utils, statelessWithInterceptor,
                statelessWithDecoratorCDI, statelessWithDecorator,
                statefulWithInterceptor,
                statefulWithDecoratorCDI, statefulWithDecorator,
                singletonWithInterceptor,
                singletonWithDecoratorCDI, singletonWithDecorator);

        // Quick Fix 1a: Remove @Interceptor from @Stateless bean
        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, statelessWithInterceptor);
        String removeInterceptorFromStateless = "package io.openliberty.sample.jakarta.ejb;\n\n" +
                "import jakarta.ejb.Stateless;\n" +
                "import jakarta.ejb.Stateful;\n" +
                "import jakarta.ejb.Singleton;\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.decorator.Decorator;\n\n" +
                "// Test case 1: Stateless with @Interceptor - should report error\n" +
                "@Stateless\n" +
                "public class SessionBeanInterceptorDecorator {\n" +
                "}\n\n" +
                "// Test case 2: Stateless with @Decorator - should report error\n" +
                "@Stateless\n" +
                "@Decorator\n" +
                "class StatelessWithDecorator {\n" +
                "}\n\n" +
                "// Test case 3: Stateful with @Interceptor - should report error\n" +
                "@Stateful\n" +
                "@Interceptor\n" +
                "class StatefulWithInterceptor {\n" +
                "}\n\n" +
                "// Test case 4: Stateful with @Decorator - should report error\n" +
                "@Stateful\n" +
                "@Decorator\n" +
                "class StatefulWithDecorator {\n" +
                "}\n\n" +
                "// Test case 5: Singleton with @Interceptor - should report error\n" +
                "@Singleton\n" +
                "@Interceptor\n" +
                "class SingletonWithInterceptor {\n" +
                "}\n\n" +
                "// Test case 6: Singleton with @Decorator - should report error\n" +
                "@Singleton\n" +
                "@Decorator\n" +
                "class SingletonWithDecorator {\n" +
                "}\n\n" +
                "// Test case 7: Valid Stateless without @Interceptor or @Decorator - should NOT report error\n" +
                "@Stateless\n" +
                "class ValidStatelessBeanNoConflict {\n" +
                "}\n";
        TextEdit removeInterceptorEdit1 = te(0, 0, 48, 0, removeInterceptorFromStateless);
        CodeAction removeInterceptorAction1 = ca(uri, "Remove @Interceptor", statelessWithInterceptor, removeInterceptorEdit1);
        
        // Quick Fix 1b: Remove @Stateless from bean with @Interceptor
        String removeStatelessFromInterceptor = "package io.openliberty.sample.jakarta.ejb;\n\n" +
                "import jakarta.ejb.Stateless;\n" +
                "import jakarta.ejb.Stateful;\n" +
                "import jakarta.ejb.Singleton;\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.decorator.Decorator;\n\n" +
                "// Test case 1: Stateless with @Interceptor - should report error\n" +
                "@Interceptor\n" +
                "public class SessionBeanInterceptorDecorator {\n" +
                "}\n\n" +
                "// Test case 2: Stateless with @Decorator - should report error\n" +
                "@Stateless\n" +
                "@Decorator\n" +
                "class StatelessWithDecorator {\n" +
                "}\n\n" +
                "// Test case 3: Stateful with @Interceptor - should report error\n" +
                "@Stateful\n" +
                "@Interceptor\n" +
                "class StatefulWithInterceptor {\n" +
                "}\n\n" +
                "// Test case 4: Stateful with @Decorator - should report error\n" +
                "@Stateful\n" +
                "@Decorator\n" +
                "class StatefulWithDecorator {\n" +
                "}\n\n" +
                "// Test case 5: Singleton with @Interceptor - should report error\n" +
                "@Singleton\n" +
                "@Interceptor\n" +
                "class SingletonWithInterceptor {\n" +
                "}\n\n" +
                "// Test case 6: Singleton with @Decorator - should report error\n" +
                "@Singleton\n" +
                "@Decorator\n" +
                "class SingletonWithDecorator {\n" +
                "}\n\n" +
                "// Test case 7: Valid Stateless without @Interceptor or @Decorator - should NOT report error\n" +
                "@Stateless\n" +
                "class ValidStatelessBeanNoConflict {\n" +
                "}\n";
        TextEdit removeStatelessEdit1b = te(0, 0, 48, 0, removeStatelessFromInterceptor);
        CodeAction removeStatelessAction1b = ca(uri, "Remove @Stateless", statelessWithInterceptor, removeStatelessEdit1b);
        
        assertJavaCodeAction(codeActionParams1, utils, removeInterceptorAction1, removeStatelessAction1b);

        // Quick Fix 2: Remove @Decorator from @Stateless bean
        JakartaJavaCodeActionParams codeActionParams2 = createCodeActionParams(uri, statelessWithDecorator);
        String removeDecoratorFromStateless = "package io.openliberty.sample.jakarta.ejb;\n\n" +
                "import jakarta.ejb.Stateless;\n" +
                "import jakarta.ejb.Stateful;\n" +
                "import jakarta.ejb.Singleton;\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.decorator.Decorator;\n\n" +
                "// Test case 1: Stateless with @Interceptor - should report error\n" +
                "@Stateless\n" +
                "@Interceptor\n" +
                "public class SessionBeanInterceptorDecorator {\n" +
                "}\n\n" +
                "// Test case 2: Stateless with @Decorator - should report error\n" +
                "@Stateless\n" +
                "class StatelessWithDecorator {\n" +
                "}\n\n" +
                "// Test case 3: Stateful with @Interceptor - should report error\n" +
                "@Stateful\n" +
                "@Interceptor\n" +
                "class StatefulWithInterceptor {\n" +
                "}\n\n" +
                "// Test case 4: Stateful with @Decorator - should report error\n" +
                "@Stateful\n" +
                "@Decorator\n" +
                "class StatefulWithDecorator {\n" +
                "}\n\n" +
                "// Test case 5: Singleton with @Interceptor - should report error\n" +
                "@Singleton\n" +
                "@Interceptor\n" +
                "class SingletonWithInterceptor {\n" +
                "}\n\n" +
                "// Test case 6: Singleton with @Decorator - should report error\n" +
                "@Singleton\n" +
                "@Decorator\n" +
                "class SingletonWithDecorator {\n" +
                "}\n\n" +
                "// Test case 7: Valid Stateless without @Interceptor or @Decorator - should NOT report error\n" +
                "@Stateless\n" +
                "class ValidStatelessBeanNoConflict {\n" +
                "}\n";
        TextEdit removeDecoratorEdit2 = te(0, 0, 48, 0, removeDecoratorFromStateless);
        CodeAction removeDecoratorAction2 = ca(uri, "Remove @Decorator", statelessWithDecorator, removeDecoratorEdit2);
        
        // Quick Fix 2b: Remove @Stateless from bean with @Decorator
        String removeStatelessFromDecorator = "package io.openliberty.sample.jakarta.ejb;\n\n" +
                "import jakarta.ejb.Stateless;\n" +
                "import jakarta.ejb.Stateful;\n" +
                "import jakarta.ejb.Singleton;\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.decorator.Decorator;\n\n" +
                "// Test case 1: Stateless with @Interceptor - should report error\n" +
                "@Stateless\n" +
                "@Interceptor\n" +
                "public class SessionBeanInterceptorDecorator {\n" +
                "}\n\n" +
                "// Test case 2: Stateless with @Decorator - should report error\n" +
                "@Decorator\n" +
                "class StatelessWithDecorator {\n" +
                "}\n\n" +
                "// Test case 3: Stateful with @Interceptor - should report error\n" +
                "@Stateful\n" +
                "@Interceptor\n" +
                "class StatefulWithInterceptor {\n" +
                "}\n\n" +
                "// Test case 4: Stateful with @Decorator - should report error\n" +
                "@Stateful\n" +
                "@Decorator\n" +
                "class StatefulWithDecorator {\n" +
                "}\n\n" +
                "// Test case 5: Singleton with @Interceptor - should report error\n" +
                "@Singleton\n" +
                "@Interceptor\n" +
                "class SingletonWithInterceptor {\n" +
                "}\n\n" +
                "// Test case 6: Singleton with @Decorator - should report error\n" +
                "@Singleton\n" +
                "@Decorator\n" +
                "class SingletonWithDecorator {\n" +
                "}\n\n" +
                "// Test case 7: Valid Stateless without @Interceptor or @Decorator - should NOT report error\n" +
                "@Stateless\n" +
                "class ValidStatelessBeanNoConflict {\n" +
                "}\n";
        TextEdit removeStatelessEdit2b = te(0, 0, 48, 0, removeStatelessFromDecorator);
        CodeAction removeStatelessAction2b = ca(uri, "Remove @Stateless", statelessWithDecorator, removeStatelessEdit2b);
        
        assertJavaCodeAction(codeActionParams2, utils, removeDecoratorAction2, removeStatelessAction2b);

        // Quick Fix 3: Remove @Interceptor from @Stateful bean
        JakartaJavaCodeActionParams codeActionParams3 = createCodeActionParams(uri, statefulWithInterceptor);
        String removeInterceptorFromStateful = "package io.openliberty.sample.jakarta.ejb;\n\n" +
                "import jakarta.ejb.Stateless;\n" +
                "import jakarta.ejb.Stateful;\n" +
                "import jakarta.ejb.Singleton;\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.decorator.Decorator;\n\n" +
                "// Test case 1: Stateless with @Interceptor - should report error\n" +
                "@Stateless\n" +
                "@Interceptor\n" +
                "public class SessionBeanInterceptorDecorator {\n" +
                "}\n\n" +
                "// Test case 2: Stateless with @Decorator - should report error\n" +
                "@Stateless\n" +
                "@Decorator\n" +
                "class StatelessWithDecorator {\n" +
                "}\n\n" +
                "// Test case 3: Stateful with @Interceptor - should report error\n" +
                "@Stateful\n" +
                "class StatefulWithInterceptor {\n" +
                "}\n\n" +
                "// Test case 4: Stateful with @Decorator - should report error\n" +
                "@Stateful\n" +
                "@Decorator\n" +
                "class StatefulWithDecorator {\n" +
                "}\n\n" +
                "// Test case 5: Singleton with @Interceptor - should report error\n" +
                "@Singleton\n" +
                "@Interceptor\n" +
                "class SingletonWithInterceptor {\n" +
                "}\n\n" +
                "// Test case 6: Singleton with @Decorator - should report error\n" +
                "@Singleton\n" +
                "@Decorator\n" +
                "class SingletonWithDecorator {\n" +
                "}\n\n" +
                "// Test case 7: Valid Stateless without @Interceptor or @Decorator - should NOT report error\n" +
                "@Stateless\n" +
                "class ValidStatelessBeanNoConflict {\n" +
                "}\n";
        TextEdit removeInterceptorEdit3 = te(0, 0, 48, 0, removeInterceptorFromStateful);
        CodeAction removeInterceptorAction3 = ca(uri, "Remove @Interceptor", statefulWithInterceptor, removeInterceptorEdit3);
        
        // Quick Fix 3b: Remove @Stateful from bean with @Interceptor
        String removeStatefulFromInterceptor = "package io.openliberty.sample.jakarta.ejb;\n\n" +
                "import jakarta.ejb.Stateless;\n" +
                "import jakarta.ejb.Stateful;\n" +
                "import jakarta.ejb.Singleton;\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.decorator.Decorator;\n\n" +
                "// Test case 1: Stateless with @Interceptor - should report error\n" +
                "@Stateless\n" +
                "@Interceptor\n" +
                "public class SessionBeanInterceptorDecorator {\n" +
                "}\n\n" +
                "// Test case 2: Stateless with @Decorator - should report error\n" +
                "@Stateless\n" +
                "@Decorator\n" +
                "class StatelessWithDecorator {\n" +
                "}\n\n" +
                "// Test case 3: Stateful with @Interceptor - should report error\n" +
                "@Interceptor\n" +
                "class StatefulWithInterceptor {\n" +
                "}\n\n" +
                "// Test case 4: Stateful with @Decorator - should report error\n" +
                "@Stateful\n" +
                "@Decorator\n" +
                "class StatefulWithDecorator {\n" +
                "}\n\n" +
                "// Test case 5: Singleton with @Interceptor - should report error\n" +
                "@Singleton\n" +
                "@Interceptor\n" +
                "class SingletonWithInterceptor {\n" +
                "}\n\n" +
                "// Test case 6: Singleton with @Decorator - should report error\n" +
                "@Singleton\n" +
                "@Decorator\n" +
                "class SingletonWithDecorator {\n" +
                "}\n\n" +
                "// Test case 7: Valid Stateless without @Interceptor or @Decorator - should NOT report error\n" +
                "@Stateless\n" +
                "class ValidStatelessBeanNoConflict {\n" +
                "}\n";
        TextEdit removeStatefulEdit3b = te(0, 0, 48, 0, removeStatefulFromInterceptor);
        CodeAction removeStatefulAction3b = ca(uri, "Remove @Stateful", statefulWithInterceptor, removeStatefulEdit3b);
        
        assertJavaCodeAction(codeActionParams3, utils, removeInterceptorAction3, removeStatefulAction3b);

        // Quick Fix 4: Remove @Decorator from @Singleton bean
        JakartaJavaCodeActionParams codeActionParams4 = createCodeActionParams(uri, singletonWithDecorator);
        String removeDecoratorFromSingleton = "package io.openliberty.sample.jakarta.ejb;\n\n" +
                "import jakarta.ejb.Stateless;\n" +
                "import jakarta.ejb.Stateful;\n" +
                "import jakarta.ejb.Singleton;\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.decorator.Decorator;\n\n" +
                "// Test case 1: Stateless with @Interceptor - should report error\n" +
                "@Stateless\n" +
                "@Interceptor\n" +
                "public class SessionBeanInterceptorDecorator {\n" +
                "}\n\n" +
                "// Test case 2: Stateless with @Decorator - should report error\n" +
                "@Stateless\n" +
                "@Decorator\n" +
                "class StatelessWithDecorator {\n" +
                "}\n\n" +
                "// Test case 3: Stateful with @Interceptor - should report error\n" +
                "@Stateful\n" +
                "@Interceptor\n" +
                "class StatefulWithInterceptor {\n" +
                "}\n\n" +
                "// Test case 4: Stateful with @Decorator - should report error\n" +
                "@Stateful\n" +
                "@Decorator\n" +
                "class StatefulWithDecorator {\n" +
                "}\n\n" +
                "// Test case 5: Singleton with @Interceptor - should report error\n" +
                "@Singleton\n" +
                "@Interceptor\n" +
                "class SingletonWithInterceptor {\n" +
                "}\n\n" +
                "// Test case 6: Singleton with @Decorator - should report error\n" +
                "@Singleton\n" +
                "class SingletonWithDecorator {\n" +
                "}\n\n" +
                "// Test case 7: Valid Stateless without @Interceptor or @Decorator - should NOT report error\n" +
                "@Stateless\n" +
                "class ValidStatelessBeanNoConflict {\n" +
                "}\n";
        TextEdit removeDecoratorEdit4 = te(0, 0, 48, 0, removeDecoratorFromSingleton);
        CodeAction removeDecoratorAction4 = ca(uri, "Remove @Decorator", singletonWithDecorator, removeDecoratorEdit4);
        
        // Quick Fix 4b: Remove @Singleton from bean with @Decorator
        String removeSingletonFromDecorator = "package io.openliberty.sample.jakarta.ejb;\n\n" +
                "import jakarta.ejb.Stateless;\n" +
                "import jakarta.ejb.Stateful;\n" +
                "import jakarta.ejb.Singleton;\n" +
                "import jakarta.interceptor.Interceptor;\n" +
                "import jakarta.decorator.Decorator;\n\n" +
                "// Test case 1: Stateless with @Interceptor - should report error\n" +
                "@Stateless\n" +
                "@Interceptor\n" +
                "public class SessionBeanInterceptorDecorator {\n" +
                "}\n\n" +
                "// Test case 2: Stateless with @Decorator - should report error\n" +
                "@Stateless\n" +
                "@Decorator\n" +
                "class StatelessWithDecorator {\n" +
                "}\n\n" +
                "// Test case 3: Stateful with @Interceptor - should report error\n" +
                "@Stateful\n" +
                "@Interceptor\n" +
                "class StatefulWithInterceptor {\n" +
                "}\n\n" +
                "// Test case 4: Stateful with @Decorator - should report error\n" +
                "@Stateful\n" +
                "@Decorator\n" +
                "class StatefulWithDecorator {\n" +
                "}\n\n" +
                "// Test case 5: Singleton with @Interceptor - should report error\n" +
                "@Singleton\n" +
                "@Interceptor\n" +
                "class SingletonWithInterceptor {\n" +
                "}\n\n" +
                "// Test case 6: Singleton with @Decorator - should report error\n" +
                "@Decorator\n" +
                "class SingletonWithDecorator {\n" +
                "}\n\n" +
                "// Test case 7: Valid Stateless without @Interceptor or @Decorator - should NOT report error\n" +
                "@Stateless\n" +
                "class ValidStatelessBeanNoConflict {\n" +
                "}\n";
        TextEdit removeSingletonEdit4b = te(0, 0, 48, 0, removeSingletonFromDecorator);
        CodeAction removeSingletonAction4b = ca(uri, "Remove @Singleton", singletonWithDecorator, removeSingletonEdit4b);
        
        assertJavaCodeAction(codeActionParams4, utils, removeDecoratorAction4, removeSingletonAction4b);
    }
}
