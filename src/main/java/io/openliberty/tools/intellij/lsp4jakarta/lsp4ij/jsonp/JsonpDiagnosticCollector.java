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
import org.eclipse.lsp4j.DiagnosticSeverity;
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

        // Detect manual JSON parsing patterns that should use JSON-B
        detectManualJsonParsingPatterns(diagnostics, allMethodInvocations);
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
        for(PsiMethodCallExpression m: builderMethodInvocations){
            if(getMethodName(m.resolveMethod()).equals(JsonpConstants.CREATE_POINTER)){
                PsiExpression arg = m.getArgumentList().getExpressions()[0];
                if(isInvalidArgumentCreatePointer(arg)) {
                    buildInvalidArgumentDiagnostic(diagnostics, msg, errCode, arg);
                }
            } else if(getMethodName(m.resolveMethod()).equals(JsonpConstants.JAKARTA_JSON_BUILDER_ADD_METHOD)){
                PsiExpression[] args = m.getArgumentList().getExpressions();
                for(PsiExpression arg : args) {
                    if(isInvalidNullArgument(arg)) {
                        buildInvalidArgumentDiagnostic(diagnostics, msg, errCode, arg);
                    }
                }
            }
        }
    }

    /**
     * Adds a diagnostic for an invalid argument in JSON-P method invocations
     *
     * @param diagnostics the list to add the diagnostic to
     * @param msg the diagnostic message
     * @param errCode the error code for the diagnostic
     * @param arg the invalid argument expression
     */
    private void buildInvalidArgumentDiagnostic(List<Diagnostic> diagnostics, String msg, String errCode, PsiExpression arg) {
        Range range = PositionUtils.toNameRange(arg);
        Diagnostic diagnostic = new Diagnostic(range, msg);
        completeDiagnostic(diagnostic, errCode);
        diagnostics.add(diagnostic);
    }

    /**
     * Method is used to check if value of arg passed or Cast Expression inside passed arg is null
     *
     * @param arg
     * @return
     */
    private boolean isInvalidNullArgument(PsiExpression arg) {
        return (arg instanceof PsiLiteralExpression lit && lit.getValue() == null)
                || (arg instanceof PsiTypeCastExpression cast
                && cast.getOperand() instanceof PsiLiteralExpression
                && ((PsiLiteralExpression) cast.getOperand()).getValue() == null);
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
        if(getMethodName(method).equals(JsonpConstants.CREATE_POINTER)){
            return mce.getArgumentList().getExpressionCount() == JsonpConstants.EXPRESSION_COUNT_CREATE_POINTER
                    && methodParentTypeFQ.equals(method.getContainingClass().getQualifiedName());
        } else if(getMethodName(method).equals(JsonpConstants.JAKARTA_JSON_BUILDER_ADD_METHOD)){
            return methodParentTypeFQ.equals(method.getContainingClass().getQualifiedName());
        }
        return false;
    }

    /**
     * Check if valid method exists
     *
     * @param method
     * @return
     */
    private String getMethodName(PsiMethod method) {
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

    /**
     * Detects manual JSON parsing patterns where Json.createReader() is used
     * followed by manual field mapping with getter methods like getString(), getInt(), etc.
     * This pattern should use JSON-B instead for better performance and maintainability.
     *
     * @param diagnostics the list to add diagnostics to
     * @param allMethodInvocations all method call expressions in the file
     */
    private void detectManualJsonParsingPatterns(List<Diagnostic> diagnostics,
                                                 Collection<PsiMethodCallExpression> allMethodInvocations) {
        for (PsiMethodCallExpression mce : allMethodInvocations) {
            if (isJsonCreateReaderInvocation(mce) && isManualParsingPattern(mce, allMethodInvocations)) {
                Range range = PositionUtils.toNameRange(mce);
                Diagnostic diagnostic = new Diagnostic(range, Messages.getMessage("UseJsonbInsteadOfManualParsing"));
                completeDiagnostic(diagnostic, JsonpConstants.DIAGNOSTIC_CODE_USE_JSONB,
                                   DiagnosticSeverity.Warning);
                diagnostics.add(diagnostic);
            }
        }
    }

    /**
     * Checks if a method call expression is Json.createReader().
     *
     * @param mce the method call expression
     * @return true if it's Json.createReader()
     */
    private boolean isJsonCreateReaderInvocation(PsiMethodCallExpression mce) {
        PsiMethod method = mce.resolveMethod();
        if (method == null || !JsonpConstants.CREATE_READER.equals(method.getName())) {
            return false;
        }
        PsiClass containingClass = method.getContainingClass();
        return containingClass != null && JsonpConstants.JSON_FQ_NAME.equals(containingClass.getQualifiedName());
    }

    /**
     * Checks if a createReader invocation is part of a manual parsing pattern.
     * A manual parsing pattern is detected when readObject() and JsonObject getter
     * methods (getString, getInt, etc.) are also called in the same method scope.
     *
     * @param createReaderMce the createReader method call expression
     * @param allMethodInvocations all method call expressions in the file
     * @return true if manual parsing pattern is detected
     */
    private boolean isManualParsingPattern(PsiMethodCallExpression createReaderMce,
                                           Collection<PsiMethodCallExpression> allMethodInvocations) {
        boolean hasReadObject = allMethodInvocations.stream()
            .anyMatch(mi -> isReadObjectInvocation(mi) && isInSameMethodScope(createReaderMce, mi));

        if (!hasReadObject) {
            return false;
        }

        return allMethodInvocations.stream()
            .anyMatch(mi -> isJsonObjectGetterMethod(mi) && isInSameMethodScope(createReaderMce, mi));
    }

    /**
     * Checks if a method call expression is JsonReader.readObject().
     *
     * @param mce the method call expression
     * @return true if it's readObject() on a JsonReader
     */
    private boolean isReadObjectInvocation(PsiMethodCallExpression mce) {
        PsiMethod method = mce.resolveMethod();
        if (method == null || !JsonpConstants.READ_OBJECT.equals(method.getName())) {
            return false;
        }
        PsiClass containingClass = method.getContainingClass();
        return containingClass != null && JsonpConstants.JSON_READER_FQ_NAME.equals(containingClass.getQualifiedName());
    }

    /**
     * Checks if a method call expression is a JsonObject getter method
     * (getString, getInt, getBoolean, etc.).
     *
     * @param mce the method call expression
     * @return true if it's a JsonObject getter method
     */
    private boolean isJsonObjectGetterMethod(PsiMethodCallExpression mce) {
        PsiMethod method = mce.resolveMethod();
        if (method == null) {
            return false;
        }
        String name = method.getName();
        if (!name.equals(JsonpConstants.GET_STRING) &&
            !name.equals(JsonpConstants.GET_INT) &&
            !name.equals(JsonpConstants.GET_BOOLEAN) &&
            !name.equals(JsonpConstants.GET_JSON_NUMBER) &&
            !name.equals(JsonpConstants.GET_JSON_OBJECT) &&
            !name.equals(JsonpConstants.GET_JSON_ARRAY)) {
            return false;
        }
        PsiClass containingClass = method.getContainingClass();
        return containingClass != null && JsonpConstants.JSON_OBJECT_FQ_NAME.equals(containingClass.getQualifiedName());
    }

    /**
     * Checks if two method call expressions are enclosed by the same PsiMethod.
     *
     * @param mce1 first method call expression
     * @param mce2 second method call expression
     * @return true if both are enclosed by the same PsiMethod
     */
    private boolean isInSameMethodScope(PsiMethodCallExpression mce1, PsiMethodCallExpression mce2) {
        PsiMethod enclosing1 = PsiTreeUtil.getParentOfType(mce1, PsiMethod.class);
        PsiMethod enclosing2 = PsiTreeUtil.getParentOfType(mce2, PsiMethod.class);
        return enclosing1 != null && enclosing1 == enclosing2;
    }
}
