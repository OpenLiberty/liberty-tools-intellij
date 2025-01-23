/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation.
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
 * Test Liberty Tools creation of a Run/Debug configuration without a null pointer exception
 * using a single module MicroProfile Gradle project.
 */
public class GradleSingleModMPCfgProjectTest extends SingleModMPProjectCfgTestCommon {

    /**
     * Single module Microprofile project name specified in file settings.gradle.
     */
    private static final String SM_MP_PROJECT_NAME = "singleModGradleMP";

    /**
     * Project name of Microprofile single module in file settings-copy.gradle.
     */
    private static final String SM_MP_PROJECT_NAME_NEW = "singleMod GradleMP";

    /**
     * The path to the folder containing the test projects.
     */
    private static final String PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "gradle").toAbsolutePath().toString();

    /**
     * The path to the folder containing the copy of the test project.
     */
    private static final String PROJECTS_PATH_NEW = Paths.get("src", "test", "resources", "projects", "gsample2").toAbsolutePath().toString();

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

    GradleSingleModMPCfgProjectTest() {
        // set the new locations for the test, not the original locations
        setProjectsDirPath(PROJECTS_PATH_NEW);
        setSmMPProjectName(SM_MP_PROJECT_NAME_NEW);
        setWLPInstallPath("build");
    }
}