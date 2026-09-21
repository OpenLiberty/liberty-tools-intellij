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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence;

import java.beans.Introspector;
import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.PositionUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class that encapsulates all {@code @IdClass} validation logic for
 * Jakarta Persistence entity diagnostics.
 *
 * <p>Implements the rules from Jakarta Persistence 3.0 spec §2.4 and §2.4.1.1:
 * <ul>
 *   <li>Every {@code @Id} field or property in the entity must have a corresponding
 *       member in the id class ({@code IdClassMemberMissingInKeyClass}).</li>
 *   <li>Corresponding members must have matching types. For relationship
 *       {@code @Id} attributes ({@code @ManyToOne} / {@code @OneToOne}), the
 *       expected key-class type is the parent entity's PK type, not the
 *       relationship type itself (spec §2.4.1.1, third bullet).</li>
 * </ul>
 * </p>
 */
class IdClassService {

    /**
     * Validates that the members of an {@code @IdClass} key class correspond to the
     * {@code @Id} fields or properties of the entity, and that their names and types
     * match per Jakarta Persistence spec §2.4 and §2.4.1.1.
     *
     * @param entityType        the {@code @Entity} class under inspection
     * @param idClassAnnotation the {@code @IdClass} annotation on the entity
     * @param idMembers         the members of the entity annotated with {@code @Id}
     * @param diagnostics       the list to add diagnostics to
     */
    void validate(PsiClass entityType, PsiJavaFile unit,
                  PsiAnnotation idClassAnnotation,
                  List<PsiJvmModifiersOwner> idMembers,
                  List<Diagnostic> diagnostics) {
        PsiClass keyClass = resolveIdClass(idClassAnnotation);
        if (keyClass == null || idMembers.isEmpty()) {
            return;
        }

        // Infer access type: if any @Id is on a field → field-based; otherwise → property-based.
        boolean fieldAccess = idMembers.stream().anyMatch(m -> m instanceof PsiField);

        // Build name→expectedType map for entity @Id members.
        // For @ManyToOne / @OneToOne @Id members the expected type is the parent entity's
        // PK type, not the relationship type (spec §2.4.1.1, third bullet).
        Map<String, String> entityIdMap = new HashMap<>();
        for (PsiJvmModifiersOwner member : idMembers) {
            String name = fieldAccess
                    ? ((PsiNamedElement) member).getName()
                    : getterPropertyName(((PsiNamedElement) member).getName());
            if (name != null) {
                entityIdMap.put(name, resolveExpectedKeyClassType(member));
            }
        }

        // Build name→type map for key class members using the same access mode.
        Map<String, String> keyClassMap = new HashMap<>();
        if (fieldAccess) {
            for (PsiField field : keyClass.getFields()) {
                if (!field.hasModifierProperty(PsiModifier.STATIC)
                        && !field.hasModifierProperty(PsiModifier.TRANSIENT)) {
                    keyClassMap.put(field.getName(), field.getType().getCanonicalText());
                }
            }
        } else {
            for (PsiMethod method : keyClass.getMethods()) {
                String propName = getterPropertyName(method.getName());
                if (propName != null && method.getParameterList().getParametersCount() == 0) {
                    PsiType returnType = method.getReturnType();
                    keyClassMap.put(propName, returnType != null ? returnType.getCanonicalText() : null);
                }
            }
        }

        // Check every entity @Id member against the key class map.
        for (PsiJvmModifiersOwner idMember : idMembers) {
            String memberName = fieldAccess
                    ? ((PsiNamedElement) idMember).getName()
                    : getterPropertyName(((PsiNamedElement) idMember).getName());
            if (memberName == null) {
                continue;
            }
            if (!keyClassMap.containsKey(memberName)) {
                diagnostics.add(diagnostic((PsiElement) idMember,
                        Messages.getMessage("IdClassMemberMissingInKeyClass", memberName),
                        PersistenceConstants.DIAGNOSTIC_CODE_IDCLASS_MEMBER_MISSING_IN_KEY_CLASS));
            } else {
                String entityTypeName = entityIdMap.get(memberName);
                String keyTypeName = keyClassMap.get(memberName);
                if (entityTypeName != null && keyTypeName != null && !entityTypeName.equals(keyTypeName)) {
                    diagnostics.add(diagnostic((PsiElement) idMember,
                            Messages.getMessage("IdClassMemberTypeMismatch", memberName, entityTypeName, keyTypeName),
                            PersistenceConstants.DIAGNOSTIC_CODE_IDCLASS_MEMBER_TYPE_MISMATCH));
                }
            }
        }
    }

