/*******************************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package io.openliberty.tools.intellij.liberty.lsp;

import java.util.List;

import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.FileChangeType;
import org.eclipse.lsp4j.FileEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;

import io.openliberty.tools.intellij.lsp4mp.lsp4ij.LanguageClientImpl;
import io.openliberty.tools.intellij.lsp4mp.lsp4ij.LanguageServerWrapper;

/**
 * Client for Liberty language server
 * Adapted from https://github.com/redhat-developer/intellij-quarkus/blob/2585eb422beeb69631076d2c39196d6eca2f5f2e/src/main/java/com/redhat/devtools/intellij/quarkus/lsp/QuarkusLanguageClient.java
 */
public class LibertyConfigLanguageClient extends LanguageClientImpl implements LibertyCustomConfigManager.Listener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibertyConfigLanguageClient.class);

    private final MessageBusConnection connection;

    public LibertyConfigLanguageClient(Project project) {
        super(project);
        connection = project.getMessageBus().connect(project);
        connection.subscribe(LibertyCustomConfigManager.TOPIC, this);
        LibertyCustomConfigManager.getInstance(project);
    }

    @Override
    public void processConfigXml(List<String> uris) {
        LanguageServerWrapper wrapper = getLanguageServerWrapper();
        if (wrapper != null) {
            List<FileEvent> fileEvents = uris.stream()
                    .map(uri -> new FileEvent(uri, FileChangeType.Changed)).toList();
            DidChangeWatchedFilesParams params = new DidChangeWatchedFilesParams();
            params.setChanges(fileEvents);
            wrapper.getInitializedServer().thenAccept(ls -> ls.getWorkspaceService().didChangeWatchedFiles(params));
        }
    }
}
