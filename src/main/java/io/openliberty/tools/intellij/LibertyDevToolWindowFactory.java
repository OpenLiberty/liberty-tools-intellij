package io.openliberty.tools.intellij;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

import static io.openliberty.tools.intellij.util.Constants.LibertyRB;

final class LibertyDevToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        // build Tree View
        LibertyExplorer explorer = new LibertyExplorer(project);
        ContentManager contentManager = toolWindow.getContentManager();
        Content content = contentManager.getFactory().createContent(explorer,
                LibertyRB.getString("liberty.tool.window.display.name"), false);
        contentManager.addContent(content);

    }
}
