package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ASTRewriteCorrectionProposal;
import org.eclipse.lsp4j.CodeActionKind;

public class PrefixSlashAnnotationProposal extends ASTRewriteCorrectionProposal {
    private final PsiAnnotation fAnnotation;

    public PrefixSlashAnnotationProposal(String name, int relevance, PsiFile sourceCU, PsiElement declaringNode, PsiAnnotation annotationNode) {
        super(name, CodeActionKind.QuickFix, declaringNode, relevance, sourceCU);
        this.fAnnotation = annotationNode;
    }

    @Override
    public void performUpdate() {
        final String FORWARD_SLASH = "/";
        final String ESCAPE_QUOTE = "\"";
        if (fAnnotation != null) {
            Project project = fAnnotation.getProject();
            PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);
            for (PsiNameValuePair pair : fAnnotation.getParameterList().getAttributes()) {
                PsiAnnotationMemberValue value = pair.getValue();
                if (value instanceof PsiLiteralExpression literal) {
                    Object literalValue = literal.getValue();
                    String valueText = (String) literalValue;
                    if (valueText != null && !valueText.startsWith(FORWARD_SLASH)) {
                        String finalPath = FORWARD_SLASH + valueText;
                        String literalText = ESCAPE_QUOTE + finalPath + ESCAPE_QUOTE;
                        PsiAnnotationMemberValue newValue = factory.createExpressionFromText(literalText, fAnnotation);
                        fAnnotation.setDeclaredAttributeValue(PsiAnnotation.DEFAULT_REFERENCED_METHOD_NAME, newValue);
                    }
                }
            }
        }
    }
}
