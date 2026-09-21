/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4mp.it.core;

import com.intellij.maven.testFramework.MavenImportingTestCase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.testFramework.IndexingTestUtil;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public abstract class BaseMicroProfileTest extends MavenImportingTestCase {

    private static final AtomicInteger counter = new AtomicInteger(0);

    protected TestFixtureBuilder<IdeaProjectTestFixture> myProjectBuilder;

    @Override
    protected void setUpFixtures() throws Exception {
        myProjectBuilder = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getName());
        final JavaTestFixtureFactory factory = JavaTestFixtureFactory.getFixtureFactory();
        myProjectBuilder.addModule(JavaModuleFixtureBuilder.class);
        IdeaProjectTestFixture fixture = factory.createCodeInsightFixture(myProjectBuilder.getFixture());
        setTestFixture(fixture);
        fixture.setUp();
        LanguageLevelProjectExtension.getInstance(fixture.getProject()).setLanguageLevel(LanguageLevel.JDK_11);
    }

    protected Module createMavenModule(File projectDir) throws Exception {
        return createMavenModules(Collections.singletonList(projectDir))
                .stream()
                .reduce((first, second) -> second)
                .orElseThrow(() -> new IllegalStateException("No module was created"));
    }

    private List<Module> createMavenModules(List<File> projectDirs) throws Exception {
        Project project = getTestFixture().getProject();
        List<VirtualFile> pomFiles = new ArrayList<>();
        for (File projectDir : projectDirs) {
            File moduleDir = new File(project.getBasePath(),
                    projectDir.getName() + counter.getAndIncrement());
            FileUtils.copyDirectory(projectDir, moduleDir);
            VirtualFile pomFile = LocalFileSystem.getInstance()
                    .refreshAndFindFileByIoFile(moduleDir)
                    .findFileByRelativePath("pom.xml");
            pomFiles.add(pomFile);
        }
        importProjects(pomFiles.toArray(VirtualFile[]::new));
        Module[] modules = ModuleManager.getInstance(project).getModules();
        for (Module module : modules) {
            setupJdkForModule(module.getName());
        }
        IndexingTestUtil.waitUntilIndexesAreReady(project);
        Thread.sleep(10000L);
        return Arrays.asList(modules).stream().skip(1).collect(Collectors.toList());
    }

    protected static String getFileUri(String relativePath, Module module) {
        VirtualFile file = LocalFileSystem.getInstance()
                .refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module) + "/" + relativePath);
        return VfsUtilCore.virtualToIoFile(file).toURI().toString();
    }
}
