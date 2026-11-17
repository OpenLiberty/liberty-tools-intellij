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
 *     IBM Corporation - initial implementation
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.jsonp;

import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Detects JSON-P code that reads a JsonObject and copies its values into a plain
 * Java object field by field, which Jakarta JSON Binding (Jsonb.fromJson) can do
 * with less code and type safety.
 *
 * Within a single method, a JsonReader.readObject() call is reported when a value read from the
 * resulting JsonObject is passed to a setter or constructor, or assigned to a field, of a
 * non-JSON object. Values that are only inspected, or generic JSON-P processing, are not reported.
 *
 * https://openliberty.io/docs/latest/json-p-b.html
 */
public class ManualJsonMappingAnalyzer {

    private ManualJsonMappingAnalyzer() {
    }

    /**
     * Finds the JsonReader.readObject() invocations whose result is mapped manually into an object.
     *
     * @param allMethodInvocations all method call expressions of the file
     * @return the readObject() invocations to report
     */
    public static List<PsiMethodCallExpression> findManualMappings(Collection<PsiMethodCallExpression> allMethodInvocations) {
        List<PsiMethodCallExpression> result = new ArrayList<>();
        // Most files have no JsonReader.readObject() call, so exit before any further work
        List<PsiMethodCallExpression> readObjectInvocations = allMethodInvocations.stream()
                .filter(ManualJsonMappingAnalyzer::isJsonReaderReadObject).collect(Collectors.toList());
        if (readObjectInvocations.isEmpty()) {
            return result;
        }
        List<PsiMethodCallExpression> mappedGetters = allMethodInvocations.stream()
                .filter(ManualJsonMappingAnalyzer::isJsonObjectValueGetter)
                .filter(ManualJsonMappingAnalyzer::isCopiedIntoObject).collect(Collectors.toList());
        if (mappedGetters.isEmpty()) {
            return result;
        }
        for (PsiMethodCallExpression readObject : readObjectInvocations) {
            PsiMethod enclosingMethod = PsiTreeUtil.getParentOfType(readObject, PsiMethod.class);
            if (enclosingMethod == null) {
                continue;
            }
            PsiVariable assignedVariable = getAssignedVariable(readObject);
            boolean mapped = mappedGetters.stream()
                    .anyMatch(getter -> PsiTreeUtil.getParentOfType(getter, PsiMethod.class) == enclosingMethod
                            && isReadFrom(getter, readObject, assignedVariable));
            if (mapped) {
                result.add(readObject);
            }
        }
        return result;
    }

    private static boolean isJsonReaderReadObject(PsiMethodCallExpression mce) {
        return isMethodOfClass(mce, JsonpConstants.JSON_READER_READ_OBJECT, JsonpConstants.JAKARTA_JSON_READER_FQ_NAME);
    }

    /** Any JsonObject method that reads a value, e.g. getString, getInt, getJsonNumber. */
    private static boolean isJsonObjectValueGetter(PsiMethodCallExpression mce) {
        PsiMethod method = mce.resolveMethod();
        return method != null && method.getName().startsWith(JsonpConstants.GETTER_PREFIX)
                && isMethodOfClass(mce, method.getName(), JsonpConstants.JAKARTA_JSON_OBJECT_FQ_NAME);
    }

    private static boolean isMethodOfClass(PsiMethodCallExpression mce, String methodName, String classFQName) {
        PsiMethod method = mce.resolveMethod();
        if (method == null || !methodName.equals(method.getName())) {
            return false;
        }
        PsiClass containingClass = method.getContainingClass();
        return containingClass != null && classFQName.equals(containingClass.getQualifiedName());
    }

