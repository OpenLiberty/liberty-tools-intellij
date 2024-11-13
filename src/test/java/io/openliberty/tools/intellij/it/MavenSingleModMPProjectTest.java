/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation.
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tests Liberty Tools actions using a single module MicroProfile Maven project.
 */
public class MavenSingleModMPProjectTest extends SingleModMPProjectTestCommon {

    /**
     * Single module Microprofile project name.
     */
    private static final String SM_MP_PROJECT_NAME = "singleModMavenMP";

    /**
     * The path to the folder containing the test projects.
     */
    private static final String PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "maven").toAbsolutePath().toString();

    /**
     * The paths to the integration test reports. The first is used when maven-surefire-report-plugin 3.4 is used and the second when version 3.5 is used.
     */
    private final Path pathToITReport34 = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, "target", "site", "failsafe-report.html");
    private final Path pathToITReport35 = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, "target", "reports", "failsafe.html");

    /**
     * The paths to the unit test reports. The first is used when maven-surefire-report-plugin 3.4 is used and the second when version 3.5 is used.
     */
    private final Path pathToUTReport34 = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, "target", "site", "surefire-report.html");
    private final Path pathToUTReport35 = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, "target", "reports", "surefire.html");

    /**
     * Prepares the environment for test execution.
     */
    @BeforeAll
    public static void setup() {
        StepWorker.registerProcessor(new StepLogger());
        prepareEnv(PROJECTS_PATH, SM_MP_PROJECT_NAME);
    }

    MavenSingleModMPProjectTest() {
        setProjectsDirPath(PROJECTS_PATH);
        setSmMPProjectName(SM_MP_PROJECT_NAME);
        setBuildCategory(BuildType.MAVEN_TYPE);
        setSmMpProjPort(9080);
        setSmMpProjResURI("api/resource");
        setSmMPProjOutput("Hello! Welcome to Open Liberty");
        setWLPInstallPath(Paths.get("target", "liberty").toString());
        setTestReportPath(Paths.get(getProjectsDirPath(), getSmMPProjectName(), "build", "reports", "tests", "test", "index.html"));
        setBuildFileName("pom.xml");
        setBuildFileOpenCommand("Liberty: View pom.xml");
        setStartParams("-DhotTests=true");
        setStartParamsDebugPort("-DdebugPort=9876");
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
}
