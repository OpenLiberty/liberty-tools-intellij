/*******************************************************************************
* Copyright (c) 2021, 2023 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     IBM Corporation, Adit Rada, Yijia Jing - initial API and implementation
*******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.it.jsonb;

import com.google.gson.Gson;
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
public class JsonbDiagnosticsCollectorTest extends BaseJakartaTest {

    @Test
    @Ignore
    public void deleteExtraJsonbCreatorAnnotation() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/jsonb/ExtraJsonbCreatorAnnotations.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaDiagnosticsParams diagnosticsParams = new JakartaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d1 = JakartaForJavaAssert.d(18, 11, 39,
                "Only one constructor or static factory method can be annotated with @JsonbCreator in a given class.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "MultipleJsonbCreatorAnnotations");
        
        Diagnostic d2 = JakartaForJavaAssert.d(21, 48, 61,
                "Only one constructor or static factory method can be annotated with @JsonbCreator in a given class.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "MultipleJsonbCreatorAnnotations");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d1, d2);

        // test code actions
        JakartaJavaCodeActionParams codeActionParams1 = JakartaForJavaAssert.createCodeActionParams(uri, d1);
        TextEdit te1 = JakartaForJavaAssert.te(17, 4, 18, 4, "");
        CodeAction ca1 = JakartaForJavaAssert.ca(uri, "Remove @JsonbCreator", d1, te1);
        
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams1, utils, ca1);

        JakartaJavaCodeActionParams codeActionParams2 = JakartaForJavaAssert.createCodeActionParams(uri, d2);
        TextEdit te2 = JakartaForJavaAssert.te(20, 4, 21, 4, "");
        CodeAction ca2 = JakartaForJavaAssert.ca(uri, "Remove @JsonbCreator", d2, te2);

        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams2, utils, ca2);
    }
    
    @Test
    @Ignore
    public void JsonbTransientNotMutuallyExclusive() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(myProject);

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "src/main/java/io/openliberty/sample/jakarta/jsonb/JsonbTransientDiagnostic.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaDiagnosticsParams diagnosticsParams = new JakartaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));
        
        // Diagnostic for the field "id"
        Diagnostic d1 = JakartaForJavaAssert.d(21, 16, 18,
                "When a class field is annotated with @JsonbTransient, this field, getter or setter must not be annotated with other JSON Binding annotations.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "NonmutualJsonbTransientAnnotation");
        d1.setData(new Gson().toJsonTree(Arrays.asList("JsonbTransient")));

        // Diagnostic for the field "name"
        Diagnostic d2 = JakartaForJavaAssert.d(25, 19, 23,
                "When a class field is annotated with @JsonbTransient, this field, getter or setter must not be annotated with other JSON Binding annotations.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "NonmutualJsonbTransientAnnotation");
        d2.setData(new Gson().toJsonTree(Arrays.asList("JsonbProperty",  "JsonbTransient")));

        // Diagnostic for the field "favoriteLanguage"
        Diagnostic d3 = JakartaForJavaAssert.d(30, 19, 35,
                "When a class field is annotated with @JsonbTransient, this field, getter or setter must not be annotated with other JSON Binding annotations.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "NonmutualJsonbTransientAnnotation");
        d3.setData(new Gson().toJsonTree(Arrays.asList("JsonbProperty", "JsonbAnnotation",  "JsonbTransient")));
        
        // Diagnostic for the field "favoriteEditor"
        Diagnostic d4 = JakartaForJavaAssert.d(39, 19, 33,
                "When an accessor is annotated with @JsonbTransient, its field or the accessor must not be annotated with other JSON Binding annotations.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "NonmutualJsonbTransientAnnotationOnAccessor");
        d4.setData(new Gson().toJsonTree(Arrays.asList("JsonbProperty")));
        
        // Diagnostic for the getter "getId"
        Diagnostic d5 = JakartaForJavaAssert.d(42, 16, 21,
                "When a class field is annotated with @JsonbTransient, this field, getter or setter must not be annotated with other JSON Binding annotations.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "NonmutualJsonbTransientAnnotation");
        d5.setData(new Gson().toJsonTree(Arrays.asList("JsonbProperty")));
       
        // Diagnostic for the setter "setId"
        Diagnostic d6 = JakartaForJavaAssert.d(49, 17, 22,
                "When a class field is annotated with @JsonbTransient, this field, getter or setter must not be annotated with other JSON Binding annotations.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "NonmutualJsonbTransientAnnotation");
        d6.setData(new Gson().toJsonTree(Arrays.asList("JsonbAnnotation")));
        
        // Diagnostic for the getter "getFavoriteEditor"
        Diagnostic d7 = JakartaForJavaAssert.d(67, 19, 36,
                "When an accessor is annotated with @JsonbTransient, its field or the accessor must not be annotated with other JSON Binding annotations.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "NonmutualJsonbTransientAnnotationOnAccessor");
        d7.setData(new Gson().toJsonTree(Arrays.asList("JsonbTransient")));
       
        // Diagnostic for the setter "setFavoriteEditor"
        Diagnostic d8 = JakartaForJavaAssert.d(74, 17, 34,
                "When an accessor is annotated with @JsonbTransient, its field or the accessor must not be annotated with other JSON Binding annotations.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "NonmutualJsonbTransientAnnotationOnAccessor");
        d8.setData(new Gson().toJsonTree(Arrays.asList("JsonbAnnotation", "JsonbTransient")));
  
        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3, d4, d5, d6, d7, d8);


        // Test code actions
        // Quick fix for the field "id"
        JakartaJavaCodeActionParams codeActionParams1 = JakartaForJavaAssert.createCodeActionParams(uri, d1);
        TextEdit te1 = JakartaForJavaAssert.te(20, 4, 21, 4, "");
        CodeAction ca1 = JakartaForJavaAssert.ca(uri, "Remove @JsonbTransient", d1, te1);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams1, utils, ca1);
        
        // Quick fix for the field "name"
        JakartaJavaCodeActionParams codeActionParams2 = JakartaForJavaAssert.createCodeActionParams(uri, d2);
        TextEdit te3 = JakartaForJavaAssert.te(24, 4, 25, 4, "");
        TextEdit te4 = JakartaForJavaAssert.te(23, 4, 24, 4, "");
        CodeAction ca3 = JakartaForJavaAssert.ca(uri, "Remove @JsonbTransient", d2, te3);
        CodeAction ca4 = JakartaForJavaAssert.ca(uri, "Remove @JsonbProperty", d2, te4);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams2, utils, ca3, ca4);
       
        // Quick fix for the field "favoriteLanguage"
        JakartaJavaCodeActionParams codeActionParams3 = JakartaForJavaAssert.createCodeActionParams(uri, d3);
        TextEdit te5 = JakartaForJavaAssert.te(29, 4, 30, 4, "");
        TextEdit te6 = JakartaForJavaAssert.te(27, 4, 29, 4, "");
        CodeAction ca5 = JakartaForJavaAssert.ca(uri, "Remove @JsonbTransient", d3, te5);
        CodeAction ca6 = JakartaForJavaAssert.ca(uri, "Remove @JsonbProperty, @JsonbAnnotation", d3, te6);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams3, utils, ca5, ca6);
        
        // Quick fix for the accessor "getId"
        JakartaJavaCodeActionParams codeActionParams4 = JakartaForJavaAssert.createCodeActionParams(uri, d5);
        TextEdit te7 = JakartaForJavaAssert.te(41, 4, 42, 4, "");
        CodeAction ca7 = JakartaForJavaAssert.ca(uri, "Remove @JsonbProperty", d5, te7);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams4, utils, ca7);

        // Quick fix for the accessor "setId"
        JakartaJavaCodeActionParams codeActionParams5 = JakartaForJavaAssert.createCodeActionParams(uri, d6);
        TextEdit te8 = JakartaForJavaAssert.te(48, 4, 49, 4, "");
        CodeAction ca8 = JakartaForJavaAssert.ca(uri, "Remove @JsonbAnnotation", d6, te8);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams5, utils, ca8);
    }
}