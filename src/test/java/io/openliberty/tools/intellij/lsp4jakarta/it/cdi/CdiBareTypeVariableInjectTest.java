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

package io.openliberty.tools.intellij.lsp4jakarta.it.cdi;

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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Collections;

import static io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert.*;

/**
 * Tests for CDI §2.2.1 legal bean type diagnostics — bare type variable at injection points.
 *
 * According to CDI specification section 2.2.1:
 * "A type variable is not a legal bean type."
 *
 * Tests cover:
 * - @Inject fields whose type is a bare type variable (T or T[])
 * - @Inject method parameters whose type is a bare type variable (T or T[])
 * - Multi-parameter @Inject methods with bare type variables
 * - Non-generic classes with method-level type variables (<U>)
 * - Non-generic classes with valid injection points (no diagnostics)
 * - Quickfix: Remove @Inject annotation from offending elements
 */
@RunWith(JUnit4.class)
public class CdiBareTypeVariableInjectTest extends BaseJakartaTest {

    // Shared header block used in all expected-file strings for BareTypeVariableInjectBean.
    private static final String FILE_HEADER =
            """
                    package io.openliberty.sample.jakarta.cdi;
                    
                    import java.util.List;
                    
                    import jakarta.enterprise.context.Dependent;
                    import jakarta.inject.Inject;
                    
                    @Dependent
                    public class BareTypeVariableInjectBean<T> {
                    
                    """;

    private static final String INVALID_BARE_FIELD =
            """
                        // Invalid: bare type variable T as @Inject field type
                        @Inject
                        T bareTypeField;
                    
                    """;

    private static final String INVALID_BARE_ARRAY_FIELD =
            """
                        // Invalid: array of bare type variable T[] as @Inject field type
                        @Inject
                        T[] bareTypeArrayField;
                    
                    """;

    private static final String INVALID_BARE_METHOD =
            """
                        // Invalid: bare type variable T as @Inject method parameter
                        @Inject
                        public void setBareType(T value) {
                        }
                    
                    """;

    private static final String INVALID_BARE_ARRAY_METHOD =
            """
                        // Invalid: array of bare type variable T[] as @Inject method parameter
                        @Inject
                        public void setBareTypeArray(T[] values) {
                        }
                    
                    """;

    private static final String INVALID_MULTI_PARAM_METHOD =
            """
                        // Invalid: multi-parameter method with bare type variables
                        @Inject
                        public void setMultipleParams(String validParam, T bareParam, int count, T[] bareArrayParam) {
                        }
                    
                    """;

    private static final String VALID_TAIL =
            """
                        // Valid: parameterized type with type variable (List<T>)
                        @Inject
                        List<T> genericList;
                    
                        // Valid: concrete type \u2014 no diagnostic
                        @Inject
                        String concreteField;
                    }
                    """;

    // Bare field without @Inject (annotation removed by quickfix)
    private static final String BARE_FIELD_NO_INJECT =
            """
                        // Invalid: bare type variable T as @Inject field type
                        T bareTypeField;
                    
                    """;

    // Bare array field without @Inject (annotation removed by quickfix)
    private static final String BARE_ARRAY_FIELD_NO_INJECT =
            """
                        // Invalid: array of bare type variable T[] as @Inject field type
                        T[] bareTypeArrayField;
                    
                    """;

    // setBareType method without @Inject (annotation removed by quickfix)
    private static final String BARE_METHOD_NO_INJECT =
            """
                        // Invalid: bare type variable T as @Inject method parameter
                        public void setBareType(T value) {
                        }
                    
                    """;

    // setBareTypeArray method without @Inject (annotation removed by quickfix)
    private static final String BARE_ARRAY_METHOD_NO_INJECT =
            """
                        // Invalid: array of bare type variable T[] as @Inject method parameter
                        public void setBareTypeArray(T[] values) {
                        }
                    
                    """;

    // setMultipleParams method without @Inject (annotation removed by quickfix)
    private static final String MULTI_PARAM_METHOD_NO_INJECT =
            """
                        // Invalid: multi-parameter method with bare type variables
                        public void setMultipleParams(String validParam, T bareParam, int count, T[] bareArrayParam) {
                        }
                    
                    """;

