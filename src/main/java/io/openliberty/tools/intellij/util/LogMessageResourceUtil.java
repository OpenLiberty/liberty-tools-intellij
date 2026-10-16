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

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.PropertyKey;

import java.util.logging.Logger;

/**
 * Resource bundle for log messages that are NOT intended for translation.
 * These messages are for debugging/diagnostic purposes only.
 * User-facing messages should use {@link LocalizedResourceUtil} instead.
 */
public class LogMessageResourceUtil extends DynamicBundle {
    private static final Logger LOGGER = Logger.getLogger(LogMessageResourceUtil.class.getName());
    static final String BUNDLE_NAME = "messages.LibertyLogMessages";
    private static final LogMessageResourceUtil INSTANCE = new LogMessageResourceUtil();

    protected LogMessageResourceUtil() {
        super(BUNDLE_NAME);
    }

    public static String message(@PropertyKey(resourceBundle = BUNDLE_NAME) String key) {
        return message(key, (Object[]) null);
    }

    public static String message(@PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... args) {
        try {
            if (args == null) {
                return INSTANCE.getMessage(key);
            } else {
                return INSTANCE.getMessage(key, args);
            }
        } catch (Exception e) {
            LOGGER.info("Failed to get log message for '" + key + "'");
            return key;
        }
    }
}
