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

import org.junit.jupiter.api.BeforeAll;

import java.nio.file.Paths;

import static io.openliberty.tools.intellij.it.Utils.ItConstants.*;

/**
 * Tests that use a single module non Liberty Tools compliant REST Maven project.
 */
public class MavenSingleModNLTRestProjectTest extends SingleModNLTRestProjectTestCommon {

    /**
     * The path to the folder containing the test projects.
     */
    private static final String PROJECTS_PATH = Paths.get(MAVEN_PROJECT_PATH).toAbsolutePath().toString();

    /**
     * Single module REST project that lacks the configuration to be recognized by Liberty tools.
     */
    private static final String SM_NLT_REST_PROJECT_NAME = MAVEN_NLT_PROJECT;

    /**
     * Prepares the environment for test execution.
     */
    @BeforeAll
    public static void setup() {
        prepareEnv(PROJECTS_PATH, SM_NLT_REST_PROJECT_NAME);
    }

    MavenSingleModNLTRestProjectTest() {
        setProjectsDirPath(PROJECTS_PATH);
        setSmNLTRestProjectName(SM_NLT_REST_PROJECT_NAME);
        setBuildFileName(MAVEN_BUILD_FILE);
        setHelperFilesDirPath(Paths.get(NLT_MAVEN_PROJECT_PATH).toAbsolutePath().toString());
    }
}
