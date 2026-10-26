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
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence.CompositeKeyResolver.KeyStrategy;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence.CompositeKeyResolver.RelationalEntityPk;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.util.PsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.ArrayList;
import java.util.List;

/**
 * Diagnostic collector for Jakarta Persistence 3.0 derived-identity rules
 * (specification section 2.4.1).
 *
 * <h3>Checks performed</h3>
 * <ol>
 * <li>{@code @MapsId} must be placed on a field or property also annotated
 *     with {@code @OneToOne} or {@code @ManyToOne}.</li>
 * <li>{@code @MapsId} that specifies a {@code value} can only be used on an
 *     entity that declares a composite primary key via {@code @EmbeddedId} or
 *     {@code @IdClass}.</li>
 * <li>When {@code @MapsId} specifies a value, that value must name an attribute
 *     in the {@code @EmbeddedId} class.</li>
 * <li>The type of that attribute must be compatible with the parent entity's
 *     primary key type.</li>
 * <li>A value-less {@code @MapsId} with {@code @EmbeddedId}: the embedded id
 *     type must be the same as the parent entity's primary key type
 *     (spec §2.4.1.1, Examples 5b/6b).</li>
 * </ol>
 *
 * <p><b>Note:</b> {@code @IdClass} member alignment is covered by a separate
 * diagnostic collector and is intentionally out of scope here.</p>
 */
