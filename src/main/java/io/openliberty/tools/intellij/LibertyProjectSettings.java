/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@State(
        name = "LibertyProjectSettings",
        storages = @Storage("liberty-project-settings.xml")
)
public class LibertyProjectSettings implements PersistentStateComponent<LibertyProjectSettings> {

    private volatile Set<String> customLibertyProjects = Collections.synchronizedSet(new HashSet<>());

    public static LibertyProjectSettings getInstance(Project project) {
        return project.getService(LibertyProjectSettings.class);
    }

    public synchronized Set<String> getCustomLibertyProjects() {
        if (customLibertyProjects == null) {
            customLibertyProjects = Collections.synchronizedSet(new HashSet<>());
        }
        return customLibertyProjects;
    }

    public synchronized void setCustomLibertyProjects(Set<String> customLibertyProjects) {
        this.customLibertyProjects = customLibertyProjects;
    }

    @Nullable
    @Override
    public LibertyProjectSettings getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull LibertyProjectSettings state) {
        XmlSerializerUtil.copyBean(state, this);
    }
}
