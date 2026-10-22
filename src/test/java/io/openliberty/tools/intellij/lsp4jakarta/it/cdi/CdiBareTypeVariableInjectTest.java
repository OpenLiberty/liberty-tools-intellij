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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;

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
 * - Quickfix: Remove @Inject annotation from the offending element
 */
@RunWith(JUnit4.class)
public class CdiBareTypeVariableInjectTest extends BaseJakartaTest {

    // Shared header block used in all expected-file strings for this test resource.
    private static final String FILE_HEADER =
            "/*******************************************************************************\n"
            + " * Copyright (c) 2026 IBM Corporation and others.\n"
            + " *\n"
            + " * This program and the accompanying materials are made available under the\n"
            + " * terms of the Eclipse Public License v. 2.0 which is available at\n"
            + " * http://www.eclipse.org/legal/epl-2.0.\n"
            + " *\n"
            + " * SPDX-License-Identifier: EPL-2.0\n"
            + " *\n"
            + " * Contributors:\n"
            + " *     IBM Corporation - initial implementation\n"
            + " *******************************************************************************/\n"
            + "package io.openliberty.sample.jakarta.cdi;\n"
            + "\n"
            + "import java.util.List;\n"
            + "\n"
            + "import jakarta.enterprise.context.Dependent;\n"
            + "import jakarta.inject.Inject;\n"
            + "\n"
            + "@Dependent\n"
            + "public class BareTypeVariableInjectBean<T> {\n"
            + "\n";

    private static final String INVALID_BARE_FIELD =
            "    // Invalid: bare type variable T as @Inject field type\n"
            + "    @Inject\n"
            + "    T bareTypeField;\n"
            + "\n";

    private static final String INVALID_BARE_ARRAY_FIELD =
            "    // Invalid: array of bare type variable T[] as @Inject field type\n"
            + "    @Inject\n"
            + "    T[] bareTypeArrayField;\n"
            + "\n";

    private static final String INVALID_BARE_METHOD =
            "    // Invalid: bare type variable T as @Inject method parameter\n"
            + "    @Inject\n"
            + "    public void setBareType(T value) {\n"
            + "    }\n"
            + "\n";

    private static final String INVALID_BARE_ARRAY_METHOD =
            "    // Invalid: array of bare type variable T[] as @Inject method parameter\n"
            + "    @Inject\n"
            + "    public void setBareTypeArray(T[] values) {\n"
            + "    }\n"
            + "\n";

    private static final String VALID_TAIL =
            "    // Valid: parameterized type with type variable (List<T>)\n"
            + "    @Inject\n"
            + "    List<T> genericList;\n"
            + "\n"
            + "    // Valid: concrete type \u2014 no diagnostic\n"
            + "    @Inject\n"
            + "    String concreteField;\n"
            + "}\n";

    // Bare field without @Inject (annotation removed by quickfix)
    private static final String BARE_FIELD_NO_INJECT =
            "    // Invalid: bare type variable T as @Inject field type\n"
            + "    T bareTypeField;\n"
            + "\n";

    // Bare array field without @Inject (annotation removed by quickfix)
    private static final String BARE_ARRAY_FIELD_NO_INJECT =
            "    // Invalid: array of bare type variable T[] as @Inject field type\n"
            + "    T[] bareTypeArrayField;\n"
            + "\n";

    // setBareType method without @Inject (annotation removed by quickfix)
    private static final String BARE_METHOD_NO_INJECT =
            "    // Invalid: bare type variable T as @Inject method parameter\n"
            + "    public void setBareType(T value) {\n"
            + "    }\n"
            + "\n";

    // setBareTypeArray method without @Inject (annotation removed by quickfix)
    private static final String BARE_ARRAY_METHOD_NO_INJECT =
            "    // Invalid: array of bare type variable T[] as @Inject method parameter\n"
            + "    public void setBareTypeArray(T[] values) {\n"
            + "    }\n"
            + "\n";

    @Test
    public void bareTypeVariableAtInjectField() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/cdi/BareTypeVariableInjectBean.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 25 (0-based 24): T bareTypeField — field name "bareTypeField" at col 6..19
        Diagnostic injectBareTypeField = d(24, 6, 19,
                "A type variable is not a legal bean type. Injection point fields must not use a bare type variable (T or T[]).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidBareTypeVariableInInjectField");

        // Line 29 (0-based 28): T[] bareTypeArrayField — field name "bareTypeArrayField" at col 8..26
        Diagnostic injectBareTypeArrayField = d(28, 8, 26,
                "A type variable is not a legal bean type. Injection point fields must not use a bare type variable (T or T[]).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidBareTypeVariableInInjectField");

