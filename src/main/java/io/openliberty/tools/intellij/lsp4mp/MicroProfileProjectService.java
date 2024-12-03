/*******************************************************************************
 * Copyright (c) 2020, 2024 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package io.openliberty.tools.intellij.lsp4mp;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;
import io.openliberty.tools.intellij.util.LibertyToolPluginDisposable;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Adapted from https://github.com/redhat-developer/intellij-quarkus/blob/2585eb422beeb69631076d2c39196d6eca2f5f2e/src/main/java/com/redhat/devtools/intellij/quarkus/QuarkusProjectService.java
 */
public class MicroProfileProjectService implements LibraryTable.Listener, BulkFileListener, ModuleListener, Disposable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MicroProfileProjectService.class);

    private final Project project;

    private final Map<Module, MutablePair<VirtualFile, Boolean>> schemas = new ConcurrentHashMap<>();

    private final ExecutorService executor;

    @Override
    public void dispose() {
        executor.shutdown();
    }
    public interface Listener {
        void libraryUpdated(Library library);
        void sourceUpdated(List<Pair<Module, VirtualFile>> sources);
    }

    public static MicroProfileProjectService getInstance(Project project) {
        return project.getService(MicroProfileProjectService.class);
    }

    public static final Topic<Listener> TOPIC = Topic.create(MicroProfileProjectService.class.getName(), Listener.class);

    private final MessageBusConnection connection;

    public MicroProfileProjectService(Project project) {
        this.project = project;
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            this.executor = ConcurrencyUtil.newSameThreadExecutorService();
        } else {
            this.executor = new ThreadPoolExecutor(0, 1,
                    1L, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(),
                    r -> new Thread(r, "Quarkus lib pool " + project.getName()));
        }
        LibraryTablesRegistrar.getInstance().getLibraryTable(project).addListener(this, project);
        connection = ApplicationManager.getApplication().getMessageBus().connect(LibertyToolPluginDisposable.getInstance(project));
        connection.subscribe(VirtualFileManager.VFS_CHANGES, this);
        project.getMessageBus().connect(LibertyToolPluginDisposable.getInstance(project)).subscribe(ModuleListener.TOPIC, this);
    }

    private void handleLibraryUpdate(Library library) {
            project.getMessageBus().syncPublisher(TOPIC).libraryUpdated(library);
            schemas.forEach((module, pair) -> {
                pair.setRight(Boolean.FALSE);
            });
    }

    @Override
    public void afterLibraryAdded(@NotNull Library newLibrary) {
        handleLibraryUpdate(newLibrary);
    }

    @Override
    public void afterLibraryRemoved(@NotNull Library library) {
        handleLibraryUpdate(library);
    }

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        List<Pair<Module, VirtualFile>> pairs = events.stream().map(event -> toPair(event)).filter(Objects::nonNull).collect(Collectors.toList());
        if (!pairs.isEmpty()) {
            pairs.forEach(pair -> schemas.computeIfPresent(pair.getLeft(), (m, p) -> {
                p.setRight(Boolean.FALSE);
                return p;
            }));
            project.getMessageBus().syncPublisher(TOPIC).sourceUpdated(pairs);
        }
    }

    private Pair<Module, VirtualFile> toPair(VFileEvent event) {
        VirtualFile file = event.getFile();
        if (file != null && file.exists() && "java".equalsIgnoreCase(file.getExtension())) {
            Module module = ProjectFileIndex.getInstance(project).getModuleForFile(file);
            if (module != null && (event instanceof VFileCreateEvent || event instanceof VFileContentChangeEvent || event instanceof VFileDeleteEvent)) {
                return Pair.of(module, file);
            }
        }
        return null;
    }

}
