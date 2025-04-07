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
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.LanguageLevelProjectExtension;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.testFramework.IndexingTestUtil;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import com.intellij.testFramework.builders.ModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Modified from:
 * https://github.com/eclipse/lsp4mp/blob/bc926f75df2ca103d78c67b997c87adb7ab480b1/microprofile.jdt/org.eclipse.lsp4mp.jdt.test/src/main/java/org/eclipse/lsp4mp/jdt/core/BasePropertiesManagerTest.java
 * With certain methods modified or deleted to fit the purposes of LSP4Jakarta
 *
 */
public abstract class BaseJakartaTest extends MavenImportingTestCase {

    protected TestFixtureBuilder<IdeaProjectTestFixture> myProjectBuilder;

    @Override
    protected void setUpFixtures() throws Exception {
        // Don't call super.setUpFixtures() here, that will create FocusListener leak.
        myProjectBuilder = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getName());
        final JavaTestFixtureFactory factory = JavaTestFixtureFactory.getFixtureFactory();
        ModuleFixtureBuilder moduleBuilder = myProjectBuilder.addModule(JavaModuleFixtureBuilder.class);
        final var testFixture = factory.createCodeInsightFixture(myProjectBuilder.getFixture());
        setTestFixture(testFixture);
        testFixture.setUp();
        LanguageLevelProjectExtension.getInstance(testFixture.getProject()).setLanguageLevel(LanguageLevel.JDK_1_6);
    }

    private static AtomicInteger counter = new AtomicInteger(0);

    /**
     * Create a new module into the test project from existing project folder.
     *
     * @param projectDirs the project folders
     * @return the created modules
     */
    protected List<Module> createMavenModules(List<File> projectDirs) throws Exception {
        Project project = getTestFixture().getProject();
        List<VirtualFile> pomFiles = new ArrayList<>();
        for (File projectDir : projectDirs) {
            File moduleDir = new File(project.getBasePath(), projectDir.getName() + counter.getAndIncrement());
            FileUtils.copyDirectory(projectDir, moduleDir);
            VirtualFile pomFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(moduleDir).findFileByRelativePath("pom.xml");
            pomFiles.add(pomFile);
        }
        // Calling the non-suspending importProjects method instead of the Kotlin suspending function
        // importProjectsAsync(file: VirtualFile) to prevent blocking unit tests starting from IntelliJ version 2024.2.
        importProjects(pomFiles.toArray(VirtualFile[]::new));
        Module[] modules = ModuleManager.getInstance(getTestFixture().getProject()).getModules();
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
        return Arrays.asList(modules).stream().skip(1).collect(Collectors.toList());
    }

    protected Module createMavenModule(File projectDir) throws Exception {
        List<Module> modules = createMavenModules(Collections.singletonList(projectDir));
        return modules.get(modules.size() - 1);
    }

}
