package io.openliberty.tools.intellij.util;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Util class for loading a localized resource bundle
 */
public final class LocalizedResourceUtil {
    private static final Logger LOGGER = Logger.getLogger(LocalizedResourceUtil.class.getName());
    private static ResourceBundle resourceBundle = null;

    private static synchronized void initializeBundles() {
        resourceBundle = ResourceBundle.getBundle("messages.LibertyBundles", Locale.getDefault());
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
            LOGGER.info("Failed to get message for '" + key + "'");
        }
        return (msg == null) ? key : msg;
    }
}