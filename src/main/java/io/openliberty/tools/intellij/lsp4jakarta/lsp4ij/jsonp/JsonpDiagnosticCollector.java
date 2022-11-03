/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Yijia Jing
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jsonp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.PositionUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.Range;

public class JsonpDiagnosticCollector extends AbstractDiagnosticsCollector {

    public JsonpDiagnosticCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return JsonpConstants.DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit == null) {
            return;
        }
        Collection<PsiMethodCallExpression> allMethodInvocations = PsiTreeUtil.findChildrenOfType(unit, PsiMethodCallExpression.class);
        List<PsiMethodCallExpression> createPointerInvocations = new ArrayList<>();
        for (PsiMethodCallExpression mi : allMethodInvocations) {
            if (isMatchedJsonCreatePointer(mi)) {
                createPointerInvocations.add(mi);
            }
        }
        for (PsiMethodCallExpression m: createPointerInvocations) {
            PsiExpression arg = m.getArgumentList().getExpressions()[0]; // already checked that call has one arg
            if (isInvalidArgument(arg)) {
                // If the argument supplied to a createPointer invocation is a String literal and is neither an empty String
                // or a sequence of '/' prefixed tokens, a diagnostic highlighting the invalid argument is created.
                Range range = PositionUtils.toNameRange(arg);
                Diagnostic diagnostic = new Diagnostic(range, JsonpConstants.CREATE_POINTER_ERROR_MESSAGE);
                completeDiagnostic(diagnostic, JsonpConstants.DIAGNOSTIC_CODE_CREATE_POINTER);
                diagnostics.add(diagnostic);
            }
        }
    }

    private boolean isInvalidArgument(PsiExpression arg) {
        if (arg instanceof PsiLiteralExpression) {
            if (((PsiLiteralExpression) arg).getValue() instanceof String) {
                String argValue = (String)((PsiLiteralExpression) arg).getValue();
                if (!(argValue.isEmpty() || argValue.matches("^(\\/[^\\/]+)+$"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isMatchedJsonCreatePointer(PsiMethodCallExpression mi) {
        String fqName = JsonpConstants.JSON_FQ_NAME + "." + JsonpConstants.CREATE_POINTER;
        String miName = null;
        PsiMethod m = mi.resolveMethod();
        if (m != null && m.getClass() != null) {
            miName = m.getContainingClass().getQualifiedName() + "." + m.getName();
        }
        return mi.getArgumentList().getExpressionCount() == 1
                && fqName.equals(miName);
    }
}
