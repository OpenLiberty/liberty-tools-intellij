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
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.lemminx.customservice.XMLLanguageClientAPI;
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
 * Client for LemMinX language server and Liberty LemMinX ext
 * Adapted from https://github.com/redhat-developer/intellij-quarkus/blob/2585eb422beeb69631076d2c39196d6eca2f5f2e/src/main/java/com/redhat/devtools/intellij/quarkus/lsp/QuarkusLanguageClient.java
 */
public class LibertyXmlLanguageClient extends LanguageClientImpl implements XMLLanguageClientAPI, MicroProfileProjectService.Listener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LibertyXmlLanguageClient.class);

    private static final String JAVA_FILE_EXTENSION = "java";

    public LibertyXmlLanguageClient(Project project) {
        super(project);
    }

    private void sendPropertiesChangeEvent(List<MicroProfilePropertiesScope> scope, Set<String> uris) {
        MicroProfileLanguageServerAPI server = (MicroProfileLanguageServerAPI) getLanguageServer();
        if (server != null) {
            MicroProfilePropertiesChangeEvent event = new MicroProfilePropertiesChangeEvent();
            event.setType(scope);
            event.setProjectURIs(uris);
            server.propertiesChanged(event);
        }
    }

    @Override
    public void libraryUpdated(Library library) {
        sendPropertiesChangeEvent(Collections.singletonList(MicroProfilePropertiesScope.dependencies), MicroProfileModuleUtil.getModulesURIs(getProject()));
    }

    @Override
    public void sourceUpdated(List<Pair<Module, VirtualFile>> sources) {
        List<Pair<String, MicroProfilePropertiesScope>> info = sources.stream().filter(pair -> isJavaFile(pair.getRight()) || isConfigSource(pair.getRight(), pair.getLeft())).map(pair -> Pair.of(PsiUtilsLSImpl.getProjectURI(pair.getLeft()), getScope(pair.getRight()))).collect(Collectors.toList());
        if (!info.isEmpty()) {
            sendPropertiesChangeEvent(info.stream().map(Pair::getRight).collect(Collectors.toList()), info.stream().map(Pair::getLeft).collect(Collectors.toSet()));
        }
    }

    private MicroProfilePropertiesScope getScope(VirtualFile file) {
        return isJavaFile(file) ? MicroProfilePropertiesScope.sources : MicroProfilePropertiesScope.configfiles;
    }

    private boolean isJavaFile(VirtualFile file) {
        return JAVA_FILE_EXTENSION.equals(file.getExtension());
    }

    private boolean isConfigSource(VirtualFile file, Module project) {
        return PsiMicroProfileProjectManager.getInstance(project.getProject()).isConfigSource(file);
    }


    <R> CompletableFuture<R> runAsBackground(String title, Supplier<R> supplier) {
        CompletableFuture<R> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            Runnable task = () -> ProgressManager.getInstance().runProcess(() -> {
                try {
                    future.complete(supplier.get());
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            }, new EmptyProgressIndicator());
            if (DumbService.getInstance(getProject()).isDumb()) {
                DumbService.getInstance(getProject()).runWhenSmart(task);
            } else {
                task.run();
            }
        });
        return future;
    }
}
