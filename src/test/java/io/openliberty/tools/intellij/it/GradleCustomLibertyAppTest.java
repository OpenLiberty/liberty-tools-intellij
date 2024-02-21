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

public class GradleCustomLibertyAppTest extends SingleModMPProjectTestCommon{

    private static final String PROJECT_NAME = "custom-liberty-install-gradle-app";
    private static final String PROJECTS_PATH = Paths.get("src", "test", "resources", "projects", "gradle").toAbsolutePath().toString();
    private final String TARGET_DIR = "build";
    private final String PROJECT_OUTPUT = "Hello! Welcome to Open Liberty";
    private final String WLP_INSTALL_PATH = "build";
    private final int PROJECT_PORT = 9080;
    private final String PROJECT_RES_URI = "api/resource";
    private final Path TEST_REPORT_PATH = Paths.get(PROJECTS_PATH, PROJECT_NAME, "build", "reports", "tests", "test", "index.html");
    private final String BUILD_FILE_NAME = "build.gradle";
    private final String BUILD_FILE_OPEN_CMD = "Liberty: View Gradle config";
    private final String DEV_MODE_START_PARAMS = "--hotTests";


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
        boolean testReportDeleted = TestUtils.deleteFile(TEST_REPORT_PATH);
        Assertions.assertTrue(testReportDeleted, () -> "Test report file: " + TEST_REPORT_PATH + " was not be deleted.");
    }

    @Override
    public void validateTestReportsExist() {
        TestUtils.validateTestReportExists(TEST_REPORT_PATH);
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
