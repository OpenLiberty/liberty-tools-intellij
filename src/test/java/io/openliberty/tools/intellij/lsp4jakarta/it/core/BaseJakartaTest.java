/*******************************************************************************
* Copyright (c) 2019 Red Hat Inc. and others.
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.jetbrains.idea.maven.MavenImportingTestCase;

/**
 * Modified from:
 * https://github.com/eclipse/lsp4mp/blob/bc926f75df2ca103d78c67b997c87adb7ab480b1/microprofile.jdt/org.eclipse.lsp4mp.jdt.test/src/main/java/org/eclipse/lsp4mp/jdt/core/BasePropertiesManagerTest.java
 * With certain methods modified or deleted to fit the purposes of LSP4Jakarta
 *
 */
public class BaseJakartaTest extends MavenImportingTestCase {

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

    protected static IJavaProject loadJavaProject(String projectName, String parentDirName)
            throws CoreException, Exception {
        // Move project to working directory
        File projectFolder = copyProjectToWorkingDirectory(projectName, parentDirName);

        IPath path = new Path(new File(projectFolder, "/.project").getAbsolutePath());
        IProjectDescription description = ResourcesPlugin.getWorkspace().loadProjectDescription(path);
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(description.getName());

        if (!project.exists()) {
            project.create(description, null);
            project.open(null);

            // We need to call waitForBackgroundJobs with a Job which does nothing to have a
            // resolved classpath (IJavaProject#getResolvedClasspath) when search is done.
            IWorkspaceRunnable runnable = new IWorkspaceRunnable() {
                @Override
                public void run(IProgressMonitor monitor) throws CoreException {
                    monitor.done();

                }
            };
            IProgressMonitor monitor = new NullProgressMonitor();
            JavaCore.run(runnable, null, monitor);
            waitForBackgroundJobs(monitor);
        }

        IJavaProject javaProject = JavaModelManager.getJavaModelManager().getJavaModel()
                .getJavaProject(description.getName());
        return javaProject;
    }

    private static File copyProjectToWorkingDirectory(String projectName, String parentDirName) throws IOException {
        File from = new File("projects/" + parentDirName + "/" + projectName);
        File to = new File(getWorkingProjectDirectory(),
                java.nio.file.Paths.get(parentDirName, projectName).toString());

        if (to.exists()) {
            FileUtils.forceDelete(to);
        }

        if (from.isDirectory()) {
            FileUtils.copyDirectory(from, to);
        } else {
            FileUtils.copyFile(from, to);
        }

        return to;
    }

    public static File getWorkingProjectDirectory() throws IOException {
        File dir = new File("target", "workingProjects");
        FileUtils.forceMkdir(dir);
        return dir;
    }

    private static void waitForBackgroundJobs(IProgressMonitor monitor) throws Exception {
        JobHelpers.waitForJobsToComplete(monitor);
    }
}
