/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.idea.maven.project.MavenGeneralSettings;
import org.jetbrains.idea.maven.project.MavenWorkspaceSettingsComponent;
import org.jetbrains.idea.maven.server.MavenServerManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import org.apache.maven.artifact.versioning.ComparableVersion;

public class LibertyMavenUtil {

    /**
     * Return the project name given a pom.xml build file
     * @param file pom.xml
     * @return String of project name
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public static String getProjectNameFromPom(VirtualFile file) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        File inputFile = new File(file.getCanonicalPath());
        Document doc = builder.parse(inputFile);

        doc.getDocumentElement().normalize();
        Node root = doc.getDocumentElement();

        NodeList nList = root.getChildNodes();
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);
            if (nNode.getNodeName().equals("artifactId")) {
                if (nNode.getTextContent() != null) {
                    return nNode.getTextContent();
                }
            }
        }
        return null;
    }

    /**
     * Check if a pom uses the liberty maven plugin
     *
     * @param file pom.xml build file
     * @return BuildFile, validBuildFile true if using the liberty maven plugin,
     * validContainerVersion true if plugin version is valid for dev mode in containers
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public static BuildFile validPom(PsiFile file) throws ParserConfigurationException, IOException, SAXException {
        BuildFile buildFile = new BuildFile(false, false);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        File inputFile = new File(file.getVirtualFile().getCanonicalPath());
        Document doc = builder.parse(inputFile);

        doc.getDocumentElement().normalize();
        Node root = doc.getDocumentElement();

        NodeList nList = root.getChildNodes();
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node nNode = nList.item(temp);

            // check for liberty maven plugin in profiles
            if (nNode.getNodeName().equals("profiles")) {
                NodeList profiles = nNode.getChildNodes();
                for (int i = 0; i < profiles.getLength(); i++) {
                    Node profile = profiles.item(i);
                    if (profile.getNodeName().equals("profile")) {
                        NodeList profileList = profile.getChildNodes();
                        for (int j = 0; j < profileList.getLength(); j++) {
                            if (profileList.item(j).getNodeName().equals("build")) {
                                NodeList buildNodeList = profileList.item(j).getChildNodes();
                                buildFile = mavenPluginDetected(buildNodeList);
                                if (buildFile.isValidBuildFile()){
                                    return buildFile;
                                }
                            }
                        }
                    }
                }
            }

            // check for liberty maven plugin in plugins
            if (nNode.getNodeName().equals("build")) {
                NodeList buildNodeList = nNode.getChildNodes();
                buildFile = mavenPluginDetected(buildNodeList);
                if (buildFile.isValidBuildFile()){
                    return buildFile;
                }

                // check for liberty maven plugin in plugin management
                // indicates this is a parent pom, list in the Liberty Dev Dashboard
                for (int i = 0; i < buildNodeList.getLength(); i++) {
                    Node buildNode = buildNodeList.item(i);
                    if (buildNode.getNodeName().equals("pluginManagement")) {
                        NodeList pluginManagementList = buildNode.getChildNodes();
                        buildFile = mavenPluginDetected(pluginManagementList);
                    }
                }
            }
        }
        return buildFile;
    }

    private static BuildFile mavenPluginDetected(NodeList buildList) {
        for (int i = 0; i < buildList.getLength(); i++) {
            Node buildNode = buildList.item(i);
            if (buildNode.getNodeName().equals("plugins")) {
                NodeList plugins = buildNode.getChildNodes();
                if (buildNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element pluginsElem = (Element) buildNode;
                    NodeList pluginsList = pluginsElem.getElementsByTagName("plugin");
                    for (int j = 0; j < pluginsList.getLength(); j++) {
                        Node pluginNode = pluginsList.item(j);
                        if (pluginNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element pluginElem = (Element) pluginNode;
                            String groupId = "";
                            String artifactId = "";
                            String version = "";
                            if (pluginElem.getElementsByTagName("groupId").getLength() != 0) {
                                groupId = pluginElem.getElementsByTagName("groupId").item(0).getTextContent();
                            }
                            if (pluginElem.getElementsByTagName("artifactId").getLength() != 0) {
                                artifactId = pluginElem.getElementsByTagName("artifactId").item(0).getTextContent();
                            }
                            if (pluginElem.getElementsByTagName("version").getLength() != 0) {
                                version = pluginElem.getElementsByTagName("version").item(0).getTextContent();
                            }
                            if (groupId.equals("io.openliberty.tools") && artifactId.equals("liberty-maven-plugin")){
                                boolean validContainerVersion = containerVersion(version);
                                return (new BuildFile(true, validContainerVersion));
                            }
                        }
                    }
                }
            }
        }
        return (new BuildFile(false, false));
    }

    /**
     * Given liberty-maven-plugin version, determine if it is compatible for dev mode with containers
     *
     * @param version plugin version
     * @return true if valid for dev mode in contianers
     */
    private static boolean containerVersion(String version){
        try {
            if (version.isEmpty()) {
                return true;
            }
            ComparableVersion pluginVersion = new ComparableVersion(version);
            ComparableVersion containerVersion = new ComparableVersion(Constants.LIBERTY_MAVEN_PLUGIN_CONTAINER_VERSION);
            if (pluginVersion.compareTo(containerVersion) >= 0) {
                return true;
            }
            return false;
        } catch (NullPointerException | ClassCastException e) {
            return false;
        }
    }

    public static String getMavenSettingsCmd(Project project) {
        MavenServerManager mavenManager = MavenServerManager.getInstance();
        MavenGeneralSettings mavenSettings = MavenWorkspaceSettingsComponent.getInstance(project).getSettings().getGeneralSettings();
        String mavenHome = mavenSettings.getMavenHome();

        if (mavenManager.WRAPPED_MAVEN.equals(mavenHome)) {
            // it is set to use the wrapper
            String mvnwPath = getLocalMavenWrapper(project);
            if (mvnwPath != null) {
                return mvnwPath;
            }
        } else {
            // try to use maven home path defined in the settings
            String mavenPath = getCustomMavenPath(mavenHome);
            if (mavenPath != null) {
                return mavenPath;
            }
        }
        return "mvn"; // default maven
    }

    private static String getLocalMavenWrapper(Project project) {
        String mvnw = SystemInfo.isWindows ? ".\\mvnw.cmd" : "./mvnw";
        File file = new File(project.getBasePath(), mvnw);
        return file.exists() ? mvnw : null;
    }

    private static String getCustomMavenPath(String customMavenHome) {
        String additionalCMD = SystemInfo.isWindows ? "cmd /K " : ""; // without it, a new terminal window is opened
        File mavenHomeFile = MavenServerManager.getMavenHomeFile(customMavenHome); // when customMavenHome path is invalid it returns null
        if (mavenHomeFile != null) {
            // When a custom maven is specified, IntelliJ settings force it to point to the root folder and consider the subfolders invalid,
            // and consequently, it will return null. For this reason, we need to use ./bin/mvn in order to execute maven.
            File file = new File(mavenHomeFile.getAbsolutePath(), "bin" + File.separator + "mvn");
            return file.exists() ? additionalCMD + "\"" + file.getAbsolutePath() + "\"" : null;
        }
        return null;
    }
}
