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
package io.openliberty.tools.intellij.lsp4jakarta.it.persistence;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.lsp4jakarta.it.core.BaseJakartaTest;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
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

import com.google.gson.JsonArray;

import java.io.File;
import java.util.Arrays;

import static io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert.*;

@RunWith(JUnit4.class)
public class JakartaPersistenceTest extends BaseJakartaTest {

    @Test
    public void deleteMapKeyOrMapKeyClass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/MapKeyAndMapKeyClassTogether.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d1 = d(16, 32, 42,
                "@MapKeyClass and @MapKey annotations cannot be used on the same field or property.",
                DiagnosticSeverity.Error, "jakarta-persistence", "RemoveMapKeyorMapKeyClass");

        Diagnostic d2 = d(11, 25, 32,
                "@MapKeyClass and @MapKey annotations cannot be used on the same field or property.",
                DiagnosticSeverity.Error, "jakarta-persistence", "RemoveMapKeyorMapKeyClass");

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2);

        // Starting codeAction tests.
        String newText = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import java.util.HashMap;\nimport java.util.Map;\n\n" +
                "import jakarta.persistence.MapKey;\nimport jakarta.persistence.MapKeyClass;\n\n" +
                "public class MapKeyAndMapKeyClassTogether {\n    @MapKey()\n" +
                "    @MapKeyClass(Map.class)\n    Map<Integer, String> testMap = new HashMap<>();\n" +
                "    \n    \n    @MapKey()\n    public Map<Integer, String> getTestMap(){\n" +
                "    	return this.testMap;\n    }\n}\n";

        String newText1 = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import java.util.HashMap;\nimport java.util.Map;\n\n" +
                "import jakarta.persistence.MapKey;\nimport jakarta.persistence.MapKeyClass;\n\n" +
                "public class MapKeyAndMapKeyClassTogether {\n    @MapKey()\n    @MapKeyClass(Map.class)\n" +
                "    Map<Integer, String> testMap = new HashMap<>();\n    \n    \n" +
                "    @MapKeyClass(Map.class)\n    public Map<Integer, String> getTestMap(){\n" +
                "    	return this.testMap;\n    }\n}\n";

        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d1);

            TextEdit te1 = te(0, 0, 20, 0, newText);
            TextEdit te2 = te(0, 0, 20, 0, newText1);
            CodeAction ca1 = ca(uri, "Remove @MapKeyClass", d1, te1);
            CodeAction ca2 = ca(uri, "Remove @MapKey", d1, te2);

            assertJavaCodeAction(codeActionParams1, utils, ca2, ca1);

        String newText2 = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import java.util.HashMap;\nimport java.util.Map;\n\n" +
                "import jakarta.persistence.MapKey;\nimport jakarta.persistence.MapKeyClass;\n\n" +
                "public class MapKeyAndMapKeyClassTogether {\n    @MapKey()\n" +
                "    Map<Integer, String> testMap = new HashMap<>();\n    \n    \n    @MapKey()\n" +
                "    @MapKeyClass(Map.class)\n    public Map<Integer, String> getTestMap(){\n" +
                "    	return this.testMap;\n    }\n}\n";

        String newText3 = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import java.util.HashMap;\nimport java.util.Map;\n\n" +
                "import jakarta.persistence.MapKey;\nimport jakarta.persistence.MapKeyClass;\n\n" +
                "public class MapKeyAndMapKeyClassTogether {\n    @MapKeyClass(Map.class)\n" +
                "    Map<Integer, String> testMap = new HashMap<>();\n    \n    \n    @MapKey()\n" +
                "    @MapKeyClass(Map.class)\n    public Map<Integer, String> getTestMap(){\n" +
                "    	return this.testMap;\n    }\n}\n";

        JakartaJavaCodeActionParams codeActionParams2 = createCodeActionParams(uri, d2);

            TextEdit te3 = te(0, 0, 20, 0, newText2);
            TextEdit te4 = te(0, 0, 20, 0, newText3);
            CodeAction ca3 = ca(uri, "Remove @MapKeyClass", d2, te3);
            CodeAction ca4 = ca(uri, "Remove @MapKey", d2, te4);

            assertJavaCodeAction(codeActionParams2, utils, ca4, ca3);
    }

    @Test
    public void completeMapKeyJoinColumnAnnotation() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/MultipleMapKeyAnnotations.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();
        
        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // test diagnostics are present
        Diagnostic d1 = d(16, 25, 30,
                "A field with multiple @MapKeyJoinColumn annotations must specify both the name and referencedColumnName attributes in the corresponding @MapKeyJoinColumn annotations.",
                DiagnosticSeverity.Error, "jakarta-persistence", "SupplyAttributesToAnnotations");
        Diagnostic d2 = d(16, 25, 30,
                "A field with multiple @MapKeyJoinColumn annotations must specify both the name and referencedColumnName attributes in the corresponding @MapKeyJoinColumn annotations.",
                DiagnosticSeverity.Error, "jakarta-persistence", "SupplyAttributesToAnnotations");

        Diagnostic d3 = d(20, 25, 30,
                "A field with multiple @MapKeyJoinColumn annotations must specify both the name and referencedColumnName attributes in the corresponding @MapKeyJoinColumn annotations.",
                DiagnosticSeverity.Error, "jakarta-persistence", "SupplyAttributesToAnnotations");
        Diagnostic d4 = d(20, 25, 30,
                "A field with multiple @MapKeyJoinColumn annotations must specify both the name and referencedColumnName attributes in the corresponding @MapKeyJoinColumn annotations.",
                DiagnosticSeverity.Error, "jakarta-persistence", "SupplyAttributesToAnnotations");

        Diagnostic d5 = d(24, 25, 30,
                "A field with multiple @MapKeyJoinColumn annotations must specify both the name and referencedColumnName attributes in the corresponding @MapKeyJoinColumn annotations.",
                DiagnosticSeverity.Error, "jakarta-persistence", "SupplyAttributesToAnnotations");

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3, d4, d5);
        String newText = "package io.openliberty.sample.jakarta.persistence;\n\nimport java.util.Map;\n\n" +
                "import jakarta.persistence.Entity;\nimport jakarta.persistence.Id;\nimport jakarta.persistence.MapKeyJoinColumn;\n\n" +
                "@Entity\npublic class MultipleMapKeyAnnotations {\n\n    @Id\n    int id;\n\n    @MapKeyJoinColumn(name=\"\",referencedColumnName=\"\")\n    " +
                "@MapKeyJoinColumn(name=\"\",referencedColumnName=\"\")\n    Map<Integer, String> test1;\n    \n    " +
                "@MapKeyJoinColumn(name = \"n1\")\n    @MapKeyJoinColumn(referencedColumnName = \"rcn2\")\n    " +
                "Map<Integer, String> test2;\n    \n    @MapKeyJoinColumn(name = \"n1\", referencedColumnName = \"rcn1\")\n    " +
                "@MapKeyJoinColumn()\n    Map<Integer, String> test3;\n}";
        String newText1 = "package io.openliberty.sample.jakarta.persistence;\n\nimport java.util.Map;\n\n" +
                "import jakarta.persistence.Entity;\nimport jakarta.persistence.Id;\nimport jakarta.persistence.MapKeyJoinColumn;\n\n" +
                "@Entity\npublic class MultipleMapKeyAnnotations {\n\n    @Id\n    int id;\n\n    @MapKeyJoinColumn()\n    @MapKeyJoinColumn()\n    " +
                "Map<Integer, String> test1;\n    \n    @MapKeyJoinColumn(name = \"n1\",referencedColumnName=\"\")\n    " +
                "@MapKeyJoinColumn(referencedColumnName = \"rcn2\",name=\"\")\n    Map<Integer, String> test2;\n    \n    " +
                "@MapKeyJoinColumn(name = \"n1\", referencedColumnName = \"rcn1\")\n    @MapKeyJoinColumn()\n    " +
                "Map<Integer, String> test3;\n}";
        String newText2 = "package io.openliberty.sample.jakarta.persistence;\n\nimport java.util.Map;\n\n" +
                "import jakarta.persistence.Entity;\nimport jakarta.persistence.Id;\nimport jakarta.persistence.MapKeyJoinColumn;\n\n" +
                "@Entity\npublic class MultipleMapKeyAnnotations {\n\n    @Id\n    int id;\n\n    @MapKeyJoinColumn()\n    @MapKeyJoinColumn()\n    " +
                "Map<Integer, String> test1;\n    \n    @MapKeyJoinColumn(name = \"n1\")\n    @MapKeyJoinColumn(referencedColumnName = \"rcn2\")\n    " +
                "Map<Integer, String> test2;\n    \n    @MapKeyJoinColumn(name = \"n1\", referencedColumnName = \"rcn1\")\n    " +
                "@MapKeyJoinColumn(name=\"\",referencedColumnName=\"\")\n    Map<Integer, String> test3;\n}";

        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d1);
        TextEdit te1 = te(0, 0, 25, 1, newText);
        CodeAction ca1 = ca(uri, "Add the missing attributes to the @MapKeyJoinColumn annotation", d1, te1);

        assertJavaCodeAction(codeActionParams1, utils, ca1);

        JakartaJavaCodeActionParams codeActionParams2 = createCodeActionParams(uri, d3);
        TextEdit te2 = te(0, 0, 25, 1, newText1);
        CodeAction ca2 = ca(uri, "Add the missing attributes to the @MapKeyJoinColumn annotation", d3, te2);

        assertJavaCodeAction(codeActionParams2, utils, ca2);

        JakartaJavaCodeActionParams codeActionParams3 = createCodeActionParams(uri, d5);
        TextEdit te3 = te(0, 0, 25, 1, newText2);
        CodeAction ca3 = ca(uri, "Add the missing attributes to the @MapKeyJoinColumn annotation", d5, te3);

        assertJavaCodeAction(codeActionParams3, utils, ca3);
    }

    @Test
    public void addEmptyConstructor() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityMissingConstructor.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // test diagnostics are present
        Diagnostic d = d(6, 13, 37,
                "A class using the @Entity annotation must contain a public or protected constructor with no arguments.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MissingEmptyConstructor");

        assertJavaDiagnostics(diagnosticsParams, utils, d);

        // test quick fixes
        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d);
        TextEdit te1 = te(0, 0, 13, 1, "package io.openliberty.sample.jakarta.persistence;\n\nimport jakarta.persistence.Entity;\nimport jakarta.persistence.Id;\n\n@Entity\npublic class EntityMissingConstructor {\n\n    @Id\n    int id;\n\n    protected EntityMissingConstructor() {\n    }\n\n    private EntityMissingConstructor(int x) {\n    }\n\n}");
        CodeAction ca1 = ca(uri, Messages.getMessage("AddNoArgProtectedConstructor"), d, te1);
        TextEdit te2 = te(0, 0, 13, 1, "package io.openliberty.sample.jakarta.persistence;\n\nimport jakarta.persistence.Entity;\nimport jakarta.persistence.Id;\n\n@Entity\npublic class EntityMissingConstructor {\n\n    @Id\n    int id;\n\n    public EntityMissingConstructor() {\n    }\n\n    private EntityMissingConstructor(int x) {\n    }\n\n}");
        CodeAction ca2 = ca(uri, Messages.getMessage("AddNoArgPublicConstructor"), d, te2);

        assertJavaCodeAction(codeActionParams1, utils, ca1, ca2);
    }

    @Test
    public void removeFinalModifiers() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/FinalModifiers.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // test diagnostics are present
        Diagnostic d1 = d(13, 21, 28,
                "A class using the @Entity annotation cannot contain any methods that are declared final.",
                DiagnosticSeverity.Error, "jakarta-persistence", "RemoveFinalMethods");
        d1.setData("int");

        Diagnostic d2 = d(10, 14, 15,
                "A class using the @Entity annotation cannot contain any persistent instance variables that are declared final.",
                DiagnosticSeverity.Error, "jakarta-persistence", "RemoveFinalVariables");
        d2.setData("int");

        Diagnostic d3 = d(11, 17, 18,
                "A class using the @Entity annotation cannot contain any persistent instance variables that are declared final.",
                DiagnosticSeverity.Error, "jakarta-persistence", "RemoveFinalVariables");
        d3.setData("java.lang.String");

        Diagnostic d4 = d(11, 30, 31,
                "A class using the @Entity annotation cannot contain any persistent instance variables that are declared final.",
                DiagnosticSeverity.Error, "jakarta-persistence", "RemoveFinalVariables");
        d4.setData("java.lang.String");

        Diagnostic d5 = d(6, 19, 33,
                "A class using the @Entity annotation must not be final.",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidClass");
        d5.setData("io.openliberty.sample.jakarta.persistence.FinalModifiers");

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3, d4, d5);

        // test quick fixes
        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d1);
        String newText1 = "package io.openliberty.sample.jakarta.persistence;\n\nimport jakarta.persistence.Entity;\nimport jakarta.persistence.Id;\n\n@Entity\n" +
                "public final class FinalModifiers {\n\n    " +
                "@Id\n    "+
                "int id;\n    "+
                "final int x = 1;\n    " +
                "final String y = \"hello\", z = \"world\";\n    \n    " +
                "public int methody() {\n        final int ret = 100;\n        return 100 + ret;\n    }\n}";
        TextEdit te1 = te(0, 0, 17, 1, newText1);
        CodeAction ca1 = ca(uri, "Remove the 'final' modifier from this method", d1, te1);

        assertJavaCodeAction(codeActionParams1, utils, ca1);

        JakartaJavaCodeActionParams codeActionParams2 = createCodeActionParams(uri, d2);
        String newText2 = "package io.openliberty.sample.jakarta.persistence;\n\nimport jakarta.persistence.Entity;\nimport jakarta.persistence.Id;\n\n@Entity\n" +
                "public final class FinalModifiers {\n\n    " +
                "@Id\n    "+
                "int id;\n    "+
                "int x = 1;\n    " +
                "final String y = \"hello\", z = \"world\";\n    \n    " +
                "public final int methody() {\n        final int ret = 100;\n        return 100 + ret;\n    }\n}";
        TextEdit te2 = te(0, 0, 17, 1, newText2);
        CodeAction ca2 = ca(uri, "Remove the 'final' modifier from this field", d2, te2);

        assertJavaCodeAction(codeActionParams2, utils, ca2);

        JakartaJavaCodeActionParams codeActionParams3 = createCodeActionParams(uri, d3);
        String newText3 = "package io.openliberty.sample.jakarta.persistence;\n\nimport jakarta.persistence.Entity;\nimport jakarta.persistence.Id;\n\n@Entity\n" +
                "public final class FinalModifiers {\n\n    " +
                "@Id\n    "+
                "int id;\n    "+
                "final int x = 1;\n    " +
                "String y = \"hello\", z = \"world\";\n    \n    " +
                "public final int methody() {\n        final int ret = 100;\n        return 100 + ret;\n    }\n}";
        TextEdit te3 = te(0, 0, 17, 1, newText3);
        CodeAction ca3 = ca(uri, "Remove the 'final' modifier from this field", d3, te3);

        assertJavaCodeAction(codeActionParams3, utils, ca3);

        JakartaJavaCodeActionParams codeActionParams4 = createCodeActionParams(uri, d4);
        String newText4 = "package io.openliberty.sample.jakarta.persistence;\n\nimport jakarta.persistence.Entity;\nimport jakarta.persistence.Id;\n\n@Entity\n" +
                "public final class FinalModifiers {\n\n    " +
                "@Id\n    "+
                "int id;\n    "+
                "final int x = 1;\n    " +
                "String y = \"hello\", z = \"world\";\n    \n    " +
                "public final int methody() {\n        final int ret = 100;\n        return 100 + ret;\n    }\n}";
        TextEdit te4 = te(0, 0, 17, 1, newText4);
        CodeAction ca4 = ca(uri, "Remove the 'final' modifier from this field", d4, te4);

        assertJavaCodeAction(codeActionParams4, utils, ca4);

        JakartaJavaCodeActionParams codeActionParams5 = createCodeActionParams(uri, d5);
        String newText5 = "package io.openliberty.sample.jakarta.persistence;\n\nimport jakarta.persistence.Entity;\nimport jakarta.persistence.Id;\n\n@Entity\n" +
                "public class FinalModifiers {\n\n    " +
                "@Id\n    "+
                "int id;\n    "+
                "final int x = 1;\n    " +
                "final String y = \"hello\", z = \"world\";\n    \n    " +
                "public final int methody() {\n        final int ret = 100;\n        return 100 + ret;\n    }\n}";
        TextEdit te5 = te(0, 0, 17, 1, newText5);
        CodeAction ca5 = ca(uri, "Remove the 'final' modifier from this class", d5, te5);

        assertJavaCodeAction(codeActionParams5, utils, ca5);
    }

    @Test
    public void testMethodOrFieldType() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/MapKeyAnnotationsType.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d1 = d(27, 19, 25,
                "`@MapKey` annotation can only be applied to methods with a return type of java.util.Map.",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidReturnTypeOfMethod");

        Diagnostic d2 = d(13, 11, 15,
                "`@MapKey` annotation can only be applied to fields of type java.util.Map.",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidTypeOfField");

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2);
    }

    @Test
    public void testAccessorAndNamingConventions() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/MapKeyAnnotationsGetterConvention.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d1 = d(37, 33, 41,
                "Method is not public and may not be accessible as expected.",
                DiagnosticSeverity.Warning, "jakarta-persistence", "InvalidMethodAccessSpecifier");

        Diagnostic d2 = d(42, 33, 41,
                "This method does not conform to persistent property getter naming conventions.",
                DiagnosticSeverity.Warning, "jakarta-persistence", "InvalidMethodName");

        Diagnostic d3 = d(47, 32, 42,
                "Method has no matching field name.",
                DiagnosticSeverity.Warning, "jakarta-persistence", "InvalidMapKeyAnnotationsFieldNotFound");

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3);
    }


    @Test
    public void testIdDateMissingTemporal() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityIdDateMissingTemporal.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic idDateMissingTemporalD1 = d(11, 14, 16,
                "A field or property marked with @Id and of type java.util.Date must explicitly specify @Temporal(TemporalType.DATE).",
                DiagnosticSeverity.Error, "jakarta-persistence", "MissingTemporalAnnotation");

        assertJavaDiagnostics(diagnosticsParams, utils, idDateMissingTemporalD1);

        // test quick fix
        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, idDateMissingTemporalD1);
        String newText = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import java.util.Date;\n\n" +
                "import jakarta.persistence.Entity;\n" +
                "import jakarta.persistence.Id;\n" +
                "import jakarta.persistence.Temporal;\n" +
                "import jakarta.persistence.TemporalType;\n\n" +
                "@Entity\n" +
                "public class EntityIdDateMissingTemporal {\n\n" +
                "    @Temporal(TemporalType.DATE)\n" +
                "    @Id\n" +
                "    private Date pk;\n\n" +
                "\tpublic Date getPk() {\n" +
                "\t\treturn pk;\n" +
                "\t}\n\n" +
                "\tpublic void setPk(Date pk) {\n" +
                "\t\tthis.pk = pk;\n" +
                "\t}\n" +
                "\t\n" +
                "\t\n" +
                "}\n";
        TextEdit idDateMissingTemporalTE1 = te(0, 0, 23, 0, newText);
        CodeAction idDateMissingTemporalCA1 = ca(uri, "Insert @Temporal(TemporalType.DATE)", idDateMissingTemporalD1, idDateMissingTemporalTE1);

        assertJavaCodeAction(codeActionParams1, utils, idDateMissingTemporalCA1);
    }

    @Test
    public void testPropertyIdDateMissingTemporal() throws Exception{
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityPropertyIdDateMissingTemporal.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic propertyIdDateMissingTemporalD1 = d(13, 13, 18,
                "A field or property marked with @Id and of type java.util.Date must explicitly specify @Temporal(TemporalType.DATE).",
                DiagnosticSeverity.Error, "jakarta-persistence", "MissingTemporalAnnotation");

        assertJavaDiagnostics(diagnosticsParams, utils, propertyIdDateMissingTemporalD1);

        // test quick fix
        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, propertyIdDateMissingTemporalD1);
        String newText = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import java.util.Date;\n\n" +
                "import jakarta.persistence.Entity;\n" +
                "import jakarta.persistence.Id;\n" +
                "import jakarta.persistence.Temporal;\n" +
                "import jakarta.persistence.TemporalType;\n\n" +
                "@Entity\n" +
                "public class EntityPropertyIdDateMissingTemporal {\n\n" +
                "\tprivate Date pk;\n\n" +
                "    @Temporal(TemporalType.DATE)\n" +
                "    @Id\n" +
                "    public Date getPk() {\n" +
                "\t\treturn pk;\n" +
                "\t}\n\n" +
                "\tpublic void setPk(Date pk) {\n" +
                "\t\tthis.pk = pk;\n" +
                "\t}\n" +
                "\t\n" +
                "\t\n" +
                "}\n";
        TextEdit propertyIdDateMissingTemporalTE1 = te(0, 0, 23, 0, newText);
        CodeAction propertyIdDateMissingTemporalCA1 = ca(uri, "Insert @Temporal(TemporalType.DATE)", propertyIdDateMissingTemporalD1, propertyIdDateMissingTemporalTE1);

        assertJavaCodeAction(codeActionParams1, utils, propertyIdDateMissingTemporalCA1);
    }

    @Test
    public void testIdDateInvalidTemporalType() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityInvalidTemporalType.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic idDateInvalidTemporalTypeD1 = d(13, 1, 29,
                "The @Temporal annotation on a field or property annotated with @Id and of type java.util.Date must specify TemporalType.DATE.",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidValueInTemporalAnnotation");

        assertJavaDiagnostics(diagnosticsParams, utils, idDateInvalidTemporalTypeD1);

        // test quick fix
        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, idDateInvalidTemporalTypeD1);
        String newText = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import java.util.Date;\n\n" +
                "import jakarta.persistence.Entity;\n" +
                "import jakarta.persistence.Id;\n" +
                "import jakarta.persistence.Temporal;\n" +
                "import jakarta.persistence.TemporalType;\n\n" +
                "@Entity\n" +
                "public class EntityInvalidTemporalType {\n\n" +
                "\t@Id\n" +
                "\t@Temporal(TemporalType.DATE)\n" +
                "\tprivate Date pk;\n\n" +
                "\tpublic Date getPk() {\n" +
                "\t\treturn pk;\n" +
                "\t}\n\n" +
                "\tpublic void setPk(Date pk) {\n" +
                "\t\tthis.pk = pk;\n" +
                "\t}\n" +
                "}\n";
        TextEdit idDateInvalidTemporalTypeTE1 = te(0, 0, 24, 0, newText);
        CodeAction idDateInvalidTemporalTypeCA1 = ca(uri, "Change @Temporal value to TemporalType.DATE", idDateInvalidTemporalTypeD1, idDateInvalidTemporalTypeTE1);

        assertJavaCodeAction(codeActionParams1, utils, idDateInvalidTemporalTypeCA1);
    }

    @Test
    public void testPropertyIdDateInvalidTemporalType() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityPropertyInvalidTemporalType.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic propertyIdDateInvalidTemporalTypeD1 = d(15, 1, 29,
                "The @Temporal annotation on a field or property annotated with @Id and of type java.util.Date must specify TemporalType.DATE.",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidValueInTemporalAnnotation");

        assertJavaDiagnostics(diagnosticsParams, utils, propertyIdDateInvalidTemporalTypeD1);

        // test quick fix
        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, propertyIdDateInvalidTemporalTypeD1);
        String newText = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import java.util.Date;\n\n" +
                "import jakarta.persistence.Entity;\n" +
                "import jakarta.persistence.Id;\n" +
                "import jakarta.persistence.Temporal;\n" +
                "import jakarta.persistence.TemporalType;\n\n" +
                "@Entity\n" +
                "public class EntityPropertyInvalidTemporalType {\n\n" +
                "\tprivate Date pk;\n\n" +
                "\t@Id\n" +
                "\t@Temporal(TemporalType.DATE)\n" +
                "\tpublic Date getPk() {\n" +
                "\t\treturn pk;\n" +
                "\t}\n\n" +
                "\tpublic void setPk(Date pk) {\n" +
                "\t\tthis.pk = pk;\n" +
                "\t}\n" +
                "}\n";
        TextEdit te1 = te(0, 0, 24, 0, newText);
        CodeAction ca1 = ca(uri, "Change @Temporal value to TemporalType.DATE", propertyIdDateInvalidTemporalTypeD1, te1);

        assertJavaCodeAction(codeActionParams1, utils, ca1);
    }


    @Test
    public void missingPrimaryKey() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityMissingPrimaryKey.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        Diagnostic d1 = d(5, 13, 36,
                "A class using the @Entity annotation must define a primary key using @Id or @EmbeddedId.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MissingPrimaryKey");

        assertJavaDiagnostics(diagnosticsParams, utils, d1);
    }

    @Test
    public void testEntityWithEmbeddedId() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityWithEmbeddedId.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void testEntityWithIdOnGetter() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityWithIdOnGetter.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void testEntityWithEmbeddedIdOnGetter() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityWithEmbeddedIdOnGetter.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void testEntityWithMappedSuperclass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityWithMappedSuperclass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void testEntityWithMappedSuperclassIdOnGetter() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityWithMappedSuperclassIdOnGetter.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }


    @Test
    public void testDuplicateVersionAnnotationOnFields() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityDuplicateVersion.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostics for duplicate @Version annotations on fields
        Diagnostic duplicateVersionD1 = d(13, 16, 24,
                "Multiple fields or properties are annotated with @Version. Only one @Version annotation is allowed per entity class.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MultipleVersionAnnotations");

        Diagnostic duplicateVersiond2 = d(16, 16, 24,
                "Multiple fields or properties are annotated with @Version. Only one @Version annotation is allowed per entity class.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MultipleVersionAnnotations");

        assertJavaDiagnostics(diagnosticsParams, utils, duplicateVersionD1, duplicateVersiond2);
    }

    @Test
    public void testDuplicateVersionAnnotationOnMethods() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityDuplicateVersionMethods.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostics for duplicate @Version annotations on methods
        Diagnostic duplicateVersionInMethodD1 = d(18, 15, 26,
                "Multiple fields or properties are annotated with @Version. Only one @Version annotation is allowed per entity class.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MultipleVersionAnnotations");

        Diagnostic duplicateVersionInMethodD2 = d(23, 15, 26,
                "Multiple fields or properties are annotated with @Version. Only one @Version annotation is allowed per entity class.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MultipleVersionAnnotations");

        assertJavaDiagnostics(diagnosticsParams, utils, duplicateVersionInMethodD1, duplicateVersionInMethodD2);
    }

    @Test
    public void testVersionAnnotationInHierarchy() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityChildWithVersion.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostic for @Version annotation in both parent and child entity
        Diagnostic versionInHierarchyD1 = d(9, 16, 28,
                "A @Version annotation is already present in the entity hierarchy. Only one @Version annotation is allowed across the entire entity inheritance hierarchy.",
                DiagnosticSeverity.Error, "jakarta-persistence", "VersionAnnotationInHierarchy");

        assertJavaDiagnostics(diagnosticsParams, utils, versionInHierarchyD1);
    }
    @Test
    public void testInvalidVersionFieldType() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityInvalidVersionFieldType.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostic for invalid @Version field type (String)
        Diagnostic invalidVersionTypeD1 = d(13, 19, 26,
                "A field or property annotated with @Version must be of type int, Integer, short, Short, long, Long, or java.sql.Timestamp.",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidVersionFieldOrPropertyType");

        assertJavaDiagnostics(diagnosticsParams, utils, invalidVersionTypeD1);
    }

    @Test
    public void testValidVersionFieldType() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityValidVersionFieldType.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test that no diagnostics are generated for valid @Version field type (int)
        assertJavaDiagnostics(diagnosticsParams, utils);
    }
    
    @Test
    public void testInvalidVersionMethodType() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityInvalidVersionMethodType.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test diagnostic for invalid @Version method return type (String)
        Diagnostic invalidVersionTypeD1 = d(26, 18, 28,
                "A field or property annotated with @Version must be of type int, Integer, short, Short, long, Long, or java.sql.Timestamp.",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidVersionFieldOrPropertyType");

        assertJavaDiagnostics(diagnosticsParams, utils, invalidVersionTypeD1);
    }

    @Test
    public void testValidVersionMethodType() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityValidVersionMethodType.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test that no diagnostics are generated for valid @Version method return type (long)
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void testValidVersionTimestampType() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/EntityValidVersionTimestamp.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Test that no diagnostics are generated for valid @Version method return type (Timestamp)
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void testMapKeyTemporalValidation() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        // Test invalid cases
        VirtualFile javaFileInvalid = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/MapKeyTemporalInvalid.java");
        String uriInvalid = VfsUtilCore.virtualToIoFile(javaFileInvalid).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParamsInvalid = new JakartaJavaDiagnosticsParams();
        diagnosticsParamsInvalid.setUris(Arrays.asList(uriInvalid));

        // Invalid: map key is String (field)
        Diagnostic invalidStringKeyField = d(19, 32, 44,
                "@MapKeyTemporal can only be used when the map key type is java.util.Date or java.util.Calendar.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MapKeyTemporalNotOnTemporalType");

        // Invalid: map key is Integer (field)
        Diagnostic invalidIntegerKeyField = d(24, 33, 46,
                "@MapKeyTemporal can only be used when the map key type is java.util.Date or java.util.Calendar.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MapKeyTemporalNotOnTemporalType");

        // Invalid: map key is String (method)
        Diagnostic invalidStringKeyMethod = d(29, 31, 46,
                "@MapKeyTemporal can only be used when the map key type is java.util.Date or java.util.Calendar.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MapKeyTemporalNotOnTemporalType");

        // Invalid: map key is Integer (method)
        Diagnostic invalidIntegerKeyMethod = d(36, 32, 48,
                "@MapKeyTemporal can only be used when the map key type is java.util.Date or java.util.Calendar.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MapKeyTemporalNotOnTemporalType");

        // Invalid: FQN String key (field)
        Diagnostic invalidFqnStringKeyField = d(43, 42, 57,
                "@MapKeyTemporal can only be used when the map key type is java.util.Date or java.util.Calendar.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MapKeyTemporalNotOnTemporalType");

        assertJavaDiagnostics(diagnosticsParamsInvalid, utils, invalidStringKeyField, invalidIntegerKeyField,
                invalidStringKeyMethod, invalidIntegerKeyMethod, invalidFqnStringKeyField);

        // Test quickfix for removing @MapKeyTemporal annotation
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uriInvalid, invalidStringKeyField);

        String newText = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import java.util.Map;\n\n" +
                "import jakarta.persistence.ElementCollection;\n" +
                "import jakarta.persistence.Entity;\n" +
                "import jakarta.persistence.Id;\n" +
                "import jakarta.persistence.MapKeyTemporal;\n" +
                "import jakarta.persistence.TemporalType;\n\n" +
                "@Entity\n" +
                "public class MapKeyTemporalInvalid {\n\n" +
                "    @Id\n" +
                "    private Long id;\n\n" +
                "    // Invalid: map key is String\n" +
                "    @ElementCollection\n" +
                "    private Map<String, String> stringEvents;\n\n" +
                "    // Invalid: map key is Integer\n" +
                "    @ElementCollection\n" +
                "    @MapKeyTemporal(TemporalType.DATE)\n" +
                "    private Map<Integer, String> integerEvents;\n\n" +
                "    // Invalid: getter with String key\n" +
                "    @ElementCollection\n" +
                "    @MapKeyTemporal(TemporalType.DATE)\n" +
                "    public Map<String, String> getStringEvents() {\n" +
                "        return this.stringEvents;\n" +
                "    }\n\n" +
                "    // Invalid: getter with Integer key\n" +
                "    @ElementCollection\n" +
                "    @MapKeyTemporal(TemporalType.TIMESTAMP)\n" +
                "    public Map<Integer, String> getIntegerEvents() {\n" +
                "        return this.integerEvents;\n" +
                "    }\n\n" +
                "    // Invalid: FQN String key without import\n" +
                "    @ElementCollection\n" +
                "    @MapKeyTemporal(TemporalType.DATE)\n" +
                "    private Map<java.lang.String, String> fqnStringEvents;\n" +
                "}\n";

        TextEdit te = te(0, 0, 45, 0, newText);
        CodeAction ca = ca(uriInvalid, "Remove @MapKeyTemporal", invalidStringKeyField, te);

        assertJavaCodeAction(codeActionParams, utils, ca);

        // Test valid cases - should have no diagnostics
        VirtualFile javaFileValid = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/MapKeyTemporalValid.java");
        String uriValid = VfsUtilCore.virtualToIoFile(javaFileValid).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParamsValid = new JakartaJavaDiagnosticsParams();
        diagnosticsParamsValid.setUris(Arrays.asList(uriValid));

        assertJavaDiagnostics(diagnosticsParamsValid, utils);
    }

    @Test
    public void testNamedEntityGraphOnValidEntityClass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/NamedEntityGraphOnValidEntityClass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void testNamedEntityGraphOnNonEntityClass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/NamedEntityGraphOnNonEntityClass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        JsonArray namedEntityGraphData = new JsonArray();
        namedEntityGraphData.add("jakarta.persistence.NamedEntityGraph");
        Diagnostic expectedDiagnostic = d(4, 0, 38,
                "@NamedEntityGraph must only be applied to a class annotated with @Entity.",
                DiagnosticSeverity.Error, "jakarta-persistence", "NamedEntityGraphOnNonEntityClass", namedEntityGraphData);

        assertJavaDiagnostics(diagnosticsParams, utils, expectedDiagnostic);

        // test quick fixes
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, expectedDiagnostic);

        String insertNewText = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.Entity;\n" +
                "import jakarta.persistence.NamedEntityGraph;\n\n" +
                "@Entity\n" +
                "@NamedEntityGraph(name = \"Graph.User\")\n" +
                "public class NamedEntityGraphOnNonEntityClass {\n}\n";
        TextEdit insertTE = te(0, 0, 7, 0, insertNewText);
        CodeAction insertCA = ca(uri, "Insert @Entity", expectedDiagnostic, insertTE);

        String removeNewText = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.NamedEntityGraph;\n\n" +
                "public class NamedEntityGraphOnNonEntityClass {\n}\n";
        TextEdit removeTE = te(0, 0, 7, 0, removeNewText);
        CodeAction removeCA = ca(uri, "Remove @NamedEntityGraph", expectedDiagnostic, removeTE);

        assertJavaCodeAction(codeActionParams, utils, insertCA, removeCA);
    }

    @Test
    public void testNamedQueryOnValidClass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/NamedQueryOnValidClass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void testNamedQueryOnInvalidClass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/NamedQueryOnInvalidClass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        JsonArray namedQueryData = new JsonArray();
        namedQueryData.add("jakarta.persistence.NamedQuery");
        Diagnostic expectedDiagnostic = d(4, 0, 66,
                "@NamedQuery must only be applied to a class annotated with @Entity or @MappedSuperclass.",
                DiagnosticSeverity.Error, "jakarta-persistence", "NamedQueryOnInvalidClass", namedQueryData);

        assertJavaDiagnostics(diagnosticsParams, utils, expectedDiagnostic);

        // test quick fixes
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, expectedDiagnostic);

        String insertEntityText = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.Entity;\n" +
                "import jakarta.persistence.MappedSuperclass;\n" +
                "import jakarta.persistence.NamedQuery;\n\n" +
                "@MappedSuperclass\n" +
                "@Entity\n" +
                "@NamedQuery(name = \"User.findAll\", query = \"SELECT u FROM User u\")\n" +
                "public class NamedQueryOnInvalidClass {\n}\n";
        TextEdit insertEntityTE = te(0, 0, 7, 0, insertEntityText);
        CodeAction insertEntityCA = ca(uri, "Insert @Entity", expectedDiagnostic, insertEntityTE);

        String insertMappedSuperclassText = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.Entity;\n" +
                "import jakarta.persistence.MappedSuperclass;\n" +
                "import jakarta.persistence.NamedQuery;\n\n" +
                "@MappedSuperclass\n" +
                "@Entity\n" +
                "@NamedQuery(name = \"User.findAll\", query = \"SELECT u FROM User u\")\n" +
                "public class NamedQueryOnInvalidClass {\n}\n";
        TextEdit insertMappedSuperclassTE = te(0, 0, 7, 0, insertMappedSuperclassText);
        CodeAction insertMappedSuperclassCA = ca(uri, "Insert @MappedSuperclass", expectedDiagnostic, insertMappedSuperclassTE);

        String removeText = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.NamedQuery;\n\n" +
                "public class NamedQueryOnInvalidClass {\n}\n";
        TextEdit removeTE = te(0, 0, 7, 0, removeText);
        CodeAction removeCA = ca(uri, "Remove @NamedQuery", expectedDiagnostic, removeTE);

        assertJavaCodeAction(codeActionParams, utils, insertEntityCA, insertMappedSuperclassCA, removeCA);
    }

    @Test
    public void testNamedNativeQueryOnValidClass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/NamedNativeQueryOnValidClass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void testNamedNativeQueryOnInvalidClass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/NamedNativeQueryOnInvalidClass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        JsonArray namedNativeQueryData = new JsonArray();
        namedNativeQueryData.add("jakarta.persistence.NamedNativeQuery");
        Diagnostic expectedDiagnostic = d(4, 0, 136,
                "@NamedNativeQuery must only be applied to a class annotated with @Entity or @MappedSuperclass.",
                DiagnosticSeverity.Error, "jakarta-persistence", "NamedNativeQueryOnInvalidClass", namedNativeQueryData);

        assertJavaDiagnostics(diagnosticsParams, utils, expectedDiagnostic);

        // test quick fixes
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, expectedDiagnostic);

        String insertEntityText = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.Entity;\n" +
                "import jakarta.persistence.MappedSuperclass;\n" +
                "import jakarta.persistence.NamedNativeQuery;\n\n" +
                "@MappedSuperclass\n" +
                "@Entity\n" +
                "@NamedNativeQuery(name = \"User.findById\", query = \"SELECT * FROM USER WHERE ID = ?\", resultClass = NamedNativeQueryOnInvalidClass.class)\n" +
                "public class NamedNativeQueryOnInvalidClass {\n}\n";
        TextEdit insertEntityTE = te(0, 0, 7, 0, insertEntityText);
        CodeAction insertEntityCA = ca(uri, "Insert @Entity", expectedDiagnostic, insertEntityTE);

        String insertMappedSuperclassText = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.Entity;\n" +
                "import jakarta.persistence.MappedSuperclass;\n" +
                "import jakarta.persistence.NamedNativeQuery;\n\n" +
                "@MappedSuperclass\n" +
                "@Entity\n" +
                "@NamedNativeQuery(name = \"User.findById\", query = \"SELECT * FROM USER WHERE ID = ?\", resultClass = NamedNativeQueryOnInvalidClass.class)\n" +
                "public class NamedNativeQueryOnInvalidClass {\n}\n";
        TextEdit insertMappedSuperclassTE = te(0, 0, 7, 0, insertMappedSuperclassText);
        CodeAction insertMappedSuperclassCA = ca(uri, "Insert @MappedSuperclass", expectedDiagnostic, insertMappedSuperclassTE);

        String removeText = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.NamedNativeQuery;\n\n" +
                "public class NamedNativeQueryOnInvalidClass {\n}\n";
        TextEdit removeTE = te(0, 0, 7, 0, removeText);
        CodeAction removeCA = ca(uri, "Remove @NamedNativeQuery", expectedDiagnostic, removeTE);

        assertJavaCodeAction(codeActionParams, utils, insertEntityCA, insertMappedSuperclassCA, removeCA);
    }

    @Test
    public void testNamedEntityGraphsOnValidEntityClass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/NamedEntityGraphsOnValidEntityClass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void testNamedEntityGraphsOnNonEntityClass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/NamedEntityGraphsOnNonEntityClass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        JsonArray namedEntityGraphsData = new JsonArray();
        namedEntityGraphsData.add("jakarta.persistence.NamedEntityGraphs");
        Diagnostic expectedDiagnostic = d(5, 0, 101,
                "@NamedEntityGraphs must only be applied to a class annotated with @Entity.",
                DiagnosticSeverity.Error, "jakarta-persistence", "NamedEntityGraphsOnNonEntityClass", namedEntityGraphsData);

        assertJavaDiagnostics(diagnosticsParams, utils, expectedDiagnostic);

        // test quick fixes
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, expectedDiagnostic);

        String insertEntityText = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.Entity;\n" +
                "import jakarta.persistence.NamedEntityGraph;\n" +
                "import jakarta.persistence.NamedEntityGraphs;\n\n" +
                "@Entity\n" +
                "@NamedEntityGraphs({@NamedEntityGraph(name = \"Graph.User\"), @NamedEntityGraph(name = \"Graph.Order\")})\n" +
                "public class NamedEntityGraphsOnNonEntityClass {\n}\n";
        TextEdit insertEntityTE = te(0, 0, 8, 0, insertEntityText);
        CodeAction insertEntityCA = ca(uri, "Insert @Entity", expectedDiagnostic, insertEntityTE);

        String removeText = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.NamedEntityGraph;\n" +
                "import jakarta.persistence.NamedEntityGraphs;\n\n" +
                "public class NamedEntityGraphsOnNonEntityClass {\n}\n";
        TextEdit removeTE = te(0, 0, 8, 0, removeText);
        CodeAction removeCA = ca(uri, "Remove @NamedEntityGraphs", expectedDiagnostic, removeTE);

        assertJavaCodeAction(codeActionParams, utils, insertEntityCA, removeCA);
    }

    @Test
    public void testNamedQueriesOnValidClass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/NamedQueriesOnValidClass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void testNamedQueriesOnInvalidClass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/NamedQueriesOnInvalidClass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        JsonArray namedQueriesData = new JsonArray();
        namedQueriesData.add("jakarta.persistence.NamedQueries");
        Diagnostic expectedDiagnostic = d(5, 0, 169,
                "@NamedQueries must only be applied to a class annotated with @Entity or @MappedSuperclass.",
                DiagnosticSeverity.Error, "jakarta-persistence", "NamedQueriesOnInvalidClass", namedQueriesData);

        assertJavaDiagnostics(diagnosticsParams, utils, expectedDiagnostic);

        // test quick fixes
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, expectedDiagnostic);

        String insertEntityText = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.Entity;\n" +
                "import jakarta.persistence.MappedSuperclass;\n" +
                "import jakarta.persistence.NamedQuery;\n" +
                "import jakarta.persistence.NamedQueries;\n\n" +
                "@MappedSuperclass\n" +
                "@Entity\n" +
                "@NamedQueries({@NamedQuery(name = \"User.findAll\", query = \"SELECT u FROM User u\"), @NamedQuery(name = \"User.findById\", query = \"SELECT u FROM User u WHERE u.id = :id\")})\n" +
                "public class NamedQueriesOnInvalidClass {\n}\n";
        TextEdit insertEntityTE = te(0, 0, 8, 0, insertEntityText);
        CodeAction insertEntityCA = ca(uri, "Insert @Entity", expectedDiagnostic, insertEntityTE);

        String insertMappedSuperclassText = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.Entity;\n" +
                "import jakarta.persistence.MappedSuperclass;\n" +
                "import jakarta.persistence.NamedQuery;\n" +
                "import jakarta.persistence.NamedQueries;\n\n" +
                "@MappedSuperclass\n" +
                "@Entity\n" +
                "@NamedQueries({@NamedQuery(name = \"User.findAll\", query = \"SELECT u FROM User u\"), @NamedQuery(name = \"User.findById\", query = \"SELECT u FROM User u WHERE u.id = :id\")})\n" +
                "public class NamedQueriesOnInvalidClass {\n}\n";
        TextEdit insertMappedSuperclassTE = te(0, 0, 8, 0, insertMappedSuperclassText);
        CodeAction insertMappedSuperclassCA = ca(uri, "Insert @MappedSuperclass", expectedDiagnostic, insertMappedSuperclassTE);

        String removeText = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.NamedQuery;\n" +
                "import jakarta.persistence.NamedQueries;\n\n" +
                "public class NamedQueriesOnInvalidClass {\n}\n";
        TextEdit removeTE = te(0, 0, 8, 0, removeText);
        CodeAction removeCA = ca(uri, "Remove @NamedQueries", expectedDiagnostic, removeTE);

        assertJavaCodeAction(codeActionParams, utils, insertEntityCA, insertMappedSuperclassCA, removeCA);
    }

    @Test
    public void testNamedNativeQueriesOnValidClass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/NamedNativeQueriesOnValidClass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void testNamedNativeQueriesOnInvalidClass() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/persistence/NamedNativeQueriesOnInvalidClass.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        JsonArray namedNativeQueriesData = new JsonArray();
        namedNativeQueriesData.add("jakarta.persistence.NamedNativeQueries");
        Diagnostic expectedDiagnostic = d(5, 0, 233,
                "@NamedNativeQueries must only be applied to a class annotated with @Entity or @MappedSuperclass.",
                DiagnosticSeverity.Error, "jakarta-persistence", "NamedNativeQueriesOnInvalidClass", namedNativeQueriesData);

        assertJavaDiagnostics(diagnosticsParams, utils, expectedDiagnostic);

        // test quick fixes
        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, expectedDiagnostic);

        String insertEntityText = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.Entity;\n" +
                "import jakarta.persistence.MappedSuperclass;\n" +
                "import jakarta.persistence.NamedNativeQuery;\n" +
                "import jakarta.persistence.NamedNativeQueries;\n\n" +
                "@MappedSuperclass\n" +
                "@Entity\n" +
                "@NamedNativeQueries({@NamedNativeQuery(name = \"User.findAll\", query = \"SELECT * FROM USER\"), @NamedNativeQuery(name = \"User.findById\", query = \"SELECT * FROM USER WHERE ID = ?\", resultClass = NamedNativeQueriesOnInvalidClass.class)})\n" +
                "public class NamedNativeQueriesOnInvalidClass {\n}\n";
        TextEdit insertEntityTE = te(0, 0, 8, 0, insertEntityText);
        CodeAction insertEntityCA = ca(uri, "Insert @Entity", expectedDiagnostic, insertEntityTE);

        String insertMappedSuperclassText = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.Entity;\n" +
                "import jakarta.persistence.MappedSuperclass;\n" +
                "import jakarta.persistence.NamedNativeQuery;\n" +
                "import jakarta.persistence.NamedNativeQueries;\n\n" +
                "@MappedSuperclass\n" +
                "@Entity\n" +
                "@NamedNativeQueries({@NamedNativeQuery(name = \"User.findAll\", query = \"SELECT * FROM USER\"), @NamedNativeQuery(name = \"User.findById\", query = \"SELECT * FROM USER WHERE ID = ?\", resultClass = NamedNativeQueriesOnInvalidClass.class)})\n" +
                "public class NamedNativeQueriesOnInvalidClass {\n}\n";
        TextEdit insertMappedSuperclassTE = te(0, 0, 8, 0, insertMappedSuperclassText);
        CodeAction insertMappedSuperclassCA = ca(uri, "Insert @MappedSuperclass", expectedDiagnostic, insertMappedSuperclassTE);

        String removeText = "package io.openliberty.sample.jakarta.persistence;\n\n" +
                "import jakarta.persistence.NamedNativeQuery;\n" +
                "import jakarta.persistence.NamedNativeQueries;\n\n" +
                "public class NamedNativeQueriesOnInvalidClass {\n}\n";
        TextEdit removeTE = te(0, 0, 8, 0, removeText);
        CodeAction removeCA = ca(uri, "Remove @NamedNativeQueries", expectedDiagnostic, removeTE);

        assertJavaCodeAction(codeActionParams, utils, insertEntityCA, insertMappedSuperclassCA, removeCA);
    }

}