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

public class GradleSingleModLSTest extends SingleModLibertyLSTestCommon {

    /**
     * Application Name
     */
    public static String PROJECT_NAME = "singleModGradleMP";

    /**
     * The path to the folder containing the test projects.
     */
    public static String PROJECTS_PATH = Paths.get("src", "test", "resources", "apps", "gradle").toAbsolutePath().toString();

    /**
     * Application resoruce URL.
     */
    public static String BASE_URL = "http://localhost:9090/";

    /**
     * Application response payload.
     */
    public static String APP_EXPECTED_OUTPUT = "Hello! Welcome to Open Liberty";

    /**
     * Relative location of the WLP installation.
     */
    public static String WLP_INSTALL_PATH = "/build";

    /**
     * The path to the test report.
     */
    private final Path pathToTestReport = Paths.get(PROJECTS_PATH, "build", "reports", "tests", "test", "index.html");

    public GradleSingleModLSTest() {
        super(PROJECT_NAME, PROJECTS_PATH);
    }

    /**
     * Prepares the environment for test execution.
     */
    @BeforeAll
    public static void setup() {
        prepareEnv(PROJECTS_PATH, PROJECT_NAME);
    }

    /**
     * Deletes test reports.
     */
    @Override
    public void deleteTestReports() {
        boolean testReportDeleted = TestUtils.deleteFile(pathToTestReport.toFile());
        Assertions.assertTrue(testReportDeleted, () -> "Test report file: " + pathToTestReport + " was not be deleted.");
    }

    /**
     * Validates that test reports were generated.
     */
    @Override
    public void validateTestReportsExist() {
        TestUtils.validateTestReportExists(pathToTestReport);
    }
}
