/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.util;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import io.openliberty.tools.intellij.LibertyPluginIcons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaVersionUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaVersionUtil.class);
    public static boolean isJavaHomeValid(String javaHome, String serverType) {

        if (javaHome == null) {
            String errorMessage = LocalizedResourceUtil.getMessage("javaHome.is.null", serverType, Constants.REQUIRED_JAVA_VERSION);
            LOGGER.error(errorMessage);
            showErrorPopup(errorMessage);
            return false;
        }

        File javaHomeDir = new File(javaHome);
        if (!javaHomeDir.exists()) {
            String errorMessage = LocalizedResourceUtil.getMessage("javaHomeDir.does.not.exist", serverType, Constants.REQUIRED_JAVA_VERSION);
            LOGGER.error(errorMessage);
            showErrorPopup(errorMessage);
            return false;
        }

        if (!checkJavaVersion(javaHome, Constants.REQUIRED_JAVA_VERSION)) {
            String errorMessage = LocalizedResourceUtil.getMessage("java.version.message", serverType, Constants.REQUIRED_JAVA_VERSION);
            LOGGER.error(errorMessage);
            showErrorPopup(errorMessage);
            return false;
        }
        return true;
    }

    private static void notifyError(String errMsg, Project project) {
        Notification notif = new Notification(Constants.LIBERTY_DEV_DASHBOARD_ID, errMsg, NotificationType.WARNING)
                .setTitle(LocalizedResourceUtil.getMessage("java.runtime.error.message"))
                .setIcon(LibertyPluginIcons.libertyIcon)
                .setSubtitle("")
                .setListener(NotificationListener.URL_OPENING_LISTENER);
        Notifications.Bus.notify(notif, project);
    }

    private static void showErrorPopup(String errorMessage) {
        notifyError(errorMessage, ProjectManager.getInstance().getDefaultProject());

    }

    protected static boolean checkJavaVersion(String javaHome, int expectedVersion) {
        final ProcessBuilder builder = new ProcessBuilder(javaHome +
                File.separator + "bin" + File.separator + "java", "-version");
        try {
            final Process p = builder.start();
            final Reader r = new InputStreamReader(p.getErrorStream());
            final StringBuilder sb = new StringBuilder();
            int i;
            while ((i = r.read()) != -1) {
                sb.append((char) i);
            }
            return parseMajorJavaVersion(sb.toString()) >= expectedVersion;
        }
        catch (IOException ioe) {}
        return false;
    }

    private static int parseMajorJavaVersion(String content) {
        final String versionRegex = "version \"(.*)\"";
        Pattern p = Pattern.compile(versionRegex);
        Matcher m = p.matcher(content);
        if (!m.find()) {
            return 0;
        }
        String version = m.group(1);

        // Ignore '1.' prefix for legacy Java versions
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }

        // Extract the major version number.
        final String numberRegex = "\\d+";
        p = Pattern.compile(numberRegex);
        m = p.matcher(version);
        if (!m.find()) {
            return 0;
        }
        return Integer.parseInt(m.group());
    }
}
