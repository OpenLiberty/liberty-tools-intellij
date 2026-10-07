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
import com.intellij.openapi.project.Project;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Assisted by IBM Bob
 *
 * Extracts Liberty multi-module metadata from a Maven {@code pom.xml} file.
 *
 * <p>Uses a two-pass hybrid approach</p>
 * <ol>
 *   <li>Parse the raw {@code pom.xml} directly (fast path). If all {@code <module>}
 *       values are literal strings (no {@code ${...}} variables), use them directly.</li>
 *   <li>If any {@code <module>} value contains a Maven variable, fall back to running
 *       {@code mvn help:evaluate -Dexpression=project.modules} to obtain fully resolved
 *       values from the effective POM.</li>
 * </ol>
 *
 * <p>Specifically this class determines:</p>
 * <ul>
 *   <li>The project name ({@code artifactId})</li>
 *   <li>The parent project name (from {@code <parent>/<artifactId>})</li>
 *   <li>The list of resolved child module names</li>
 *   <li>Whether the Liberty Maven plugin is configured</li>
 *   <li>Whether Liberty dev mode is explicitly skipped ({@code <skip>true</skip>})</li>
 *   <li>Whether this POM is an aggregator ({@code packaging=pom} + has modules)</li>
 * </ul>
 */
public class MavenProjectMetadata extends AbstractProjectMetadata {

    private static final Logger LOGGER = Logger.getInstance(MavenProjectMetadata.class);

    private static final String POM_FILE_NAME = "pom.xml";
    private static final String MODULES_TAG  = "modules";
    private static final String MODULE_TAG   = "module";
    private static final String STRINGS_TAG  = "strings";
    private static final String STRING_TAG   = "string";

    /**
     * Parses the {@code pom.xml} in the given directory and populates all metadata fields.
     *
     * @param pomDir  directory containing the {@code pom.xml} file
     * @param project the IntelliJ project (used to resolve the Maven executable when needed)
     */
    public MavenProjectMetadata(File pomDir, Project project) {
        super(new File(pomDir, POM_FILE_NAME).getAbsolutePath());
        try {
            parsePom(pomDir, project);
        } catch (Exception e) {
            LOGGER.warn("Could not parse Maven metadata from: " + pomDir, e);
        }
    }

    // -------------------------------------------------------------------------
    // Parsing
    // -------------------------------------------------------------------------

    private void parsePom(File pomDir, Project project) throws Exception {
        DocumentBuilder db = newDocumentBuilder();
        File pomFile = new File(pomDir, POM_FILE_NAME);
        Document doc = db.parse(pomFile);
        Element root = doc.getDocumentElement();

        // -- Project name (artifactId direct child of <project>) --
        projectName = getDirectChildText(root, "artifactId");

        // -- Parent project name --
        NodeList parentNodes = root.getElementsByTagName("parent");
        if (parentNodes.getLength() > 0) {
            Element parentEl = (Element) parentNodes.item(0);
            String parentArtifactId = getDirectChildText(parentEl, "artifactId");
            if (!parentArtifactId.isEmpty()) {
                parentProjectName = parentArtifactId;
            }
        }

        // -- packaging=pom --
        boolean pomPackaging = "pom".equals(getDirectChildText(root, "packaging"));

        // -- Child modules --
        if (root.getElementsByTagName(MODULES_TAG).getLength() > 0) {
            List<String> moduleNames = getModulesIfResolved(doc);
            if (moduleNames == null) {
                // One or more <module> values contain ${...} variables — use help:evaluate
                moduleNames = resolveModulesViaHelpEvaluate(pomDir, project, db);
            }
            if (moduleNames != null && !moduleNames.isEmpty()) {
                subprojects.addAll(moduleNames);
                if (pomPackaging) {
                    isAggregator = true;
                }
            }
        }

        // -- Liberty Maven plugin presence and skip flag --
        hasLibertyPlugin = detectLibertyPlugin(doc);
    }

    /**
     * Returns the list of module names if all {@code <module>} values are plain
     * strings (no {@code ${...}} Maven variables), or {@code null} if any variable
     * is present (indicating that {@code help:evaluate} is needed).
     */
    private static List<String> getModulesIfResolved(Document doc) {
        List<String> modules = new ArrayList<>();
        Element root = doc.getDocumentElement();
        NodeList modulesNodes = root.getElementsByTagName(MODULES_TAG);
        if (modulesNodes.getLength() == 0) {
            return modules; // no modules element
        }
        Element modulesEl = (Element) modulesNodes.item(0);
        NodeList moduleNodes = modulesEl.getElementsByTagName(MODULE_TAG);
        for (int i = 0; i < moduleNodes.getLength(); i++) {
            String value = moduleNodes.item(i).getTextContent().trim();
            if (value.contains("${")) {
                return null; // unresolved variable — trigger fallback
            }
            if (!value.isEmpty()) {
                modules.add(value);
            }
        }
        return modules;
    }

