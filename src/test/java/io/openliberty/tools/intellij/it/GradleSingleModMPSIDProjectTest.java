/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.it;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Tests Liberty Tools actions using a single module MicroProfile Gradle project with space in directory and name.
 */
public class GradleSingleModMPSIDProjectTest extends SingleModMPProjectTestCommon {

    /**
     * Single module Microprofile project name.
     */
    private static final String SM_MP_PROJECT_NAME = "singleModGradleMP";

    /**
     * Single module Microprofile project name with space.
     */
    private static final String SM_MP_PROJECT_NAME_NEW = "singleMod GradleMP";

    /**
     * The path to the folder containing the test projects.
     */
    private static final String PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "gradle").toAbsolutePath().toString();

    /**
     * The path to the folder containing the test projects, including directories with spaces.
     */
    private static final String PROJECTS_PATH_NEW = Paths.get("src", "test", "resources", "projects", "gradle sample").toAbsolutePath().toString();

    /**
     * Prepares the environment for test execution.
     */
    @BeforeAll
    public static void setup() {
        try {
            // Copy the directory to allow renaming.
            TestUtils.copyDirectory(PROJECTS_PATH, PROJECTS_PATH_NEW);

            Path pathNew = Path.of(PROJECTS_PATH_NEW);
            Path projectDirPath = pathNew.resolve(SM_MP_PROJECT_NAME);

            // Define paths for the original and copy of settings.gradle
            Path originalPath = projectDirPath.resolve("settings.gradle");
            Path originalPathCopy = projectDirPath.resolve("settings-copy.gradle");

            // Rename settings.gradle to settings-duplicate.gradle
            Files.move(originalPath, originalPath.resolveSibling("settings-duplicate.gradle"));
            // Rename settings-copy.gradle to settings.gradle
            Files.move(originalPathCopy, originalPathCopy.resolveSibling("settings.gradle"));

            Path projectDirNewPath = pathNew.resolve(SM_MP_PROJECT_NAME_NEW);

            // Rename the project directory to a new name, replacing it if it already exists
            Files.move(projectDirPath, projectDirNewPath, StandardCopyOption.REPLACE_EXISTING);

            // Prepare the environment with the new project path and name
            prepareEnv(PROJECTS_PATH_NEW, SM_MP_PROJECT_NAME_NEW);

        } catch (IOException e) {
            System.err.println("Setup failed: " + e.getMessage());
            e.printStackTrace();
            Assertions.fail("Test setup failed due to an IOException: " + e.getMessage());
        }
    }

    /**
     * Cleanup includes deleting the created project path.
     */
    @AfterAll
    public static void cleanup() {
        try {
            closeProjectView();
        } finally {
            deleteDirectoryIfExists(PROJECTS_PATH_NEW);
        }
    }

    GradleSingleModMPSIDProjectTest() {
        // set the new locations for the test, not the original locations
        setProjectsDirPath(PROJECTS_PATH_NEW);
        setTestReportPath(Paths.get(PROJECTS_PATH_NEW, SM_MP_PROJECT_NAME_NEW, "build", "reports", "tests", "test", "index.html"));
        setSmMPProjectName(SM_MP_PROJECT_NAME_NEW);
        setBuildCategory(BuildType.GRADLE_TYPE);
        setSmMpProjPort(9080);
        setSmMpProjResURI("api/resource");
        setSmMPProjOutput("Hello! Welcome to Open Liberty");
        setWLPInstallPath("build");
        setBuildFileName("build.gradle");
        setBuildFileOpenCommand("Liberty: View Gradle config");
        setStartParams("--hotTests");
        setStartParamsDebugPort("--libertyDebugPort=9876");
        setProjectTypeIsMultiple(false);
        setAbsoluteWLPPath(Paths.get(getProjectsDirPath(), getSmMPProjectName(), getWLPInstallPath()).toString());
    }
}