    /**
     * Resolves the type that the corresponding key-class member should have for a
     * given entity {@code @Id} member, following Jakarta Persistence spec §2.4.1.1.
     *
     * <ul>
     *   <li>For a basic {@code @Id} field or property this is the declared type.</li>
     *   <li>For a relationship {@code @Id} ({@code @ManyToOne} / {@code @OneToOne}),
     *       the key-class must hold the parent entity's PK type (simple PK) or
     *       {@code @IdClass} type (composite PK).</li>
     * </ul>
     *
     * @param member the entity {@code @Id} field or method
     * @return the expected key-class type name, or the raw member type if unresolvable
     */
    private String resolveExpectedKeyClassType(PsiJvmModifiersOwner member) {
        String rawType = resolvedTypeName(member);

        // Only relationship @Id fields need special handling.
        boolean isRelationship = member.hasAnnotation(PersistenceConstants.MANYTOONE)
                || member.hasAnnotation(PersistenceConstants.ONETOONE);
        if (!isRelationship || rawType == null) {
            return rawType;
        }

        // Resolve the parent entity PsiClass from the field/method type.
        PsiType memberType = member instanceof PsiField
                ? ((PsiField) member).getType()
                : ((PsiMethod) member).getReturnType();
        if (!(memberType instanceof PsiClassType parentClassType)) {
            return rawType;
        }
        PsiClass parentClass = parentClassType.resolve();
        if (parentClass == null) {
            return rawType;
        }

        // If the parent has @IdClass (composite PK), return that class's type.
        PsiAnnotation idClassAnn = parentClass.getAnnotation(PersistenceConstants.IDCLASS);
        if (idClassAnn != null) {
            PsiClass pkClass = resolveIdClass(idClassAnn);
            if (pkClass != null) {
                return pkClass.getQualifiedName();
            }
        }

        // Simple parent PK: return the type of the parent's @Id field or getter.
        for (PsiField field : parentClass.getFields()) {
            if (field.getAnnotation(PersistenceConstants.ID) != null) {
                return field.getType().getCanonicalText();
            }
        }
        for (PsiMethod method : parentClass.getMethods()) {
            if (method.getAnnotation(PersistenceConstants.ID) != null) {
                PsiType rt = method.getReturnType();
                return rt != null ? rt.getCanonicalText() : rawType;
            }
        }

        return rawType;
    }

    /**
     * Resolves the {@link PsiClass} referenced by an {@code @IdClass} annotation's
     * {@code value} attribute (a class literal such as {@code EmployeePK.class}).
     *
     * @param idClassAnnotation the {@code @IdClass} annotation
     * @return the resolved key-class {@link PsiClass}, or {@code null} if it cannot be resolved
     */
    private PsiClass resolveIdClass(PsiAnnotation idClassAnnotation) {
        PsiAnnotationMemberValue value = idClassAnnotation.findAttributeValue("value");
        if (value instanceof PsiClassObjectAccessExpression classExpr
                && classExpr.getOperand().getType() instanceof PsiClassType classType) {
            return classType.resolve();
        }
        return null;
    }

    /**
     * Returns the canonical type name of a field or getter method.
     *
     * @param member the field or method to inspect
     * @return canonical type name, or {@code null}
     */
    private String resolvedTypeName(PsiJvmModifiersOwner member) {
        if (member instanceof PsiField field) {
            return field.getType().getCanonicalText();
        }
        if (member instanceof PsiMethod method) {
            PsiType rt = method.getReturnType();
            return rt != null ? rt.getCanonicalText() : null;
        }
        return null;
    }

    /**
     * Derives a Java bean property name from a getter method name using
     * {@link Introspector#decapitalize}, consistent with the approach in
     * {@link PersistenceMapKeyDiagnosticsCollector}.
     * Returns {@code null} if the method name does not follow getter conventions.
     *
     * @param methodName the getter method name (e.g. {@code "getName"}, {@code "isActive"})
     * @return the property name (e.g. {@code "name"}, {@code "active"}), or {@code null}
     */
    private String getterPropertyName(String methodName) {
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
     * Builds an Error-severity diagnostic with the persistence source tag.
     *
     * @param element the PSI element to compute the range from
     * @param message the diagnostic message
     * @param code    the diagnostic code string
     * @return the constructed {@link Diagnostic}
     */
    private Diagnostic diagnostic(PsiElement element, String message, String code) {
        Range range = PositionUtils.toNameRange(element);
        Diagnostic d = new Diagnostic(range, message);
        d.setSource(PersistenceConstants.DIAGNOSTIC_SOURCE);
        d.setCode(code);
        d.setSeverity(DiagnosticSeverity.Error);
        return d;
    }
}
