/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 *  SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package io.openliberty.tools.intellij.liberty.lsp;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LibertyCustomConfigManager implements LibraryTable.Listener, Disposable {

    private final static Logger LOGGER = Logger.getInstance(LibertyCustomConfigManager.class);

    private final Project project;
    private final MessageBusConnection appConnection;
    private final LibertyCustomConfigListener listener;

    @Override
    public void dispose() {

    }

    public interface Listener {
        void processConfigXml(List<String> uris);
    }

    public static LibertyCustomConfigManager getInstance(@NotNull Project project) {
        return project.getService(LibertyCustomConfigManager.class);
    }

    public static final Topic<Listener> TOPIC = Topic.create(LibertyCustomConfigManager.class.getName(), Listener.class);


    public LibertyCustomConfigManager(Project project) {
        this.project = project;
        LibraryTablesRegistrar.getInstance().getLibraryTable(project).addListener(this, project);
        listener = new LibertyCustomConfigListener(this);
        appConnection = ApplicationManager.getApplication().getMessageBus().connect(project);
        appConnection.subscribe(VirtualFileManager.VFS_CHANGES, listener);
    }

    protected void handleProcessConfigXml(List<String> uris) {
        project.getMessageBus().syncPublisher(TOPIC).processConfigXml(uris);
    }
}
