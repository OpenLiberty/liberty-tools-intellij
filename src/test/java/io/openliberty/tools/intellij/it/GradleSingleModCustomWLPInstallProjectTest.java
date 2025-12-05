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
 * Integration test class for validating Gradle single-module project behavior
 * with a custom Liberty installation.
 *
 * This subclass disables all inherited tests by default using @Disabled at the method level,
 * and selectively re-enables only the relevant ones required for regression validation of
 * issue https://github.com/OpenLiberty/liberty-tools-intellij/issues/415
 * (debug mode startup via toolbar/menu). Other overridden tests are ignored here
 * as they are not applicable to this custom installation configuration.
 *
 * GHA tag - Gradle-Custom-Liberty-Installation
 */
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
     * Verifies that the Liberty server starts correctly in debug mode
     * using the toolbar-based run configuration.
     */
    @Override
    @Test
    public void testStartWithConfigInDebugModeUsingToolbar() {
        super.testStartWithConfigInDebugModeUsingToolbar();
    }

    /**
     * Verifies that the Liberty server starts correctly in debug mode
     * using the menu-based run configuration.
     */
    @Override
    @Test
    public void testStartWithConfigInDebugModeUsingMenu() {
        super.testStartWithConfigInDebugModeUsingMenu();
    }

    /**
     * These tests are temporarily disabled due to
     * timeout issues in GitHub Actions builds.
     * See issue: https://github.com/OpenLiberty/ci.common/issues/1308
     * Re-enable once resolved
     */

    @Disabled("Temporarily disabled due to GHA timeout issues")
    @Override
    @Test
    public void testCustomStartParametersClearedOnConfigRemoval() {}

    @Disabled("Temporarily disabled due to GHA timeout issues")
    @Override
    @Test
    public void testMultipleConfigEditHistory() {}

    @Disabled("Temporarily disabled due to GHA timeout issues")
    @Override
    @Test
    public void testOpenBuildFileActionUsingPopUpMenu() {}

    @Disabled("Temporarily disabled due to GHA timeout issues")
    @Override
    @Test
    public void testRunTestsActionUsingDropDownMenu() {}

    @Disabled("Temporarily disabled due to GHA timeout issues")
    @Override
    @Test
    public void testRunTestsActionUsingPlayToolbarButton() {}

    @Disabled("Temporarily disabled due to GHA timeout issues")
    @Override
    @Test
    public void testRunTestsActionUsingPopUpMenu() {}

    @Disabled("Temporarily disabled due to GHA timeout issues")
    @Override
    @Test
    public void testRunTestsActionUsingSearch() {}

    @Disabled("Temporarily disabled due to GHA timeout issues. It requires further investigation as part of the container test")
    @Override
    @Test
    public void testStartInContainerActionUsingDropDownMenu() {}

    @Disabled("Temporarily disabled due to GHA timeout issues. It requires further investigation as part of the container test")
    @Override
    @Test
    public void testStartInContainerActionUsingPlayToolbarButton() {}

    @Disabled("Temporarily disabled due to GHA timeout issues. It requires further investigation as part of the container test")
    @Override
    @Test
    public void testStartInContainerActionUsingPopUpMenu() {}

    @Disabled("Temporarily disabled due to GHA timeout issues. It requires further investigation as part of the container test")
    @Override
    @Test
    public void testStartInContainerActionUsingSearch() {}

    @Disabled("Temporarily disabled due to GHA timeout issues. It requires further investigation as part of the container test")
    @Override
    @Test
    public void testStartInContainerParamClearedOnConfigRemoval() {}

    @Disabled("Temporarily disabled due to GHA timeout issues")
    @Override
    @Test
    public void testStartWithConfigInRunModeUsingMenu() {}

    @Disabled("Temporarily disabled due to GHA timeout issues")
    @Override
    @Test
    public void testStartWithConfigInRunModeUsingToolbar() {}

    @Disabled("Temporarily disabled due to GHA timeout issues")
    @Override
    @Test
    public void testStartWithParamsActionUsingDropDownMenu() {}

    @Disabled("Temporarily disabled due to GHA timeout issues")
    @Override
    @Test
    public void testStartWithParamsActionUsingPlayToolbarButton() {}

    @Disabled("Temporarily disabled due to GHA timeout issues")
    @Override
    @Test
    public void testStartWithParamsActionUsingPopUpMenu() {}

    @Disabled("Temporarily disabled due to GHA timeout issues")
    @Override
    @Test
    public void testStartWithParamsActionUsingSearch() {}

}