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

public class GradleSingleModMPLSTest extends SingleModMPLSTestCommon {

    /**
     * Application Name
     */
    public static String PROJECT_NAME = GRADLE_MPLS_PROJECT;

    /**
     * The path to the folder containing the test projects.
     */
    public static String PROJECTS_PATH = Paths.get(GRADLE_PROJECT_PATH_STR).toAbsolutePath().toString();

    /**
     * Application resoruce URL.
     */
    public GradleSingleModMPLSTest() {
        super(PROJECT_NAME, PROJECTS_PATH);
    }

    /**
     * Prepares the environment for test execution.
     */
    @BeforeAll
    public static void setup() {
        prepareEnv(PROJECTS_PATH, PROJECT_NAME);
    }
}
