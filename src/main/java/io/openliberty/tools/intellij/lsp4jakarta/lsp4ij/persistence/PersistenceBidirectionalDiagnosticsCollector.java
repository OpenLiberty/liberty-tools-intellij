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

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.DiagnosticsUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.List;

/**
 * Persistence diagnostic collector that validates bidirectional JPA
 * relationships across entity classes.
 *
 * <p>Two rules are checked using cross-file (module-wide) analysis:
 * <ol>
 *   <li>The inverse side of a bidirectional relationship must declare the
 *       {@code mappedBy} attribute on its {@code @OneToMany}, {@code @OneToOne},
 *       or {@code @ManyToMany} annotation.</li>
 *   <li>The inverse side of a relationship must not carry {@code @JoinTable},
 *       {@code @JoinColumn}, or {@code @JoinColumns} annotations.</li>
 * </ol>
 *
 * <p>The target entity type is resolved per-member by extracting the element type
 * from the member's {@link PsiType} (resolving collection type arguments where needed)
 * and checking that the resolved {@link PsiClass} carries {@code @Entity}. This avoids
 * a full module-wide scan and is consistent with the current workspace state.
 *
 * <p>Specification reference:
 * https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0
 */
public class PersistenceBidirectionalDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public PersistenceBidirectionalDiagnosticsCollector() {
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

