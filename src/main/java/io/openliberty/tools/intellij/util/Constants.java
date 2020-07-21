package io.openliberty.tools.intellij.util;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public final class Constants {
    public static final String LIBERTY_DEV_DASHBOARD_ID = "Liberty Dev Dashboard";
    public static final String LIBERTY_GRADLE_PROJECT = "Liberty Gradle Project";
    public static final String LIBERTY_MAVEN_PROJECT = "Liberty Maven Project";

    public static final String LIBERTY_DEV_START = "Start";
    public static final String LIBERTY_DEV_CUSTOM_START = "Start...";
    public static final String LIBERTY_DEV_STOP = "Stop";
    public static final String LIBERTY_DEV_TESTS = "Run Tests";

    // Maven
    public static final String VIEW_INTEGRATION_TEST_REPORT = "View integration test report";
    public static final String VIEW_UNIT_TEST_REPORT = "View unit test report";

    // Gradle
    public static final String VIEW_GRADLE_TEST_REPORT = "View test report";
    public static final String TEST_REPORT_STRING = "Test Summary";

    public static final String LIBERTY_TREE = "LibertyTree";

    public static final Icon libertyIcon = IconLoader.getIcon("/icons/OL_logo_13.svg");

    /**
     * Constants for Data Context, passing information between the tree nodes and the Actions
     */
    public static final String LIBERTY_BUILD_FILE = "LIBERTY_BUILD_FILE";
    public static final String LIBERTY_PROJECT_NAME = "LIBERTY_PROJECT_NAME";
    public static final String LIBERTY_PROJECT_TYPE = "LIBERTY_PROJECT_TYPE";

    /**
     * Constants for Action IDs
     */
    public static final String LIBERTY_DEV_START_ACTION_ID = "io.openliberty.tools.intellij.actions.LibertyDevStartAction";
    public static final String LIBERTY_DEV_CUSTOM_START_ACTION_ID = "io.openliberty.tools.intellij.actions.LibertyDevCustomStartAction";
    public static final String LIBERTY_DEV_STOP_ACTION_ID = "io.openliberty.tools.intellij.actions.LibertyDevStopAction";
    public static final String LIBERTY_DEV_TESTS_ACTION_ID = "io.openliberty.tools.intellij.actions.LibertyDevRunTestsAction";
    public static final String VIEW_INTEGRATION_TEST_REPORT_ACTION_ID = "io.openliberty.tools.intellij.actions.ViewIntegrationTestReport";
    public static final String VIEW_UNIT_TEST_REPORT_ACTION_ID = "io.openliberty.tools.intellij.actions.ViewUnitTestReport";
    public static final String VIEW_GRADLE_TEST_REPORT_ACTION_ID = "io.openliberty.tools.intellij.actions.ViewTestReport";
    public static final String VIEW_GRADLE_CONFIG_ACTION_ID = "io.openliberty.tools.intellij.actions.ViewGradleConfig";
    public static final String VIEW_EFFECTIVE_POM_ACTION_ID = "io.openliberty.tools.intellij.actions.ViewEffectivePom";
}
