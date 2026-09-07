/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.util;

import com.intellij.openapi.diagnostic.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Assisted by IBM Bob
 *
 * Extracts Liberty multi-module metadata from a Maven {@code pom.xml} file.
 *
 * <p>Specifically this class determines:</p>
 * <ul>
 *   <li>The project name ({@code artifactId})</li>
 *   <li>The parent project name (from {@code <parent>/<artifactId>})</li>
 *   <li>The list of declared child modules (from {@code <modules>})</li>
 *   <li>Whether the Liberty Maven plugin is configured</li>
 *   <li>Whether Liberty dev mode is explicitly skipped</li>
 *   <li>Whether this POM is an aggregator (packaging=pom + has modules)</li>
 *   <li>Inter-project dependencies ({@code <dependency>} artifactIds)</li>
 * </ul>
 */
public class MavenProjectMetadata extends AbstractProjectMetadata {

    private static final Logger LOGGER = Logger.getInstance(MavenProjectMetadata.class);

    /**
     * Parses the given {@code pom.xml} and populates all metadata fields.
     *
     * @param pomXmlPath Absolute path to the {@code pom.xml} file.
     * @throws Exception if the file cannot be read or parsed.
     */
    public MavenProjectMetadata(String pomXmlPath) throws Exception {
        super(pomXmlPath);
        String xmlContent = new String(Files.readAllBytes(Paths.get(pomXmlPath)));
        parsePomXml(xmlContent);
    }

    // -------------------------------------------------------------------------
    // Parsing
    // -------------------------------------------------------------------------

