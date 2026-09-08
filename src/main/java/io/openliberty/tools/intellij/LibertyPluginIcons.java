/*******************************************************************************
 * Copyright (c) 2020, 2026 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij;

import com.intellij.openapi.util.IconLoader;
import com.intellij.ide.ui.LafManager;

import javax.swing.*;

public interface LibertyPluginIcons {
    Icon IntelliJGear = IconLoader.getIcon("AllIcons.General.GearPlain", LibertyPluginIcons.class);
    Icon libertyIcon = IconLoader.getIcon("/icons/OL_logo_13.svg", LibertyPluginIcons.class);
    Icon libertyIcon_40 = IconLoader.getIcon("/icons/OL_logo_40.svg", LibertyPluginIcons.class);
    Icon gradleIcon = IconLoader.getIcon("/icons/gradle-tag-1.png", LibertyPluginIcons.class);
    Icon mavenIcon = IconLoader.getIcon("/icons/maven-tag.png", LibertyPluginIcons.class);

    // -------------------------------------------------------------------------
    // State icons — light/dark variants auto-selected via isDarkTheme()
    // -------------------------------------------------------------------------

    static Icon stateIcon(String name) {
        String lafId = LafManager.getInstance().getCurrentUIThemeLookAndFeel().getId();
        String theme = lafId.toLowerCase(java.util.Locale.ROOT).contains("dark") ? "dark" : "light";
        return IconLoader.getIcon("/icons/state/" + theme + "/" + name + ".svg", LibertyPluginIcons.class);
    }

    /** Stopped state: grey square. */
    static Icon stoppedIcon()    { return stateIcon("stopped");    }
    /** Running state: green play triangle. */
    static Icon runningIcon()    { return stateIcon("running");    }
    /** Starting state: blue half-circle arc. */
    static Icon startingIcon()   { return stateIcon("starting");   }
    /** Stopping state: orange circle with inner square. */
    static Icon stoppingIcon()   { return stateIcon("stopping");   }
    /** Incomplete state: blue half-filled circle (some children running, some not). */
    static Icon incompleteIcon() { return stateIcon("incomplete"); }

}
