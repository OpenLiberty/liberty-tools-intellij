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

import javax.swing.tree.DefaultMutableTreeNode;
import io.openliberty.tools.intellij.util.Constants;

public class LibertyModuleNode extends DefaultMutableTreeNode {
    private LibertyModule libertyModule;

    public LibertyModuleNode(LibertyModule libertyModule) {
        super(libertyModule.getName());
        this.libertyModule = libertyModule;
    }

    public String getName() {
        return libertyModule.getName();
    }

    public VirtualFile getFilePath() {
        return libertyModule.getBuildFile();
    }

    public String getProjectType() {
        return libertyModule.getProjectType();
    }

    public boolean isValidContainerVersion() {
        return libertyModule.isValidContainerVersion();
    }

    public boolean isGradleProjectType() {
        return this.projectType.equals(Constants.LIBERTY_GRADLE_PROJECT);
    }

    public boolean isMavenProjectType() {
        return this.projectType.equals(Constants.LIBERTY_MAVEN_PROJECT);
    }
}
