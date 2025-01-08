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

import java.nio.file.Paths;

public class ItConstants {

    /**
     * Constants for Integration testing
     */
    public static final String SYSTEM_RESOURCE_JAVA = "SystemResource.java";
    public static final String SYSTEM_RESOURCE_2_JAVA = "SystemResource2.java";
    public static final String SYSTEM_RESOURCE = "SystemResource";
    public static final String SYSTEM_RESOURCE_2 = "SystemResource2";

    public static final String CONFIG_DIR_PATH = Paths.get("src", "main", "liberty", "config").toString();
    public static final String SYSTEM_DIR_PATH = Paths.get("src", "main", "java", "io", "openliberty", "mp", "sample", "system").toString();
    public static final String HEALTH_DIR_PATH = Paths.get("src", "main", "java", "io", "openliberty", "mp", "sample", "health").toString();
    public static final String META_INF_DIR_PATH = Paths.get("src", "main", "resources", "META-INF").toString();
    public static final String DEFAULT_SERVER_PATH = Paths.get("wlp", "usr", "servers", "defaultServer").toString();
    public static final String SERVER_XML = "server.xml";
    public static final String SERVER_ENV = "server.env";
    public static final String BOOTSTRAP_PROPERTIES = "bootstrap.properties";
    public static final String COMPACT_MODE = "Compact Mode";

    public static final String SERVICE_LIVE_HEALTH_CHECK_JAVA = "ServiceLiveHealthCheck.java";
    public static final String MPG_PROPERTIES = "microprofile-config.properties";

    public static final String CLOSE_ALL_TABS = "Close All Tabs";
}
