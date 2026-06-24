/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation.
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

import static io.openliberty.tools.intellij.it.Utils.ItConstants.*;

/**
 * Tests that use a single module non Liberty Tools compliant REST Gradle project.
 */
public class GradleSingleModNLTRestProjectTest extends SingleModNLTRestProjectTestCommon {

    /**
     * The path to the folder containing the test projects.
     */
    private static final String PROJECTS_PATH = Paths.get(GRADLE_PROJECT_PATH_STR).toAbsolutePath().toString();

    /**
     * Single module REST project that lacks the configuration to be recognized by Liberty tools.
     */
    private static final String SM_NLT_REST_PROJECT_NAME = GRADLE_NLT_PROJECT;

    /**
     * Prepares the environment for test execution.
     */
    @BeforeAll
    public static void setup() {
        prepareEnv(PROJECTS_PATH, SM_NLT_REST_PROJECT_NAME);
    }

    GradleSingleModNLTRestProjectTest() {
        setProjectsDirPath(PROJECTS_PATH);
        setSmNLTRestProjectName(SM_NLT_REST_PROJECT_NAME);
        setBuildFileName(GRADLE_BUILD_FILE);
        setHelperFilesDirPath(Paths.get(NLT_GRADLE_PROJECT_PATH).toAbsolutePath().toString());
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