        for (PsiClass type : unit.getClasses()) {
            // Only process @Entity-annotated classes.
            if (!isMatchedAnnotation(type.getAnnotations(), PersistenceConstants.ENTITY)) {
                continue;
            }

            for (PsiField field : type.getFields()) {
                validateRelationshipMember(field, field.getType(), type, unit, diagnostics);
            }

            for (PsiMethod method : type.getMethods()) {
                validateRelationshipMember(method, method.getReturnType(), type, unit, diagnostics);
            }
        }
    }

    /**
     * Validates relationship annotations on a single field or method.
     *
     * @param member        the PSI element (field or method) being validated
     * @param memberType    the field type or method return type
     * @param declaringType the entity class that owns the member
     * @param unit          the compilation unit of the declaring class
     * @param diagnostics   the list to append new diagnostics to
     */
    private void validateRelationshipMember(PsiElement member, PsiType memberType,
                                            PsiClass declaringType,
                                            PsiJavaFile unit,
                                            List<Diagnostic> diagnostics) {
        for (String relAnnotationFQ : PersistenceConstants.INVERSE_CAPABLE_RELATIONSHIP_ANNOTATIONS) {
            PsiAnnotation relAnnotation = AnnotationUtils.getAnnotation(member, relAnnotationFQ);
            if (relAnnotation == null) {
                continue;
            }

            String relSimpleName = JDTUtils.getSimpleName(relAnnotationFQ);
            String mappedByValue = AnnotationUtils.getAnnotationMemberValue(
                    relAnnotation, PersistenceConstants.MAPPED_BY);
            boolean hasMappedBy = mappedByValue != null && !mappedByValue.isEmpty();

            if (hasMappedBy) {
                // Explicitly the inverse side — owner-only annotations are forbidden here.
                for (String ownerOnlyAnnotation : PersistenceConstants.OWNER_ONLY_ANNOTATIONS) {
                    if (AnnotationUtils.hasAnnotation(member, ownerOnlyAnnotation)) {
                        String messageKey = PersistenceConstants.JOIN_TABLE.equals(ownerOnlyAnnotation)
                                ? "JoinTableOnInverseSide" : "JoinColumnOnInverseSide";
                        String diagnosticCode = PersistenceConstants.JOIN_TABLE.equals(ownerOnlyAnnotation)
                                ? PersistenceConstants.DIAGNOSTIC_CODE_JOIN_TABLE_ON_INVERSE
                                : PersistenceConstants.DIAGNOSTIC_CODE_JOIN_COLUMN_ON_INVERSE;
                        diagnostics.add(createDiagnostic(member, unit,
                                Messages.getMessage(messageKey),
                                diagnosticCode, null,
                                DiagnosticSeverity.Error));
                    }
                }
            } else {
                // No mappedBy — flag only when the target entity back-references this class,
                // proving the relationship is bidirectional.
                PsiClass targetType = resolveTargetEntityType(memberType);
                if (targetType != null && isInverseSideOf(targetType, declaringType, relAnnotationFQ)) {
                    diagnostics.add(createDiagnostic(member, unit,
                            Messages.getMessage("InverseSideMissingMappedBy", relSimpleName),
                            PersistenceConstants.DIAGNOSTIC_CODE_INVERSE_MISSING_MAPPED_BY, null,
                            DiagnosticSeverity.Error));
                }
            }
            // Only one relationship annotation per member expected — stop after first match.
            break;
        }
    }

    /**
     * Resolves the target entity {@link PsiClass} for a given member type.
     *
     * <p>For collection-typed members ({@code List<Employee>}, {@code Set<Order>}),
     * the first type argument is resolved. For single-valued members the declared
     * type is resolved directly. Only {@code @Entity}-annotated classes are returned.
     *
     * @param memberType the field type or method return type
     * @return the target entity {@link PsiClass}, or {@code null} if not resolvable
     */
    private PsiClass resolveTargetEntityType(PsiType memberType) {
        PsiClass resolved = DiagnosticsUtils.resolveElementType(memberType);
        if (resolved == null) {
            return null;
        }
        // Only return @Entity-annotated types to avoid false positives.
        return AnnotationUtils.getAnnotation(resolved, PersistenceConstants.ENTITY) != null
                ? resolved : null;
    }

    /**
     * Checks whether {@code targetType} has a back-reference field or method
     * pointing at {@code declaringType} via the mirrored relationship annotation,
     * confirming a bidirectional relationship.
     *
     * @param targetType      the other side of the potential relationship
     * @param declaringType   the entity class whose member is being validated
     * @param relAnnotationFQ the fully-qualified relationship annotation on the declaring side
     * @return {@code true} if a back-reference exists
     */
    private boolean isInverseSideOf(PsiClass targetType, PsiClass declaringType,
                                    String relAnnotationFQ) {
        // Mirrored annotations:
        //   @OneToMany  ↔  @ManyToOne
        //   @ManyToMany ↔  @ManyToMany  (self-mirroring)
        //   @OneToOne   ↔  @OneToOne    (self-mirroring)
        //
        // For self-mirroring annotations (@ManyToMany, @OneToOne), a bare match of the
        // annotation on the other side is not sufficient — the other side must also carry
        // a non-empty mappedBy, which is the explicit marker that it is the inverse side.
        // Without this extra check both sides would flag each other as the inverse.
        String mirroredAnnotationFQ = getMirroredAnnotation(relAnnotationFQ);
        boolean requiresMappedByOnTarget = mirroredAnnotationFQ.equals(relAnnotationFQ);
        String declaringSimpleName = declaringType.getName();

        for (PsiField field : targetType.getFields()) {
            if (hasMirroredBackReference(field, field.getType(), mirroredAnnotationFQ,
                    requiresMappedByOnTarget, declaringSimpleName)) {
                return true;
            }
        }

        for (PsiMethod method : targetType.getMethods()) {
            PsiType returnType = method.getReturnType();
            if (returnType != null && hasMirroredBackReference(method, returnType,
                    mirroredAnnotationFQ, requiresMappedByOnTarget, declaringSimpleName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns {@code true} when {@code member} carries the given {@code mirroredAnnotationFQ}
     * and its declared type matches {@code declaringSimpleName}.
     *
     * <p>When {@code requiresMappedByOnTarget} is {@code true} (self-mirroring annotations
     * such as {@code @OneToOne} and {@code @ManyToMany}), the annotation must also have a
     * non-empty {@code mappedBy} attribute to confirm the member is explicitly the inverse side.
     *
     * @param member                  the field or method to inspect
     * @param memberType              the field type or method return type
     * @param mirroredAnnotationFQ    the fully-qualified annotation to look for
     * @param requiresMappedByOnTarget whether a non-empty {@code mappedBy} is required
     * @param declaringSimpleName     the simple name of the declaring entity class
     * @return {@code true} if this member is a matching back-reference
     */
    private boolean hasMirroredBackReference(PsiElement member, PsiType memberType,
                                             String mirroredAnnotationFQ,
                                             boolean requiresMappedByOnTarget,
                                             String declaringSimpleName) {
        PsiAnnotation annotation = AnnotationUtils.getAnnotation(member, mirroredAnnotationFQ);
        if (annotation == null) {
            return false;
        }
        if (requiresMappedByOnTarget) {
            String mappedByValue = AnnotationUtils.getAnnotationMemberValue(
                    annotation, PersistenceConstants.MAPPED_BY);
            if (mappedByValue == null || mappedByValue.isEmpty()) {
                return false;
            }
        }
        return declaringSimpleName.equals(DiagnosticsUtils.getElementTypeSimpleName(memberType));
    }

    /**
     * Returns the mirrored relationship annotation FQ name.
     * {@code @OneToMany} ↔ {@code @ManyToOne}; others are self-mirroring.
     *
     * @param relAnnotationFQ the fully-qualified annotation on the declaring side
     * @return the mirrored fully-qualified annotation name
     */
    private String getMirroredAnnotation(String relAnnotationFQ) {
        if (PersistenceConstants.ONE_TO_MANY.equals(relAnnotationFQ)) {
            return PersistenceConstants.MANY_TO_ONE;
        }
        // MANY_TO_MANY and ONE_TO_ONE are self-mirroring.
        return relAnnotationFQ;
    }
}
