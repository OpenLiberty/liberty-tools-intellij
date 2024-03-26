/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.it;

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

public class MavenCustomLibertyAppTest extends SingleModMPProjectTestCommon{

    private static final String PROJECT_NAME = "custom-liberty-install-maven-app";
    private static final String PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "maven").toAbsolutePath().toString();
    private final String TARGET_DIR = "target";
    private final String WLP_INSTALL_PATH = Paths.get("target", "liberty").toString();
    private final String PROJECT_OUTPUT = "Hello! Welcome to Open Liberty";
    private final int PROJECT_PORT = 9080;
    private final String PROJECT_RES_URI = "api/resource";
    private final String BUILD_FILE_NAME = "pom.xml";
    private final String BUILD_FILE_OPEN_CMD = "Liberty: View effective POM";
    private final Path pathToITReport = Paths.get(PROJECTS_PATH, PROJECT_NAME, "target", "site", "failsafe-report.html");
    private final Path pathToUTReport = Paths.get(PROJECTS_PATH, PROJECT_NAME, "target", "site", "surefire-report.html");
    private final String DEV_MODE_START_PARAMS = "-DhotTests=true";


    @BeforeAll
    public static void setup() {
        prepareEnv(PROJECTS_PATH, PROJECT_NAME);
    }

    @Override
    public String getProjectsDirPath() {
        return PROJECTS_PATH;
    }

    @Override
    public String getSmMPProjectName() {
        return PROJECT_NAME;
    }

    @Override
    public String getSmMPProjOutput() {
        return PROJECT_OUTPUT;
    }

    @Override
    public int getSmMpProjPort() {
        return PROJECT_PORT;
    }

    @Override
    public String getSmMpProjResURI() {
        return PROJECT_RES_URI;
    }

    @Override
    public String getWLPInstallPath() {
        return WLP_INSTALL_PATH;
    }

    @Override
    public String getBuildFileName() {
        return BUILD_FILE_NAME;
    }

    @Override
    public String getBuildFileOpenCommand() {
        return BUILD_FILE_OPEN_CMD;
    }

    @Override
    public String getStartParams() {
        return DEV_MODE_START_PARAMS;
    }

    @Override
    public void deleteTestReports() {
        boolean itReportDeleted = TestUtils.deleteFile(pathToITReport);
        Assertions.assertTrue(itReportDeleted, () -> "Test report file: " + pathToITReport + " was not be deleted.");

        boolean utReportDeleted = TestUtils.deleteFile(pathToUTReport);
        Assertions.assertTrue(utReportDeleted, () -> "Test report file: " + pathToUTReport + " was not be deleted.");
    }

    @Override
    public void validateTestReportsExist() {
        TestUtils.validateTestReportExists(pathToITReport);
        TestUtils.validateTestReportExists(pathToUTReport);
    }

    @Override
    public String getAbsoluteWLPPath() {
        String wlpPath = "";
        try {
            Path configPath = Paths.get(PROJECTS_PATH,PROJECT_NAME, TARGET_DIR, "liberty-plugin-config.xml");

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