    private static final String NON_GENERIC_HEADER =
            """
                    package io.openliberty.sample.jakarta.cdi;
                    
                    import jakarta.enterprise.context.Dependent;
                    import jakarta.inject.Inject;
                    
                    @Dependent
                    public class NonGenericBareTypeInjectBean {
                    
                    """;

    private static final String INVALID_METHOD_LEVEL_BARE =
            """
                        // Invalid: method-level bare type variable U as @Inject method parameter
                        @Inject
                        public <U> void setMethodLevelBareType(U value) {
                        }
                    
                    """;

    private static final String INVALID_METHOD_LEVEL_BARE_ARRAY =
            """
                        // Invalid: array of method-level bare type variable U[] as @Inject method parameter
                        @Inject
                        public <U> void setMethodLevelBareTypeArray(U[] values) {
                        }
                    
                    """;

    private static final String NON_GENERIC_VALID_TAIL =
            """
                        // Valid: concrete type \u2014 no diagnostic
                        @Inject
                        public void setConcrete(String value) {
                        }
                    }
                    """;

    private static final String METHOD_LEVEL_BARE_NO_INJECT =
            """
                        // Invalid: method-level bare type variable U as @Inject method parameter
                        public <U> void setMethodLevelBareType(U value) {
                        }
                    
                    """;

    private static final String METHOD_LEVEL_BARE_ARRAY_NO_INJECT =
            """
                        // Invalid: array of method-level bare type variable U[] as @Inject method parameter
                        public <U> void setMethodLevelBareTypeArray(U[] values) {
                        }
                    
                    """;

