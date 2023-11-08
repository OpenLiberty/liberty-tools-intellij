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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.idea.maven.execution.MavenExternalParameters;
import org.jetbrains.idea.maven.project.MavenGeneralSettings;
import org.jetbrains.idea.maven.project.MavenWorkspaceSettingsComponent;
import org.jetbrains.idea.maven.server.MavenServerConnector;
import org.jetbrains.idea.maven.server.MavenServerManager;
import org.jetbrains.idea.maven.utils.MavenUtil;
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
import java.util.Collection;
import java.util.Iterator;

import org.apache.maven.artifact.versioning.ComparableVersion;

public class LibertyMavenUtil {
    private static Logger LOGGER = Logger.getInstance(LibertyMavenUtil.class);

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

        File inputFile = new File(file.getPath());
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
        VirtualFile parentFolder = file.getParent();
        return parentFolder.getName();
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

        File inputFile = new File(file.getVirtualFile().getPath());
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

    /**
     * Get Maven preferences from Build Tools in IntelliJ and return a command to execute or an exception
     *
     * @param project liberty project
     * @return String command to execute in the terminal or an exception to display
     * @throws LibertyException
     */
    public static String getMavenSettingsCmd(Project project, VirtualFile buildFile) throws LibertyException {
        MavenGeneralSettings mavenSettings = MavenWorkspaceSettingsComponent.getInstance(project).getSettings().getGeneralSettings();
        String mavenHome = mavenSettings.getMavenHome();
        if (MavenServerManager.WRAPPED_MAVEN.equals(mavenHome)) {
            // it is set to use the wrapper
            return getLocalMavenWrapper(buildFile);
        } else {
            // try to use maven home path defined in the settings
            return getCustomMavenPath(project, mavenHome);
        }
    }

    /**
     * Get the local wrapper path for Maven that is in the project level
     *
     * @param buildFile the build file specified in the application project directory
     * @return the Maven wrapper path to be executed or an exception to display
     * @throws LibertyException
     */
    private static String getLocalMavenWrapper(VirtualFile buildFile) throws LibertyException {
        String mvnw = SystemInfo.isWindows ? ".\\mvnw.cmd" : "./mvnw";
        File wrapper = new File(buildFile.getParent().getPath(), mvnw);
        if (!wrapper.exists()){
            String translatedMessage = LocalizedResourceUtil.getMessage("maven.wrapper.does.not.exist");
            throw new LibertyException("A Maven wrapper for the project could not be found. Make sure to configure a " +
                    "valid Maven wrapper or change the build preferences for Maven inside IntelliJ Maven preferences.", translatedMessage);
        }
        if (!wrapper.canExecute()) {
            String translatedMessage = LocalizedResourceUtil.getMessage("maven.wrapper.cannot.execute");
            throw new LibertyException("Could not execute Maven wrapper because the process does not have permission to " +
                    "execute it. Consider giving executable permission for the Maven wrapper file or changing the build " +
                    "preferences for Maven inside IntelliJ Maven preferences.", translatedMessage);
        }
        return mvnw;
    }

