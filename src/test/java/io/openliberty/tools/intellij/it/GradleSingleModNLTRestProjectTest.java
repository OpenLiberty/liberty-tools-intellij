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

import com.automation.remarks.junit5.Video;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

/**
 * Tests that use a single module non Liberty Tools compliant REST Gradle project.
 */
public class GradleSingleModNLTRestProjectTest extends SingleModNLTRestProjectTestCommon {
    /**
     * The path to the folder containing helper test files.
     */
    public static String HELPER_FILES_PATH = Paths.get("src", "test", "resources", "files", "smNLTRestProject", "gradle").toAbsolutePath().toString();

    /**
     * The path to the folder containing the test projects.
     */
    private static final String PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "gradle").toAbsolutePath().toString();

    /**
     * Single module REST project that lacks the configuration to be recognized by Liberty tools.
     */
    private static final String SM_NLT_REST_PROJECT_NAME = "singleModGradleRESTNoLTXmlCfg";

    /**
     * Build file name.
     */
    private final String BUILD_FILE_NAME = "build.gradle";

    /**
     * Prepares the environment for test execution.
     */
    @BeforeAll
    public static void setup() {
        prepareEnv(PROJECTS_PATH, SM_NLT_REST_PROJECT_NAME);
    }

    /**
     * Returns the directory path containing helper files.
     *
     * @return The directory path containing helper files.
     */
    public String getHelperFilesDirPath() {
        return HELPER_FILES_PATH;
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
     * Returns the name of the build file used by the project.
     *
     * @return The name of the build file used by the project.
     */
    @Override
    public String getBuildFileName() {
        return BUILD_FILE_NAME;
    }

    /**
     * Tests:
     * - Refresh button on Liberty tool window toolbar.
     * - Detecting a project with a valid Liberty M/G plugin configuration in build file only.
     * The build file in this case uses the plugins DSL to apply the Liberty Tools binary dependency
     * directly from the gradle plugin community (<a href="https://plugins.gradle.org/">...</a>).
     */
    @Test
    @Video
    public void testsRefreshProjectWithLTBuildCfgOnly() {
        testsRefreshProjectWithLTBuildCfgOnly("pluginsDSLOnlyDepDef.build.gradle");
    }
}
