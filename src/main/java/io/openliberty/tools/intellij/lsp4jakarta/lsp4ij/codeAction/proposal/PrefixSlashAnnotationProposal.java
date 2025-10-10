package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.Change;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.corrections.proposal.ChangeCorrectionProposal;

public class PrefixSlashAnnotationProposal extends ChangeCorrectionProposal {

    private final PsiFile fSourceCU;
    private final PsiElement declaringNode;

    public PrefixSlashAnnotationProposal(String name, String kind, int relevance, PsiFile sourceCU, PsiElement declaringNode) {
        super(name, kind, relevance);
        this.fSourceCU=sourceCU;
        this.declaringNode=declaringNode;
    }

    @Override
    public Change getChange() {
        return null;
    }
}
