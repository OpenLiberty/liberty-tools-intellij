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
import com.intellij.psi.util.InheritanceUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.util.AnnotationValueExpressionUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.util.PsiUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.util.List;

/**
 * Persistence diagnostic collector that validates {@code @AttributeOverride} and
 * {@code @AssociationOverride} name attributes against declared fields in the target
 * embeddable or mapped-superclass chain.
 *
 * @see <a href="https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0">
 *      Jakarta Persistence 3.0, §11.1.4 and §11.1.2</a>
 */
public class PersistenceMappingDiagnosticsCollector extends AbstractDiagnosticsCollector {

    /** Bundles annotation-family constants to keep validation logic generic. */
    private static final class OverrideDescriptor {
        final String singleFqn;
        final String containerFqn;
        final String errorCode;
        final String msgNotFound;
        final String msgDotInvalid;
        final String msgMapPrefix;

        OverrideDescriptor(String singleFqn, String containerFqn, String errorCode,
                           String msgNotFound, String msgDotInvalid, String msgMapPrefix) {
            this.singleFqn = singleFqn;
            this.containerFqn = containerFqn;
            this.errorCode = errorCode;
            this.msgNotFound = msgNotFound;
            this.msgDotInvalid = msgDotInvalid;
            this.msgMapPrefix = msgMapPrefix;
        }
    }

    private static final OverrideDescriptor ATTRIBUTE_DESC = new OverrideDescriptor(
            PersistenceConstants.ATTRIBUTE_OVERRIDE,
            PersistenceConstants.ATTRIBUTE_OVERRIDES,
            PersistenceConstants.DIAGNOSTIC_CODE_INVALID_ATTRIBUTE_OVERRIDE_NAME,
            "AttributeOverrideNameNotFound",
            "AttributeOverrideDotNotationInvalid",
            "AttributeOverrideMissingMapPrefix");

    private static final OverrideDescriptor ASSOCIATION_DESC = new OverrideDescriptor(
            PersistenceConstants.ASSOCIATION_OVERRIDE,
            PersistenceConstants.ASSOCIATION_OVERRIDES,
            PersistenceConstants.DIAGNOSTIC_CODE_INVALID_ASSOCIATION_OVERRIDE_NAME,
            "AssociationOverrideNameNotFound",
            "AssociationOverrideDotNotationInvalid",
            "AssociationOverrideMissingMapPrefix");

