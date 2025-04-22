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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

/**
 * This test class disables all inherited tests via @Disabled, re-enabling only the critical tests
 * related to issue https://github.com/OpenLiberty/liberty-tools-intellij/issues/415
 * (server startup in debug mode via toolbar and main menu).
 */
@Disabled("Disable inherited tests, only run selected overrides")
public class GradleSingleModCustomWLPInstallProjectTest extends SingleModMPProjectTestCommon {
    /**
     * Single module project name.
     */
    private static final String SM_MP_PROJECT_NAME = "singleModGradleCustomInstall";

    /**
     * The path to the folder containing the test projects.
     */
    private static final String PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "gradle").toAbsolutePath().toString();

    /**
     * Prepares the environment for test execution.
     */
    @BeforeAll
    public static void setup() {
        prepareEnv(PROJECTS_PATH, SM_MP_PROJECT_NAME);
    }

    GradleSingleModCustomWLPInstallProjectTest() {
        setProjectsDirPath(PROJECTS_PATH);
        setSmMPProjectName(SM_MP_PROJECT_NAME);
        setBuildCategory(BuildType.GRADLE_TYPE);
        setSmMpProjPort(9080);
        setSmMpProjResURI("api/resource");
        setSmMPProjOutput("Hello! Welcome to Open Liberty");
        setWLPInstallPath("build");
        setTestReportPath(Paths.get(getProjectsDirPath(), getSmMPProjectName(), "build", "reports", "tests", "test", "index.html"));
        setBuildFileName("build.gradle");
        setBuildFileOpenCommand("Liberty: View Gradle config");
        setStartParams("--hotTests");
        setStartParamsDebugPort("--libertyDebugPort=9876");
        setProjectTypeIsMultiple(false);
        setAbsoluteWLPPath(Paths.get(System.getProperty("user.home"), "customInstallDir").toString());
    }

    /**
     * Re-enables this specific test from the base class to validate server startup
     * in debug mode using the toolbar configuration.
     *
     * The entire test class is annotated with @Disabled to prevent running all
     * inherited tests by default. This method is selectively re-enabled with @Test
     * to ensure only this critical scenario is executed as part of this test suite.
     *
     * Reference: https://github.com/OpenLiberty/liberty-tools-intellij/issues/415
     * This test ensures that the debug mode startup functionality via the toolbar
     * is properly verified, which is a part of the fix for issue #415.
     */
    @Override
    @Test
    public void testStartWithConfigInDebugModeUsingToolbar() {
        super.testStartWithConfigInDebugModeUsingToolbar();
    }

    /**
     * Re-enables this specific test from the base class to verify server startup
     * in debug mode using the main menu configuration.
     *
     * The class-level @Disabled annotation disables all inherited tests. This method
     * is intentionally re-enabled to allow execution of just this test while keeping
     * all other inherited tests suppressed in this subclass.
     *
     * Reference: https://github.com/OpenLiberty/liberty-tools-intellij/issues/415
     * This test ensures that the debug mode startup functionality via the main menu
     * is properly verified, which is a part of the fix for issue #415.
     */
    @Override
    @Test
    public void testStartWithConfigInDebugModeUsingMenu() {
        super.testStartWithConfigInDebugModeUsingMenu();
    }
}