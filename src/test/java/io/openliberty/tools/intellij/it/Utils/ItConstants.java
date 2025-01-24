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
    public static final String MPCFG_PROPERTIES = "microprofile-config.properties";

    public static final String[] CONFIG_DIR_PATH = {"src", "main", "liberty", "config"};
    public static final String[] SERVER_XML_PATH = {"src", "main", "liberty", "config", "server.xml"};
    public static final String[] SERVER_ENV_PATH = {"src", "main", "liberty", "config", "server.env"};
    public static final String[] BOOTSTRAP_PROPERTIES_PATH = {"src", "main", "liberty", "config", "bootstrap.properties"};
    public static final String[] SYSTEM_RESOURCE_PATH = {"src", "main", "java", "io", "openliberty", "mp", "sample", "system", "SystemResource.java"};
    public static final String[] SYSTEM_RESOURCE_2_PATH = {"src", "main", "java", "io", "openliberty", "mp", "sample", "system", "SystemResource2.java"};
    public static final String[] SYSTEM_DIR_PATH = {"src", "main", "java", "io.openliberty.mp.sample", "system"};
    public static final String[] SERVICE_LIVE_HEALTHCHECK_PATH = {"src", "main", "java", "io", "openliberty", "mp", "sample", "health", "ServiceLiveHealthCheck.java"};
    public static final String[] HEALTH_DIR_PATH = {"src", "main", "java", "io.openliberty.mp.sample", "health"};
    public static final String[] MPCFG_PATH = {"src", "main", "resources", "META-INF", "microprofile-config.properties"};
    public static final String[] META_INF_DIR_PATH = {"src", "main", "resources", "META-INF"};
    public static final String[] DEFAULT_SERVER_ENV_PATH = {"wlp", "usr", "servers", "defaultServer", "server.env"};
    public static final String[] MESSAGES_LOG_PATH = {"wlp", "usr", "servers", "defaultServer", "logs", "messages.log"};

    public static final String SERVER_XML = "server.xml";
    public static final String SERVER_ENV = "server.env";
    public static final String BOOTSTRAP_PROPERTIES = "bootstrap.properties";
    public static final String COMPACT_MODE = "Compact Mode";

    public static final String CLOSE_ALL_TABS = "Close All Tabs";
}
