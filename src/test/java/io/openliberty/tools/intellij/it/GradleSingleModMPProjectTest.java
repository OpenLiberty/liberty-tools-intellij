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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tests Liberty Tools actions using a single module MicroProfile Gradle project.
 */
public class GradleSingleModMPProjectTest extends SingleModMPProjectTestCommon {
    /**
     * Single module Microprofile project name.
     */
    private static final String SM_MP_PROJECT_NAME = "singleModGradleMP";

    /**
     * The path to the folder containing the test projects.
     */
    private static final String PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "gradle").toAbsolutePath().toString();

    /**
     * Target directory
     */
    private final String TARGET_DIR = "build";

    /**
     * Prepares the environment for test execution.
     */
    @BeforeAll
    public static void setup() {
        prepareEnv(PROJECTS_PATH, SM_MP_PROJECT_NAME);
    }

    GradleSingleModMPProjectTest() {
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
    }

    @Override
    public String getCustomWLPPath() {
        String wlpPath = "";
        try {
            Path configPath = Paths.get(PROJECTS_PATH,SM_MP_PROJECT_NAME, TARGET_DIR, "liberty-plugin-config.xml");

            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(configPath.toString());
            document.getDocumentElement().normalize();

            NodeList nodeList = document.getElementsByTagName("serverDirectory");
            if (nodeList.getLength() > 0) {
                Element element = (Element) nodeList.item(0);
                String serverDirectory = element.getTextContent();

                /* Trim value starts from /wlp to get the exact custom installation path of server */
                Pattern pattern = Pattern.compile("^(.*?)(/wlp)");
                Matcher matcher = pattern.matcher(serverDirectory);
                wlpPath = (matcher.find()) ? matcher.group(1) : "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Paths.get(wlpPath).toAbsolutePath().toString();
    }

    @Override
    public String getTargetDir() {
        return TARGET_DIR;
    }
}