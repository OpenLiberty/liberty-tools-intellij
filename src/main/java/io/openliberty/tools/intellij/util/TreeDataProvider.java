/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.tools.intellij.util;

import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.UiDataProvider;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

public class TreeDataProvider implements UiDataProvider {

    public VirtualFile currentFile;
    public String projectName;
    public Constants.ProjectType projectType;
    public HashMap<String, ArrayList<Object>> map = new HashMap<String, ArrayList<Object>>();

    @Override
    public void uiDataSnapshot(@NotNull DataSink sink) {
        sink.set(Constants.LIBERTY_BUILD_FILE_DATAKEY, this.currentFile);
        sink.set(Constants.LIBERTY_PROJECT_NAME, this.projectName);
        sink.set(Constants.LIBERTY_PROJECT_TYPE, this.projectType);
        sink.set(Constants.LIBERTY_PROJECT_MAP, this.map);
    }

    public void saveData(@NotNull VirtualFile file, @NotNull String projectName, @NotNull Constants.ProjectType projectType) {
        this.currentFile = file;
        this.projectName = projectName;
        this.projectType = projectType;
    }

    public void setProjectMap(@NotNull HashMap<String, ArrayList<Object>> map) {
        this.map = map;
    }
}
