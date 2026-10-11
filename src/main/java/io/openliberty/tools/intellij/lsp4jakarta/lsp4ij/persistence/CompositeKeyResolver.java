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
        PsiClass embeddedIdType = null;

        // Check @EmbeddedId on fields
        for (PsiField field : entityType.getFields()) {
            if (hasAnnotation(field.getAnnotations(), PersistenceConstants.EMBEDDEDID)) {
                embeddedIdType = resolveFieldClass(field);
                break;
            }
        }

        // Check @EmbeddedId on property getters (if not found on fields)
        if (embeddedIdType == null) {
            for (PsiMethod method : entityType.getMethods()) {
                if (hasAnnotation(method.getAnnotations(), PersistenceConstants.EMBEDDEDID)) {
                    embeddedIdType = resolveMethodReturnClass(method);
                    break;
                }
            }
        }

        // Check for @IdClass at the type level
        boolean hasIdClass = hasAnnotation(entityType.getAnnotations(), PersistenceConstants.IDCLASS);

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
        PsiClass parentEntity = null;
        if (member instanceof PsiField field) {
            parentEntity = resolveFieldClass(field);
        } else if (member instanceof PsiMethod method) {
            parentEntity = resolveMethodReturnClass(method);
        }
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
        PsiAnnotation idClassAnn = findAnnotation(entityType.getAnnotations(), PersistenceConstants.IDCLASS);
        if (idClassAnn != null) {
            PsiAnnotationMemberValue value = idClassAnn.findAttributeValue("value");
            if (value instanceof PsiClassObjectAccessExpression classObj) {
                PsiType type = classObj.getOperand().getType();
                return type.getCanonicalText();
            }
        }
        // Check @Id and @EmbeddedId on fields
        for (PsiField field : entityType.getFields()) {
            if (isIdOrEmbeddedId(field.getAnnotations())) {
                return field.getType().getCanonicalText();
            }
        }
        // Check @Id and @EmbeddedId on methods
        for (PsiMethod method : entityType.getMethods()) {
            if (isIdOrEmbeddedId(method.getAnnotations())) {
                PsiType rt = method.getReturnType();
                return rt != null ? rt.getCanonicalText() : null;
            }
        }
        return null;
    }

    private static boolean isIdOrEmbeddedId(PsiAnnotation[] annotations) {
        return hasAnnotation(annotations, PersistenceConstants.ID)
                || hasAnnotation(annotations, PersistenceConstants.EMBEDDEDID);
    }

    /**
     * Returns {@code true} if any annotation in {@code annotations} has the given fully-qualified name.
     */
    static boolean hasAnnotation(PsiAnnotation[] annotations, String fqName) {
        for (PsiAnnotation ann : annotations) {
            if (fqName.equals(ann.getQualifiedName())) {
                return true;
            }
        }
        return false;
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
        for (PsiMethod method : embeddableType.getMethods()) {
            if (getterName.equals(method.getName()) && method.getParameterList().getParametersCount() == 0) {
                PsiType rt = method.getReturnType();
                return rt != null ? rt.getCanonicalText() : null;
            }
        }
        return null;
    }

    /**
     * Returns the annotation value string from a {@code @MapsId} annotation.
     * Returns {@code null} or empty string if not specified.
     */
    static String getMapsIdValue(PsiAnnotation mapsIdAnn) {
        PsiAnnotationMemberValue val = mapsIdAnn.findAttributeValue("value");
        if (val == null) {
            return null;
        }
        String text = val.getText();
        // Strip surrounding quotes from string literals
        if (text.startsWith("\"") && text.endsWith("\"") && text.length() > 1) {
            return text.substring(1, text.length() - 1);
        }
        return text.isEmpty() ? null : text;
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

    // ---- private PSI resolution helpers ----------------------------------------

    /** Resolves the class that a field's type refers to, or {@code null}. */
    private static PsiClass resolveFieldClass(PsiField field) {
        PsiType type = field.getType();
        if (type instanceof PsiClassType classType) {
            return classType.resolve();
        }
        return null;
    }

    /** Resolves the class that a method's return type refers to, or {@code null}. */
    private static PsiClass resolveMethodReturnClass(PsiMethod method) {
        PsiType rt = method.getReturnType();
        if (rt instanceof PsiClassType classType) {
            return classType.resolve();
        }
        return null;
    }

    /** Finds the first annotation matching {@code fqName} in the array, or {@code null}. */
    static PsiAnnotation findAnnotation(PsiAnnotation[] annotations, String fqName) {
        for (PsiAnnotation ann : annotations) {
            if (fqName.equals(ann.getQualifiedName())) {
                return ann;
            }
        }
        return null;
    }
}
