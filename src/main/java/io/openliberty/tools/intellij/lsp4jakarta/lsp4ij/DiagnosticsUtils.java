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
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.PsiTypeUtils;

import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants;

import java.beans.Introspector;
import java.util.ArrayList;
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
     * Returns the declared type of the given field or method member.
     * For a {@link PsiField}, the field's type is returned; for a {@link PsiMethod},
     * the method's return type is returned; for any other kind of member {@code null}
     * is returned.
     *
     * @param member the field or method to inspect; may be {@code null}
     * @return the {@link PsiType} of the member, or {@code null} if it cannot be determined
     */
    public static PsiType getMemberType(PsiJvmModifiersOwner member) {
        if (member instanceof PsiField field) {
            return field.getType();
        } else if (member instanceof PsiMethod method) {
            return method.getReturnType();
        }
        return null;
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

    /**
     * Resolves the {@link PsiClass} referenced by a class-literal attribute of an annotation.
     * For example, given {@code @IdClass(MyKey.class)}, calling this with attribute name
     * {@code "value"} returns the {@code PsiClass} for {@code MyKey}.
     *
     * @param annotation    the annotation to inspect
     * @param attributeName the annotation attribute name (e.g. {@code "value"})
     * @return the resolved {@link PsiClass}, or {@code null} if the attribute is absent,
     *         not a class literal, or cannot be resolved
     */
    public static PsiClass resolveClassFromAnnotationAttribute(PsiAnnotation annotation, String attributeName) {
        PsiAnnotationMemberValue value = annotation.findAttributeValue(attributeName);
        if (!(value instanceof PsiClassObjectAccessExpression)) {
            return null;
        }
        PsiType type = ((PsiClassObjectAccessExpression) value).getOperand().getType();
        if (!(type instanceof PsiClassType)) {
            return null;
        }
        return ((PsiClassType) type).resolve();
    }

    /**
     * Returns the JavaBeans property name for the given method if it qualifies as
     * a property getter: a non-static, no-argument method whose name follows
     * {@code get<Name>} or {@code is<Name>} conventions and whose return type is
     * not {@code void}. Returns {@code null} if any condition is not met.
     *
     * @param method the method to inspect
     * @return the property name (e.g. {@code "name"}), or {@code null} if not a getter
     */
    public static String getterPropertyName(PsiMethod method) {
        if (method.getParameterList().getParametersCount() != 0
                || method.hasModifierProperty(PsiModifier.STATIC)
                || PsiTypeUtils.isVoidReturnType(method)) {
            return null;
        }
        return getterPropertyName(method.getName());
    }

    /**
     * Derives a Java bean property name from a method name using
     * {@link Introspector#decapitalize}.
     * Returns {@code null} if the name does not follow getter conventions
     * ({@code get<Name>} or {@code is<Name>}).
     *
     * @param methodName the method name (e.g. {@code "getName"}, {@code "isActive"})
     * @return the property name (e.g. {@code "name"}, {@code "active"}), or {@code null}
     */
    public static String getterPropertyName(String methodName) {
        if (methodName == null) {
            return null;
        }
        if (methodName.startsWith("get") && methodName.length() > 3) {
            return Introspector.decapitalize(methodName.substring(3));
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            return Introspector.decapitalize(methodName.substring(2));
        }
        return null;
    }

    /**
     * Resolves the element {@link PsiClass} from a {@link PsiType}.
     *
     * <p>For a parameterized collection type ({@code List<Employee>}) the first type
     * argument is resolved. For a plain class type ({@code Department}) the type itself
     * is resolved directly.
     *
     * @param psiType the PSI type to inspect
     * @return the resolved {@link PsiClass}, or {@code null} if it cannot be resolved
     */
    public static PsiClass resolveElementType(PsiType psiType) {
        if (!(psiType instanceof PsiClassType classType)) {
            return null;
        }
        PsiType[] typeArguments = classType.getParameters();
        if (typeArguments.length > 0 && typeArguments[0] instanceof PsiClassType argType) {
            return argType.resolve();
        }
        return classType.resolve();
    }

    /**
     * Extracts the simple class name from a {@link PsiType}.
     *
     * <p>For a plain class type ({@code Department}) returns the class's simple name.
     * For a parameterized collection type ({@code List<Employee>}) returns the simple
     * name of the first type argument.
     *
     * @param psiType the PSI type to inspect
     * @return the simple class name, or {@code null} if it cannot be extracted
     */
    public static String getElementTypeSimpleName(PsiType psiType) {
        PsiClass resolved = resolveElementType(psiType);
        return resolved != null ? resolved.getName() : null;
    }

    /**
     * Returns the fully-qualified name of the first type argument from a parameterised
     * superinterface on the given class.
     *
     * <p>For example, given a class that implements {@code ObserverMethod<AuditEvent>},
     * this method returns {@code "AuditEvent"} when called with
     * {@code interfaceFQName = "jakarta.enterprise.inject.spi.ObserverMethod"}.
     *
     * @param classBinding the class whose {@code implements} list is inspected
     * @param interfaceFQName the fully-qualified name of the superinterface to search for
     * @return the FQN of the first type argument, or {@code "java.lang.Object"} if not found
     */
    public static String resolveTypeArgumentFQName(PsiClass classBinding, String interfaceFQName) {
        for (PsiClassType ifaceType : classBinding.getImplementsListTypes()) {
            PsiClass iface = ifaceType.resolve();
            if (iface != null && interfaceFQName.equals(iface.getQualifiedName())) {
                PsiType[] typeArgs = ifaceType.getParameters();
                if (typeArgs.length > 0) {
                    return typeArgs[0].getCanonicalText();
                }
            }
        }
        return "java.lang.Object";
    }

    /**
     * Returns {@code true} if the given annotation array represents an injection point that uses
     * the {@code @Default} qualifier (explicitly or implicitly).
     *
     * <p>An injection point has the {@code @Default} qualifier when:
     * <ul>
     *   <li>No qualifier annotations are present other than {@code @Default} itself
     *       (the container implicitly adds {@code @Default} and {@code @Any}), OR</li>
     *   <li>{@code @Default} is explicitly declared.</li>
     * </ul>
     * An injection point does <em>not</em> have {@code @Default} when a custom qualifier
     * (i.e. any qualifier other than {@code @Default} or {@code @Any}) is present.
     *
     * @param type        the declaring class (used for import resolution)
     * @param annotations the annotations on the injection point
     * @return {@code true} if the injection point has the {@code @Default} qualifier
     */
    public static boolean hasDefaultQualifier(PsiClass type, PsiAnnotation[] annotations) {
        boolean hasExplicitDefault = false;
        boolean hasCustomQualifier = false;

        for (PsiAnnotation annotation : annotations) {
            String fqn = annotation.getQualifiedName();
            if (fqn == null) {
                continue; // unresolved annotation — skip
            }
            // @Inject is not a qualifier; @Any is a built-in qualifier but not a custom one — skip both
            if (isMatchedJavaElement(type, fqn, ManagedBeanConstants.INJECT_FQ_NAME)
                    || isMatchedJavaElement(type, fqn, ManagedBeanConstants.CDI_ANY_FQ_NAME)) {
                continue;
            }
            if (isMatchedJavaElement(type, fqn, ManagedBeanConstants.CDI_DEFAULT_FQ_NAME)) {
                hasExplicitDefault = true;
            } else {
                // Check if this annotation is a CDI qualifier (meta-annotated with @Qualifier)
                if (AnnotationUtil.hasMetaAnnotation(annotation, type, ManagedBeanConstants.QUALIFIER_FQ_NAME)) {
                    hasCustomQualifier = true;
                }
            }
        }

        // If a custom qualifier is present, @Default is not implicitly added
        return !hasCustomQualifier || hasExplicitDefault;
    }
}
