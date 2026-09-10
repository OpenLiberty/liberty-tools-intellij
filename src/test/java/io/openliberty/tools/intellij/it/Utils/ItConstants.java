/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.it.Utils;

public class ItConstants {

    /**
     * Constants for Integration testing
     */
    public static final String SYSTEM_RESOURCE_JAVA = "SystemResource.java";
    public static final String SYSTEM_RESOURCE_2_JAVA = "SystemResource2.java";
    public static final String SYSTEM_RESOURCE = "SystemResource";
    public static final String SYSTEM_RESOURCE_2 = "SystemResource2";
    public static final String SERVICE_LIVEHEALTH_CHECK = "ServiceLiveHealthCheck";
    public static final String SERVICE_LIVE_HEALTH_CHECK_JAVA = "ServiceLiveHealthCheck.java";

    public static final String[] CONFIG_DIR_PATH = {"src", "main", "liberty", "config"};
    public static final String[] SYSTEM_DIR_PATH = {"src", "main", "java", "io", "openliberty", "mp", "sample", "system"};
    public static final String[] HEALTH_DIR_PATH = {"src", "main", "java", "io", "openliberty", "mp", "sample", "health"};
    public static final String[] SYSTEM_DIR_PATH_FOR_EXPAND = {"src", "main", "java", "io.openliberty.mp.sample", "system"};
    public static final String[] HEALTH_DIR_PATH_FOR_EXPAND = {"src", "main", "java", "io.openliberty.mp.sample", "health"};
    public static final String[] META_INF_DIR_PATH = {"src", "main", "resources", "META-INF"};
    public static final String[] DEFAULT_SERVER_PATH = {"wlp", "usr", "servers", "defaultServer"};
    public static final String[] MESSAGES_LOG_PATH = {"wlp", "usr", "servers", "defaultServer", "logs"};

    public static final String GRADLE_PROJECT_PATH_STR = "src/test/resources/projects/gradle";
    public static final String GRADLE_PROJECT_PATH_WITH_SPACE = "src/test/resources/projects/gradle sample";
    public static final String NLT_GRADLE_PROJECT_PATH = "src/test/resources/files/smNLTRestProject/gradle";
    public static final String MAVEN_PROJECT_PATH = "src/test/resources/projects/maven";
    public static final String MAVEN_PROJECT_PATH_WITH_SPACE = "src/test/resources/projects/maven sample";
    public static final String NLT_MAVEN_PROJECT_PATH = "src/test/resources/files/smNLTRestProject/maven";
    public static final String MULTIPLE_PRO_FOLDER = "src/test/resources/projects/multiple-project";
    public static final String MULTIPLE_TEST_PRO_PATH = "src/test/resources/projects";
    public static final String INDEX_HTML_PATH = "build/reports/tests/test/index.html";

    public static final String GRADLE_MPLS_PROJECT = "sampleGradleMPLSApp";
    public static final String GRADLE_MP_PROJECT = "singleModGradleMP";
    public static final String GRADLE_MP_PROJECT_WITH_SPACE = "singleMod GradleMP";
    public static final String GRADLE_NLT_PROJECT = "singleModGradleRESTNoLTXmlCfg";
    public static final String MAVEN_MP_PROJECT = "singleModMavenMP";
    public static final String MAVEN_NLT_PROJECT = "singleModMavenRESTNoLTXmlCfg";

    public static final String GRADLE_BUILD_FILE = "build.gradle";
    public static final String MAVEN_BUILD_FILE = "pom.xml";
    public static final String SETTINGS_GRADLE = "settings.gradle";
    public static final String SETTINGS_COPY_GRADLE = "settings-copy.gradle";
    public static final String SETTINGS_DUPLICATE_GRADLE = "settings-duplicate.gradle";
    public static final String SERVER_XML = "server.xml";
    public static final String SERVER_ENV = "server.env";
    public static final String BOOTSTRAP_PROPERTIES = "bootstrap.properties";
    public static final String MPCFG_PROPERTIES = "microprofile-config.properties";
    public static final String MESSAGES_LOG = "messages.log";
    public static final String COMPACT_MODE = "Compact Mode";

    public static final String CLOSE_ALL_TABS = "Close All Tabs";
}
