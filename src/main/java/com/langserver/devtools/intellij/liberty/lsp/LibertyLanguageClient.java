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
package com.langserver.devtools.intellij.liberty.lsp;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import com.langserver.devtools.intellij.lsp4mp4ij.psi.core.project.PsiMicroProfileProjectManager;
import com.langserver.devtools.intellij.lsp4mp.MicroProfileModuleUtil;
import com.langserver.devtools.intellij.lsp4mp.MicroProfileProjectService;
import com.langserver.devtools.intellij.lsp4mp.lsp4ij.LanguageClientImpl;
import com.langserver.devtools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import io.openliberty.tools.langserver.api.LibertyLanguageClientAPI;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesChangeEvent;
import org.eclipse.lsp4mp.commons.MicroProfilePropertiesScope;
import org.eclipse.lsp4mp.ls.api.MicroProfileLanguageServerAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Client for Liberty language server
 * Adapted from https://github.com/redhat-developer/intellij-quarkus/blob/2585eb422beeb69631076d2c39196d6eca2f5f2e/src/main/java/com/redhat/devtools/intellij/quarkus/lsp/QuarkusLanguageClient.java
 */
public class LibertyLanguageClient extends LanguageClientImpl implements LibertyLanguageClientAPI, MicroProfileProjectService.Listener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibertyLanguageClient.class);

    public LibertyLanguageClient(Project project) {
        super(project);
    }

    @Override
    public void libraryUpdated(Library library) {
        // not needed for Liberty LS
    }

    @Override
    public void sourceUpdated(List<Pair<Module, VirtualFile>> sources) {
        // not needed for Liberty LS
    }
}
