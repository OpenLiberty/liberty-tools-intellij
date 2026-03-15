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
package io.openliberty.tools.intellij.lsp4jakarta.it.annotations;

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
public class ResourceAnnotationTest extends BaseJakartaTest {

    @Test
    public void ResourceAnnotation() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/annotations/ResourceAnnotation.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // expected annotations
        Diagnostic d1 = d(24, 0, 22, "The @Resource annotation must define the attribute 'type'.",
                DiagnosticSeverity.Error, "jakarta-annotations", "MissingResourceTypeAttribute");

        Diagnostic d2 = d(42, 0, 30, "The @Resource annotation must define the attribute 'name'.",
                DiagnosticSeverity.Error, "jakarta-annotations", "MissingResourceNameAttribute");

        Diagnostic d3 = d(48, 4, 13, "The @Resource method 'setStudentId' must follow the standard JavaBeans convention: must declare exactly one parameter.",
                DiagnosticSeverity.Error, "jakarta-annotations", "MustDeclareExactlyOneParam");

        Diagnostic d4 = d(53, 4, 13, "The @Resource method 'getStudentId' must follow the standard JavaBeans convention: method name must start with set.",
                DiagnosticSeverity.Error, "jakarta-annotations", "NameMustStartWithSet");

        Diagnostic d5 = d(58, 4, 13, "The @Resource method 'setIsHappy' must follow the standard JavaBeans convention: return type must be void.",
                DiagnosticSeverity.Error, "jakarta-annotations", "ReturnTypeMustBeVoid");

        Diagnostic d6 = d(63, 4, 13, "The @Resource method 'setStudentId' must follow the standard JavaBeans convention: must be public.",
                DiagnosticSeverity.Error, "jakarta-annotations", "MethodMustBePublic");

        Diagnostic d7 = d(72, 30, 44, "Priority values should generally be non-negative, with negative values reserved for special meanings such as \"undefined\" or \"not specified\".",
                DiagnosticSeverity.Warning, "jakarta-annotations", "PriorityShouldBeNonNegative");

        Diagnostic d8 = d(25, 0, 13, "Priority values should generally be non-negative, with negative values reserved for special meanings such as \"undefined\" or \"not specified\".",
                DiagnosticSeverity.Warning, "jakarta-annotations", "PriorityShouldBeNonNegative");

