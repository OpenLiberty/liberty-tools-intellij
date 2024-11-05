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
 * Tests that use a single module non Liberty Tools compliant REST Maven project.
 */
public class MavenSingleModNLTRestProjectTest extends SingleModNLTRestProjectTestCommon {

    /**
     * Prepares the environment for test execution.
     */
    @BeforeAll
    public static void setup() {
        setSmNLTRestProjectName("singleModMavenRESTNoLTXmlCfg");
        setProjectsDirPath(Paths.get("src", "test", "resources", "projects", "maven").toAbsolutePath().toString());
        setBuildFileName("pom.xml");
        setHelperFilesDirPath(Paths.get("src", "test", "resources", "files", "smNLTRestProject", "maven").toAbsolutePath().toString());

        prepareEnv(getProjectsDirPath(), getSmNLTRestProjectName());
    }
}
