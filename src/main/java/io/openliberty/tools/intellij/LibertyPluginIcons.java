/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public interface LibertyPluginIcons {
    Icon IntelliJGear = IconLoader.getIcon("AllIcons.General.GearPlain", LibertyPluginIcons.class);
    Icon libertyIcon = IconLoader.getIcon("/icons/OL_logo_13.svg", LibertyPluginIcons.class);
    Icon libertyIcon_40 = IconLoader.getIcon("/icons/OL_logo_40.svg", LibertyPluginIcons.class);
    Icon gradleIcon = IconLoader.getIcon("icons/gradle-tag-1.png", LibertyPluginIcons.class);
    Icon mavenIcon = IconLoader.getIcon("/icons/maven-tag.png", LibertyPluginIcons.class);
}
