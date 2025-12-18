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

import com.intellij.remoterobot.stepsProcessing.StepLogger;
import com.intellij.remoterobot.stepsProcessing.StepWorker;
import io.openliberty.tools.intellij.it.SingleModMPProjectTestCommon;
import io.openliberty.tools.intellij.it.TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test Liberty Tools creation of a Run/Debug configuration without a null pointer exception
 * using a single module MicroProfile Maven project.
 *
 * GHA tag - Maven-MicroProfile-RunDebugCreation
 */
public class MavenSingleModMPCfgProjectTest extends SingleModMPProjectCfgTestCommon {

    /**
     * Single module Microprofile project name.
     */
    private static final String SM_MP_PROJECT_NAME = "singleModMavenMP";

    /**
     * The path to the folder containing the test projects.
     */
    private static final String PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "maven").toAbsolutePath().toString();

    /**
     * The path to the folder containing the copy of the test project.
     */
    private static final String PROJECTS_PATH_NEW = Paths.get("src", "test", "resources", "projects", "msample2").toAbsolutePath().toString();

    /**
     * Prepares the environment for test execution.
     */
    @BeforeAll
    public static void setup() {
        try {
            StepWorker.registerProcessor(new StepLogger());
            // Copy the directory from PROJECTS_PATH to PROJECTS_PATH_NEW
            TestUtils.copyDirectory(PROJECTS_PATH, PROJECTS_PATH_NEW);
            prepareEnv(PROJECTS_PATH_NEW, SM_MP_PROJECT_NAME);
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

    MavenSingleModMPCfgProjectTest() {
        setProjectsDirPath(PROJECTS_PATH_NEW);
        setSmMPProjectName(SM_MP_PROJECT_NAME);
        setWLPInstallPath(Paths.get("target", "liberty").toString());
    }
}
