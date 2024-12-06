 /*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
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
 package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix;

 import com.intellij.psi.*;
 import com.intellij.psi.util.PsiTreeUtil;
 import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
 import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
 import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.RemoveAnnotationsProposal;
 import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
 import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
 import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
 import io.openliberty.tools.intellij.util.ExceptionUtil;
 import org.eclipse.lsp4j.CodeAction;
 import org.eclipse.lsp4j.Diagnostic;
 import org.eclipse.lsp4mp.commons.codeaction.CodeActionResolveData;

 import java.util.*;
 import java.util.logging.Logger;
 import java.util.logging.Level;

 /**
  * QuickFix for removing parameter annotations
  */
public abstract class RemoveParamAnnotationQuickFix implements IJavaCodeActionParticipant {

     private final String[] annotations;
     private static final Logger LOGGER = Logger.getLogger(RemoveParamAnnotationQuickFix.class.getName());
     private static final String ANNOTATION_TO_REMOVE = "annotationsToRemove";
     private static final String INDEX_KEY = "paramIndex";
	
    public RemoveParamAnnotationQuickFix(String ...annotations) {
        this.annotations = annotations;
    }

    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {

        final PsiElement node = context.getCoveredNode();
        final PsiMethod method = PsiTreeUtil.getParentOfType(node, PsiMethod.class);

        final List<CodeAction> codeActions = new ArrayList<>();
        final PsiParameterList parameters = method.getParameterList();
        int parametersCount = parameters.getParametersCount();
        for (int i = 0; i < parametersCount; ++i) {
            final PsiParameter parameter = parameters.getParameter(i);
            final PsiAnnotation[] psiAnnotations = parameter.getAnnotations();
            final List<String> annotationsToRemove = new ArrayList<>();
            // Search for annotations to remove from the current method parameter.
            if (psiAnnotations != null) {
                Arrays.stream(psiAnnotations).forEach(a -> {
                    if (Arrays.stream(annotations).anyMatch(m -> m.equals(a.getQualifiedName()))) {
                        annotationsToRemove.add(a.getQualifiedName());
                    }
                });
            }
            if (!annotationsToRemove.isEmpty()) {
                // Create label
                String label = getLabel(parameter, annotationsToRemove);
                Map<String, Object> extendedData = new HashMap<>();
                extendedData.put(ANNOTATION_TO_REMOVE, annotationsToRemove);
                extendedData.put(INDEX_KEY, i);
                codeActions.add(JDTUtils.createCodeAction(context, diagnostic, label, getParticipantId(), extendedData));
            }
        }
        return codeActions;
    }
    private String getLabel(PsiParameter parameter,List<String> annotationsToRemove) {
        final StringBuilder sb = new StringBuilder();
        // Java annotations in comma delimited list, assume that is ok.
        sb.append("@").append(getShortName(annotationsToRemove.get(0)));
        for (int j = 1; j < annotationsToRemove.size(); ++j) {
            sb.append(", @").append(getShortName(annotationsToRemove.get(j)));
        }
        return Messages.getMessage("RemoveTheModifierFromParameter", sb.toString(), parameter.getName().toString());
    }

     @Override
     public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
         final PsiElement node = context.getCoveredNode();
         final CodeAction toResolve = context.getUnresolved();
         final PsiClass parentType = getBinding(node);
         final PsiMethod method = PsiTreeUtil.getParentOfType(node, PsiMethod.class);
         CodeActionResolveData data = (CodeActionResolveData) toResolve.getData();
         List<String> annotationsToRemove;
         int paramIndex;

         if (data.getExtendedDataEntry(ANNOTATION_TO_REMOVE) instanceof List &&
                 data.getExtendedDataEntry(INDEX_KEY) instanceof Number) {
             annotationsToRemove = (List<String>) data.getExtendedDataEntry(ANNOTATION_TO_REMOVE);
             paramIndex = ((Number) data.getExtendedDataEntry(INDEX_KEY)).intValue();
         } else {
             LOGGER.log(Level.WARNING, "The CodeActionResolveData was corrupted somewhere in the LSP layer. Unable to resolve code action.");
             return null;
         }

         final PsiParameter parameter = method.getParameterList().getParameter(paramIndex);
         final PsiAnnotation[] psiAnnotations = parameter.getAnnotations();
         final List<PsiAnnotation> psiAnnotationsToRemove = new ArrayList<>();

         if (psiAnnotations != null) {
             Arrays.stream(psiAnnotations).forEach(a -> {
                 if (annotationsToRemove.stream().anyMatch(m -> m.equals(a.getQualifiedName()))) {
                     psiAnnotationsToRemove.add(a);
                 }
             });
         }
         assert parentType != null;
         String label = getLabel(parameter, annotationsToRemove);
         RemoveAnnotationsProposal proposal = new RemoveAnnotationsProposal(label, context.getSource().getCompilationUnit(),
                 context.getASTRoot(), parentType, 0, psiAnnotationsToRemove);

         ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER, "Unable to create workspace edit for code action to extend the HttpServlet class");
         return toResolve;
     }

    protected PsiClass getBinding(PsiElement node) {
        return PsiTreeUtil.getParentOfType(node, PsiClass.class);
    }

    protected static String getShortName(String qualifiedName) {
        final int i = qualifiedName.lastIndexOf('.');
        if (i != -1) {
            return qualifiedName.substring(i+1);
        }
        return qualifiedName;
    }
}