        // Line 33 (0-based 32): setBareType — method name at col 16..27 (diagnostic on method)
        Diagnostic injectBareTypeMethodParam = d(32, 16, 27,
                "A type variable is not a legal bean type. Parameter 'value' must not use a bare type variable (T or T[]).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidBareTypeVariableInInjectMethodParam");

        // Line 38 (0-based 37): setBareTypeArray — method name at col 16..32 (diagnostic on method)
        Diagnostic injectBareTypeArrayMethodParam = d(37, 16, 32,
                "A type variable is not a legal bean type. Parameter 'values' must not use a bare type variable (T or T[]).",
                DiagnosticSeverity.Error, "jakarta-cdi", "InvalidBareTypeVariableInInjectMethodParam");

        assertJavaDiagnostics(diagnosticsParams, utils,
                injectBareTypeField,
                injectBareTypeArrayField,
                injectBareTypeMethodParam,
                injectBareTypeArrayMethodParam);

        // --- QuickFix: Remove @Inject from T bareTypeField ---
        // Expected: whole-file replacement with @Inject removed from bareTypeField
        String afterRemoveBareTypeFieldInject = FILE_HEADER
                + BARE_FIELD_NO_INJECT
                + INVALID_BARE_ARRAY_FIELD
                + INVALID_BARE_METHOD
                + INVALID_BARE_ARRAY_METHOD
                + VALID_TAIL;
        JakartaJavaCodeActionParams codeActionBareTypeField = createCodeActionParams(uri, injectBareTypeField);
        TextEdit removeBareTypeFieldInject = te(0, 0, 48, 0, afterRemoveBareTypeFieldInject);
        CodeAction removeBareTypeFieldInjectAction = ca(uri, "Remove @Inject", injectBareTypeField,
                removeBareTypeFieldInject);
        assertJavaCodeAction(codeActionBareTypeField, utils, removeBareTypeFieldInjectAction);

        // --- QuickFix: Remove @Inject from T[] bareTypeArrayField ---
        String afterRemoveBareTypeArrayFieldInject = FILE_HEADER
                + INVALID_BARE_FIELD
                + BARE_ARRAY_FIELD_NO_INJECT
                + INVALID_BARE_METHOD
                + INVALID_BARE_ARRAY_METHOD
                + VALID_TAIL;
        JakartaJavaCodeActionParams codeActionBareTypeArrayField = createCodeActionParams(uri, injectBareTypeArrayField);
        TextEdit removeBareTypeArrayFieldInject = te(0, 0, 48, 0, afterRemoveBareTypeArrayFieldInject);
        CodeAction removeBareTypeArrayFieldInjectAction = ca(uri, "Remove @Inject", injectBareTypeArrayField,
                removeBareTypeArrayFieldInject);
        assertJavaCodeAction(codeActionBareTypeArrayField, utils, removeBareTypeArrayFieldInjectAction);

        // --- QuickFix: Remove @Inject from setBareType method ---
        String afterRemoveBareTypeMethodInject = FILE_HEADER
                + INVALID_BARE_FIELD
                + INVALID_BARE_ARRAY_FIELD
                + BARE_METHOD_NO_INJECT
                + INVALID_BARE_ARRAY_METHOD
                + VALID_TAIL;
        JakartaJavaCodeActionParams codeActionBareTypeMethodParam = createCodeActionParams(uri, injectBareTypeMethodParam);
        TextEdit removeBareTypeMethodInject = te(0, 0, 48, 0, afterRemoveBareTypeMethodInject);
        CodeAction removeBareTypeMethodInjectAction = ca(uri, "Remove @Inject", injectBareTypeMethodParam,
                removeBareTypeMethodInject);
        assertJavaCodeAction(codeActionBareTypeMethodParam, utils, removeBareTypeMethodInjectAction);

        // --- QuickFix: Remove @Inject from setBareTypeArray method ---
        String afterRemoveBareTypeArrayMethodInject = FILE_HEADER
                + INVALID_BARE_FIELD
                + INVALID_BARE_ARRAY_FIELD
                + INVALID_BARE_METHOD
                + BARE_ARRAY_METHOD_NO_INJECT
                + VALID_TAIL;
        JakartaJavaCodeActionParams codeActionBareTypeArrayMethodParam = createCodeActionParams(uri,
                injectBareTypeArrayMethodParam);
        TextEdit removeBareTypeArrayMethodInject = te(0, 0, 48, 0, afterRemoveBareTypeArrayMethodInject);
        CodeAction removeBareTypeArrayMethodInjectAction = ca(uri, "Remove @Inject", injectBareTypeArrayMethodParam,
                removeBareTypeArrayMethodInject);
        assertJavaCodeAction(codeActionBareTypeArrayMethodParam, utils, removeBareTypeArrayMethodInjectAction);
    }
}
