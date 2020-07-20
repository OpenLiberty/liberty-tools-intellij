package io.openliberty.tools.intellij.util;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

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
     * @param file pom.xml build file
     * @return true if the liberty maven plugin is detected in the pom.xml
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public static boolean validPom(PsiFile file) throws ParserConfigurationException, IOException, SAXException {
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
                                if (mavenPluginDetected(buildNodeList)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }

            // check for liberty maven plugin in plugins
            if (nNode.getNodeName().equals("build")) {
                NodeList buildNodeList = nNode.getChildNodes();
                if (mavenPluginDetected(buildNodeList)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean mavenPluginDetected(NodeList buildNodeList) {
        for (int i = 0; i < buildNodeList.getLength(); i++) {
            Node buildNode = buildNodeList.item(i);
            if (buildNode.getNodeName().equals("plugins")) {
                NodeList plugins = buildNode.getChildNodes();
                for (int j = 0; j < plugins.getLength(); j++) {
                    NodeList plugin = plugins.item(j).getChildNodes();
                    boolean groupId = false;
                    boolean artifactId = false;
                    for (int k = 0; k < plugin.getLength(); k++) {
                        Node node = plugin.item(k);
                        if (node.getNodeName().equals("groupId") && node.getTextContent().equals("io.openliberty.tools")) {
                            groupId = true;
                        } else if (node.getNodeName().equals("artifactId") && node.getTextContent().equals("liberty-maven-plugin")) {
                            artifactId = true;
                        }
                        if (groupId && artifactId) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

}
