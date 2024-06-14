/*******************************************************************************
 * Copyright (c) 2023 Red Hat, Inc.
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
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import io.openliberty.tools.intellij.lsp4mp4ij.classpath.ClasspathResourceChangedManager;
import io.openliberty.tools.intellij.util.LibertyToolPluginDisposable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Quarkus deployment support provides the capability to collect, download and add to a module classpath the Quarkus deployment dependencies.
 */
public class MicroProfileDeploymentSupport implements ClasspathResourceChangedManager.Listener, Disposable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicroProfileDeploymentSupport.class);
    private static final Key<CompletableFuture<Void>> MICROPROFILE_DEPLOYMENT_SUPPORT_KEY = new Key<>(MicroProfileDeploymentSupport.class.getName());

    private final MessageBusConnection connection;
    private final Project project;

    public static MicroProfileDeploymentSupport getInstance(Project project) {
        return ServiceManager.getService(project, MicroProfileDeploymentSupport.class);
    }

    public MicroProfileDeploymentSupport(Project project) {
        this.project = project;
        connection = project.getMessageBus().connect(LibertyToolPluginDisposable.getInstance(project));
        connection.subscribe(ClasspathResourceChangedManager.TOPIC, this);
    }

    @Override
    public void dispose() {
        connection.dispose();
        cancelFutures();
    }

    @Override
    public void librariesChanged() {
        cancelFutures();
    }

    private void cancelFutures() {
        if (!project.isDisposed()) {
            for (var module : ModuleManager.getInstance(project).getModules()) {
                CompletableFuture<Void> loader = module.getUserData(MICROPROFILE_DEPLOYMENT_SUPPORT_KEY);
                if (loader != null) {
                    loader.cancel(true);
                    module.putUserData(MICROPROFILE_DEPLOYMENT_SUPPORT_KEY, null);
                }
            }
        }
    }

    @Override
    public void sourceFilesChanged(Set<Pair<VirtualFile, Module>> sources) {
        // Do nothing
    }

}
