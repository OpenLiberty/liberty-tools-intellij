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
import com.intellij.psi.util.PsiUtil;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;

import java.util.Arrays;

/**
 * Stateless PSI model-query helpers used by {@link DerivedIdentityDiagnosticsCollector}
 * to resolve composite-key metadata and parent-entity primary key types.
 *
 * <p>All methods are {@code static}; this class is not instantiated.</p>
 */
final class CompositeKeyResolver {

    private CompositeKeyResolver() {
        // utility class — not instantiated
    }

    /**
     * Holds composite-key classification for an entity.
     */
    static final class KeyStrategy {
        /** Resolved {@code @EmbeddedId} class, or {@code null} if none. */
        final PsiClass embeddedIdType;
        /** {@code true} if the entity declares {@code @IdClass}. */
        final boolean hasIdClass;

        KeyStrategy(PsiClass embeddedIdType, boolean hasIdClass) {
            this.embeddedIdType = embeddedIdType;
            this.hasIdClass = hasIdClass;
        }

        boolean isComposite() {
            return embeddedIdType != null || hasIdClass;
        }
    }

    /**
     * Scans the direct members of {@code entityType} to determine whether a
     * composite key is declared and to resolve the {@code @EmbeddedId} class.
     */
    static KeyStrategy resolveKeyStrategy(PsiClass entityType) {
        // Check @EmbeddedId on fields
        PsiClass embeddedIdType = Arrays.stream(entityType.getFields())
                .filter(field -> AnnotationUtils.hasAnnotation(field, PersistenceConstants.EMBEDDEDID))
                .findFirst()
                .map(field -> PsiUtil.resolveClassInClassTypeOnly(field.getType()))
                .orElse(null);

        // Check @EmbeddedId on property getters (if not found on fields)
        if (embeddedIdType == null) {
            embeddedIdType = Arrays.stream(entityType.getMethods())
                    .filter(method -> AnnotationUtils.hasAnnotation(method, PersistenceConstants.EMBEDDEDID))
                    .findFirst()
                    .map(method -> PsiUtil.resolveClassInClassTypeOnly(method.getReturnType()))
                    .orElse(null);
        }

        // Check for @IdClass at the type level
        boolean hasIdClass = AnnotationUtils.hasAnnotation(entityType, PersistenceConstants.IDCLASS);

        return new KeyStrategy(embeddedIdType, hasIdClass);
    }

    /** Holds the resolved parent entity and the FQ type name of its primary key. */
    static final class ParentPk {
        final PsiClass entity;
        final String pkFqType;

        ParentPk(PsiClass entity, String pkFqType) {
            this.entity = entity;
            this.pkFqType = pkFqType;
        }
    }

    /**
     * Resolves the parent entity referenced by the relationship {@code member}
     * and the FQ type of its primary key. Returns {@code null} if the parent
     * cannot be resolved (library type, unresolvable FQN, or no primary key).
     */
    static ParentPk resolveParentPk(PsiJvmModifiersOwner member) {
        PsiClass parentEntity = PsiUtil.resolveClassInClassTypeOnly(PsiUtil.getTypeByPsiElement(member));
        if (parentEntity == null) {
            return null;
        }
        String pkFqType = resolvePkType(parentEntity);
        if (pkFqType == null) {
            return null;
        }
        return new ParentPk(parentEntity, pkFqType);
    }

    /**
     * Returns the FQ type name of the primary key of {@code entityType}:
     * the {@code @IdClass} class value, else the {@code @EmbeddedId} field type,
     * else the type of the simple {@code @Id} field or property.
     * Returns {@code null} if none can be determined.
     */
    static String resolvePkType(PsiClass entityType) {
        // Check @IdClass on the type
        PsiAnnotation idClassAnn = AnnotationUtils.getAnnotation(entityType, PersistenceConstants.IDCLASS);
        if (idClassAnn != null) {
            PsiAnnotationMemberValue value = AnnotationUtils.getAnnotationMemberValueExpression(idClassAnn, "value");
            if (value instanceof PsiClassObjectAccessExpression classObj) {
                PsiType type = classObj.getOperand().getType();
                return type.getCanonicalText();
            }
        }
        // Check @Id and @EmbeddedId on fields, then on methods
        return Arrays.stream(entityType.getFields())
                .filter(field -> AnnotationUtils.hasAnyAnnotation(field, PersistenceConstants.ID, PersistenceConstants.EMBEDDEDID))
                .findFirst()
                .map(field -> field.getType().getCanonicalText())
                .or(() -> Arrays.stream(entityType.getMethods())
                        .filter(method -> AnnotationUtils.hasAnyAnnotation(method, PersistenceConstants.ID, PersistenceConstants.EMBEDDEDID))
                        .findFirst()
                        .map(PsiMethod::getReturnType)
                        .map(PsiType::getCanonicalText))
                .orElse(null);
    }

    /**
     * Returns the FQ type of attribute {@code attrName} in {@code embeddableType},
     * checking fields first then property getters.
     * Returns {@code null} if not found.
     */
    static String resolveAttributeType(PsiClass embeddableType, String attrName) {
        // Check fields directly
        PsiField field = embeddableType.findFieldByName(attrName, false);
        if (field != null) {
            return field.getType().getCanonicalText();
        }
        // Fall back to property getter: getXxx()
        String getterName = "get" + Character.toUpperCase(attrName.charAt(0)) + attrName.substring(1);
        return Arrays.stream(embeddableType.getMethods())
                .filter(method -> getterName.equals(method.getName())
                        && method.getParameterList().getParametersCount() == 0)
                .findFirst()
                .map(PsiMethod::getReturnType)
                .map(PsiType::getCanonicalText)
                .orElse(null);
    }

    /**
     * Returns {@code true} if {@code typeA} and {@code typeB} are compatible
     * for JPA identity purposes: identical FQ names, or a primitive/wrapper
     * boxing pair.
     */
    static boolean typesCompatible(String typeA, String typeB) {
        if (typeA == null || typeB == null) {
            return true;
        }
        return typeA.equals(typeB)
                || typeB.equals(PersistenceConstants.PRIMITIVE_TO_WRAPPER.get(typeA))
                || typeA.equals(PersistenceConstants.PRIMITIVE_TO_WRAPPER.get(typeB));
    }
}
