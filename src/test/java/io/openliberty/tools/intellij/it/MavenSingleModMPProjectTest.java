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

import com.intellij.remoterobot.stepsProcessing.StepLogger;
import com.intellij.remoterobot.stepsProcessing.StepWorker;
import org.junit.jupiter.api.Assertions;
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
 * Tests Liberty Tools actions using a single module MicroProfile Maven project.
 */
public class MavenSingleModMPProjectTest extends SingleModMPProjectTestCommon {

    /**
     * Single module Microprofile project name.
     */
    private static final String SM_MP_PROJECT_NAME = "singleModMavenMP";

    /**
     * The path to the folder containing the test projects.
     */
    private static final String PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "maven").toAbsolutePath().toString();

    /**
     * Project port.
     */
    private final int SM_MP_PROJECT_PORT = 9080;

    /**
     * Project resource URI.
     */
    private final String SM_MP_PROJECT_RES_URI = "api/resource";

    /**
     * Project response.
     */
    private final String SM_MP_PROJECT_OUTPUT = "Hello! Welcome to Open Liberty";

    /**
     * Relative location of the WLP installation.
     */
    private final String WLP_INSTALL_PATH = Paths.get("target", "liberty").toString();

    /**
     * Build file name.
     */
    private final String BUILD_FILE_NAME = "pom.xml";

    /**
     * Action command to open the build file.
     */
    private final String BUILD_FILE_OPEN_CMD = "Liberty: View effective POM";

    /**
     * The path to the integration test reports.
     */
    private final Path pathToITReport = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, "target", "site", "failsafe-report.html");

    /**
     * The path to the unit test reports.
     */
    private final Path pathToUTReport = Paths.get(PROJECTS_PATH, SM_MP_PROJECT_NAME, "target", "site", "surefire-report.html");

    /**
     * Dev mode configuration start parameters.
     */
    private final String DEV_MODE_START_PARAMS = "-DhotTests=true";

    /**
     * Target directory
     */
    private final String TARGET_DIR = "target";

    /**
     * Prepares the environment for test execution.
     */
    @BeforeAll
    public static void setup() {
        StepWorker.registerProcessor(new StepLogger());
        prepareEnv(PROJECTS_PATH, SM_MP_PROJECT_NAME);
    }

    /**
     * Returns the path where the Liberty server was installed.
     *
     * @return The path where the Liberty server was installed.
     */
    @Override
    public String getWLPInstallPath() {
        return WLP_INSTALL_PATH;
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
     * Returns the name of the single module MicroProfile project.
     *
     * @return The name of the single module MicroProfile project.
     */
    @Override
    public String getSmMPProjectName() {
        return SM_MP_PROJECT_NAME;
    }

    /**
     * Returns the expected HTTP response payload associated with the single module
     * MicroProfile project.
     *
     * @return The expected HTTP response payload associated with the single module
     * MicroProfile project.
     */
    @Override
    public String getSmMPProjOutput() {
        return SM_MP_PROJECT_OUTPUT;
    }

    /**
     * Returns the port number associated with the single module MicroProfile project.
     *
     * @return The port number associated with the single module MicroProfile project.
     */
    @Override
    public int getSmMpProjPort() {
        return SM_MP_PROJECT_PORT;
    }

    /**
     * Return the Resource URI associated with the single module MicroProfile project.
     *
     * @return The Resource URI associated with the single module MicroProfile project.
     */
    @Override
    public String getSmMpProjResURI() {
        return SM_MP_PROJECT_RES_URI;
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
     * Returns the name of the custom action command used to open the build file.
     *
     * @return The name of the custom action command used to open the build file.
     */
    @Override
    public String getBuildFileOpenCommand() {
        return BUILD_FILE_OPEN_CMD;
    }

    /**
     * Returns the custom start parameters to be used to start dev mode.
     *
     * @return The custom start parameters to be used to start dev mode.
     */
    @Override
    public String getStartParams() {
        return DEV_MODE_START_PARAMS;
    }

    /**
     * Deletes test reports.
     */
    @Override
    public void deleteTestReports() {
        boolean itReportDeleted = TestUtils.deleteFile(pathToITReport);
        Assertions.assertTrue(itReportDeleted, () -> "Test report file: " + pathToITReport + " was not be deleted.");

        boolean utReportDeleted = TestUtils.deleteFile(pathToUTReport);
        Assertions.assertTrue(utReportDeleted, () -> "Test report file: " + pathToUTReport + " was not be deleted.");
    }

    /**
     * Validates that test reports were generated.
     */
    @Override
    public void validateTestReportsExist() {
        TestUtils.validateTestReportExists(pathToITReport);
        TestUtils.validateTestReportExists(pathToUTReport);
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
}
