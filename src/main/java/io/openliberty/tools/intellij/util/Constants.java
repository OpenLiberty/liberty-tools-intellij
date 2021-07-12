package io.openliberty.tools.intellij.util;

import java.util.HashMap;

public final class Constants {
    public static final String LIBERTY_DEV_DASHBOARD_ID = "Liberty";
    public static final String LIBERTY_GRADLE_PROJECT = "Liberty Gradle Project";
    public static final String LIBERTY_MAVEN_PROJECT = "Liberty Maven Project";

    public static final String LIBERTY_DEV_START = "Start dev mode";
    public static final String LIBERTY_DEV_CUSTOM_START = "Start dev mode...";
    public static final String LIBERTY_DEV_START_CONTAINER = "Start dev mode in a container";
    public static final String LIBERTY_DEV_STOP = "Stop dev mode";
    public static final String LIBERTY_DEV_TESTS = "Run dev mode tests";

    // Maven
    public static final String VIEW_INTEGRATION_TEST_REPORT = "View integration test report";
    public static final String VIEW_UNIT_TEST_REPORT = "View unit test report";
    public static final String LIBERTY_MAVEN_PLUGIN_CONTAINER_VERSION = "3.3-M1";

    // Gradle
    public static final String VIEW_GRADLE_TEST_REPORT = "View test report";
    public static final String TEST_REPORT_STRING = "Test Summary";
    public static final String LIBERTY_GRADLE_PLUGIN_CONTAINER_VERSION = "3.1-M1";

    public static final String LIBERTY_TREE = "LibertyTree";

    /**
     * Constants for Data Context, passing information between the tree nodes and the Actions
     */
    public static final String LIBERTY_BUILD_FILE = "LIBERTY_BUILD_FILE";
    public static final String LIBERTY_PROJECT_NAME = "LIBERTY_PROJECT_NAME";
    public static final String LIBERTY_PROJECT_TYPE = "LIBERTY_PROJECT_TYPE";
    public static final String LIBERTY_PROJECT_MAP = "LIBERTY_PROJECT_MAP";
    public static final String LIBERTY_DASHBOARD_TREE = "LIBERTY_DASHBOARD_TREE";
    public static final String LIBERTY_ACTION_TOOLBAR = "LIBERTY_ACTION_TOOLBAR";

    /**
     * Constants for Action IDs
     */
    public static final String LIBERTY_DEV_START_ACTION_ID = "io.openliberty.tools.intellij.actions.LibertyDevStartAction";
    public static final String LIBERTY_DEV_CUSTOM_START_ACTION_ID = "io.openliberty.tools.intellij.actions.LibertyDevCustomStartAction";
    public static final String LIBERTY_DEV_START_CONTAINER_ACTION_ID = "io.openliberty.tools.intellij.actions.LibertyDevStartContainerAction";
    public static final String LIBERTY_DEV_STOP_ACTION_ID = "io.openliberty.tools.intellij.actions.LibertyDevStopAction";
    public static final String LIBERTY_DEV_TESTS_ACTION_ID = "io.openliberty.tools.intellij.actions.LibertyDevRunTestsAction";
    public static final String VIEW_INTEGRATION_TEST_REPORT_ACTION_ID = "io.openliberty.tools.intellij.actions.ViewIntegrationTestReport";
    public static final String VIEW_UNIT_TEST_REPORT_ACTION_ID = "io.openliberty.tools.intellij.actions.ViewUnitTestReport";
    public static final String VIEW_GRADLE_TEST_REPORT_ACTION_ID = "io.openliberty.tools.intellij.actions.ViewTestReport";
    public static final String VIEW_GRADLE_CONFIG_ACTION_ID = "io.openliberty.tools.intellij.actions.ViewGradleConfig";
    public static final String VIEW_EFFECTIVE_POM_ACTION_ID = "io.openliberty.tools.intellij.actions.ViewEffectivePom";

    private static final HashMap<String, String> CORE_ACTIONS_MAP = new HashMap<String, String>() {
        {
            put(LIBERTY_DEV_START, LIBERTY_DEV_START_ACTION_ID);
            put(LIBERTY_DEV_STOP, LIBERTY_DEV_STOP_ACTION_ID);
            put(LIBERTY_DEV_CUSTOM_START, LIBERTY_DEV_CUSTOM_START_ACTION_ID);
            put(LIBERTY_DEV_START_CONTAINER, LIBERTY_DEV_START_CONTAINER_ACTION_ID);
            put(LIBERTY_DEV_TESTS, LIBERTY_DEV_TESTS_ACTION_ID);
        }
    };

    public static HashMap<String, String> getFullActionMap() {
        HashMap<String, String> fullActionsMap = new HashMap<>();
        fullActionsMap.putAll(CORE_ACTIONS_MAP);
        fullActionsMap.put(VIEW_UNIT_TEST_REPORT, VIEW_UNIT_TEST_REPORT_ACTION_ID);
        fullActionsMap.put(VIEW_INTEGRATION_TEST_REPORT, VIEW_INTEGRATION_TEST_REPORT_ACTION_ID);
        fullActionsMap.put(VIEW_GRADLE_TEST_REPORT, VIEW_GRADLE_TEST_REPORT_ACTION_ID);
        return fullActionsMap;
    }

    public static HashMap<String, String> getMavenMap() {
        HashMap<String, String> mavenActionsMap = new HashMap<>();
        mavenActionsMap.putAll(CORE_ACTIONS_MAP);
        mavenActionsMap.put(VIEW_UNIT_TEST_REPORT, VIEW_UNIT_TEST_REPORT_ACTION_ID);
        mavenActionsMap.put(VIEW_INTEGRATION_TEST_REPORT, VIEW_INTEGRATION_TEST_REPORT_ACTION_ID);
        return mavenActionsMap;
    }

    public static HashMap<String, String> getGradleMap() {
        HashMap<String, String> gradleActionsMap = new HashMap<>();
        gradleActionsMap.putAll(CORE_ACTIONS_MAP);
        gradleActionsMap.put(VIEW_GRADLE_TEST_REPORT, VIEW_GRADLE_TEST_REPORT_ACTION_ID);
        return gradleActionsMap;
    }
}
