package io.openliberty.tools.intellij.actions;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;

import static io.openliberty.tools.intellij.util.Constants.LibertyRB;

public class ViewEffectivePom extends LibertyGeneralAction {

    public ViewEffectivePom() {
        setActionCmd(LibertyRB.getString("view.effective.pom"));
    }

    @Override
    protected void executeLibertyAction() {
        // open build file
        FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, buildFile), true);
    }

}