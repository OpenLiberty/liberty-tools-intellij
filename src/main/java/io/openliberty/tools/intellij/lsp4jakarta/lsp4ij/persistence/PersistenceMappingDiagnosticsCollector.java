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
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
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
                String name = getAnnotationStringValue(annotation, PersistenceConstants.NAME);
                if (name != null) {
                    validateNameOnSuperclassChain(name, annotation, type, desc, unit, diagnostics);
                }
            } else if (isMatchedJavaElement(type, fqn, desc.containerFqn)) {
                for (PsiAnnotation nested : getNestedAnnotations(annotation)) {
                    String name = getAnnotationStringValue(nested, PersistenceConstants.NAME);
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
        if (!existsInSuperclassChain(type, name)) {
            String targetName = getFirstMappedSuperclassName(type);
            diagnostics.add(createDiagnostic(annotation, unit,
                    Messages.getMessage(desc.msgNotFound, name, targetName),
                    desc.errorCode, null, DiagnosticSeverity.Error));
        }
    }

    /**
     * Returns {@code true} if a field named {@code name} (supporting dot-notation)
     * exists anywhere in the {@code @MappedSuperclass} supertype chain of {@code type}.
     */
    private boolean existsInSuperclassChain(PsiClass type, String name) {
        PsiClass current = type.getSuperClass();
        while (current != null
                && !PersistenceConstants.OBJECT.equals(current.getQualifiedName())) {
            if (isMatchedAnnotation(current.getAnnotations(), PersistenceConstants.MAPPEDSUPERCLASS)) {
                if (fieldExistsInType(current, name)) {
                    return true;
                }
            }
            current = current.getSuperClass();
        }
        return false;
    }

    /**
     * Returns the simple name of the first {@code @MappedSuperclass} in the supertype
     * chain, falling back to the immediate superclass name.
     */
    private String getFirstMappedSuperclassName(PsiClass type) {
        PsiClass current = type.getSuperClass();
        while (current != null
                && !PersistenceConstants.OBJECT.equals(current.getQualifiedName())) {
            if (isMatchedAnnotation(current.getAnnotations(), PersistenceConstants.MAPPEDSUPERCLASS)) {
                return current.getName() != null ? current.getName() : "superclass";
            }
            current = current.getSuperClass();
        }
        PsiClass superClass = type.getSuperClass();
        return (superClass != null && superClass.getName() != null)
                ? superClass.getName() : "superclass";
    }

    // -----------------------------------------------------------------------
    // Field / method-level: resolve name against @Embedded type or @ElementCollection map
    // -----------------------------------------------------------------------

    /**
     * Validates override annotations on a field or getter method (property-based access).
     * The type is resolved via {@link #getMemberType(PsiJvmModifiersOwner)} — {@code field.getType()}
     * for fields, {@code method.getReturnType()} for methods — keeping the rest of the logic identical.
     *
     * <p>For {@code @AttributeOverride} / {@code @AttributeOverrides}: if the member
     * lacks {@code @Embedded}, {@code @EmbeddedId}, or {@code @ElementCollection},
     * a diagnostic is emitted for each such annotation — the override has no valid target.
     */
    private void validateOverridesOnMember(PsiJvmModifiersOwner member, PsiClass declaringType,
                                           PsiJavaFile unit, OverrideDescriptor desc,
                                           List<Diagnostic> diagnostics) {
        PsiAnnotation[] annotations = member.getAnnotations();
        boolean hasEmbedded = isMatchedAnnotation(annotations, PersistenceConstants.EMBEDDED);
        boolean hasEmbeddedId = isMatchedAnnotation(annotations, PersistenceConstants.EMBEDDEDID);
        boolean hasElementCollection = isMatchedAnnotation(annotations, PersistenceConstants.ELEMENT_COLLECTION);
        boolean hasValidTarget = hasEmbedded || hasEmbeddedId || hasElementCollection;

        // Only @AttributeOverride carries the @Embedded/@EmbeddedId/@ElementCollection
        // restriction at the field/method level.
        boolean isAttributeOverrideDesc = PersistenceConstants.ATTRIBUTE_OVERRIDE.equals(desc.singleFqn);

        for (PsiAnnotation annotation : annotations) {
            String fqn = annotation.getQualifiedName();
            boolean isSingle = isMatchedJavaElement(declaringType, fqn, desc.singleFqn);
            boolean isContainer = isMatchedJavaElement(declaringType, fqn, desc.containerFqn);

            if (!isSingle && !isContainer) {
                continue;
            }

            if (isAttributeOverrideDesc && !hasValidTarget) {
                // @AttributeOverride / @AttributeOverrides on a field or method that is not
                // @Embedded, @EmbeddedId, or @ElementCollection — no valid override target.
                diagnostics.add(createDiagnostic(annotation, unit,
                        Messages.getMessage("AttributeOverrideOnNonEmbeddedField", JDTUtils.getSimpleName(fqn)),
                        PersistenceConstants.DIAGNOSTIC_CODE_ATTRIBUTE_OVERRIDE_ON_NON_EMBEDDED,
                        null, DiagnosticSeverity.Error));
                // Skip name-resolution: there is no embeddable type to resolve against.
                continue;
            }

            if (!hasEmbedded && !hasElementCollection) {
                // For @AssociationOverride: member has no supported target annotation — skip.
                continue;
            }

            if (isSingle) {
                String name = getAnnotationStringValue(annotation, PersistenceConstants.NAME);
                if (name != null) {
                    validateNameOnMember(name, annotation, member, hasElementCollection, desc, unit, diagnostics);
                }
            } else {
                for (PsiAnnotation nested : getNestedAnnotations(annotation)) {
                    String name = getAnnotationStringValue(nested, PersistenceConstants.NAME);
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
        PsiType memberType = getMemberType(member);
        if (!(memberType instanceof PsiClassType)) {
            return;
        }
        PsiClassType classType = (PsiClassType) memberType;
        PsiClass resolvedClass = classType.resolve();

        if (isElementCollection
                && InheritanceUtil.isInheritor(resolvedClass, PersistenceConstants.MAP_INTERFACE_FQN)) {
            validateNameOnMapField(name, annotation, classType, desc, unit, diagnostics);
        } else if (resolvedClass != null) {
            validateNameAgainstType(name, name, annotation, resolvedClass, desc, unit, diagnostics);
        }
    }

    /**
     * Returns the declared type of a field or the return type of a method.
     * Returns {@code null} for any other {@link PsiJvmModifiersOwner} subtype.
     */
    private PsiType getMemberType(PsiJvmModifiersOwner member) {
        if (member instanceof PsiField) {
            return ((PsiField) member).getType();
        } else if (member instanceof PsiMethod) {
            return ((PsiMethod) member).getReturnType();
        }
        return null;
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

        PsiType[] typeArgs = mapType.getParameters();
        if (typeArgs.length < 2) {
            return; // raw map — cannot validate further
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
        if (dot == -1) {
            if (!hasFieldInType(targetType, name)) {
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
            if (!hasFieldInType(targetType, first)) {
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
        if (dot == -1) {
            return hasFieldInType(type, name);
        }
        String first = name.substring(0, dot);
        if (!hasFieldInType(type, first)) {
            return false;
        }
        PsiField nestedField = type.findFieldByName(first, false);
        if (nestedField == null || !(nestedField.getType() instanceof PsiClassType)) {
            return false;
        }
        PsiClass nestedClass = ((PsiClassType) nestedField.getType()).resolve();
        return nestedClass != null && fieldExistsInType(nestedClass, name.substring(dot + 1));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Returns {@code true} if {@code type} declares a field with the given simple name. */
    private boolean hasFieldInType(PsiClass type, String fieldName) {
        return type.findFieldByName(fieldName, false) != null;
    }

    /**
     * Extracts the string value of a named annotation attribute, stripping surrounding quotes.
     * Returns {@code null} if the attribute is absent or not a string literal.
     */
    private String getAnnotationStringValue(PsiAnnotation annotation, String attributeName) {
        PsiAnnotationMemberValue value = annotation.findAttributeValue(attributeName);
        if (value == null) {
            return null;
        }
        String text = value.getText();
        if (text != null && text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
            return text.substring(1, text.length() - 1);
        }
        return null;
    }

    /**
     * Returns the nested single-annotation instances from a container annotation's
     * {@code value} attribute (e.g. the entries inside {@code @AttributeOverrides}).
     */
    private PsiAnnotation[] getNestedAnnotations(PsiAnnotation container) {
        PsiAnnotationMemberValue value = container.findAttributeValue("value");
        if (value instanceof PsiArrayInitializerMemberValue) {
            PsiAnnotationMemberValue[] initializers =
                    ((PsiArrayInitializerMemberValue) value).getInitializers();
            int count = 0;
            PsiAnnotation[] buf = new PsiAnnotation[initializers.length];
            for (PsiAnnotationMemberValue item : initializers) {
                if (item instanceof PsiAnnotation) {
                    buf[count++] = (PsiAnnotation) item;
                }
            }
            if (count == initializers.length) {
                return buf;
            }
            PsiAnnotation[] trimmed = new PsiAnnotation[count];
            System.arraycopy(buf, 0, trimmed, 0, count);
            return trimmed;
        } else if (value instanceof PsiAnnotation) {
            return new PsiAnnotation[]{(PsiAnnotation) value};
        }
        return PsiAnnotation.EMPTY_ARRAY;
    }
}
