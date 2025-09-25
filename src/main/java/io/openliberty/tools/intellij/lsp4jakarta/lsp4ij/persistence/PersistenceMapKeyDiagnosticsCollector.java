/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation, Ankush Sharma and others.
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
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

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
        boolean hasTypeDiagnostics = false;
        PsiAnnotation[] allAnnotations = fieldOrProperty.getAnnotations();
        for (PsiAnnotation annotation : allAnnotations) {
            String matchedAnnotation = getMatchedJavaElementName(type, annotation.getQualifiedName(),
                    PersistenceConstants.SET_OF_PERSISTENCE_ANNOTATIONS);
            if (matchedAnnotation != null) {
                if (PersistenceConstants.MAPKEY.equals(matchedAnnotation))
                    hasMapKeyAnnotation = true;
                else if (PersistenceConstants.MAPKEYCLASS.equals(matchedAnnotation))
                    hasMapKeyClassAnnotation = true;
                else if (PersistenceConstants.MAPKEYJOINCOLUMN.equals(matchedAnnotation)) {
                    mapKeyJoinCols.add(annotation);
                }
            }
        }
        if (hasMapKeyAnnotation) {
            hasTypeDiagnostics = collectTypeDiagnostics(fieldOrProperty,"@MapKey",unit,diagnostics);
            collectAccessorDiagnostics(fieldOrProperty,type,unit,diagnostics);
        }
        if (hasMapKeyClassAnnotation) {
            hasTypeDiagnostics = collectTypeDiagnostics(fieldOrProperty,"@MapKeyClass",unit,diagnostics);
            collectAccessorDiagnostics(fieldOrProperty,type,unit,diagnostics);
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
        // If we have multiple MapKeyJoinColumn annotations on a single field or property we must
        // ensure each has a name and referencedColumnName
        if (mapKeyJoinCols.size() > 1) {
            validateMapKeyJoinColumnAnnotations(mapKeyJoinCols, fieldOrProperty, unit, diagnostics);
        }
    }

    private boolean collectTypeDiagnostics(PsiJvmModifiersOwner fieldOrProperty,String attribute, PsiJavaFile unit,
                                           List<Diagnostic> diagnostics){
        boolean hasTypeDiagnostics = false;
        PsiType FPType = null;
        boolean isMapOrSubtype = false;
        String messageKey = null;
        String code = null;

        if(fieldOrProperty instanceof PsiMethod method){
            FPType = method.getReturnType();
            messageKey = "MapKeyAnnotationsReturnTypeOfMethod";
            code = PersistenceConstants.DIAGNOSTIC_CODE_INVALID_RETURN_TYPE;
        }else if(fieldOrProperty instanceof PsiField field){
            FPType = field.getType();
            messageKey = "MapKeyAnnotationsTypeOfField";
            code = PersistenceConstants.DIAGNOSTIC_CODE_INVALID_TYPE;
        }

        if (FPType instanceof PsiClassType classType) {
            PsiClass psiClass = classType.resolve();
            isMapOrSubtype = InheritanceUtil.isInheritor(psiClass, "java.util.Map");
        }

        if(!isMapOrSubtype){
            hasTypeDiagnostics = true;
            diagnostics.add(createDiagnostic(fieldOrProperty, unit, Messages.getMessage(messageKey,attribute),
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

    private void collectAccessorDiagnostics(PsiJvmModifiersOwner fieldOrProperty,PsiClass type, PsiJavaFile unit,
                                            List<Diagnostic> diagnostics)  {
        String messageKey = null;
        String code = null;
        if(fieldOrProperty instanceof PsiMethod method){
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

    private boolean hasField(PsiMethod method, PsiClass type ) {
        boolean isPropertyExist = false;
        String methodName = method.getName();
        String expectedFieldName = null;

        // Exclude 'get' from method name and decapitalize the first letter
        if (methodName.startsWith("get") && methodName.length() > 3) {
            String suffix = methodName.substring(3);
            if (suffix.length() == 1) {
                expectedFieldName = suffix.toLowerCase();
            } else {
                expectedFieldName = Character.toLowerCase(suffix.charAt(0)) + suffix.substring(1);
            }
        }
        PsiField expectedField= type.findFieldByName(expectedFieldName, false);
        isPropertyExist = expectedField != null;
        return isPropertyExist;
    }
}
