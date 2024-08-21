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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.nio.file.Paths;

@DisabledOnOs({OS.WINDOWS})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GradleSingleModJakartaLSTest extends SingleModJakartaLSTestCommon {

    /**
     * Application Name
     */
    public static String PROJECT_NAME = "sampleGradleMPLSApp";

    /**
     * The path to the folder containing the test projects.
     */
    public static String PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "gradle").toAbsolutePath().toString();

    /**
     * Application resoruce URL.
     */
    public GradleSingleModJakartaLSTest() {
        super(PROJECT_NAME, PROJECTS_PATH);
    }

    /**
     * Prepares the environment for test execution.
     */
   //@BeforeAll
    @Test
    @Video
    @Order(1)
    public void setup() {
        prepareEnv(PROJECTS_PATH, PROJECT_NAME);
    }
}
