package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4mp.commons.codeaction.CodeActionResolveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PrefixSlashAnnotationQuickFix implements IJavaCodeActionParticipant {
    /**
     * Returns the unique identifier of this code action participant.
     *
     * @return the unique identifier of this code action participant
     */
    @Override
    public String getParticipantId() {
        return PrefixSlashAnnotationQuickFix.class.getName();
    }

    /**
     * Return the code action list for a given compilation unit and null otherwise.
     *
     * @param context    the java code action context.
     * @param diagnostic the diagnostic which must be fixed and null otherwise.
     * @return the code action list for a given compilation unit and null otherwise.
     */
    @Override
    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        PsiElement node = context.getCoveredNode();
        PsiElement parentType = getBinding(node);
        if (parentType != null) {
            List<CodeAction> codeActions = new ArrayList<>();
            codeActions.add(JDTUtils.createCodeAction(context, diagnostic, getLabel(), getParticipantId()));
            return codeActions;
        }
        return Collections.emptyList();

    }

    /**
     * Returns the code action with the TextEdits filled in.
     *
     * @param context the code action context to resolve
     * @return the code action with the TextEdits filled in
     */
    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {
        System.out.println("Inside resolveCodeAction------------------");
        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        final PsiClass parentType = PsiTreeUtil.getParentOfType(node, PsiClass.class);
        CodeActionResolveData data = (CodeActionResolveData) toResolve.getData();
        String name = toResolve.getTitle();
        PsiElement declaringNode = getBinding(context.getCoveredNode());
        PsiAnnotation annotationNode = PsiTreeUtil.getParentOfType(node, PsiAnnotation.class);




        if (annotationNode instanceof PsiAnnotation) {
            System.out.println("Its an annotation");
            System.out.println("Annotation: " + annotationNode.getQualifiedName());

            for (PsiNameValuePair pair : annotationNode.getParameterList().getAttributes()) {
                String pname = pair.getName(); // may be null if it's the default attribute
                PsiAnnotationMemberValue value = pair.getValue();

                String valueText = value != null ? value.getText() : "null";
                System.out.println((pname != null ? pname : "value") + " = " + valueText);
            }

            //var parameters = annotationNode.getParameterList();
            //var values = parameters.getAttributes();

            //System.out.println("Values="+values);
        }


        System.out.println("Node----"+node.toString());
        System.out.println("name----"+name);
        System.out.println("declaringNode----"+declaringNode);

        return null;
    }

    protected static PsiElement getBinding(PsiElement node) {
        PsiElement parent = PsiTreeUtil.getParentOfType(node, PsiModifierListOwner.class);
        if (parent != null) {
            return parent;
        }
        return PsiTreeUtil.getParentOfType(node, PsiClass.class);
    }

    protected String getLabel() {
        return Messages.getMessage("PrefixSlashToValueAttribute"); // uses Java syntax
    }
}
