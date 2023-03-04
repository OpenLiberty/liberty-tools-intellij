package io.openliberty.tools.intellij.it;

import org.junit.jupiter.api.BeforeAll;

import java.nio.file.Paths;

public class GradleSingleModAppTest extends SingleModAppTestCommon {

    /**
     * Application Name
     */
    public static String PROJECT_NAME = "single-mod-gradle-app";

    public static String PROJECT_PATH = Paths.get("src", "test", "resources", "apps", "gradle", PROJECT_NAME).toAbsolutePath().toString();
    /**
     * Application resoruce URL.
     */
    public static String BASE_URL = "http://localhost:9090/";

    /**
     * Application response payload.
     */
    public static String APP_EXPECTED_OUTPUT = "Hello! Welcome to Open Liberty";

    /**
     * Relative location of the WLP installation.
     */
    public static String WLP_INSTALL_PATH = "/build";

    public GradleSingleModAppTest() {
        super(PROJECT_NAME, PROJECT_PATH, WLP_INSTALL_PATH, BASE_URL, APP_EXPECTED_OUTPUT);
    }

    @BeforeAll
    public static void setup() {
        prepareEnv(PROJECT_PATH, PROJECT_NAME);
    }
}
