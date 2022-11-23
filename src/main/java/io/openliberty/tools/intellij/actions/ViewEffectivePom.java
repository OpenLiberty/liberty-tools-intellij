/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.tools.intellij.actions;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import io.openliberty.tools.intellij.util.Constants;
import io.openliberty.tools.intellij.util.LocalizedResourceUtil;

import java.util.Arrays;
import java.util.List;

public class ViewEffectivePom extends LibertyGeneralAction {

    public ViewEffectivePom() {
        setActionCmd(LocalizedResourceUtil.getMessage("view.effective.pom"));
    }

    @Override
    protected List<String> getSupportedProjectTypes() {
        return Arrays.asList(Constants.LIBERTY_MAVEN_PROJECT);
    }

    @Override
    protected void executeLibertyAction() {
        // open build file
        FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, buildFile), true);
    }

}