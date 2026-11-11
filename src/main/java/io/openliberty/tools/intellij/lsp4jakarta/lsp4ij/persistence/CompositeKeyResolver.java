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

import java.beans.Introspector;

/**
 * Stateless PSI model-query helpers used by {@link DerivedIdentityDiagnosticsCollector}
 * to resolve composite-key metadata and relational-entity primary key types.
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
        PsiClass embeddedIdType = null;

        // Check @EmbeddedId on fields
        for (PsiField field : entityType.getFields()) {
            if (AnnotationUtils.hasAnnotation(field, PersistenceConstants.EMBEDDEDID)) {
                embeddedIdType = PsiUtil.resolveClassInClassTypeOnly(field.getType());
                break;
            }
        }

        // Check @EmbeddedId on property getters (if not found on fields)
        if (embeddedIdType == null) {
            for (PsiMethod method : entityType.getMethods()) {
                if (AnnotationUtils.hasAnnotation(method, PersistenceConstants.EMBEDDEDID)) {
                    embeddedIdType = PsiUtil.resolveClassInClassTypeOnly(method.getReturnType());
                    break;
                }
            }
        }

        // Check for @IdClass at the type level
        boolean hasIdClass = AnnotationUtils.hasAnnotation(entityType, PersistenceConstants.IDCLASS);

        return new KeyStrategy(embeddedIdType, hasIdClass);
    }

    /** Holds the resolved relational entity and the FQ type name of its primary key. */
    static final class RelationalEntityPk {
        final PsiClass entity;
        final String pkFqType;

        RelationalEntityPk(PsiClass entity, String pkFqType) {
            this.entity = entity;
            this.pkFqType = pkFqType;
        }
    }

    /**
     * Resolves the relational entity referenced by the relationship {@code member}
     * and the FQ type of its primary key. Returns {@code null} if the entity
     * cannot be resolved (library type, unresolvable FQN, or no primary key).
     */
    static RelationalEntityPk resolveRelationalEntityPk(PsiJvmModifiersOwner member) {
        PsiClass relationalEntity = PsiUtil.resolveClassInClassTypeOnly(PsiUtil.getTypeByPsiElement(member));
        if (relationalEntity == null) {
            return null;
        }
        String pkFqType = resolvePkType(relationalEntity);
        if (pkFqType == null) {
            return null;
        }
        return new RelationalEntityPk(relationalEntity, pkFqType);
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
        return resolveIdOrEmbeddedIdType(entityType);
    }

    /**
     * Returns the FQ type name of the {@code @EmbeddedId} or simple {@code @Id} field or
     * property of {@code entityType}, without considering {@code @IdClass}.
     * Returns {@code null} if none can be determined.
     */
    static String resolveIdOrEmbeddedIdType(PsiClass entityType) {
        // Check @Id and @EmbeddedId on fields, then on methods
        for (PsiField field : entityType.getFields()) {
            if (AnnotationUtils.hasAnyAnnotation(field, PersistenceConstants.ID, PersistenceConstants.EMBEDDEDID)) {
                return field.getType().getCanonicalText();
            }
        }
        for (PsiMethod method : entityType.getMethods()) {
            if (AnnotationUtils.hasAnyAnnotation(method, PersistenceConstants.ID, PersistenceConstants.EMBEDDEDID)) {
                PsiType returnType = method.getReturnType();
                return returnType != null ? returnType.getCanonicalText() : null;
            }
        }
        return null;
    }

    /**
     * Returns the FQ type of attribute {@code attrName} in {@code embeddableType},
     * checking fields first then property getters.
     * Returns {@code null} if not found.
     */
    static String resolveAttributeType(PsiClass embeddableType, String attrName) {
        if (attrName == null || attrName.isEmpty()) {
            return null;
        }
        // Check fields directly
        PsiField field = embeddableType.findFieldByName(attrName, false);
        if (field != null) {
            return field.getType().getCanonicalText();
        }
        // Fall back to property getter: getXxx() or isXxx()
        for (PsiMethod method : embeddableType.getMethods()) {
            if (method.getParameterList().getParametersCount() != 0) {
                continue;
            }
            String methodName = method.getName();
            String propertyName = null;
            if (methodName.startsWith("get") && methodName.length() > 3) {
                propertyName = Introspector.decapitalize(methodName.substring(3));
            } else if (methodName.startsWith("is") && methodName.length() > 2) {
                propertyName = Introspector.decapitalize(methodName.substring(2));
            }
            if (attrName.equals(propertyName)) {
                PsiType returnType = method.getReturnType();
                return returnType != null ? returnType.getCanonicalText() : null;
            }
        }
        return null;
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
