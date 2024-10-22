/*******************************************************************************
 * Copyright (c) 2020, 2024 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *     IBM Corporation
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.DeleteAnnotationProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.util.ExceptionUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4mp.commons.codeaction.CodeActionResolveData;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * QuickFix for removing annotations. Modified from
 * https://github.com/eclipse/lsp4mp/blob/6f2d700a88a3262e39cc2ba04beedb429e162246/microprofile.jdt/org.eclipse.lsp4mp.jdt.core/src/main/java/org/eclipse/lsp4mp/jdt/core/java/codeaction/InsertAnnotationMissingQuickFix.java
 *
 * @author Angelo ZERR
 *
 */
public abstract class RemoveAnnotationConflictQuickFix implements IJavaCodeActionParticipant {

    private static final String ANNOTATIONS_TO_REMOVE = "annotationsToRemove";
    private final String[] annotations;
    protected final boolean generateOnlyOneCodeAction;

    private static final Logger LOGGER = Logger.getLogger(RemoveAnnotationConflictQuickFix.class.getName());

    /**
     * Constructor for remove annotation quick fix.
     *
     * <p>
     * The participant will generate a CodeAction per annotation.
     * </p>
     *
     * @param annotations list of annotation to insert.
     */
    public RemoveAnnotationConflictQuickFix(String... annotations) {
        this(false, annotations);
    }

    /**
     * Constructor for remove annotation quick fix.
     *
     * @param generateOnlyOneCodeAction true if the participant must generate a
     *                                  CodeAction which insert the list of
     *                                  annotation and false otherwise.
     * @param annotations               list of annotation to insert.
     */
    public RemoveAnnotationConflictQuickFix(boolean generateOnlyOneCodeAction, String... annotations) {
        this.generateOnlyOneCodeAction = generateOnlyOneCodeAction;
        this.annotations = annotations;
    }


    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        PsiElement node = context.getCoveredNode();
        PsiElement parentType = getBinding(node);
        if (parentType != null) {
            List<CodeAction> codeActions = new ArrayList<>();
            removeAnnotations(diagnostic, context, codeActions);
            return codeActions;
        }
        return Collections.emptyList();

    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        final PsiClass parentType = PsiTreeUtil.getParentOfType(node, PsiClass.class);
        CodeActionResolveData data = (CodeActionResolveData) toResolve.getData();

        List<String> resolveAnnotations = new ArrayList<>();
        if (data.getExtendedDataEntry(ANNOTATIONS_TO_REMOVE) instanceof List) {
            resolveAnnotations = (List<String>) data.getExtendedDataEntry(ANNOTATIONS_TO_REMOVE);
        }
        String[] resolveAnnotationsArray = resolveAnnotations.toArray(String[]::new);
        String name = toResolve.getTitle();
        PsiElement declaringNode = getBinding(context.getCoveredNode());
        ChangeCorrectionProposal proposal = new DeleteAnnotationProposal(name, context.getSource().getCompilationUnit(),
                context.getASTRoot(), parentType, 0, declaringNode, resolveAnnotationsArray);

        Boolean success = ExceptionUtil.executeWithExceptionHandling(
                () -> {
                    WorkspaceEdit we = context.convertToWorkspaceEdit(proposal);
                    toResolve.setEdit(we);
                    return true;
                },
                e -> LOGGER.log(Level.WARNING, "Unable to create workspace edit for code action to remove annotation", e)
        );
        if (success == null || !success) {
            System.out.println("An error occurred during the code action resolution.");
        }
        return toResolve;
    }

    protected void removeAnnotations(Diagnostic diagnostic, JavaCodeActionContext context,
                                     List<CodeAction> codeActions) {
        if (generateOnlyOneCodeAction) {
            removeAnnotation(diagnostic, context, codeActions, annotations);
        } else {
            for (String annotation : annotations) {
                removeAnnotation(diagnostic, context, codeActions, annotation);
            }
        }
    }

    protected void removeAnnotation(Diagnostic diagnostic, JavaCodeActionContext context,
                                    List<CodeAction> codeActions, String... annotations) {
        // Remove the annotation and the proper import by using JDT Core Manipulation
        // API
        String name = getLabel(annotations);
        Map<String, Object> extendedData = new HashMap<>();
        extendedData.put(ANNOTATIONS_TO_REMOVE, Arrays.asList(annotations));
        codeActions.add(JDTUtils.createCodeAction(context, diagnostic, name, getParticipantId(), extendedData));
    }

    protected static PsiElement getBinding(PsiElement node) {
        PsiElement parent = PsiTreeUtil.getParentOfType(node, PsiModifierListOwner.class);
        if (parent != null) {
            return parent;
        }
        return PsiTreeUtil.getParentOfType(node, PsiClass.class);
    }

    protected String[] getAnnotations() {
        return this.annotations;
    }

    protected List<String> getFQAnnotationNames(Project p, String annotationName) {
        // Look up short names on the classpath to find FQnames. Multiple classes differ in package names.
        PsiShortNamesCache cache = PsiShortNamesCache.getInstance(p);
        PsiClass[] classes = cache.getClassesByName(annotationName, GlobalSearchScope.allScope(p));
        return Arrays.stream(classes).map(PsiClass::getQualifiedName).collect(Collectors.toList());
    }

    private static String getLabel(String[] annotations) {
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < annotations.length; i++) {
            String annotation = annotations[i];
            String annotationName = annotation.substring(annotation.lastIndexOf('.') + 1, annotation.length());
            if (i > 0) {
                list.append(", "); // assume comma list is ok: @A, @B, @C
            }
            list.append("@"); // Java syntax
            list.append(annotationName);
        }
        return Messages.getMessage("RemoveItem", list.toString());
    }
}
