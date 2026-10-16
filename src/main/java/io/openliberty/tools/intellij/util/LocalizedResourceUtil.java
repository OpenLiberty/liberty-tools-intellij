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

import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Resource bundle for user-facing messages that ARE intended for translation.
 * Messages are loaded from {@code messages/LibertyBundles.properties} and its locale
 * variants (e.g. {@code LibertyBundles_fr.properties}, {@code LibertyBundles_ja.properties}).
 *
 * <p>The {@code @PropertyKey} annotation on the {@code key} parameter enables
 * compile-time validation of message keys in supporting IDEs.</p>
 *
 * <p>Log/debug messages not intended for translation should use
 * {@link LogMessageResourceUtil} instead.</p>
 */
public class LocalizedResourceUtil extends DynamicBundle {
    private static final Logger LOGGER = Logger.getLogger(LocalizedResourceUtil.class.getName());
    static final String BUNDLE_NAME = "messages.LibertyBundles";
    private static final LocalizedResourceUtil INSTANCE = new LocalizedResourceUtil();

    /**
     * Supported languages for localized date formatting.
     * When adding new language support, update this set to include the new language code.
     */
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of(
        Locale.ENGLISH.getLanguage(),
        Locale.JAPANESE.getLanguage()
    );

    protected LocalizedResourceUtil() {
        super(BUNDLE_NAME);
    }

    public static String message(@PropertyKey(resourceBundle = BUNDLE_NAME) String key) {
        return message(key, (Object[]) null);
    }

    public static String message(@PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... args) {
        try {
            if (args == null) {
                return INSTANCE.getMessage(key); // args is defined as a non-null parameter
            } else {
                return INSTANCE.getMessage(key, args);
            }
        } catch (Exception e) {
            LOGGER.info("Failed to get message for '" + key + "'");
            return key;
        }
    }

    /**
     * Returns the locale to use for date/number formatting within the plugin.
     * Falls back to {@link Locale#ENGLISH} if the current IDE locale is not supported.
     */
    public static Locale getFormatterLocale() {
        Locale locale = getLocale();
        return SUPPORTED_LANGUAGES.contains(locale.getLanguage()) ? locale : Locale.ENGLISH;
    }
}
