/*******************************************************************************
 * Copyright (c) 2022, 2026 IBM Corporation and others.
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
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
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
        //Used to get the list of method invocations for JsonObjectBuilder add methods
        List<PsiMethodCallExpression> createObjectBuilderMethodInvocations = new ArrayList<>();
        String createPointerFQName = JsonpConstants.JSON_FQ_NAME + "." + JsonpConstants.CREATE_POINTER;
        for (PsiMethodCallExpression mi : allMethodInvocations) {
            if (isMatchedMethodFQName(mi,JsonpConstants.EXPRESSION_COUNT_CREATE_POINTER, createPointerFQName)) {
                createPointerInvocations.add(mi);
            }
            if (isMatchedMethodFQName(mi,JsonpConstants.EXPRESSION_COUNT_ADD, JsonpConstants.JAKARTA_JSON_OBJECT_BUILDER_ADD)){
                createObjectBuilderMethodInvocations.add(mi);
            }
        }
        for (PsiMethodCallExpression m: createPointerInvocations) {
            PsiExpression arg = m.getArgumentList().getExpressions()[0]; // already checked that call has one arg
            if (isInvalidArgument(arg)) {
                // If the argument supplied to a createPointer invocation is a String literal and is neither an empty String
                // or a sequence of '/' prefixed tokens, a diagnostic highlighting the invalid argument is created.
                Range range = PositionUtils.toNameRange(arg);
                Diagnostic diagnostic = new Diagnostic(range, Messages.getMessage("CreatePointerErrorMessage"));
                completeDiagnostic(diagnostic, JsonpConstants.DIAGNOSTIC_CODE_CREATE_POINTER);
                diagnostics.add(diagnostic);
            }
        }
        for(PsiMethodCallExpression m: createObjectBuilderMethodInvocations){
            PsiExpression arg = m.getArgumentList().getExpressions()[0];
            //https://jakarta.ee/specifications/jsonp/2.1/apidocs/jakarta.json/jakarta/json/jsonobjectbuilder
            //Does not allow key to be null for JsonObjectBuilder.add() method
            if(arg instanceof PsiLiteralExpression lit && lit.getValue() == null) {
                Range range = PositionUtils.toNameRange(arg);
                Diagnostic diagnostic = new Diagnostic(range, Messages.getMessage("ErrorMessageJsonPObjectKeyNonNull"));
                completeDiagnostic(diagnostic, JsonpConstants.DIAGNOSTIC_CODE_INVALID_OBJECT_BUILDER_KEY);
                diagnostics.add(diagnostic);
            }
        }
    }

    /**
     * isMatchedMethodFQName
     * Method is used to identify passed method invocations
     *
     * @param mi
     * @return boolean
     */
    private boolean isMatchedMethodFQName(PsiMethodCallExpression mi, int expressionCount, String methodNameFQ) {
        String miFQName = null;
        PsiMethod m = mi.resolveMethod();
        if (m != null && m.getClass() != null) {
            miFQName = m.getContainingClass().getQualifiedName() + "." + m.getName();
        }
        return mi.getArgumentList().getExpressionCount() == expressionCount
                && methodNameFQ.equals(miFQName);
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
}
