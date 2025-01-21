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

public class MavenSingleModLSP4JakartaTest extends SingleModJakartaLSTestCommon {

    /**
     * Application Name
     */
    public static String PROJECT_NAME = "jakarta-sample";

    /**
     * The path to the folder containing the test projects.
     */
    public static String PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "maven").toAbsolutePath().toString();

    /**
     * Application resoruce URL.
     */
    public MavenSingleModLSP4JakartaTest() {
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
