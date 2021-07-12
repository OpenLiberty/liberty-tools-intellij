package io.openliberty.tools.intellij.actions;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;

public class ViewGradleConfig extends LibertyGeneralAction {

    public ViewGradleConfig() {
        setActionCmd("view Gradle configuration file");
    }

    @Override
    protected void executeLibertyAction() {
        // open build file
        FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, buildFile), true);
    }

}