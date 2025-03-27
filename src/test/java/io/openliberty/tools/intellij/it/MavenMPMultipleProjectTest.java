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

import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tests Liberty Tools actions using multiple MicroProfile projects: one is a Gradle project, and the other is a Maven project. The tests are executed in the Maven project.
 */
public class MavenMPMultipleProjectTest extends SingleModMPProjectTestCommon {

    /**
     * The MicroProfile project name from the multiple projects used for running tests.
     */
    private static final String MP_PROJECT_NAME = "singleModMavenMP";

    /**
     * The path of the folder containing two projects.
     */
    private static final String MULTIPLE_PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "multiple-project").toAbsolutePath().toString();

    /**
     * The original path of the parent directory that containing the test projects.
     */
    private static final String MULTIPLE_PROJECTS_PATH_PARENT = Paths.get("src", "test", "resources", "projects").toAbsolutePath().toString();

    /**
     * The original path of the gradle project.
     */
    private static final String GRADLE_PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "gradle", "singleModGradleMP").toAbsolutePath().toString();

    /**
     * The original path of the maven project.
     */
    private static final String MAVEN_PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "maven", "singleModMavenMP").toAbsolutePath().toString();

    /**
     * The new path of the gradle project.
     */
    private static final String GRADLE_MULTIPLE_PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "multiple-project", "singleModGradleMP").toAbsolutePath().toString();

    /**
     * The new path of the maven project.
     */
    private static final String MAVEN_MULTIPLE_PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "multiple-project", "singleModMavenMP").toAbsolutePath().toString();

    /**
     * The paths to the integration test reports. The first is used when maven-surefire-report-plugin 3.4 is used and the second when version 3.5 is used.
     */
    private final Path pathToITReport34 = Paths.get(MULTIPLE_PROJECTS_PATH, MP_PROJECT_NAME, "target", "site", "failsafe-report.html");
    private final Path pathToITReport35 = Paths.get(MULTIPLE_PROJECTS_PATH, MP_PROJECT_NAME, "target", "reports", "failsafe.html");

    /**
     * The paths to the unit test reports. The first is used when maven-surefire-report-plugin 3.4 is used and the second when version 3.5 is used.
     */
    private final Path pathToUTReport34 = Paths.get(MULTIPLE_PROJECTS_PATH, MP_PROJECT_NAME, "target", "site", "surefire-report.html");
    private final Path pathToUTReport35 = Paths.get(MULTIPLE_PROJECTS_PATH, MP_PROJECT_NAME, "target", "reports", "surefire.html");



    /**
     * Prepares the environment for test execution.
     */
    @BeforeAll
    public static void setup() {
        try {
            File theDir = new File(MULTIPLE_PROJECTS_PATH);
            if (theDir.exists()){
                TestUtils.deleteDirectory(theDir);
            }
            // Create a parent directory 'multiple-project' for multiple projects
            theDir.mkdirs();

            // Copy the directory to allow renaming.
            TestUtils.copyDirectory(GRADLE_PROJECTS_PATH, GRADLE_MULTIPLE_PROJECTS_PATH);
            TestUtils.copyDirectory(MAVEN_PROJECTS_PATH, MAVEN_MULTIPLE_PROJECTS_PATH);

            // Prepare the environment with the new project path and name
            prepareEnv(MULTIPLE_PROJECTS_PATH_PARENT, MP_PROJECT_NAME, true);

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
            deleteDirectoryIfExists(MULTIPLE_PROJECTS_PATH);
        }
    }

    MavenMPMultipleProjectTest() {
        // set the new locations for the test, not the original locations
        setProjectsDirPath(MULTIPLE_PROJECTS_PATH);
        setTestReportPath(Paths.get(MULTIPLE_PROJECTS_PATH, MP_PROJECT_NAME, "build", "reports", "tests", "test", "index.html"));
        setSmMPProjectName(MP_PROJECT_NAME);
        setBuildCategory(BuildType.MAVEN_TYPE);
        setSmMpProjPort(9080);
        setSmMpProjResURI("api/resource");
        setSmMPProjOutput("Hello! Welcome to Open Liberty");
        setWLPInstallPath(Paths.get("target", "liberty").toString());
        setBuildFileName("pom.xml");
        setBuildFileOpenCommand("Liberty: View pom.xml");
        setStartParams("-DhotTests=true");
        setStartParamsDebugPort("-DdebugPort=9876");
        setProjectTypeIsMultiple(true);
        setBuildDirectory("target");
    }

    /**
     * Deletes test reports.
     */
    @Override
    public void deleteTestReports() {
        boolean itReportDeleted = TestUtils.deleteFile(pathToITReport34);
        Assertions.assertTrue(itReportDeleted, () -> "Test report file: " + pathToITReport34 + " was not be deleted.");
        itReportDeleted = TestUtils.deleteFile(pathToITReport35);
        Assertions.assertTrue(itReportDeleted, () -> "Test report file: " + pathToITReport35 + " was not be deleted.");

        boolean utReportDeleted = TestUtils.deleteFile(pathToUTReport34);
        Assertions.assertTrue(utReportDeleted, () -> "Test report file: " + pathToUTReport34 + " was not be deleted.");
        utReportDeleted = TestUtils.deleteFile(pathToUTReport35);
        Assertions.assertTrue(utReportDeleted, () -> "Test report file: " + pathToUTReport35 + " was not be deleted.");
    }

    /**
     * Validates that test reports were generated.
     */
    @Override
    public void validateTestReportsExist() {
        TestUtils.validateTestReportExists(pathToITReport34, pathToITReport35);
        TestUtils.validateTestReportExists(pathToUTReport34, pathToUTReport35);
    }

    @Disabled("Skipping this test for MavenMPMultipleProjectTest")
    @Override
    @Test
    public void testStartWithCustomConfigInDebugModeUsingMenu() {

    }

    @Disabled("Skipping this test for MavenMPMultipleProjectTest")
    @Override
    @Test
    public void testStartWithCustomConfigInDebugModeUsingToolbar() {

    }
}