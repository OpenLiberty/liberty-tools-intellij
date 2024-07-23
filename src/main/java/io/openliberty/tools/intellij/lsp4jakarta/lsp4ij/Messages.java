/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public final class Messages {
    private static final Logger LOGGER = LoggerFactory.getLogger(Messages.class);
    private static ResourceBundle resourceBundle = null;

    private static synchronized void initializeBundles() {
        resourceBundle = ResourceBundle.getBundle("io.openliberty.tools.intellij.lsp4jakarta.messages.messages",
                Locale.getDefault());
    }

    /**
     * Returns message for the given key defined in resource bundle file.
     *
     * @param key  the given key
     * @param args replacements
     * @return Returns message for the given key defined in resource bundle file
     */
    public static String getMessage(String key, Object... args) {
        if (resourceBundle == null) {
            initializeBundles();
        }
        String msg = null;
        try {
            msg = resourceBundle.getString(key);
            if (msg != null && args != null && args.length > 0) {
                msg = MessageFormat.format(msg, args);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get message for '" + key + "'", e);
        }
        return (msg == null) ? key : msg;
    }
}
