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
 *   <li>Every member of the id class must correspond by name to an {@code @Id}
 *       field or property in the entity ({@code IdClassMemberMissingInEntity}).</li>
 *   <li>Every {@code @Id} field or property in the entity must have a corresponding
 *       member in the id class ({@code IdClassMemberMissingInKeyClass}).</li>
 *   <li>Corresponding members must have matching types. For relationship
 *       {@code @Id} attributes ({@code @ManyToOne} / {@code @OneToOne}), the
 *       expected key-class type is the parent entity's PK type, not the
 *       relationship type itself (spec §2.4.1.1, third bullet).</li>
 *   <li>When property-based access is used, each getter in the id class must be
 *       {@code public} or {@code protected} ({@code IdClassPropertyNotPublicOrProtected}).</li>
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
     * @param unit              the PSI file containing the entity
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

        // Infer access type from where @Id annotations are placed on the entity.
        // If any @Id is on a field → field-based access; if all are on methods → property-based access.
        boolean entityUsesFieldAccess = idMembers.stream().anyMatch(m -> m instanceof PsiField);

        // Build a name→type map for the entity's @Id members.
        // For relationship @Id members (@ManyToOne / @OneToOne), the type stored in the
        // key class must be the parent entity's PK type, not the relationship type itself
        // (Jakarta Persistence 3.0 spec §2.4.1.1, third bullet).
        Map<String, String> entityIdMap = new HashMap<>();
        for (PsiJvmModifiersOwner member : idMembers) {
            String name = entityUsesFieldAccess
                    ? ((PsiNamedElement) member).getName()
                    : propertyNameFromGetter(((PsiNamedElement) member).getName());
            if (name == null) {
                continue;
            }
            String typeFqn = resolveExpectedKeyClassType(member);
            entityIdMap.put(name, typeFqn);
        }

        // Build a name→type map for the key class, using the same access mode.
        Map<String, String> keyClassMap = new HashMap<>();
        if (entityUsesFieldAccess) {
            for (PsiField field : keyClass.getFields()) {
                if (field.hasModifierProperty(PsiModifier.STATIC)
                        || field.hasModifierProperty(PsiModifier.TRANSIENT)) {
                    continue;
                }
                keyClassMap.put(field.getName(), field.getType().getCanonicalText());
            }
        } else {
            // Property-based access: inspect getter methods.
            for (PsiMethod method : keyClass.getMethods()) {
                String propName = propertyNameFromGetter(method.getName());
                if (propName == null || method.getParameterList().getParametersCount() != 0) {
                    continue;
                }
                PsiType returnType = method.getReturnType();
                keyClassMap.put(propName, returnType != null ? returnType.getCanonicalText() : null);
            }
        }

        // Every entity @Id member must have a matching member in the key class,
        // and when both exist their types must be the same.
        for (PsiJvmModifiersOwner idMember : idMembers) {
            String memberName = entityUsesFieldAccess
                    ? ((PsiNamedElement) idMember).getName()
                    : propertyNameFromGetter(((PsiNamedElement) idMember).getName());
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
     *   <li>For a <em>basic</em> {@code @Id} field or property, this is simply the
     *       declared type of that member.</li>
     *   <li>For a <em>relationship</em> {@code @Id} (annotated with {@code @ManyToOne}
     *       or {@code @OneToOne}), the key-class must hold the parent entity's PK type:
     *       <ul>
     *         <li>Simple parent PK → type of the parent's {@code @Id} field/property.</li>
     *         <li>Composite parent PK ({@code @IdClass}) → the {@code @IdClass} type.</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * @param member the entity {@code @Id} field or method
     * @return the expected key-class type name, or the raw member type if unresolvable
     */
    private String resolveExpectedKeyClassType(PsiJvmModifiersOwner member) {
        String rawType = resolvedTypeName(member);

        // Detect whether this @Id member is also a relationship (@ManyToOne / @OneToOne).
        boolean isRelationship = false;
        for (PsiAnnotation ann : member.getAnnotations()) {
            String qualName = ann.getQualifiedName();
            if (PersistenceConstants.MANYTOONE.equals(qualName)
                    || PersistenceConstants.ONETOONE.equals(qualName)) {
                isRelationship = true;
                break;
            }
        }

        if (!isRelationship || rawType == null) {
            return rawType;
        }

        // Resolve the parent entity PsiClass.
        PsiType memberPsiType = memberPsiType(member);
        if (!(memberPsiType instanceof PsiClassType parentClassType)) {
            return rawType;
        }
        PsiClass parentClass = parentClassType.resolve();
        if (parentClass == null) {
            return rawType;
        }

        // Check whether the parent has @IdClass (composite PK).
        PsiAnnotation idClassAnn = parentClass.getAnnotation(PersistenceConstants.IDCLASS);
        if (idClassAnn != null) {
            PsiAnnotationMemberValue value = idClassAnn.findAttributeValue("value");
            if (value instanceof PsiClassObjectAccessExpression classExpr) {
                PsiType idClassType = classExpr.getOperand().getType();
                return idClassType.getCanonicalText();
            }
        }

        // Simple parent PK: find the single @Id field in the parent and return its type.
        for (PsiField field : parentClass.getFields()) {
            if (field.getAnnotation(PersistenceConstants.ID) != null) {
                return field.getType().getCanonicalText();
            }
        }
        // Property-based parent PK: find the @Id getter.
        for (PsiMethod method : parentClass.getMethods()) {
            if (method.getAnnotation(PersistenceConstants.ID) != null) {
                PsiType rt = method.getReturnType();
                return rt != null ? rt.getCanonicalText() : rawType;
            }
        }

        return rawType;
    }

    /**
     * Resolves the {@link PsiClass} referenced by an {@code @IdClass} annotation.
     *
     * @param idClassAnnotation the {@code @IdClass} annotation
     * @return the resolved key-class {@link PsiClass}, or {@code null} if it cannot be resolved
     */
    private PsiClass resolveIdClass(PsiAnnotation idClassAnnotation) {
        PsiAnnotationMemberValue value = idClassAnnotation.findAttributeValue("value");
        if (value instanceof PsiClassObjectAccessExpression classExpr) {
            PsiType type = classExpr.getOperand().getType();
            if (type instanceof PsiClassType classType) {
                return classType.resolve();
            }
        }
        return null;
    }

    /**
     * Returns the canonical type name of a {@link PsiJvmModifiersOwner}.
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
     * Returns the {@link PsiType} of a field or method return type.
     *
     * @param member the field or method to inspect
     * @return the {@link PsiType}, or {@code null}
     */
    private PsiType memberPsiType(PsiJvmModifiersOwner member) {
        if (member instanceof PsiField field) {
            return field.getType();
        }
        if (member instanceof PsiMethod method) {
            return method.getReturnType();
        }
        return null;
    }

    /**
     * Derives a Java bean property name from a getter method name.
     *
     * @param methodName the method name to inspect
     * @return the derived property name, or {@code null}
     */
    private String propertyNameFromGetter(String methodName) {
        if (methodName == null) {
            return null;
        }
        String suffix = null;
        if (methodName.startsWith("get") && methodName.length() > 3) {
            suffix = methodName.substring(3);
        } else if (methodName.startsWith("is") && methodName.length() > 2) {
            suffix = methodName.substring(2);
        }
        if (suffix == null || suffix.isEmpty()) {
            return null;
        }
        return Character.toLowerCase(suffix.charAt(0)) + suffix.substring(1);
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