    @Test
    public void bareTypeVariableAtInjectionPoints() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/BareTypeVariableInjectBean.java");
        Assert.assertNotNull(javaFile);
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Collections.singletonList(uri));

        // Line 13 (0-based 12): T bareTypeField — field name "bareTypeField" at col 6..19
        Diagnostic injectBareTypeField = d(12, 6, 19,
                "A type variable is not a legal bean type. Injection point fields must not use a bare type variable (T or T[]).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidBareTypeVariableInInjectField");

        // Line 17 (0-based 16): T[] bareTypeArrayField — field name "bareTypeArrayField" at col 8..26
        Diagnostic injectBareTypeArrayField = d(16, 8, 26,
                "A type variable is not a legal bean type. Injection point fields must not use a bare type variable (T or T[]).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidBareTypeVariableInInjectField");

        // Line 21 (0-based 20): setBareType — method name at col 16..27 (diagnostic on method)
        Diagnostic injectBareTypeMethodParam = d(20, 16, 27,
                "A type variable is not a legal bean type. Parameter 'value' must not use a bare type variable (T or T[]).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidBareTypeVariableInInjectMethodParam");

        // Line 26 (0-based 25): setBareTypeArray — method name at col 16..32 (diagnostic on method)
        Diagnostic injectBareTypeArrayMethodParam = d(25, 16, 32,
                "A type variable is not a legal bean type. Parameter 'values' must not use a bare type variable (T or T[]).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidBareTypeVariableInInjectMethodParam");

        // Line 31 (0-based 30): setMultipleParams — method name at col 16..33 (bareParam)
        Diagnostic injectBareTypeMultiParam = d(30, 16, 33,
                "A type variable is not a legal bean type. Parameter 'bareParam' must not use a bare type variable (T or T[]).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidBareTypeVariableInInjectMethodParam");

        // Line 31 (0-based 30): setMultipleParams — method name at col 16..33 (bareArrayParam)
        Diagnostic injectBareTypeMultiArrayParam = d(30, 16, 33,
                "A type variable is not a legal bean type. Parameter 'bareArrayParam' must not use a bare type variable (T or T[]).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidBareTypeVariableInInjectMethodParam");

        assertJavaDiagnostics(diagnosticsParams, utils,
                injectBareTypeField,
                injectBareTypeArrayField,
                injectBareTypeMethodParam,
                injectBareTypeArrayMethodParam,
                injectBareTypeMultiParam,
                injectBareTypeMultiArrayParam);

        // --- QuickFix: Remove @Inject from T bareTypeField ---
        String afterRemoveBareTypeFieldInject = FILE_HEADER
                + BARE_FIELD_NO_INJECT
                + INVALID_BARE_ARRAY_FIELD
                + INVALID_BARE_METHOD
                + INVALID_BARE_ARRAY_METHOD
                + INVALID_MULTI_PARAM_METHOD
                + VALID_TAIL;
        JakartaJavaCodeActionParams codeActionBareTypeField = createCodeActionParams(uri, injectBareTypeField);
        TextEdit removeBareTypeFieldInject = te(0, 0, 41, 0, afterRemoveBareTypeFieldInject);
        CodeAction removeBareTypeFieldInjectAction = ca(uri, "Remove @Inject", injectBareTypeField,
                removeBareTypeFieldInject);
        assertJavaCodeAction(codeActionBareTypeField, utils, removeBareTypeFieldInjectAction);

        // --- QuickFix: Remove @Inject from T[] bareTypeArrayField ---
        String afterRemoveBareTypeArrayFieldInject = FILE_HEADER
                + INVALID_BARE_FIELD
                + BARE_ARRAY_FIELD_NO_INJECT
                + INVALID_BARE_METHOD
                + INVALID_BARE_ARRAY_METHOD
                + INVALID_MULTI_PARAM_METHOD
                + VALID_TAIL;
        JakartaJavaCodeActionParams codeActionBareTypeArrayField = createCodeActionParams(uri, injectBareTypeArrayField);
        TextEdit removeBareTypeArrayFieldInject = te(0, 0, 41, 0, afterRemoveBareTypeArrayFieldInject);
        CodeAction removeBareTypeArrayFieldInjectAction = ca(uri, "Remove @Inject", injectBareTypeArrayField,
                removeBareTypeArrayFieldInject);
        assertJavaCodeAction(codeActionBareTypeArrayField, utils, removeBareTypeArrayFieldInjectAction);

        // --- QuickFix: Remove @Inject from setBareType method ---
        String afterRemoveBareTypeMethodInject = FILE_HEADER
                + INVALID_BARE_FIELD
                + INVALID_BARE_ARRAY_FIELD
                + BARE_METHOD_NO_INJECT
                + INVALID_BARE_ARRAY_METHOD
                + INVALID_MULTI_PARAM_METHOD
                + VALID_TAIL;
        JakartaJavaCodeActionParams codeActionBareTypeMethodParam = createCodeActionParams(uri, injectBareTypeMethodParam);
        TextEdit removeBareTypeMethodInject = te(0, 0, 41, 0, afterRemoveBareTypeMethodInject);
        CodeAction removeBareTypeMethodInjectAction = ca(uri, "Remove @Inject", injectBareTypeMethodParam,
                removeBareTypeMethodInject);
        assertJavaCodeAction(codeActionBareTypeMethodParam, utils, removeBareTypeMethodInjectAction);

        // --- QuickFix: Remove @Inject from setBareTypeArray method ---
        String afterRemoveBareTypeArrayMethodInject = FILE_HEADER
                + INVALID_BARE_FIELD
                + INVALID_BARE_ARRAY_FIELD
                + INVALID_BARE_METHOD
                + BARE_ARRAY_METHOD_NO_INJECT
                + INVALID_MULTI_PARAM_METHOD
                + VALID_TAIL;
        JakartaJavaCodeActionParams codeActionBareTypeArrayMethodParam = createCodeActionParams(uri,
                injectBareTypeArrayMethodParam);
        TextEdit removeBareTypeArrayMethodInject = te(0, 0, 41, 0, afterRemoveBareTypeArrayMethodInject);
        CodeAction removeBareTypeArrayMethodInjectAction = ca(uri, "Remove @Inject", injectBareTypeArrayMethodParam,
                removeBareTypeArrayMethodInject);
        assertJavaCodeAction(codeActionBareTypeArrayMethodParam, utils, removeBareTypeArrayMethodInjectAction);

        // --- QuickFix: Remove @Inject from setMultipleParams method ---
        String afterRemoveMultiParamMethodInject = FILE_HEADER
                + INVALID_BARE_FIELD
                + INVALID_BARE_ARRAY_FIELD
                + INVALID_BARE_METHOD
                + INVALID_BARE_ARRAY_METHOD
                + MULTI_PARAM_METHOD_NO_INJECT
                + VALID_TAIL;
        JakartaJavaCodeActionParams codeActionMultiParam = createCodeActionParams(uri, injectBareTypeMultiParam);
        TextEdit removeMultiParamMethodInject = te(0, 0, 41, 0, afterRemoveMultiParamMethodInject);
        CodeAction removeMultiParamMethodInjectAction = ca(uri, "Remove @Inject", injectBareTypeMultiParam,
                removeMultiParamMethodInject);
        assertJavaCodeAction(codeActionMultiParam, utils, removeMultiParamMethodInjectAction);
    }

    @Test
    public void validNonGenericInjectionPoints() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/ValidBareTypeVariableInjectBean.java");
        Assert.assertNotNull(javaFile);
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Collections.singletonList(uri));

        // No diagnostics expected: all @Inject fields and method parameters use concrete types.
        assertJavaDiagnostics(diagnosticsParams, utils);
    }

    @Test
    public void bareTypeVariableAtNonGenericInjectMethod() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/NonGenericBareTypeInjectBean.java");
        Assert.assertNotNull(javaFile);
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Collections.singletonList(uri));

        // Line 11 (0-based 10): setMethodLevelBareType — method name at col 20..42
        Diagnostic injectMethodLevelBareType = d(10, 20, 42,
                "A type variable is not a legal bean type. Parameter 'value' must not use a bare type variable (T or T[]).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidBareTypeVariableInInjectMethodParam");
        Diagnostic injectMethodLevelGeneric = d(10, 20, 42,
                "The @Inject annotation must not define a generic method.",
                DiagnosticSeverity.Error, "jakarta-di", "RemoveInjectForGeneric");
        injectMethodLevelGeneric.setData("void");

        // Line 16 (0-based 15): setMethodLevelBareTypeArray — method name at col 20..47
        Diagnostic injectMethodLevelBareTypeArray = d(15, 20, 47,
                "A type variable is not a legal bean type. Parameter 'values' must not use a bare type variable (T or T[]).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidBareTypeVariableInInjectMethodParam");
        Diagnostic injectMethodLevelArrayGeneric = d(15, 20, 47,
                "The @Inject annotation must not define a generic method.",
                DiagnosticSeverity.Error, "jakarta-di", "RemoveInjectForGeneric");
        injectMethodLevelArrayGeneric.setData("void");

        assertJavaDiagnostics(diagnosticsParams, utils,
                injectMethodLevelBareType,
                injectMethodLevelGeneric,
                injectMethodLevelBareTypeArray,
                injectMethodLevelArrayGeneric);

        // --- QuickFix: Remove @Inject from setMethodLevelBareType method ---
        String afterRemoveMethodLevelBareType = NON_GENERIC_HEADER
                + METHOD_LEVEL_BARE_NO_INJECT
                + INVALID_METHOD_LEVEL_BARE_ARRAY
                + NON_GENERIC_VALID_TAIL;
        JakartaJavaCodeActionParams codeActionMethodLevelBare = createCodeActionParams(uri, injectMethodLevelBareType);
        TextEdit removeMethodLevelBareEdit = te(0, 0, 23, 0, afterRemoveMethodLevelBareType);
        CodeAction removeMethodLevelBareAction = ca(uri, "Remove @Inject", injectMethodLevelBareType,
                removeMethodLevelBareEdit);
        assertJavaCodeAction(codeActionMethodLevelBare, utils, removeMethodLevelBareAction);

        // --- QuickFix: Remove @Inject from setMethodLevelBareTypeArray method ---
        String afterRemoveMethodLevelBareTypeArray = NON_GENERIC_HEADER
                + INVALID_METHOD_LEVEL_BARE
                + METHOD_LEVEL_BARE_ARRAY_NO_INJECT
                + NON_GENERIC_VALID_TAIL;
        JakartaJavaCodeActionParams codeActionMethodLevelBareArray = createCodeActionParams(uri, injectMethodLevelBareTypeArray);
        TextEdit removeMethodLevelBareArrayEdit = te(0, 0, 23, 0, afterRemoveMethodLevelBareTypeArray);
        CodeAction removeMethodLevelBareArrayAction = ca(uri, "Remove @Inject", injectMethodLevelBareTypeArray,
                removeMethodLevelBareArrayEdit);
        assertJavaCodeAction(codeActionMethodLevelBareArray, utils, removeMethodLevelBareArrayAction);
    }
}