        Diagnostic d9 = d(76, 4, 13, "The @Resource method 'setIsHappy1' must follow the standard JavaBeans convention: method must contain property name.",
                DiagnosticSeverity.Error, "jakarta-annotations", "FieldMustExistInSetter");

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3, d4, d5, d6, d7, d8, d9);

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d1);
        String newText = "package io.openliberty.sample.jakarta.annotations;\n\nimport jakarta.annotation.Priority;\n" +
                "import jakarta.annotation.Resource;\n\n@Resource(type = Object.class, name = \"aa\")\n@Priority(0)\n" +
                "public class ResourceAnnotation {\n\n    private Integer studentId;\n\n\n\t" +
                "@Resource(shareable = true)\n    private boolean isHappy;\n\n\t@Resource(name = \"test\")\n    " +
                "private boolean isSad;\n\n\n    private String emailAddress;\n\n\n}\n\n@Resource(name = \"aa\",type=Object.class)\n" +
                "@Priority(-1)\nclass PostDoctoralStudent {\n\n    private Integer studentId;\n\n\n\t" +
                "@Resource(shareable = true)\n    private boolean isHappy;\n\n\t@Resource\n    private boolean isSad;\n\n\n    " +
                "private String emailAddress;\n\n}\n\n@Resource(type = Object.class)\n@Priority(1)\nclass MasterStudent {\n\n    " +
                "private Integer studentId;\n\n    @Resource\n    public void setStudentId() {\n        this.studentId = studentId;\n    }\n\n    " +
                "@Resource\n    public void getStudentId(Integer studentId) {\n        this.studentId = studentId;\n    }\n\n    " +
                "@Resource\n    public boolean setIsHappy(boolean isHappy) {\n        return isHappy;\n    }\n\n    " +
                "@Resource\n    private void setStudentId(Integer studentId) {\n         this.studentId = studentId;\n    }\n\n    " +
                "public Integer setStudentId1(@Priority(20) Integer studentId) {\n        return studentId;\n    }\n\n    " +
                "public void setStudentId3(@Priority(-20) Integer studentId) {\n        this.studentId = studentId;\n    }\n\n    " +
                "@Resource\n    public void setIsHappy1(boolean isHappy) {\n        this.isHappy = isHappy;\n    }\n\n    " +
                "@Resource\n    public void setIsSad(boolean isSad) {\n        this.isSad = isSad;\n    }\n\n    private boolean isSad;\n\n    " +
                "private boolean isHappy;\n} \n";
        TextEdit te = te(0, 0, 90, 0, newText);

        CodeAction ca = ca(uri, "Add type to jakarta.annotation.Resource", d1, te);
        assertJavaCodeAction(codeActionParams, utils, ca);

        JakartaJavaCodeActionParams codeActionParams1 = createCodeActionParams(uri, d2);

        newText = "package io.openliberty.sample.jakarta.annotations;\n\nimport jakarta.annotation.Priority;\n" +
                "import jakarta.annotation.Resource;\n\n@Resource(type = Object.class, name = \"aa\")\n" +
                "@Priority(0)\npublic class ResourceAnnotation {\n\n    private Integer studentId;\n\n\n\t" +
                "@Resource(shareable = true)\n    private boolean isHappy;\n\n\t@Resource(name = \"test\")\n    " +
                "private boolean isSad;\n\n\n    private String emailAddress;\n\n\n}\n\n@Resource(name = \"aa\")\n" +
                "@Priority(-1)\nclass PostDoctoralStudent {\n\n    private Integer studentId;\n\n\n\t@Resource(shareable = true)\n    " +
                "private boolean isHappy;\n\n\t@Resource\n    private boolean isSad;\n\n\n    private String emailAddress;\n\n}\n\n" +
                "@Resource(type = Object.class, name=\"\")\n@Priority(1)\nclass MasterStudent {\n\n    private Integer studentId;\n\n    " +
                "@Resource\n    public void setStudentId() {\n        this.studentId = studentId;\n    }\n\n    " +
                "@Resource\n    public void getStudentId(Integer studentId) {\n        this.studentId = studentId;\n    }\n\n    " +
                "@Resource\n    public boolean setIsHappy(boolean isHappy) {\n        return isHappy;\n    }\n\n    " +
                "@Resource\n    private void setStudentId(Integer studentId) {\n         this.studentId = studentId;\n    }\n\n    " +
                "public Integer setStudentId1(@Priority(20) Integer studentId) {\n        return studentId;\n    }\n\n    " +
                "public void setStudentId3(@Priority(-20) Integer studentId) {\n        this.studentId = studentId;\n    }\n\n    " +
                "@Resource\n    public void setIsHappy1(boolean isHappy) {\n        this.isHappy = isHappy;\n    }\n\n    " +
                "@Resource\n    public void setIsSad(boolean isSad) {\n        this.isSad = isSad;\n    }\n\n    " +
                "private boolean isSad;\n\n    private boolean isHappy;\n} \n";
        TextEdit te1 = te(0, 0, 90, 0, newText);
        CodeAction ca1 = ca(uri, "Add name to jakarta.annotation.Resource", d2, te1);
        assertJavaCodeAction(codeActionParams1, utils, ca1);
    }

    @Test
    public void ResourceAnnotationTypeMismatch() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/annotations/ResourceAnnotationTypeMismatch.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // expected annotations
        Diagnostic d1 = d(8, 1, 51, "Type of the field must be compatible with the type element of the Resource annotation, if specified.",
                DiagnosticSeverity.Error, "jakarta-annotations", "ResourceTypeMismatch");

        Diagnostic d2 = d(17, 1, 33, "Type of the field must be compatible with the type element of the Resource annotation, if specified.",
                DiagnosticSeverity.Error, "jakarta-annotations", "ResourceTypeMismatch");

        Diagnostic d3 = d(23, 1, 33, "Type of the field must be compatible with the type element of the Resource annotation, if specified.",
                DiagnosticSeverity.Error, "jakarta-annotations", "ResourceTypeMismatch");

        Diagnostic d4 = d(26, 1, 32, "Type of the field must be compatible with the type element of the Resource annotation, if specified.",
                DiagnosticSeverity.Error, "jakarta-annotations", "ResourceTypeMismatch");

        Diagnostic d5 = d(44, 1, 31, "Type of the parameter must be compatible with the type element of the Resource annotation, if specified.",
                DiagnosticSeverity.Error, "jakarta-annotations", "ResourceTypeMismatch");

        Diagnostic d6 = d(49, 1, 32, "Type of the parameter must be compatible with the type element of the Resource annotation, if specified.",
                DiagnosticSeverity.Error, "jakarta-annotations", "ResourceTypeMismatch");

        assertJavaDiagnostics(diagnosticsParams, utils, d1, d2, d3, d4, d5, d6);

        JakartaJavaCodeActionParams codeActionParams = createCodeActionParams(uri, d1);
        String newText11 = "package io.openliberty.sample.jakarta.annotations;\n\nimport io.openliberty.sample.jakarta.di.Greeting;\nimport jakarta.annotation.Resource;\n\n@Resource(type = Object.class, name = \"\")\nclass ResourceAnnotationTypeMismatch {\n\n\tprivate Integer studentId;\n\n\t@Resource(type = Integer.class)\n\tprivate Integer mathsStudentId;\n\n\t@Resource(type = Integer.class)\n\tprivate Object itStudentId;\n\n\t@Resource(type = Greeting.class)\n\tprivate Integer bioStudentId;\n\n\t@Resource(name = \"studentId\")\n\tprivate Integer mechStudentId;\n\n\t@Resource(type = Greeting.class)\n\tprivate int englishStudentId;\n\n\t@Resource(type = Boolean.class)\n\tprivate boolean frenchhStudentId;\n\n\t@Resource\n\tpublic void setStudentId(Integer studentId) {\n\t\tthis.studentId = studentId;\n\t}\n\n\t@Resource(type = Integer.class)\n\tpublic void setStudentId1(Integer studentId) {\n\t\tthis.studentId1 = studentId;\n\t}\n\n\t@Resource(type = Integer.class)\n\tpublic void setItStudentId(Object itStudentId) {\n\t\tthis.itStudentId = itStudentId;\n\t}\n\n\t@Resource(type = Object.class)\n\tpublic void setMechStudentId(Integer mechStudentId) {\n\t\tthis.mechStudentId = mechStudentId;\n\t}\n\n\t@Resource(type = Boolean.class)\n\tpublic void setFrenchhStudentId(boolean frenchhStudentId) {\n\t\tthis.frenchhStudentId = frenchhStudentId;\n\t}\n\n\tprivate Integer studentId1;\n}\n";
        TextEdit te11 = te(0, 0, 56, 0, newText11);
        CodeAction ca11 = ca(uri, "Remove @Resource", d1, te11);
        String newText12 = "package io.openliberty.sample.jakarta.annotations;\n\nimport io.openliberty.sample.jakarta.di.Greeting;\nimport jakarta.annotation.Resource;\n\n@Resource(type = Object.class, name = \"\")\nclass ResourceAnnotationTypeMismatch {\n\n\t@Resource( name = \"studentId\")\n\tprivate Integer studentId;\n\n\t@Resource(type = Integer.class)\n\tprivate Integer mathsStudentId;\n\n\t@Resource(type = Integer.class)\n\tprivate Object itStudentId;\n\n\t@Resource(type = Greeting.class)\n\tprivate Integer bioStudentId;\n\n\t@Resource(name = \"studentId\")\n\tprivate Integer mechStudentId;\n\n\t@Resource(type = Greeting.class)\n\tprivate int englishStudentId;\n\n\t@Resource(type = Boolean.class)\n\tprivate boolean frenchhStudentId;\n\n\t@Resource\n\tpublic void setStudentId(Integer studentId) {\n\t\tthis.studentId = studentId;\n\t}\n\n\t@Resource(type = Integer.class)\n\tpublic void setStudentId1(Integer studentId) {\n\t\tthis.studentId1 = studentId;\n\t}\n\n\t@Resource(type = Integer.class)\n\tpublic void setItStudentId(Object itStudentId) {\n\t\tthis.itStudentId = itStudentId;\n\t}\n\n\t@Resource(type = Object.class)\n\tpublic void setMechStudentId(Integer mechStudentId) {\n\t\tthis.mechStudentId = mechStudentId;\n\t}\n\n\t@Resource(type = Boolean.class)\n\tpublic void setFrenchhStudentId(boolean frenchhStudentId) {\n\t\tthis.frenchhStudentId = frenchhStudentId;\n\t}\n\n\tprivate Integer studentId1;\n}\n";
        TextEdit te12 = te(0, 0, 56, 0, newText12);
        CodeAction ca12 = ca(uri, "Remove type attribute from @Resource", d1, te12);
        assertJavaCodeAction(codeActionParams, utils, ca11, ca12);

        JakartaJavaCodeActionParams codeActionParams2 = createCodeActionParams(uri, d6);
        String newText21 = "package io.openliberty.sample.jakarta.annotations;\n\nimport io.openliberty.sample.jakarta.di.Greeting;\nimport jakarta.annotation.Resource;\n\n@Resource(type = Object.class, name = \"\")\nclass ResourceAnnotationTypeMismatch {\n\n\t@Resource(type = Object.class, name = \"studentId\")\n\tprivate Integer studentId;\n\n\t@Resource(type = Integer.class)\n\tprivate Integer mathsStudentId;\n\n\t@Resource(type = Integer.class)\n\tprivate Object itStudentId;\n\n\t@Resource(type = Greeting.class)\n\tprivate Integer bioStudentId;\n\n\t@Resource(name = \"studentId\")\n\tprivate Integer mechStudentId;\n\n\t@Resource(type = Greeting.class)\n\tprivate int englishStudentId;\n\n\t@Resource(type = Boolean.class)\n\tprivate boolean frenchhStudentId;\n\n\t@Resource\n\tpublic void setStudentId(Integer studentId) {\n\t\tthis.studentId = studentId;\n\t}\n\n\t@Resource(type = Integer.class)\n\tpublic void setStudentId1(Integer studentId) {\n\t\tthis.studentId1 = studentId;\n\t}\n\n\t@Resource(type = Integer.class)\n\tpublic void setItStudentId(Object itStudentId) {\n\t\tthis.itStudentId = itStudentId;\n\t}\n\n\t@Resource(type = Object.class)\n\tpublic void setMechStudentId(Integer mechStudentId) {\n\t\tthis.mechStudentId = mechStudentId;\n\t}\n\n\tpublic void setFrenchhStudentId(boolean frenchhStudentId) {\n\t\tthis.frenchhStudentId = frenchhStudentId;\n\t}\n\n\tprivate Integer studentId1;\n}\n";
        TextEdit te21 = te(0, 0, 56, 0, newText21);
        CodeAction ca21 = ca(uri, "Remove @Resource", d6, te21);
        String newText22 = "package io.openliberty.sample.jakarta.annotations;\n\nimport io.openliberty.sample.jakarta.di.Greeting;\nimport jakarta.annotation.Resource;\n\n@Resource(type = Object.class, name = \"\")\nclass ResourceAnnotationTypeMismatch {\n\n\t@Resource(type = Object.class, name = \"studentId\")\n\tprivate Integer studentId;\n\n\t@Resource(type = Integer.class)\n\tprivate Integer mathsStudentId;\n\n\t@Resource(type = Integer.class)\n\tprivate Object itStudentId;\n\n\t@Resource(type = Greeting.class)\n\tprivate Integer bioStudentId;\n\n\t@Resource(name = \"studentId\")\n\tprivate Integer mechStudentId;\n\n\t@Resource(type = Greeting.class)\n\tprivate int englishStudentId;\n\n\t@Resource(type = Boolean.class)\n\tprivate boolean frenchhStudentId;\n\n\t@Resource\n\tpublic void setStudentId(Integer studentId) {\n\t\tthis.studentId = studentId;\n\t}\n\n\t@Resource(type = Integer.class)\n\tpublic void setStudentId1(Integer studentId) {\n\t\tthis.studentId1 = studentId;\n\t}\n\n\t@Resource(type = Integer.class)\n\tpublic void setItStudentId(Object itStudentId) {\n\t\tthis.itStudentId = itStudentId;\n\t}\n\n\t@Resource(type = Object.class)\n\tpublic void setMechStudentId(Integer mechStudentId) {\n\t\tthis.mechStudentId = mechStudentId;\n\t}\n\n\t@Resource()\n\tpublic void setFrenchhStudentId(boolean frenchhStudentId) {\n\t\tthis.frenchhStudentId = frenchhStudentId;\n\t}\n\n\tprivate Integer studentId1;\n}\n";
        TextEdit te22 = te(0, 0, 56, 0, newText22);
        CodeAction ca22 = ca(uri, "Remove type attribute from @Resource", d6, te22);
        assertJavaCodeAction(codeActionParams2, utils, ca21, ca22);
    }
}
