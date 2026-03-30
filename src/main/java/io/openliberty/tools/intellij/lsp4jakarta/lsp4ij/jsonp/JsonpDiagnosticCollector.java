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
import org.apache.commons.lang3.StringUtils;
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
        //Used to get the list of method invocations for JsonArrayBuilder add methods
        List<PsiMethodCallExpression> createArrayBuilderMethodInvocations = new ArrayList<>();

        for (PsiMethodCallExpression mi : allMethodInvocations) {
            if (isMatchedMethodFQName(mi, JsonpConstants.JSON_FQ_NAME)) {
                createPointerInvocations.add(mi);
            }
            if (isMatchedMethodFQName(mi, JsonpConstants.JAKARTA_JSON_OBJECT_BUILDER_FQ_NAME)){
                createObjectBuilderMethodInvocations.add(mi);
            }
            if (isMatchedMethodFQName(mi, JsonpConstants.JAKARTA_JSON_ARRAY_BUILDER_FQ_NAME)){
                createArrayBuilderMethodInvocations.add(mi);
            }
        }

        // If the argument supplied to a createPointer invocation is a String literal and is neither an empty String
        // or a sequence of '/' prefixed tokens, a diagnostic highlighting the invalid argument is created.
        createDiagnosticsForMethodInvocations(diagnostics, createPointerInvocations,
                Messages.getMessage("CreatePointerErrorMessage"),
                JsonpConstants.DIAGNOSTIC_CODE_CREATE_POINTER);

        //https://jakarta.ee/specifications/jsonp/2.1/apidocs/jakarta.json/jakarta/json/jsonobjectbuilder
        //Does not allow key to be null for JsonObjectBuilder.add() method
        createDiagnosticsForMethodInvocations(diagnostics, createObjectBuilderMethodInvocations,
                Messages.getMessage("ErrorMessageJsonPObjectKeyNonNull"),
                JsonpConstants.DIAGNOSTIC_CODE_INVALID_OBJECT_BUILDER_KEY);

        // https://jakarta.ee/specifications/jsonp/2.1/apidocs/jakarta.json/jakarta/json/jsonarraybuilder
        // Does not allow value to be null for JsonArrayBuilder.add() method
        createDiagnosticsForMethodInvocations(diagnostics, createArrayBuilderMethodInvocations,
                Messages.getMessage("ErrorMessageJsonPArrayValueNonNull"),
                JsonpConstants.DIAGNOSTIC_CODE_INVALID_ARRAY_BUILDER_VALUE);
    }

    /**
     * Method used to create diagnostics for
     * invalid createPointer or
     * invalid JsonObjectBuilder or
     * invalid JsonArrayBuilder
     *
     * @param diagnostics
     * @param builderMethodInvocations
     * @param msg
     * @param errCode
     */
    private void createDiagnosticsForMethodInvocations(List<Diagnostic> diagnostics,
                                                       List<PsiMethodCallExpression> builderMethodInvocations,
                                                       String msg, String errCode) {
        boolean invalidArgs= false;
        for(PsiMethodCallExpression m: builderMethodInvocations){
            PsiExpression arg = m.getArgumentList().getExpressions()[0];
            if(getMethodName(m).equals(JsonpConstants.CREATE_POINTER)){
                invalidArgs = isInvalidArgumentCreatePointer(arg);
            } else if(getMethodName(m).equals(JsonpConstants.JAKARTA_JSON_BUILDER_ADD_METHOD)){
                invalidArgs = isInvalidNullArgument(arg);
            }
            if(invalidArgs) {
                Range range = PositionUtils.toNameRange(arg);
                Diagnostic diagnostic = new Diagnostic(range, msg);
                completeDiagnostic(diagnostic, errCode);
                diagnostics.add(diagnostic);
            }
        }
    }

    /**
     * Method is used to check if value of arg passed or Cast Expression inside passed arg is null
     *
     * @param arg
     * @return
     */
    private boolean isInvalidNullArgument(PsiExpression arg) {
        if (arg instanceof PsiLiteralExpression lit && lit.getValue() == null) {
            return true;
        }
        if (arg instanceof PsiTypeCastExpression cast) {
            return cast.getOperand() instanceof PsiLiteralExpression
                    && ((PsiLiteralExpression) cast.getOperand()).getValue() == null ;
        }
        return false;
    }

    /**
     * isMatchedMethodFQName
     * Method is used to identify passed method invocations
     *
     * @param mce
     * @param methodParentTypeFQ
     * @return boolean
     */
    private boolean isMatchedMethodFQName(PsiMethodCallExpression mce, String methodParentTypeFQ) {
        PsiMethod method = mce.resolveMethod();
        if(getMethodName(mce).equals(JsonpConstants.CREATE_POINTER)){
            return mce.getArgumentList().getExpressionCount() == JsonpConstants.EXPRESSION_COUNT_CREATE_POINTER
                    && methodParentTypeFQ.equals(method.getContainingClass().getQualifiedName());
        } else if(getMethodName(mce).equals(JsonpConstants.JAKARTA_JSON_BUILDER_ADD_METHOD)){
            return methodParentTypeFQ.equals(method.getContainingClass().getQualifiedName());
        }
        return false;
    }

    /**
     * Check if valid method exists
     *
     * @param methodCall
     * @return
     */
    private String getMethodName(PsiMethodCallExpression methodCall) {
        PsiMethod method = methodCall.resolveMethod();
        if(method != null && method.getClass() != null){
           return method.getName();
        }
        return StringUtils.EMPTY;
    }

    private boolean isInvalidArgumentCreatePointer(PsiExpression arg) {
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