public class DerivedIdentityDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public DerivedIdentityDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return PersistenceConstants.DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit == null) {
            return;
        }
        // Visit nested classes too, so an @Entity nested in a non-entity holder is validated.
        List<PsiClass> allClasses = new ArrayList<>();
        PsiUtils.collectAllClasses(unit.getClasses(), allClasses);
        for (PsiClass type : allClasses) {
            if (isMatchedAnnotation(type.getAnnotations(), PersistenceConstants.ENTITY)) {
                validateEntityType(type, unit, diagnostics);
            }
        }
    }

    /**
     * Validates all {@code @MapsId}-annotated fields and property getters on {@code type}.
     */
    private void validateEntityType(PsiClass type, PsiJavaFile unit, List<Diagnostic> diagnostics) {
        // Resolve the composite-key strategy once per entity (avoids repeated annotation scans).
        KeyStrategy keyStrategy = CompositeKeyResolver.resolveKeyStrategy(type);

        for (PsiField field : type.getFields()) {
            if (field.hasModifierProperty(PsiModifier.STATIC)) {
                continue;
            }
            PsiAnnotation mapsIdAnn = AnnotationUtils.getAnnotation(field, PersistenceConstants.MAPSID);
            if (mapsIdAnn != null) {
                validateMapsIdMember(field, field.getAnnotations(), mapsIdAnn, keyStrategy, unit, diagnostics);
            }
        }
        for (PsiMethod method : type.getMethods()) {
            PsiAnnotation mapsIdAnn = AnnotationUtils.getAnnotation(method, PersistenceConstants.MAPSID);
            if (mapsIdAnn != null) {
                validateMapsIdMember(method, method.getAnnotations(), mapsIdAnn, keyStrategy, unit, diagnostics);
            }
        }
    }

    /**
     * Coordinator: validates structural placement first, then cross-file attribute binding.
     * Attribute binding is skipped when structural validation reports a fatal violation.
     */
    private void validateMapsIdMember(PsiJvmModifiersOwner member, PsiAnnotation[] memberAnnotations,
                                      PsiAnnotation mapsIdAnn, KeyStrategy keyStrategy,
                                      PsiJavaFile unit, List<Diagnostic> diagnostics) {
        String mapsIdValue = AnnotationUtils.getAnnotationMemberValue(mapsIdAnn, "value");
        boolean hasValue = mapsIdValue != null && !mapsIdValue.isEmpty();

        boolean isValidStructure = isValidMapsIdStructure(member, memberAnnotations, keyStrategy, hasValue, unit, diagnostics);
        if (!isValidStructure) {
            return;
        }
        if (hasValue) {
            validateMapsIdAttributeBinding(member, memberAnnotations, keyStrategy, mapsIdValue, unit, diagnostics);
        }
        // Value-less @MapsId: skipped for @IdClass entities, since @IdClass member
        // alignment is validated by a separate collector, and when the relationship
        // itself is the @Id (its type is the parent entity, not a key type).
        else if (!keyStrategy.hasIdClass
                && !isMatchedAnnotation(memberAnnotations, PersistenceConstants.ID)) {
            validateValuelessMapsId(member, unit, diagnostics);
        }
    }

    /**
     * Validates structural constraints (Rules 1–2).
     *
     * @return {@code true} if structural constraints pass and attribute-binding checks can proceed,
     *         {@code false} if a structural violation was found (and reported as a diagnostic),
     *         in which case further checks should be skipped.
     */
    private boolean isValidMapsIdStructure(PsiJvmModifiersOwner member, PsiAnnotation[] memberAnnotations,
                                           KeyStrategy keyStrategy, boolean mapsIdHasValue,
                                           PsiJavaFile unit, List<Diagnostic> diagnostics) {
        // Rule 1: @MapsId must be on @OneToOne or @ManyToOne.
        boolean hasToOne = isMatchedAnnotation(memberAnnotations, PersistenceConstants.ONETOONE)
                || isMatchedAnnotation(memberAnnotations, PersistenceConstants.MANYTOONE);
        if (!hasToOne) {
            diagnostics.add(createDiagnostic(member, unit,
                    Messages.getMessage("MapsIdOnInvalidRelationship"),
                    PersistenceConstants.DIAGNOSTIC_CODE_MAPSID_ON_INVALID_RELATIONSHIP,
                    null, DiagnosticSeverity.Error));
            return false;
        }

        // Rule 2: @MapsId("value") requires a composite key.
        if (mapsIdHasValue && !keyStrategy.isComposite()) {
            diagnostics.add(createDiagnostic(member, unit,
                    Messages.getMessage("MapsIdRequiresCompositeKey"),
                    PersistenceConstants.DIAGNOSTIC_CODE_MAPSID_REQUIRES_COMPOSITE_KEY,
                    null, DiagnosticSeverity.Error));
            return false;
        }

        return true;
    }

    /**
     * Validates that the {@code @MapsId} value names an existing attribute in the
     * {@code @EmbeddedId} class and that its type is compatible with the relational entity's
     * primary key type (Rules 4–5, spec §2.4.1).
     * Only runs when the entity uses {@code @EmbeddedId}.
     */
    private void validateMapsIdAttributeBinding(PsiJvmModifiersOwner member, PsiAnnotation[] memberAnnotations,
                                                KeyStrategy keyStrategy, String mapsIdValue,
                                                PsiJavaFile unit, List<Diagnostic> diagnostics) {
        // Only applicable when the entity uses @EmbeddedId.
        if (keyStrategy.embeddedIdType == null) {
            return; // @IdClass path — attribute-name checks handled by separate collector
        }

        RelationalEntityPk relationalEntityPk = CompositeKeyResolver.resolveRelationalEntityPk(member);
        if (relationalEntityPk == null) {
            return;
        }

        // Rule 4: the named value must be an attribute in the @EmbeddedId class.
        PsiClass embeddedIdClass = keyStrategy.embeddedIdType;
        String embeddedAttrFqType = CompositeKeyResolver.resolveAttributeType(embeddedIdClass, mapsIdValue);
        if (embeddedAttrFqType == null) {
            diagnostics.add(createDiagnostic(member, unit,
                    Messages.getMessage("MapsIdValueNotInEmbeddedIdClass", mapsIdValue, embeddedIdClass.getName()),
                    PersistenceConstants.DIAGNOSTIC_CODE_MAPSID_VALUE_NOT_IN_EMBEDDEDID,
                    null, DiagnosticSeverity.Error));
            return;
        }

        // Rule 5: the attribute's type must be compatible with the relational entity's PK type.
        if (!CompositeKeyResolver.typesCompatible(embeddedAttrFqType, relationalEntityPk.pkFqType)) {
            diagnostics.add(createDiagnostic(member, unit,
                    Messages.getMessage("MapsIdValueTypeMismatch",
                            mapsIdValue, embeddedIdClass.getName(),
                            embeddedAttrFqType, relationalEntityPk.entity.getName(), relationalEntityPk.pkFqType),
                    PersistenceConstants.DIAGNOSTIC_CODE_MAPSID_VALUE_TYPE_MISMATCH,
                    null, DiagnosticSeverity.Error));
        }
    }

    /**
     * Validates a value-less {@code @MapsId} with {@code @EmbeddedId}
     * (spec §2.4.1.1, Examples 5b/6b): the embedded id type must match the relational entity's PK type.
     * Skipped when the relationship is also annotated {@code @Id}, or when the entity uses
     * {@code @IdClass}.
     */
    private void validateValuelessMapsId(PsiJvmModifiersOwner member, PsiJavaFile unit,
                                         List<Diagnostic> diagnostics) {
        RelationalEntityPk relationalEntityPk = CompositeKeyResolver.resolveRelationalEntityPk(member);
        if (relationalEntityPk == null) {
            return;
        }
        PsiClass declaringType = null;
        if (member instanceof PsiField field) {
            declaringType = field.getContainingClass();
        } else if (member instanceof PsiMethod method) {
            declaringType = method.getContainingClass();
        }
        if (declaringType == null) {
            return;
        }
        String ownPkType = CompositeKeyResolver.resolveIdOrEmbeddedIdType(declaringType);
        if (ownPkType != null && !CompositeKeyResolver.typesCompatible(ownPkType, relationalEntityPk.pkFqType)) {
            diagnostics.add(createDiagnostic(member, unit,
                    Messages.getMessage("MapsIdPrimaryKeyTypeMismatch",
                            ownPkType, relationalEntityPk.entity.getName(), relationalEntityPk.pkFqType),
                    PersistenceConstants.DIAGNOSTIC_CODE_MAPSID_PK_TYPE_MISMATCH,
                    null, DiagnosticSeverity.Error));
        }
    }
}