    public PersistenceMappingDiagnosticsCollector() {
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
            // 1. Class-level annotations (MappedSuperclass chain target)
            validateOverridesOnClass(type, ATTRIBUTE_DESC, unit, diagnostics);
            validateOverridesOnClass(type, ASSOCIATION_DESC, unit, diagnostics);

            // 2. Field-level annotations (@Embedded / @ElementCollection target)
            for (PsiField field : type.getFields()) {
                validateOverridesOnMember(field, type, unit, ATTRIBUTE_DESC, diagnostics);
                validateOverridesOnMember(field, type, unit, ASSOCIATION_DESC, diagnostics);
            }

            // 3. Method-level annotations (property-based access)
            for (PsiMethod method : type.getMethods()) {
                validateOverridesOnMember(method, type, unit, ATTRIBUTE_DESC, diagnostics);
                validateOverridesOnMember(method, type, unit, ASSOCIATION_DESC, diagnostics);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Class-level: resolve name against @MappedSuperclass chain
    // -----------------------------------------------------------------------

    private void validateOverridesOnClass(PsiClass type, OverrideDescriptor desc,
                                          PsiJavaFile unit, List<Diagnostic> diagnostics) {
        for (PsiAnnotation annotation : type.getAnnotations()) {
            String fqn = annotation.getQualifiedName();
            if (isMatchedJavaElement(type, fqn, desc.singleFqn)) {
                String name = AnnotationValueExpressionUtil.getAnnotationStringValue(annotation, PersistenceConstants.NAME);
                if (name != null) {
                    validateNameOnSuperclassChain(name, annotation, type, desc, unit, diagnostics);
                }
            } else if (isMatchedJavaElement(type, fqn, desc.containerFqn)) {
                for (PsiAnnotation nested : AnnotationValueExpressionUtil.getNestedAnnotations(annotation)) {
                    String name = AnnotationValueExpressionUtil.getAnnotationStringValue(nested, PersistenceConstants.NAME);
                    if (name != null) {
                        validateNameOnSuperclassChain(name, nested, type, desc, unit, diagnostics);
                    }
                }
            }
        }
    }

    /**
     * Walks the {@code @MappedSuperclass} chain of {@code type} and checks that
     * {@code name} resolves to a field declared in some superclass. Emits a
     * diagnostic if the name cannot be resolved in any superclass.
     */
    private void validateNameOnSuperclassChain(String name, PsiAnnotation annotation,
                                               PsiClass type, OverrideDescriptor desc,
                                               PsiJavaFile unit, List<Diagnostic> diagnostics) {
        PsiClass mappedSuperClass = findMappedSuperclassWithField(type, name);
        if (mappedSuperClass==null) {
            String targetName = resolveSuperclassChainName(type);
            diagnostics.add(createDiagnostic(annotation, unit,
                    Messages.getMessage(desc.msgNotFound, name, targetName),
                    desc.errorCode, null, DiagnosticSeverity.Error));
        }
    }

    /**
     * Returns {@code true} if a field named {@code name} (supporting dot-notation)
     * exists anywhere in the {@code @MappedSuperclass} supertype chain of {@code type}.
     */
    private PsiClass findMappedSuperclassWithField(PsiClass type, String name) {
        PsiClass current = type.getSuperClass();
        while (current != null
                && !PersistenceConstants.OBJECT.equals(current.getQualifiedName())) {
            if (isMatchedAnnotation(current.getAnnotations(), PersistenceConstants.MAPPEDSUPERCLASS)) {
                if (fieldExistsInType(current, name)) {
                    return current;
                }
            }
            current = current.getSuperClass();
        }
        return null;
    }

    /**
     * Returns a human-readable name for the first {@code @MappedSuperclass} in the
     * supertype chain, falling back to the immediate superclass name.
     */
    private String resolveSuperclassChainName(PsiClass type) {
        String superclassName = type.getSuperClass()!=null?type.getSuperClass().getName():null;
        return superclassName != null ? superclassName : type.getQualifiedName();
    }

    // -----------------------------------------------------------------------
    // Field / method-level: resolve name against @Embedded type or @ElementCollection map
    // -----------------------------------------------------------------------

    /**
     * Validates override annotations on a field or method (property-based access).
     */
    private void validateOverridesOnMember(PsiJvmModifiersOwner member, PsiClass declaringType,
                                           PsiJavaFile unit, OverrideDescriptor desc,
                                           List<Diagnostic> diagnostics) {
        PsiAnnotation[] annotations = member.getAnnotations();
        boolean hasEmbedded = isMatchedAnnotation(annotations, PersistenceConstants.EMBEDDED);
        boolean hasElementCollection = isMatchedAnnotation(annotations, PersistenceConstants.ELEMENT_COLLECTION);

        if (!hasEmbedded && !hasElementCollection) {
            return;
        }

        for (PsiAnnotation annotation : annotations) {
            String fqn = annotation.getQualifiedName();
            if (isMatchedJavaElement(declaringType, fqn, desc.singleFqn)) {
                String name = AnnotationValueExpressionUtil.getAnnotationStringValue(annotation, PersistenceConstants.NAME);
                if (name != null) {
                    validateNameOnMember(name, annotation, member, hasElementCollection, desc, unit, diagnostics);
                }
            } else if (isMatchedJavaElement(declaringType, fqn, desc.containerFqn)) {
                for (PsiAnnotation nested : AnnotationValueExpressionUtil.getNestedAnnotations(annotation)) {
                    String name = AnnotationValueExpressionUtil.getAnnotationStringValue(nested, PersistenceConstants.NAME);
                    if (name != null) {
                        validateNameOnMember(name, nested, member, hasElementCollection, desc, unit, diagnostics);
                    }
                }
            }
        }
    }

    private void validateNameOnMember(String name, PsiAnnotation annotation,
                                      PsiJvmModifiersOwner member, boolean isElementCollection,
                                      OverrideDescriptor desc, PsiJavaFile unit,
                                      List<Diagnostic> diagnostics) {
        PsiType memberType = PsiUtils.getMemberType(member);
        if (!(memberType instanceof PsiClassType)) {
            return;
        }
        PsiClassType classType = (PsiClassType) memberType;
        PsiClass resolvedClass = classType.resolve();

        if (isElementCollection
                && InheritanceUtil.isInheritor(resolvedClass, PersistenceConstants.MAP_INTERFACE_FQDN)) {
            validateNameOnMapField(name, annotation, classType, desc, unit, diagnostics);
        } else if (resolvedClass != null) {
            validateNameAgainstType(name, name, annotation, resolvedClass, desc, unit, diagnostics);
        }
    }



    /**
     * Handles {@code @ElementCollection} map fields: requires {@code "key."} or
     * {@code "value."} prefix, then resolves the suffix against the corresponding
     * map type argument.
     */
    private void validateNameOnMapField(String name, PsiAnnotation annotation,
                                        PsiClassType mapType, OverrideDescriptor desc,
                                        PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (!name.startsWith(PersistenceConstants.MAP_KEY_PREFIX)
                && !name.startsWith(PersistenceConstants.MAP_VALUE_PREFIX)) {
            diagnostics.add(createDiagnostic(annotation, unit,
                    Messages.getMessage(desc.msgMapPrefix, name),
                    desc.errorCode, null, DiagnosticSeverity.Error));
            return;
        }

        // Retrieve the actual type arguments of the Map, i.e. [K, V] from Map<K, V>.
        // getParameters() returns an empty array for a raw Map (no generics) and a
        // single-element array if somehow only one bound is present. In either case
        // we cannot determine which class to resolve the "key." or "value." suffix
        // against, so we bail out silently rather than producing a false-positive
        // diagnostic.
        PsiType[] typeArgs = mapType.getParameters();
        if (typeArgs.length < 2) {
            return;
        }

        String suffix;
        PsiType targetTypeArg;
        if (name.startsWith(PersistenceConstants.MAP_KEY_PREFIX)) {
            suffix = name.substring(PersistenceConstants.MAP_KEY_PREFIX.length());
            targetTypeArg = typeArgs[0];
        } else {
            suffix = name.substring(PersistenceConstants.MAP_VALUE_PREFIX.length());
            targetTypeArg = typeArgs[1];
        }

        if (targetTypeArg instanceof PsiClassType) {
            PsiClass targetClass = ((PsiClassType) targetTypeArg).resolve();
            if (targetClass != null) {
                validateNameAgainstType(suffix, suffix, annotation, targetClass, desc, unit, diagnostics);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Core name resolution (supports dot-notation)
    // -----------------------------------------------------------------------

    /**
     * Validates {@code name} against declared fields of {@code targetType},
     * supporting dot-notation for nested embeddables.
     *
     * @param fullName the original full name from the annotation (used in error messages)
     * @param name     the remaining segment(s) to resolve against {@code targetType}
     */
    private void validateNameAgainstType(String fullName, String name, PsiAnnotation annotation,
                                         PsiClass targetType, OverrideDescriptor desc,
                                         PsiJavaFile unit, List<Diagnostic> diagnostics) {
        int dot = name.indexOf('.');
        if (dot == PersistenceConstants.NOT_FOUND) {
            if (!PsiUtils.hasFieldInType(targetType, name)) {
                String message = fullName.equals(name)
                        ? Messages.getMessage(desc.msgNotFound, name, targetType.getName())
                        : Messages.getMessage(desc.msgDotInvalid, fullName, name, targetType.getName());
                diagnostics.add(createDiagnostic(annotation, unit, message,
                        desc.errorCode, null, DiagnosticSeverity.Error));
            }
        } else {
            // Dot-notation: resolve first segment, recurse on remainder
            String first = name.substring(0, dot);
            String rest = name.substring(dot + 1);
            if (!PsiUtils.hasFieldInType(targetType, first)) {
                diagnostics.add(createDiagnostic(annotation, unit,
                        Messages.getMessage(desc.msgDotInvalid, fullName, first, targetType.getName()),
                        desc.errorCode, null, DiagnosticSeverity.Error));
            } else {
                PsiField nestedField = targetType.findFieldByName(first, false);
                if (nestedField != null && nestedField.getType() instanceof PsiClassType) {
                    PsiClass nestedClass = ((PsiClassType) nestedField.getType()).resolve();
                    if (nestedClass != null) {
                        validateNameAgainstType(fullName, rest, annotation, nestedClass,
                                desc, unit, diagnostics);
                    }
                }
            }
        }
    }

    /**
     * Returns {@code true} if {@code name} (supporting dot-notation) resolves to a
     * field declared in {@code type} or its nested embeddable chain.
     */
    private boolean fieldExistsInType(PsiClass type, String name) {
        int dot = name.indexOf('.');
        if (dot == PersistenceConstants.NOT_FOUND) {
            return PsiUtils.hasFieldInType(type, name);
        }
        String first = name.substring(0, dot);
        if (!PsiUtils.hasFieldInType(type, first)) {
            return false;
        }
        PsiField nestedField = type.findFieldByName(first, false);
        if (nestedField == null || !(nestedField.getType() instanceof PsiClassType)) {
            return false;
        }
        PsiClass nestedClass = ((PsiClassType) nestedField.getType()).resolve();
        return nestedClass != null && fieldExistsInType(nestedClass, name.substring(dot + 1));
    }

}
