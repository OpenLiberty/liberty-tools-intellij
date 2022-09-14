/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.tools.intellij.util;

import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.treeStructure.Tree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;

public class TreeDataProvider implements DataProvider {

    public VirtualFile currentFile;
    public String projectName;
    public String projectType;
    public HashMap<String, ArrayList<Object>> map = new HashMap<String, ArrayList<Object>>();

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (dataId.equals(Constants.LIBERTY_BUILD_FILE)) {
            return this.currentFile;
        } else if (dataId.equals(Constants.LIBERTY_PROJECT_NAME)) {
            return this.projectName;
        } else if (dataId.equals(Constants.LIBERTY_PROJECT_TYPE)) {
            return this.projectType;
        } else if (dataId.equals(Constants.LIBERTY_PROJECT_MAP)) {
            return this.map;
        }
        return null;
    }

    public void saveData(@NotNull VirtualFile file, @NotNull String projectName, @NotNull String projectType) {
        this.currentFile = file;
        this.projectName = projectName;
        this.projectType = projectType;
    }

    public void setProjectMap(@NotNull HashMap<String, ArrayList<Object>> map) {
        this.map = map;
    }
}
