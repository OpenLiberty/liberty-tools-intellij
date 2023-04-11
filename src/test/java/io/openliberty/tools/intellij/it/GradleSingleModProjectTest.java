/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.it;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tests Liberty Tools actions using a Gradle project.
 */
public class GradleSingleModProjectTest extends SingleModProjectTestCommon {

    /**
     * Single module Microprofile project name.
     */
    private static final String SM_MP_PROJECT_NAME = "singleModGradleMP";

    /**
     * Single module REST project that lacks the configuration to be recognized by Liberty tools.
     */
    private final String SM_NLT_REST_PROJECT_NAME = "singleModGradleRESTNoLTXmlCfg";

    /**
     * The path to the folder containing the test projects.
     */
    private static final String PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "gradle").toAbsolutePath().toString();

    /**
     * Project port.
     */
    private final int SM_MP_PROJECT_PORT = 9090;

    /**
     * Project resource URI.
     */
    private final String SM_MP_PROJECT_RES_URI = "api/resource";

    /**
     * Project response.
     */
    private final String SM_MP_PROJECT_OUTPUT = "Hello! Welcome to Open Liberty";

    /**
     * Relative location of the WLP installation.
     */
    private final String WLP_INSTALL_PATH = "build";

    /**
     * The path to the test report.
     */
    private final Path TEST_REPORT_PATH = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, "build", "reports", "tests", "test", "index.html");

    /**
     * Build file name.
     */
    private final String BUILD_FILE_NAME = "build.gradle";

    /**
     * Action command to open the build file.
     */
    private final String BUILD_FILE_OPEN_CMD = "Liberty: View Gradle config";

    /**
     * Dev mode configuration start parameters.
     */
    private final String DEV_MODE_START_PARAMS = "--hotTests";

    /**
     * Prepares the environment for test execution.
     */
    @BeforeAll
    public static void setup() {
        prepareEnv(PROJECTS_PATH, SM_MP_PROJECT_NAME);
    }

    /**
     * Returns the path where the Liberty server was installed.
     *
     * @return The path where the Liberty server was installed.
     */
    @Override
    public String getWLPInstallPath() {
        return WLP_INSTALL_PATH;
    }

    /**
     * Returns the projects directory path.
     *
     * @return The projects directory path.
     */
    @Override
    public String getProjectsDirPath() {
        return PROJECTS_PATH;
    }

    /**
     * Returns the name of the single module MicroProfile project.
     *
     * @return The name of the single module MicroProfile project.
     */
    @Override
    public String getSmMPProjectName() {
        return SM_MP_PROJECT_NAME;
    }

    /**
     * Returns the name of the single module REST project that does not meet
     * the requirements needed to automatically show in the Liberty tool window.
     * This project's Liberty config file does not the expected name and the
     * build file does not have any Liberty plugin related entries.
     *
     * @return The name of the single module REST project that does not meet the
     * requirements needed to automatically show in the Liberty tool window.
     */
    @Override
    public String getSmNLTRestProjectName() {
        return SM_NLT_REST_PROJECT_NAME;
    }

    /**
     * Returns the expected HTTP response payload associated with the single module
     * MicroProfile project.
     *
     * @return The expected HTTP response payload associated with the single module
     * MicroProfile project.
     */
    @Override
    public String getSmMPProjOutput() {
        return SM_MP_PROJECT_OUTPUT;
    }

    /**
     * Returns the port number associated with the single module MicroProfile project.
     *
     * @return The port number associated with the single module MicroProfile project.
     */
    @Override
    public int getSmMpProjPort() {
        return SM_MP_PROJECT_PORT;
    }

    /**
     * Return the Resource URI associated with the single module MicroProfile project.
     *
     * @return The Resource URI associated with the single module MicroProfile project.
     */
    @Override
    public String getSmMpProjResURI() {
        return SM_MP_PROJECT_RES_URI;
    }

    /**
     * Returns the name of the build file used by the project.
     *
     * @return The name of the build file used by the project.
     */
    @Override
    public String getBuildFileName() {
        return BUILD_FILE_NAME;
    }

    /**
     * Returns the name of the custom action command used to open the build file.
     *
     * @return The name of the custom action command used to open the build file.
     */
    @Override
    public String getBuildFileOpenCommand() {
        return BUILD_FILE_OPEN_CMD;
    }

    /**
     * Returns the custom start parameters to be used to start dev mode.
     *
     * @return The custom start parameters to be used to start dev mode.
     */
    @Override
    public String getStartParams() {
        return DEV_MODE_START_PARAMS;
    }

    /**
     * Deletes test reports.
     */
    @Override
    public void deleteTestReports() {
        boolean testReportDeleted = TestUtils.deleteFile(TEST_REPORT_PATH.toFile());
        Assertions.assertTrue(testReportDeleted, () -> "Test report file: " + TEST_REPORT_PATH + " was not be deleted.");
    }

    /**
     * Validates that test reports were generated.
     */
    @Override
    public void validateTestReportsExist() {
        TestUtils.validateTestReportExists(TEST_REPORT_PATH);
    }
}
