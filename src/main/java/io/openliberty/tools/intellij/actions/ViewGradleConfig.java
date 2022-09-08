package io.openliberty.tools.intellij.actions;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;

import static io.openliberty.tools.intellij.util.Constants.LibertyRB;

public class ViewGradleConfig extends LibertyGeneralAction {

    public ViewGradleConfig() {
        setActionCmd(LibertyRB.getString("view.gradle.config.file"));
    }

    @Override
    protected void executeLibertyAction() {
        // open build file
        FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, buildFile), true);
    }

}