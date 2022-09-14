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

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

public class LibertyProjectNode extends DefaultMutableTreeNode {
    public VirtualFile path;
    public String name;
    public String projectType;
    public boolean validContainerVersion;

    public LibertyProjectNode(PsiFile file, String name, String projectType, boolean validContainerVersion) {
        super(name);
        this.name = name;
        this.projectType = projectType;
        this.path = file.getVirtualFile();
        this.validContainerVersion = validContainerVersion;
    }

    public String getName() {
        return this.name;
    }

    public VirtualFile getFilePath() {
        return this.path;
    }

    public String getProjectType() { return this.projectType; }

    public boolean isValidContainerVersion() { return this.validContainerVersion; }
}
