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

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi;

import com.intellij.psi.*;
import com.intellij.psi.impl.PsiClassImplUtil;
import com.intellij.psi.util.InheritanceUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.List;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.cdi.ManagedBeanConstants.*;

/**
 * CDI diagnostics collector that validates the use of the {@code @Typed} annotation.
 *
 * <p>Per CDI 3.0 specification section 2.2.2:</p>
 * <blockquote>
 * If a bean class or producer method or field specifies a {@code @Typed} annotation, and the
 * {@code value} member specifies a class which does not correspond to a type in the unrestricted
 * set of bean types of a bean, the container automatically detects the problem and treats it as
 * a definition error.
 * </blockquote>
 *
 * <p>The unrestricted set of bean types for a managed bean is: the bean class itself, every
 * superclass (excluding {@code Object}), and every interface implemented directly or indirectly
 * by the class. For a producer method the set is derived from the return type; for a producer
 * field it is derived from the field type.</p>
 *
 * @see <a href="https://jakarta.ee/specifications/cdi/3.0/jakarta-cdi-spec-3.0#restricting_bean_types">CDI 3.0 §2.2.2</a>
 */
public class CdiTypedAnnotationDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public CdiTypedAnnotationDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit == null) {
            return;
        }

        for (PsiClass type : unit.getClasses()) {
            // Check @Typed on the bean class itself
            PsiAnnotation typedOnClass = AnnotationUtils.getAnnotation(type, TYPED_FQ_NAME);
            if (typedOnClass != null) {
                checkTypedAnnotation(type, type, typedOnClass, unit, diagnostics);
            }

            // Check @Typed on producer fields
            for (PsiField field : type.getFields()) {
                PsiAnnotation typedOnField = AnnotationUtils.getAnnotation(field, TYPED_FQ_NAME);
                if (typedOnField != null) {
                    PsiClass fieldType = resolveClassType(field.getType());
                    checkTypedAnnotation(type, fieldType, typedOnField, unit, diagnostics);
                }
            }

            // Check @Typed on producer methods
            for (PsiMethod method : type.getMethods()) {
                PsiAnnotation typedOnMethod = AnnotationUtils.getAnnotation(method, TYPED_FQ_NAME);
                if (typedOnMethod != null) {
                    PsiType returnType = method.getReturnType();
                    PsiClass returnClass = returnType != null ? resolveClassType(returnType) : null;
                    checkTypedAnnotation(type, returnClass, typedOnMethod, unit, diagnostics);
                }
            }
        }
    }

    /**
     * Validates a {@code @Typed} annotation against the unrestricted bean types of {@code beanClass}.
     * Each class name listed in the annotation's {@code value} member that is not in the supertype
     * hierarchy of {@code beanClass} produces a diagnostic.
     *
     * @param declaringClass the class that owns the annotated element (used for diagnostic placement)
     * @param beanClass      the class whose unrestricted bean types are checked; may be {@code null}
     *                       if the type could not be resolved
     * @param typedAnnotation the {@code @Typed} annotation
     * @param unit           the compilation unit
     * @param diagnostics    list to add diagnostics to
     */
    private void checkTypedAnnotation(PsiClass declaringClass, PsiClass beanClass,
                                      PsiAnnotation typedAnnotation, PsiJavaFile unit,
                                      List<Diagnostic> diagnostics) {
        if (beanClass == null) {
            return;
        }

        List<String> typedValues = getTypedAnnotationValues(typedAnnotation);
        if (typedValues.isEmpty()) {
            return;
        }

        for (String typedValue : typedValues) {
            if (!isInUnrestrictedBeanTypes(beanClass, typedValue)) {
                diagnostics.add(createDiagnostic(typedAnnotation, unit,
                        Messages.getMessage("InvalidTypedAnnotationNonMatchingBeanType", typedValue),
                        DIAGNOSTIC_CODE_TYPED_NON_MATCHING_BEAN_TYPE, null,
                        DiagnosticSeverity.Error));
            }
        }
    }

    /**
     * Returns {@code true} if {@code typeName} (simple or fully qualified) matches the bean class
     * itself, any of its superclasses (excluding {@code Object}), or any directly or indirectly
     * implemented interface.
     *
     * <p>Uses {@link InheritanceUtil#isInheritor} for transitive hierarchy checks, the same PSI
     * utility used by {@link AbstractDiagnosticsCollector#doesImplementInterfaces}.</p>
     */
    private boolean isInUnrestrictedBeanTypes(PsiClass beanClass, String typeName) {
        // Check the bean class itself by simple name or FQ name
        String beanFQName = beanClass.getQualifiedName();
        String beanSimpleName = beanClass.getName();
        if (typeName.equals(beanFQName) || typeName.equals(beanSimpleName)) {
            return true;
        }

        // Check all superclasses and interfaces (transitively) via InheritanceUtil.
        // isInheritor handles both classes and interfaces including transitive ones.
        // We pass checkDeep=true (default overload) to walk the full hierarchy.
        if (InheritanceUtil.isInheritor(beanClass, typeName)) {
            return true;
        }

        // Also check by simple name against the full supertype hierarchy.
        // PsiClassImplUtil.getAllSuperClassesRecursively includes both superclasses and interfaces.
        for (PsiClass superType : PsiClassImplUtil.getAllSuperClassesRecursively(beanClass)) {
            String superFQName = superType.getQualifiedName();
            if (superFQName == null) continue;
            if ("java.lang.Object".equals(superFQName)) continue;
            String superSimpleName = superType.getName();
            if (typeName.equals(superFQName) || typeName.equals(superSimpleName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extracts the class names listed in the {@code value} member of a {@code @Typed} annotation.
     * Handles both single-class ({@code @Typed(Foo.class)}) and array-of-classes
     * ({@code @Typed({Foo.class, Bar.class})}) forms.
     */
    private List<String> getTypedAnnotationValues(PsiAnnotation typedAnnotation) {
        java.util.ArrayList<String> values = new java.util.ArrayList<>();
        PsiAnnotationMemberValue valueMember = typedAnnotation.findAttributeValue("value");
        if (valueMember == null) {
            return values;
        }
        if (valueMember instanceof PsiArrayInitializerMemberValue) {
            for (PsiAnnotationMemberValue element : ((PsiArrayInitializerMemberValue) valueMember).getInitializers()) {
                String name = extractClassName(element);
                if (name != null) {
                    values.add(name);
                }
            }
        } else {
            String name = extractClassName(valueMember);
            if (name != null) {
                values.add(name);
            }
        }
        return values;
    }

    /**
     * Extracts the class simple name from a {@code Foo.class} annotation value expression.
     * Returns {@code null} if extraction fails.
     */
    private String extractClassName(PsiAnnotationMemberValue element) {
        if (element instanceof PsiClassObjectAccessExpression) {
            PsiType type = ((PsiClassObjectAccessExpression) element).getOperand().getType();
            if (type instanceof PsiClassType) {
                PsiClass resolved = ((PsiClassType) type).resolve();
                if (resolved != null) {
                    return resolved.getName();
                }
            }
            // Fallback: use the type's presentation text stripped of generics
            String text = type.getPresentableText();
            int idx = text.indexOf('<');
            return idx >= 0 ? text.substring(0, idx) : text;
        }
        return null;
    }

    /**
     * Resolves a {@link PsiType} to a {@link PsiClass}, erasing generic parameters.
     * Returns {@code null} if the type cannot be resolved to a class.
     */
    private PsiClass resolveClassType(PsiType type) {
        PsiType erased = type instanceof PsiClassType ? ((PsiClassType) type).rawType() : type;
        if (erased instanceof PsiClassType) {
            return ((PsiClassType) erased).resolve();
        }
        return null;
    }
}
