/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation
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

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for common IntelliJ PSI-based diagnostic logic.
 */
public class DiagnosticsUtils {
    
    /**
     * inheritsFrom
     * find super class and Check
     *
     * @param clazz
     * @param fqSuperType
     * @return
     */
    public static boolean inheritsFrom(PsiClass clazz, String fqSuperType) {
        Project project = clazz.getProject();
        PsiClass superClass = JavaPsiFacade.getInstance(project)
                .findClass(fqSuperType, GlobalSearchScope.allScope(project));
        return inheritsFrom(clazz, superClass);
    }

    /**
     * inheritsFrom
     * Check if specified superClass is present or not in the type hierarchy
     *
     * @param clazz
     * @param superClass
     * @return
     */
    public static boolean inheritsFrom(PsiClass clazz, PsiClass superClass) {
        if (clazz == null || superClass == null) {
            return false;
        }
        return clazz.isEquivalentTo(superClass) || clazz.isInheritor(superClass, true);
    }

    /**
     * isPublic
     * Check if the given method is public or not
     *
     * @param method
     * @return
     */
    public static boolean isPublic(PsiMethod method) {
        return method.hasModifierProperty(PsiModifier.PUBLIC);
    }

    /**
     * hasField
     * Checks if the given type has a field matching the method name.
     *
     * @param methodName
     * @param type
     * @return
     */
    public static boolean hasField(String methodName, PsiClass type)  {
        if (methodName == null || methodName.length() <= 3) {
            return false;
        }
        String expectedFieldName = Introspector.decapitalize(methodName.substring(3));
        if (expectedFieldName.isEmpty()) {
            return false;
        }
        PsiField field = type.findFieldByName(expectedFieldName, false);
        return field!= null;
    }


    /**
     * validateSetterMethod
     * This is to check whether a method is a valid setter.
     *
     * @param method
     * @param parentType
     * @return
     */
    public static List<String> validateSetterMethod(PsiMethod method, PsiClass parentType) {
        List<String> errorCodes = new ArrayList<>();
        String methodName = method.getName();
        PsiType returnType = method.getReturnType();
        if (!methodName.startsWith("set")) {
            errorCodes.add(CommonConstants.DIAGNOSTIC_CODE_METHOD_NAME_START_WITH_SET);
        }
        if (!hasField(methodName, parentType)) {
            errorCodes.add(CommonConstants.DIAGNOSTIC_CODE_FIELD_MUST_EXIST_IN_SETTER);
        }
        if (!(returnType == null || returnType.equals(PsiTypes.voidType()))) {
            errorCodes.add(CommonConstants.DIAGNOSTIC_CODE_RETURN_TYPE_MUST_BE_VOID);
        }
        if (method.getParameterList().getParametersCount() != 1) {
            errorCodes.add(CommonConstants.DIAGNOSTIC_CODE_MUST_DECLARE_EXACTLY_ONE_PARAM);
        }
        if (!isPublic(method)) {
            errorCodes.add(CommonConstants.DIAGNOSTIC_CODE_METHOD_MUST_BE_PUBLIC);
        }
        return errorCodes;
    }

    /**
     * Check the given PsiClass is a Java Class
     * @param psiClass
     * @return Returns true if the given PsiClass is a Java Class
     */
    public static boolean isClass(PsiClass psiClass) {
        return psiClass != null &&
                !psiClass.isInterface() &&
                !psiClass.isEnum() &&
                !psiClass.isAnnotationType() &&
                !psiClass.isRecord();
    }

    /**
     * Returns true if the java element name matches the given fully qualified java
     * element name and false otherwise.
     *
     * @param type              Java class.
     * @param javaElementName   given object name.
     * @param javaElementFQName the fully qualified name.
     * @return true if the java element name matches the given fully qualified java
     *         element name and false otherwise.
     */
    public static boolean isMatchedJavaElement(PsiClass type, String javaElementName, String javaElementFQName) {
        if (javaElementFQName.equals(javaElementName)) {
            JavaPsiFacade facade = JavaPsiFacade.getInstance(type.getProject());
            Object o = facade.findClass(javaElementFQName, GlobalSearchScope.allScope(type.getProject()));
            return (o != null);
        }
        return false;
    }

    public static List<PsiClass> collectSuperClasses(PsiClass psiClass) {
        List<PsiClass> superClasses = new ArrayList<>();
        PsiClass current = psiClass.getSuperClass();

        while (current != null) {
            superClasses.add(current);
            current = current.getSuperClass();
        }
        return superClasses;
    }

    /**
     * Returns {@code true} if the entity type uses field-based access, i.e. at
     * least one field on {@code psiClass} directly carries {@code @Id} or
     * {@code @EmbeddedId}.
     *
     * <p>JPA access type is determined by the placement of {@code @Id} on the
     * declaring entity — no superclass walk is needed here.
     *
     * @param psiClass     the entity class to inspect
     * @param idFQ         the fully-qualified name of {@code @Id}
     * @param embeddedIdFQ the fully-qualified name of {@code @EmbeddedId}
     * @return {@code true} if any field on {@code psiClass} is annotated with
     *         {@code @Id} or {@code @EmbeddedId}
     */
    public static boolean isFieldBasedAccess(PsiClass psiClass, String idFQ, String embeddedIdFQ) {
        return Arrays.stream(psiClass.getFields())
                .anyMatch(f -> f.hasAnnotation(idFQ) || f.hasAnnotation(embeddedIdFQ));
    }

