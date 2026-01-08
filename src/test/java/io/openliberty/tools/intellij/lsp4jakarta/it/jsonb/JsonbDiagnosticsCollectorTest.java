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
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
import org.eclipse.lsp4jakarta.commons.JakartaJavaCodeActionParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;


@RunWith(JUnit4.class)
public class JsonbDiagnosticsCollectorTest extends BaseJakartaTest {

    @Test
    public void deleteExtraJsonbCreatorAnnotation() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/jsonb/ExtraJsonbCreatorAnnotations.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d1 = JakartaForJavaAssert.d(18, 11, 39,
                "Only one constructor or static factory method can be annotated with @JsonbCreator in a given class.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "MultipleJsonbCreatorAnnotations");
        
        Diagnostic d2 = JakartaForJavaAssert.d(21, 48, 61,
                "Only one constructor or static factory method can be annotated with @JsonbCreator in a given class.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "MultipleJsonbCreatorAnnotations");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d1, d2);

            // Starting codeAction tests
        String newText = "/*******************************************************************************\n" +
                " * Copyright (c) 2021 IBM Corporation and others.\n *\n" +
                " * This program and the accompanying materials are made available under the\n" +
                " * terms of the Eclipse Public License v. 2.0 which is available at\n" +
                " * http://www.eclipse.org/legal/epl-2.0.\n *\n" +
                " * SPDX-License-Identifier: EPL-2.0\n *\n * Contributors:\n" +
                " *     IBM Corporation - initial API and implementation\n" +
                " *******************************************************************************/\n" +
                "package io.openliberty.sample.jakarta.jsonb;\n\n" +
                "import jakarta.json.bind.annotation.JsonbCreator;\n\n" +
                "public class ExtraJsonbCreatorAnnotations {\n" +
                "    public ExtraJsonbCreatorAnnotations() {}\n    \n    @JsonbCreator\n" +
                "    private static ExtraJsonbCreatorAnnotations factoryMethod() {\n" +
                "        return null;\n    }\n}";

            JakartaJavaCodeActionParams codeActionParams1 = JakartaForJavaAssert.createCodeActionParams(uri, d1);
            TextEdit te1 = JakartaForJavaAssert.te(0, 0, 24, 1, newText);
            CodeAction ca1 = JakartaForJavaAssert.ca(uri, "Remove @JsonbCreator", d1, te1);

            JakartaForJavaAssert.assertJavaCodeAction(codeActionParams1, utils, ca1);

        String newText1 = "/*******************************************************************************\n" +
                " * Copyright (c) 2021 IBM Corporation and others.\n *\n" +
                " * This program and the accompanying materials are made available under the\n" +
                " * terms of the Eclipse Public License v. 2.0 which is available at\n" +
                " * http://www.eclipse.org/legal/epl-2.0.\n *\n" +
                " * SPDX-License-Identifier: EPL-2.0\n *\n * Contributors:\n" +
                " *     IBM Corporation - initial API and implementation\n" +
                " *******************************************************************************/\n" +
                "package io.openliberty.sample.jakarta.jsonb;\n\n" +
                "import jakarta.json.bind.annotation.JsonbCreator;\n\n" +
                "public class ExtraJsonbCreatorAnnotations {\n    @JsonbCreator\n" +
                "    public ExtraJsonbCreatorAnnotations() {}\n    \n" +
                "    private static ExtraJsonbCreatorAnnotations factoryMethod() {\n" +
                "        return null;\n    }\n}";

        JakartaJavaCodeActionParams codeActionParams2 = JakartaForJavaAssert.createCodeActionParams(uri, d2);
            TextEdit te2 = JakartaForJavaAssert.te(0, 0, 24, 1, newText1);
            CodeAction ca2 = JakartaForJavaAssert.ca(uri, "Remove @JsonbCreator", d2, te2);

            JakartaForJavaAssert.assertJavaCodeAction(codeActionParams2, utils, ca2);
    }
    
    @Test
    public void JsonbTransientNotMutuallyExclusive() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/jsonb/JsonbTransientDiagnostic.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));
        
        // Diagnostic for the field "id"
        Diagnostic d1 = JakartaForJavaAssert.d(21, 16, 18,
                "When a class field is annotated with @JsonbTransient, this field, getter or setter must not be annotated with other JSON Binding annotations.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "NonmutualJsonbTransientAnnotation");
        d1.setData(new Gson().toJsonTree(Arrays.asList("jakarta.json.bind.annotation.JsonbTransient")));

        // Diagnostic for the field "name"
        Diagnostic d2 = JakartaForJavaAssert.d(25, 19, 23,
                "When a class field is annotated with @JsonbTransient, this field, getter or setter must not be annotated with other JSON Binding annotations.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "NonmutualJsonbTransientAnnotation");
        d2.setData(new Gson().toJsonTree(Arrays.asList("jakarta.json.bind.annotation.JsonbProperty",  "jakarta.json.bind.annotation.JsonbTransient")));

        // Diagnostic for the field "favoriteLanguage"
        Diagnostic d3 = JakartaForJavaAssert.d(30, 19, 35,
                "When a class field is annotated with @JsonbTransient, this field, getter or setter must not be annotated with other JSON Binding annotations.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "NonmutualJsonbTransientAnnotation");
        
        d3.setData(new Gson().toJsonTree(Arrays.asList("jakarta.json.bind.annotation.JsonbProperty", "jakarta.json.bind.annotation.JsonbAnnotation",  "jakarta.json.bind.annotation.JsonbTransient")));

        // Diagnostic for the field "favoriteEditor"
        Diagnostic d4 = JakartaForJavaAssert.d(39, 19, 33,
                "When an accessor is annotated with @JsonbTransient, its field or the accessor must not be annotated with other JSON Binding annotations.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "NonmutualJsonbTransientAnnotationOnAccessor");
        
        d4.setData(new Gson().toJsonTree(Arrays.asList("jakarta.json.bind.annotation.JsonbProperty")));

        // Diagnostic for the getter "getId"
        Diagnostic d5 = JakartaForJavaAssert.d(42, 16, 21,
                "When a class field is annotated with @JsonbTransient, this field, getter or setter must not be annotated with other JSON Binding annotations.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "NonmutualJsonbTransientAnnotation");
       
        d5.setData(new Gson().toJsonTree(Arrays.asList("jakarta.json.bind.annotation.JsonbProperty")));

        // Diagnostic for the setter "setId"
        Diagnostic d6 = JakartaForJavaAssert.d(49, 17, 22,
                "When a class field is annotated with @JsonbTransient, this field, getter or setter must not be annotated with other JSON Binding annotations.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "NonmutualJsonbTransientAnnotation");
        
        d6.setData(new Gson().toJsonTree(Arrays.asList("jakarta.json.bind.annotation.JsonbAnnotation")));

        // Diagnostic for the getter "getFavoriteEditor"
        Diagnostic d7 = JakartaForJavaAssert.d(67, 19, 36,
                "When an accessor is annotated with @JsonbTransient, its field or the accessor must not be annotated with other JSON Binding annotations.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "NonmutualJsonbTransientAnnotationOnAccessor");
       
        d7.setData(new Gson().toJsonTree(Arrays.asList("jakarta.json.bind.annotation.JsonbTransient")));

        // Diagnostic for the setter "setFavoriteEditor"
        Diagnostic d8 = JakartaForJavaAssert.d(74, 17, 34,
                "When an accessor is annotated with @JsonbTransient, its field or the accessor must not be annotated with other JSON Binding annotations.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "NonmutualJsonbTransientAnnotationOnAccessor");
  
        d8.setData(new Gson().toJsonTree(Arrays.asList("jakarta.json.bind.annotation.JsonbAnnotation", "jakarta.json.bind.annotation.JsonbTransient")));

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3, d4, d5, d6, d7, d8);

        // Test code actions
        // Quick fix for the field "id"
        JakartaJavaCodeActionParams codeActionParams1 = JakartaForJavaAssert.createCodeActionParams(uri, d1);
        String newText = "/******************************************************************************* \n" +
                "* Copyright (c) 2022 IBM Corporation and others.\n *\n * This program and the accompanying " +
                "materials are made available under the\n * terms of the Eclipse Public License v. 2.0 which " +
                "is available at\n * http://www.eclipse.org/legal/epl-2.0.\n *\n * SPDX-License-Identifier: " +
                "EPL-2.0\n *\n * Contributors:\n *     Adit Rada, Yijia Jing - initial API and implementation\n" +
                " *******************************************************************************/\n\n" +
                "package io.openliberty.sample.jakarta.jsonb;\n\n" +
                "import jakarta.json.bind.annotation.JsonbAnnotation;\n" +
                "import jakarta.json.bind.annotation.JsonbProperty;\n" +
                "import jakarta.json.bind.annotation.JsonbTransient;\n\n" +
                "public class JsonbTransientDiagnostic {\n    private int id;\n\n    @JsonbProperty(\"name\")\n   " +
                " @JsonbTransient\n    private String name;    // Diagnostic: JsonbTransient is mutually exclusive" +
                " with other JsonB annotations\n\n    @JsonbProperty(\"fav_lang\")\n    @JsonbAnnotation\n   " +
                " @JsonbTransient\n    private String favoriteLanguage;    // Diagnostic: JsonbTransient " +
                "is mutually exclusive with other JsonB annotations\n    \n    // No diagnostic as field is not " +
                "annotated with other Jsonb annotations,\n    // even though the accessors are annotated with " +
                "@JsonbTransient\n    private String favoriteDatabase;\n    \n    // Diagnostic will appear as " +
                "field accessors have @JsonbTransient,\n    // but field itself has annotation other than " +
                "transient\n    @JsonbProperty(\"fav_editor\")\n    private String favoriteEditor;\n    \n    " +
                "@JsonbProperty(\"person-id\")\n    private int getId() { \n        // A diagnostic is expected on" +
                " getId because as a getter, it is annotated with other \n        // Jsonb annotations while its " +
                "corresponding field id is annotated with JsonbTransient\n        return id;\n    }\n    \n   " +
                " @JsonbAnnotation\n    private void setId(int id) {\n        // A diagnostic is expected on setId" +
                " because as a setter, it is annotated with other \n        // Jsonb annotations while its " +
                "corresponding field id is annotated with JsonbTransient\n        this.id = id;\n    }\n    \n   " +
                " @JsonbTransient\n    private String getFavoriteDatabase() {\n        return favoriteDatabase;\n " +
                "   }\n    \n    @JsonbTransient\n    private void setFavoriteDatabase(String favoriteDatabase) {\n" +
                "        this.favoriteDatabase = favoriteDatabase;\n    }\n    \n    // A diagnostic will appear" +
                " as field has conflicting annotation\n    @JsonbTransient\n    private String getFavoriteEditor()" +
                " {\n        return favoriteEditor;\n    }\n    \n    // A diagnostic will appear as @JsonbTransient" +
                " is not mutually exclusive on this accessor\n    @JsonbAnnotation\n    @JsonbTransient\n    " +
                "private void setFavoriteEditor(String favoriteEditor) {\n        this.favoriteEditor = " +
                "favoriteEditor;\n    }\n}";
        TextEdit te1 = JakartaForJavaAssert.te(0, 0, 77, 1, newText);
        CodeAction ca1 = JakartaForJavaAssert.ca(uri, "Remove @JsonbTransient", d1, te1);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams1, utils, ca1);

        // Quick fix for the field "name"
        JakartaJavaCodeActionParams codeActionParams2 = JakartaForJavaAssert.createCodeActionParams(uri, d2);
        newText = "/******************************************************************************* \n* Copyright " +
                "(c) 2022 IBM Corporation and others.\n *\n * This program and the accompanying materials are" +
                " made available under the\n * terms of the Eclipse Public License v. 2.0 which is available " +
                "at\n * http://www.eclipse.org/legal/epl-2.0.\n *\n * SPDX-License-Identifier: EPL-2.0\n *\n *" +
                " Contributors:\n *     Adit Rada, Yijia Jing - initial API and implementation\n" +
                " *******************************************************************************/\n\npackage " +
                "io.openliberty.sample.jakarta.jsonb;\n\nimport jakarta.json.bind.annotation.JsonbAnnotation;\n" +
                "import jakarta.json.bind.annotation.JsonbProperty;\nimport jakarta.json.bind.annotation." +
                "JsonbTransient;\n\npublic class JsonbTransientDiagnostic {\n    @JsonbTransient\n    " +
                "private int id;\n\n    @JsonbTransient\n    private String name;    // Diagnostic: " +
                "JsonbTransient is mutually exclusive with other JsonB annotations\n\n    " +
                "@JsonbProperty(\"fav_lang\")\n    @JsonbAnnotation\n    @JsonbTransient\n    " +
                "private String favoriteLanguage;    // Diagnostic: JsonbTransient is mutually exclusive with other" +
                " JsonB annotations\n    \n    // No diagnostic as field is not annotated with other Jsonb " +
                "annotations,\n    // even though the accessors are annotated with @JsonbTransient\n    " +
                "private String favoriteDatabase;\n    \n    // Diagnostic will appear as field accessors have" +
                " @JsonbTransient,\n    // but field itself has annotation other than transient\n    " +
                "@JsonbProperty(\"fav_editor\")\n    private String favoriteEditor;\n    \n    " +
                "@JsonbProperty(\"person-id\")\n    private int getId() { \n       " +
                " // A diagnostic is expected on getId because as a getter, it is annotated with other \n        " +
                "// Jsonb annotations while its corresponding field id is annotated with JsonbTransient\n        " +
                "return id;\n    }\n    \n    @JsonbAnnotation\n    private void setId(int id) {\n        " +
                "// A diagnostic is expected on setId because as a setter, it is annotated with other \n        " +
                "// Jsonb annotations while its corresponding field id is annotated with JsonbTransient\n        " +
                "this.id = id;\n    }\n    \n    @JsonbTransient\n    private String getFavoriteDatabase() {\n     " +
                "   return favoriteDatabase;\n    }\n    \n    @JsonbTransient\n    " +
                "private void setFavoriteDatabase(String favoriteDatabase) {\n        " +
                "this.favoriteDatabase = favoriteDatabase;\n    }\n    \n    // A diagnostic will appear as" +
                " field has conflicting annotation\n    @JsonbTransient\n    private String getFavoriteEditor() " +
                "{\n        return favoriteEditor;\n    }\n    \n    // A diagnostic will appear as @JsonbTransient" +
                " is not mutually exclusive on this accessor\n    @JsonbAnnotation\n    @JsonbTransient\n   " +
                " private void setFavoriteEditor(String favoriteEditor) {\n        this.favoriteEditor = " +
                "favoriteEditor;\n    }\n}";

        String newText1 = "/******************************************************************************* \n* " +
                "Copyright (c) 2022 IBM Corporation and others.\n *\n * This program and the accompanying materials" +
                " are made available under the\n * terms of the Eclipse Public License v. 2.0 which is available " +
                "at\n * http://www.eclipse.org/legal/epl-2.0.\n *\n * SPDX-License-Identifier: EPL-2.0\n *\n *" +
                " Contributors:\n *     Adit Rada, Yijia Jing - initial API and implementation\n " +
                "*******************************************************************************/\n\npackage " +
                "io.openliberty.sample.jakarta.jsonb;\n\nimport jakarta.json.bind.annotation.JsonbAnnotation;\n" +
                "import jakarta.json.bind.annotation.JsonbProperty;\nimport jakarta.json.bind.annotation." +
                "JsonbTransient;\n\npublic class JsonbTransientDiagnostic {\n    @JsonbTransient\n    private int " +
                "id;\n\n    @JsonbProperty(\"name\")\n    private String name;    // Diagnostic: JsonbTransient " +
                "is mutually exclusive with other JsonB annotations\n\n    @JsonbProperty(\"fav_lang\")\n    " +
                "@JsonbAnnotation\n    @JsonbTransient\n    private String favoriteLanguage;    // Diagnostic:" +
                " JsonbTransient is mutually exclusive with other JsonB annotations\n    \n    // No diagnostic as " +
                "field is not annotated with other Jsonb annotations,\n    // even though the accessors are " +
                "annotated with @JsonbTransient\n    private String favoriteDatabase;\n    \n    // Diagnostic " +
                "will appear as field accessors have @JsonbTransient,\n    // but field itself has annotation " +
                "other than transient\n    @JsonbProperty(\"fav_editor\")\n    private String favoriteEditor;\n    " +
                "\n    @JsonbProperty(\"person-id\")\n    private int getId() { \n        // A diagnostic is" +
                " expected on getId because as a getter, it is annotated with other \n        // Jsonb annotations" +
                " while its corresponding field id is annotated with JsonbTransient\n        return id;\n    }\n" +
                "    \n    @JsonbAnnotation\n    private void setId(int id) {\n        // A diagnostic is expected" +
                " on setId because as a setter, it is annotated with other \n        // Jsonb annotations while" +
                " its corresponding field id is annotated with JsonbTransient\n        this.id = id;\n    }\n   " +
                " \n    @JsonbTransient\n    private String getFavoriteDatabase() {\n        " +
                "return favoriteDatabase;\n    }\n    \n    @JsonbTransient\n    private void " +
                "setFavoriteDatabase(String favoriteDatabase) {\n        this.favoriteDatabase = favoriteDatabase;\n" +
                "    }\n    \n    // A diagnostic will appear as field has conflicting annotation\n    " +
                "@JsonbTransient\n    private String getFavoriteEditor() {\n        return favoriteEditor;\n    }\n " +
                "   \n    // A diagnostic will appear as @JsonbTransient is not mutually exclusive on this " +
                "accessor\n    @JsonbAnnotation\n    @JsonbTransient\n    private void setFavoriteEditor" +
                "(String favoriteEditor) {\n        this.favoriteEditor = favoriteEditor;\n    }\n}";

        TextEdit te3 = JakartaForJavaAssert.te(0, 0, 77, 1, newText1);
        TextEdit te4 = JakartaForJavaAssert.te(0, 0, 77, 1, newText);
        CodeAction ca3 = JakartaForJavaAssert.ca(uri, "Remove @JsonbTransient", d2, te3);
        CodeAction ca4 = JakartaForJavaAssert.ca(uri, "Remove @JsonbProperty", d2, te4);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams2, utils, ca4, ca3);

        // Quick fix for the field "favoriteLanguage"
        JakartaJavaCodeActionParams codeActionParams3 = JakartaForJavaAssert.createCodeActionParams(uri, d3);
        newText1 = "/******************************************************************************* \n* Copyright" +
                " (c) 2022 IBM Corporation and others.\n *\n * This program and the accompanying materials are made" +
                " available under the\n * terms of the Eclipse Public License v. 2.0 which is available at\n" +
                " * http://www.eclipse.org/legal/epl-2.0.\n *\n * SPDX-License-Identifier: EPL-2.0\n *\n * " +
                "Contributors:\n *     Adit Rada, Yijia Jing - initial API and implementation\n ******************" +
                "*************************************************************/\n\npackage io.openliberty.sample." +
                "jakarta.jsonb;\n\nimport jakarta.json.bind.annotation.JsonbAnnotation;\nimport jakarta.json.bind." +
                "annotation.JsonbProperty;\nimport jakarta.json.bind.annotation.JsonbTransient;\n\npublic class " +
                "JsonbTransientDiagnostic {\n    @JsonbTransient\n    private int id;\n\n   " +
                " @JsonbProperty(\"name\")\n    @JsonbTransient\n    private String name;    // Diagnostic: " +
                "JsonbTransient is mutually exclusive with other JsonB annotations\n\n    @JsonbTransient\n    " +
                "private String favoriteLanguage;    // Diagnostic: JsonbTransient is mutually exclusive with other" +
                " JsonB annotations\n    \n    // No diagnostic as field is not annotated with other Jsonb " +
                "annotations,\n    // even though the accessors are annotated with @JsonbTransient\n   " +
                " private String favoriteDatabase;\n    \n    // Diagnostic will appear as field accessors have " +
                "@JsonbTransient,\n    // but field itself has annotation other than transient\n   " +
                " @JsonbProperty(\"fav_editor\")\n    private String favoriteEditor;\n    \n   " +
                " @JsonbProperty(\"person-id\")\n    private int getId() { \n        // A diagnostic is expected " +
                "on getId because as a getter, it is annotated with other \n        // Jsonb annotations while its" +
                " corresponding field id is annotated with JsonbTransient\n        return id;\n    }\n    \n    " +
                "@JsonbAnnotation\n    private void setId(int id) {\n        // A diagnostic is expected on setId" +
                " because as a setter, it is annotated with other \n        // Jsonb annotations while its " +
                "corresponding field id is annotated with JsonbTransient\n        this.id = id;\n    }\n    \n    " +
                "@JsonbTransient\n    private String getFavoriteDatabase() {\n        return favoriteDatabase;\n   " +
                " }\n    \n    @JsonbTransient\n    private void setFavoriteDatabase(String favoriteDatabase) {\n  " +
                "      this.favoriteDatabase = favoriteDatabase;\n    }\n    \n    // A diagnostic will appear as " +
                "field has conflicting annotation\n    @JsonbTransient\n    private String getFavoriteEditor() {\n " +
                "       return favoriteEditor;\n    }\n    \n    // A diagnostic will appear as @JsonbTransient " +
                "is not mutually exclusive on this accessor\n    @JsonbAnnotation\n    @JsonbTransient\n   " +
                " private void setFavoriteEditor(String favoriteEditor) {\n        this.favoriteEditor = " +
                "favoriteEditor;\n    }\n}";

        newText = "/******************************************************************************* \n* " +
                "Copyright (c) 2022 IBM Corporation and others.\n *\n * This program and the accompanying" +
                " materials are made available under the\n * terms of the Eclipse Public License v. 2.0 " +
                "which is available at\n * http://www.eclipse.org/legal/epl-2.0.\n *\n * SPDX-License-Identifier" +
                ": EPL-2.0\n *\n * Contributors:\n *     Adit Rada, Yijia Jing - initial API and implementation\n" +
                " *******************************************************************************/\n\n" +
                "package io.openliberty.sample.jakarta.jsonb;\n\nimport jakarta.json.bind.annotation." +
                "JsonbAnnotation;\nimport jakarta.json.bind.annotation.JsonbProperty;\nimport jakarta." +
                "json.bind.annotation.JsonbTransient;\n\npublic class JsonbTransientDiagnostic {\n   " +
                " @JsonbTransient\n    private int id;\n\n    @JsonbProperty(\"name\")\n    @JsonbTransient\n   " +
                " private String name;    // Diagnostic: JsonbTransient is mutually exclusive with other JsonB " +
                "annotations\n\n    @JsonbProperty(\"fav_lang\")\n    @JsonbAnnotation\n    private " +
                "String favoriteLanguage;    // Diagnostic: JsonbTransient is mutually exclusive with other " +
                "JsonB annotations\n    \n    // No diagnostic as field is not annotated with other Jsonb " +
                "annotations,\n    // even though the accessors are annotated with @JsonbTransient\n    " +
                "private String favoriteDatabase;\n    \n    // Diagnostic will appear as field accessors" +
                " have @JsonbTransient,\n    // but field itself has annotation other than transient\n    " +
                "@JsonbProperty(\"fav_editor\")\n    private String favoriteEditor;\n    \n    " +
                "@JsonbProperty(\"person-id\")\n    private int getId() { \n        // A diagnostic is expected" +
                " on getId because as a getter, it is annotated with other \n        // Jsonb annotations while " +
                "its corresponding field id is annotated with JsonbTransient\n        return id;\n    }\n    \n" +
                "    @JsonbAnnotation\n    private void setId(int id) {\n        // A diagnostic is expected on" +
                " setId because as a setter, it is annotated with other \n        // Jsonb annotations while its" +
                " corresponding field id is annotated with JsonbTransient\n        this.id = id;\n    }\n    \n" +
                "    @JsonbTransient\n    private String getFavoriteDatabase() {\n        " +
                "return favoriteDatabase;\n    }\n    \n    @JsonbTransient\n    private void setFavoriteDatabase" +
                "(String favoriteDatabase) {\n        this.favoriteDatabase = favoriteDatabase;\n    }\n    \n  " +
                "  // A diagnostic will appear as field has conflicting annotation\n    @JsonbTransient\n   " +
                " private String getFavoriteEditor() {\n        return favoriteEditor;\n    }\n    \n    " +
                "// A diagnostic will appear as @JsonbTransient is not mutually exclusive on this accessor\n  " +
                "  @JsonbAnnotation\n    @JsonbTransient\n    private void setFavoriteEditor(String favoriteEditor)" +
                " {\n        this.favoriteEditor = favoriteEditor;\n    }\n}";

        TextEdit te5 = JakartaForJavaAssert.te(0, 0, 77, 1, newText);
        TextEdit te6 = JakartaForJavaAssert.te(0, 0, 77, 1, newText1);
        CodeAction ca5 = JakartaForJavaAssert.ca(uri, "Remove @JsonbTransient", d3, te5);
        CodeAction ca6 = JakartaForJavaAssert.ca(uri, "Remove @JsonbProperty, @JsonbAnnotation", d3, te6);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams3, utils, ca6, ca5);

        // Quick fix for the accessor "getId"
        JakartaJavaCodeActionParams codeActionParams4 = JakartaForJavaAssert.createCodeActionParams(uri, d5);
        newText = "/******************************************************************************* " +
                "\n* Copyright (c) 2022 IBM Corporation and others.\n *\n * This program and the " +
                "accompanying materials are made available under the\n * terms of the Eclipse Public License v. 2.0 " +
                "which is available at\n * http://www.eclipse.org/legal/epl-2.0.\n *\n * SPDX-License-Identifier: " +
                "EPL-2.0\n *\n * Contributors:\n *     Adit Rada, Yijia Jing - initial API and implementation\n " +
                "*******************************************************************************/" +
                "\n\npackage io.openliberty.sample.jakarta.jsonb;\n\nimport" +
                " jakarta.json.bind.annotation.JsonbAnnotation;\nimport jakarta.json.bind.annotation.JsonbProperty;" +
                "\nimport jakarta.json.bind.annotation.JsonbTransient;\n\npublic class JsonbTransientDiagnostic " +
                "{\n    @JsonbTransient\n    private int id;\n\n    @JsonbProperty(\"name\")\n    @JsonbTransient\n" +
                "    private String name;    // Diagnostic: JsonbTransient is mutually exclusive with other" +
                " JsonB annotations\n\n    @JsonbProperty(\"fav_lang\")\n    @JsonbAnnotation\n   " +
                " @JsonbTransient\n    private String favoriteLanguage;    // Diagnostic: JsonbTransient is" +
                " mutually exclusive with other JsonB annotations\n    \n    // No diagnostic as field is not" +
                " annotated with other Jsonb annotations,\n    // even though the accessors are annotated with" +
                " @JsonbTransient\n    private String favoriteDatabase;\n    \n    // Diagnostic will appear as" +
                " field accessors have @JsonbTransient,\n    // but field itself has annotation other than " +
                "transient\n    @JsonbProperty(\"fav_editor\")\n    private String favoriteEditor;\n    \n  " +
                "  private int getId() { \n        // A diagnostic is expected on getId because as a getter," +
                " it is annotated with other \n        // Jsonb annotations while its corresponding field id " +
                "is annotated with JsonbTransient\n        return id;\n    }\n    \n    @JsonbAnnotation\n   " +
                " private void setId(int id) {\n        // A diagnostic is expected on setId because as a setter, " +
                "it is annotated with other \n        // Jsonb annotations while its corresponding field id is " +
                "annotated with JsonbTransient\n        this.id = id;\n    }\n    \n    @JsonbTransient\n  " +
                "  private String getFavoriteDatabase() {\n        return favoriteDatabase;\n    }\n    \n " +
                "   @JsonbTransient\n    private void setFavoriteDatabase(String favoriteDatabase) {\n       " +
                " this.favoriteDatabase = favoriteDatabase;\n    }\n    \n    // A diagnostic will appear as " +
                "field has conflicting annotation\n    @JsonbTransient\n    private String getFavoriteEditor() {\n " +
                "       return favoriteEditor;\n    }\n    \n    // A diagnostic will appear as @JsonbTransient " +
                "is not mutually exclusive on this accessor\n    @JsonbAnnotation\n    @JsonbTransient\n   " +
                " private void setFavoriteEditor(String favoriteEditor) {\n        this.favoriteEditor =" +
                " favoriteEditor;\n    }\n}";


        TextEdit te7 = JakartaForJavaAssert.te(0, 0, 77, 1, newText);
        CodeAction ca7 = JakartaForJavaAssert.ca(uri, "Remove @JsonbProperty", d5, te7);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams4, utils, ca7);

        // Quick fix for the accessor "setId"
        JakartaJavaCodeActionParams codeActionParams5 = JakartaForJavaAssert.createCodeActionParams(uri, d6);
        newText = "/*******************************************************************************" +
                " \n* Copyright (c) 2022 IBM Corporation and others.\n *\n * This program and the" +
                " accompanying materials are made available under the\n * terms of the Eclipse Public License " +
                "v. 2.0 which is available at\n * http://www.eclipse.org/legal/epl-2.0.\n *\n * SPDX-License-" +
                "Identifier: EPL-2.0\n *\n * Contributors:\n *     Adit Rada, Yijia Jing - initial API and" +
                " implementation\n *******************************************************************************/" +
                "\n\npackage io.openliberty.sample.jakarta.jsonb;\n\nimport jakarta.json.bind.annotation." +
                "JsonbAnnotation;\nimport jakarta.json.bind.annotation.JsonbProperty;\nimport jakarta.json.bind." +
                "annotation.JsonbTransient;\n\npublic class JsonbTransientDiagnostic {\n    @JsonbTransient\n  " +
                "  private int id;\n\n    @JsonbProperty(\"name\")\n    @JsonbTransient\n    private String name; " +
                "   // Diagnostic: JsonbTransient is mutually exclusive with other JsonB annotations\n\n   " +
                " @JsonbProperty(\"fav_lang\")\n    @JsonbAnnotation\n    @JsonbTransient\n    private String" +
                " favoriteLanguage;    // Diagnostic: JsonbTransient is mutually exclusive with other JsonB " +
                "annotations\n    \n    // No diagnostic as field is not annotated with other Jsonb annotations,\n" +
                "    // even though the accessors are annotated with @JsonbTransient\n    private String " +
                "favoriteDatabase;\n    \n    // Diagnostic will appear as field accessors have @JsonbTransient,\n " +
                "   // but field itself has annotation other than transient\n    @JsonbProperty(\"fav_editor\")\n" +
                "    private String favoriteEditor;\n    \n    @JsonbProperty(\"person-id\")\n   " +
                " private int getId() { \n        // A diagnostic is expected on getId because as a getter," +
                " it is annotated with other \n        // Jsonb annotations while its corresponding field id is " +
                "annotated with JsonbTransient\n        return id;\n    }\n    \n    private void setId(int id) " +
                "{\n        // A diagnostic is expected on setId because as a setter, it is annotated with other " +
                "\n        // Jsonb annotations while its corresponding field id is annotated with JsonbTransient\n" +
                "        this.id = id;\n    }\n    \n    @JsonbTransient\n    private String getFavoriteDatabase()" +
                " {\n        return favoriteDatabase;\n    }\n    \n    @JsonbTransient\n    private void " +
                "setFavoriteDatabase(String favoriteDatabase) {\n        this.favoriteDatabase = favoriteDatabase;\n" +
                "    }\n    \n    // A diagnostic will appear as field has conflicting annotation\n   " +
                " @JsonbTransient\n    private String getFavoriteEditor() {\n        return favoriteEditor;\n   " +
                " }\n    \n    // A diagnostic will appear as @JsonbTransient is not mutually exclusive on this " +
                "accessor\n    @JsonbAnnotation\n    @JsonbTransient\n    private void setFavoriteEditor(String" +
                " favoriteEditor) {\n        this.favoriteEditor = favoriteEditor;\n    }\n}";

        TextEdit te8 = JakartaForJavaAssert.te(0, 0, 77, 1, newText);
        CodeAction ca8 = JakartaForJavaAssert.ca(uri, "Remove @JsonbAnnotation", d6, te8);
        JakartaForJavaAssert.assertJavaCodeAction(codeActionParams5, utils, ca8);
    }

    @Test
    public void JsonbPropertyUniquenessSubClass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/jsonb/JsonbTransientDiagnosticSubClass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d1 = JakartaForJavaAssert.d(11, 19, 36,
                "Multiple fields or properties with @JsonbProperty must not have JSON members with duplicate names, the member names must be unique.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "DuplicatePropertyNamesOnJsonbFields");
        d1.setData(new Gson().toJsonTree(Arrays.asList("jakarta.json.bind.annotation.JsonbProperty")));

        Diagnostic d2 = JakartaForJavaAssert.d(17, 19, 34,
                "Multiple fields or properties with @JsonbProperty must not have JSON members with duplicate names, the member names must be unique.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "DuplicatePropertyNamesOnJsonbFields");
        d2.setData(new Gson().toJsonTree(Arrays.asList("jakarta.json.bind.annotation.JsonbProperty")));

        Diagnostic d3 = JakartaForJavaAssert.d(20, 19, 34,
                "Multiple fields or properties with @JsonbProperty must not have JSON members with duplicate names, the member names must be unique.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "DuplicatePropertyNamesOnJsonbFields");
        d3.setData(new Gson().toJsonTree(Arrays.asList("jakarta.json.bind.annotation.JsonbProperty")));

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3);
    }

    @Test
    public void JsonbPropertyUniquenessSubSubClass() throws Exception {

        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/jsonb/JsonbTransientDiagnosticSubSubClass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d1 = JakartaForJavaAssert.d(8, 19, 31,
                "Multiple fields or properties with @JsonbProperty must not have JSON members with duplicate names, the member names must be unique.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "DuplicatePropertyNamesOnJsonbFields");
        d1.setData(new Gson().toJsonTree(Arrays.asList("jakarta.json.bind.annotation.JsonbProperty")));

        Diagnostic d2 = JakartaForJavaAssert.d(11, 19, 36,
                "Multiple fields or properties with @JsonbProperty must not have JSON members with duplicate names, the member names must be unique.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "DuplicatePropertyNamesOnJsonbFields");
        d2.setData(new Gson().toJsonTree(Arrays.asList("jakarta.json.bind.annotation.JsonbProperty")));

        Diagnostic d3 = JakartaForJavaAssert.d(14, 19, 37,
                "Multiple fields or properties with @JsonbProperty must not have JSON members with duplicate names, the member names must be unique.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "DuplicatePropertyNamesOnJsonbFields");
        d3.setData(new Gson().toJsonTree(Arrays.asList("jakarta.json.bind.annotation.JsonbProperty")));

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3);
    }
    
    @Test
    public void JsonbDeserialization() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/jsonb/JsonbDeserialization.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d1 = JakartaForJavaAssert.d(4, 13, 33,
                "Missing Public or Protected NoArgsConstructor: Class JsonbDeserialization is used with JSONB, but does not declare a public or protected no-argument constructor.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "InvalidJsonBNoArgsConstructorMissing");

        Diagnostic d2 = JakartaForJavaAssert.d(56, 21, 31,
                "Missing Public or Protected NoArgsConstructor: Class ChildClass is used with JSONB, but does not declare a public or protected no-argument constructor.",
                DiagnosticSeverity.Error, "jakarta-jsonb", "InvalidJsonBNoArgsConstructorMissing");

        Diagnostic d3 = JakartaForJavaAssert.d(83, 14, 22,
                "Cannot de-serialize class SubChild because it is non-static. Please declare the class as static for JSONB de-serialization.",
                DiagnosticSeverity.Warning, "jakarta-jsonb", "InvalidJsonBNonStaticInnerClass");

        JakartaForJavaAssert.assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3);
    }
}