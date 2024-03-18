package io.openliberty.tools.intellij.lsp4mp4ij;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import io.openliberty.tools.intellij.lsp4mp4ij.classpath.ClasspathResourceChangedManager;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import org.jetbrains.annotations.NotNull;

public class MicroProfilePostStartupActivity implements StartupActivity, DumbAware {
    @Override
    public void runActivity(@NotNull Project project) {
        ClasspathResourceChangedManager.getInstance(project);
        // Force the instantiation of the manager to be sure that classpath listener
        // are registered before QuarkusLanguageClient classpath listener
        // When an application.properties changed
        // - the manager need to update the properties cache
        // - and after the QuarkusLanguageClient throws an event to trigger Java validation.
        // As java validation requires the properties cache, it needs that cache must be updated before.
        PsiMicroProfileProjectManager.getInstance(project);
    }
}