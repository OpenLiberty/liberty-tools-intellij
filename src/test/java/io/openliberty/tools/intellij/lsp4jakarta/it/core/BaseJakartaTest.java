/*******************************************************************************
* Copyright (c) 2019, 2025 Red Hat Inc. and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
* which is available at https://www.apache.org/licenses/LICENSE-2.0.
*
* SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
*
* Contributors:
*     Red Hat Inc. - initial API and implementation
*******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.it.core;

import com.intellij.maven.testFramework.MavenImportingTestCase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.testFramework.IndexingTestUtil;
import com.intellij.testFramework.PlatformTestUtil;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Modified from:
 * https://github.com/eclipse/lsp4mp/blob/bc926f75df2ca103d78c67b997c87adb7ab480b1/microprofile.jdt/org.eclipse.lsp4mp.jdt.test/src/main/java/org/eclipse/lsp4mp/jdt/core/BasePropertiesManagerTest.java
 * With certain methods modified or deleted to fit the purposes of LSP4Jakarta
 *
 */
public abstract class BaseJakartaTest extends MavenImportingTestCase {

    private static AtomicInteger counter = new AtomicInteger(0);

    /**
     * Create a new module into the test project from existing project folder.
     *
     * @param projectDirs the project folders
     * @return the created modules
     */
    protected List<Module> createMavenModules(List<File> projectDirs) throws Exception {
        Project project = getProject();
        List<VirtualFile> pomFiles = new ArrayList<>();
        for (File projectDir : projectDirs) {
            File moduleDir = new File(project.getBasePath(), projectDir.getName() + counter.getAndIncrement());
            FileUtils.copyDirectory(projectDir, moduleDir);
            VirtualFile pomFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(moduleDir).findFileByRelativePath("pom.xml");
            pomFiles.add(pomFile);
        }
        // importProjects() internally calls runBlockingMaybeCancellable{} which deadlocks if called from the
        // EDT (the test thread). Run it on a pooled thread and wait for completion so the EDT stays free
        // to process events dispatched by the Maven sync coroutines.
        VirtualFile[] pomArray = pomFiles.toArray(VirtualFile[]::new);
        AtomicReference<Exception> importError = new AtomicReference<>();
        Future<?> importFuture = ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                importProjects(pomArray);
            } catch (Exception e) {
                importError.set(e);
            }
        });
        // waitForFuture pumps the IDE event queue while blocking on EDT, preventing deadlock
        // with Maven sync coroutines that need to dispatch events back to the EDT.
        PlatformTestUtil.waitForFuture(importFuture);
        if (importError.get() != null) {
            throw importError.get();
        }
        Module[] modules = ModuleManager.getInstance(getProject()).getModules();
        for (Module module : modules) {
            setupJdkForModule(module.getName());
        }
        // Starting from IntelliJ 2024.2, indexing runs asynchronously in a background thread, https://plugins.jetbrains.com/docs/intellij/testing-faq.html#how-to-handle-indexing.
        // Use the following method to ensure indexes are fully populated before proceeding.
        IndexingTestUtil.waitUntilIndexesAreReady(project);
        // REVISIT: After calling setupJdkForModule() initialization appears to continue in the background
        // and a may cause a test to intermittently fail if it accesses the module too early. A 10-second wait
        // is hopefully long enough but would be preferable to synchronize on a completion event if one is
        // ever introduced in the future.
        Thread.sleep(10000L);
        // QuarkusProjectService.getInstance(myTestFixture.getProject()).processModules();
        return new ArrayList<>(Arrays.asList(modules));
    }

    protected Module createMavenModule(File projectDir) throws Exception {
        List<Module> modules = createMavenModules(Collections.singletonList(projectDir));
        return modules.get(modules.size() - 1);
    }

    @Override
    public void setUp() {
        try {
            // Call super.setUp() to initialize MavenTestCase infrastructure (myProject, myDir, projects manager, etc.).
            super.setUp();
            ApplicationManager.getApplication().runWriteAction(() ->
                    LanguageLevelProjectExtension.getInstance(getProject()).setLanguageLevel(LanguageLevel.JDK_1_6));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void tearDown() {
        try {
            super.tearDown();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
