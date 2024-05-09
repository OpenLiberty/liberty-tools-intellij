/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp;

import com.intellij.openapi.project.Project;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4mp.ls.api.MicroProfileLanguageServerAPI;
import com.redhat.devtools.lsp4ij.LanguageServerFactory;
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl;
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider;

public class JakartaLanguageServerFactory implements LanguageServerFactory {
    @Override
    public StreamConnectionProvider createConnectionProvider(Project project) {
        return new JakartaLanguageServer();
    }

    @Override
    public LanguageClientImpl createLanguageClient(Project project) {
        return new JakartaLanguageClient(project);
    }

    @Override
    public Class<? extends LanguageServer> getServerInterface() {
        return MicroProfileLanguageServerAPI.class;
    }

}
