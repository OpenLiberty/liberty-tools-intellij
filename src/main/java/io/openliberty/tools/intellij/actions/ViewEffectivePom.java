package io.openliberty.tools.intellij.actions;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;

public class ViewEffectivePom extends LibertyGeneralAction {

    public ViewEffectivePom() {
        setActionCmd(LocalizedResourceUtil.getMessage("view.effective.pom"));
    }

    @Override
    protected void executeLibertyAction() {
        // open build file
        FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, buildFile), true);
    }

}