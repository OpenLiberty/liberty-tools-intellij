package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.quickfix;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.PrefixSlashAnnotationProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;
import io.openliberty.tools.intellij.util.ExceptionUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4mp.commons.codeaction.CodeActionResolveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class PrefixSlashAnnotationQuickFix implements IJavaCodeActionParticipant {

    private static final Logger LOGGER = Logger.getLogger(PrefixSlashAnnotationQuickFix.class.getName());

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
        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        PsiElement declaringNode = getBinding(context.getCoveredNode());
        PsiAnnotation annotationNode = PsiTreeUtil.getParentOfType(node, PsiAnnotation.class);
        String label = getLabel();
        ChangeCorrectionProposal proposal = new PrefixSlashAnnotationProposal(label, 0, context.getSource().getCompilationUnit(), declaringNode, annotationNode);
        ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER, "Unable to create workspace edit for code action " + label);
        return toResolve;
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