    /**
     * Checks that the getter is called on the JsonObject returned by the given readObject() invocation,
     * either directly (chained call) or through the variable it was assigned to.
     *
     * @param assignedVariable the variable readObject() is assigned to, or null if it is not assigned
     */
    private static boolean isReadFrom(PsiMethodCallExpression getter, PsiMethodCallExpression readObject,
                                      PsiVariable assignedVariable) {
        PsiExpression receiver = PsiUtil.skipParenthesizedExprDown(getter.getMethodExpression().getQualifierExpression());
        if (receiver == readObject) {
            return true;
        }
        if (receiver instanceof PsiReferenceExpression ref) {
            return assignedVariable != null && assignedVariable.equals(ref.resolve());
        }
        return false;
    }

    /**
     * Returns the variable the result of the given invocation is stored in, or null.
     */
    private static PsiVariable getAssignedVariable(PsiMethodCallExpression invocation) {
        PsiElement parent = invocation.getParent();
        if (parent instanceof PsiVariable variable && variable.getInitializer() == invocation) {
            return variable;
        }
        if (parent instanceof PsiAssignmentExpression assignment && assignment.getRExpression() == invocation
                && assignment.getLExpression() instanceof PsiReferenceExpression lhs
                && lhs.resolve() instanceof PsiVariable variable) {
            return variable;
        }
        return null;
    }

    /** True when the invocation returns a jakarta.json type such as JsonNumber or JsonObject. */
    private static boolean returnsJsonType(PsiMethodCallExpression invocation) {
        PsiType returnType = invocation.getType();
        return returnType != null && returnType.getCanonicalText().startsWith(JsonpConstants.JAKARTA_JSON_PACKAGE_PREFIX);
    }

    /** A setter name is "set" followed by an upper-case letter, e.g. setName. */
    private static boolean isSetterName(String name) {
        return name.length() > JsonpConstants.SETTER_PREFIX.length() && name.startsWith(JsonpConstants.SETTER_PREFIX)
                && Character.isUpperCase(name.charAt(JsonpConstants.SETTER_PREFIX.length()));
    }

    /**
     * Checks that the value returned by the getter is stored into a non-JSON object, which means
     * it is passed to a setter or constructor, or assigned to a field. One call on the returned
     * value is allowed, e.g. {@code json.getJsonNumber("x").doubleValue()}. A getter that returns
     * a JSON type (JsonNumber, JsonObject, ...) counts only when such a call turns it into a plain value.
     */
    private static boolean isCopiedIntoObject(PsiMethodCallExpression getter) {
        PsiExpression value = getter;
        if (getter.getParent() instanceof PsiReferenceExpression ref && ref.getQualifierExpression() == getter
                && ref.getParent() instanceof PsiMethodCallExpression chained) {
            value = chained;
        }
        if (value == getter && returnsJsonType(getter)) {
            return false;
        }
        PsiElement parent = value.getParent();
        if (parent instanceof PsiExpressionList argumentList) {
            PsiElement call = argumentList.getParent();
            if (call instanceof PsiMethodCallExpression setter) {
                PsiMethod method = setter.resolveMethod();
                PsiClass declaringClass = method != null ? method.getContainingClass() : null;
                String declaringClassName = declaringClass != null ? declaringClass.getQualifiedName() : null;
                return method != null && isSetterName(method.getName())
                        && setter.getArgumentList().getExpressionCount() == 1
                        && declaringClassName != null
                        && !declaringClassName.startsWith(JsonpConstants.JAKARTA_JSON_PACKAGE_PREFIX);
            }
            if (call instanceof PsiNewExpression creation) {
                PsiType type = creation.getType();
                if (type == null) {
                    return false;
                }
                String typeName = type.getCanonicalText();
                // JDK types (File, BigDecimal, ...) are value conversions, not POJO mapping
                return !typeName.startsWith(JsonpConstants.JAKARTA_JSON_PACKAGE_PREFIX)
                        && !typeName.startsWith("java.") && !typeName.startsWith("javax.");
            }
            return false;
        }
        if (parent instanceof PsiAssignmentExpression assignment && assignment.getRExpression() == value
                && assignment.getLExpression() instanceof PsiReferenceExpression lhs) {
            return lhs.resolve() instanceof PsiField;
        }
        return false;
    }
}
