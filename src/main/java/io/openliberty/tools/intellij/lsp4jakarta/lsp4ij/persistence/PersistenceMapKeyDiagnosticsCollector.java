/*******************************************************************************
 * Copyright (c) 2020, 2023 IBM Corporation, Ankush Sharma and others.
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
            PsiClass[] alltypes;
            PsiAnnotation[] allAnnotations;

            alltypes = unit.getClasses();
            PsiMethod[] methods;
            PsiField[] fields;

            for (PsiClass type : alltypes) {
                methods = type.getMethods();
                for (PsiMethod method : methods) {
                    List<PsiAnnotation> mapKeyJoinCols = new ArrayList<>();
                    boolean hasMapKeyAnnotation = false;
                    boolean hasMapKeyClassAnnotation = false;
                    allAnnotations = method.getAnnotations();
                    for (PsiAnnotation annotation : allAnnotations) {
                        String matchedAnnotation = getMatchedJavaElementName(type, annotation.getQualifiedName(),
                                PersistenceConstants.SET_OF_PERSISTENCE_ANNOTATIONS);
                        if (matchedAnnotation != null) {
                            if (PersistenceConstants.MAPKEY.equals(matchedAnnotation)) {
                                hasMapKeyAnnotation = true;
                            } else if (PersistenceConstants.MAPKEYCLASS.equals(matchedAnnotation)) {
                                hasMapKeyClassAnnotation = true;
                            } else if (PersistenceConstants.MAPKEYJOINCOLUMN.equals(matchedAnnotation)) {
                                mapKeyJoinCols.add(annotation);
                            }
                        }
                    }
                    if (hasMapKeyAnnotation && hasMapKeyClassAnnotation) {
                        // A single field cannot have the same
                        diagnostics.add(createDiagnostic(method, unit,
                                Messages.getMessage("MapKeyAnnotationsNotOnSameField"),
                                PersistenceConstants.DIAGNOSTIC_CODE_INVALID_ANNOTATION, null,
                                DiagnosticSeverity.Error));
                    }
                    // If we have multiple MapKeyJoinColumn annotations on a single method we must
                    // ensure each has a name and referencedColumnName
                    if (mapKeyJoinCols.size() > 1) {
                        validateMapKeyJoinColumnAnnotations(mapKeyJoinCols, method, unit, diagnostics);
                    }
                }

                // Go through each field to ensure they do not have both MapKey and MapKeyColumn
                // Annotations
                fields = type.getFields();
                for (PsiField field : fields) {
                    List<PsiAnnotation> mapKeyJoinCols = new ArrayList<>();
                    boolean hasMapKeyAnnotation = false;
                    boolean hasMapKeyClassAnnotation = false;
                    allAnnotations = field.getAnnotations();
                    for (PsiAnnotation annotation : allAnnotations) {
                        String matchedAnnotation = getMatchedJavaElementName(type, annotation.getQualifiedName(),
                                PersistenceConstants.SET_OF_PERSISTENCE_ANNOTATIONS);
                        if (matchedAnnotation != null) {
                            if (PersistenceConstants.MAPKEY.equals(matchedAnnotation)) {
                                hasMapKeyAnnotation = true;
                            } else if (PersistenceConstants.MAPKEYCLASS.equals(matchedAnnotation)) {
                                hasMapKeyClassAnnotation = true;
                            } else if (PersistenceConstants.MAPKEYJOINCOLUMN.equals(matchedAnnotation)) {
                                mapKeyJoinCols.add(annotation);
                            }
                        }
                    }
                    if (hasMapKeyAnnotation && hasMapKeyClassAnnotation) {
                        // A single field cannot have the same
                        diagnostics.add(createDiagnostic(field, unit,
                                Messages.getMessage("MapKeyAnnotationsNotOnSameField"),
                                PersistenceConstants.DIAGNOSTIC_CODE_INVALID_ANNOTATION, null,
                                DiagnosticSeverity.Error));
                    }
                    if (mapKeyJoinCols.size() > 1) {
                        validateMapKeyJoinColumnAnnotations(mapKeyJoinCols, field, unit, diagnostics);
                    }
                }
            }
        }
    }

    private void validateMapKeyJoinColumnAnnotations(List<PsiAnnotation> annotations, PsiElement element,
                                                     PsiJavaFile unit, List<Diagnostic> diagnostics) {
        String message = element instanceof PsiMethod ?
                Messages.getMessage("MultipleMapKeyJoinColumnMethod") :
                Messages.getMessage("MultipleMapKeyJoinColumnField");
        annotations.forEach(annotation -> {
            boolean allNamesSpecified;
            boolean allReferencedColumnNameSpecified;
            List<PsiNameValuePair> memberValues = Arrays.asList(annotation.getParameterList().getAttributes());
            allNamesSpecified = memberValues.stream()
                    .anyMatch(mv -> mv.getName().equals(PersistenceConstants.NAME));
            allReferencedColumnNameSpecified = memberValues.stream()
                    .anyMatch(mv -> mv.getName().equals(PersistenceConstants.REFERENCEDCOLUMNNAME));
            if (!allNamesSpecified || !allReferencedColumnNameSpecified) {
                diagnostics.add(createDiagnostic(element, unit, message,
                        PersistenceConstants.DIAGNOSTIC_CODE_MISSING_ATTRIBUTES, null, DiagnosticSeverity.Error));
            }
        });
    }}