    private void parsePomXml(String xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xmlContent)));
        doc.getDocumentElement().normalize();

        Element root = doc.getDocumentElement();

        // -- Project name (first <artifactId> that is a direct child of <project>) --
        NodeList artifactIdNodes = root.getElementsByTagName("artifactId");
        for (int i = 0; i < artifactIdNodes.getLength(); i++) {
            Node node = artifactIdNodes.item(i);
            if ("project".equals(node.getParentNode().getNodeName())) {
                projectName = node.getTextContent().trim();
                break;
            }
        }

        // -- Parent project name --
        NodeList parentNodes = root.getElementsByTagName("parent");
        if (parentNodes.getLength() > 0) {
            Element parentElement = (Element) parentNodes.item(0);
            NodeList parentArtifactIds = parentElement.getElementsByTagName("artifactId");
            if (parentArtifactIds.getLength() > 0) {
                parentProjectName = parentArtifactIds.item(0).getTextContent().trim();
            }
        }

        // -- packaging=pom check --
        boolean pomPackaging = false;
        NodeList packagingNodes = root.getElementsByTagName("packaging");
        if (packagingNodes.getLength() > 0) {
            pomPackaging = "pom".equals(packagingNodes.item(0).getTextContent().trim());
        }

        // -- Child modules --
        // An aggregator must have both packaging=pom AND declare <modules>.
        NodeList modulesNodes = root.getElementsByTagName("modules");
        if (modulesNodes.getLength() > 0) {
            subprojects = extractModuleNames((Element) modulesNodes.item(0));
            if (!subprojects.isEmpty() && pomPackaging) {
                isAggregator = true;
            }
        }

        // -- Inter-project dependencies --
        projectDependencies = extractProjectDependencies(root);

        // -- Liberty Maven plugin presence and skip flag --
        hasLibertyPlugin = detectLibertyPlugin(doc);
    }

    /**
     * Returns the bare module names listed inside a {@code <modules>} element.
     */
    private List<String> extractModuleNames(Element modulesElement) {
        List<String> modules = new ArrayList<>();
        NodeList moduleNodes = modulesElement.getElementsByTagName("module");
        for (int i = 0; i < moduleNodes.getLength(); i++) {
            String name = moduleNodes.item(i).getTextContent().trim();
            if (!name.isEmpty()) {
                modules.add(name);
            }
        }
        return modules;
    }

    /**
     * Returns all {@code artifactId} values found inside {@code <dependencies>},
     * {@code <dependencyManagement>}, and profile {@code <dependencies>} sections.
     */
    private List<String> extractProjectDependencies(Element root) {
        List<String> deps = new ArrayList<>();

        // Regular <dependencies>
        NodeList depSections = root.getElementsByTagName("dependencies");
        for (int i = 0; i < depSections.getLength(); i++) {
            collectArtifactIds((Element) depSections.item(i), deps);
        }

        // <dependencyManagement>
        NodeList depMgmtSections = root.getElementsByTagName("dependencyManagement");
        for (int i = 0; i < depMgmtSections.getLength(); i++) {
            Element depMgmt = (Element) depMgmtSections.item(i);
            NodeList inner = depMgmt.getElementsByTagName("dependencies");
            for (int j = 0; j < inner.getLength(); j++) {
                collectArtifactIds((Element) inner.item(j), deps);
            }
        }

        return deps;
    }

    private void collectArtifactIds(Element dependenciesElement, List<String> target) {
        NodeList depNodes = dependenciesElement.getElementsByTagName("dependency");
        for (int i = 0; i < depNodes.getLength(); i++) {
            String artifactId = getChildText((Element) depNodes.item(i), "artifactId");
            if (!artifactId.isEmpty() && !target.contains(artifactId)) {
                target.add(artifactId);
            }
        }
    }

    /**
     * Scans build, profiles, and pluginManagement sections for the Liberty Maven plugin.
     * Also sets {@link #isModuleDisabled} when {@code <skip>true</skip>} is found.
     */
    private boolean detectLibertyPlugin(Document doc) {
        Element root = doc.getDocumentElement();

        // <build>
        if (findLibertyPluginInElement(root, "build")) {
            return true;
        }

        // <profiles>/<profile>/<build>
        NodeList profileNodes = doc.getElementsByTagName("profile");
        for (int i = 0; i < profileNodes.getLength(); i++) {
            if (findLibertyPluginInElement((Element) profileNodes.item(i), "build")) {
                return true;
            }
        }

        // <pluginManagement>
        NodeList pluginMgmtNodes = doc.getElementsByTagName("pluginManagement");
        for (int i = 0; i < pluginMgmtNodes.getLength(); i++) {
            if (findLibertyPluginInElement((Element) pluginMgmtNodes.item(i), "plugins")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Searches {@code containerElement} for a {@code <plugins>} list under {@code sectionTag}
     * and returns true when the Liberty Maven plugin is found.
     * Also populates {@link #isModuleDisabled} when {@code <skip>true</skip>} is present.
     */
    private boolean findLibertyPluginInElement(Element containerElement, String sectionTag) {
        NodeList sectionNodes = containerElement.getElementsByTagName(sectionTag);
        for (int i = 0; i < sectionNodes.getLength(); i++) {
            Element section = (Element) sectionNodes.item(i);
            NodeList pluginsNodes = section.getElementsByTagName("plugins");
            for (int j = 0; j < pluginsNodes.getLength(); j++) {
                Element pluginsElement = (Element) pluginsNodes.item(j);
                NodeList pluginNodes = pluginsElement.getElementsByTagName("plugin");
                for (int k = 0; k < pluginNodes.getLength(); k++) {
                    Element plugin = (Element) pluginNodes.item(k);
                    String groupId = getChildText(plugin, "groupId");
                    String artifactId = getChildText(plugin, "artifactId");
                    if ("io.openliberty.tools".equals(groupId) && "liberty-maven-plugin".equals(artifactId)) {
                        // Check for <skip>true</skip> in any <configuration> element
                        NodeList configNodes = plugin.getElementsByTagName("configuration");
                        for (int m = 0; m < configNodes.getLength(); m++) {
                            String skip = getChildText((Element) configNodes.item(m), "skip");
                            if ("true".equalsIgnoreCase(skip)) {
                                isModuleDisabled = true;
                                break;
                            }
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns the trimmed text content of the first direct child element with the given tag,
     * or an empty string if not found.
     */
    private String getChildText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return "";
    }

    @Override
    public String toString() {
        return "MavenProjectMetadata{name=" + projectName
                + ", parent=" + parentProjectName
                + ", subprojects=" + subprojects
                + ", aggregator=" + isAggregator
                + ", libertyPlugin=" + hasLibertyPlugin
                + ", disabled=" + isModuleDisabled
                + ", dependencies=" + projectDependencies
                + ", buildFile=" + buildFilePath + "}";
    }
}
