/*******************************************************************************
* Copyright (c) 2019, 2023 Red Hat Inc. and others.
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
        super.setUpFixtures();
        //myTestFixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getName()).getFixture();
        //myTestFixture.setUp();
        myProjectBuilder = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder(getName());
        final JavaTestFixtureFactory factory = JavaTestFixtureFactory.getFixtureFactory();
        ModuleFixtureBuilder moduleBuilder = myProjectBuilder.addModule(JavaModuleFixtureBuilder.class);
        myTestFixture = factory.createCodeInsightFixture(myProjectBuilder.getFixture());
        myTestFixture.setUp();
        LanguageLevelProjectExtension.getInstance(myTestFixture.getProject()).setLanguageLevel(LanguageLevel.JDK_1_6);
    }

    protected Module createJavaModule(final String name) throws Exception {
        ModuleFixture moduleFixture = myProjectBuilder.addModule(JavaModuleFixtureBuilder.class).getFixture();
        moduleFixture.setUp();
        Module module = myProjectBuilder.addModule(JavaModuleFixtureBuilder.class).getFixture().getModule();
        return module;
    }

    private static AtomicInteger counter = new AtomicInteger(0);

    /**
     * Create a new module into the test project from existing project folder.
     *
     * @param projectDirs the project folders
     * @return the created modules
     */
    protected List<Module> createMavenModules(List<File> projectDirs) throws Exception {
        Project project = myTestFixture.getProject();
        List<VirtualFile> pomFiles = new ArrayList<>();
        for(File projectDir : projectDirs) {
            File moduleDir = new File(project.getBasePath(), projectDir.getName() + counter.getAndIncrement());
            FileUtils.copyDirectory(projectDir, moduleDir);
            VirtualFile pomFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(moduleDir).findFileByRelativePath("pom.xml");
            pomFiles.add(pomFile);

        }
        importProjects(pomFiles.toArray(VirtualFile[]::new));
        Module[] modules = ModuleManager.getInstance(myTestFixture.getProject()).getModules();
        for(Module module : modules) {
            setupJdkForModule(module.getName());
        }
        // QuarkusProjectService.getInstance(myTestFixture.getProject()).processModules();
        return Arrays.asList(modules).stream().skip(1).collect(Collectors.toList());
    }

    protected Module createMavenModule(File projectDir) throws Exception {
        List<Module> modules = createMavenModules(Collections.singletonList(projectDir));
        return modules.get(modules.size() - 1);
    }

    /**
     * Create a new module into the test project from existing in memory POM.
     *
     * @param name the new module name
     * @param xml the project POM
     * @return the created module
     */
    protected Module createMavenModule(String name, String xml) throws Exception {
        Module module = myTestFixture.getModule();
        File moduleDir = new File(module.getModuleFilePath()).getParentFile();
        VirtualFile pomFile = createPomFile(LocalFileSystem.getInstance().findFileByIoFile(moduleDir), xml);
        importProject(pomFile);
        Module[] modules = ModuleManager.getInstance(myTestFixture.getProject()).getModules();
        if (modules.length > 0) {
            module = modules[modules.length - 1];
            setupJdkForModule(module.getName());
        }
        return module;
    }
}
