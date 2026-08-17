/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation, Ankush Sharma and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation, Ankush Sharma - initial API and implementation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.persistence;

import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.DiagnosticsUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import java.beans.Introspector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PersistenceMapKeyDiagnosticsCollector extends AbstractDiagnosticsCollector {

    public PersistenceMapKeyDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return PersistenceConstants.DIAGNOSTIC_SOURCE;
    }

    @Override
    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit != null) {
            PsiClass[] alltypes = unit.getClasses();
            PsiMethod[] methods;
            PsiField[] fields;

            for (PsiClass type : alltypes) {
                methods = type.getMethods();
                for (PsiMethod method : methods) {
                    collectDiagnostics(unit, diagnostics, type, method);
                }
                // Go through each field to ensure they do not have both MapKey and MapKeyColumn
                // Annotations
                fields = type.getFields();
                for (PsiField field : fields) {
                    collectDiagnostics(unit, diagnostics, type, field);
                }
            }
        }
    }

    private void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics,
                                    PsiClass type, PsiJvmModifiersOwner fieldOrProperty) {
        List<PsiAnnotation> mapKeyJoinCols = new ArrayList<PsiAnnotation>();
        boolean hasMapKeyAnnotation = false;
        boolean hasMapKeyClassAnnotation = false;
        boolean hasMapKeyEnumeratedAnnotation = false;
        boolean hasMapKeyTemporalAnnotation = false;
        boolean hasTypeDiagnostics = false;
        PsiAnnotation[] allAnnotations = fieldOrProperty.getAnnotations();

        // Collect @Convert annotations for later validation
        List<PsiAnnotation> convertAnnotations = new ArrayList<PsiAnnotation>();

        for (PsiAnnotation annotation : allAnnotations) {
            String matchedAnnotation = getMatchedJavaElementName(type, annotation.getQualifiedName(),
                    PersistenceConstants.SET_OF_PERSISTENCE_ANNOTATIONS);
            if (matchedAnnotation != null) {
                if (PersistenceConstants.MAPKEY.equals(matchedAnnotation))
                    hasMapKeyAnnotation = true;
                else if (PersistenceConstants.MAPKEYCLASS.equals(matchedAnnotation))
                    hasMapKeyClassAnnotation = true;
                else if (PersistenceConstants.MAPKEYJOINCOLUMN.equals(matchedAnnotation))
                    mapKeyJoinCols.add(annotation);
                else if (PersistenceConstants.MAPKEYENUMERATED.equals(matchedAnnotation))
                    hasMapKeyEnumeratedAnnotation = true;
            }
            // Check for @MapKeyTemporal annotation
            if (PersistenceConstants.MAPKEYTEMPORAL.equals(annotation.getQualifiedName())) {
                hasMapKeyTemporalAnnotation = true;
            }
            // Collect @Convert annotations
            String convertMatch = getMatchedJavaElementName(type, annotation.getQualifiedName(),
                    new String[]{PersistenceConstants.CONVERT});
            if (convertMatch != null) {
                convertAnnotations.add(annotation);
            }
        }
        if (hasMapKeyAnnotation) {
            hasTypeDiagnostics = collectTypeDiagnostics(fieldOrProperty, "@MapKey", unit, diagnostics);
            collectAccessorDiagnostics(fieldOrProperty, type, unit, diagnostics);
        }
        if (hasMapKeyClassAnnotation) {
            hasTypeDiagnostics = collectTypeDiagnostics(fieldOrProperty, "@MapKeyClass", unit, diagnostics);
            collectAccessorDiagnostics(fieldOrProperty, type, unit, diagnostics);
        }
        if (!hasTypeDiagnostics && (hasMapKeyAnnotation && hasMapKeyClassAnnotation)) {
            //A single field or property cannot be annotated with both @MapKey and @MapKeyClass
            //Specification References:
            //https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/mapkey
            //https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/mapkeyclass
            diagnostics.add(createDiagnostic(fieldOrProperty, unit,
                    Messages.getMessage("MapKeyAnnotationsNotOnSameField"),
                    PersistenceConstants.DIAGNOSTIC_CODE_INVALID_ANNOTATION, null,
                    DiagnosticSeverity.Error));
        }
        // Validate @MapKeyTemporal annotation
        if (hasMapKeyTemporalAnnotation) {
            validateMapKeyTemporalAnnotation(fieldOrProperty, unit, diagnostics);
        }
        // If we have multiple MapKeyJoinColumn annotations on a single field or property we must
        // ensure each has a name and referencedColumnName
        if (mapKeyJoinCols.size() > 1) {
            validateMapKeyJoinColumnAnnotations(mapKeyJoinCols, fieldOrProperty, unit, diagnostics);
        }
        // Spec section 11.1.34: @MapKeyEnumerated must only be applied to a Map-typed
        // element collection or relationship, and the map key type must be an enum.
        if (hasMapKeyEnumeratedAnnotation) {
            validateMapKeyEnumerated(fieldOrProperty, unit, diagnostics);
        }
        // Validate @Convert annotation rules
        if (!convertAnnotations.isEmpty()) {
            collectConvertDiagnostics(fieldOrProperty, type, convertAnnotations, allAnnotations, unit, diagnostics);
        }
    }

    /**
     * Validates {@code @Convert} annotation rules on a single field or method
     */
    private void collectConvertDiagnostics(PsiJvmModifiersOwner fieldOrProperty, PsiClass type,
                                           List<PsiAnnotation> convertAnnotations,
                                           PsiAnnotation[] allAnnotations,
                                           PsiJavaFile unit, List<Diagnostic> diagnostics) {

        // Rule: Multiple @Convert on the same attribute
        if (convertAnnotations.size() > 1) {
            diagnostics.add(createDiagnostic(fieldOrProperty, unit,
                    Messages.getMessage("ConvertAnnotationMultipleOnSameAttribute"),
                    PersistenceConstants.DIAGNOSTIC_CODE_CONVERT_MULTIPLE_ON_SAME_ATTRIBUTE,
                    null, DiagnosticSeverity.Error));
        }

        // Determine restricted co-annotation name (e.g. "@Id"), if present
        String restrictedAnnotationName = null;
        for (PsiAnnotation otherAnnotation : allAnnotations) {
            String matched = getMatchedJavaElementName(type, otherAnnotation.getQualifiedName(),
                    PersistenceConstants.CONVERT_RESTRICTED_ANNOTATIONS);
            if (matched != null) {
                restrictedAnnotationName = "@" + matched.substring(matched.lastIndexOf('.') + 1);
                break;
            }
        }

        for (PsiAnnotation convertAnnotation : convertAnnotations) {
            PsiNameValuePair[] attributes = convertAnnotation.getParameterList().getAttributes();

            boolean hasConverter = Arrays.stream(attributes)
                    .anyMatch(a -> PersistenceConstants.CONVERTER.equals(a.getName()));
            boolean hasDisableConversion = Arrays.stream(attributes)
                    .anyMatch(a -> PersistenceConstants.DISABLE_CONVERSION.equals(a.getName())
                            && "true".equals(a.getLiteralValue()));

            // Rule: Neither converter nor disableConversion=true specified
            if (!hasConverter && !hasDisableConversion) {
                diagnostics.add(createDiagnostic(fieldOrProperty, unit,
                        Messages.getMessage("ConvertAnnotationMissingConverterOrDisable"),
                        PersistenceConstants.DIAGNOSTIC_CODE_CONVERT_MISSING_CONVERTER_OR_DISABLE,
                        null, DiagnosticSeverity.Error));
            }

            // Rule: @Convert on a restricted target
            if (restrictedAnnotationName != null) {
                diagnostics.add(createDiagnostic(fieldOrProperty, unit,
                        Messages.getMessage("ConvertAnnotationOnRestrictedTarget", restrictedAnnotationName),
                        PersistenceConstants.DIAGNOSTIC_CODE_CONVERT_ON_RESTRICTED_TARGET,
                        null, DiagnosticSeverity.Error));
            }
        }
    }

    private boolean collectTypeDiagnostics(PsiJvmModifiersOwner fieldOrProperty, String attribute, PsiJavaFile unit,
                                           List<Diagnostic> diagnostics) {
        boolean hasTypeDiagnostics = false;
        PsiType fieldOrPropertyType = null;
        boolean isMapOrSubtype = false;
        String messageKey = null;
        String code = null;

        if (fieldOrProperty instanceof PsiMethod method) {
            fieldOrPropertyType = method.getReturnType();
            messageKey = "MapKeyAnnotationsReturnTypeOfMethod";
            code = PersistenceConstants.DIAGNOSTIC_CODE_INVALID_RETURN_TYPE;
        } else if (fieldOrProperty instanceof PsiField field) {
            fieldOrPropertyType = field.getType();
            messageKey = "MapKeyAnnotationsTypeOfField";
            code = PersistenceConstants.DIAGNOSTIC_CODE_INVALID_TYPE;
        }
        if (fieldOrPropertyType instanceof PsiClassType classType) {
            PsiClass psiClass = classType.resolve();
            isMapOrSubtype = InheritanceUtil.isInheritor(psiClass, PersistenceConstants.MAP_INTERFACE_FQDN);
        }
        if (!isMapOrSubtype) {
            hasTypeDiagnostics = true;
            diagnostics.add(createDiagnostic(fieldOrProperty, unit, Messages.getMessage(messageKey, attribute),
                    code, null, DiagnosticSeverity.Error));
        }
        return hasTypeDiagnostics;
    }

    private void validateMapKeyJoinColumnAnnotations(List<PsiAnnotation> annotations, PsiElement element,
                                                     PsiJavaFile unit, List<Diagnostic> diagnostics) {
        String message = (element instanceof PsiMethod) ?
                Messages.getMessage("MultipleMapKeyJoinColumnMethod") :
                Messages.getMessage("MultipleMapKeyJoinColumnField");
        annotations.forEach(annotation -> {
            boolean allNamesSpecified, allReferencedColumnNameSpecified;
            List<PsiNameValuePair> memberValues = Arrays.asList(annotation.getParameterList().getAttributes());
            allNamesSpecified = memberValues.stream()
                    .anyMatch((mv) -> mv.getName().equals(PersistenceConstants.NAME));
            allReferencedColumnNameSpecified = memberValues.stream()
                    .anyMatch((mv) -> mv.getName().equals(PersistenceConstants.REFERENCEDCOLUMNNAME));
            if (!allNamesSpecified || !allReferencedColumnNameSpecified) {
                diagnostics.add(createDiagnostic(element, unit, message,
                        PersistenceConstants.DIAGNOSTIC_CODE_MISSING_ATTRIBUTES, null, DiagnosticSeverity.Error));
            }
        });
    }

    /**
     * Validates {@code @MapKeyEnumerated} usage per spec section 11.1.34.
     * Two checks:
     * 1. The field/property type must be {@code java.util.Map} (or a subtype).
     * 2. The map key type parameter must resolve to an enum type; this includes
     *    raw Map (no type params), unbounded/lower-bounded wildcards, and concrete
     *    non-enum types.
     */
    private void validateMapKeyEnumerated(PsiJvmModifiersOwner fieldOrProperty, PsiJavaFile unit,
                                          List<Diagnostic> diagnostics) {
        PsiType elementType = (fieldOrProperty instanceof PsiMethod m)
                ? m.getReturnType()
                : (fieldOrProperty instanceof PsiField f) ? f.getType() : null;

        if (elementType == null) {
            return;
        }

        // Check 1: must be a Map (or subtype) — rejects List, Set, String, primitives, arrays, …
        if (!(elementType instanceof PsiClassType classType)
                || !InheritanceUtil.isInheritor(classType.resolve(), PersistenceConstants.MAP_INTERFACE_FQDN)) {
            diagnostics.add(createDiagnostic(fieldOrProperty, unit,
                    Messages.getMessage("MapKeyEnumeratedOnNonMapType"),
                    PersistenceConstants.DIAGNOSTIC_CODE_MAPKEYENUMERATED_NON_MAP, null,
                    DiagnosticSeverity.Error));
            return;
        }

        // Check 2: the map key type parameter must be an enum.
        // Flag if: raw Map, unbounded/lower-bounded wildcard, non-enum concrete type,
        // upper-bounded wildcard whose bound is not an enum, or any other key form.
        PsiType[] typeParams = classType.getParameters();
        boolean keyIsEnum = typeParams.length >= 1 && DiagnosticsUtils.isEnumKeyType(typeParams[0]);
        if (!keyIsEnum) {
            diagnostics.add(createDiagnostic(fieldOrProperty, unit,
                    Messages.getMessage("MapKeyEnumeratedOnNonEnumType"),
                    PersistenceConstants.DIAGNOSTIC_CODE_MAPKEYENUMERATED_NON_ENUM, null,
                    DiagnosticSeverity.Error));
        }
    }


    private void collectAccessorDiagnostics(PsiJvmModifiersOwner fieldOrProperty, PsiClass type, PsiJavaFile unit,
                                            List<Diagnostic> diagnostics) {
        String messageKey = null;
        String code = null;
        if (fieldOrProperty instanceof PsiMethod method) {
            String methodName = method.getName();
            boolean isPublic = method.getModifierList().hasModifierProperty(PsiModifier.PUBLIC);
            boolean isStartsWithGet = methodName.startsWith("get");
            boolean isPropertyExist = false;

            if (isStartsWithGet) {
                isPropertyExist = hasField(method, type);
            }
            if (!isPublic) {
                messageKey = "MapKeyAnnotationsInvalidMethodAccessSpecifier";
                code = PersistenceConstants.DIAGNOSTIC_CODE_INVALID_ACCESS_SPECIFIER;
            } else if (!isStartsWithGet) {
                messageKey = "MapKeyAnnotationsOnInvalidMethod";
                code = PersistenceConstants.DIAGNOSTIC_CODE_INVALID_METHOD_NAME;
            } else if (!isPropertyExist) {
                messageKey = "MapKeyAnnotationsFieldNotFound";
                code = PersistenceConstants.DIAGNOSTIC_CODE_FIELD_NOT_EXIST;
            }
            if (messageKey != null) {
                diagnostics.add(createDiagnostic(fieldOrProperty, unit, Messages.getMessage(messageKey),
                        code, null, DiagnosticSeverity.Warning));
            }
        }
    }

    private boolean hasField(PsiMethod method, PsiClass type) {
        String methodName = method.getName();
        // Exclude 'get' from method name and decapitalize the first letter
        String expectedFieldName = (methodName.startsWith("get") && methodName.length() > 3) ? Introspector.decapitalize(methodName.substring(3)) : null;
        PsiField expectedField = StringUtils.isNotBlank(expectedFieldName) ? type.findFieldByName(expectedFieldName, false) : null;
        return expectedField != null;
    }

    private void validateMapKeyTemporalAnnotation(PsiJvmModifiersOwner fieldOrProperty, PsiJavaFile unit,
                                                  List<Diagnostic> diagnostics) {
        // @MapKeyTemporal must only be applied when the map key type is Date or Calendar
        // Specification: https://jakarta.ee/specifications/persistence/3.0/jakarta-persistence-spec-3.0#a15583
        PsiType fieldOrPropertyType = null;

        if (fieldOrProperty instanceof PsiMethod method) {
            fieldOrPropertyType = method.getReturnType();
        } else if (fieldOrProperty instanceof PsiField field) {
            fieldOrPropertyType = field.getType();
        }

        if (fieldOrPropertyType instanceof PsiClassType classType) {
            // Get the Map's key type (first type parameter)
            PsiType[] typeParameters = classType.getParameters();
            String keyTypeName = typeParameters.length > 0 ? typeParameters[0].getCanonicalText() : null;

            // Check if key type is Date or Calendar
            boolean isValidKeyType = PersistenceConstants.UTIL_DATE.equals(keyTypeName) ||
                    PersistenceConstants.UTIL_CALENDAR.equals(keyTypeName);

            if (!isValidKeyType) {
                diagnostics.add(createDiagnostic(fieldOrProperty, unit,
                        Messages.getMessage("MapKeyTemporalNotOnTemporalType"),
                        PersistenceConstants.DIAGNOSTIC_CODE_INVALID_MAPKEYTEMPORAL_TYPE,
                        null, DiagnosticSeverity.Error));
            }
        }
    }
}
