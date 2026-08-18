/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.tools.intellij;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;
import org.jetbrains.annotations.NotNull;

final class LibertyDevToolWindowFactory implements ToolWindowFactory {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        // build Tree View
        LibertyExplorer explorer = new LibertyExplorer(project);
        ContentManager contentManager = toolWindow.getContentManager();
        Content content = contentManager.getFactory().createContent(explorer,
                LocalizedResourceUtil.getMessage("liberty.tool.window.display.name"), false);
        contentManager.addContent(content);

    }
}
