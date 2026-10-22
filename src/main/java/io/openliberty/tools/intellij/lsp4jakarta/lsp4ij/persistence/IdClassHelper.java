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

import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.DiagnosticsUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.util.PsiUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class that encapsulates all {@code @IdClass} validation logic for
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
class IdClassHelper {
    
    /**
     * A finding produced by {@link #validate} — carries the PSI element to highlight,
     * the human-readable message, and the diagnostic code. Diagnostic creation is
     * intentionally left to the caller so that it can use its own
     * {@code AbstractDiagnosticsCollector.createDiagnostic} infrastructure.
     */
    record Finding(PsiElement element, String message, String code) {}

    /**
     * Validates that the members of an {@code @IdClass} key class correspond to the
     * {@code @Id} fields or properties of the entity, and that their names and types
     * match per Jakarta Persistence spec §2.4 and §2.4.1.1.
     *
     * @param entityType        the {@code @Entity} class under inspection
     * @param idClassAnnotation the {@code @IdClass} annotation on the entity
     * @param idMembers         the members of the entity annotated with {@code @Id}
     * @return list of findings; empty when validation passes
     */
    List<Finding> validate(PsiClass entityType,
                           PsiAnnotation idClassAnnotation,
                           List<PsiJvmModifiersOwner> idMembers) {
        List<Finding> findings = new ArrayList<>();
        PsiClass keyClass = resolveIdClass(idClassAnnotation);
        if (keyClass == null || idMembers.isEmpty()) {
            return findings;
        }

        // Infer access type: if any @Id is on a field → field-based; otherwise → property-based.
        boolean fieldAccess = idMembers.stream().anyMatch(m -> m instanceof PsiField);

        // Build name→member and name→expectedType maps for entity @Id members in one pass.
        // For @ManyToOne / @OneToOne @Id members the expected type is the related entity's
        // PK type, not the relationship type (spec §2.4.1.1, third bullet).
        Map<String, PsiJvmModifiersOwner> entityIdMembers = new HashMap<>();
        Map<String, String> entityIdMap = new HashMap<>();
        for (PsiJvmModifiersOwner member : idMembers) {
            String memberName = fieldAccess
                    ? ((PsiNamedElement) member).getName()
                    : DiagnosticsUtils.getterPropertyName(((PsiNamedElement) member).getName());
            if (memberName != null) {
                entityIdMembers.put(memberName, member);
                entityIdMap.put(memberName, resolveExpectedKeyClassType(member));
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
                String propertyName = DiagnosticsUtils.getterPropertyName(method);
                if (propertyName != null) {
                    PsiType returnType = method.getReturnType();
                    keyClassMap.put(propertyName, returnType != null ? returnType.getCanonicalText() : null);
                }
            }
        }

        // Iterate entityIdMap directly — no need to re-derive memberName from idMembers again.
        for (Map.Entry<String, String> entry : entityIdMap.entrySet()) {
            String memberName = entry.getKey();
            PsiElement idMember = (PsiElement) entityIdMembers.get(memberName);
            if (!keyClassMap.containsKey(memberName)) {
                findings.add(new Finding(idMember,
                        Messages.getMessage("IdClassMemberMissingInKeyClass", memberName),
                        PersistenceConstants.DIAGNOSTIC_CODE_IDCLASS_MEMBER_MISSING_IN_KEY_CLASS));
            } else {
                String entityTypeName = entry.getValue();
                String keyTypeName = keyClassMap.get(memberName);
                if (entityTypeName != null && keyTypeName != null && !entityTypeName.equals(keyTypeName)) {
                    findings.add(new Finding(idMember,
                            Messages.getMessage("IdClassMemberTypeMismatch", memberName, entityTypeName, keyTypeName),
                            PersistenceConstants.DIAGNOSTIC_CODE_IDCLASS_MEMBER_TYPE_MISMATCH));
                }
            }
        }
        return findings;
    }

    /**
     * Resolves the type that the corresponding key-class member should have for a
     * given entity {@code @Id} member, following Jakarta Persistence spec §2.4.1.1.
     *
     * <ul>
     *   <li>For a basic {@code @Id} field or property this is the declared type.</li>
     *   <li>For a relationship {@code @Id} ({@code @ManyToOne} / {@code @OneToOne}),
     *       spec §2.4.1.1 (third bullet) requires the key-class to hold the <em>related</em>
     *       entity's PK type — not the entity type itself:
     *     <ul>
     *       <li>If the related entity has a simple PK, the key-class member type is that
     *           {@code @Id} field/property type.</li>
     *       <li>If the related entity itself has a composite PK ({@code @IdClass}), the
     *           key-class member type is that {@code @IdClass} type.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param member the entity {@code @Id} field or method
     * @return the expected key-class type name, or the {@code @Id} field type if unresolvable
     */
    private String resolveExpectedKeyClassType(PsiJvmModifiersOwner member) {
        String idFieldType = PsiUtils.resolvedTypeName(member);

        // Only relationship @Id fields need special handling.
        boolean isRelationship = member.hasAnnotation(PersistenceConstants.MANY_TO_ONE)
                || member.hasAnnotation(PersistenceConstants.ONE_TO_ONE);
        if (!isRelationship || idFieldType == null) {
            return idFieldType;
        }

        // Resolve the related entity PsiClass from the field/method type.
        PsiType memberType = member instanceof PsiField
                ? ((PsiField) member).getType()
                : ((PsiMethod) member).getReturnType();
        if (!(memberType instanceof PsiClassType idClassType)) {
            return idFieldType;
        }
        PsiClass idType = idClassType.resolve();
        if (idType == null) {
            return idFieldType;
        }

        // Spec §2.4.1.1 third bullet: if the *related* entity itself has a composite PK
        // (@IdClass), the key-class member must use that @IdClass type, not the entity type.
        PsiAnnotation idClassAnnotation = idType.getAnnotation(PersistenceConstants.IDCLASS);
        if (idClassAnnotation != null) {
            PsiClass pkClass = resolveIdClass(idClassAnnotation);
            if (pkClass != null) {
                return pkClass.getQualifiedName();
            }
        }

        // Simple PK on the related entity: return its @Id field/getter type directly.
        for (PsiField field : idType.getFields()) {
            if (field.getAnnotation(PersistenceConstants.ID) != null) {
                return field.getType().getCanonicalText();
            }
        }
        for (PsiMethod method : idType.getMethods()) {
            if (method.getAnnotation(PersistenceConstants.ID) != null) {
                PsiType rt = method.getReturnType();
                return rt != null ? rt.getCanonicalText() : idFieldType;
            }
        }

        return idFieldType;
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
}
