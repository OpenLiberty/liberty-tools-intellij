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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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
        if (psiType instanceof PsiClassType classType) {
            PsiType[] typeArguments = classType.getParameters();
            if (typeArguments.length > 0 && typeArguments[0] instanceof PsiClassType argType) {
                // Collection type: return the simple name of the first type argument.
                PsiClass argClass = argType.resolve();
                return argClass != null ? argClass.getName() : null;
            }
            // Plain class type.
            PsiClass resolved = classType.resolve();
            return resolved != null ? resolved.getName() : null;
        }
        return null;
    }

    /**
     * Visits every source {@link PsiClass} in the module that owns {@code context}
     * and passes each one to {@code visitor}.
     *
     * <p>Performs a direct filesystem traversal of the module's non-test source roots
     * (via {@link ModuleRootManager}) rather than relying on IntelliJ's annotation
     * index, so it is safe to call during tests before the index is fully built.
     *
     * <p>Example — count {@code @NamedEntityGraph} names module-wide:
     * <pre>{@code
     * Map<String, Integer> counts = new HashMap<>();
     * DiagnosticsUtils.scanSourceClasses(anyClassInModule, psiClass -> {
     *     PsiAnnotation ann = psiClass.getAnnotation(NAMED_ENTITY_GRAPH);
     *     if (ann != null) {
     *         String name = AnnotationUtils.getAnnotationMemberValue(ann, "name");
     *         if (name != null) counts.merge(name, 1, Integer::sum);
     *     }
     * });
     * }</pre>
     *
     * @param context any {@link PsiClass} from the module (provides module and project)
     * @param visitor called once for every source class found
     */
    public static void scanSourceClasses(PsiClass context, Consumer<PsiClass> visitor) {
        Module module = ModuleUtilCore.findModuleForPsiElement(context);
        if (module == null) {
            return;
        }
        PsiManager psiManager = PsiManager.getInstance(context.getProject());
        for (VirtualFile sourceRoot : ModuleRootManager.getInstance(module).getSourceRoots(false)) {
            visitClasses(sourceRoot, psiManager, visitor);
        }
    }

    /**
     * Recursively visits all {@code .java} files under {@code directory} and
     * passes every {@link PsiClass} found to {@code visitor}.
     *
     * @param directory  the virtual file directory to traverse
     * @param psiManager the PSI manager used to parse virtual files
     * @param visitor    called for each class found
     */
    private static void visitClasses(VirtualFile directory, PsiManager psiManager,
                                     Consumer<PsiClass> visitor) {
        for (VirtualFile child : directory.getChildren()) {
            if (child.isDirectory()) {
                visitClasses(child, psiManager, visitor);
            } else if ("java".equals(child.getExtension())) {
                PsiFile psiFile = psiManager.findFile(child);
                if (psiFile instanceof PsiJavaFile javaFile) {
                    for (PsiClass psiClass : javaFile.getClasses()) {
                        visitor.accept(psiClass);
                    }
                }
            }
        }
    }
}
