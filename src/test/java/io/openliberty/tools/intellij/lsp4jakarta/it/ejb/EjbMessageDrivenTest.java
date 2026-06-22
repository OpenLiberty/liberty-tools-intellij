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

import java.io.File;
import java.util.Arrays;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.lsp4jakarta.it.core.BaseJakartaTest;
import io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for Jakarta EE Enterprise Beans (EJB) @MessageDriven diagnostics.
 * 
 * Tests validation that classes annotated with @MessageDriven must implement
 * the appropriate message listener interface (jakarta.jms.MessageListener for JMS).
 * 
 * @see <a href="https://jakarta.ee/specifications/enterprise-beans/4.0/jakarta-enterprise-beans-spec-core-4.0#the-required-message-listener-interface">
 *      Jakarta EE Enterprise Beans Specification - Section 5.4.2</a>
 */
@RunWith(JUnit4.class)
public class EjbMessageDrivenTest extends BaseJakartaTest {

    @Test
    public void messageDrivenBeanValidTest() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/MessageDrivenBeanValid.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // No diagnostics expected for valid message-driven bean
        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void messageDrivenBeanMissingInterfaceTest() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/MessageDrivenBeanMissingInterface.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostic for missing MessageListener interface (on annotation closing line)
        Diagnostic expectedDiagnostic = JakartaForJavaAssert.d(12, 13, 46,
                "A class annotated with @MessageDriven must implement the jakarta.jms.MessageListener interface.",
                DiagnosticSeverity.Error, "jakarta-ejb", "ImplementMessageListener");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, expectedDiagnostic);

        // Test code action to implement MessageListener interface
        JakartaJavaCodeActionParams codeActionParams = JakartaForJavaAssert.createCodeActionParams(uri, expectedDiagnostic);
        String newText = "package io.openliberty.sample.jakarta.ejb;\n" +
                "\n" +
                "import jakarta.ejb.MessageDriven;\n" +
                "import jakarta.jms.Message;\n" +
                "import jakarta.jms.MessageListener;\n" +
                "import jakarta.jms.TextMessage;\n" +
                "\n" +
                "@MessageDriven(\n" +
                "        activationConfig = {\n" +
                "                @jakarta.ejb.ActivationConfigProperty(propertyName = \"destinationType\", propertyValue = \"jakarta.jms.Queue\"),\n" +
                "                @jakarta.ejb.ActivationConfigProperty(propertyName = \"destination\", propertyValue = \"jms/MyQueue\")\n" +
                "        }\n" +
                ")\n" +
                "public class MessageDrivenBeanMissingInterface implements MessageListener {\n" +
                "    // Missing implements MessageListener - this should trigger a diagnostic\n" +
                "\n" +
                "    public void onMessage(Message message) {\n" +
                "        try {\n" +
                "            if (message instanceof TextMessage textMessage) {\n" +
                "                String text = textMessage.getText();\n" +
                "                System.out.println(\"Received message: \" + text);\n" +
                "            }\n" +
                "        } catch (Exception e) {\n" +
                "            e.printStackTrace();\n" +
                "        }\n" +
                "    }\n" +
                "}";
        TextEdit textEdit = JakartaForJavaAssert.te(0, 0, 25, 1, newText);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams, utils,
                JakartaForJavaAssert.ca(uri, "Let 'MessageDrivenBeanMissingInterface' implement 'MessageListener'", expectedDiagnostic, textEdit));
    }

    @Test
    public void messageDrivenBeanWrongInterfaceTest() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/ejb/MessageDrivenBeanWrongInterface.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostic for wrong interface (on annotation closing line)
        Diagnostic expectedDiagnostic = JakartaForJavaAssert.d(11, 13, 44,
                "A class annotated with @MessageDriven must implement the jakarta.jms.MessageListener interface.",
                DiagnosticSeverity.Error, "jakarta-ejb", "ImplementMessageListener");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, expectedDiagnostic);

        // Test code action to implement MessageListener interface
        JakartaJavaCodeActionParams codeActionParams = JakartaForJavaAssert.createCodeActionParams(uri, expectedDiagnostic);
        String newText = "package io.openliberty.sample.jakarta.ejb;\n" +
                "\n" +
                "import jakarta.ejb.MessageDriven;\n" +
                "import jakarta.jms.MessageListener;\n" +
                "\n" +
                "import java.io.Serializable;\n" +
                "\n" +
                "@MessageDriven(\n" +
                "        activationConfig = {\n" +
                "                @jakarta.ejb.ActivationConfigProperty(propertyName = \"destinationType\", propertyValue = \"jakarta.jms.Queue\"),\n" +
                "                @jakarta.ejb.ActivationConfigProperty(propertyName = \"destination\", propertyValue = \"jms/MyQueue\")\n" +
                "        }\n" +
                ")\n" +
                "public class MessageDrivenBeanWrongInterface implements Serializable, MessageListener {\n" +
                "    // Implements wrong interface - should trigger a diagnostic\n" +
                "\n" +
                "    public void someMethod() {\n" +
                "        System.out.println(\"This is not a message listener method\");\n" +
                "    }\n" +
                "}";
        TextEdit textEdit = JakartaForJavaAssert.te(0, 0, 17, 1, newText);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams, utils,
                JakartaForJavaAssert.ca(uri, "Let 'MessageDrivenBeanWrongInterface' implement 'MessageListener'", expectedDiagnostic, textEdit));
    }
}