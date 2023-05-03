/*******************************************************************************
 * Copyright (c) 2020, 2023 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package io.openliberty.tools.intellij.lsp4mp.lsp4ij;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Track file opened / closed to start language servers / disconnect file from language servers.
 */
public class ConnectDocumentToLanguageServerSetupParticipant implements ProjectComponent, FileEditorManagerListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectDocumentToLanguageServerSetupParticipant.class);

    private Project project;

    public ConnectDocumentToLanguageServerSetupParticipant(Project project) {
        this.project = project;
    }

    @Override
    public void projectOpened() {
        project.getMessageBus().connect(project).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this);
    }

    @Override
    public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document != null) {
            // Force the start of all languages servers mapped with the given file
            LanguageServiceAccessor.getInstance(source.getProject())
                    .getLanguageServers(document, capabilities -> true);
        }
    }

    @Override
    public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        URI uri = LSPIJUtils.toUri(file);
        if (uri != null) {
            try {
                // Remove the cached file wrapper if needed
                LSPVirtualFileWrapper.dispose(file);
                // Disconnect the given file from all language servers
                LanguageServiceAccessor.getInstance(source.getProject())
                        .getLSWrappers(file, capabilities -> true)
                        .forEach(
                                wrapper -> wrapper.disconnect(uri)
                        );
            } catch (Exception e) {
                LOGGER.warn("Error while disconnecting the file '" + uri + "' from all language servers", e);
            }
        }
    }

}