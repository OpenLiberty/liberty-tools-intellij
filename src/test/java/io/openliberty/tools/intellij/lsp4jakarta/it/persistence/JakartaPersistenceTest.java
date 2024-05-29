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
        Diagnostic d1 = d(12, 25, 30,
                "A field with multiple @MapKeyJoinColumn annotations must specify both the name and referencedColumnName attributes in the corresponding @MapKeyJoinColumn annotations.",
                DiagnosticSeverity.Error, "jakarta-persistence", "SupplyAttributesToAnnotations");
        Diagnostic d2 = d(12, 25, 30,
                "A field with multiple @MapKeyJoinColumn annotations must specify both the name and referencedColumnName attributes in the corresponding @MapKeyJoinColumn annotations.",
                DiagnosticSeverity.Error, "jakarta-persistence", "SupplyAttributesToAnnotations");

        Diagnostic d3 = d(16, 25, 30,
                "A field with multiple @MapKeyJoinColumn annotations must specify both the name and referencedColumnName attributes in the corresponding @MapKeyJoinColumn annotations.",
                DiagnosticSeverity.Error, "jakarta-persistence", "SupplyAttributesToAnnotations");
        Diagnostic d4 = d(16, 25, 30,
                "A field with multiple @MapKeyJoinColumn annotations must specify both the name and referencedColumnName attributes in the corresponding @MapKeyJoinColumn annotations.",
                DiagnosticSeverity.Error, "jakarta-persistence", "SupplyAttributesToAnnotations");

        Diagnostic d5 = d(20, 25, 30,
                "A field with multiple @MapKeyJoinColumn annotations must specify both the name and referencedColumnName attributes in the corresponding @MapKeyJoinColumn annotations.",
                DiagnosticSeverity.Error, "jakarta-persistence", "SupplyAttributesToAnnotations");

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3, d4, d5);
        String newText = "package io.openliberty.sample.jakarta.persistence;\n\nimport java.util.Map;\n\n" +
                "import jakarta.persistence.Entity;\nimport jakarta.persistence.Id;\nimport jakarta.persistence.MapKeyJoinColumn;\n\n" +
                "@Entity\npublic class MultipleMapKeyAnnotations {\n    @MapKeyJoinColumn(name=\"\",referencedColumnName=\"\")\n    " +
                "@MapKeyJoinColumn(name=\"\",referencedColumnName=\"\")\n    Map<Integer, String> test1;\n    \n    " +
                "@MapKeyJoinColumn(name = \"n1\")\n    @MapKeyJoinColumn(referencedColumnName = \"rcn2\")\n    " +
                "Map<Integer, String> test2;\n    \n    @MapKeyJoinColumn(name = \"n1\", referencedColumnName = \"rcn1\")\n    " +
                "@MapKeyJoinColumn()\n    Map<Integer, String> test3;\n}";
        String newText1 = "package io.openliberty.sample.jakarta.persistence;\n\nimport java.util.Map;\n\n" +
                "import jakarta.persistence.Entity;\nimport jakarta.persistence.Id;\nimport jakarta.persistence.MapKeyJoinColumn;\n\n" +
                "@Entity\npublic class MultipleMapKeyAnnotations {\n    @MapKeyJoinColumn()\n    @MapKeyJoinColumn()\n    " +
                "Map<Integer, String> test1;\n    \n    @MapKeyJoinColumn(name = \"n1\",referencedColumnName=\"\")\n    " +
                "@MapKeyJoinColumn(referencedColumnName = \"rcn2\",name=\"\")\n    Map<Integer, String> test2;\n    \n    " +
                "@MapKeyJoinColumn(name = \"n1\", referencedColumnName = \"rcn1\")\n    @MapKeyJoinColumn()\n    " +
                "Map<Integer, String> test3;\n}";
        String newText2 = "package io.openliberty.sample.jakarta.persistence;\n\nimport java.util.Map;\n\n" +
                "import jakarta.persistence.Entity;\nimport jakarta.persistence.Id;\nimport jakarta.persistence.MapKeyJoinColumn;\n\n" +
                "@Entity\npublic class MultipleMapKeyAnnotations {\n    @MapKeyJoinColumn()\n    @MapKeyJoinColumn()\n    " +
                "Map<Integer, String> test1;\n    \n    @MapKeyJoinColumn(name = \"n1\")\n    @MapKeyJoinColumn(referencedColumnName = \"rcn2\")\n    " +
                "Map<Integer, String> test2;\n    \n    @MapKeyJoinColumn(name = \"n1\", referencedColumnName = \"rcn1\")\n    " +
                "@MapKeyJoinColumn(name=\"\",referencedColumnName=\"\")\n    Map<Integer, String> test3;\n}";

        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d1);
        TextEdit te1 = te(0, 0, 21, 1, newText);
        CodeAction ca1 = ca(uri, "Add the missing attributes to the @MapKeyJoinColumn annotation", d1, te1);

        assertJavaCodeAction(codeActionParams1, utils, ca1);

        JakartaJavaCodeActionParams codeActionParams2 = createCodeActionParams(uri, d3);
        TextEdit te2 = te(0, 0, 21, 1, newText1);
        CodeAction ca2 = ca(uri, "Add the missing attributes to the @MapKeyJoinColumn annotation", d3, te2);

        assertJavaCodeAction(codeActionParams2, utils, ca2);

        JakartaJavaCodeActionParams codeActionParams3 = createCodeActionParams(uri, d5);
        TextEdit te3 = te(0, 0, 21, 1, newText2);
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
        Diagnostic d = d(5, 13, 37,
                "A class using the @Entity annotation must contain a public or protected constructor with no arguments.",
                DiagnosticSeverity.Error, "jakarta-persistence", "MissingEmptyConstructor");

        assertJavaDiagnostics(diagnosticsParams, utils, d);

        // test quick fixes
        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d);
        TextEdit te1 = te(0, 0, 9, 1, "package io.openliberty.sample.jakarta.persistence;\n\nimport jakarta.persistence.Entity;\n\n@Entity\npublic class EntityMissingConstructor {\n\n    protected EntityMissingConstructor() {\n    }\n\n    private EntityMissingConstructor(int x) {\n    }\n\n}");
        CodeAction ca1 = ca(uri, Messages.getMessage("AddNoArgProtectedConstructor"), d, te1);
        TextEdit te2 = te(0, 0, 9, 1, "package io.openliberty.sample.jakarta.persistence;\n\nimport jakarta.persistence.Entity;\n\n@Entity\npublic class EntityMissingConstructor {\n\n    public EntityMissingConstructor() {\n    }\n\n    private EntityMissingConstructor(int x) {\n    }\n\n}");
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
        Diagnostic d1 = d(10, 21, 28,
                "A class using the @Entity annotation cannot contain any methods that are declared final.",
                DiagnosticSeverity.Error, "jakarta-persistence", "RemoveFinalMethods");
        d1.setData("int");

        Diagnostic d2 = d(7, 14, 15,
                "A class using the @Entity annotation cannot contain any persistent instance variables that are declared final.",
                DiagnosticSeverity.Error, "jakarta-persistence", "RemoveFinalVariables");
        d2.setData("int");

        Diagnostic d3 = d(8, 17, 18,
                "A class using the @Entity annotation cannot contain any persistent instance variables that are declared final.",
                DiagnosticSeverity.Error, "jakarta-persistence", "RemoveFinalVariables");
        d3.setData("java.lang.String");

        Diagnostic d4 = d(8, 30, 31,
                "A class using the @Entity annotation cannot contain any persistent instance variables that are declared final.",
                DiagnosticSeverity.Error, "jakarta-persistence", "RemoveFinalVariables");
        d4.setData("java.lang.String");

        Diagnostic d5 = d(5, 19, 33,
                "A class using the @Entity annotation must not be final.",
                DiagnosticSeverity.Error, "jakarta-persistence", "InvalidClass");
        d5.setData("io.openliberty.sample.jakarta.persistence.FinalModifiers");

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3, d4, d5);

        // test quick fixes
        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d1);
        String newText1 = "package io.openliberty.sample.jakarta.persistence;\n\nimport jakarta.persistence.Entity;\n\n@Entity\n" +
                "public final class FinalModifiers {\n\n    " +
                "final int x = 1;\n    " +
                "final String y = \"hello\", z = \"world\";\n    \n    " +
                "public int methody() {\n        final int ret = 100;\n        return 100 + ret;\n    }\n}";
        TextEdit te1 = te(0, 0, 14, 1, newText1);
        CodeAction ca1 = ca(uri, "Remove the 'final' modifier from this method", d1, te1);

        assertJavaCodeAction(codeActionParams1, utils, ca1);

        JakartaJavaCodeActionParams codeActionParams2 = createCodeActionParams(uri, d2);
        String newText2 = "package io.openliberty.sample.jakarta.persistence;\n\nimport jakarta.persistence.Entity;\n\n@Entity\n" +
                "public final class FinalModifiers {\n\n    " +
                "int x = 1;\n    " +
                "final String y = \"hello\", z = \"world\";\n    \n    " +
                "public final int methody() {\n        final int ret = 100;\n        return 100 + ret;\n    }\n}";
        TextEdit te2 = te(0, 0, 14, 1, newText2);
        CodeAction ca2 = ca(uri, "Remove the 'final' modifier from this field", d2, te2);

        assertJavaCodeAction(codeActionParams2, utils, ca2);

        JakartaJavaCodeActionParams codeActionParams3 = createCodeActionParams(uri, d3);
        String newText3 = "package io.openliberty.sample.jakarta.persistence;\n\nimport jakarta.persistence.Entity;\n\n@Entity\n" +
                "public final class FinalModifiers {\n\n    " +
                "final int x = 1;\n    " +
                "String y = \"hello\", z = \"world\";\n    \n    " +
                "public final int methody() {\n        final int ret = 100;\n        return 100 + ret;\n    }\n}";
        TextEdit te3 = te(0, 0, 14, 1, newText3);
        CodeAction ca3 = ca(uri, "Remove the 'final' modifier from this field", d3, te3);

        assertJavaCodeAction(codeActionParams3, utils, ca3);

        JakartaJavaCodeActionParams codeActionParams4 = createCodeActionParams(uri, d4);
        String newText4 = "package io.openliberty.sample.jakarta.persistence;\n\nimport jakarta.persistence.Entity;\n\n@Entity\n" +
                "public final class FinalModifiers {\n\n    " +
                "final int x = 1;\n    " +
                "String y = \"hello\", z = \"world\";\n    \n    " +
                "public final int methody() {\n        final int ret = 100;\n        return 100 + ret;\n    }\n}";
        TextEdit te4 = te(0, 0, 14, 1, newText4);
        CodeAction ca4 = ca(uri, "Remove the 'final' modifier from this field", d4, te4);

        assertJavaCodeAction(codeActionParams4, utils, ca4);

        JakartaJavaCodeActionParams codeActionParams5 = createCodeActionParams(uri, d5);
        String newText5 = "package io.openliberty.sample.jakarta.persistence;\n\nimport jakarta.persistence.Entity;\n\n@Entity\n" +
                "public class FinalModifiers {\n\n    " +
                "final int x = 1;\n    " +
                "final String y = \"hello\", z = \"world\";\n    \n    " +
                "public final int methody() {\n        final int ret = 100;\n        return 100 + ret;\n    }\n}";
        TextEdit te5 = te(0, 0, 14, 1, newText5);
        CodeAction ca5 = ca(uri, "Remove the 'final' modifier from this class", d5, te5);

        assertJavaCodeAction(codeActionParams5, utils, ca5);
    }
}