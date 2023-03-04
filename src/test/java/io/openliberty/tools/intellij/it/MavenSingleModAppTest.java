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

import org.junit.jupiter.api.BeforeAll;

import java.nio.file.Paths;

/**
 * Tests Liberty Tools actions using a Maven application.
 */
public class MavenSingleModAppTest extends SingleModAppTestCommon {
    /**
     * Application Name
     */
    public static String PROJECT_NAME = "single-mod-maven-app";

    public static String PROJECT_PATH = Paths.get("src", "test", "resources", "apps", "maven", PROJECT_NAME).toAbsolutePath().toString();
    /**
     * Application resoruce URL.
     */
    public static String BASE_URL = "http://localhost:9080/";

    /**
     * Application response payload.
     */
    public static String APP_EXPECTED_OUTPUT = "Hello! Welcome to Open Liberty";

    /**
     * Relative location of the WLP installation.
     */
    public static String WLP_INSTALL_PATH = "/target/liberty";

    public MavenSingleModAppTest() {
        super(PROJECT_NAME, PROJECT_PATH, WLP_INSTALL_PATH, BASE_URL, APP_EXPECTED_OUTPUT);
    }

    @BeforeAll
    public static void setup() {
        prepareEnv(PROJECT_PATH, PROJECT_NAME);
    }
}
