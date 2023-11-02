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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.lsp4mp.MicroProfileProjectService;
import org.microshed.lsp4ij.LanguageClientImpl;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.lemminx.customservice.XMLLanguageClientAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Client for LemMinX language server and Liberty LemMinX ext
 * Adapted from https://github.com/redhat-developer/intellij-quarkus/blob/2585eb422beeb69631076d2c39196d6eca2f5f2e/src/main/java/com/redhat/devtools/intellij/quarkus/lsp/QuarkusLanguageClient.java
 */
public class LibertyXmlLanguageClient extends LanguageClientImpl implements XMLLanguageClientAPI, MicroProfileProjectService.Listener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibertyXmlLanguageClient.class);

    public LibertyXmlLanguageClient(Project project) {
        super(project);
    }

    @Override
    public void libraryUpdated(Library library) {
        // not needed for LemMinX LS
    }

    @Override
    public void sourceUpdated(List<Pair<Module, VirtualFile>> sources) {
        // not needed for LemMinX LS
    }
}