    /**
     * Returns {@code true} if {@code psiClass} or any superclass in its hierarchy
     * declares a field with the given {@code fieldName}, using PSI's
     * {@link PsiClass#findFieldByName(String, boolean)} with hierarchy search enabled.
     *
     * @param psiClass  the class to start the search from
     * @param fieldName the simple field name to look for
     * @return {@code true} if a matching field is found in the class hierarchy
     */
    public static boolean hasFieldInHierarchy(PsiClass psiClass, String fieldName) {
        // findFieldByName(name, true) searches the class and all superclasses.
        return psiClass.findFieldByName(fieldName, true) != null;
    }

    /**
     * Returns the values of an array-valued annotation member, normalizing PSI's
     * two representations into a single list.
     *
     * <p>PSI represents the member as a {@link PsiArrayInitializerMemberValue} when
     * array notation is used (e.g. {@code {@A, @B}}), but as the bare element when a
     * single value is given without braces (e.g. {@code @A}). Both forms are handled.</p>
     *
     * @param annotation  the annotation to inspect
     * @param memberName  the member/attribute name
     * @param elementType the expected type of each element; elements of other types are skipped
     * @return the matching elements, or an empty list if the member is absent
     */
    public static <T extends PsiAnnotationMemberValue> List<T> getAnnotationMemberValues(
            PsiAnnotation annotation, String memberName, Class<T> elementType) {
        PsiAnnotationMemberValue value = annotation.findAttributeValue(memberName);
        if (value == null) {
            return List.of();
        }
        PsiAnnotationMemberValue[] items = (value instanceof PsiArrayInitializerMemberValue arrayValue)
                ? arrayValue.getInitializers()
                : new PsiAnnotationMemberValue[] { value };
        return Arrays.stream(items)
                .filter(elementType::isInstance)
                .map(elementType::cast)
                .toList();
    }

    /**
     * Returns {@code true} if the given {@code @Priority} annotation carries a
     * negative integer value.
     *
     * <p>Handles two PSI representations of a negative integer literal:
     * <ul>
     *   <li>A {@link PsiPrefixExpression} with a {@code MINUS} operator wrapping a
     *       {@link PsiLiteralExpression} (e.g. {@code -1}).</li>
     *   <li>A raw text value that can be parsed as a negative integer.</li>
     * </ul>
     *
     * @param priorityAnnotation the {@code @Priority} annotation to inspect;
     *                           must not be {@code null}
     * @return {@code true} if the priority value is negative; {@code false} otherwise
     */
    public static boolean isNegativePriorityValue(PsiAnnotation priorityAnnotation) {
        PsiAnnotationMemberValue valueAttr = priorityAnnotation.findAttributeValue("value");
        if (valueAttr == null) {
            return false;
        }

        // Case 1: literal negative integer written as -<number> (PsiPrefixExpression)
        if (valueAttr instanceof PsiPrefixExpression prefix) {
            IElementType op = prefix.getOperationSign().getTokenType();
            if (JavaTokenType.MINUS.equals(op) && prefix.getOperand() instanceof PsiLiteralExpression literal) {
                return literal.getValue() instanceof Integer;
            }
        }

        // Case 2: fall back to text-based parsing (covers numeric constant expressions)
        try {
            return Integer.parseInt(valueAttr.getText()) < 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Converts a list of fully-qualified annotation names into a comma-separated
     * string of simple names with the given prefix, removing duplicates.
     *
     * @param fqAnnotationNames list of fully-qualified annotation names
     * @param prefix            prefix to prepend to each simple name (e.g. {@code "@"})
     * @return comma-separated display string, e.g. {@code "@AfterBegin, @BeforeCompletion"}
     */
    public static String getSimpleAnnotationNames(List<String> fqAnnotationNames, String prefix) {
        return fqAnnotationNames.stream()
                .map(fq -> prefix + JDTUtils.getSimpleName(fq))
                .distinct()
                .collect(Collectors.joining(", "));
    }

    /**
     * Returns {@code true} only when {@code keyType} is definitively an enum type.
     * Concrete class types, upper-bounded wildcards whose bound is an enum, and
     * all other forms (raw, unbound wildcard, lower-bounded wildcard, type variable)
     * return {@code false}.
     */
    public static boolean isEnumKeyType(PsiType keyType) {
        if (keyType instanceof PsiClassType keyClassType) {
            PsiClass keyClass = keyClassType.resolve();
            return keyClass != null && keyClass.isEnum();
        }
        if (keyType instanceof PsiWildcardType wildcardType && wildcardType.isExtends()) {
            PsiType bound = wildcardType.getBound();
            if (bound instanceof PsiClassType boundClassType) {
                PsiClass boundClass = boundClassType.resolve();
                return boundClass != null && boundClass.isEnum();
            }
        }
        return false;
    }
}
