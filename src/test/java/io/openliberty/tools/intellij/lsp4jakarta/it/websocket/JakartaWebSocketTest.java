/*******************************************************************************
* Copyright (c) 2022, 2023 IBM Corporation and others.
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

package io.openliberty.tools.intellij.lsp4jakarta.it.websocket;

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
import org.eclipse.lsp4jakarta.commons.JakartaDiagnosticsParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;

@RunWith(JUnit4.class)
public class JakartaWebSocketTest extends BaseJakartaTest {

    @Test
    @Ignore
    public void addPathParamsAnnotation() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/websocket/AnnotationTest.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaDiagnosticsParams diagnosticsParams = new JakartaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // OnOpen PathParams Annotation check
        Diagnostic d1 = JakartaForJavaAssert.d(18, 47, 64,
                "Parameters of type String, any Java primitive type, or boxed version thereof must be annotated with @PathParams.",
                DiagnosticSeverity.Error, "jakarta-websocket", "AddPathParamsAnnotation"
        );
        
        // OnClose PathParams Annotation check
        Diagnostic d2 = JakartaForJavaAssert.d(24, 49, 67,
                "Parameters of type String, any Java primitive type, or boxed version thereof must be annotated with @PathParams.",
                DiagnosticSeverity.Error, "jakarta-websocket", "AddPathParamsAnnotation"
        );
        
        Diagnostic d3 = JakartaForJavaAssert.d(24, 76, 94,
                "Parameters of type String, any Java primitive type, or boxed version thereof must be annotated with @PathParams.",
                DiagnosticSeverity.Error, "jakarta-websocket", "AddPathParamsAnnotation"
        );

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3);
        
        // Expected code actions
        JakartaJavaCodeActionParams codeActionsParams = JakartaForJavaAssert.createCodeActionParams(uri, d1);
        String newText = "\nimport jakarta.websocket.server.PathParam;\nimport jakarta.websocket.server.ServerEndpoint;\nimport jakarta.websocket.Session;\n\n" 
                        + "/**\n * Expected Diagnostics are related to validating that the parameters have the \n * valid annotation @PathParam (code: AddPathParamsAnnotation)\n * See issue #247 (onOpen) and #248 (onClose)\n */\n" 
                        + "@ServerEndpoint(value = \"/infos\")\npublic class AnnotationTest {\n    // @PathParam missing annotation for \"String missingAnnotation\"\n    @OnOpen\n    public void OnOpen(Session session, @PathParam(value = \"\") ";
        TextEdit te = JakartaForJavaAssert.te(5, 32, 18, 40, newText);
        CodeAction ca = JakartaForJavaAssert.ca(uri, "Insert @jakarta.websocket.server.PathParam", d1, te);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionsParams, utils, ca);
    }

    @Test
    public void changeInvalidParamType() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/websocket/InvalidParamType.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaDiagnosticsParams diagnosticsParams = new JakartaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // OnOpen Invalid Param Types
        Diagnostic d1 = JakartaForJavaAssert.d(19, 47, 59,
        "Invalid parameter type. When using @jakarta.websocket.OnOpen, parameter must be of type: \n- jakarta.websocket.EndpointConfig\n- jakarta.websocket.Session\n- annotated with @PathParams and of type String or any Java primitive type or boxed version thereof",
                DiagnosticSeverity.Error, "jakarta-websocket", "OnOpenChangeInvalidParam");

        // OnClose Invalid Param Type
        Diagnostic d2 = JakartaForJavaAssert.d(24, 73, 85,
                "Invalid parameter type. When using @jakarta.websocket.OnClose, parameter must be of type: \n- jakarta.websocket.CloseReason\n- jakarta.websocket.Session\n- annotated with @PathParams and of type String or any Java primitive type or boxed version thereof",
                DiagnosticSeverity.Error, "jakarta-websocket", "OnCloseChangeInvalidParam");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d1, d2);
    }

    @Test
    public void testPathParamInvalidURI() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/websockets/PathParamURIWarningTest.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaDiagnosticsParams diagnosticsParams = new JakartaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d = JakartaForJavaAssert.d(22, 59, 77, "PathParam value does not match specified Endpoint URI",
                DiagnosticSeverity.Warning, "jakarta-websocket", "ChangePathParamValue");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d);
    }

    @Test
    public void testServerEndpointRelativeURI() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/websocket/ServerEndpointRelativePathTest.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaDiagnosticsParams diagnosticsParams = new JakartaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d = JakartaForJavaAssert.d(6, 0, 27, "Server endpoint paths must not contain the sequences '/../', '/./' or '//'.",
                DiagnosticSeverity.Error, "jakarta-websocket", "ChangeInvalidServerEndpoint");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d);
    }

    @Test
    public void testServerEndpointNoSlashURI() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/websocket/ServerEndpointNoSlash.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaDiagnosticsParams diagnosticsParams = new JakartaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));
        Diagnostic d1 = JakartaForJavaAssert.d(7, 0, 23, "Server endpoint paths must start with a leading '/'.", DiagnosticSeverity.Error,
                "jakarta-websocket", "ChangeInvalidServerEndpoint");
        Diagnostic d2 = JakartaForJavaAssert.d(7, 0, 23, "Server endpoint paths must be a URI-template (level-1) or a partial URI.",
                DiagnosticSeverity.Error, "jakarta-websocket", "ChangeInvalidServerEndpoint");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d1, d2);
    }

    @Test
    public void testServerEndpointInvalidTemplateURI() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/websocket/ServerEndpointInvalidTemplateURI.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaDiagnosticsParams diagnosticsParams = new JakartaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));
        Diagnostic d = JakartaForJavaAssert.d(6, 0, 46, "Server endpoint paths must be a URI-template (level-1) or a partial URI.",
                DiagnosticSeverity.Error, "jakarta-websocket", "ChangeInvalidServerEndpoint");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d);
    }

    @Test
    public void testServerEndpointDuplicateVariableURI() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/websocket/ServerEndpointDuplicateVariableURI.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaDiagnosticsParams diagnosticsParams = new JakartaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));
        Diagnostic d = JakartaForJavaAssert.d(6, 0, 40, "Server endpoint paths must not use the same variable more than once in a path.",
                DiagnosticSeverity.Error, "jakarta-websocket", "ChangeInvalidServerEndpoint");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d);
    }

    @Test
    public void testDuplicateOnMessage() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/websocket/DuplicateOnMessage.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaDiagnosticsParams diagnosticsParams = new JakartaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));
        Diagnostic d1 = JakartaForJavaAssert.d(12, 4, 14,
                "Classes annotated with @ServerEndpoint or @ClientEndpoint must have only one @OnMessage annotated method for each of the native WebSocket message formats: text, binary and pong.",
                DiagnosticSeverity.Error, "jakarta-websocket", "OnMessageDuplicateMethod");
        Diagnostic d2 = JakartaForJavaAssert.d(17, 4, 14,
                "Classes annotated with @ServerEndpoint or @ClientEndpoint must have only one @OnMessage annotated method for each of the native WebSocket message formats: text, binary and pong.",
                DiagnosticSeverity.Error, "jakarta-websocket", "OnMessageDuplicateMethod");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d1, d2);
    }
}
