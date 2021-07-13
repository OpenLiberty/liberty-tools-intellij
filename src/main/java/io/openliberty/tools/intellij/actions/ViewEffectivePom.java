package io.openliberty.tools.intellij.actions;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;

public class ViewEffectivePom extends LibertyGeneralAction {

    public ViewEffectivePom() {
        setActionCmd("view effective POM");
    }

    @Override
    protected void executeLibertyAction() {
        // open build file
        FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, buildFile), true);
    }

}