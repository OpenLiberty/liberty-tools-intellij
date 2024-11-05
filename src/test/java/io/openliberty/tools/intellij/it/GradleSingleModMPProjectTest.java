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

import org.junit.jupiter.api.BeforeAll;

import java.nio.file.Paths;

/**
 * Tests Liberty Tools actions using a single module MicroProfile Gradle project.
 */
public class GradleSingleModMPProjectTest extends SingleModMPProjectTestCommon {

    /**
     * Prepares the environment for test execution.
     */
    @BeforeAll
    public static void setup() {
        setSmMPProjectName("singleModGradleMP");
        setBuildCategory(BuildType.GRADLE_TYPE);
        setProjectsDirPath(Paths.get("src", "test", "resources", "projects", "gradle").toAbsolutePath().toString());
        setSmMpProjPort(9080);
        setSmMpProjResURI("api/resource");
        setSmMPProjOutput("Hello! Welcome to Open Liberty");
        setWLPInstallPath("build");
        setTestReportPath(Paths.get(getProjectsDirPath(), getSmMPProjectName(), "build", "reports", "tests", "test", "index.html"));
        setBuildFileName("build.gradle");
        setBuildFileOpenCommand("Liberty: View Gradle config");
        setStartParams("--hotTests");
        setStartParamsDebugPort("--libertyDebugPort=9876");

        prepareEnv(getProjectsDirPath(), getSmMPProjectName());
    }
}