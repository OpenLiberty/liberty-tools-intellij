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

import java.util.*;

public final class Constants {
    public static final int REQUIRED_JAVA_VERSION = 17;
    public static final String LIBERTY_DEV_DASHBOARD_ID = "Liberty";
    public static final String LIBERTY_GRADLE_PROJECT = "Liberty Gradle Project";
    public static final String LIBERTY_MAVEN_PROJECT = "Liberty Maven Project";

    public static final String LIBERTY_MAVEN_START_CMD = " io.openliberty.tools:liberty-maven-plugin:dev ";
    public static final String LIBERTY_MAVEN_START_CONTAINER_CMD = " io.openliberty.tools:liberty-maven-plugin:devc ";
    public static final String LIBERTY_GRADLE_START_CMD = " libertyDev ";
    public static final String LIBERTY_GRADLE_START_CONTAINER_CMD = " libertyDevc ";
    public static final String LIBERTY_DEV_START = LocalizedResourceUtil.getMessage("start.dev");
    public static final String LIBERTY_DEV_CUSTOM_START = LocalizedResourceUtil.getMessage("start.dev.custom.params");
    public static final String LIBERTY_DEV_START_CONTAINER = LocalizedResourceUtil.getMessage("start.dev.container");
    public static final String LIBERTY_DEV_STOP = LocalizedResourceUtil.getMessage("stop.dev");
    public static final String LIBERTY_DEV_TESTS = LocalizedResourceUtil.getMessage("run.tests.dev");

    // Maven
    public static final String VIEW_INTEGRATION_TEST_REPORT = LocalizedResourceUtil.getMessage("tool.window.maven.integration.test.text");
    public static final String VIEW_UNIT_TEST_REPORT = LocalizedResourceUtil.getMessage("tool.window.maven.unit.test.text");
    public static final String LIBERTY_MAVEN_PLUGIN_CONTAINER_VERSION = "3.3-M1";
    public static final String LIBERTY_MAVEN_DEBUG_PARAM = "-DdebugPort=";

    // Gradle
    public static final String VIEW_GRADLE_TEST_REPORT = LocalizedResourceUtil.getMessage("tool.window.gradle.test.text");
    public static final String TEST_REPORT_STRING = LocalizedResourceUtil.getMessage("test.summary");
    public static final String LIBERTY_GRADLE_PLUGIN_CONTAINER_VERSION = "3.1-M1";
    public static final String LIBERTY_GRADLE_DEBUG_PARAM = "--libertyDebugPort=";


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

    // action triggered from shift-shift "Search Everywhere" IntelliJ window or "cmd/ctl + shift + A" Actions menu
    public static final String GO_TO_ACTION_TRIGGERED = "GoToAction";

    public static final Map<String, String> FULL_ACTIONS_MAP = Collections.unmodifiableMap(new LinkedHashMap<String, String>() {
                {
                    put(LIBERTY_DEV_START, LIBERTY_DEV_START_ACTION_ID);
                    put(LIBERTY_DEV_START_CONTAINER, LIBERTY_DEV_START_CONTAINER_ACTION_ID);
                    put(LIBERTY_DEV_CUSTOM_START, LIBERTY_DEV_CUSTOM_START_ACTION_ID);
                    put(LIBERTY_DEV_TESTS, LIBERTY_DEV_TESTS_ACTION_ID);
                    put(LIBERTY_DEV_STOP, LIBERTY_DEV_STOP_ACTION_ID);
                    put(VIEW_UNIT_TEST_REPORT, VIEW_UNIT_TEST_REPORT_ACTION_ID);
                    put(VIEW_INTEGRATION_TEST_REPORT, VIEW_INTEGRATION_TEST_REPORT_ACTION_ID);
                    put(VIEW_GRADLE_TEST_REPORT, VIEW_GRADLE_TEST_REPORT_ACTION_ID);
                }
            });

    /**
     * Constants for langauge servers
     */
    public static final String SERVER_ENV_GLOB_PATTERN = "**/{src/main/liberty/config,usr/servers/**}/server.env";
}