    /**
     * Get the custom Maven path from Built Tools in IntelliJ
     *
     * @param project liberty project
     * @param customMavenHome the custom Maven Home
     * @return Maven path to be executed or an exception to display
     * @throws LibertyException
     */
    private static String getCustomMavenPath(Project project, String customMavenHome) throws LibertyException {
        File mavenHomeFile = MavenUtil.resolveMavenHomeDirectory(customMavenHome); // when customMavenHome path is invalid it returns null
        if (mavenHomeFile == null) {
            String translatedMessage = LocalizedResourceUtil.getMessage("maven.invalid.build.preference");
            throw new LibertyException("Make sure to configure a valid path for Maven home path inside IntelliJ Maven preferences.", translatedMessage);
        }

        // When a custom maven is specified, IntelliJ settings force it to point to the root folder and consider the subfolders invalid,
        // and consequently, it will return null. For this reason, we need to use ./bin/mvn in order to execute maven.
        String maven = SystemInfo.isWindows ? "mvn.cmd" : "mvn";
        File mavenExecutable = new File(new File(mavenHomeFile.getAbsolutePath(), "bin"), maven);
        if (mavenExecutable.exists()) {
            if (mavenExecutable.canExecute()) {
                String additionalCMD = SystemInfo.isWindows ? "cmd /K " : ""; // without it, a new terminal window is opened
                return additionalCMD + LibertyProjectUtil.includeEscapeToString(mavenExecutable.getAbsolutePath());
            } else {
                String mavenJdk = getMavenJdkPath(project);
                String mavenPath = mavenHomeFile.getAbsolutePath();
                String classworldsPath = LibertyMavenUtil.getMavenClassworldsJarPath(mavenPath);
                File java = new File (new File(mavenJdk, "bin"), "java"); // mavenJdk could be null, checked later
                File classworldsConf = MavenUtil.getMavenConfFile(mavenHomeFile);

                if (java.exists() && classworldsConf.exists() && classworldsPath != null && mavenJdk != null) {
                    return LibertyProjectUtil.includeEscapeToString(java.getAbsolutePath()) +
                            " -Dmaven.multiModuleProjectDirectory=" + LibertyProjectUtil.includeEscapeToString(project.getBasePath()) +
                            " -Dmaven.home=" + LibertyProjectUtil.includeEscapeToString(mavenPath) +
                            " -Dclassworlds.conf=" + LibertyProjectUtil.includeEscapeToString(classworldsConf.getAbsolutePath()) +
                            " -classpath " + LibertyProjectUtil.includeEscapeToString(classworldsPath) +
                            " " + MavenExternalParameters.MAVEN_LAUNCHER_CLASS;
                } else {
                    String translatedMessage = LocalizedResourceUtil.getMessage("maven.cannot.execute", mavenExecutable.getAbsolutePath());
                    throw new LibertyException(String.format("Could not execute Maven from %s because the process does not "+
                            "have permission to execute it. Consider giving executable permission for the Maven executable or " +
                            "configure IntelliJ to use the Maven wrapper.", mavenExecutable.getAbsolutePath()), translatedMessage);
                }
            }
        } else {
            String translatedMessage = LocalizedResourceUtil.getMessage("maven.does.not.exist", mavenExecutable.getAbsolutePath());
            throw new LibertyException(String.format("Could not execute the Maven executable %s. Make sure a valid path is configured " +
                    "inside IntelliJ Maven preferences.", mavenExecutable.getAbsolutePath()), translatedMessage);
        }
    }

    /**
     * Get the classworlds jar path from maven home. This is an adaptation from the original getMavenClasspathEntries in org.jetbrains.idea.maven.execution.MavenExternalParameters
     * @param mavenHome the custom Maven Home
     * @return classworlds jar path
     */
    private static String getMavenClassworldsJarPath(final String mavenHome) {
        File mavenHomeBootAsFile = new File(new File(mavenHome, "core"), "boot");
        // if the dir "core/boot" does not exist we are using a Maven version > 2.0.5
        // in this case the classpath must be constructed from the dir "boot"
        if (!mavenHomeBootAsFile.exists()) {
            mavenHomeBootAsFile = new File(mavenHome, "boot");
        }
        File[] files = mavenHomeBootAsFile.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().contains("classworlds") && file.getName().toLowerCase().endsWith(".jar")) {
                    return file.getAbsolutePath();
                }
            }
        }
        return null;
    }

    /**
     * Get the jdk path from IntelliJ Maven preferences
     * @param project liberty project
     * @return path for jdk used by maven
     */
    private static String getMavenJdkPath (Project project) {
        MavenServerManager mavenManager = MavenServerManager.getInstance();
        Collection<MavenServerConnector> msc = mavenManager.getAllConnectors();
        for (Iterator<MavenServerConnector> it = msc.iterator(); it.hasNext();){
            MavenServerConnector ms = it.next();
            if (ms.getProject().getProjectFilePath().equals(project.getProjectFilePath())) {
                return ms.getJdk().getHomePath();
            }
        }
        return null;
    }
}
