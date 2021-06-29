package io.openliberty.tools.intellij.actions;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;

public class ViewEffectivePom extends LibertyGeneralAction {

    @Override
    protected void executeLibertyAction() {
        setActionCmd("view effective POM");
        // open build file
        FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, buildFile), true);
    }

}