    /**
     * Runs {@code mvn help:evaluate -Dexpression=project.modules -q -DforceStdout}
     * in the given directory and parses the XML output to obtain resolved module names.
     * Returns an empty list if the command fails or produces no output.
     */
    private static List<String> resolveModulesViaHelpEvaluate(File pomDir, Project project,
                                                               DocumentBuilder db) {
        List<String> modules = new ArrayList<>();
        try {
            String mavenCmd = LibertyMavenUtil.getMavenExecutable(project);
            if (mavenCmd == null) {
                LOGGER.warn("Could not resolve Maven executable — skipping help:evaluate for: " + pomDir);
                return modules;
            }
            String[] command = {mavenCmd, "help:evaluate",
                    "-Dexpression=project.modules", "-q", "-DforceStdout"};
            Process process = Runtime.getRuntime().exec(command, null, pomDir);
            Document resultDoc = db.parse(process.getInputStream());
            Element root = resultDoc.getDocumentElement();
            if (STRINGS_TAG.equals(root.getTagName())) {
                NodeList children = root.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node node = children.item(i);
                    if (node.getNodeType() == Node.ELEMENT_NODE
                            && STRING_TAG.equals(((Element) node).getTagName())) {
                        String value = node.getTextContent().trim();
                        if (!value.isEmpty()) {
                            modules.add(value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("help:evaluate failed for: " + pomDir, e);
        }
        return modules;
    }

    // -------------------------------------------------------------------------
    // Liberty plugin detection
    // -------------------------------------------------------------------------

    /**
     * Scans {@code <build>}, {@code <profiles>}, and {@code <pluginManagement>}
     * sections for the Liberty Maven plugin. Also sets {@link #isModuleDisabled}
     * when {@code <skip>true</skip>} is found.
     */
    private boolean detectLibertyPlugin(Document doc) {
        Element root = doc.getDocumentElement();
        if (findLibertyPluginInElement(root, "build")) return true;
        NodeList profileNodes = doc.getElementsByTagName("profile");
        for (int i = 0; i < profileNodes.getLength(); i++) {
            if (findLibertyPluginInElement((Element) profileNodes.item(i), "build")) return true;
        }
        NodeList pluginMgmtNodes = doc.getElementsByTagName("pluginManagement");
        for (int i = 0; i < pluginMgmtNodes.getLength(); i++) {
            if (findLibertyPluginInElement((Element) pluginMgmtNodes.item(i), "plugins")) return true;
        }
        return false;
    }

    private boolean findLibertyPluginInElement(Element container, String sectionTag) {
        NodeList sections = container.getElementsByTagName(sectionTag);
        for (int i = 0; i < sections.getLength(); i++) {
            NodeList pluginsNodes = ((Element) sections.item(i)).getElementsByTagName("plugins");
            for (int j = 0; j < pluginsNodes.getLength(); j++) {
                NodeList pluginNodes = ((Element) pluginsNodes.item(j)).getElementsByTagName("plugin");
                for (int k = 0; k < pluginNodes.getLength(); k++) {
                    Element plugin = (Element) pluginNodes.item(k);
                    String groupId   = getDirectChildText(plugin, "groupId");
                    String artifactId = getDirectChildText(plugin, "artifactId");
                    if ("io.openliberty.tools".equals(groupId)
                            && "liberty-maven-plugin".equals(artifactId)) {
                        NodeList configNodes = plugin.getElementsByTagName("configuration");
                        for (int m = 0; m < configNodes.getLength(); m++) {
                            if ("true".equalsIgnoreCase(
                                    getDirectChildText((Element) configNodes.item(m), "skip"))) {
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
     * Returns the text content of the first direct child element with the given
     * tag name, or an empty string when not found.
     */
    private static String getDirectChildText(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && tagName.equals(n.getNodeName())) {
                return n.getTextContent().trim();
            }
        }
        return "";
    }

    /**
     * Creates a securely configured {@link DocumentBuilder}.
     * Silences Apache Xerces System.err output via a no-op {@link ErrorHandler}.
     */
    private static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newDefaultInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setErrorHandler(new ErrorHandler() {
            @Override public void warning(SAXParseException e) throws SAXException {}
            @Override public void error(SAXParseException e) throws SAXException {}
            @Override public void fatalError(SAXParseException e) throws SAXException {}
        });
        return db;
    }

    @Override
    public String toString() {
        return "MavenProjectMetadata{name=" + projectName
                + ", parent=" + parentProjectName
                + ", subprojects=" + subprojects
                + ", aggregator=" + isAggregator
                + ", libertyPlugin=" + hasLibertyPlugin
                + ", disabled=" + isModuleDisabled
                + ", buildFile=" + buildFilePath + "}";
    }
}
